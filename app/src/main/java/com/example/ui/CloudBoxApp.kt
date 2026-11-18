package com.example.ui

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.PermanentDrawerSheet
import androidx.compose.material3.PermanentNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.model.AppDestination
import com.example.model.AuthSessionState
import com.example.model.CloudFile
import com.example.model.DriveUser
import com.example.model.ScreenDataState
import com.example.ui.components.AppUpdateDialog
import com.example.ui.components.FileTypeTile
import com.example.ui.components.OfflineStatusBanner
import com.example.ui.components.UploadProgressOverlayCard
import com.example.ui.files.FilesScreen
import com.example.ui.home.HomeScreen
import com.example.ui.manage.OfflineFilesScreen
import com.example.ui.manage.SettingsScreen
import com.example.ui.manage.StorageScreen
import com.example.ui.manage.TrashScreen
import com.example.ui.onboarding.OnboardingScreen
import com.example.ui.onboarding.SignInScreen
import com.example.ui.onboarding.SplashScreen
import com.example.ui.search.SearchScreen
import com.example.ui.shared.SharedScreen
import com.example.ui.shared.StarredScreen
import com.example.ui.sheets.CreateUploadSheet
import com.example.ui.sheets.FileActionsSheet
import com.example.ui.sheets.FileDetailsContent
import com.example.ui.sheets.FileDetailsSheet
import com.example.ui.sheets.MoveToFolderDialog
import com.example.ui.sheets.NewFolderDialog
import com.example.ui.sheets.RenameFileDialog
import com.example.ui.sheets.ShareAndPermissionsSheet
import com.example.ui.sheets.SortAndFilterSheet
import com.example.ui.theme.CloudBoxTheme
import com.example.ui.theme.LocalCloudBoxColors
import com.example.ui.viewer.FileViewerScreen
import com.example.util.DocumentScannerUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private enum class WidthClass { COMPACT, MEDIUM, EXPANDED }
private enum class PendingCameraMode { PHOTO, DOCUMENT_SCAN }

@Composable
fun CloudBoxApp(viewModel: CloudBoxViewModel = viewModel()) {
  val context = LocalContext.current
  val credentialManager = remember { CredentialManager.create(context) }
  val scope = rememberCoroutineScope()

  val settings by viewModel.settings.collectAsStateWithLifecycle()
  val authState by viewModel.authState.collectAsStateWithLifecycle()
  val pendingConsent by viewModel.pendingConsentResult.collectAsStateWithLifecycle()
  val networkStatus by viewModel.networkStatus.collectAsStateWithLifecycle()
  val isSigningIn by viewModel.isSigningIn.collectAsStateWithLifecycle()
  val splashTimedOut by viewModel.splashTimedOut.collectAsStateWithLifecycle()

  val consentLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
      viewModel.onDriveConsentActivityResult(result.data)
    }

  LaunchedEffect(Unit) {
    viewModel.initializeSession(credentialManager)
  }

  LaunchedEffect(pendingConsent) {
    val pi = pendingConsent?.pendingIntent ?: return@LaunchedEffect
    viewModel.consumePendingConsent()
    consentLauncher.launch(IntentSenderRequest.Builder(pi.intentSender).build())
  }

  CloudBoxTheme(themePreference = settings.themePreference) {
    when {
      authState is AuthSessionState.Checking -> {
        SplashScreen(
          timedOut = splashTimedOut,
          onRetry = { viewModel.initializeSession(credentialManager) },
        )
      }
      !settings.onboardingCompleted -> {
        OnboardingScreen(onComplete = { viewModel.completeOnboarding() })
      }
      authState is AuthSessionState.SignedOut ||
        authState is AuthSessionState.Error ||
        authState is AuthSessionState.NeedsDrivePermission -> {
        val needsDriveUser = (authState as? AuthSessionState.NeedsDrivePermission)?.user
        val errMsg =
          when (val s = authState) {
            is AuthSessionState.Error -> s.message
            is AuthSessionState.NeedsDrivePermission -> s.errorMessage
            else -> null
          }
        SignInScreen(
          isOnline = networkStatus.isOnline,
          isSigningIn = isSigningIn,
          errorMessage = errMsg,
          needsDrivePermissionForUser = needsDriveUser,
          onContinueWithGoogle = { viewModel.signInWithGoogle(context, credentialManager) },
          onConnectDrive = {
            if (needsDriveUser != null) {
              viewModel.checkDriveAuthorization(needsDriveUser, context)
            }
          },
          onSignOut = { viewModel.signOut(credentialManager) },
        )
      }
      authState is AuthSessionState.Authenticated -> {
        val currentUser = (authState as AuthSessionState.Authenticated).user
        CloudBoxWorkspace(
          viewModel = viewModel,
          user = currentUser,
          onSignOut = { viewModel.signOut(credentialManager) },
        )
      }
    }
  }
}

