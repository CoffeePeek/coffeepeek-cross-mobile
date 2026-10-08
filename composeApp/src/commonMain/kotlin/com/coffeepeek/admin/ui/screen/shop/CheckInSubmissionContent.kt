package com.coffeepeek.admin.ui.screen.shop

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.coffeepeek.admin.theme.CpDimens
import com.coffeepeek.admin.ui.component.AppButton
import com.coffeepeek.admin.ui.component.CoffeePeekLoader
import com.coffeepeek.admin.ui.icons.CpIcons
import com.coffeepeek.domain.model.CheckInVisibility

@Composable
internal fun CheckInSubmissionContent(
    submittedVisibility: CheckInVisibility?,
    onGoToFeed: () -> Unit,
    onViewCheckIns: () -> Unit,
) {
    AnimatedContent(
        targetState = submittedVisibility,
        transitionSpec = {
            (fadeIn(tween(250)) + scaleIn(tween(300), initialScale = 0.9f)) togetherWith fadeOut(tween(150))
        },
        label = "check-in-submission",
    ) { visibility ->
        Column(
            modifier = Modifier.fillMaxWidth().fillMaxHeight(0.76f)
                .navigationBarsPadding().padding(CpDimens.spacing6)
                .semantics { liveRegion = LiveRegionMode.Polite },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(CpDimens.spacing4),
        ) {
            Spacer(Modifier.weight(1f))
            if (visibility == null) {
                CoffeePeekLoader(size = 88.dp)
                Text("Отправляем чекин…", style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
                Text("Сохраняем ваш кофейный момент", color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
            } else {
                Icon(CpIcons.CheckCircle, null, Modifier.size(88.dp), tint = MaterialTheme.colorScheme.primary)
                Text("Чекин создан!", style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
                Text(
                    if (visibility == CheckInVisibility.Public)
                        "Спасибо, что делитесь кофейными моментами! Чекин появится в ленте после проверки. Сейчас он доступен в ваших чекинах."
                    else "Кофейный момент сохранён в ваших чекинах и виден только вам.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center,
                )
            }
            Spacer(Modifier.weight(1f))
            if (visibility != null) {
                AppButton("К ленте", onClick = onGoToFeed)
                OutlinedButton(onClick = onViewCheckIns, modifier = Modifier.fillMaxWidth().heightIn(min = CpDimens.buttonHeight)) {
                    Text("Мои чекины")
                }
            }
        }
    }
}
