package com.coffeepeek.feature.collections.domain.model

import kotlin.test.*

class SavedCollectionRulesTest {
    @Test fun newListDefaultsToPrivateAndMayStartWithNoPlaces() {
        val metadata = SavedCollectionMetadata("New list")
        assertEquals(SavedCollectionVisibility.Private, metadata.visibility)
        assertTrue(SavedCollectionRules.validMetadata(metadata))
        assertTrue(SavedCollection(SavedCollectionSummary("id", metadata.title)).places.isEmpty())
    }

    @Test fun multilineDescriptionIsAllowedWhileOtherControlsAndExcessLengthAreRejected() {
        assertTrue(SavedCollectionRules.validMetadata(SavedCollectionMetadata("Title", "First\nSecond\tThird")))
        assertFalse(SavedCollectionRules.validMetadata(SavedCollectionMetadata("Title", "Bad\u0000text")))
        assertFalse(SavedCollectionRules.validMetadata(SavedCollectionMetadata("Title", "x".repeat(1001))))
    }

    @Test fun onlyBoundedSupportedCoverUploadsAreAccepted() {
        val valid = CollectionCoverInput(byteArrayOf(1), "image/jpeg", "cover.jpg")
        assertTrue(SavedCollectionRules.validCover(valid))
        assertFalse(SavedCollectionRules.validCover(valid.copy(mimeType = "image/svg+xml")))
        assertFalse(SavedCollectionRules.validCover(valid.copy(bytes = ByteArray(0))))
        assertFalse(SavedCollectionRules.validCover(valid.copy(bytes = ByteArray(1_048_577))))
    }
}
