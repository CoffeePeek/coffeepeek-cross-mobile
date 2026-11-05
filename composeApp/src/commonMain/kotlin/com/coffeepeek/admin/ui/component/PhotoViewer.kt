package com.coffeepeek.admin.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.carousel.HorizontalMultiBrowseCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coffeepeek.composeapp.generated.resources.Res
import coffeepeek.composeapp.generated.resources.ic_hide_image_24px
import com.coffeepeek.admin.theme.CpColor
import com.coffeepeek.admin.ui.icons.CpIcons
import com.coffeepeek.core.designsystem.component.CoffeePeekImage

object PhotoViewer {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    operator fun invoke(
        images: List<String>,
        isEdit: Boolean = false,
        onAdd: () -> Unit = {},
        onDelete: (String) -> Unit = {},
        preferredItemWidth: Dp = 400.dp,
        itemSpacing: Dp = 8.dp,
        cardModifier: Modifier,
        modifier: Modifier,
    ) {
        val size = if (isEdit) images.size + 1 else images.size
        val state = rememberCarouselState(itemCount = { size })
        if (size > 0) {
            Box {
                HorizontalMultiBrowseCarousel(
                    state = state,
                    preferredItemWidth = preferredItemWidth,
                    itemSpacing = itemSpacing,
                    modifier = modifier,
                ) { index ->
                    if (index <= images.lastIndex) {
                        val item = images[index]
                        Box(modifier = cardModifier) {
                            CoffeePeekImage(
                                data = item,
                                contentScale = ContentScale.Crop,
                                placeholder = Res.drawable.ic_hide_image_24px,
                                modifier = cardModifier,
                            )
                            if (isEdit) {
                                Buttons.IconButton(
                                    backgroundColor = CpColor.Error,
                                    imageVector = CpIcons.Delete,
                                    onClick = { onDelete(item) },
                                )
                            }
                        }
                    } else if (isEdit) {
                        Box(modifier = cardModifier.clickable(onClick = onAdd)) {
                            Icon(
                                imageVector = CpIcons.Add,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(200.dp)
                                    .align(Alignment.Center)
                                    .clickable(onClick = onAdd),
                            )
                        }
                    }
                }
            }
        }
    }
}
