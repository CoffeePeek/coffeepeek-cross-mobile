package com.coffeepeek.admin.base

import androidx.lifecycle.ViewModel
import coffeepeek.composeapp.generated.resources.Res
import coffeepeek.composeapp.generated.resources.maybe_later
import com.coffeepeek.api.utils.ApiException
import com.coffeepeek.admin.utils.ErrorHandler
import com.coffeepeek.admin.utils.LoadingHandler
import com.coffeepeek.domain.model.Session
import com.coffeepeek.domain.repository.SessionRepository
import io.ktor.utils.io.core.Closeable
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString

/** Legacy app ViewModel base; migrated features use their own lifecycle-owned state. */
abstract class BaseViewModel : ViewModel(), Closeable {
    protected val workScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    init {
        addCloseable(this)
    }

    /** Возвращает сессию или сбрасывает её и закрывает доступ к защищённым экранам. */
    protected suspend fun requireAuthSession(sessionRepository: SessionRepository): Session? {
        val session = sessionRepository.getSession()
        if (!sessionRepository.isActiveSession(session)) {
            sessionRepository.saveSession(null)
            return null
        }
        return session
    }

    /**
     * Временный обработчик запросов старых экранов входа и регистрации.
     * Отмена корутины не является ошибкой для UI и не показывается пользователю.
     * @param errorMessage Текст ошибки. Если null — выведется дефолтная ошибка
     * @param onSuccess Лямбда, которая выполнится при успехе
     * @param onError Локальный обработчик ошибки. Если задан, глобальное окно не показывается
     * @param request Сам запрос
     */
    protected fun <T> launchRequest(
        onSuccess: (T) -> Unit = {},
        errorMessage: StringResource? = null,
        onError: ((Exception) -> Unit)? = null,
        request: suspend () -> T
    ) {
        workScope.launch {
            try {
                LoadingHandler.showLoading()
                val result = request()
                LoadingHandler.clearLoading()
                onSuccess(result)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (onError != null) {
                    onError(e)
                    return@launch
                }

                val messageToShow = when (e) {
                    is ApiException -> e.message
                    else -> e.message?.takeIf { it.isNotBlank() }
                } ?: getString(errorMessage ?: Res.string.maybe_later)

                ErrorHandler.showError(messageToShow)
            } finally {
                LoadingHandler.clearLoading()
            }
        }
    }

    override fun close() {
        workScope.cancel()
    }
}
