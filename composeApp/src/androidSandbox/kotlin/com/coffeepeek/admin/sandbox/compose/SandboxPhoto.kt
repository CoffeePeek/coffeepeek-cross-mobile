package com.coffeepeek.admin.sandbox.compose

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.coffeepeek.R
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme

/** Local memory decoding only. An unknown URL never invokes a network image loader. */
@Composable internal fun SandboxPhoto(bytes: ByteArray?, modifier: Modifier = Modifier) {
    val image = remember(bytes) { bytes?.let { BitmapFactory.decodeByteArray(it, 0, it.size)?.asImageBitmap() } }
    if (image != null) Image(image, stringResource(R.string.sandbox_photo), modifier, contentScale = ContentScale.Fit)
    else Icon(CoffeePeekIcons.Coffee, stringResource(R.string.sandbox_photo), modifier)
}
@PreviewLightDark @Composable private fun SandboxPhotoPreview() = CoffeePeekTheme { Surface { SandboxPhoto(null) } }
