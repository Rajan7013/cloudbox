package com.example.ui.theme

import android.app.Activity
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.example.model.FileCategory
import com.example.model.ThemePreference

@Immutable
data class CloudBoxExtendedColors(
  val isDark: Boolean,
  val border: Color,
  val success: Color,
  val warning: Color,
  val textSecondary: Color,
  val toastBackground: Color,
  val scrim: Color,
  val heroGradientStart: Color,
  val heroGradientEnd: Color,
  val sepiaPaperBg: Color,
  val sepiaPaperText: Color,
) {
  fun colorForCategory(category: FileCategory): Color =
    when (category) {
      FileCategory.FOLDER -> TypeFolderColor
      FileCategory.DOCUMENT -> TypeDocumentColor
      FileCategory.SPREADSHEET -> TypeSheetColor
      FileCategory.PDF -> TypePdfColor
      FileCategory.IMAGE -> TypeImageColor
      FileCategory.VIDEO -> TypeVideoColor
      FileCategory.AUDIO -> TypeAudioColor
      FileCategory.ARCHIVE -> TypeArchiveColor
      FileCategory.OTHER -> TypeOtherColor
    }
}

val LocalCloudBoxColors = staticCompositionLocalOf {
  CloudBoxExtendedColors(
    isDark = false,
    border = BorderLight,
    success = SuccessLight,
    warning = WarningLight,
    textSecondary = TextSecondaryLight,
    toastBackground = ToastDarkFill,
    scrim = ScrimLight,
    heroGradientStart = HeroGradientStart,
    heroGradientEnd = HeroGradientEnd,
    sepiaPaperBg = PaperSepiaBackground,
    sepiaPaperText = PaperSepiaText,
  )
}

private val CloudBoxDarkColorScheme =
  darkColorScheme(
    primary = IndigoPrimaryDark,
    onPrimary = IndigoOnPrimaryDark,
    primaryContainer = IndigoContainerDark,
    onPrimaryContainer = IndigoOnContainerDark,
    secondary = VioletAccentDark,
    onSecondary = IndigoOnPrimaryDark,
    secondaryContainer = SurfaceVariantDark,
    onSecondaryContainer = TextPrimaryDark,
    background = BackgroundDark,
    onBackground = TextPrimaryDark,
    surface = SurfaceDark,
    onSurface = TextPrimaryDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = TextSecondaryDark,
    outline = BorderDark,
    outlineVariant = BorderDark,
    error = ErrorDark,
    onError = IndigoOnPrimaryDark,
    errorContainer = Color(0xFF3F1D24),
    onErrorContainer = Color(0xFFFCA5A5),
  )

private val CloudBoxLightColorScheme =
  lightColorScheme(
    primary = IndigoPrimaryLight,
    onPrimary = IndigoOnPrimaryLight,
    primaryContainer = IndigoContainerLight,
    onPrimaryContainer = IndigoOnContainerLight,
    secondary = VioletAccentLight,
    onSecondary = IndigoOnPrimaryLight,
    secondaryContainer = SurfaceVariantLight,
    onSecondaryContainer = TextPrimaryLight,
    background = BackgroundLight,
    onBackground = TextPrimaryLight,
    surface = SurfaceLight,
    onSurface = TextPrimaryLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = TextSecondaryLight,
    outline = BorderLight,
    outlineVariant = BorderLight,
    error = ErrorLight,
    onError = IndigoOnPrimaryLight,
    errorContainer = Color(0xFFFEE2E2),
    onErrorContainer = Color(0xFF991B1B),
  )

// Spec 2.3 Shapes
val CloudBoxShapes =
  Shapes(
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(26.dp),
  )

@Composable
private fun animateColorScheme(target: ColorScheme): ColorScheme {
  val spec = tween<Color>(durationMillis = 250)
  return target.copy(
    primary = animateColorAsState(target.primary, spec, label = "primary").value,
    onPrimary = animateColorAsState(target.onPrimary, spec, label = "onPrimary").value,
    primaryContainer = animateColorAsState(target.primaryContainer, spec, label = "primaryContainer").value,
    onPrimaryContainer = animateColorAsState(target.onPrimaryContainer, spec, label = "onPrimaryContainer").value,
    background = animateColorAsState(target.background, spec, label = "background").value,
    onBackground = animateColorAsState(target.onBackground, spec, label = "onBackground").value,
    surface = animateColorAsState(target.surface, spec, label = "surface").value,
    onSurface = animateColorAsState(target.onSurface, spec, label = "onSurface").value,
    surfaceVariant = animateColorAsState(target.surfaceVariant, spec, label = "surfaceVariant").value,
    onSurfaceVariant = animateColorAsState(target.onSurfaceVariant, spec, label = "onSurfaceVariant").value,
    outline = animateColorAsState(target.outline, spec, label = "outline").value,
    error = animateColorAsState(target.error, spec, label = "error").value,
  )
}

@Composable
fun CloudBoxTheme(
  themePreference: ThemePreference = ThemePreference.SYSTEM,
  content: @Composable () -> Unit,
) {
  val isDark =
    when (themePreference) {
      ThemePreference.SYSTEM -> isSystemInDarkTheme()
      ThemePreference.LIGHT -> false
      ThemePreference.DARK -> true
    }

  val targetScheme = if (isDark) CloudBoxDarkColorScheme else CloudBoxLightColorScheme
  val animatedScheme = animateColorScheme(targetScheme)

  val view = LocalView.current
  if (!view.isInEditMode) {
    SideEffect {
      val window = (view.context as? Activity)?.window
      if (window != null) {
        val insetsController = WindowCompat.getInsetsController(window, view)
        insetsController.isAppearanceLightStatusBars = !isDark
        insetsController.isAppearanceLightNavigationBars = !isDark
      }
    }
  }

  val extended =
    CloudBoxExtendedColors(
      isDark = isDark,
      border = if (isDark) BorderDark else BorderLight,
      success = if (isDark) SuccessDark else SuccessLight,
      warning = if (isDark) WarningDark else WarningLight,
      textSecondary = if (isDark) TextSecondaryDark else TextSecondaryLight,
      toastBackground = ToastDarkFill,
      scrim = if (isDark) ScrimDark else ScrimLight,
      heroGradientStart = HeroGradientStart,
      heroGradientEnd = HeroGradientEnd,
      sepiaPaperBg = PaperSepiaBackground,
      sepiaPaperText = PaperSepiaText,
    )

  CompositionLocalProvider(LocalCloudBoxColors provides extended) {
    MaterialTheme(
      colorScheme = animatedScheme,
      typography = CloudBoxTypography,
      shapes = CloudBoxShapes,
      content = content,
    )
  }
}
