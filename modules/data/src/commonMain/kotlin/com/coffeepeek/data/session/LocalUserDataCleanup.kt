package com.coffeepeek.data.session

/** What the legacy sign-in does when another account takes over; the application supplies it. */
fun interface LocalUserDataCleanup {
    suspend fun clear()
}
