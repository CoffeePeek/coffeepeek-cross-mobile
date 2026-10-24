package com.coffeepeek.feature.appupdate.impl.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.LifecycleEventEffect
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.appupdate.api.AppUpdateEnvironment
import com.coffeepeek.feature.appupdate.impl.AppUpdateController
import com.coffeepeek.feature.appupdate.impl.resources.Res
import com.coffeepeek.feature.appupdate.impl.resources.appupdate_app_store
import com.coffeepeek.feature.appupdate.impl.resources.appupdate_body_available
import com.coffeepeek.feature.appupdate.impl.resources.appupdate_body_downloading
import com.coffeepeek.feature.appupdate.impl.resources.appupdate_body_downloading_percent
import com.coffeepeek.feature.appupdate.impl.resources.appupdate_body_installing
import com.coffeepeek.feature.appupdate.impl.resources.appupdate_body_ready
import com.coffeepeek.feature.appupdate.impl.resources.appupdate_body_required
import com.coffeepeek.feature.appupdate.impl.resources.appupdate_body_starting
import com.coffeepeek.feature.appupdate.impl.resources.appupdate_body_verifying
import com.coffeepeek.feature.appupdate.impl.resources.appupdate_cancel
import com.coffeepeek.feature.appupdate.impl.resources.appupdate_download
import com.coffeepeek.feature.appupdate.impl.resources.appupdate_install
import com.coffeepeek.feature.appupdate.impl.resources.appupdate_later
import com.coffeepeek.feature.appupdate.impl.resources.appupdate_retry
import com.coffeepeek.feature.appupdate.impl.resources.appupdate_title_available
import com.coffeepeek.feature.appupdate.impl.resources.appupdate_title_downloading
import com.coffeepeek.feature.appupdate.impl.resources.appupdate_title_installing
import com.coffeepeek.feature.appupdate.impl.resources.appupdate_title_ready
import com.coffeepeek.feature.appupdate.impl.resources.appupdate_title_required
import com.coffeepeek.feature.appupdate.impl.resources.appupdate_title_starting
import com.coffeepeek.feature.appupdate.impl.resources.appupdate_title_verifying
import com.coffeepeek.feature.appupdate.impl.needsPlayFallback
import androidx.lifecycle.Lifecycle
import com.coffeepeek.feature.appupdate.domain.InstallationStage
import com.coffeepeek.feature.appupdate.domain.InstallationState
import com.coffeepeek.feature.appupdate.domain.UpdateInstaller
import kotlin.math.roundToInt
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
internal fun AppUpdatePrompt(
    controller: AppUpdateController,
    installer: UpdateInstaller,
    environment: AppUpdateEnvironment,
    installationEffect: @Composable () -> Unit,
) {
    installationEffect()
    val transfer by installer.state.collectAsState()
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { controller.check() }
    val state by controller.state.collectAsState()
    val update = transfer.update ?: state.update ?: return
    val required = (state.update ?: update).isRequired(environment.versionCode ?: return)
    if (environment.channel == "play" && !needsPlayFallback(required, transfer)) return
    var hidden by rememberSaveable(update.versionCode) { mutableStateOf(false) }
    LaunchedEffect(state.showPrompt) { if (state.showPrompt) hidden = false }
    LaunchedEffect(transfer.stage) { if (transfer.stage == InstallationStage.Ready) hidden = false }
    if (hidden || (!state.showPrompt && transfer.stage == InstallationStage.Available && transfer.error == null)) return
    val dismiss = { hidden = true; controller.dismiss() }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    val threshold = with(LocalDensity.current) { 64.dp.toPx() }
    Dialog(
        onDismissRequest = { if (!required) dismiss() },
        properties = DialogProperties(
            dismissOnBackPress = !required,
            dismissOnClickOutside = !required,
            usePlatformDefaultWidth = false,
        ),
    ) {
        Box(
            modifier = Modifier.fillMaxSize().statusBarsPadding().padding(16.dp),
            contentAlignment = Alignment.TopCenter,
        ) {
            UpdateCard(
                required = required,
                ios = environment.platform == "ios",
                transfer = transfer,
                onUpdate = {
                    if (transfer.stage == InstallationStage.Ready) installer.install() else installer.download(update)
                },
                onCancel = installer::cancel,
                onDismiss = dismiss,
                modifier = Modifier.padding(top = 24.dp)
                    .offset { IntOffset(0, dragOffset.roundToInt()) }
                    .pointerInput(required, update.versionCode, threshold) {
                        if (!required) detectVerticalDragGestures(
                            onVerticalDrag = { change, amount ->
                                change.consume()
                                dragOffset = (dragOffset + amount).coerceAtMost(0f)
                            },
                            onDragEnd = {
                                if (dragOffset < -threshold) dismiss()
                                dragOffset = 0f
                            },
                            onDragCancel = { dragOffset = 0f },
                        )
                    },
            )
        }
    }
}

