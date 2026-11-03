package com.coffeepeek.admin.session

import com.coffeepeek.room.repository.SettingRepository

/** Review drafts saved by earlier app versions sit in the settings table under this prefix; sign-out removes them. */
internal class OldReviewDrafts(private val settings: SettingRepository) {
    suspend fun clear() {
        settings.readAll().filter { it.key.startsWith(KEY_PREFIX) }.forEach { settings.delete(it.key) }
    }

    private companion object {
        const val KEY_PREFIX = "review_draft:"
    }
}
