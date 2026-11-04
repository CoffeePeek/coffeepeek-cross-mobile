package com.coffeepeek.admin.utils

const val COFFEEPEEK_SHARE_TEXT =
    "Делюсь отличным приложением, которое объединяет кофейни и любителей кофе — загляни: https://coffeepeek.by"

expect object ShareHelper {
    fun shareText(text: String)
    fun shareImages(images: List<ByteArray>, text: String)
}
