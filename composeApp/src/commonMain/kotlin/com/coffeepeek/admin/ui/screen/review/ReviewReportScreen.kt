package com.coffeepeek.admin.ui.screen.review

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import com.coffeepeek.admin.theme.CpDimens
import com.coffeepeek.admin.ui.component.CpTopBar
import com.coffeepeek.admin.ui.component.ReviewTextInput
import com.coffeepeek.admin.ui.component.CoffeePeekLoader
import com.coffeepeek.admin.ui.Navigator
import com.coffeepeek.admin.di.platformViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun ReviewReportScreen(reviewId: String) {
    val vm: ReviewReportViewModel = platformViewModel(parameters = { parametersOf(reviewId) })
    val state by vm.state.collectAsState()
    Scaffold(topBar = { CpTopBar("Пожаловаться на отзыв") }) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).imePadding().verticalScroll(rememberScrollState()).padding(CpDimens.spacing4),
            verticalArrangement = Arrangement.spacedBy(CpDimens.spacing3),
        ) {
            if (state.isSubmitted) {
                Text("Жалоба отправлена", style = MaterialTheme.typography.titleMedium)
                Text("Спасибо. Мы проверим отзыв.")
                Button(onClick = Navigator::popBack, modifier = Modifier.fillMaxWidth()) { Text("Готово") }
                return@Column
            }
            Text("Опишите проблему с отзывом", style = MaterialTheme.typography.titleMedium)
            ReviewTextInput(
                value = state.text, onValueChange = vm::updateText,
                placeholder = "Что не так с этим отзывом?", maxLength = 2000,
                modifier = Modifier.fillMaxWidth(),
            )
            state.error?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error) }
            Button(onClick = vm::submit, enabled = !state.isSubmitting && state.text.isNotBlank(), modifier = Modifier.fillMaxWidth()) {
                if (state.isSubmitting) CoffeePeekLoader() else Text("Отправить")
            }
        }
    }
}
