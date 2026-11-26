package com.coffeepeek.admin.ui

import com.coffeepeek.admin.ui.Navigator.isHandledByRootNav

internal data class NativeRootEntry(val key: Long, val screen: Navigator.Screen)
internal data class NativeRootStacks(val selected: Int, val stacks: List<List<NativeRootEntry>>)

/** Kotlin routing policy; UIKit confirms the stack actually shown, including cancelled swipes. */
internal class NativeRootNavigationModel(private var loggedIn: Boolean, businessMode: Boolean = false) {
    private var nextKey = 0L
    private val roots = listOf(Navigator.Screen.FeedTab, Navigator.Screen.CommunityTab, Navigator.Screen.ProfileTab,
        Navigator.Screen.SettingsTab, Navigator.Screen.BusinessTab).map(::entry)
    var visibleTabs = tabs(businessMode)
        private set
    var actual = NativeRootStacks(visibleTabs.first(), roots.map(::listOf))
        private set

    fun configure(loggedIn: Boolean, businessMode: Boolean): NativeRootStacks {
        this.loggedIn = loggedIn
        val next = tabs(businessMode)
        if (next != visibleTabs) {
            visibleTabs = next
            actual = NativeRootStacks(next.first(), roots.map(::listOf))
        }
        return actual
    }
    private fun tabs(businessMode: Boolean) = mainTabScreens(loggedIn, businessMode).map { checkNotNull(nativeTabIndex(it)) }
    private fun entry(screen: Navigator.Screen) = NativeRootEntry(++nextKey, screen)

    fun plan(event: Navigator.NavEvent): NativeRootStacks = when (event) {
        Navigator.NavEvent.PopBack, Navigator.NavEvent.NavigateUp -> withStack(actual.stacks[actual.selected].let {
            if (it.size > 1) it.dropLast(1) else it
        })
        is Navigator.NavEvent.SelectTab -> select(event.tab, reset = true)
        Navigator.NavEvent.ReturnToProfile -> returnToProfile()
        is Navigator.NavEvent.NavigateTo -> navigate(event.screen.profileDestination(loggedIn))
    }

    fun planLink(screen: Navigator.Screen): NativeRootStacks {
        val target = screen.appLinkDestination(loggedIn)
        if (target == Navigator.Screen.Main) return withStack(listOf(roots[actual.selected]))
        return if (nativeTabIndex(target) != null) select(target, reset = true)
        else withStack(listOf(roots[actual.selected], entry(target)))
    }

    private fun navigate(screen: Navigator.Screen): NativeRootStacks {
        if (screen is Navigator.Screen.ResetPassword) return navigate(Navigator.Screen.Auth)
        if (screen == Navigator.Screen.Main) return withStack(listOf(roots[actual.selected]))
        if (nativeTabIndex(screen) != null) return select(screen, reset = false)
        if (!screen.isHandledByRootNav()) return actual
        val stack = actual.stacks[actual.selected]
        val profileIndex = existingProfileIndex(stack.map { it.screen }, screen)
        return withStack(when {
            replacesAuthForm(stack.last().screen, screen) -> stack.dropLast(1) + entry(screen)
            profileIndex >= 0 -> stack.take(profileIndex + 1)
            else -> stack + entry(screen)
        })
    }

    private fun select(screen: Navigator.Screen, reset: Boolean): NativeRootStacks {
        val index = nativeTabIndex(screen)?.takeIf { it in visibleTabs } ?: return actual
        val stacks = if (reset) actual.stacks.mapIndexed { position, stack -> if (position == index) stack.take(1) else stack }
            else actual.stacks
        return actual.copy(selected = index, stacks = stacks)
    }

    private fun returnToProfile(): NativeRootStacks {
        val profile = select(Navigator.Screen.ProfileTab, reset = true)
        if (profile.selected != nativeTabIndex(Navigator.Screen.ProfileTab)) return actual
        return profile.copy(stacks = profile.stacks.mapIndexed { index, stack ->
            if (index == actual.selected) stack.dropLastWhile { it.screen.isCollectionEditorOverlay() } else stack
        })
    }

    private fun Navigator.Screen.isCollectionEditorOverlay(): Boolean = this is Navigator.Screen.CollectionEditor ||
        this == Navigator.Screen.CollectionsLibrary || this == Navigator.Screen.CollectionsComposer
    private fun withStack(stack: List<NativeRootEntry>) = actual.copy(stacks = actual.stacks.mapIndexed { index, previous ->
        if (index == actual.selected) stack else previous
    })

    /** A cancelled interactive transition reports its old stack; no speculative pop is committed. */
    fun confirm(next: NativeRootStacks) {
        require(next.selected in visibleTabs && next.stacks.size == roots.size)
        require(next.stacks.indices.all { next.stacks[it].firstOrNull() == roots[it] })
        actual = next
    }
}
