package com.coffeepeek.feature.authorblocking.impl.ui.compose

import androidx.compose.runtime.*
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.coffeepeek.core.designsystem.component.*
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.authorblocking.api.AuthorBlockPrompt
import com.coffeepeek.feature.authorblocking.api.AuthorBlockingPresenter
import com.coffeepeek.feature.authorblocking.api.AuthorReference
import com.coffeepeek.feature.authorblocking.impl.resources.*
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
internal fun AuthorBlockingHost(presenter: AuthorBlockingPresenter, onLogin: () -> Unit, bottomClearanceDp: Float) {
    val prompt by presenter.prompt.collectAsStateWithLifecycle()
    val needsLogin by presenter.needsLogin.collectAsStateWithLifecycle()
    key(presenter) { LifecycleEventEffect(Lifecycle.Event.ON_START) { presenter.refresh() } }
    AuthorBlockingHostContent(prompt, needsLogin, bottomClearanceDp,
        onLogin = {
            presenter.loginHandled()
            onLogin()
        }, onLoginDismiss = presenter::loginHandled,
        onConfirm = presenter::confirm, onDismiss = presenter::dismiss)
}

@Composable
internal fun AuthorBlockingHostContent(prompt: AuthorBlockPrompt?, needsLogin: Boolean, bottomClearanceDp: Float,
    onLogin: () -> Unit, onLoginDismiss: () -> Unit, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    if (needsLogin) CoffeePeekActionSheet(stringResource(Res.string.author_blocking_login_title),
        stringResource(Res.string.author_blocking_login_message),
        listOf(CoffeePeekDialogAction(stringResource(Res.string.author_blocking_login), onLogin)),
        onLoginDismiss, stringResource(Res.string.author_blocking_cancel))
    prompt?.let { current ->
        val error = current.error
        when {
            current.working -> CoffeePeekOperationStatus(stringResource(if (current.blocked)
                Res.string.author_blocking_blocking else Res.string.author_blocking_unblocking), bottomClearanceDp.dp)
            error != null -> AuthorBlockingFailure(error, onConfirm, onDismiss)
            else -> AuthorBlockingConfirmation(current, onConfirm, onDismiss)
        }
    }
}

@Composable
private fun AuthorBlockingConfirmation(prompt: AuthorBlockPrompt, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    val title = stringResource(if (prompt.blocked) Res.string.author_blocking_block_title
        else Res.string.author_blocking_unblock_title, prompt.author.name)
    val message = stringResource(if (prompt.blocked) Res.string.author_blocking_block_message
        else Res.string.author_blocking_unblock_message)
    val label = stringResource(if (prompt.blocked) Res.string.author_blocking_block else Res.string.author_blocking_unblock)
    CoffeePeekActionSheet(title, message, listOf(CoffeePeekDialogAction(label, onConfirm, destructive = prompt.blocked)),
        onDismiss, stringResource(Res.string.author_blocking_cancel))
}

@Composable
private fun AuthorBlockingFailure(error: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    val title = stringResource(Res.string.author_blocking_save_failed)
    val retry = stringResource(Res.string.author_blocking_retry)
    val cancel = stringResource(Res.string.author_blocking_cancel)
    if (!CoffeePeekNativeAlert(title, error, retry, onConfirm, onDismiss, cancel)) {
        CoffeePeekActionSheet(title, error, listOf(CoffeePeekDialogAction(retry, onConfirm)), onDismiss, cancel)
    }
}

// Modal previews may require Interactive Preview; no repository or application DI is used.
@Composable private fun AuthorBlockingHostPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    AuthorBlockingHostContent(AuthorBlockPrompt(AuthorReference("a", "anna", "Анна"), true),
        false, 0f, {}, {}, {}, {})
}
@Preview @Composable private fun AuthorBlockingHostLightPreview() = AuthorBlockingHostPreview(false)
@Preview @Composable private fun AuthorBlockingHostDarkPreview() = AuthorBlockingHostPreview(true)
