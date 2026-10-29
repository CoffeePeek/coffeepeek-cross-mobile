package com.coffeepeek.admin.sandbox.packscan

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.coffeepeek.R
import com.coffeepeek.admin.sandbox.packscan.geometry.PackPoint
import com.coffeepeek.admin.sandbox.packscan.imaging.PackModeration
import com.coffeepeek.admin.sandbox.packscan.imaging.PackModerationReason
import com.coffeepeek.admin.sandbox.packscan.imaging.PackModerationStatus
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import java.io.File
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal data class PackScanProbeState(
    val processing: Boolean = false,
    val repainting: Boolean = false,
    val result: PackCutoutResult? = null,
    val error: String? = null,
    // Latest brush repair; null shows the automatic result.
    val edited: Bitmap? = null,
    val repaintMillis: Long? = null,
    val segmenterMissing: Boolean = false,
    val inpainterMissing: Boolean = false,
) {
    val busy: Boolean get() = processing || repainting
    val current: Bitmap? get() = edited ?: result?.softened
}

private class PackScanProbeActions(
    val onTakePhoto: () -> Unit,
    val onPickPhoto: () -> Unit,
    val onRepaint: () -> Unit,
    val onClearStrokes: () -> Unit,
    val onResetEdits: () -> Unit,
    val onClose: () -> Unit,
)

/** Sandbox-only evaluation of the Google-free bag pipeline (U2-Net-p, MI-GAN) on real coffee bags. */
@Composable internal fun PackScanProbe(onClose: () -> Unit) {
    val context = LocalContext.current.applicationContext
    val processor = remember(context) { PackCutoutProcessor(context) }
    DisposableEffect(processor) { onDispose { processor.close() } }
    val scope = rememberCoroutineScope()
    var state by remember {
        mutableStateOf(PackScanProbeState(segmenterMissing = !processor.segmenterFile.isFile, inpainterMissing = !processor.inpainterFile.isFile))
    }
    val strokes = remember { mutableStateListOf<List<PackPoint>>() }
    val run: (Uri) -> Unit = { uri -> scope.launch { strokes.clear(); processor.probe(uri, context, state) { state = it } } }
    var pendingCapture by remember { mutableStateOf<Uri?>(null) }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { saved ->
        pendingCapture?.takeIf { saved }?.let(run)
        pendingCapture = null
    }
    val gallery = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri -> uri?.let(run) }
    val actions = PackScanProbeActions(
        // Sandbox declares no CAMERA permission, so the system camera app captures without a runtime request.
        onTakePhoto = { context.captureUri().also { pendingCapture = it }.let(camera::launch) },
        onPickPhoto = { gallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
        onRepaint = { scope.launch { processor.repaintProbe(state, strokes.toList(), context) { state = it }; strokes.clear() } },
        onClearStrokes = { strokes.clear() },
        onResetEdits = { state = state.copy(edited = null, repaintMillis = null); strokes.clear() },
        onClose = onClose,
    )
    PackScanProbeContent(state, strokes, actions)
}

private fun Context.captureUri(): Uri {
    val file = File(File(cacheDir, "camera").apply { mkdirs() }, "pack-probe.jpg")
    return FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
}

private suspend fun PackCutoutProcessor.probe(photo: Uri, context: Context, previous: PackScanProbeState, update: (PackScanProbeState) -> Unit) {
    val idle = previous.copy(processing = false, repainting = false, result = null, error = null, edited = null, repaintMillis = null)
    update(idle.copy(processing = true))
    val next = try {
        val result = process(photo)
        withContext(Dispatchers.IO) { context.exportPackProbe(result) }
        idle.copy(result = result)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        idle.copy(error = e.describe())
    }
    update(next)
}

private suspend fun PackCutoutProcessor.repaintProbe(
    state: PackScanProbeState,
    strokes: List<List<PackPoint>>,
    context: Context,
    update: (PackScanProbeState) -> Unit,
) {
    val image = state.current ?: return
    update(state.copy(repainting = true, error = null))
    val next = try {
        val (edited, millis) = repaint(image, strokes)
        withContext(Dispatchers.IO) { context.exportPackImages("edited" to edited) }
        state.copy(edited = edited, repaintMillis = millis)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        state.copy(error = e.describe())
    }
    update(next)
}

