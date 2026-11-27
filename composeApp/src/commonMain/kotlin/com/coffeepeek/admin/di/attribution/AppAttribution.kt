package com.coffeepeek.admin.di.attribution

import androidx.compose.runtime.*
import com.coffeepeek.admin.ui.Navigator
import com.coffeepeek.admin.ui.appLinkScreen
import com.coffeepeek.admin.utils.ShareHelper
import com.coffeepeek.feature.attribution.api.AttributionFeature
import com.coffeepeek.feature.attribution.domain.validAttributionCode
import io.ktor.http.Url
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import com.coffeepeek.core.session.SessionReader
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.flow.MutableStateFlow

internal object AppAttribution {
    var feature: AttributionFeature? = null
    var actionScope: CoroutineScope? = null
    var sessions: SessionReader? = null
    val pending = MutableStateFlow<String?>(null)

    fun open(link: String): Boolean {
        val url = runCatching { Url(link) }.getOrNull() ?: return false
        if (url.protocol.name !in setOf("http", "https") || !url.host.equals("coffeepeek.by", true)) return false
        pending.value = link
        return true
    }

    suspend fun visible(path: String?) {
        if (path == null) return
        try { feature?.visible(path) }
        catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) { /* Attribution must not fail a loaded destination. */ }
    }

    fun shareAction(path: String, placement: String, title: String = "") {
        dispatchShare(path, placement) { if (title.isBlank()) it else "$title — $it" }
    }

    private fun dispatchShare(path: String, placement: String, render: (String) -> String) {
        val generation = sessions?.accountGeneration()
        val scope = actionScope ?: return ShareHelper.shareText(render("https://coffeepeek.by$path"))
        scope.launch {
            if (sessions?.accountGeneration() != generation) return@launch
            val url = withTimeoutOrNull(3_000) { feature?.share(path, placement)?.getOrNull() } ?: "https://coffeepeek.by$path"
            if (sessions?.accountGeneration() == generation) ShareHelper.shareText(render(url))
        }
    }

    fun shareTextAction(text: String, placement: String) {
        val url = Regex("https://coffeepeek\\.by(/[^\\s?#]*)?(?=[\\s?#]|$)").find(text) ?: return ShareHelper.shareText(text)
        dispatchShare(url.groupValues[1].ifEmpty { "/" }, placement) { text.replaceRange(url.range, it) }
    }
}

@Composable
internal fun AttributionAppLinks() {
    val link by AppAttribution.pending.collectAsState()
    val value = link ?: return
    val url = Url(value)
    val code = if (url.segments.size == 2 && url.segments[0] == "l") url.segments[1] else url.parameters.getAll("cp_link")?.singleOrNull()
    if (code != null) {
        com.coffeepeek.feature.attribution.impl.AttributionLinkResolver(code, AppAttribution.feature,
            onResolved = { path -> openResolvedLink(path, code); AppAttribution.pending.compareAndSet(value, null) },
            onDismiss = { AppAttribution.pending.compareAndSet(value, null) })
        return
    }
    LaunchedEffect(link) {
        appLinkScreen(value)?.let(Navigator::openAppLink)
        AppAttribution.pending.compareAndSet(value, null)
    }
}

private fun openResolvedLink(path: String, code: String) {
    if (path in setOf("/coffee-shops", "/roasters", "/coffees")) {
        com.coffeepeek.admin.utils.OpenInBrowser.openInBrowser("https://coffeepeek.by$path?cp_link=$code")
    } else appLinkScreen("https://coffeepeek.by$path")?.let(Navigator::openAppLink)
}

@Composable
internal fun AttributionLoaded(path: String?, loaded: Boolean) {
    val lifecycle by androidx.lifecycle.compose.LocalLifecycleOwner.current.lifecycle.currentStateFlow.collectAsState()
    val visible = com.coffeepeek.admin.analytics.LocalAnalyticsVisible.current
    val ready = com.coffeepeek.admin.analytics.Analytics.service?.state?.collectAsState()?.value?.ready
    LaunchedEffect(path, loaded, lifecycle, visible, ready) {
        if (loaded && visible && lifecycle == androidx.lifecycle.Lifecycle.State.RESUMED) AppAttribution.visible(path)
    }
}
