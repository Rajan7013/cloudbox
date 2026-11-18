package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.R

val InterFontFamily = FontFamily(
  Font(R.font.inter, FontWeight.Normal),
  Font(R.font.inter, FontWeight.Medium),
  Font(R.font.inter, FontWeight.SemiBold),
  Font(R.font.inter, FontWeight.Bold),
)

// Spec 2.2 Typography Scale (Inter, tabular numerals on numeric styles)
val CloudBoxTypography =
  Typography(
    // Display: 32 / 40, 700
    displaySmall =
      TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = (-0.5).sp,
      ),
    // H1: 24 / 32, 700
    headlineMedium =
      TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = (-0.25).sp,
      ),
    // H2: 20 / 28, 600
    headlineSmall =
      TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 28.sp,
      ),
    // Title: 17 / 24, 600
    titleMedium =
      TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 17.sp,
        lineHeight = 24.sp,
      ),
    titleLarge =
      TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 28.sp,
      ),
    // Body: 15 / 22, 400
    bodyLarge =
      TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp,
      ),
    bodyMedium =
      TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
      ),
    // Label: 14 / 20, 600
    labelLarge =
      TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        fontFeatureSettings = "tnum",
      ),
    // Caption: 12 / 16, 500
    bodySmall =
      TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        fontFeatureSettings = "tnum",
      ),
    labelMedium =
      TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        fontFeatureSettings = "tnum",
      ),
    // Overline: 11 / 16, 700, +0.5 tracking
    labelSmall =
      TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp,
        fontFeatureSettings = "tnum",
      ),
  )