private fun Exception.describe(): String = message ?: javaClass.simpleName

@Composable private fun PackScanProbeContent(
    state: PackScanProbeState,
    strokes: SnapshotStateList<List<PackPoint>>,
    actions: PackScanProbeActions,
) {
    Surface(Modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxSize().systemBarsPadding().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.sandbox_pack_probe_title), Modifier.weight(1f), style = MaterialTheme.typography.titleLarge)
                TextButton(actions.onClose) { Text(stringResource(R.string.sandbox_close)) }
            }
            PackProbeSourceButtons(enabled = !state.busy && !state.segmenterMissing, actions)
            if (state.segmenterMissing) Text(stringResource(R.string.sandbox_pack_probe_segmenter_missing), color = MaterialTheme.colorScheme.error)
            if (state.inpainterMissing) Text(stringResource(R.string.sandbox_pack_probe_inpainter_missing), color = MaterialTheme.colorScheme.error)
            if (state.busy) PackProbeProgress(if (state.repainting) R.string.sandbox_pack_probe_repainting else R.string.sandbox_pack_probe_processing)
            state.error?.let { Text(stringResource(R.string.sandbox_pack_probe_failed, it), color = MaterialTheme.colorScheme.error) }
            state.result?.let { PackCutoutResultView(it, state, strokes, actions) }
        }
    }
}

@Composable private fun PackProbeSourceButtons(enabled: Boolean, actions: PackScanProbeActions) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(actions.onTakePhoto, enabled = enabled) { Text(stringResource(R.string.sandbox_pack_probe_take_photo)) }
        OutlinedButton(actions.onPickPhoto, enabled = enabled) { Text(stringResource(R.string.sandbox_pack_probe_pick_photo)) }
    }
}

@Composable private fun PackProbeProgress(@StringRes label: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
        Text(stringResource(label))
    }
}

@Composable private fun PackCutoutResultView(
    result: PackCutoutResult,
    state: PackScanProbeState,
    strokes: SnapshotStateList<List<PackPoint>>,
    actions: PackScanProbeActions,
) {
    PackModerationBlock(result.moderation)
    PackProbeImage(R.string.sandbox_pack_probe_card_label, result.card)
    val t = result.timings
    Text(stringResource(R.string.sandbox_pack_probe_timings, t.segmentation, t.fit, t.repair, t.soften, t.total), style = MaterialTheme.typography.bodySmall)
    result.start?.let { Text(stringResource(R.string.sandbox_pack_probe_start, it), style = MaterialTheme.typography.bodySmall) }
    state.repaintMillis?.let { Text(stringResource(R.string.sandbox_pack_probe_brush_millis, it), style = MaterialTheme.typography.bodySmall) }
    state.current?.let { current ->
        Text(stringResource(R.string.sandbox_pack_probe_editor_label), style = MaterialTheme.typography.labelLarge)
        PackBrushEditor(current, strokes, enabled = !state.busy && !state.inpainterMissing)
        PackEditorButtons(state, hasStrokes = strokes.isNotEmpty(), actions)
    }
    PackProbeImage(R.string.sandbox_pack_probe_overlay_label, result.overlay)
    PackProbeImage(R.string.sandbox_pack_probe_rectified_label, result.rectified)
}

/** Verdict of the pre-moderation with every reason the user would be shown. */
@Composable private fun PackModerationBlock(moderation: PackModeration) {
    val (title, color) = when (moderation.status) {
        PackModerationStatus.Accepted -> R.string.sandbox_pack_probe_status_accepted to MaterialTheme.colorScheme.primary
        PackModerationStatus.AcceptedWithHint -> R.string.sandbox_pack_probe_status_hint to MaterialTheme.colorScheme.tertiary
        PackModerationStatus.Retake -> R.string.sandbox_pack_probe_status_retake to MaterialTheme.colorScheme.error
    }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(stringResource(title), color = color, style = MaterialTheme.typography.titleMedium)
        moderation.reasons.forEach { Text("• " + stringResource(it.message())) }
    }
}