@Composable
private fun UpdateCard(
    required: Boolean,
    ios: Boolean,
    transfer: InstallationState,
    onUpdate: () -> Unit,
    onCancel: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.widthIn(max = 560.dp).fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shadowElevation = 12.dp,
    ) {
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier.size(52.dp).background(MaterialTheme.colorScheme.primary, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    if (transfer.busy) {
                        val progress = transfer.progress
                        if (progress != null && transfer.stage == InstallationStage.Downloading) {
                            CircularProgressIndicator(progress = { progress }, modifier = Modifier.size(36.dp), color = MaterialTheme.colorScheme.onPrimary)
                        } else {
                            CircularProgressIndicator(modifier = Modifier.size(36.dp), color = MaterialTheme.colorScheme.onPrimary)
                        }
                    } else {
                        Icon(
                            imageVector = if (transfer.stage == InstallationStage.Ready) CoffeePeekIcons.Check else CoffeePeekIcons.Coffee,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(28.dp),
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = when (transfer.stage) {
                            InstallationStage.Starting -> stringResource(Res.string.appupdate_title_starting)
                            InstallationStage.Downloading -> stringResource(Res.string.appupdate_title_downloading)
                            InstallationStage.Verifying -> stringResource(Res.string.appupdate_title_verifying)
                            InstallationStage.Ready -> stringResource(Res.string.appupdate_title_ready)
                            InstallationStage.Installing -> stringResource(Res.string.appupdate_title_installing)
                            InstallationStage.Available -> stringResource(if (required) Res.string.appupdate_title_required else Res.string.appupdate_title_available)
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = when (transfer.stage) {
                            InstallationStage.Starting -> stringResource(Res.string.appupdate_body_starting)
                            InstallationStage.Downloading -> transfer.progress?.let { stringResource(Res.string.appupdate_body_downloading_percent, "${(it * 100).toInt()}%") } ?: stringResource(Res.string.appupdate_body_downloading)
                            InstallationStage.Verifying -> stringResource(Res.string.appupdate_body_verifying)
                            InstallationStage.Ready -> stringResource(Res.string.appupdate_body_ready)
                            InstallationStage.Installing -> stringResource(Res.string.appupdate_body_installing)
                            InstallationStage.Available -> stringResource(if (required) Res.string.appupdate_body_required else Res.string.appupdate_body_available)
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            transfer.error?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (transfer.canCancel && transfer.busy) {
                    OutlinedButton(
                        onClick = onCancel,
                        modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    ) { Text(stringResource(Res.string.appupdate_cancel), color = MaterialTheme.colorScheme.onSurface) }
                } else {
                    if (!required) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        ) {
                            Text(stringResource(Res.string.appupdate_later), color = MaterialTheme.colorScheme.onSurface, textAlign = TextAlign.Center)
                        }
                    }
                    if (!transfer.busy) {
                        Button(
                            onClick = onUpdate,
                            modifier = Modifier.weight(if (required) 1f else 1.4f).heightIn(min = 48.dp),
                            shape = RoundedCornerShape(14.dp),
                        ) {
                            Text(
                                when {
                                    transfer.stage == InstallationStage.Ready -> stringResource(Res.string.appupdate_install)
                                    transfer.error != null -> stringResource(Res.string.appupdate_retry)
                                    ios -> stringResource(Res.string.appupdate_app_store)
                                    else -> stringResource(Res.string.appupdate_download)
                                },
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun UpdateCardLightPreview() {
    CoffeePeekTheme(darkTheme = false) {
        UpdateCard(false, false, InstallationState(), {}, {}, {}, Modifier.padding(16.dp))
    }
}

@Preview
@Composable
private fun UpdateCardDarkPreview() {
    CoffeePeekTheme(darkTheme = true) {
        UpdateCard(true, false, InstallationState(stage = InstallationStage.Ready), {}, {}, {}, Modifier.padding(16.dp))
    }
}