@Composable
private fun CloudBoxWorkspace(
  viewModel: CloudBoxViewModel,
  user: DriveUser,
  onSignOut: () -> Unit,
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val snackbarHostState = remember { SnackbarHostState() }

  val settings by viewModel.settings.collectAsStateWithLifecycle()
  val networkStatus by viewModel.networkStatus.collectAsStateWithLifecycle()
  val activeTab by viewModel.activeTab.collectAsStateWithLifecycle()
  val quotaState by viewModel.quotaState.collectAsStateWithLifecycle()
  val recentState by viewModel.recentState.collectAsStateWithLifecycle()
  val homeQuickCategory by viewModel.homeQuickCategory.collectAsStateWithLifecycle()
  val breadcrumbs by viewModel.breadcrumbs.collectAsStateWithLifecycle()
  val filesState by viewModel.filesState.collectAsStateWithLifecycle()
  val filesFilter by viewModel.filesFilter.collectAsStateWithLifecycle()
  val filesNextToken by viewModel.filesNextPageToken.collectAsStateWithLifecycle()
  val isLoadingMore by viewModel.isLoadingMoreFiles.collectAsStateWithLifecycle()
  val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
  val selectedIds by viewModel.selectedIds.collectAsStateWithLifecycle()
  val sharedSegment by viewModel.sharedSegment.collectAsStateWithLifecycle()
  val sharedOwnerFilter by viewModel.sharedOwnerFilter.collectAsStateWithLifecycle()
  val sharedState by viewModel.sharedState.collectAsStateWithLifecycle()
  val openedSharedIds by viewModel.openedSharedIds.collectAsStateWithLifecycle()
  val starredState by viewModel.starredState.collectAsStateWithLifecycle()
  val trashState by viewModel.trashState.collectAsStateWithLifecycle()
  val storageBreakdownState by viewModel.storageBreakdownState.collectAsStateWithLifecycle()
  val offlineFiles by viewModel.offlineFiles.collectAsStateWithLifecycle()
  val recentSearches by viewModel.recentSearches.collectAsStateWithLifecycle()
  val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
  val searchCategory by viewModel.searchCategory.collectAsStateWithLifecycle()
  val searchState by viewModel.searchState.collectAsStateWithLifecycle()
  val uploadTasks by viewModel.uploadTasks.collectAsStateWithLifecycle()
  val uploadCollapsed by viewModel.uploadCardCollapsed.collectAsStateWithLifecycle()
  val permissionsState by viewModel.permissionsState.collectAsStateWithLifecycle()
  val isShareBusy by viewModel.isShareBusy.collectAsStateWithLifecycle()
  val cacheSizeBytes by viewModel.cacheSizeBytes.collectAsStateWithLifecycle()
  val availableFolders by viewModel.availableFolders.collectAsStateWithLifecycle()
  val selectedDetailFile by viewModel.selectedDetailFile.collectAsStateWithLifecycle()
  val viewModeOverride by viewModel.viewMode.collectAsStateWithLifecycle()
  val effectiveViewMode = viewModeOverride ?: settings.defaultViewMode

  // Active modals & full-screen overlays
  var showSearchOverlay by remember { mutableStateOf(false) }
  var viewerFile by remember { mutableStateOf<CloudFile?>(null) }
  var showCreateSheet by remember { mutableStateOf(false) }
  var showNewFolderDialog by remember { mutableStateOf(false) }
  var showSortFilterSheet by remember { mutableStateOf(false) }
  var actionSheetFile by remember { mutableStateOf<CloudFile?>(null) }
  var shareSheetFile by remember { mutableStateOf<CloudFile?>(null) }
  var detailsSheetFile by remember { mutableStateOf<CloudFile?>(null) }
  var renameDialogFile by remember { mutableStateOf<CloudFile?>(null) }
  var moveDialogFile by remember { mutableStateOf<CloudFile?>(null) }
  var pendingDownloadFile by remember { mutableStateOf<CloudFile?>(null) }
  var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }
  var pendingCameraMode by remember { mutableStateOf(PendingCameraMode.PHOTO) }

  // Toast collector
  LaunchedEffect(Unit) {
    viewModel.toastEvents.collect { event ->
      val res =
        snackbarHostState.showSnackbar(
          message = event.message,
          actionLabel = event.actionLabel,
          duration = if (event.actionLabel != null) SnackbarDuration.Long else SnackbarDuration.Short,
        )
      if (res == SnackbarResult.ActionPerformed) {
        event.onAction?.invoke()
      }
    }
  }

  // System pickers & camera launchers
  val uploadFilesLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
      if (uris.isNotEmpty()) viewModel.enqueueUploads(uris)
    }

  val uploadFolderLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
      if (uri != null) viewModel.uploadFolderTree(uri)
    }

  val photoPickerLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia()) { uris ->
      if (uris.isNotEmpty()) viewModel.enqueueUploads(uris)
    }

  val exportDocumentLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("*/*")) { targetUri ->
      val fileToSave = pendingDownloadFile
      pendingDownloadFile = null
      if (targetUri != null && fileToSave != null) {
        viewModel.exportFileToDevice(fileToSave, targetUri)
      }
    }

  val cameraTakePictureLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
      val capturedUri = pendingCameraUri
      pendingCameraUri = null
      if (success && capturedUri != null) {
        if (pendingCameraMode == PendingCameraMode.PHOTO) {
          viewModel.enqueueUploads(listOf(capturedUri))
        } else {
          scope.launch {
            viewModel.emitToast("Processing scanned document PDF…")
            val pdfFile = withContext(Dispatchers.IO) {
              DocumentScannerUtil.convertImageToScannedPdf(context, capturedUri)
            }
            if (pdfFile != null) {
              viewModel.enqueueScannedPdf(pdfFile)
            } else {
              viewModel.emitToast("Couldn't process scanned document")
            }
          }
        }
      }
    }

  val cameraPermissionLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
      if (granted) {
        val (uri, _) = viewModel.createTempCameraCapture()
        pendingCameraUri = uri
        cameraTakePictureLauncher.launch(uri)
      } else {
        viewModel.emitToast("Camera permission is needed to take a photo or scan documents")
      }
    }

  fun launchCamera(mode: PendingCameraMode) {
    pendingCameraMode = mode
    val hasPerm =
      ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
        PackageManager.PERMISSION_GRANTED
    if (hasPerm) {
      val (uri, _) = viewModel.createTempCameraCapture()
      pendingCameraUri = uri
      cameraTakePictureLauncher.launch(uri)
    } else {
      cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
    }
  }

  fun startDownload(file: CloudFile) {
    pendingDownloadFile = file
    exportDocumentLauncher.launch(file.name)
  }

  fun openFileOrFolder(file: CloudFile) {
    if (file.isFolder) {
      showSearchOverlay = false
      viewModel.openFolder(file)
    } else {
      if (activeTab == AppDestination.SHARED) viewModel.markSharedOpened(file.id)
      viewModel.selectDetailFile(file)
      viewerFile = file
    }
  }

  fun openShareSheet(file: CloudFile) {
    shareSheetFile = file
    viewModel.loadPermissions(file.id)
  }

  fun copyLinkToClipboard(link: String) {
    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    cm.setPrimaryClip(ClipData.newPlainText("CloudBox link", link))
    viewModel.emitToast("Link copied")
  }

  // Handle back navigation properly
  BackHandler(
    enabled =
      viewerFile != null ||
        showSearchOverlay ||
        (activeTab == AppDestination.FILES && breadcrumbs.size > 1) ||
        activeTab in setOf(AppDestination.OFFLINE, AppDestination.TRASH, AppDestination.STORAGE, AppDestination.SETTINGS),
  ) {
    when {
      viewerFile != null -> viewerFile = null
      showSearchOverlay -> showSearchOverlay = false
      activeTab == AppDestination.FILES && viewModel.navigateUpFolder() -> Unit
      activeTab in setOf(AppDestination.OFFLINE, AppDestination.TRASH, AppDestination.STORAGE, AppDestination.SETTINGS) -> {
        viewModel.selectTab(AppDestination.HOME)
      }
    }
  }

  val isStorageFull = (quotaState as? ScreenDataState.Success)?.data?.isAlmostFull == true
  val drawerState = rememberDrawerState(DrawerValue.Closed)

  BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
    val widthClass =
      when {
        maxWidth >= 840.dp -> WidthClass.EXPANDED
        maxWidth >= 600.dp -> WidthClass.MEDIUM
        else -> WidthClass.COMPACT
      }

    val mainContent: @Composable () -> Unit = {
      val fabRotation by animateFloatAsState(if (showCreateSheet) 45f else 0f, tween(200), label = "fabRot")
      val showPrimaryScaffoldChrome =
        viewerFile == null &&
          !showSearchOverlay &&
          activeTab in setOf(AppDestination.HOME, AppDestination.FILES, AppDestination.SHARED, AppDestination.STARRED)

      Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
          if (widthClass == WidthClass.COMPACT && showPrimaryScaffoldChrome) {
            CloudBoxBottomBar(
              activeTab = activeTab,
              onSelectTab = { viewModel.selectTab(it) },
            )
          }
        },
        floatingActionButton = {
          if (widthClass != WidthClass.EXPANDED && showPrimaryScaffoldChrome) {
            FloatingActionButton(
              onClick = { showCreateSheet = true },
              shape = RoundedCornerShape(20.dp),
              containerColor = MaterialTheme.colorScheme.primary,
              contentColor = MaterialTheme.colorScheme.onPrimary,
              modifier = Modifier.size(58.dp).testTag("create_upload_fab"),
            ) {
              Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = "Create or upload",
                modifier = Modifier.rotate(fabRotation),
              )
            }
          }
        },
      ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
          AnimatedVisibility(visible = !networkStatus.isOnline) {
            OfflineStatusBanner()
          }

          Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when {
              viewerFile != null -> {
                val activeFile = viewerFile!!
                val folderFiles = (filesState as? ScreenDataState.Success)?.data.orEmpty()
                val siblingImages = folderFiles.filter { it.category == com.example.model.FileCategory.IMAGE }
                FileViewerScreen(
                  file = activeFile,
                  siblingImages = siblingImages,
                  repository = viewModel.repository,
                  isOnline = networkStatus.isOnline,
                  autoplayMedia = settings.autoplayMedia,
                  keepScreenOn = settings.keepScreenOnViewer,
                  onBack = { viewerFile = null },
                  onSelectSiblingImage = { nextImg -> viewerFile = nextImg },
                  onOpenMoreActions = { actionSheetFile = it },
                  onDownload = { startDownload(it) },
                  onShare = { openShareSheet(it) },
                  onToggleStar = { viewModel.toggleStar(it) },
                  onToggleOffline = { viewModel.toggleOfflineAvailability(it) },
                  onShowDetails = { detailsSheetFile = it },
                  onShowToast = { viewModel.emitToast(it) },
                )
              }
              showSearchOverlay -> {
                SearchScreen(
                  query = searchQuery,
                  selectedCategory = searchCategory,
                  recentSearches = recentSearches,
                  searchState = searchState,
                  isOnline = networkStatus.isOnline,
                  imageLoader = viewModel.clientProvider.imageLoader,
                  onBack = { showSearchOverlay = false },
                  onQueryChange = { viewModel.updateSearchQuery(it) },
                  onSubmitSearch = { viewModel.submitSearch(it) },
                  onRemoveRecentSearch = { viewModel.removeRecentSearch(it) },
                  onSelectCategory = { viewModel.selectSearchCategory(it) },
                  onOpenFile = { openFileOrFolder(it) },
                  onFileMoreClick = { actionSheetFile = it },
                )
              }
              else -> {
                Row(modifier = Modifier.fillMaxSize()) {
                  if (widthClass == WidthClass.MEDIUM) {
                    CloudBoxNavigationRail(
                      activeTab = activeTab,
                      onSelectTab = { viewModel.selectTab(it) },
                      onCreateClick = { showCreateSheet = true },
                    )
                  }

                  Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    when (activeTab) {
                      AppDestination.HOME ->
                        HomeScreen(
                          user = user,
                          quotaState = quotaState,
                          recentState = recentState,
                          selectedQuickCategory = homeQuickCategory,
                          isRefreshing = isRefreshing,
                          imageLoader = viewModel.clientProvider.imageLoader,
                          showMenuButton = widthClass == WidthClass.COMPACT,
                          onOpenDrawer = { scope.launch { drawerState.open() } },
                          onOpenSearch = { showSearchOverlay = true },
                          onOpenSettings = { viewModel.selectTab(AppDestination.SETTINGS) },
                          onOpenStorage = { viewModel.selectTab(AppDestination.STORAGE) },
                          onSelectQuickCategory = { viewModel.toggleHomeQuickCategory(it) },
                          onOpenAllFiles = { viewModel.selectTab(AppDestination.FILES) },
                          onOpenFile = { openFileOrFolder(it) },
                          onFileMoreClick = { actionSheetFile = it },
                          onUploadFirstFile = { showCreateSheet = true },
                          onRefresh = { viewModel.refreshCurrentTab() },
                          onReconnectDrive = { viewModel.checkDriveAuthorization(user, context) },
                        )
                      AppDestination.FILES ->
                        FilesScreen(
                          user = user,
                          breadcrumbs = breadcrumbs,
                          filesState = filesState,
                          filter = filesFilter,
                          viewMode = effectiveViewMode,
                          selectedIds = selectedIds,
                          hasMorePages = filesNextToken != null,
                          isLoadingMore = isLoadingMore,
                          isRefreshing = isRefreshing,
                          imageLoader = viewModel.clientProvider.imageLoader,
                          showMenuButton = widthClass == WidthClass.COMPACT,
                          onOpenDrawer = { scope.launch { drawerState.open() } },
                          onNavigateUp = { viewModel.navigateUpFolder() },
                          onSelectBreadcrumb = { viewModel.navigateToBreadcrumb(it) },
                          onOpenSearch = { showSearchOverlay = true },
                          onOpenSettings = { viewModel.selectTab(AppDestination.SETTINGS) },
                          onOpenSortFilter = { showSortFilterSheet = true },
                          onToggleViewMode = { viewModel.toggleViewMode() },
                          onOpenFolder = { viewModel.openFolder(it) },
                          onOpenFile = { openFileOrFolder(it) },
                          onFileMoreClick = { actionSheetFile = it },
                          onToggleSelect = { viewModel.toggleSelection(it) },
                          onSelectAll = { viewModel.selectAllVisible(it) },
                          onClearSelection = { viewModel.clearSelection() },
                          onBatchStar = { viewModel.batchStarSelected(it) },
                          onBatchTrash = { viewModel.batchTrashSelected(it) },
                          onUploadClick = { uploadFilesLauncher.launch(arrayOf("*/*")) },
                          onNewFolderClick = { showNewFolderDialog = true },
                          onClearFilters = { viewModel.clearFilters() },
                          onLoadMore = { viewModel.loadMoreFiles() },
                          onRefresh = { viewModel.refreshCurrentTab() },
                          onReconnectDrive = { viewModel.checkDriveAuthorization(user, context) },
                        )
                      AppDestination.SHARED ->
                        SharedScreen(
                          user = user,
                          segment = sharedSegment,
                          selectedOwnerFilter = sharedOwnerFilter,
                          sharedState = sharedState,
                          openedSharedIds = openedSharedIds,
                          isRefreshing = isRefreshing,
                          imageLoader = viewModel.clientProvider.imageLoader,
                          showMenuButton = widthClass == WidthClass.COMPACT,
                          onOpenDrawer = { scope.launch { drawerState.open() } },
                          onSelectSegment = { viewModel.selectSharedSegment(it) },
                          onSelectOwnerFilter = { viewModel.selectSharedOwnerFilter(it) },
                          onOpenSortFilter = { showSortFilterSheet = true },
                          onOpenSearch = { showSearchOverlay = true },
                          onOpenSettings = { viewModel.selectTab(AppDestination.SETTINGS) },
                          onOpenFile = { openFileOrFolder(it) },
                          onFileMoreClick = { actionSheetFile = it },
                          onShareFileShortcut = { viewModel.selectTab(AppDestination.FILES) },
                          onRefresh = { viewModel.refreshCurrentTab() },
                          onReconnectDrive = { viewModel.checkDriveAuthorization(user, context) },
                        )
                      AppDestination.STARRED ->
                        StarredScreen(
                          user = user,
                          starredState = starredState,
                          filter = filesFilter,
                          viewMode = effectiveViewMode,
                          isRefreshing = isRefreshing,
                          imageLoader = viewModel.clientProvider.imageLoader,
                          showMenuButton = widthClass == WidthClass.COMPACT,
                          onOpenDrawer = { scope.launch { drawerState.open() } },
                          onOpenSearch = { showSearchOverlay = true },
                          onOpenSettings = { viewModel.selectTab(AppDestination.SETTINGS) },
                          onOpenSortFilter = { showSortFilterSheet = true },
                          onToggleViewMode = { viewModel.toggleViewMode() },
                          onOpenFile = { openFileOrFolder(it) },
                          onFileMoreClick = { actionSheetFile = it },
                          onBrowseFiles = { viewModel.selectTab(AppDestination.FILES) },
                          onRefresh = { viewModel.refreshCurrentTab() },
                          onReconnectDrive = { viewModel.checkDriveAuthorization(user, context) },
                        )
                      AppDestination.OFFLINE ->
                        OfflineFilesScreen(
                          offlineFiles = offlineFiles,
                          imageLoader = viewModel.clientProvider.imageLoader,
                          onBack = { viewModel.selectTab(AppDestination.HOME) },
                          onOpenFile = { openFileOrFolder(it) },
                          onRetryOffline = { viewModel.toggleOfflineAvailability(it) },
                          onRemoveOffline = { viewModel.toggleOfflineAvailability(it) },
                        )
                      AppDestination.TRASH ->
                        TrashScreen(
                          trashState = trashState,
                          imageLoader = viewModel.clientProvider.imageLoader,
                          onBack = { viewModel.selectTab(AppDestination.HOME) },
                          onRestore = { viewModel.restoreFromTrash(it) },
                          onDeleteForever = { viewModel.deletePermanently(it) },
                          onEmptyTrash = { viewModel.emptyTrash() },
                          onRetry = { viewModel.loadTrash() },
                        )
                      AppDestination.STORAGE ->
                        StorageScreen(
                          quotaState = quotaState,
                          breakdownState = storageBreakdownState,
                          onBack = { viewModel.selectTab(AppDestination.HOME) },
                          onSelectCategory = { cat -> viewModel.openFilesFilteredByCategory(cat) },
                          onOpenTrash = { viewModel.selectTab(AppDestination.TRASH) },
                          onRetry = { viewModel.loadStorageBreakdown() },
                        )
                      AppDestination.SETTINGS ->
                        SettingsScreen(
                          user = user,
                          quotaState = quotaState,
                          settings = settings,
                          cacheSizeBytes = cacheSizeBytes,
                          offlineFileCount = offlineFiles.size,
                          onBack = { viewModel.selectTab(AppDestination.HOME) },
                          onSetTheme = { pref -> scope.launch { viewModel.settingsRepo.setThemePreference(pref) } },
                          onSetDefaultView = { mode -> scope.launch { viewModel.settingsRepo.setDefaultViewMode(mode) } },
                          onSetWifiOnly = { on -> scope.launch { viewModel.settingsRepo.setWifiOnlyUploads(on) } },
                          onSetAutoplay = { on -> scope.launch { viewModel.settingsRepo.setAutoplayMedia(on) } },
                          onSetKeepScreenOn = { on -> scope.launch { viewModel.settingsRepo.setKeepScreenOnViewer(on) } },
                          onSetNotifyShare = { on -> scope.launch { viewModel.settingsRepo.setNotifyNewShare(on) } },
                          onSetNotifyUpload = { on -> scope.launch { viewModel.settingsRepo.setNotifyUploadFinished(on) } },
                          onOpenOfflineFiles = { viewModel.selectTab(AppDestination.OFFLINE) },
                          onOpenTrash = { viewModel.selectTab(AppDestination.TRASH) },
                          onClearCache = { viewModel.clearCache() },
                          onSignOut = onSignOut,
                        )
                      AppDestination.SEARCH -> Unit
                    }
                  }

                  // Expanded 360dp Right Detail Pane
                  if (widthClass == WidthClass.EXPANDED && selectedDetailFile != null && activeTab in setOf(
                      AppDestination.HOME,
                      AppDestination.FILES,
                      AppDestination.SHARED,
                      AppDestination.STARRED,
                    )
                  ) {
                    val detail = selectedDetailFile!!
                    Surface(
                      modifier = Modifier.width(340.dp).fillMaxHeight(),
                      color = MaterialTheme.colorScheme.surface,
                      border = BorderStroke(1.dp, LocalCloudBoxColors.current.border),
                    ) {
                      Column(
                        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                      ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                          FileTypeTile(
                            category = detail.category,
                            thumbnailLink = detail.thumbnailLink,
                            imageLoader = viewModel.clientProvider.imageLoader,
                            size = 48.dp,
                          )
                          Spacer(modifier = Modifier.width(12.dp))
                          Text(detail.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                          Button(onClick = { openFileOrFolder(detail) }, modifier = Modifier.weight(1f)) {
                            Text("Open")
                          }
                          OutlinedButton(onClick = { openShareSheet(detail) }, modifier = Modifier.weight(1f)) {
                            Text("Share")
                          }
                        }
                        HorizontalDivider(color = LocalCloudBoxColors.current.border)
                        FileDetailsContent(
                          file = detail,
                          locationName = breadcrumbs.lastOrNull()?.name ?: "My Files",
                        )
                      }
                    }
                  }
                }
              }
            }

            // Floating Upload Progress Card
            if (uploadTasks.isNotEmpty() && viewerFile == null) {
              UploadProgressOverlayCard(
                tasks = uploadTasks,
                collapsed = uploadCollapsed,
                onToggleCollapse = { viewModel.toggleUploadCardCollapsed() },
                onCancelTask = { viewModel.cancelUpload(it) },
                onRetryTask = { viewModel.retryUpload(it) },
                modifier =
                  Modifier
                    .align(Alignment.BottomEnd)
                    .padding(horizontal = 16.dp, vertical = 14.dp)
                    .then(if (widthClass == WidthClass.EXPANDED) Modifier.width(360.dp) else Modifier.fillMaxWidth()),
              )
            }
          }
        }
      }
    }

    if (widthClass == WidthClass.EXPANDED && viewerFile == null && !showSearchOverlay) {
      PermanentNavigationDrawer(
        drawerContent = {
          PermanentDrawerSheet(
            modifier = Modifier.width(268.dp),
            drawerContainerColor = MaterialTheme.colorScheme.surface,
          ) {
            DrawerContent(
              activeTab = activeTab,
              showCreateButton = true,
              onCreateClick = { showCreateSheet = true },
              onSelectTab = { viewModel.selectTab(it) },
            )
          }
        },
        content = mainContent,
      )
    } else {
      ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = viewerFile == null && !showSearchOverlay,
        drawerContent = {
          ModalDrawerSheet(drawerContainerColor = MaterialTheme.colorScheme.surface) {
            DrawerContent(
              activeTab = activeTab,
              showCreateButton = false,
              onCreateClick = {},
              onSelectTab = { dest ->
                scope.launch { drawerState.close() }
                viewModel.selectTab(dest)
              },
            )
          }
        },
        content = mainContent,
      )
    }
  }

  // Sheets & Dialogs
  if (showCreateSheet) {
    CreateUploadSheet(
      isOnline = networkStatus.isOnline,
      isStorageFull = isStorageFull,
      onDismiss = { showCreateSheet = false },
      onNewFolder = { showNewFolderDialog = true },
      onUploadFiles = { uploadFilesLauncher.launch(arrayOf("*/*")) },
      onUploadFolder = { uploadFolderLauncher.launch(null) },
      onUploadMedia = {
        photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo))
      },
      onTakePhoto = { launchCamera(PendingCameraMode.PHOTO) },
      onScanDocument = { launchCamera(PendingCameraMode.DOCUMENT_SCAN) },
    )
  }

  if (showNewFolderDialog) {
    NewFolderDialog(
      onDismiss = { showNewFolderDialog = false },
      onCreate = { name -> viewModel.createFolder(name) },
    )
  }

  if (showSortFilterSheet) {
    SortAndFilterSheet(
      currentFilter = filesFilter,
      onDismiss = { showSortFilterSheet = false },
      onApply = { viewModel.applyFilter(it) },
    )
  }

  actionSheetFile?.let { file ->
    FileActionsSheet(
      file = file,
      isOnline = networkStatus.isOnline,
      imageLoader = viewModel.clientProvider.imageLoader,
      onDismiss = { actionSheetFile = null },
      onShare = { openShareSheet(file) },
      onDownload = { startDownload(file) },
      onToggleStar = { viewModel.toggleStar(file) },
      onToggleOffline = { viewModel.toggleOfflineAvailability(file) },
      onRename = { renameDialogFile = file },
      onMove = {
        viewModel.loadFoldersForMovePicker()
        moveDialogFile = file
      },
      onMakeCopy = { viewModel.copyFile(file) },
      onDetails = { detailsSheetFile = file },
      onMoveToTrash = { viewModel.moveToTrash(file) },
      onRestoreFromTrash = { viewModel.restoreFromTrash(file) },
    )
  }

  shareSheetFile?.let { file ->
    ShareAndPermissionsSheet(
      file = file,
      currentUser = user,
      permissionsState = permissionsState,
      isBusy = isShareBusy,
      isOnline = networkStatus.isOnline,
      onDismiss = { shareSheetFile = null },
      onInvite = { emails, role -> viewModel.inviteEmails(file.id, emails, role) },
      onChangeRole = { permId, role -> viewModel.changePermissionRole(file.id, permId, role) },
      onRemovePermission = { permId -> viewModel.removePermission(file.id, permId) },
      onSetAnyoneLink = { enabled, role -> viewModel.setAnyoneLinkAccess(file.id, enabled, role) },
      onCopyLink = { copyLinkToClipboard(it) },
      onRetryLoad = { viewModel.loadPermissions(file.id) },
    )
  }

  detailsSheetFile?.let { file ->
    FileDetailsSheet(
      file = file,
      locationName = breadcrumbs.lastOrNull()?.name ?: "My Files",
      onDismiss = { detailsSheetFile = null },
    )
  }

  renameDialogFile?.let { file ->
    RenameFileDialog(
      file = file,
      onDismiss = { renameDialogFile = null },
      onConfirm = { newName -> viewModel.renameFile(file, newName) },
    )
  }

  moveDialogFile?.let { file ->
    MoveToFolderDialog(
      file = file,
      folders = availableFolders,
      onDismiss = { moveDialogFile = null },
      onMoveTo = { targetId, targetName -> viewModel.moveFile(file, targetId, targetName) },
    )
  }

  val availableUpdate by viewModel.availableUpdate.collectAsStateWithLifecycle()
  val updateDownloadProgress by viewModel.updateDownloadProgress.collectAsStateWithLifecycle()

  availableUpdate?.let { updateInfo ->
    AppUpdateDialog(
      updateInfo = updateInfo,
      downloadProgress = updateDownloadProgress,
      onDismiss = { viewModel.dismissUpdateDialog() },
      onStartUpdate = { viewModel.startAppUpdate() },
    )
  }
}

