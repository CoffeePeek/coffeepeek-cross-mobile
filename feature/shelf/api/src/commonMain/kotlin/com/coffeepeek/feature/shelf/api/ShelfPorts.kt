package com.coffeepeek.feature.shelf.api

import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap

/**
 * Application image port for a protected bag card: loads it with the owner's session and owns the loading and failure look.
 * The image sizes itself to the card's proportions within the incoming constraints, so [modifier] (clip, shadow) follows the card;
 * [alignment] places it when it is given a fixed size.
 */
typealias ShelfImage = @Composable (url: String, description: String?, alignment: Alignment, modifier: Modifier) -> Unit

/** The device time zone, so days on the shelf and archive months follow the user's calendar. */
fun interface ShelfTimeZone {
    /** Offset from UTC at the instant [epochSeconds], DST included. */
    fun offsetSeconds(epochSeconds: Long): Int

    companion object {
        val Utc = ShelfTimeZone { 0 }
    }
}

/** A photo of a bag, already upright (EXIF orientation applied); it never leaves the device. */
class ShelfPhoto(val bytes: ByteArray, val fromCamera: Boolean)

/**
 * Application gallery: the system photo picker, one photo at a time. The returned function opens it; [onPicked] gets the photo,
 * [onFailed] when it cannot be read. Cancelling calls neither.
 */
typealias ShelfGalleryPicker = @Composable (onPicked: (ShelfPhoto) -> Unit, onFailed: () -> Unit) -> () -> Unit

enum class ShelfCameraStatus { Starting, Ready, Denied, Unavailable }

/** A focus the user asked for, as shares of the viewfinder; [sharp] is null while the lens is still moving. */
data class ShelfCameraFocus(val x: Float, val y: Float, val sharp: Boolean?)

/** One open camera, for as long as the camera screen is shown: its live picture, the torch and the shot. */
interface ShelfCameraSession {
    val status: ShelfCameraStatus
    val torchAvailable: Boolean
    val torch: Boolean
    /** The tapped focus point, while it is shown; null when the camera focuses on its own. */
    val focus: ShelfCameraFocus?

    fun setTorch(on: Boolean)

    /** Focuses and meters at [x], [y] (shares of the viewfinder), as on a tap. */
    fun focusAt(x: Float, y: Float)

    /** Asks for camera access again after it was refused. */
    fun requestAccess()

    @Composable
    fun Viewfinder(modifier: Modifier)

    /** The picture the viewfinder shows right now, to hold on screen while the shot is taken; null if there is none. */
    fun snapshot(): ImageBitmap?

    /** Focuses once more at the last tapped point or the centre, then shoots: an upright photo, or null when the shot failed. */
    suspend fun capture(): ShelfPhoto?
}

/** Application camera: asks for access itself and runs only while the session is in composition. */
interface ShelfCamera {
    @Composable
    fun rememberSession(): ShelfCameraSession
}

/** A published catalogue coffee the bag can be linked to. */
data class ShelfCoffeeOption(val slug: String, val name: String, val roasterName: String)

/** Catalogue search for the bag form; the application adapts the coffee feature's picker. */
fun interface ShelfCoffeeSearch {
    suspend fun search(query: String): Result<List<ShelfCoffeeOption>>
}

/** A home check-in, as a bag's card lists it: [preparedOn] is an ISO date, [method] a display name, [score] 1–5. */
data class ShelfBrew(
    val id: String,
    val preparedOn: String,
    val method: String?,
    val doseGrams: Double?,
    val score: Int?,
)

/** The owner's home check-ins brewed from bag [bagId], linked by the check-in itself; the application adapts home check-ins. */
fun interface ShelfBrewLog {
    suspend fun brews(bagId: String): Result<List<ShelfBrew>>
}
