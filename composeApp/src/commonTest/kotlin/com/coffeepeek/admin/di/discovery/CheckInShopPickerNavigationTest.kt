package com.coffeepeek.admin.di.discovery

import com.coffeepeek.admin.di.shop.CheckInFlowHandoff
import com.coffeepeek.admin.ui.Navigator
import kotlin.test.*

class CheckInShopPickerNavigationTest {
    private val opened = mutableListOf<Navigator.Screen.ShopCheckInEditorFromPicker>()
    private var closed = 0
    private var current = true
    private val navigation = CheckInShopPickerNavigation({ current }, opened::add) { closed++ }

    @AfterTest fun tearDown() { CheckInFlowHandoff.tracker = null }

    @Test fun selectionIsDeliveredOnceAndThenCloseIsIgnored() {
        navigation.select("coffee")
        navigation.select("another")
        navigation.close(back = true)
        assertEquals(listOf(Navigator.Screen.ShopCheckInEditorFromPicker("coffee")), opened)
        assertEquals(0, closed)
    }

    @Test fun emptySelectionDoesNotConsumeTheDestination() {
        navigation.select(" ")
        navigation.select("coffee")
        assertEquals(listOf(Navigator.Screen.ShopCheckInEditorFromPicker("coffee")), opened)
    }

    @Test fun explicitCloseAndBackAreTerminal() {
        navigation.close(back = false)
        navigation.close(back = true)
        navigation.select("coffee")
        assertEquals(1, closed)
        assertTrue(opened.isEmpty())
    }

    @Test fun backIsAlsoTerminal() {
        navigation.close(back = true)
        navigation.select("coffee")
        assertEquals(1, closed)
        assertTrue(opened.isEmpty())
    }

    @Test fun replacedSessionCannotSelectOrClose() {
        current = false
        navigation.select("coffee")
        navigation.close(back = true)
        assertTrue(opened.isEmpty())
        assertEquals(0, closed)
    }

    @Test fun disposedCallbackCannotSelectOrClose() {
        navigation.dispose()
        navigation.select("coffee")
        navigation.close(back = false)
        assertTrue(opened.isEmpty())
        assertEquals(0, closed)
    }
}