private data class NavItemMeta(
  val dest: AppDestination,
  val label: String,
  val icon: ImageVector,
)

private val primaryNavItems =
  listOf(
    NavItemMeta(AppDestination.HOME, "Home", Icons.Filled.Home),
    NavItemMeta(AppDestination.FILES, "Files", Icons.Filled.Folder),
    NavItemMeta(AppDestination.SHARED, "Shared", Icons.Filled.People),
    NavItemMeta(AppDestination.STARRED, "Starred", Icons.Filled.Star),
  )

private val secondaryNavItems =
  listOf(
    NavItemMeta(AppDestination.OFFLINE, "Offline files", Icons.Filled.CloudDownload),
    NavItemMeta(AppDestination.TRASH, "Trash", Icons.Filled.DeleteOutline),
    NavItemMeta(AppDestination.STORAGE, "Storage", Icons.Filled.PieChart),
    NavItemMeta(AppDestination.SETTINGS, "Settings", Icons.Filled.Settings),
  )

@Composable
private fun CloudBoxBottomBar(
  activeTab: AppDestination,
  onSelectTab: (AppDestination) -> Unit,
) {
  NavigationBar(
    containerColor = MaterialTheme.colorScheme.surface,
    tonalElevation = 0.dp,
  ) {
    primaryNavItems.forEach { item ->
      NavigationBarItem(
        selected = activeTab == item.dest,
        onClick = { onSelectTab(item.dest) },
        icon = { Icon(item.icon, contentDescription = item.label) },
        label = { Text(item.label) },
        modifier = Modifier.testTag("nav_tab_${item.dest.route}"),
      )
    }
  }
}

