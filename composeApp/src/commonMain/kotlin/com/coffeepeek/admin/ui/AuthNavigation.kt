package com.coffeepeek.admin.ui

/** Native auth links replace the current form while preserving its entry route. */
internal fun replacesAuthForm(current: Navigator.Screen?, destination: Navigator.Screen): Boolean {
    val forms = setOf(Navigator.Screen.Auth, Navigator.Screen.Register)
    return current != destination && current in forms && destination in forms
}
