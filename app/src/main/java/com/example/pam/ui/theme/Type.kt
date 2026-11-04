package com.example.pam.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import com.example.pam.R

@OptIn(ExperimentalTextApi::class)
val DMSans = FontFamily(
    Font(
        resId = R.font.dm_sans,
        weight = FontWeight.Thin,
        variationSettings = FontVariation.Settings(FontVariation.weight(FontWeight.Thin.weight))
    ),
    Font(
        resId = R.font.dm_sans,
        weight = FontWeight.ExtraLight,
        variationSettings = FontVariation.Settings(FontVariation.weight(FontWeight.ExtraLight.weight))
    ),
    Font(
        resId = R.font.dm_sans,
        weight = FontWeight.Light,
        variationSettings = FontVariation.Settings(FontVariation.weight(FontWeight.Light.weight))
    ),
    Font(
        resId = R.font.dm_sans,
        weight = FontWeight.Normal,
        variationSettings = FontVariation.Settings(FontVariation.weight(FontWeight.Normal.weight))
    ),
    Font(
        resId = R.font.dm_sans,
        weight = FontWeight.Medium,
        variationSettings = FontVariation.Settings(FontVariation.weight(FontWeight.Medium.weight))
    ),
    Font(
        resId = R.font.dm_sans,
        weight = FontWeight.SemiBold,
        variationSettings = FontVariation.Settings(FontVariation.weight(FontWeight.SemiBold.weight))
    ),
    Font(
        resId = R.font.dm_sans,
        weight = FontWeight.Bold,
        variationSettings = FontVariation.Settings(FontVariation.weight(FontWeight.Bold.weight))
    ),
    Font(
        resId = R.font.dm_sans,
        weight = FontWeight.ExtraBold,
        variationSettings = FontVariation.Settings(FontVariation.weight(FontWeight.ExtraBold.weight))
    ),
    Font(
        resId = R.font.dm_sans,
        weight = FontWeight.Black,
        variationSettings = FontVariation.Settings(FontVariation.weight(FontWeight.Black.weight))
    ),
    Font(
        resId = R.font.dm_sans_italic,
        weight = FontWeight.Normal,
        style = FontStyle.Italic,
        variationSettings = FontVariation.Settings(FontVariation.weight(FontWeight.Normal.weight))
    ),
    Font(
        resId = R.font.dm_sans_italic,
        weight = FontWeight.Medium,
        style = FontStyle.Italic,
        variationSettings = FontVariation.Settings(FontVariation.weight(FontWeight.Medium.weight))
    ),
    Font(
        resId = R.font.dm_sans_italic,
        weight = FontWeight.Bold,
        style = FontStyle.Italic,
        variationSettings = FontVariation.Settings(FontVariation.weight(FontWeight.Bold.weight))
    )
)

private val defaultTypography = Typography()

val Typography = Typography(
    displayLarge = defaultTypography.displayLarge.copy(fontFamily = DMSans),
    displayMedium = defaultTypography.displayMedium.copy(fontFamily = DMSans),
    displaySmall = defaultTypography.displaySmall.copy(fontFamily = DMSans),

    headlineLarge = defaultTypography.headlineLarge.copy(fontFamily = DMSans),
    headlineMedium = defaultTypography.headlineMedium.copy(fontFamily = DMSans),
    headlineSmall = defaultTypography.headlineSmall.copy(fontFamily = DMSans),

    titleLarge = defaultTypography.titleLarge.copy(fontFamily = DMSans),
    titleMedium = defaultTypography.titleMedium.copy(fontFamily = DMSans),
    titleSmall = defaultTypography.titleSmall.copy(fontFamily = DMSans),

    bodyLarge = defaultTypography.bodyLarge.copy(fontFamily = DMSans),
    bodyMedium = defaultTypography.bodyMedium.copy(fontFamily = DMSans),
    bodySmall = defaultTypography.bodySmall.copy(fontFamily = DMSans),

    labelLarge = defaultTypography.labelLarge.copy(fontFamily = DMSans),
    labelMedium = defaultTypography.labelMedium.copy(fontFamily = DMSans),
    labelSmall = defaultTypography.labelSmall.copy(fontFamily = DMSans)
)