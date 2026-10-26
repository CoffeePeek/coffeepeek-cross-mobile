package com.coffeepeek.admin.map

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Bitmap
import android.graphics.PointF
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.View
import android.view.animation.PathInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.ImageButton
import androidx.compose.ui.graphics.toArgb
import com.coffeepeek.core.designsystem.theme.CoffeePeekColors
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import kotlin.math.roundToInt

internal data class MapMarkerViewSpec(
    val target: MapMarkerTarget,
    val bitmap: Bitmap,
    val description: String,
    val selected: Boolean = false,
    val onClick: () -> Unit,
)

/** Native touch/accessibility targets; only the visual positions, scale and alpha are animated. */
internal class MapMarkerViews(context: Context, private val mapView: MapView) : FrameLayout(context) {
    private class Entry(var spec: MapMarkerViewSpec, val view: ImageView) {
        var position = LatLng(spec.target.latitude, spec.target.longitude)
        var animator: ValueAnimator? = null
        var exiting = false
    }

    private data class Snapshot(val position: LatLng, val alpha: Float, val scale: Float)
    private val entries = mutableMapOf<String, Entry>()
    private var map: MapLibreMap? = null
    private val cameraListener = MapLibreMap.OnCameraMoveListener { reposition() }
    private val layoutListener = OnLayoutChangeListener { _, _, _, _, _, _, _, _, _ -> reposition() }

