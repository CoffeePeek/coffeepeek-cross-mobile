package com.coffeepeek.feature.homecheckin.impl.ui

import com.coffeepeek.feature.homecheckin.domain.model.HomePreparationIssue
import com.coffeepeek.feature.homecheckin.domain.model.HomePreparationPhoto
import com.coffeepeek.feature.homecheckin.impl.ui.compose.*
import com.coffeepeek.feature.homecheckin.impl.ui.compose.model.*
import kotlin.test.*

class NativeHomeDetailPresentationTest {
    @Test fun menuDeleteOnlyRequestsConfirmationAndEditKeepsItsExistingIntent() {
        val sent = mutableListOf<HomeCheckInDetailAction>()
        val state = HomeCheckInDetailState(entry = previewHomePreparation())
        val actions = nativeHomeDetailActions(state, sent::add).actions
        actions.getValue("edit")()
        actions.getValue("delete")()
        assertEquals(listOf<HomeCheckInDetailAction>(HomeCheckInDetailAction.Edit, HomeCheckInDetailAction.RequestDelete), sent)
        assertFalse("confirmDelete" in actions)
    }

    @Test fun unavailableInlinePhotoDoesNotExposeOriginalOrDetailFallback() {
        val photo = nativeHomeDetailPhoto(HomePreparationPhoto("photo", "original", thumbnailUrl = "", detailUrl = "detail"), "photo")
        assertEquals("", photo.previewUrl)
        assertNull(photo.detailUrl)
    }

    @Test fun retryAfterFailedDeleteReloadsInsteadOfRepeatingDelete() {
        val sent = mutableListOf<HomeCheckInDetailAction>()
        val state = HomeCheckInDetailState(entry = previewHomePreparation(), deleteIssue = HomePreparationIssue.Offline)
        nativeHomeDetailActions(state, sent::add).actions.getValue("retry")()
        assertEquals(listOf<HomeCheckInDetailAction>(HomeCheckInDetailAction.Reload), sent)
    }

    @Test fun confirmationBackCancelsAndDoesNotEditOrDeleteWithoutConfirmation() {
        val sent = mutableListOf<HomeCheckInDetailAction>()
        val state = HomeCheckInDetailState(entry = previewHomePreparation(), showDeleteConfirmation = true)
        val actions = nativeHomeDetailActions(state, sent::add).actions
        assertFalse("edit" in actions)
        assertFalse("delete" in actions)
        actions.getValue("back")()
        assertEquals(listOf<HomeCheckInDetailAction>(HomeCheckInDetailAction.CancelDelete), sent)
    }

    @Test fun deletionInFlightAndAccessRevocationKeepPrivateActionsDisabled() {
        val deleting = HomeCheckInDetailState(entry = previewHomePreparation(), isDeleting = true)
        assertTrue(nativeHomeDetailActions(deleting, { error("Unexpected action") }).actions.isEmpty())
        var closed = false
        val revoked = HomeCheckInDetailState(isAccessRevoked = true)
        val actions = nativeHomeDetailActions(revoked, { error("Private action after revocation") }, { closed = true })
        actions.actions.getValue("back")()
        assertTrue(closed)
        assertEquals(setOf("back"), actions.actions.keys)
    }
}
