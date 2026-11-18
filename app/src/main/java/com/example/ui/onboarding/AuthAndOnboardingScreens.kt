package com.example.ui.onboarding

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.DriveUser
import com.example.ui.components.CloudBoxErrorCard
import com.example.ui.components.OfflineStatusBanner
import com.example.ui.theme.LocalCloudBoxColors
import kotlinx.coroutines.launch

@Composable
fun SplashScreen(
  timedOut: Boolean,
  onRetry: () -> Unit,
) {
  val extra = LocalCloudBoxColors.current
  var logoEntered by remember { mutableStateOf(false) }
  LaunchedEffect(Unit) { logoEntered = true }

  val scale by animateFloatAsState(
    targetValue = if (logoEntered) 1f else 0.8f,
    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
    label = "splashScale",
  )

  val bgBrush =
    if (extra.isDark) {
      Brush.verticalGradient(listOf(MaterialTheme.colorScheme.background, MaterialTheme.colorScheme.background))
    } else {
      Brush.linearGradient(listOf(extra.heroGradientStart, extra.heroGradientEnd))
    }

  Box(
    modifier = Modifier.fillMaxSize().background(bgBrush).safeDrawingPadding().testTag("splash_screen"),
    contentAlignment = Alignment.Center,
  ) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
      Surface(
        modifier = Modifier.size(92.dp).scale(scale),
        shape = RoundedCornerShape(26.dp),
        color = Color.White.copy(alpha = 0.16f),
        shadowElevation = 12.dp,
      ) {
        Box(contentAlignment = Alignment.Center) {
          Icon(
            imageVector = Icons.Filled.Cloud,
            contentDescription = "CloudBox logo",
            tint = Color.White,
            modifier = Modifier.size(48.dp),
          )
        }
      }
      Spacer(modifier = Modifier.height(20.dp))
      AnimatedVisibility(visible = logoEntered, enter = fadeIn(tween(400, delayMillis = 150))) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Text(
            text = "CloudBox",
            style = MaterialTheme.typography.displaySmall,
            color = Color.White,
          )
          Spacer(modifier = Modifier.height(6.dp))
          Text(
            text = "Your files, everywhere",
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White.copy(alpha = 0.85f),
          )
        }
      }
      Spacer(modifier = Modifier.height(36.dp))
      CircularProgressIndicator(
        color = Color.White,
        strokeWidth = 3.dp,
        modifier = Modifier.size(28.dp),
      )
      if (timedOut) {
        Spacer(modifier = Modifier.height(16.dp))
        Text(
          text = "Still loading…",
          style = MaterialTheme.typography.bodyMedium,
          color = Color.White.copy(alpha = 0.9f),
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedButton(
          onClick = onRetry,
          border = BorderStroke(1.dp, Color.White),
          colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
          modifier = Modifier.testTag("splash_retry_button"),
        ) {
          Text("Retry")
        }
      }
    }
  }
}

private data class OnboardingSlide(
  val title: String,
  val body: String,
  val icon: ImageVector,
  val badge: String,
)

