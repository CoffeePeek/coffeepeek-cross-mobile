package com.coffeepeek.admin.feature.authorblocking.ui

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
    val prompt by vm.prompt.collectAsState()
    val needsLogin by vm.needsLogin.collectAsState()
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle, vm) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_START) vm.refresh() }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    if (needsLogin) {
        val title = "Блокировка авторов"
        val message = "Войдите в аккаунт, чтобы блокировки сохранялись на всех ваших устройствах."
        val login = { vm.loginHandled(); Navigator.navigate(Navigator.Screen.Auth) }
        if (!NativeActionSheet(title, listOf(PlatformAction("Войти", onClick = login)), message, dismissBeforeAction = false, onDismiss = vm::loginHandled)) {
            CpActionSheet(title, message, listOf(PlatformAction("Войти", onClick = login)), vm::loginHandled)
        }
    }
    prompt?.let { current ->
        val title = if (current.blocked) "Заблокировать автора «${current.author.name}»?" else "Разблокировать автора «${current.author.name}»?"
        val message = if (current.blocked) "Вы перестанете видеть публикации друг друга. Подписки друг на друга будут удалены. Автор не получит уведомление."
            else "Публикации автора снова смогут появляться в ленте. Прежние подписки не восстановятся."
        val label = if (current.blocked) "Заблокировать" else "Разблокировать"
        val error = current.error
        when {
            current.working -> CpOperationStatus(if (current.blocked) "Блокируем автора…" else "Разблокируем автора…")
            error != null -> {
                val errorTitle = "Не удалось сохранить"
                if (!NativeAlert(errorTitle, error, "Повторить", vm::confirm, vm::dismiss, "Отмена")) {
                    CpActionSheet(errorTitle, error, listOf(PlatformAction("Повторить", onClick = vm::confirm)), vm::dismiss)
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