@StringRes private fun PackModerationReason.message(): Int = when (this) {
    PackModerationReason.EdgesNotFound -> R.string.sandbox_pack_probe_reason_edges
    PackModerationReason.NotWholeBag -> R.string.sandbox_pack_probe_reason_whole
    PackModerationReason.TooFar -> R.string.sandbox_pack_probe_reason_far
    PackModerationReason.OutOfFrame -> R.string.sandbox_pack_probe_reason_frame
    PackModerationReason.ObjectsOnBag -> R.string.sandbox_pack_probe_reason_objects
    PackModerationReason.BagCovered -> R.string.sandbox_pack_probe_reason_covered
    PackModerationReason.Tilted -> R.string.sandbox_pack_probe_reason_tilted
    PackModerationReason.MoveCloser -> R.string.sandbox_pack_probe_reason_closer
    PackModerationReason.Glare -> R.string.sandbox_pack_probe_reason_glare
}

@Composable private fun PackEditorButtons(state: PackScanProbeState, hasStrokes: Boolean, actions: PackScanProbeActions) {
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(actions.onRepaint, enabled = hasStrokes && !state.busy && !state.inpainterMissing) {
            Text(stringResource(R.string.sandbox_pack_probe_repaint))
        }
        TextButton(actions.onClearStrokes, enabled = hasStrokes) { Text(stringResource(R.string.sandbox_pack_probe_clear_strokes)) }
        TextButton(actions.onResetEdits, enabled = state.edited != null && !state.busy) {
            Text(stringResource(R.string.sandbox_pack_probe_reset_edits))
        }
    }
}

@Composable private fun PackProbeImage(@StringRes label: Int, bitmap: Bitmap?) {
    bitmap ?: return
    val image = remember(bitmap) { bitmap.asImageBitmap() }
    Text(stringResource(label), style = MaterialTheme.typography.labelLarge)
    Image(image, stringResource(label), Modifier.fillMaxWidth().heightIn(max = 420.dp).checkerboard(), contentScale = ContentScale.Fit)
}

// Transparency checkerboard keeps the same fixed colors in both themes.
internal fun Modifier.checkerboard(): Modifier = clipToBounds().drawBehind {
    val cell = 12.dp.toPx()
    for (row in 0..(size.height / cell).toInt()) {
        for (column in 0..(size.width / cell).toInt()) {
            val color = if ((row + column) % 2 == 0) CheckerLight else CheckerDark
            drawRect(color, Offset(column * cell, row * cell), Size(cell, cell))
        }
    }
}

private val CheckerLight = Color(0xFFDDDDDD)
private val CheckerDark = Color(0xFFAAAAAA)

private val PreviewActions = PackScanProbeActions({}, {}, {}, {}, {}, {})

@PreviewLightDark @Composable private fun PackScanProbeIdlePreview() = CoffeePeekTheme {
    PackScanProbeContent(PackScanProbeState(), remember { mutableStateListOf() }, PreviewActions)
}

@PreviewLightDark @Composable private fun PackScanProbeProcessingPreview() = CoffeePeekTheme {
    PackScanProbeContent(PackScanProbeState(processing = true), remember { mutableStateListOf() }, PreviewActions)
}

@PreviewLightDark @Composable private fun PackScanProbeModelsMissingPreview() = CoffeePeekTheme {
    PackScanProbeContent(PackScanProbeState(segmenterMissing = true, inpainterMissing = true), remember { mutableStateListOf() }, PreviewActions)
}

@PreviewLightDark @Composable private fun PackModerationRetakePreview() = CoffeePeekTheme {
    Surface {
        PackModerationBlock(PackModeration(PackModerationStatus.Retake, listOf(PackModerationReason.EdgesNotFound, PackModerationReason.ObjectsOnBag)))
    }
}

@PreviewLightDark @Composable private fun PackModerationHintPreview() = CoffeePeekTheme {
    Surface { PackModerationBlock(PackModeration(PackModerationStatus.AcceptedWithHint, listOf(PackModerationReason.Glare))) }
}
