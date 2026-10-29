package com.coffeepeek.core.designsystem.component

/** UI delivery sources only: absence falls back, an explicit blank variant stays unavailable. No image IO. */
class CoffeePeekPhotoStripSources(originals: List<String>, previews: List<String>? = null, details: List<String>? = null) {
    val originals: List<String> = originals.toList()
    val previews: List<String> = this.originals.indices.map { previews?.getOrNull(it) ?: this.originals[it] }
    val details: List<String> = this.originals.indices.map { details?.getOrNull(it) ?: this.originals[it] }

    fun display(index: Int): String = previews[index].ifBlank { details[index] }
    fun upgrade(index: Int): String? = details[index].takeIf { it.isNotBlank() && it != display(index) }
    fun canOpen(index: Int): Boolean = index in originals.indices && originals.all(String::isNotBlank)
}
