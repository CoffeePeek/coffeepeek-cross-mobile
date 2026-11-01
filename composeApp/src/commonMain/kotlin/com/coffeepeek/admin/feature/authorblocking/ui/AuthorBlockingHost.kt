package com.coffeepeek.admin.feature.authorblocking.ui

import com.coffeepeek.feature.authorblocking.api.AuthorBlockError

import coffeepeek.composeapp.generated.resources.*
import com.coffeepeek.core.designsystem.resources.coffeePeekStringResource as stringResource

import androidx.compose.runtime.*
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.coffeepeek.feature.authorblocking.api.AuthorBlockingPresenter
import com.coffeepeek.admin.ui.Navigator
import com.coffeepeek.admin.ui.component.*
import org.koin.compose.koinInject

@Composable
internal fun AuthorBlockingHost(vm: AuthorBlockingPresenter = koinInject()) {
    val localizedLegacyAuthorBlockingHostAuthorBlocking = stringResource(Res.string.legacy_author_blocking_host_author_blocking)
    val localizedLegacyAuthorBlockingHostBlockingSignInMessage = stringResource(Res.string.legacy_author_blocking_host_blocking_sign_in_message)
    val localizedLogin = stringResource(Res.string.login)
    val localizedLegacyAuthorBlockingHostBlockAuthorMessage = stringResource(Res.string.legacy_author_blocking_host_block_author_message)
    val localizedLegacyAuthorBlockingHostUnblockAuthorMessage = stringResource(Res.string.legacy_author_blocking_host_unblock_author_message)
    val localizedLegacyAuthorBlockingHostBlock = stringResource(Res.string.legacy_author_blocking_host_block)
    val localizedLegacyAuthorBlockingHostProfileUnblock = stringResource(Res.string.legacy_author_blocking_host_profile_unblock)
    val localizedLegacyAuthorBlockingHostBlockingAuthor = stringResource(Res.string.legacy_author_blocking_host_blocking_author)
    val localizedLegacyAuthorBlockingHostUnblockingAuthor = stringResource(Res.string.legacy_author_blocking_host_unblocking_author)
    val localizedLegacyAuthorBlockingHostSaveFailed = stringResource(Res.string.legacy_author_blocking_host_save_failed)
    val localizedHistoryHomeRetry = stringResource(Res.string.history_home_retry)
    val localizedHistoryHomeDeleteCancel = stringResource(Res.string.history_home_delete_cancel)

    val prompt by vm.prompt.collectAsState()
    val needsLogin by vm.needsLogin.collectAsState()
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle, vm) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_START) vm.refresh() }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    if (needsLogin) {
        val title = localizedLegacyAuthorBlockingHostAuthorBlocking
        val message = localizedLegacyAuthorBlockingHostBlockingSignInMessage
        val login = { vm.loginHandled(); Navigator.navigate(Navigator.Screen.Auth) }
        if (!NativeActionSheet(title, listOf(PlatformAction(localizedLogin, onClick = login)), message, dismissBeforeAction = false, onDismiss = vm::loginHandled)) {
            CpActionSheet(title, message, listOf(PlatformAction(localizedLogin, onClick = login)), vm::loginHandled)
        }
    }
    prompt?.let { current ->
        val title = if (current.blocked) stringResource(Res.string.legacy_block_author_named, current.author.name) else stringResource(Res.string.legacy_unblock_author_named, current.author.name)
        val message = if (current.blocked) localizedLegacyAuthorBlockingHostBlockAuthorMessage
            else localizedLegacyAuthorBlockingHostUnblockAuthorMessage
        val label = if (current.blocked) localizedLegacyAuthorBlockingHostBlock else localizedLegacyAuthorBlockingHostProfileUnblock
        val error = current.error?.let {
            when (it) { AuthorBlockError.SaveFailed -> stringResource(Res.string.legacy_author_blocking_save_failed_message) }
        }
        when {
            current.working -> CpOperationStatus(if (current.blocked) localizedLegacyAuthorBlockingHostBlockingAuthor else localizedLegacyAuthorBlockingHostUnblockingAuthor)
            error != null -> {
                val errorTitle = localizedLegacyAuthorBlockingHostSaveFailed
                if (!NativeAlert(errorTitle, error, localizedHistoryHomeRetry, vm::confirm, vm::dismiss, localizedHistoryHomeDeleteCancel)) {
                    CpActionSheet(errorTitle, error, listOf(PlatformAction(localizedHistoryHomeRetry, onClick = vm::confirm)), vm::dismiss)
                }
            }
            else -> {
                val actions = listOf(PlatformAction(label, destructive = current.blocked, onClick = vm::confirm))
                if (!NativeActionSheet(title, actions, message, dismissBeforeAction = false, onDismiss = vm::dismiss)) {
                    CpActionSheet(title, message, actions, vm::dismiss)
                }
            }
        }
    }
}