    init {
        clipChildren = false
        clipToPadding = false
        mapView.addView(this, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        addOnLayoutChangeListener(layoutListener)
    }

    fun update(activeMap: MapLibreMap, specs: List<MapMarkerViewSpec>, reducedMotion: Boolean) {
        if (map !== activeMap) {
            map?.removeOnCameraMoveListener(cameraListener)
            map = activeMap
            activeMap.addOnCameraMoveListener(cameraListener)
        }
        val previous = entries.values.toList()
        val snapshots = previous.associate { it.spec.target.key to snapshot(it) }
        val oldTargets = previous.map { it.spec.target }
        val animate = previous.isNotEmpty() && !reducedMotion
        specs.forEach { spec -> updateEntry(spec, snapshots, oldTargets, animate) }
        val nextTargets = specs.map { it.target }
        val nextKeys = nextTargets.map { it.key }.toSet()
        previous.filter { it.spec.target.key !in nextKeys }.forEach { entry ->
            retire(entry, markerParent(entry.spec.target, nextTargets), animate)
        }
    }

    private fun snapshot(entry: Entry) = Snapshot(LatLng(entry.position), entry.view.alpha, entry.view.scaleX)

    private fun updateEntry(spec: MapMarkerViewSpec, snapshots: Map<String, Snapshot>, old: List<MapMarkerTarget>, animate: Boolean) {
        val existing = entries[spec.target.key]
        val from = snapshots[spec.target.key] ?: snapshots[markerParent(spec.target, old)?.key]
        val entry = existing ?: Entry(spec, markerView()).also {
            entries[spec.target.key] = it
            addView(it.view)
        }
        val changed = entry.exiting || existing == null || entry.spec.target != spec.target
        entry.spec = spec
        entry.exiting = false
        configure(entry)
        if (changed) startEntry(entry, existing == null, from, animate)
        else if (!animate) {
            cancel(entry)
            applyFrame(entry, LatLng(spec.target.latitude, spec.target.longitude), 1f, 1f)
        }
    }

    private fun startEntry(entry: Entry, new: Boolean, from: Snapshot?, animate: Boolean) {
        val start = from ?: snapshot(entry)
        val initial = if (new) start.copy(alpha = 0f, scale = 0.65f) else start
        transition(entry, initial, LatLng(entry.spec.target.latitude, entry.spec.target.longitude), 1f, 1f, animate)
    }

    private fun markerView() = ImageButton(context).apply {
        background = null
        setOnFocusChangeListener { _, focused -> foreground = if (focused) focusRing() else null }
    }

    private fun focusRing() = GradientDrawable().apply {
        shape = GradientDrawable.OVAL
        setColor(Color.TRANSPARENT)
        setStroke((2 * resources.displayMetrics.density).roundToInt(), CoffeePeekColors.GoldWarm.toArgb())
    }

    private fun configure(entry: Entry) {
        val spec = entry.spec
        val touch = (48 * resources.displayMetrics.density).roundToInt()
        entry.view.apply {
            setImageBitmap(spec.bitmap)
            scaleType = ImageView.ScaleType.CENTER
            layoutParams = LayoutParams(maxOf(touch, spec.bitmap.width), maxOf(touch, spec.bitmap.height))
            contentDescription = spec.description
            isFocusable = true
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
            translationZ = if (spec.selected) 1f else 0f
            setOnClickListener { spec.onClick() }
        }
        position(entry)
    }

    private fun retire(entry: Entry, parent: MapMarkerTarget?, animate: Boolean) {
        if (entry.exiting && animate) return
        entry.exiting = true
        entry.view.isClickable = false
        entry.view.isFocusable = false
        entry.view.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        val destination = parent?.let { LatLng(it.latitude, it.longitude) } ?: entry.position
        transition(entry, snapshot(entry), destination, 0f, 0.5f, animate) {
            entries.remove(entry.spec.target.key)
            removeView(entry.view)
        }
    }

    private fun transition(entry: Entry, from: Snapshot, to: LatLng, alpha: Float, scale: Float, animate: Boolean, finish: () -> Unit = {}) {
        cancel(entry)
        if (!animate) {
            applyFrame(entry, to, alpha, scale)
            finish()
            return
        }
        entry.animator = markerAnimator(entry, finish) {
            frame(entry, from, to, alpha, scale, it)
        }.also { it.start() }
    }

    private fun markerAnimator(entry: Entry, finish: () -> Unit, update: (Float) -> Unit) = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = MARKER_TRANSITION_MS
        interpolator = PathInterpolator(0.22f, 1f, 0.36f, 1f)
        addUpdateListener { update(it.animatedValue as Float) }
        addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                entry.animator = null
                finish()
            }
        })
    }

    private fun frame(entry: Entry, from: Snapshot, to: LatLng, alpha: Float, scale: Float, progress: Float) {
        val point = LatLng(
            from.position.latitude + (to.latitude - from.position.latitude) * progress,
            from.position.longitude + (to.longitude - from.position.longitude) * progress,
        )
        applyFrame(entry, point, from.alpha + (alpha - from.alpha) * progress, from.scale + (scale - from.scale) * progress)
    }

    private fun applyFrame(entry: Entry, point: LatLng, alpha: Float, scale: Float) {
        entry.position = point
        entry.view.alpha = alpha
        entry.view.scaleX = scale
        entry.view.scaleY = scale
        position(entry)
    }

    private fun position(entry: Entry) {
        val point = map?.projection?.toScreenLocation(entry.position) ?: return
        entry.view.translationX = point.x - entry.view.layoutParams.width / 2f
        entry.view.translationY = point.y - entry.view.layoutParams.height / 2f
    }

    private fun reposition() = entries.values.forEach(::position)

    private fun cancel(entry: Entry) {
        entry.animator?.removeAllListeners()
        entry.animator?.cancel()
        entry.animator = null
    }

    fun covers(point: PointF): Boolean = entries.values.any { entry ->
        val view = entry.view
        !entry.exiting && point.x in view.translationX..(view.translationX + view.layoutParams.width) &&
            point.y in view.translationY..(view.translationY + view.layoutParams.height)
    }

    fun clear() {
        entries.values.forEach(::cancel)
        entries.clear()
        removeAllViews()
    }

    fun dispose() {
        clear()
        map?.removeOnCameraMoveListener(cameraListener)
        removeOnLayoutChangeListener(layoutListener)
        mapView.removeView(this)
    }
}
