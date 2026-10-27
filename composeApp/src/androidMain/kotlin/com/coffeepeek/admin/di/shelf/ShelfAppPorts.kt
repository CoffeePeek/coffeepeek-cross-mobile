package com.coffeepeek.admin.di.shelf

import androidx.compose.ui.layout.ContentScale
import com.coffeepeek.admin.utils.CpImage
import com.coffeepeek.feature.shelf.api.ShelfImage
import com.coffeepeek.feature.shelf.api.ShelfTimeZone
import java.time.Instant
import java.time.ZoneId

/** Bag cards go through the protected photo loader: the owner's session, no shared cache, the whole card visible. */
internal val AndroidShelfImage: ShelfImage = { url, description, alignment, modifier ->
    CpImage(url, contentScale = ContentScale.Fit, alignment = alignment, modifier = modifier, contentDescription = description)
}

/** Read on every call, so a time-zone change while the app runs is picked up. */
internal val AndroidShelfTimeZone = ShelfTimeZone { epochSeconds ->
    ZoneId.systemDefault().rules.getOffset(Instant.ofEpochSecond(epochSeconds)).totalSeconds
}
