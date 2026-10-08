package com.coffeepeek.admin.ui.screen.profile

import coffeepeek.composeapp.generated.resources.Res
import coffeepeek.composeapp.generated.resources.local_data_cleanup_error
import com.coffeepeek.admin.auth.GoogleAuth
import com.coffeepeek.admin.settings.CityPreference
import com.coffeepeek.admin.settings.ReviewDraftStore
import com.coffeepeek.admin.theme.ThemeManager
import com.coffeepeek.admin.theme.ThemeMode
import com.coffeepeek.admin.utils.ErrorHandler
import com.coffeepeek.domain.model.City
import com.coffeepeek.domain.model.UserProfile
import com.coffeepeek.domain.repository.AuthRepository
import com.coffeepeek.domain.repository.SessionRepository
import com.coffeepeek.domain.repository.ShopRepository
import com.coffeepeek.domain.repository.UserRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString

data class ProfileUiState(
    val isLoggedIn: Boolean = false,
    val email: String = "",
    val displayName: String = "",
    val about: String? = null,
    val avatarUrl: String? = null,
    val initials: String = "",
    val checkInCount: Int = 0,
    val addedShopsCount: Int = 0,
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val refreshError: String? = null,
) {
    val hasContent: Boolean
        get() = displayName.isNotBlank() || !avatarUrl.isNullOrBlank()
}

class ProfileViewModel(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val sessionRepository: SessionRepository,
    private val shopRepository: ShopRepository,
    private val cityPreference: CityPreference,
    private val reviewDrafts: ReviewDraftStore,
) {
    private val workScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    val themeMode: StateFlow<ThemeMode> = ThemeManager.themeMode
    private val _cities = MutableStateFlow<List<City>>(emptyList())
    val cities: StateFlow<List<City>> = _cities.asStateFlow()
    val selectedCityId: StateFlow<String?> = cityPreference.selectedCityId

    private var loadedForUserId: String? = null

    init {
        observeProfileCache()
        observeSessionChanges()
        loadCities()
    }

    fun refreshProfile() {
        val session = sessionRepository.peekSession()
        if (!sessionRepository.isActiveSession(session)) {
            resetProfileState()
            return
        }
        workScope.launch {
            if (loadedForUserId != session?.userId || !sessionRepository.isActiveSession(sessionRepository.peekSession())) return@launch
            val current = _uiState.value
            val showFullScreenLoader = !current.hasContent && current.error == null
            _uiState.update {
                it.copy(
                    isLoading = showFullScreenLoader,
                    isRefreshing = !showFullScreenLoader,
                    error = if (showFullScreenLoader) null else it.error,
                    refreshError = null,
                )
            }
            userRepository.refreshProfile()
                .onFailure { err ->
                    if (sessionRepository.peekSession()?.userId != session?.userId || loadedForUserId != session?.userId) return@onFailure
                    val message = err.message ?: "Ошибка загрузки профиля"
                    _uiState.update { state ->
                        if (state.hasContent) {
                            state.copy(
                                isLoading = false,
                                isRefreshing = false,
                                refreshError = message,
                            )
                        } else {
                            state.copy(
                                isLoading = false,
                                isRefreshing = false,
                                error = message,
                            )
                        }
                    }
                }
        }
    }

    fun setTheme(mode: ThemeMode) {
        ThemeManager.setTheme(mode)
    }

    fun setCity(cityId: String) {
        if (_cities.value.none { it.id == cityId }) return
        cityPreference.select(cityId)
    }

    fun logout() {
        resetProfileState()
        workScope.launch {
            authRepository.logout().onFailure { error ->
                ErrorHandler.showError(
                    error.message?.takeIf(String::isNotBlank)
                        ?: getString(Res.string.local_data_cleanup_error),
                )
            }
            GoogleAuth.signOut()
            // Drafts belong to the signed-in user; don't leak them into the next account.
            reviewDrafts.clearAll()
        }
    }

    private fun observeProfileCache() {
        workScope.launch {
            userRepository.observeProfile().collect { profile ->
                if (profile != null) {
                    applyProfile(profile)
                } else {
                    _uiState.value = ProfileUiState(
                        isLoggedIn = sessionRepository.isActiveSession(sessionRepository.peekSession()) && loadedForUserId != null,
                        isLoading = sessionRepository.isActiveSession(sessionRepository.peekSession()) && loadedForUserId != null,
                    )
                }
            }
        }
    }

    private fun loadCities() {
        workScope.launch {
            shopRepository.getCatalogs()
                .onSuccess { catalogs ->
                    _cities.value = catalogs.cities
                    cityPreference.resolve(catalogs.cities)
                }
        }
    }

    private fun observeSessionChanges() {
        workScope.launch {
            sessionRepository.observeSession()
                .map { session ->
                    when {
                        !sessionRepository.isActiveSession(session) -> null
                        else -> session?.userId
                    }
                }
                .distinctUntilChanged()
                .collect { userId ->
                    if (userId == null) {
                        resetProfileState()
                    } else if (userId != loadedForUserId) {
                        if (loadedForUserId != null) {
                            resetProfileState()
                        }
                        _uiState.update {
                            it.copy(
                                isLoggedIn = true,
                                isLoading = true,
                            )
                        }
                        loadedForUserId = userId
                        val profile = userRepository.observeProfile().value
                        if (profile == null) {
                            refreshProfile()
                        } else {
                            applyProfile(profile)
                        }
                    }
                }
        }
    }

    private fun applyProfile(profile: UserProfile) {
        _uiState.update {
            val session = sessionRepository.peekSession()
            if (!sessionRepository.isActiveSession(session) || loadedForUserId == null || loadedForUserId != session?.userId) {
                return@update ProfileUiState(isLoading = false)
            }
            it.copy(
                isLoggedIn = true,
                email = profile.email,
                displayName = profile.userName,
                about = profile.about,
                avatarUrl = profile.avatarUrl,
                initials = buildInitials(profile.userName),
                checkInCount = profile.checkInCount,
                addedShopsCount = profile.addedShopsCount,
                isLoading = false,
                isRefreshing = false,
                error = null,
                refreshError = null,
            )
        }
    }

    private fun resetProfileState() {
        loadedForUserId = null
        _uiState.value = ProfileUiState(isLoading = false)
    }

    private fun buildInitials(name: String): String {
        val parts = name.trim().split(" ", "_", ".")
        return when {
            parts.size >= 2 -> "${parts[0].firstOrNull() ?: ""}${parts[1].firstOrNull() ?: ""}".uppercase()
            name.length >= 2 -> name.take(2).uppercase()
            name.isNotEmpty() -> name.first().uppercase()
            else -> "?"
        }
    }
}
