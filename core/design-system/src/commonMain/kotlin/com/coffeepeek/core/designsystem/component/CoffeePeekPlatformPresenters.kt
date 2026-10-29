package com.coffeepeek.core.designsystem.component

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier

/**
 * Seams for platform-native presentation. The app installs a presenter for a platform that has a native counterpart
 * (a UIKit sheet or navigation bar on iOS); a component asks it first and falls back to its own Compose rendering
 * when there is no presenter or the presenter reports that it did not take over.
 */
interface CoffeePeekNativeSheetPresenter {
    /** Presents [content] as a native sheet and returns true, or returns false to let the caller render its own sheet. */
    @Composable
    fun Present(
        onDismiss: () -> Unit,
        dismissEnabled: Boolean,
        maxSheetHeightFraction: Float?,
        content: @Composable ColumnScope.() -> Unit,
    ): Boolean
}

/** A header action is presentation state; its callback stays with the owning screen. */
data class CoffeePeekHeaderAction(val label: String, val onClick: () -> Unit)

interface CoffeePeekNativeHeaderPresenter {
    /** Shows the title, back and [action] in the native navigation bar and returns true, or false to let the caller draw its bar. */
    @Composable
    fun Present(title: String, onBack: (() -> Unit)?, customBack: Boolean, action: CoffeePeekHeaderAction?): Boolean
}

val LocalCoffeePeekNativeSheetPresenter = staticCompositionLocalOf<CoffeePeekNativeSheetPresenter?> { null }
val LocalCoffeePeekNativeHeaderPresenter = staticCompositionLocalOf<CoffeePeekNativeHeaderPresenter?> { null }

interface CoffeePeekNativeDatePickerPresenter {
    /**
     * Shows the native date (or time) wheels and returns true, or false to let the caller render its own picker.
     * Millis are the UTC-midnight value of the civil day, as Material's date picker uses them.
     */
    @Composable
    fun Present(
        title: String, initialMillis: Long, maximumMillis: Long?, timeOnly: Boolean,
        onConfirm: (Long) -> Unit, onDismiss: () -> Unit,
    ): Boolean
}

interface CoffeePeekNativeAlertPresenter {
    /** Shows a system alert and returns true, or false to let the caller render its own dialog. */
    @Composable
    fun Present(
        title: String, message: String, confirmLabel: String, onConfirm: () -> Unit,
        onDismiss: () -> Unit, cancelLabel: String?, destructive: Boolean,
    ): Boolean
}

interface CoffeePeekNativePhotoSourcePresenter {
    /** Shows the system "gallery or camera" action sheet and returns true, or false to let the caller render its own sheet. */
    @Composable
    fun Present(title: String, onGallery: () -> Unit, onCamera: () -> Unit, onDismiss: () -> Unit): Boolean
}

enum class CoffeePeekHaptic { Selection, Success, Error }

/** Small platform reactions a component may trigger; a platform without them installs nothing. */
interface CoffeePeekPlatformEffects {
    fun haptic(event: CoffeePeekHaptic)

    /** Lets a status-bar tap scroll [state] to the top. */
    @Composable
    fun ScrollToTop(state: LazyListState)
}

val LocalCoffeePeekNativeDatePickerPresenter = staticCompositionLocalOf<CoffeePeekNativeDatePickerPresenter?> { null }
val LocalCoffeePeekNativeAlertPresenter = staticCompositionLocalOf<CoffeePeekNativeAlertPresenter?> { null }
val LocalCoffeePeekNativePhotoSourcePresenter = staticCompositionLocalOf<CoffeePeekNativePhotoSourcePresenter?> { null }
val LocalCoffeePeekPlatformEffects = staticCompositionLocalOf<CoffeePeekPlatformEffects?> { null }

/** The native photo-source sheet when the platform has one; false means the caller shows its own. */
@Composable
fun CoffeePeekNativePhotoSource(title: String, onGallery: () -> Unit, onCamera: () -> Unit, onDismiss: () -> Unit): Boolean =
    LocalCoffeePeekNativePhotoSourcePresenter.current?.Present(title, onGallery, onCamera, onDismiss) == true

/** Uses the installed native alert; false lets a feature keep its existing shared dialog layout. */
@Composable
fun CoffeePeekNativeAlert(
    title: String, message: String, confirmLabel: String, onConfirm: () -> Unit, onDismiss: () -> Unit,
    cancelLabel: String? = null, destructive: Boolean = false,
): Boolean = LocalCoffeePeekNativeAlertPresenter.current?.Present(title, message, confirmLabel, onConfirm, onDismiss, cancelLabel, destructive) == true

/** The native glyph a platform draws for a glass icon button; it replaces the Compose icon when a presenter takes over. */
enum class CoffeePeekGlassIcon { Back, Add, Edit, Favorite, FavoriteFilled, Share, Close, Zones, Location }

/** The native glyph of a menu row. */
enum class CoffeePeekMenuIcon { Edit, Hide, Report, NoteEdit, Add, CoffeeBean, Logout, Block, Share }

data class CoffeePeekNativeMenuItem(
    val id: String,
    val title: String,
    val icon: CoffeePeekMenuIcon?,
    val destructive: Boolean,
    val onClick: () -> Unit,
)

interface CoffeePeekNativeControlsPresenter {
    /** Native equal-width segments; false keeps the shared renderer (for example for too many options). */
    @Composable
    fun SegmentedControl(labels: List<String>, selectedIndex: Int, onSelected: (Int) -> Unit, modifier: Modifier, enabled: Boolean): Boolean

    /** Draws the glass button natively and returns true, or returns false to let the caller draw its own. */
    @Composable
    fun GlassIconButton(
        icon: CoffeePeekGlassIcon, onClick: () -> Unit, contentDescription: String, modifier: Modifier, enabled: Boolean,
    ): Boolean

    /** Opens a native menu from the [content] trigger and returns true, or false to let the caller show its own popup. */
    @Composable
    fun Menu(
        items: List<CoffeePeekNativeMenuItem>, contentDescription: String, modifier: Modifier, enabled: Boolean,
        content: @Composable (onClick: () -> Unit) -> Unit,
    ): Boolean

    /** Draws the platform's spinner and returns true, or false to let the caller draw its own loader. */
    @Composable
    fun ActivityIndicator(contentDescription: String, modifier: Modifier): Boolean
}

val LocalCoffeePeekNativeControlsPresenter = staticCompositionLocalOf<CoffeePeekNativeControlsPresenter?> { null }