@Composable
fun OnboardingScreen(
  onComplete: () -> Unit,
) {
  val extra = LocalCloudBoxColors.current
  val slides =
    remember {
      listOf(
        OnboardingSlide(
          title = "Store everything",
          body = "Documents, photos, video and audio safely organized in your Google Drive.",
          icon = Icons.Filled.CloudDone,
          badge = "Google Drive Storage",
        ),
        OnboardingSlide(
          title = "Share with control",
          body = "Invite collaborators as Viewer, Commenter or Editor and manage link access anytime.",
          icon = Icons.Filled.Share,
          badge = "Granular Permissions",
        ),
        OnboardingSlide(
          title = "Find it fast",
          body = "Instant search, smart type filters, nested breadcrumbs and offline pinned files.",
          icon = Icons.Filled.Search,
          badge = "Instant Search & Offline",
        ),
      )
    }

  val pagerState = rememberPagerState(pageCount = { slides.size })
  val scope = rememberCoroutineScope()

  BoxWithConstraints(
    modifier =
      Modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.background)
        .safeDrawingPadding()
        .testTag("onboarding_screen"),
  ) {
    val isWide = maxWidth >= 600.dp

    Column(
      modifier = Modifier.fillMaxSize().padding(horizontal = if (isWide) 32.dp else 20.dp, vertical = 16.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Row(
        modifier = Modifier.fillMaxWidth().widthIn(max = 880.dp),
        horizontalArrangement = Arrangement.End,
      ) {
        TextButton(
          onClick = onComplete,
          modifier = Modifier.height(48.dp).testTag("onboarding_skip_button"),
        ) {
          Text("Skip")
        }
      }

      HorizontalPager(
        state = pagerState,
        modifier = Modifier.weight(1f).widthIn(max = 880.dp),
      ) { page ->
        val slide = slides[page]
        if (isWide) {
          Row(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(32.dp),
          ) {
            OnboardingIllustrationCard(slide = slide, modifier = Modifier.weight(1f).height(260.dp))
            Column(modifier = Modifier.weight(1f)) {
              Text(slide.title, style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.onBackground)
              Spacer(modifier = Modifier.height(12.dp))
              Text(slide.body, style = MaterialTheme.typography.bodyLarge, color = extra.textSecondary)
            }
          }
        } else {
          Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
          ) {
            OnboardingIllustrationCard(slide = slide, modifier = Modifier.fillMaxWidth().height(220.dp))
            Spacer(modifier = Modifier.height(32.dp))
            Text(
              text = slide.title,
              style = MaterialTheme.typography.displaySmall,
              textAlign = TextAlign.Center,
              color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
              text = slide.body,
              style = MaterialTheme.typography.bodyLarge,
              textAlign = TextAlign.Center,
              color = extra.textSecondary,
              modifier = Modifier.padding(horizontal = 12.dp),
            )
          }
        }
      }

      Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 16.dp),
      ) {
        repeat(slides.size) { idx ->
          val active = pagerState.currentPage == idx
          val width by animateDpAsState(targetValue = if (active) 24.dp else 8.dp, label = "dotW")
          Box(
            modifier =
              Modifier
                .height(8.dp)
                .width(width)
                .clip(CircleShape)
                .background(
                  if (active) MaterialTheme.colorScheme.primary
                  else MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                ),
          )
        }
      }

      Button(
        onClick = {
          if (pagerState.currentPage < slides.lastIndex) {
            scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
          } else {
            onComplete()
          }
        },
        modifier =
          Modifier
            .fillMaxWidth()
            .widthIn(max = 420.dp)
            .height(52.dp)
            .testTag("onboarding_next_button"),
        shape = RoundedCornerShape(14.dp),
      ) {
        Text(
          text = if (pagerState.currentPage == slides.lastIndex) "Get started" else "Next",
          style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
        )
      }
      Spacer(modifier = Modifier.height(8.dp))
    }
  }
}

@Composable
private fun OnboardingIllustrationCard(slide: OnboardingSlide, modifier: Modifier = Modifier) {
  val extra = LocalCloudBoxColors.current
  Card(
    modifier = modifier,
    shape = RoundedCornerShape(24.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    border = BorderStroke(1.dp, extra.border),
  ) {
    Box(modifier = Modifier.fillMaxSize()) {
      Image(
        painter = painterResource(id = R.drawable.img_cloudbox_onboarding),
        contentDescription = slide.title,
        contentScale = ContentScale.Crop,
        modifier = Modifier.fillMaxSize(),
      )
      Surface(
        modifier = Modifier.align(Alignment.BottomStart).padding(14.dp),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Icon(
            imageVector = slide.icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp),
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = slide.badge,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
          )
        }
      }
    }
  }
}

