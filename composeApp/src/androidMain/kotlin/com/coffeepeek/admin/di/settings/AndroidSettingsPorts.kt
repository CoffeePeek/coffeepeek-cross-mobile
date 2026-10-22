package com.coffeepeek.admin.di.settings

import com.coffeepeek.admin.analytics.Analytics
import com.coffeepeek.admin.analytics.AnalyticsConsent
import com.coffeepeek.admin.analytics.AnalyticsService
import com.coffeepeek.admin.feature.appupdate.ui.AppUpdateState
import com.coffeepeek.admin.legal.LegalUrls
import com.coffeepeek.admin.settings.CityPreference
import com.coffeepeek.admin.theme.ThemeManager
import com.coffeepeek.admin.utils.COFFEEPEEK_SHARE_TEXT
import com.coffeepeek.admin.utils.OpenInBrowser
import com.coffeepeek.admin.utils.ShareHelper
import com.coffeepeek.admin.utils.appReviewUrl
import com.coffeepeek.admin.config.AppConfig
import com.coffeepeek.domain.repository.SessionRepository
import com.coffeepeek.domain.repository.ShopRepository
import com.coffeepeek.feature.settings.api.CityPort
import com.coffeepeek.feature.settings.api.SettingsAccountPort
import com.coffeepeek.feature.settings.api.SettingsExternalActions
import com.coffeepeek.feature.settings.api.ThemePort
import com.coffeepeek.feature.settings.api.UpdateCheckPort
import com.coffeepeek.feature.settings.api.UsageConsentPort
import com.coffeepeek.feature.settings.domain.SettingsCity
import com.coffeepeek.feature.settings.domain.UpdateCheckState
import com.coffeepeek.feature.settings.domain.UsageConsentState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import com.coffeepeek.admin.theme.ThemeMode as LegacyThemeMode
import com.coffeepeek.feature.settings.domain.ThemeMode as SettingsThemeMode

/** Adapter over the process-wide [ThemeManager], which the app root and the iOS shell also read. */
internal class AndroidThemePort : ThemePort {
    override fun current() = ThemeManager.themeMode.value.toSettings()
    override fun observe(): Flow<SettingsThemeMode> = ThemeManager.themeMode.map { it.toSettings() }
    override fun set(mode: SettingsThemeMode) = ThemeManager.setTheme(mode.toLegacy())
}

internal fun LegacyThemeMode.toSettings() = when (this) {
    LegacyThemeMode.SYSTEM -> SettingsThemeMode.SYSTEM
    LegacyThemeMode.LIGHT -> SettingsThemeMode.LIGHT
    LegacyThemeMode.DARK -> SettingsThemeMode.DARK
}

internal fun SettingsThemeMode.toLegacy() = when (this) {
    SettingsThemeMode.SYSTEM -> LegacyThemeMode.SYSTEM
    SettingsThemeMode.LIGHT -> LegacyThemeMode.LIGHT
    SettingsThemeMode.DARK -> LegacyThemeMode.DARK
}

/** The legacy order: load the catalog, then resolve the saved city against it (only on success). */
internal class AndroidCityPort(
    private val shops: ShopRepository,
    private val preference: CityPreference,
) : CityPort {
    override fun observeSelectedCityId(): Flow<String?> = preference.selectedCityId

    override suspend fun loadCities(): Result<List<SettingsCity>> = shops.getCatalogs().map { catalogs ->
        preference.resolve(catalogs.cities)
        catalogs.cities.map { SettingsCity(it.id, it.name) }
    }

    override fun select(cityId: String) = preference.select(cityId)
}

/** Signed in = an active session that has a user id, exactly what the legacy settings state used. */
internal class AndroidSettingsAccountPort(private val sessions: SessionRepository) : SettingsAccountPort {
    override fun isSignedIn(): Boolean = signedIn(sessions.peekSession())
    override fun observeSignedIn(): Flow<Boolean> = sessions.observeSession().map(::signedIn).distinctUntilChanged()
    private fun signedIn(session: com.coffeepeek.domain.model.Session?) = sessions.isActiveSession(session) && session?.userId != null
}

/** The row exists only for a signed-in user with analytics ready; the service is read when observed, as the legacy screen did. */
internal class AndroidUsageConsentPort(private val service: () -> AnalyticsService? = { Analytics.service }) : UsageConsentPort {
    override fun observe(): Flow<UsageConsentState> = service()?.state
        ?.map { UsageConsentState(available = it.authReady && !it.userId.isNullOrBlank(), accepted = it.consent == AnalyticsConsent.Accepted) }
        ?.distinctUntilChanged()
        ?: flowOf(UsageConsentState.Unavailable)

    override suspend fun set(accepted: Boolean) {
        service()?.setConsent(if (accepted) AnalyticsConsent.Accepted else AnalyticsConsent.Declined)
    }
}

internal class AndroidUpdateCheckPort(private val updates: AppUpdateState) : UpdateCheckPort {
    override fun observe(): Flow<UpdateCheckState> = updates.state
        .map { UpdateCheckState(checking = it.checking, message = it.message, updateAvailable = it.update != null) }
        .distinctUntilChanged()

    override fun check() = updates.check(manual = true)
}

internal class AndroidSettingsExternalActions(
    private val openUrl: (String) -> Unit = { OpenInBrowser.openInBrowser(it) },
    private val shareText: (String) -> Unit = { ShareHelper.shareText(it) },
    private val reviewUrl: String? = appReviewUrl(AppConfig.updatePlatform, AppConfig.updateChannel),
) : SettingsExternalActions {
    override val canRateApp: Boolean = reviewUrl != null
    override fun openSupport() = openUrl(SUPPORT_URL)
    override fun openTerms() = openUrl(LegalUrls.TERMS)
    override fun openPrivacy() = openUrl(LegalUrls.PRIVACY)
    override fun shareApp() = shareText(COFFEEPEEK_SHARE_TEXT)
    override fun rateApp() { reviewUrl?.let(openUrl) }

    internal companion object { const val SUPPORT_URL = "https://coffeepeek.by/support" }
}
