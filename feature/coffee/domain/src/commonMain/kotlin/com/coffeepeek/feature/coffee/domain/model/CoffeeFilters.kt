package com.coffeepeek.feature.coffee.domain.model

data class CoffeeFilters(
    val values: Map<String, Set<String>> = emptyMap(),
    val availableOnly: Boolean = false,
) {
    val activeCount: Int get() = values.values.sumOf { it.size } + if (availableOnly) 1 else 0

    fun toggle(group: String, code: String): CoffeeFilters {
        val selected = values[group].orEmpty()
        val next = if (code in selected) selected - code else selected + code
        return copy(values = if (next.isEmpty()) values - group else values + (group to next))
    }
}
