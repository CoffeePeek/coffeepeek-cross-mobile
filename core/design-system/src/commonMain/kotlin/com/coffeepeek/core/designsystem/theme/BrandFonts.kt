package com.coffeepeek.core.designsystem.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.coffeepeek.core.designsystem.resources.Res
import com.coffeepeek.core.designsystem.resources.manrope_bold
import com.coffeepeek.core.designsystem.resources.manrope_extrabold
import com.coffeepeek.core.designsystem.resources.manrope_light
import com.coffeepeek.core.designsystem.resources.manrope_medium
import com.coffeepeek.core.designsystem.resources.manrope_regular
import com.coffeepeek.core.designsystem.resources.manrope_semibold
import org.jetbrains.compose.resources.Font

/** Packaged brand fonts; generated resource accessors stay private to this module. */
val Manrope: FontFamily
    @Composable get() = FontFamily(
        Font(Res.font.manrope_light, FontWeight.Light),
        Font(Res.font.manrope_regular, FontWeight.Normal),
        Font(Res.font.manrope_medium, FontWeight.Medium),
        Font(Res.font.manrope_semibold, FontWeight.SemiBold),
        Font(Res.font.manrope_bold, FontWeight.Bold),
        Font(Res.font.manrope_extrabold, FontWeight.ExtraBold),
    )
