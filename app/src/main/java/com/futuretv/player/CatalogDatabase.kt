package com.futuretv.player

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class CatalogDatabase(context: Context) {
    data class Stats(val total: Int, val live: Int, val movies: Int, val series: Int, val groups: Int, val seenTotal: Int = 0, val rejectedDuplicate: Int = 0)

    private val helper = Helper(context.applicationContext)

    fun replaceStreaming(
        feed: ((CatalogEntry) -> Unit) -> Unit,
        onProgress: (Int) -> Unit = {},
        onCatalogReady: (Stats) -> Unit = {},
    ): Stats {
        val db = helper.writableDatabase
        var total = 0
        var liveCount = 0
        var movieCount = 0
        var seriesCount = 0
        var seenTotal = 0
        var rejectedDuplicate = 0
        val groups = HashSet<String>()
        var transactionOpen = false
        var readySent = false
        val batchSize = 5_000
        // Limiar pra sinalizar "pronto pra navegar" -- mais baixo que o lote
        // de commit normal, pra nao atrasar quando o app fica usavel so
        // porque o lote de gravacao ficou maior (menos commits = menos
        // disputa com leitores, mas o usuario nao devia esperar mais por
        // causa disso).
        val readyThreshold = 2_000
        // O catálogo é um cache reconstruível: durante a importação, priorizamos velocidade.
        db.execSQL("PRAGMA synchronous=OFF")
        db.execSQL("PRAGMA temp_store=MEMORY")
        // Remover índices antes do DELETE evita manter três estruturas enquanto a tabela é esvaziada.
        db.execSQL("DROP INDEX IF EXISTS idx_catalog_kind_group")
        db.execSQL("DROP INDEX IF EXISTS idx_catalog_name")
        db.execSQL("DROP INDEX IF EXISTS idx_catalog_series_season")
        fun beginBatch() {
            if (!transactionOpen) {
                db.beginTransactionNonExclusive()
                transactionOpen = true
            }
        }
        fun commitBatch() {
            if (transactionOpen) {
                db.setTransactionSuccessful()
                db.endTransaction()
                transactionOpen = false
            }
        }
        try {
            beginBatch()
            db.delete(TABLE, null, null)
            val statement = db.compileStatement(
                "INSERT OR IGNORE INTO $TABLE " +
                    "(item_key,name,group_title,tvg_id,logo_url,stream_url,kind,quality,series_group,season,episode,year,synopsis,cast,backdrop_url,trailer_url,runtime,is_adult) " +
                    "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)"
            )
            feed { entry ->
                seenTotal++
                statement.clearBindings()
                bind(statement, entry)
                val insertedRowId = statement.executeInsert()
                if (insertedRowId != -1L) {
                    total++
                    when (entry.kind) {
                        MediaKind.LIVE -> liveCount++
                        MediaKind.MOVIE -> movieCount++
                        MediaKind.SERIES -> seriesCount++
                    }
                    groups += entry.groupTitle
                    if (total % 2_000 == 0) onProgress((60 + total / 8_000).coerceAtMost(94))
                    if (total % batchSize == 0) {
                        commitBatch()
                        beginBatch()
                    } else if (!readySent && total >= readyThreshold) {
                        // Precisa commitar AGORA, mesmo sem ter enchido o
                        // lote inteiro (5000) -- sinalizar "pronto" antes do
                        // commit deixaria os dados invisíveis pra quem
                        // consulta (a transação ainda não foi confirmada).
                        commitBatch()
                        beginBatch()
                    }
                    if (!readySent && total >= readyThreshold) {
                        readySent = true
                        runCatching { onCatalogReady(Stats(total, liveCount, movieCount, seriesCount, groups.size)) }
                    }
                } else {
                    // A linha tinha uma item_key que já existia -- ou é uma
                    // duplicata de verdade no M3U do provedor, ou (mais
                    // preocupante) um choque de chave entre itens
                    // DIFERENTES. Antes isso desaparecia silenciosamente;
                    // agora fica contado e visível no log pra confirmar com
                    // números reais em vez de suposição.
                    rejectedDuplicate++
                }
            }
            commitBatch()
        } finally {
            if (transactionOpen) {
                db.endTransaction()
                transactionOpen = false
            }
            // Recriar índices precisa de acesso exclusivo por um instante --
            // sobe o busy_timeout só por esse momento específico, e volta pro
            // padrão curto (500ms) logo depois, pra não afetar consultas
            // normais feitas em qualquer outro momento da importação.
            db.rawQuery("PRAGMA busy_timeout=5000", null)?.use { it.moveToFirst() }
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_catalog_kind_group ON $TABLE(kind, group_title)")
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_catalog_name ON $TABLE(name COLLATE NOCASE)")
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_catalog_series_season ON $TABLE(kind, series_group, season)")
            db.execSQL("PRAGMA synchronous=NORMAL")
            db.rawQuery("PRAGMA busy_timeout=500", null)?.use { it.moveToFirst() }
        }
        return Stats(total, liveCount, movieCount, seriesCount, groups.size, seenTotal, rejectedDuplicate)
    }

    fun replace(entries: Sequence<CatalogEntry>): Stats = replaceStreaming({ emit -> entries.forEach(emit) })

    fun clear() {
        runCatching { helper.writableDatabase.delete(TABLE, null, null) }
    }

    // Diagnóstico: mostra os group_title EXATOS (e em qual "kind" caíram)
    // pra qualquer grupo que contenha um dos termos de busca -- usado pra
    // achar rápido por que uma categoria (ex: Netflix, AMC) não aparece
    // onde deveria, em vez de tentar adivinhar o formato do painel.
    fun rawGroupsMatching(terms: List<String>): List<Pair<String, String>> = runCatching {
        val db = helper.readableDatabase
        val clause = terms.joinToString(" OR ") { "group_title LIKE ?" }
        val args = terms.map { "%$it%" }.toTypedArray()
        db.rawQuery("SELECT DISTINCT group_title, kind FROM $TABLE WHERE $clause LIMIT 60", args).use { cursor ->
            buildList {
                while (cursor.moveToNext()) add((cursor.getString(0).orEmpty()) to (cursor.getString(1).orEmpty()))
            }
        }
    }.getOrDefault(emptyList())

    fun count(): Int = helper.readableDatabase.rawQuery("SELECT COUNT(*) FROM $TABLE", null).use { if (it.moveToFirst()) it.getInt(0) else 0 }

    fun stats(): Stats {
        return aggregateStats(helper.readableDatabase)
    }

    fun groups(kind: MediaKind, hidden: Set<String>, includeAdult: Boolean = false): List<String> {
        val attempt = {
            val db = helper.readableDatabase
            val args = mutableListOf(kind.name)
            val hiddenClause = hiddenClause(hidden, args)
            val sql = "SELECT DISTINCT group_title FROM $TABLE WHERE kind=? AND is_adult=0 $hiddenClause ORDER BY group_title COLLATE NOCASE"
            val groups = db.rawQuery(sql, args.toTypedArray()).use { cursor ->
                buildList {
                    while (cursor.moveToNext()) add(cursor.getString(0).orEmpty().ifBlank { "Sem categoria" })
                }
            }.toMutableList()
            if (includeAdult) {
                val adultArgs = mutableListOf(kind.name)
                val adultHiddenClause = hiddenClause(hidden, adultArgs)
                val hasAdult = db.rawQuery("SELECT 1 FROM $TABLE WHERE kind=? AND is_adult=1 $adultHiddenClause LIMIT 1", adultArgs.toTypedArray()).use { it.moveToFirst() }
                if (hasAdult) groups += ContentSafety.LOCKED_CATEGORY
            }
            groups
        }
        // Leitura durante importação pesada pode falhar por contenção transitória;
        // uma segunda tentativa evita que as categorias somem da tela à toa.
        return runCatching(attempt).getOrNull() ?: runCatching(attempt).getOrDefault(emptyList())
    }

    fun queryPage(
        kind: MediaKind?,
        group: String,
        search: String,
        hidden: Set<String>,
        favorites: Set<String>,
        sortMode: SortMode,
        limit: Int,
        offset: Int,
        seriesOnly: Boolean = false,
        includeAdult: Boolean = false,
    ): List<CatalogEntry> {
        if (seriesOnly) return querySeriesPage(group, search, hidden, sortMode, limit, offset, includeAdult)
        if (kind == null && favorites.isEmpty()) return emptyList()
        val db = helper.readableDatabase
        val where = mutableListOf<String>()
        val args = mutableListOf<String>()
        kind?.let { where += "kind=?"; args += it.name }
        when {
            group == ContentSafety.LOCKED_CATEGORY -> where += if (includeAdult) "is_adult=1" else "0"
            group != "Todos" -> { where += "group_title=? AND is_adult=0"; args += group }
            !includeAdult -> where += "is_adult=0"
        }
        if (search.isNotBlank()) {
            where += "(LOWER(name) LIKE ? OR LOWER(group_title) LIKE ? OR LOWER(tvg_id) LIKE ?)"
            val value = "%${search.trim().lowercase()}%"
            args += value; args += value; args += value
        }
        if (hidden.isNotEmpty()) where += "UPPER(group_title) NOT IN (${hidden.joinToString(",") { "?" }})".also { args.addAll(hidden.map(String::uppercase)) }
        if (favorites.isNotEmpty()) where += "item_key IN (${favorites.joinToString(",") { "?" }})".also { args.addAll(favorites) }
        val selection = if (where.isEmpty()) "" else "WHERE ${where.joinToString(" AND ")}"
        // "Nota" ainda cai pra "recentes" até termos as notas do TMDB.
        val order = when (sortMode) {
            SortMode.ALPHABETICAL -> "is_adult ASC, name COLLATE NOCASE ASC"
            SortMode.RECENT, SortMode.RATING -> "is_adult ASC, rowid ASC"
        }
        val sql = "SELECT * FROM $TABLE $selection ORDER BY $order LIMIT $limit OFFSET $offset"
        return db.rawQuery(sql, args.toTypedArray()).use { cursor ->
            buildList {
                while (cursor.moveToNext()) add(readEntry(cursor))
            }
        }
    }

    private fun querySeriesPage(group: String, search: String, hidden: Set<String>, sortMode: SortMode, limit: Int, offset: Int, includeAdult: Boolean): List<CatalogEntry> =
        querySeriesPageGrouped(group, search, hidden, sortMode, limit, offset, includeAdult)

    private fun querySeriesPageGrouped(group: String, search: String, hidden: Set<String>, sortMode: SortMode, limit: Int, offset: Int, includeAdult: Boolean): List<CatalogEntry> {
        val db = helper.readableDatabase
        val (sourceFilter, sourceArgs) = seriesFilter("source", group, search, hidden, includeAdult)
        val sourceIdentity = "LOWER(TRIM(CASE WHEN TRIM(source.series_group) <> '' THEN source.series_group ELSE source.name END))"
        val cardOrder = when (sortMode) {
            SortMode.ALPHABETICAL -> "card.is_adult ASC, card.name COLLATE NOCASE ASC"
            SortMode.RECENT, SortMode.RATING -> "card.is_adult ASC, card.rowid ASC"
        }
        val sql = "SELECT card.* FROM $TABLE card INNER JOIN (" +
            "SELECT MIN(source.rowid) AS first_rowid FROM $TABLE source WHERE $sourceFilter GROUP BY $sourceIdentity" +
            ") roots ON card.rowid = roots.first_rowid ORDER BY $cardOrder LIMIT $limit OFFSET $offset"
        val grouped = runCatching {
            db.rawQuery(sql, sourceArgs.toTypedArray()).use { cursor ->
                buildList {
                    while (cursor.moveToNext()) add(readEntry(cursor))
                }
            }
        }.getOrDefault(emptyList())
        if (grouped.isNotEmpty()) return grouped
        return queryPage(MediaKind.SERIES, group, search, hidden, emptySet(), sortMode, limit, offset, false, includeAdult)
    }

    private fun seriesFilter(alias: String, group: String, search: String, hidden: Set<String>, includeAdult: Boolean): Pair<String, List<String>> {
        val where = mutableListOf("$alias.kind=?")
        val args = mutableListOf(MediaKind.SERIES.name)
        when {
            group == ContentSafety.LOCKED_CATEGORY -> where += if (includeAdult) "$alias.is_adult=1" else "0"
            group != "Todos" -> { where += "$alias.group_title=? AND $alias.is_adult=0"; args += group }
            !includeAdult -> where += "$alias.is_adult=0"
        }
        if (search.isNotBlank()) {
            where += "(LOWER($alias.name) LIKE ? OR LOWER($alias.group_title) LIKE ? OR LOWER($alias.tvg_id) LIKE ? OR LOWER($alias.series_group) LIKE ?)"
            val value = "%${search.trim().lowercase()}%"
            args += value; args += value; args += value; args += value
        }
        if (hidden.isNotEmpty()) where += "UPPER($alias.group_title) NOT IN (${hidden.joinToString(",") { "?" }})".also { args.addAll(hidden.map(String::uppercase)) }
        return where.joinToString(" AND ") to args
    }

    fun first(kind: MediaKind?, group: String, search: String, hidden: Set<String>, favorites: Set<String>, sortMode: SortMode): CatalogEntry? =
        queryPage(kind, group, search, hidden, favorites, sortMode, 1, 0).firstOrNull()

    // Diagnostico READ-ONLY (nao muda nenhum comportamento real do app):
    // compara quantos EPISODIOS brutos existem numa categoria de serie
    // contra quantas IDENTIDADES distintas o agrupamento (series_group ou
    // nome, ver querySeriesPageGrouped) enxerga ali. Se uma categoria tem
    // dezenas de episodios mas so 1 identidade, isso confirma ao vivo (sem
    // adivinhar) que o agrupamento esta colapsando series diferentes numa
    // so -- e mostra ATE 5 valores de identidade reais pra eu conseguir ver
    // o formato exato do titulo que esta confundindo a heuristica.
    fun seriesGroupingDebug(group: String, hidden: Set<String>, includeAdult: Boolean): String {
        val db = helper.readableDatabase
        val (whereSql, args) = seriesFilter("t", group, "", hidden, includeAdult)
        val rawCount = db.rawQuery("SELECT COUNT(*) FROM $TABLE t WHERE $whereSql", args.toTypedArray()).use {
            if (it.moveToFirst()) it.getInt(0) else 0
        }
        val identityExpr = "LOWER(TRIM(CASE WHEN TRIM(t.series_group) <> '' THEN t.series_group ELSE t.name END))"
        val identityCount = db.rawQuery("SELECT COUNT(DISTINCT $identityExpr) FROM $TABLE t WHERE $whereSql", args.toTypedArray()).use {
            if (it.moveToFirst()) it.getInt(0) else 0
        }
        val samples = db.rawQuery(
            "SELECT $identityExpr AS ident, COUNT(*) AS n FROM $TABLE t WHERE $whereSql GROUP BY ident ORDER BY n DESC LIMIT 5",
            args.toTypedArray(),
        ).use { cursor ->
            buildList {
                while (cursor.moveToNext()) add("\"${cursor.getString(0)}\" (${cursor.getInt(1)} itens)")
            }
        }
        return "\"$group\": $rawCount episódio(s) bruto(s) → $identityCount identidade(s) distinta(s).\nTop identidades: ${samples.joinToString(" | ")}"
    }

    // "Mais recente" aqui é uma aproximação pela ordem de inserção (rowid),
    // já que o M3U/Xtream importado não carrega uma data real de quando o
    // item foi adicionado ao catálogo do provedor.
    fun byKey(key: String): CatalogEntry? = runCatching {
        helper.readableDatabase.rawQuery("SELECT * FROM $TABLE WHERE item_key=? LIMIT 1", arrayOf(key)).use { cursor ->
            if (cursor.moveToFirst()) readEntry(cursor) else null
        }
    }.getOrNull()

    fun mostRecent(kind: MediaKind, hidden: Set<String>): CatalogEntry? = runCatching {
        val db = helper.readableDatabase
        val args = mutableListOf(kind.name)
        val hiddenClause = hiddenClause(hidden, args)
        // Antes usava "ORDER BY rowid DESC" como aproximação de "recém
        // adicionado", mas a ordem do catálogo do provedor é essencialmente
        // fixa entre importações -- na prática, o item nunca mudava. Sorteia
        // um item aleatório em vez disso, pra garantir variedade real cada
        // vez que a Home é aberta.
        val sql = "SELECT * FROM $TABLE WHERE kind=? AND is_adult=0 $hiddenClause ORDER BY RANDOM() LIMIT 1"
        db.rawQuery(sql, args.toTypedArray()).use { cursor -> if (cursor.moveToFirst()) readEntry(cursor) else null }
    }.getOrNull()

    // Busca o item mais recente dentro de categorias cujo nome contenha um
    // dos termos dados (ex.: "lançamento", "animação"), usado nos cards de
    // destaque da tela inicial. Se nenhuma categoria bater, retorna null e
    // quem chamou decide o fallback (ex.: mostRecent() geral).
    fun mostRecentInGroups(kind: MediaKind, keywords: List<String>, hidden: Set<String>): CatalogEntry? {
        if (keywords.isEmpty()) return null
        return runCatching {
            val db = helper.readableDatabase
            val args = mutableListOf(kind.name)
            val hiddenClause = hiddenClause(hidden, args)
            val keywordClause = keywords.joinToString(" OR ") { "LOWER(group_title) LIKE ?" }
            keywords.forEach { args += "%${it.lowercase()}%" }
            val sql = "SELECT * FROM $TABLE WHERE kind=? AND is_adult=0 $hiddenClause AND ($keywordClause) ORDER BY RANDOM() LIMIT 1"
            db.rawQuery(sql, args.toTypedArray()).use { cursor -> if (cursor.moveToFirst()) readEntry(cursor) else null }
        }.getOrNull()
    }

    fun querySeriesSeasons(seriesGroup: String, group: String, hidden: Set<String>, includeAdult: Boolean = false): List<String> {
        val db = helper.readableDatabase
        val where = mutableListOf("kind=?", "LOWER(TRIM(series_group))=LOWER(TRIM(?))")
        val args = mutableListOf(MediaKind.SERIES.name, seriesGroup)
        // Não filtra por group_title aqui: um mesmo seriado pode ter
        // temporadas espalhadas em categorias diferentes no catálogo do
        // provedor (ex.: temporada nova numa categoria "Lançamentos" separada)
        // -- filtrar por categoria escondia temporadas inteiras.
        where += if (group == ContentSafety.LOCKED_CATEGORY) {
            if (includeAdult) "is_adult=1" else "0"
        } else if (!includeAdult) "is_adult=0" else "1=1"
        if (hidden.isNotEmpty()) where += "UPPER(group_title) NOT IN (${hidden.joinToString(",") { "?" }})".also { args.addAll(hidden.map(String::uppercase)) }
        val seasonExpr = "CASE WHEN TRIM(season) = '' THEN '1' ELSE season END"
        val sql = "SELECT DISTINCT $seasonExpr AS season_value FROM $TABLE WHERE ${where.joinToString(" AND ")} ORDER BY CAST($seasonExpr AS INTEGER), season_value"
        return db.rawQuery(sql, args.toTypedArray()).use { cursor ->
            buildList {
                while (cursor.moveToNext()) cursor.getString(0).orEmpty().takeIf { it.isNotBlank() }?.let { add(it) }
            }
        }
    }

    fun querySeriesEpisodes(seriesGroup: String, season: String, group: String, hidden: Set<String>, includeAdult: Boolean = false): List<CatalogEntry> {
        val db = helper.readableDatabase
        val where = mutableListOf("kind=?", "LOWER(TRIM(series_group))=LOWER(TRIM(?))")
        val args = mutableListOf(MediaKind.SERIES.name, seriesGroup)
        val seasonExpr = "CASE WHEN TRIM(season) = '' THEN '1' ELSE season END"
        where += "$seasonExpr=?"
        args += season
        // Mesmo motivo do querySeriesSeasons: não restringe por group_title.
        where += if (group == ContentSafety.LOCKED_CATEGORY) {
            if (includeAdult) "is_adult=1" else "0"
        } else if (!includeAdult) "is_adult=0" else "1=1"
        if (hidden.isNotEmpty()) where += "UPPER(group_title) NOT IN (${hidden.joinToString(",") { "?" }})".also { args.addAll(hidden.map(String::uppercase)) }
        val sql = "SELECT * FROM $TABLE WHERE ${where.joinToString(" AND ")} ORDER BY CAST(NULLIF(episode, '') AS INTEGER), name COLLATE NOCASE"
        return db.rawQuery(sql, args.toTypedArray()).use { cursor ->
            buildList {
                while (cursor.moveToNext()) add(readEntry(cursor))
            }
        }
    }

    fun updateMetadata(key: String, metadata: CatalogMetadata) {
        val values = ContentValues()
        if (metadata.synopsis.isNotBlank()) values.put("synopsis", metadata.synopsis)
        if (metadata.year.isNotBlank()) values.put("year", metadata.year)
        if (metadata.backdrop.isNotBlank()) values.put("backdrop_url", metadata.backdrop)
        if (metadata.trailer.isNotBlank()) values.put("trailer_url", metadata.trailer)
        if (values.size() <= 0) return
        // Escrita "best effort": se a importação em segundo plano estiver
        // segurando o único escritor do SQLite no momento (WAL permite leitura
        // concorrente, mas não duas escritas ao mesmo tempo), essa chamada pode
        // lançar SQLiteDatabaseLockedException. Perder um enriquecimento de
        // metadados não deve NUNCA derrubar o app -- por isso nunca propaga.
        val updated = runCatching { helper.writableDatabase.update(TABLE, values, "item_key=?", arrayOf(key)) }.getOrNull()
        if (updated == null) {
            // Uma segunda tentativa rápida cobre o caso comum de o lock durar
            // só o tempo de um commit de lote da importação.
            Thread.sleep(400)
            runCatching { helper.writableDatabase.update(TABLE, values, "item_key=?", arrayOf(key)) }
        }
    }

    fun close() = helper.close()

    private fun hiddenClause(hidden: Set<String>, args: MutableList<String>): String {
        if (hidden.isEmpty()) return ""
        args.addAll(hidden.map(String::uppercase))
        return "AND UPPER(group_title) NOT IN (${hidden.joinToString(",") { "?" }})"
    }

    private fun aggregateStats(db: SQLiteDatabase, knownTotal: Int? = null): Stats {
        val sql = "SELECT COUNT(*), SUM(CASE WHEN kind='LIVE' THEN 1 ELSE 0 END), SUM(CASE WHEN kind='MOVIE' THEN 1 ELSE 0 END), SUM(CASE WHEN kind='SERIES' THEN 1 ELSE 0 END), COUNT(DISTINCT group_title) FROM $TABLE"
        return db.rawQuery(sql, null).use { cursor ->
            if (!cursor.moveToFirst()) return@use Stats(knownTotal ?: 0, 0, 0, 0, 0)
            Stats(
                knownTotal ?: cursor.getInt(0),
                cursor.getInt(1),
                cursor.getInt(2),
                cursor.getInt(3),
                cursor.getInt(4),
            )
        }
    }

    private fun bind(statement: android.database.sqlite.SQLiteStatement, e: CatalogEntry) {
        statement.bindString(1, e.key)
        statement.bindString(2, e.name)
        statement.bindString(3, e.groupTitle)
        statement.bindString(4, e.tvgId)
        statement.bindString(5, e.logoUrl)
        statement.bindString(6, e.streamUrl)
        statement.bindString(7, e.kind.name)
        statement.bindString(8, e.quality)
        statement.bindString(9, e.seriesGroup)
        statement.bindString(10, e.season)
        statement.bindString(11, e.episode)
        statement.bindString(12, e.year)
        statement.bindString(13, e.synopsis)
        statement.bindString(14, e.cast)
        statement.bindString(15, e.backdropUrl)
        statement.bindString(16, e.trailerUrl)
        statement.bindString(17, e.runtime)
        statement.bindLong(18, if (ContentSafety.isAdult(e)) 1L else 0L)
    }

    private fun readEntry(c: android.database.Cursor): CatalogEntry = CatalogEntry(
        key = c.getString(c.getColumnIndexOrThrow("item_key")),
        name = c.getString(c.getColumnIndexOrThrow("name")),
        groupTitle = c.getString(c.getColumnIndexOrThrow("group_title")),
        tvgId = c.getString(c.getColumnIndexOrThrow("tvg_id")),
        logoUrl = c.getString(c.getColumnIndexOrThrow("logo_url")),
        streamUrl = c.getString(c.getColumnIndexOrThrow("stream_url")),
        kind = MediaKind.valueOf(c.getString(c.getColumnIndexOrThrow("kind"))),
        quality = c.getString(c.getColumnIndexOrThrow("quality")),
        seriesGroup = c.getString(c.getColumnIndexOrThrow("series_group")),
        season = c.getString(c.getColumnIndexOrThrow("season")),
        episode = c.getString(c.getColumnIndexOrThrow("episode")),
        year = c.getString(c.getColumnIndexOrThrow("year")),
        synopsis = c.getString(c.getColumnIndexOrThrow("synopsis")),
        cast = c.getString(c.getColumnIndexOrThrow("cast")),
        backdropUrl = c.getString(c.getColumnIndexOrThrow("backdrop_url")),
        trailerUrl = c.getString(c.getColumnIndexOrThrow("trailer_url")),
        runtime = c.getString(c.getColumnIndexOrThrow("runtime")),
    )

    private class Helper(context: Context) : SQLiteOpenHelper(context, "future_catalog.db", null, 3) {
        init {
            // WAL permite leitores (MainActivity, com sua própria conexão) lerem
            // o banco enquanto outra conexão (o importador, em ActivationActivity)
            // ainda está escrevendo em segundo plano. Sem isso, o leitor recebe
            // "database is locked" durante a importação de listas grandes, e o
            // código de consulta engolia esse erro devolvendo uma lista vazia --
            // dando a falsa impressão de "catálogo pronto" com a tela em branco.
            setWriteAheadLoggingEnabled(true)
        }

        override fun onConfigure(db: SQLiteDatabase) {
            super.onConfigure(db)
            // Antes esperava até 5s pra QUALQUER consulta que esbarrasse numa
            // trava breve -- isso incluía navegação normal durante a
            // importação, nao só o rebuild de índices (que é o único
            // momento que de fato PRECISA de uma espera longa). Reduzido pro
            // caso comum: uma consulta que esbarra numa trava agora falha
            // rápido, e o loop de nova tentativa (já existente,
            // ~1.2s de intervalo) assume -- ao invés de travar a tela por
            // até 5 segundos inteiros a cada tentativa.
            db.rawQuery("PRAGMA busy_timeout=500", null)?.use { it.moveToFirst() }
        }

        override fun onCreate(db: SQLiteDatabase) {
            db.execSQL("CREATE TABLE $TABLE (item_key TEXT PRIMARY KEY, name TEXT NOT NULL, group_title TEXT NOT NULL, tvg_id TEXT, logo_url TEXT, stream_url TEXT NOT NULL, kind TEXT NOT NULL, quality TEXT, series_group TEXT, season TEXT, episode TEXT, year TEXT, synopsis TEXT, cast TEXT, backdrop_url TEXT, trailer_url TEXT, runtime TEXT, is_adult INTEGER NOT NULL DEFAULT 0)")
            db.execSQL("CREATE INDEX idx_catalog_kind_group ON $TABLE(kind, group_title)")
            db.execSQL("CREATE INDEX idx_catalog_name ON $TABLE(name COLLATE NOCASE)")
            db.execSQL("CREATE INDEX idx_catalog_series_season ON $TABLE(kind, series_group, season)")
        }
        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
            if (oldVersion < 2) db.execSQL("CREATE INDEX IF NOT EXISTS idx_catalog_series_season ON $TABLE(kind, series_group, season)")
            if (oldVersion < 3) {
                db.execSQL("ALTER TABLE $TABLE ADD COLUMN is_adult INTEGER NOT NULL DEFAULT 0")
                ContentSafety.migrationTerms().forEach { term ->
                    db.execSQL(
                        "UPDATE $TABLE SET is_adult=1 WHERE LOWER(group_title || ' ' || name) LIKE ?",
                        arrayOf("%$term%"),
                    )
                }
            }
        }
    }

    private companion object { const val TABLE = "catalog_items" }
}
