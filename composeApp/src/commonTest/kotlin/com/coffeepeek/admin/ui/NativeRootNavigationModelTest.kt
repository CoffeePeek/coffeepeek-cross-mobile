package com.coffeepeek.admin.ui

import kotlin.test.*

class NativeRootNavigationModelTest {
    private fun profileReturn() = Navigator.NavEvent.ReturnToProfile(
        RootNavigationOrigin(RootNavigationSession(true, "owner", 1)))

    @Test fun savedCollectionReturnsFromTheLibraryToTheProfileRoot() {
        val model = NativeRootNavigationModel(true)
        model.confirm(model.plan(Navigator.NavEvent.SelectTab(Navigator.Screen.ProfileTab)))
        val profile = model.actual.stacks[2].first()
        model.confirm(model.plan(Navigator.NavEvent.NavigateTo(Navigator.Screen.CollectionsLibrary)))
        model.confirm(model.plan(Navigator.NavEvent.NavigateTo(Navigator.Screen.CollectionEditor())))
        model.confirm(model.plan(profileReturn()))
        assertEquals(2, model.actual.selected)
        assertEquals(listOf(profile), model.actual.stacks[2])
        model.confirm(model.plan(Navigator.NavEvent.PopBack))
        assertEquals(listOf(profile), model.actual.stacks[2])
    }

    @Test fun savedCollectionFromAnotherTabCannotReappearWhenThatTabIsReopened() {
        val model = NativeRootNavigationModel(true)
        model.confirm(model.plan(Navigator.NavEvent.SelectTab(Navigator.Screen.ProfileTab)))
        model.confirm(model.plan(Navigator.NavEvent.NavigateTo(Navigator.Screen.CollectionsLibrary)))
        model.confirm(model.plan(Navigator.NavEvent.NavigateTo(Navigator.Screen.FeedTab)))
        model.confirm(model.plan(Navigator.NavEvent.NavigateTo(Navigator.Screen.UserProfile("own"))))
        val source = model.actual.stacks[0]
        model.confirm(model.plan(Navigator.NavEvent.NavigateTo(Navigator.Screen.CollectionsLibrary)))
        model.confirm(model.plan(Navigator.NavEvent.NavigateTo(Navigator.Screen.CollectionEditor("edited"))))
        model.confirm(model.plan(profileReturn()))
        assertEquals(2, model.actual.selected)
        assertEquals(1, model.actual.stacks[2].size)
        model.confirm(model.plan(Navigator.NavEvent.NavigateTo(Navigator.Screen.FeedTab)))
        assertEquals(source, model.actual.stacks[0])
    }

    @Test fun savedCollectionFromTheProfilePreviewClearsTheEditorWithoutALibrary() {
        val model = NativeRootNavigationModel(true)
        model.confirm(model.plan(Navigator.NavEvent.SelectTab(Navigator.Screen.ProfileTab)))
        model.confirm(model.plan(Navigator.NavEvent.NavigateTo(Navigator.Screen.CollectionEditor("edited"))))
        model.confirm(model.plan(profileReturn()))
        assertEquals(listOf(Navigator.Screen.ProfileTab), model.actual.stacks[2].map { it.screen })
    }

    @Test fun authFormsReplaceOneAnotherAndPreserveTheEntryScreen() {
        val model = NativeRootNavigationModel(false)
        model.confirm(model.plan(Navigator.NavEvent.NavigateTo(Navigator.Screen.ShopDetail("shop"))))
        model.confirm(model.plan(Navigator.NavEvent.NavigateTo(Navigator.Screen.Auth)))
        model.confirm(model.plan(Navigator.NavEvent.NavigateTo(Navigator.Screen.Register)))
        assertEquals(listOf(Navigator.Screen.FeedTab, Navigator.Screen.ShopDetail("shop"), Navigator.Screen.Register),
            model.actual.stacks[0].map { it.screen })
    }
    @Test fun reopeningAnExistingProfileReturnsToItsOriginalEntryIdentity() {
        val model = NativeRootNavigationModel(true)
        model.confirm(model.plan(Navigator.NavEvent.NavigateTo(Navigator.Screen.UserProfile("person", "Before"))))
        val profile = model.actual.stacks[0].last()
        model.confirm(model.plan(Navigator.NavEvent.NavigateTo(Navigator.Screen.ShopDetail("shop"))))
        model.confirm(model.plan(Navigator.NavEvent.NavigateTo(Navigator.Screen.UserProfile("person", "After"))))
        assertEquals(profile, model.actual.stacks[0].last())
        assertEquals(2, model.actual.stacks[0].size)
    }
    @Test fun aCancelledNativePopKeepsTheOriginalStackUntilUIKitConfirmsTheResult() {
        val model = NativeRootNavigationModel(true)
        model.confirm(model.plan(Navigator.NavEvent.NavigateTo(Navigator.Screen.ShopDetail("shop"))))
        val original = model.actual
        assertEquals(1, model.plan(Navigator.NavEvent.PopBack).stacks[0].size)
        assertEquals(original, model.actual)
        model.confirm(original)
        assertEquals(original, model.actual)
    }
    @Test fun selectingATabRootClearsOnlyItsStackWhereasARegularTabTapRetainsIt() {
        val model = NativeRootNavigationModel(true)
        model.confirm(model.plan(Navigator.NavEvent.NavigateTo(Navigator.Screen.ShopDetail("shop"))))
        model.confirm(model.plan(Navigator.NavEvent.NavigateTo(Navigator.Screen.ProfileTab)))
        model.confirm(model.plan(Navigator.NavEvent.NavigateTo(Navigator.Screen.FeedTab)))
        assertEquals(2, model.actual.stacks[0].size)
        model.confirm(model.plan(Navigator.NavEvent.SelectTab(Navigator.Screen.FeedTab)))
        assertEquals(1, model.actual.stacks[0].size)
    }
    @Test fun appLinksReplaceTheCurrentInnerStackAndRespectGuestProfileGating() {
        val model = NativeRootNavigationModel(false)
        model.confirm(model.plan(Navigator.NavEvent.NavigateTo(Navigator.Screen.ShopDetail("shop"))))
        model.confirm(model.planLink(Navigator.Screen.UserProfile("person")))
        assertEquals(listOf(Navigator.Screen.FeedTab, Navigator.Screen.Auth), model.actual.stacks[0].map { it.screen })
    }
    @Test fun businessModeUsesTheExistingTabPolicyAndCannotSelectAHiddenTab() {
        val model = NativeRootNavigationModel(true)
        model.configure(true, true)
        assertEquals(listOf(4, 3), model.visibleTabs)
        assertEquals(4, model.actual.selected)
        assertEquals(model.actual, model.plan(Navigator.NavEvent.SelectTab(Navigator.Screen.FeedTab)))
    }
    @Test fun aRootOrUnknownWebLinkReturnsToTheSelectedRootWithoutPushingMain() {
        val model = NativeRootNavigationModel(false)
        model.confirm(model.plan(Navigator.NavEvent.NavigateTo(Navigator.Screen.ShopDetail("shop"))))
        val root = model.actual.stacks[0].first()
        model.confirm(model.planLink(Navigator.Screen.Main))
        assertEquals(listOf(root), model.actual.stacks[0])
    }
    @Test fun aRestoredRetiredPasswordRouteOpensTheNativeLoginForm() {
        val model = NativeRootNavigationModel(false)
        model.confirm(model.plan(Navigator.NavEvent.NavigateTo(Navigator.Screen.ResetPassword("retired"))))
        assertEquals(Navigator.Screen.Auth, model.actual.stacks[0].last().screen)
    }
}