@Composable
private fun CloudBoxNavigationRail(
  activeTab: AppDestination,
  onSelectTab: (AppDestination) -> Unit,
  onCreateClick: () -> Unit,
) {
  NavigationRail(
    containerColor = MaterialTheme.colorScheme.surface,
    header = {
      FloatingActionButton(
        onClick = onCreateClick,
        shape = RoundedCornerShape(16.dp),
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        modifier = Modifier.padding(top = 8.dp).size(50.dp),
      ) {
        Icon(Icons.Filled.Add, contentDescription = "Create or upload")
      }
    },
  ) {
    (primaryNavItems + secondaryNavItems).forEach { item ->
      NavigationRailItem(
        selected = activeTab == item.dest,
        onClick = { onSelectTab(item.dest) },
        icon = { Icon(item.icon, contentDescription = item.label) },
        label = { Text(item.label, maxLines = 1) },
      )
    }
  }
}

@Composable
private fun DrawerContent(
  activeTab: AppDestination,
  showCreateButton: Boolean,
  onCreateClick: () -> Unit,
  onSelectTab: (AppDestination) -> Unit,
) {
  val extra = LocalCloudBoxColors.current
  Column(modifier = Modifier.fillMaxSize().padding(14.dp)) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp)) {
      Box(
        modifier =
          Modifier
            .size(38.dp)
            .clip(RoundedCornerShape(11.dp))
            .background(Brush.linearGradient(listOf(extra.heroGradientStart, extra.heroGradientEnd))),
        contentAlignment = Alignment.Center,
      ) {
        Icon(Icons.Filled.Cloud, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
      }
      Spacer(modifier = Modifier.width(12.dp))
      Text("CloudBox", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold))
    }

    if (showCreateButton) {
      Spacer(modifier = Modifier.height(6.dp))
      Button(
        onClick = onCreateClick,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("drawer_new_button"),
      ) {
        Icon(Icons.Filled.Add, contentDescription = null)
        Spacer(modifier = Modifier.width(8.dp))
        Text("New")
      }
      Spacer(modifier = Modifier.height(10.dp))
    }

    primaryNavItems.forEach { item ->
      NavigationDrawerItem(
        label = { Text(item.label) },
        selected = activeTab == item.dest,
        onClick = { onSelectTab(item.dest) },
        icon = { Icon(item.icon, contentDescription = null) },
        shape = RoundedCornerShape(14.dp),
      )
    }

    HorizontalDivider(color = extra.border, modifier = Modifier.padding(vertical = 8.dp))

    secondaryNavItems.forEach { item ->
      NavigationDrawerItem(
        label = { Text(item.label) },
        selected = activeTab == item.dest,
        onClick = { onSelectTab(item.dest) },
        icon = { Icon(item.icon, contentDescription = null) },
        shape = RoundedCornerShape(14.dp),
      )
    }
  }
}
