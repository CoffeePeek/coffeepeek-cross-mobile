package com.coffeepeek.admin.ui.screen.shop

import com.coffeepeek.admin.ui.component.CpTopBar

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.coffeepeek.admin.theme.CpDimens
import com.coffeepeek.admin.ui.Navigator
import com.coffeepeek.admin.ui.component.AppButton
import com.coffeepeek.admin.ui.component.CoffeePeekLoader
import com.coffeepeek.admin.ui.component.platformTextInputOptions
import com.coffeepeek.admin.ui.component.rememberSyncedTextFieldValue
import com.coffeepeek.admin.ui.component.limitTextLength
import com.coffeepeek.admin.ui.icons.CpIcons
import com.coffeepeek.admin.di.platformViewModel
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShopReportScreen(shopId: String, shopTitle: String) {
    val vm: ShopReportViewModel = platformViewModel(parameters = { parametersOf(shopId) })
    val state by vm.state.collectAsState()

    Scaffold(
        topBar = {
            CpTopBar("Сообщить о неточности")
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .verticalScroll(rememberScrollState())
                .padding(CpDimens.spacing4),
            verticalArrangement = Arrangement.spacedBy(CpDimens.spacing3),
        ) {
            if (state.isSubmitted) {
                ReportSubmittedContent(shopTitle = shopTitle)
                return@Column
            }

            Text(
                text = shopTitle.ifBlank { "Кофейня" },
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
            )
            Text(
                text = "Какие данные нужно исправить?",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            ShopReportReason.entries.forEach { reason ->
                ReasonRow(
                    reason = reason,
                    selected = state.selectedReason == reason,
                    onClick = { vm.selectReason(reason) },
                )
            }

            if (state.selectedReason == ShopReportReason.OTHER) {
                val commentField = rememberSyncedTextFieldValue(state.comment)
                OutlinedTextField(
                    value = commentField.value,
                    onValueChange = { updated ->
                        val limited = updated.limitTextLength(500)
                        commentField.value = limited
                        vm.updateComment(limited.text)
                    },
                    keyboardOptions = platformTextInputOptions(),
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Что именно не так?") },
                    supportingText = { Text("До 500 символов") },
                    minLines = 3,
                    maxLines = 6,
                    isError = state.error != null,
                    shape = RoundedCornerShape(CpDimens.buttonRadius),
                )
            }

            state.error?.let { error ->
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            Spacer(Modifier.height(CpDimens.spacing2))
            if (state.isSubmitting) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    CoffeePeekLoader()
                }
            } else {
                AppButton(
                    text = "Отправить",
                    onClick = vm::submit,
                    enabled = state.selectedReason != null,
                )
            }
        }
    }
}

@Composable
private fun ReportSubmittedContent(shopTitle: String) {
    Spacer(Modifier.height(48.dp))
    Icon(
        imageVector = CpIcons.CheckCircle,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
    )
    Text(
        text = "Спасибо за сообщение",
        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
    )
    Text(
        text = "Мы проверим данные о «${shopTitle.ifBlank { "кофейне" }}».",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.height(CpDimens.spacing3))
    AppButton(text = "Готово", onClick = Navigator::popBack)
}

@Composable
private fun ReasonRow(
    reason: ShopReportReason,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(CpDimens.radiusLg),
        color = if (selected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surface
        },
    ) {
        Row(
            modifier = Modifier.padding(horizontal = CpDimens.spacing3, vertical = CpDimens.spacing2),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RadioButton(selected = selected, onClick = onClick)
            Text(
                text = reason.title,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(start = CpDimens.spacing1),
            )
        }
    }
}
