package com.coffeepeek.admin.ui

/** Display metadata may change while the same public address remains in the stack. */
internal fun existingProfileIndex(stack: List<Navigator.Screen?>, destination: Navigator.Screen): Int {
    if (destination !is Navigator.Screen.UserProfile || destination.slug.isBlank()) return -1
    return stack.indexOfLast { it is Navigator.Screen.UserProfile && it.slug == destination.slug }
}