@Composable
fun SignInScreen(
  isOnline: Boolean,
  isSigningIn: Boolean,
  errorMessage: String?,
  needsDrivePermissionForUser: DriveUser?,
  onContinueWithGoogle: () -> Unit,
  onConnectDrive: () -> Unit,
  onSignOut: () -> Unit,
) {
  val extra = LocalCloudBoxColors.current
  var showLegalDialog by remember { mutableStateOf<String?>(null) }

  Column(
    modifier =
      Modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.background)
        .safeDrawingPadding()
        .testTag("sign_in_screen"),
  ) {
    if (!isOnline) {
      OfflineStatusBanner()
    }

    Box(
      modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
      contentAlignment = Alignment.Center,
    ) {
      Card(
        modifier = Modifier.fillMaxWidth().widthIn(max = 420.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, extra.border),
      ) {
        Column(
          modifier = Modifier.padding(24.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
        ) {
          Box(
            modifier =
              Modifier
                .size(76.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(Brush.linearGradient(listOf(extra.heroGradientStart, extra.heroGradientEnd))),
            contentAlignment = Alignment.Center,
          ) {
            Icon(
              imageVector = if (needsDrivePermissionForUser != null) Icons.Filled.Lock else Icons.Filled.Cloud,
              contentDescription = "CloudBox",
              tint = Color.White,
              modifier = Modifier.size(40.dp),
            )
          }

          Spacer(modifier = Modifier.height(20.dp))

          if (needsDrivePermissionForUser != null) {
            Text(
              text = "Connect Google Drive",
              style = MaterialTheme.typography.headlineMedium,
              textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
              text = "Signed in as ${needsDrivePermissionForUser.email}. Grant Drive file access (drive.file) so CloudBox can store, preview, and organize your real files.",
              style = MaterialTheme.typography.bodyLarge,
              color = extra.textSecondary,
              textAlign = TextAlign.Center,
            )
          } else {
            Text(
              text = "Welcome to CloudBox",
              style = MaterialTheme.typography.headlineMedium,
              textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
              text = "Sign in to open your Google Drive",
              style = MaterialTheme.typography.bodyLarge,
              color = extra.textSecondary,
              textAlign = TextAlign.Center,
            )
          }

          if (!errorMessage.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(16.dp))
            CloudBoxErrorCard(
              message = errorMessage,
              actionLabel = "Retry",
              onRetry = if (needsDrivePermissionForUser != null) onConnectDrive else onContinueWithGoogle,
            )
          }

          Spacer(modifier = Modifier.height(24.dp))

          if (needsDrivePermissionForUser != null) {
            Button(
              onClick = onConnectDrive,
              enabled = isOnline && !isSigningIn,
              modifier = Modifier.fillMaxWidth().height(50.dp).testTag("connect_drive_button"),
              shape = RoundedCornerShape(14.dp),
            ) {
              Text("Grant Google Drive Access", style = MaterialTheme.typography.labelLarge)
            }
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedButton(
              onClick = onSignOut,
              modifier = Modifier.fillMaxWidth().height(48.dp).testTag("switch_google_account_button"),
              shape = RoundedCornerShape(14.dp),
            ) {
              Text("Use another Google account")
            }
          } else {
            Button(
              onClick = onContinueWithGoogle,
              enabled = isOnline && !isSigningIn,
              modifier = Modifier.fillMaxWidth().height(50.dp).testTag("google_sign_in_button"),
              shape = RoundedCornerShape(14.dp),
            ) {
              if (isSigningIn) {
                CircularProgressIndicator(
                  color = MaterialTheme.colorScheme.onPrimary,
                  strokeWidth = 2.dp,
                  modifier = Modifier.size(20.dp),
                )
              } else {
                Text(
                  text = "G  Continue with Google",
                  style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                )
              }
            }
          }

          Spacer(modifier = Modifier.height(16.dp))

          val legalText =
            buildAnnotatedString {
              append("By continuing you accept ")
              withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)) {
                append("Terms & Privacy Policy")
              }
            }
          Text(
            text = legalText,
            style = MaterialTheme.typography.bodySmall,
            color = extra.textSecondary,
            textAlign = TextAlign.Center,
            modifier =
              Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable { showLegalDialog = "Terms & Privacy Policy" }
                .padding(8.dp),
          )
        }
      }
    }
  }

  if (showLegalDialog != null) {
    AlertDialog(
      onDismissRequest = { showLegalDialog = null },
      title = { Text("Terms & Privacy Policy") },
      text = {
        Text(
          "CloudBox connects directly to your Google Drive using OAuth 2.0 (drive.file scope). " +
            "Your files remain in your own Google Drive account. Offline files and search history " +
            "you save in-app are stored privately on your device.",
          style = MaterialTheme.typography.bodyMedium,
        )
      },
      confirmButton = {
        TextButton(onClick = { showLegalDialog = null }) {
          Text("Close")
        }
      },
    )
  }
}
