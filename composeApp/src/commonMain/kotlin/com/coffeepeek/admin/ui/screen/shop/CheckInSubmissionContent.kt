package com.coffeepeek.admin.ui.screen.shop

import androidx.compose.foundation.layout.*
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.coffeepeek.admin.theme.CpDimens
import com.coffeepeek.admin.ui.component.AppButton
import com.coffeepeek.admin.ui.component.SubmissionStatus
import com.coffeepeek.admin.ui.component.SubmissionStatusContent
import com.coffeepeek.domain.model.CheckInVisibility

@Composable
internal fun CheckInSubmissionContent(
    submittedVisibility: CheckInVisibility?,
    onGoToFeed: () -> Unit,
    onViewCheckIns: () -> Unit,
) {
    SubmissionStatusContent(
        status = if (submittedVisibility == null) SubmissionStatus.Sending else SubmissionStatus.Success,
        title = if (submittedVisibility == null) "Отправляем чекин…" else "Чекин создан!",
        description = when (submittedVisibility) {
            null -> "Сохраняем ваш кофейный момент"
            CheckInVisibility.Public -> "Спасибо, что делитесь кофейными моментами! Чекин появится в ленте после проверки. Сейчас он доступен в ваших чекинах."
            else -> "Кофейный момент сохранён в ваших чекинах и виден только вам."
        },
        modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(CpDimens.spacing6),
        fillAvailableHeight = false,
    ) {
        AppButton("К ленте", onClick = onGoToFeed)
        OutlinedButton(
            onClick = onViewCheckIns,
            modifier = Modifier.fillMaxWidth().heightIn(min = CpDimens.buttonHeight),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
        ) {
            Text("Мои чекины")
        }
    }
}
