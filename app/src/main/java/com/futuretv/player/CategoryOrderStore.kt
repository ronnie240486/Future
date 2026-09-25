package com.futuretv.player

import android.content.Context

/**
 * Guarda a ordem que o usuário escolheu pra cada categoria, uma lista por
 * seção (Canais/Filmes/Séries) -- recurso pedido explicitamente, no mesmo
 * espírito do CategoryOrderStore já usado no Rencia/Supreme: alguns
 * provedores organizam a playlist numa ordem que não é a que a pessoa
 * prefere ver, e em vez de tentar adivinhar uma ordem "certa", dá o
 * controle direto pra ela. Sem ordem salva, mantém o comportamento padrão
 * (ordem alfabética, igual sempre foi).
 *
 * Diferença pro Rencia/Supreme (que só tem Canais): aqui a ordem é
 * guardada separadamente por MediaKind, porque Filmes/Séries têm
 * categorias completamente diferentes de Canais.
 */
object CategoryOrderStore {
    private const val PREFS = "future_category_order"
    private const val SEPARATOR = "\u0001"

    private fun key(kind: MediaKind) = "order_${kind.name}"

    fun saveOrder(context: Context, kind: MediaKind, orderedCategoryNames: List<String>) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(key(kind), orderedCategoryNames.joinToString(SEPARATOR))
            .apply()
    }

    fun readOrder(context: Context, kind: MediaKind): List<String> {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(key(kind), null) ?: return emptyList()
        return raw.split(SEPARATOR).filter { it.isNotEmpty() }
    }

    fun hasCustomOrder(context: Context, kind: MediaKind): Boolean = readOrder(context, kind).isNotEmpty()

    fun clearOrder(context: Context, kind: MediaKind) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().remove(key(kind)).apply()
    }

    /**
     * Aplica a ordem salva (se tiver) por cima da lista recebida (que já
     * vem na ordem natural/alfabética) -- categorias com posição salva vêm
     * primeiro, na ordem escolhida; qualquer categoria NOVA (que não
     * existia quando a ordem foi salva, ex.: painel adicionou categoria
     * depois) entra no final, na ordem natural recebida. "Todos", a aba de
     * Favoritos e a categoria bloqueada (conteúdo adulto) NÃO passam por
     * aqui -- são adicionadas pelo chamador antes/depois e ficam sempre
     * fixas onde já estavam.
     */
    fun applyOrder(context: Context, kind: MediaKind, naturalOrderCategories: List<String>): List<String> {
        val savedOrder = readOrder(context, kind)
        if (savedOrder.isEmpty()) return naturalOrderCategories
        val ordered = savedOrder.filter { it in naturalOrderCategories }
        val remaining = naturalOrderCategories.filterNot { it in savedOrder }
        return ordered + remaining
    }
}
