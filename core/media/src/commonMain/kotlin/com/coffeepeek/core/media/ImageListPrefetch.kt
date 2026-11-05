package com.coffeepeek.core.media

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember

/** Warm the next few cards using their display URLs, independently of header/footer positions. */
@Composable
fun PrefetchListPhotos(
    listState: LazyListState,
    photosByItem: List<Pair<String, List<String>>>,
) {
    val upcoming by remember(listState, photosByItem) {
        derivedStateOf {
            upcomingPhotoUrls(
                photosByItem = photosByItem,
                visibleKeys = listState.layoutInfo.visibleItemsInfo.map { it.key },
                atStart = listState.firstVisibleItemIndex == 0,
            )
        }
    }
    PrefetchPhotos(upcoming)
}

internal fun upcomingPhotoUrls(
    photosByItem: List<Pair<String, List<String>>>,
    visibleKeys: List<Any>,
    atStart: Boolean,
    lookaheadItems: Int = 3,
    maxPhotos: Int = 6,
): List<String> {
    val visible = visibleKeys.toSet()
    val lastVisible = photosByItem.indexOfLast { it.first in visible }
    // A header may be the only visible item before the first card. A footer must not restart prefetch.
    if (lastVisible == -1 && !atStart) return emptyList()
    return photosByItem.asSequence().drop(lastVisible + 1).take(lookaheadItems)
        .flatMap { it.second.asSequence() }.filter(String::isNotBlank).distinct().take(maxPhotos).toList()
}
