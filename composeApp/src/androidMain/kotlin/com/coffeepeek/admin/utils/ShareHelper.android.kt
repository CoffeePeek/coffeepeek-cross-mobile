package com.coffeepeek.admin.utils

import android.content.Intent
import android.content.ClipData
import android.net.Uri
import androidx.core.content.FileProvider
import com.coffeepeek.admin.locator.Locator
import java.io.File
import java.util.UUID

actual object ShareHelper {
    actual fun shareText(text: String) {
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        val chooser = Intent.createChooser(sendIntent, null).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        Locator.appContext.startActivity(chooser)
    }

    actual fun shareImages(images: List<ByteArray>, text: String) {
        require(images.isNotEmpty() && images.all { it.isNotEmpty() })
        val uris = collectionImageUris(images)
        val send = collectionImageIntent(uris, text)
        val chooser = Intent.createChooser(send, null).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        Locator.appContext.startActivity(chooser)
    }
}

private fun collectionImageUris(images: List<ByteArray>): ArrayList<Uri> {
    val context = Locator.appContext
    val directory = File(context.cacheDir, "shared-collections").apply { mkdirs() }
    val cutoff = System.currentTimeMillis() - 172_800_000L
    directory.listFiles()?.filter { it.isFile && it.extension == "png" && it.lastModified() < cutoff }?.forEach { it.delete() }
    val batch = UUID.randomUUID().toString()
    return ArrayList(images.mapIndexed { index, bytes ->
        val file = File(directory, "$batch-${index + 1}.png").apply { writeBytes(bytes) }
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    })
}

private fun collectionImageIntent(uris: ArrayList<Uri>, text: String): Intent = Intent(
    if (uris.size == 1) Intent.ACTION_SEND else Intent.ACTION_SEND_MULTIPLE,
).apply {
    type = "image/png"
    putExtra(Intent.EXTRA_TEXT, text)
    if (uris.size == 1) putExtra(Intent.EXTRA_STREAM, uris.first()) else putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
    clipData = ClipData.newUri(Locator.appContext.contentResolver, null, uris.first()).apply {
        uris.drop(1).forEach { addItem(ClipData.Item(it)) }
    }
    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
}
