package com.example.ui

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.credentials.CredentialManager
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.auth.DriveAuthManager
import com.example.auth.DriveTokenAttemptResult
import com.example.auth.attemptAutoSignIn
import com.example.auth.onGoogleSignInClicked
import com.example.auth.signOutUser
import com.example.auth.toDriveUser
import com.example.data.DriveFileRepository
import com.example.data.FileRepository
import com.example.data.local.CloudBoxDatabase
import com.example.data.local.NetworkMonitor
import com.example.data.local.NetworkStatus
import com.example.data.local.SettingsRepository
import com.example.data.remote.DriveApiException
import com.example.data.remote.DriveClientProvider
import com.example.model.AccessRole
import com.example.model.AppDestination
import com.example.model.AuthSessionState
import com.example.model.BreadcrumbSegment
import com.example.model.CategoryStorageBreakdown
import com.example.model.CloudBoxSettings
import com.example.model.CloudFile
import com.example.model.DriveUser
import com.example.model.FileCategory
import com.example.model.FilterConfig
import com.example.model.ScreenDataState
import com.example.model.SharePermission
import com.example.model.SharedSegment
import com.example.model.SortField
import com.example.model.StorageQuotaInfo
import com.example.model.ThemePreference
import com.example.model.UploadStatus
import com.example.model.UploadTaskState
import com.example.model.ViewMode
import com.example.util.DocumentScannerUtil
import com.example.util.FormatUtils
import com.google.android.gms.auth.api.identity.AuthorizationResult
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import java.io.File
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ToastEvent(
  val message: String,
  val actionLabel: String? = null,
  val onAction: (() -> Unit)? = null,
)

class CloudBoxViewModel(application: Application) : AndroidViewModel(application) {
  private val appContext = application.applicationContext
  private val db = CloudBoxDatabase.getInstance(appContext)
  val settingsRepo = SettingsRepository(appContext)
  val authManager = DriveAuthManager(appContext)
  val clientProvider = DriveClientProvider(appContext, authManager)
  val repository: FileRepository = DriveFileRepository(appContext, db, authManager, clientProvider)
  private val networkMonitor = NetworkMonitor(appContext)

  val networkStatus: StateFlow<NetworkStatus> = networkMonitor.status
  val settings: StateFlow<CloudBoxSettings> =
    settingsRepo.settingsFlow.stateIn(viewModelScope, SharingStarted.Eagerly, CloudBoxSettings())

  val offlineFiles: StateFlow<List<CloudFile>> =
    repository.observeOfflineFiles().stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

  val recentSearches: StateFlow<List<String>> =
    repository.observeRecentSearches().stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

  private val _authState = MutableStateFlow<AuthSessionState>(AuthSessionState.Checking)
  val authState: StateFlow<AuthSessionState> = _authState.asStateFlow()

  private val _pendingConsentResult = MutableStateFlow<AuthorizationResult?>(null)
  val pendingConsentResult: StateFlow<AuthorizationResult?> = _pendingConsentResult.asStateFlow()

  private val _isSigningIn = MutableStateFlow(false)
  val isSigningIn: StateFlow<Boolean> = _isSigningIn.asStateFlow()

  private val _splashTimedOut = MutableStateFlow(false)
  val splashTimedOut: StateFlow<Boolean> = _splashTimedOut.asStateFlow()

  private val _activeTab = MutableStateFlow(AppDestination.HOME)
  val activeTab: StateFlow<AppDestination> = _activeTab.asStateFlow()

  private val _viewMode = MutableStateFlow<ViewMode?>(null)
  val viewMode: StateFlow<ViewMode?> = _viewMode.asStateFlow()

  // Toast channel
  private val _toastEvents = MutableSharedFlow<ToastEvent>(extraBufferCapacity = 8)
  val toastEvents = _toastEvents.asSharedFlow()

  // Home state
  private val _quotaState = MutableStateFlow<ScreenDataState<StorageQuotaInfo>>(ScreenDataState.Loading)
  val quotaState: StateFlow<ScreenDataState<StorageQuotaInfo>> = _quotaState.asStateFlow()

  private val _recentState = MutableStateFlow<ScreenDataState<List<CloudFile>>>(ScreenDataState.Loading)
  val recentState: StateFlow<ScreenDataState<List<CloudFile>>> = _recentState.asStateFlow()

  private val _homeQuickCategory = MutableStateFlow<FileCategory?>(null)
  val homeQuickCategory: StateFlow<FileCategory?> = _homeQuickCategory.asStateFlow()

  // Files & Nested Folder state
  private val _breadcrumbs = MutableStateFlow(listOf(BreadcrumbSegment("root", "My Files")))
  val breadcrumbs: StateFlow<List<BreadcrumbSegment>> = _breadcrumbs.asStateFlow()

  private val _filesState = MutableStateFlow<ScreenDataState<List<CloudFile>>>(ScreenDataState.Loading)
  val filesState: StateFlow<ScreenDataState<List<CloudFile>>> = _filesState.asStateFlow()

  private val _filesFilter = MutableStateFlow(FilterConfig())
  val filesFilter: StateFlow<FilterConfig> = _filesFilter.asStateFlow()

  private val _filesNextPageToken = MutableStateFlow<String?>(null)
  val filesNextPageToken: StateFlow<String?> = _filesNextPageToken.asStateFlow()

  private val _isLoadingMoreFiles = MutableStateFlow(false)
  val isLoadingMoreFiles: StateFlow<Boolean> = _isLoadingMoreFiles.asStateFlow()

  private val _isRefreshing = MutableStateFlow(false)
  val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

  // Multi-select
  private val _selectedIds = MutableStateFlow<Set<String>>(emptySet())
  val selectedIds: StateFlow<Set<String>> = _selectedIds.asStateFlow()

  // Shared state
  private val _sharedSegment = MutableStateFlow(SharedSegment.WITH_ME)
  val sharedSegment: StateFlow<SharedSegment> = _sharedSegment.asStateFlow()

  private val _sharedOwnerFilter = MutableStateFlow<String?>(null)
  val sharedOwnerFilter: StateFlow<String?> = _sharedOwnerFilter.asStateFlow()

  private val _sharedState = MutableStateFlow<ScreenDataState<List<CloudFile>>>(ScreenDataState.Loading)
  val sharedState: StateFlow<ScreenDataState<List<CloudFile>>> = _sharedState.asStateFlow()

  private val _openedSharedIds = MutableStateFlow<Set<String>>(emptySet())
  val openedSharedIds: StateFlow<Set<String>> = _openedSharedIds.asStateFlow()

  // Starred state
  private val _starredState = MutableStateFlow<ScreenDataState<List<CloudFile>>>(ScreenDataState.Loading)
  val starredState: StateFlow<ScreenDataState<List<CloudFile>>> = _starredState.asStateFlow()

  // Trash state
  private val _trashState = MutableStateFlow<ScreenDataState<List<CloudFile>>>(ScreenDataState.Loading)
  val trashState: StateFlow<ScreenDataState<List<CloudFile>>> = _trashState.asStateFlow()

  // Storage screen state
  private val _storageBreakdownState =
    MutableStateFlow<ScreenDataState<Pair<List<CategoryStorageBreakdown>, Long>>>(ScreenDataState.Loading)
  val storageBreakdownState: StateFlow<ScreenDataState<Pair<List<CategoryStorageBreakdown>, Long>>> =
    _storageBreakdownState.asStateFlow()

  // Search state
  private val _searchQuery = MutableStateFlow("")
  val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

  private val _searchCategory = MutableStateFlow<FileCategory?>(null)
  val searchCategory: StateFlow<FileCategory?> = _searchCategory.asStateFlow()

  private val _searchState = MutableStateFlow<ScreenDataState<List<CloudFile>>>(ScreenDataState.Empty)
  val searchState: StateFlow<ScreenDataState<List<CloudFile>>> = _searchState.asStateFlow()

  // Upload queue
  private val _uploadTasks = MutableStateFlow<List<UploadTaskState>>(emptyList())
  val uploadTasks: StateFlow<List<UploadTaskState>> = _uploadTasks.asStateFlow()

  private val _uploadCardCollapsed = MutableStateFlow(false)
  val uploadCardCollapsed: StateFlow<Boolean> = _uploadCardCollapsed.asStateFlow()

  private val cancelledUploadIds = ConcurrentHashMap.newKeySet<String>()

  // Share permissions state
  private val _permissionsState = MutableStateFlow<ScreenDataState<List<SharePermission>>>(ScreenDataState.Loading)
  val permissionsState: StateFlow<ScreenDataState<List<SharePermission>>> = _permissionsState.asStateFlow()

  private val _isShareBusy = MutableStateFlow(false)
  val isShareBusy: StateFlow<Boolean> = _isShareBusy.asStateFlow()

  // Cache size for settings
  private val _cacheSizeBytes = MutableStateFlow(0L)
  val cacheSizeBytes: StateFlow<Long> = _cacheSizeBytes.asStateFlow()

  // Folder picker folders for Move dialog
  private val _availableFolders = MutableStateFlow<List<CloudFile>>(emptyList())
  val availableFolders: StateFlow<List<CloudFile>> = _availableFolders.asStateFlow()

  // Selected file for Expanded detail pane or Viewer
  private val _selectedDetailFile = MutableStateFlow<CloudFile?>(null)
  val selectedDetailFile: StateFlow<CloudFile?> = _selectedDetailFile.asStateFlow()

  private var searchJob: Job? = null

  init {
    viewModelScope.launch {
      delay(3000)
      if (_authState.value is AuthSessionState.Checking) {
        _splashTimedOut.value = true
      }
    }
    viewModelScope.launch {
      networkStatus.collectLatest { net ->
        if (net.isOnline) {
          retryWaitingWifiUploads(net)
        }
      }
    }
    refreshCacheSize()
  }

  override fun onCleared() {
    super.onCleared()
    networkMonitor.unregister()
  }

  fun effectiveViewMode(): ViewMode = _viewMode.value ?: settings.value.defaultViewMode

  fun toggleViewMode() {
    val next = if (effectiveViewMode() == ViewMode.GRID) ViewMode.LIST else ViewMode.GRID
    _viewMode.value = next
    viewModelScope.launch { settingsRepo.setDefaultViewMode(next) }
  }

  fun initializeSession(credentialManager: CredentialManager) {
    _splashTimedOut.value = false
    _authState.value = AuthSessionState.Checking
    val currentUser = Firebase.auth.currentUser
    if (currentUser != null) {
      checkDriveAuthorization(currentUser.toDriveUser())
      return
    }
    attemptAutoSignIn(
      context = appContext,
      credentialManager = credentialManager,
      onAuthSuccess = { user -> checkDriveAuthorization(user) },
      onUnauthenticated = { _authState.value = AuthSessionState.SignedOut },
      scope = viewModelScope,
    )
  }

  fun signInWithGoogle(activityContext: Context, credentialManager: CredentialManager) {
    _isSigningIn.value = true
    onGoogleSignInClicked(
      context = activityContext,
      credentialManager = credentialManager,
      onAuthSuccess = { user ->
        _isSigningIn.value = false
        checkDriveAuthorization(user, activityContext)
      },
      onAuthError = { msg ->
        _isSigningIn.value = false
        _authState.value = AuthSessionState.Error(msg)
      },
      onAuthCancelled = {
        _isSigningIn.value = false
        _authState.value = AuthSessionState.Error("Sign-in was cancelled")
      },
      scope = viewModelScope,
    )
  }

  fun checkDriveAuthorization(user: DriveUser, activityOrContext: Context = appContext) {
    viewModelScope.launch {
      when (val res = authManager.requestDriveAuthorization(activityOrContext)) {
        is DriveTokenAttemptResult.Token -> {
          _pendingConsentResult.value = null
          _authState.value = AuthSessionState.Authenticated(user)
          loadAllInitialData()
        }
        is DriveTokenAttemptResult.NeedsConsent -> {
          _pendingConsentResult.value = res.authorizationResult
          _authState.value = AuthSessionState.NeedsDrivePermission(user)
        }
        is DriveTokenAttemptResult.Error -> {
          _authState.value = AuthSessionState.NeedsDrivePermission(user, res.message)
        }
      }
    }
  }

  fun consumePendingConsent() {
    _pendingConsentResult.value = null
  }

  fun onDriveConsentActivityResult(intentData: Intent?) {
    val user = Firebase.auth.currentUser?.toDriveUser()
    if (user == null) {
      _authState.value = AuthSessionState.SignedOut
      return
    }
    val res = authManager.extractTokenFromConsentIntent(appContext, intentData)
    res.fold(
      onSuccess = {
        _pendingConsentResult.value = null
        _authState.value = AuthSessionState.Authenticated(user)
        loadAllInitialData()
      },
      onFailure = { err ->
        _authState.value =
          AuthSessionState.NeedsDrivePermission(
            user,
            err.localizedMessage ?: "Google Drive access was cancelled.",
          )
      },
    )
  }

  fun signOut(credentialManager: CredentialManager) {
    signOutUser(credentialManager, authManager, viewModelScope) {
      _breadcrumbs.value = listOf(BreadcrumbSegment("root", "My Files"))
      _selectedIds.value = emptySet()
      _selectedDetailFile.value = null
      _authState.value = AuthSessionState.SignedOut
    }
  }

  fun completeOnboarding() {
    viewModelScope.launch { settingsRepo.setOnboardingCompleted(true) }
  }

  fun selectTab(tab: AppDestination) {
    _activeTab.value = tab
    _selectedIds.value = emptySet()
    when (tab) {
      AppDestination.HOME -> loadHome()
      AppDestination.FILES -> loadFolder(currentFolderId())
      AppDestination.SHARED -> loadShared()
      AppDestination.STARRED -> loadStarred()
      AppDestination.TRASH -> loadTrash()
      AppDestination.STORAGE -> loadStorageBreakdown()
      AppDestination.SETTINGS -> refreshCacheSize()
      AppDestination.OFFLINE, AppDestination.SEARCH -> Unit
    }
  }

  fun loadAllInitialData() {
    loadHome()
    loadFolder(currentFolderId())
    loadShared()
    loadStarred()
  }

  fun refreshCurrentTab() {
    viewModelScope.launch {
      _isRefreshing.value = true
      when (_activeTab.value) {
        AppDestination.HOME -> loadHome()
        AppDestination.FILES -> loadFolder(currentFolderId())
        AppDestination.SHARED -> loadShared()
        AppDestination.STARRED -> loadStarred()
        AppDestination.TRASH -> loadTrash()
        AppDestination.STORAGE -> loadStorageBreakdown()
        else -> Unit
      }
      _isRefreshing.value = false
    }
  }

  fun loadHome() {
    viewModelScope.launch {
      val online = networkStatus.value.isOnline
      if (_recentState.value !is ScreenDataState.Success) {
        _recentState.value = ScreenDataState.Loading
      }
      if (_quotaState.value !is ScreenDataState.Success) {
        _quotaState.value = ScreenDataState.Loading
      }

      if (online) {
        try {
          val (aboutUser, quota) = repository.getAboutStorageAndUser()
          if (aboutUser != null && _authState.value is AuthSessionState.Authenticated) {
            _authState.value = AuthSessionState.Authenticated(aboutUser)
          }
          _quotaState.value = ScreenDataState.Success(quota)
        } catch (e: Exception) {
          _quotaState.value = toErrorState(e)
        }
      } else {
        _quotaState.value = ScreenDataState.Error("Offline: storage quota unavailable", isAuthExpired = false)
      }

      try {
        val recent = repository.listRecentFiles(isOnline = online)
        _recentState.value = if (recent.isEmpty()) ScreenDataState.Empty else ScreenDataState.Success(recent)
      } catch (e: Exception) {
        _recentState.value = toErrorState(e)
      }
    }
  }

  fun toggleHomeQuickCategory(cat: FileCategory?) {
    _homeQuickCategory.value = if (_homeQuickCategory.value == cat) null else cat
  }

  fun currentFolderId(): String = _breadcrumbs.value.lastOrNull()?.id ?: "root"

  fun openFolder(folder: CloudFile) {
    val current = _breadcrumbs.value
    if (current.lastOrNull()?.id == folder.id) return
    _breadcrumbs.value = current + BreadcrumbSegment(folder.id, folder.name)
    _selectedIds.value = emptySet()
    _activeTab.value = AppDestination.FILES
    loadFolder(folder.id)
  }

  fun navigateToBreadcrumb(index: Int) {
    if (index !in _breadcrumbs.value.indices) return
    _breadcrumbs.value = _breadcrumbs.value.take(index + 1)
    _selectedIds.value = emptySet()
    loadFolder(currentFolderId())
  }

  fun navigateUpFolder(): Boolean {
    val current = _breadcrumbs.value
    if (current.size <= 1) return false
    _breadcrumbs.value = current.dropLast(1)
    _selectedIds.value = emptySet()
    loadFolder(currentFolderId())
    return true
  }

  fun openFilesFilteredByCategory(category: FileCategory) {
    _filesFilter.value =
      FilterConfig(
        sortField = SortField.SIZE,
        ascending = false,
        categories = setOf(category),
      )
    _breadcrumbs.value = listOf(BreadcrumbSegment("root", "My Files"))
    _activeTab.value = AppDestination.FILES
    loadFolder("root")
  }

  fun loadFolder(folderId: String = currentFolderId()) {
    viewModelScope.launch {
      if (_filesState.value !is ScreenDataState.Success) {
        _filesState.value = ScreenDataState.Loading
      }
      try {
        val result =
          repository.listFolderFiles(
            folderId = folderId,
            filter = _filesFilter.value,
            pageToken = null,
            isOnline = networkStatus.value.isOnline,
          )
        _filesNextPageToken.value = result.nextPageToken
        _filesState.value =
          if (result.files.isEmpty()) ScreenDataState.Empty else ScreenDataState.Success(result.files)
      } catch (e: Exception) {
        _filesState.value = toErrorState(e)
      }
    }
  }

  fun loadMoreFiles() {
    val token = _filesNextPageToken.value ?: return
    if (_isLoadingMoreFiles.value || !networkStatus.value.isOnline) return
    viewModelScope.launch {
      _isLoadingMoreFiles.value = true
      try {
        val result =
          repository.listFolderFiles(
            folderId = currentFolderId(),
            filter = _filesFilter.value,
            pageToken = token,
            isOnline = true,
          )
        _filesNextPageToken.value = result.nextPageToken
        val existing = (_filesState.value as? ScreenDataState.Success)?.data.orEmpty()
        val merged = (existing + result.files).distinctBy { it.id }
        _filesState.value = ScreenDataState.Success(merged)
      } catch (e: Exception) {
        emitToast(e.localizedMessage ?: "Couldn't load more files")
      } finally {
        _isLoadingMoreFiles.value = false
      }
    }
  }

  fun applyFilter(filter: FilterConfig) {
    _filesFilter.value = filter
    when (_activeTab.value) {
      AppDestination.FILES -> loadFolder(currentFolderId())
      AppDestination.SHARED -> loadShared()
      AppDestination.STARRED -> loadStarred()
      else -> Unit
    }
  }

  fun clearFilters() {
    applyFilter(FilterConfig(sortField = settings.value.defaultSortField, ascending = settings.value.defaultSortAscending))
  }

  fun toggleSelection(fileId: String) {
    val current = _selectedIds.value
    _selectedIds.value = if (fileId in current) current - fileId else current + fileId
  }

  fun selectAllVisible(files: List<CloudFile>) {
    _selectedIds.value = files.map { it.id }.toSet()
  }

  fun clearSelection() {
    _selectedIds.value = emptySet()
  }

  fun selectSharedSegment(segment: SharedSegment) {
    _sharedSegment.value = segment
    _sharedOwnerFilter.value = null
    loadShared()
  }

  fun selectSharedOwnerFilter(ownerName: String?) {
    _sharedOwnerFilter.value = if (_sharedOwnerFilter.value == ownerName) null else ownerName
  }

  fun markSharedOpened(fileId: String) {
    _openedSharedIds.value = _openedSharedIds.value + fileId
  }

  fun loadShared() {
    viewModelScope.launch {
      if (_sharedState.value !is ScreenDataState.Success) {
        _sharedState.value = ScreenDataState.Loading
      }
      try {
        val online = networkStatus.value.isOnline
        val items =
          when (_sharedSegment.value) {
            SharedSegment.WITH_ME -> repository.listSharedWithMe(_filesFilter.value, online)
            SharedSegment.BY_ME -> repository.listSharedByMe(_filesFilter.value, online)
          }
        _sharedState.value = if (items.isEmpty()) ScreenDataState.Empty else ScreenDataState.Success(items)
      } catch (e: Exception) {
        _sharedState.value = toErrorState(e)
      }
    }
  }

  fun loadStarred() {
    viewModelScope.launch {
      if (_starredState.value !is ScreenDataState.Success) {
        _starredState.value = ScreenDataState.Loading
      }
      try {
        val items = repository.listStarredFiles(_filesFilter.value, networkStatus.value.isOnline)
        _starredState.value = if (items.isEmpty()) ScreenDataState.Empty else ScreenDataState.Success(items)
      } catch (e: Exception) {
        _starredState.value = toErrorState(e)
      }
    }
  }

  fun loadTrash() {
    viewModelScope.launch {
      _trashState.value = ScreenDataState.Loading
      try {
        val items = repository.listTrashedFiles(networkStatus.value.isOnline)
        _trashState.value = if (items.isEmpty()) ScreenDataState.Empty else ScreenDataState.Success(items)
      } catch (e: Exception) {
        _trashState.value = toErrorState(e)
      }
    }
  }

  fun loadStorageBreakdown() {
    viewModelScope.launch {
      _storageBreakdownState.value = ScreenDataState.Loading
      try {
        val (aboutUser, quota) = repository.getAboutStorageAndUser()
        if (aboutUser != null && _authState.value is AuthSessionState.Authenticated) {
          _authState.value = AuthSessionState.Authenticated(aboutUser)
        }
        _quotaState.value = ScreenDataState.Success(quota)
        val breakdown = repository.listAllNonTrashedForStorage(networkStatus.value.isOnline)
        _storageBreakdownState.value = ScreenDataState.Success(breakdown)
      } catch (e: Exception) {
        _storageBreakdownState.value = toErrorState(e)
      }
    }
  }

  fun updateSearchQuery(newQuery: String) {
    _searchQuery.value = newQuery
    triggerDebouncedSearch()
  }

  fun selectSearchCategory(category: FileCategory?) {
    _searchCategory.value = category
    triggerDebouncedSearch(immediate = true)
  }

  fun submitSearch(query: String) {
    _searchQuery.value = query
    viewModelScope.launch { repository.saveRecentSearch(query) }
    triggerDebouncedSearch(immediate = true)
  }

  fun removeRecentSearch(query: String) {
    viewModelScope.launch { repository.removeRecentSearch(query) }
  }

  private fun triggerDebouncedSearch(immediate: Boolean = false) {
    searchJob?.cancel()
    val q = _searchQuery.value.trim()
    val cat = _searchCategory.value
    if (q.isEmpty() && cat == null) {
      _searchState.value = ScreenDataState.Empty
      return
    }
    searchJob =
      viewModelScope.launch {
        if (!immediate) delay(250)
        _searchState.value = ScreenDataState.Loading
        try {
          val filter = FilterConfig(categories = cat?.let { setOf(it) } ?: emptySet())
          val results = repository.searchFiles(q, filter, networkStatus.value.isOnline)
          _searchState.value =
            if (results.isEmpty()) ScreenDataState.Empty else ScreenDataState.Success(results)
        } catch (e: Exception) {
          _searchState.value = toErrorState(e)
        }
      }
  }

  suspend fun createFolder(name: String): Result<CloudFile> {
    return try {
      val created = repository.createFolder(name, currentFolderId())
      emitToast("Folder “${created.name}” created")
      loadFolder(currentFolderId())
      Result.success(created)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  fun enqueueUploads(uris: List<Uri>, overrideNames: Map<Uri, String> = emptyMap()) {
    if (uris.isEmpty()) return
    val parentId = currentFolderId()
    _uploadCardCollapsed.value = false
    val net = networkStatus.value
    val wifiOnly = settings.value.wifiOnlyUploads

    val newTasks =
      uris.map { uri ->
        val (name, size, mime) = resolveUriMetadata(uri, overrideNames[uri])
        val initialStatus =
          when {
            !net.isOnline -> UploadStatus.FAILED
            wifiOnly && !net.isUnmeteredWifi -> UploadStatus.WAITING_WIFI
            else -> UploadStatus.QUEUED
          }
        UploadTaskState(
          id = UUID.randomUUID().toString(),
          fileName = name,
          mimeType = mime,
          sourceUriString = uri.toString(),
          parentFolderId = parentId,
          totalBytes = size,
          uploadedBytes = 0L,
          status = initialStatus,
          errorMessage = if (!net.isOnline) "No connection" else null,
        )
      }

    _uploadTasks.value = _uploadTasks.value + newTasks
    newTasks.filter { it.status == UploadStatus.QUEUED }.forEach { task ->
      launchUploadTask(task)
    }
  }

  fun enqueueScannedPdf(pdfFile: File) {
    val uri = Uri.fromFile(pdfFile)
    enqueueUploads(listOf(uri), mapOf(uri to pdfFile.name))
  }

  fun uploadFolderTree(treeUri: Uri) {
    val parentId = currentFolderId()
    viewModelScope.launch {
      try {
        runCatching {
          appContext.contentResolver.takePersistableUriPermission(
            treeUri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION
          )
        }

        val rootDoc = DocumentFile.fromTreeUri(appContext, treeUri)
        if (rootDoc == null || !rootDoc.isDirectory) {
          emitToast("Could not access folder")
          return@launch
        }

        val folderName = rootDoc.name?.takeIf { it.isNotBlank() } ?: "Uploaded Folder"
        emitToast("Uploading folder “$folderName”...")
        _uploadCardCollapsed.value = false

        // Create or find top-level folder in Google Drive
        val rootDriveFolder = repository.getOrCreateFolder(folderName, parentId)

        var totalFiles = 0
        var totalFolders = 1

        suspend fun traverse(docDir: DocumentFile, driveParentId: String) {
          val children = docDir.listFiles()
          val dirs = children.filter { it.isDirectory }
          val files = children.filter { it.isFile }

          if (files.isNotEmpty()) {
            val net = networkStatus.value
            val wifiOnly = settings.value.wifiOnlyUploads
            val tasks = files.map { fileDoc ->
              val name = fileDoc.name ?: "Untitled"
              val mime = fileDoc.type ?: "application/octet-stream"
              val size = fileDoc.length()
              val initialStatus = when {
                !net.isOnline -> UploadStatus.FAILED
                wifiOnly && !net.isUnmeteredWifi -> UploadStatus.WAITING_WIFI
                else -> UploadStatus.QUEUED
              }
              UploadTaskState(
                id = UUID.randomUUID().toString(),
                fileName = name,
                mimeType = mime,
                sourceUriString = fileDoc.uri.toString(),
                parentFolderId = driveParentId,
                totalBytes = size,
                uploadedBytes = 0L,
                status = initialStatus,
                errorMessage = if (!net.isOnline) "No connection" else null,
              )
            }
            totalFiles += tasks.size
            _uploadTasks.value = _uploadTasks.value + tasks
            tasks.filter { it.status == UploadStatus.QUEUED }.forEach { task ->
              launchUploadTask(task)
            }
          }

          for (subDir in dirs) {
            val subName = subDir.name?.takeIf { it.isNotBlank() } ?: "Folder"
            val subDriveFolder = repository.getOrCreateFolder(subName, driveParentId)
            totalFolders++
            traverse(subDir, subDriveFolder.id)
          }
        }

        traverse(rootDoc, rootDriveFolder.id)
        emitToast("Queued $totalFiles file(s) across $totalFolders folder(s)")
        loadFolder(currentFolderId())
      } catch (e: Exception) {
        emitToast("Folder upload failed: ${e.localizedMessage}")
      }
    }
  }

  fun cancelUpload(taskId: String) {
    cancelledUploadIds.add(taskId)
    _uploadTasks.value = _uploadTasks.value.filterNot { it.id == taskId }
  }

  fun retryUpload(taskId: String) {
    val task = _uploadTasks.value.firstOrNull { it.id == taskId } ?: return
    val net = networkStatus.value
    if (!net.isOnline) {
      emitToast("No internet connection")
      return
    }
    if (settings.value.wifiOnlyUploads && !net.isUnmeteredWifi) {
      updateTask(task.copy(status = UploadStatus.WAITING_WIFI, errorMessage = null))
      return
    }
    val reset = task.copy(status = UploadStatus.QUEUED, uploadedBytes = 0L, errorMessage = null)
    updateTask(reset)
    launchUploadTask(reset)
  }

  fun toggleUploadCardCollapsed() {
    _uploadCardCollapsed.value = !_uploadCardCollapsed.value
  }

  fun dismissCompletedUploads() {
    _uploadTasks.value = _uploadTasks.value.filter { it.status != UploadStatus.COMPLETED }
  }

  private fun retryWaitingWifiUploads(net: NetworkStatus) {
    if (!net.isOnline) return
    val wifiOnly = settings.value.wifiOnlyUploads
    if (wifiOnly && !net.isUnmeteredWifi) return
    val waiting = _uploadTasks.value.filter { it.status == UploadStatus.WAITING_WIFI }
    waiting.forEach { task ->
      val queued = task.copy(status = UploadStatus.QUEUED, errorMessage = null)
      updateTask(queued)
      launchUploadTask(queued)
    }
  }

  private fun launchUploadTask(task: UploadTaskState) {
    cancelledUploadIds.remove(task.id)
    viewModelScope.launch {
      updateTask(task.copy(status = UploadStatus.UPLOADING, errorMessage = null))
      try {
        repository.uploadFileFromUri(
          uri = Uri.parse(task.sourceUriString),
          fileName = task.fileName,
          mimeType = task.mimeType,
          parentFolderId = task.parentFolderId,
          onProgress = { uploaded, total ->
            updateTaskById(task.id) { current ->
              current.copy(uploadedBytes = uploaded, totalBytes = total.coerceAtLeast(uploaded))
            }
          },
          isCancelled = { cancelledUploadIds.contains(task.id) },
        )
        updateTaskById(task.id) { current ->
          current.copy(
            uploadedBytes = current.totalBytes.coerceAtLeast(1L),
            status = UploadStatus.COMPLETED,
          )
        }
        loadFolder(currentFolderId())
        loadHome()

        // Auto-collapse/dismiss 2s after all uploads complete
        delay(2000)
        val remainingActive = _uploadTasks.value.any {
          it.status == UploadStatus.UPLOADING || it.status == UploadStatus.QUEUED
        }
        val completedCount = _uploadTasks.value.count { it.status == UploadStatus.COMPLETED }
        if (!remainingActive && completedCount > 0) {
          emitToast(if (completedCount == 1) "1 file uploaded" else "$completedCount files uploaded")
          dismissCompletedUploads()
        }
      } catch (e: Exception) {
        if (!cancelledUploadIds.contains(task.id)) {
          val reason =
            when {
              !networkStatus.value.isOnline -> "No connection"
              e is DriveApiException && e.code == 403 -> "No space or permission denied"
              e is DriveApiException && e.code == 413 -> "Too large"
              else -> e.localizedMessage ?: "Upload failed"
            }
          updateTaskById(task.id) { current ->
            current.copy(status = UploadStatus.FAILED, errorMessage = reason)
          }
        }
      }
    }
  }

  private fun updateTask(updated: UploadTaskState) {
    _uploadTasks.value = _uploadTasks.value.map { if (it.id == updated.id) updated else it }
  }

  private fun updateTaskById(id: String, transform: (UploadTaskState) -> UploadTaskState) {
    _uploadTasks.value = _uploadTasks.value.map { if (it.id == id) transform(it) else it }
  }

  fun toggleStar(file: CloudFile) {
    val target = !file.starred
    optimisticUpdateFile(file.copy(starred = target))
    viewModelScope.launch {
      try {
        val updated = repository.setStarred(file.id, target)
        optimisticUpdateFile(updated)
        if (!target && _activeTab.value == AppDestination.STARRED) {
          emitToast("Removed star", actionLabel = "Undo") { toggleStar(updated) }
          loadStarred()
        } else {
          emitToast(if (target) "Added star" else "Removed star")
        }
      } catch (e: Exception) {
        optimisticUpdateFile(file) // rollback
        emitToast(e.localizedMessage ?: "Couldn't update star")
      }
    }
  }

  fun renameFile(file: CloudFile, newName: String) {
    if (newName.isBlank() || newName.trim() == file.name) return
    val trimmed = newName.trim()
    optimisticUpdateFile(file.copy(name = trimmed))
    viewModelScope.launch {
      try {
        val updated = repository.renameFile(file.id, trimmed)
        optimisticUpdateFile(updated)
        emitToast("Renamed to “${updated.name}”")
      } catch (e: Exception) {
        optimisticUpdateFile(file)
        emitToast(e.localizedMessage ?: "Couldn't rename file")
      }
    }
  }

  fun loadFoldersForMovePicker() {
    viewModelScope.launch {
      try {
        val allFolders =
          repository.searchFiles(
            query = "",
            filter = FilterConfig(categories = setOf(FileCategory.FOLDER)),
            isOnline = networkStatus.value.isOnline,
          )
        _availableFolders.value = allFolders
      } catch (_: Exception) {
      }
    }
  }

  fun moveFile(file: CloudFile, targetFolderId: String, targetFolderName: String) {
    viewModelScope.launch {
      try {
        repository.moveFile(file.id, file.parentId, targetFolderId)
        emitToast("Moved to $targetFolderName")
        loadFolder(currentFolderId())
      } catch (e: Exception) {
        emitToast(e.localizedMessage ?: "Couldn't move file")
      }
    }
  }

  fun copyFile(file: CloudFile) {
    viewModelScope.launch {
      try {
        if (file.isFolder) {
          emitToast("Copying folder “${file.name}”...")
          val copied = repository.copyFolder(file.id, "Copy of ${file.name}", currentFolderId())
          emitToast("Saved copy “${copied.name}”")
        } else {
          val copied = repository.copyFile(file.id, "Copy of ${file.name}", currentFolderId())
          emitToast("Saved copy “${copied.name}”")
        }
        refreshCurrentTab()
      } catch (e: Exception) {
        emitToast(e.localizedMessage ?: "Couldn't make a copy")
      }
    }
  }

  fun moveToTrash(file: CloudFile) {
    viewModelScope.launch {
      try {
        repository.setTrashed(file.id, true)
        if (_selectedDetailFile.value?.id == file.id) _selectedDetailFile.value = null
        refreshCurrentTab()
        emitToast("Moved to trash", actionLabel = "Undo") { restoreFromTrash(file) }
      } catch (e: Exception) {
        emitToast(e.localizedMessage ?: "Couldn't move to trash")
      }
    }
  }

  fun batchTrashSelected(files: List<CloudFile>) {
    val ids = _selectedIds.value
    val targets = files.filter { it.id in ids }
    if (targets.isEmpty()) return
    _selectedIds.value = emptySet()
    viewModelScope.launch {
      var trashedCount = 0
      targets.forEach { file ->
        runCatching { repository.setTrashed(file.id, true) }.onSuccess { trashedCount++ }
      }
      refreshCurrentTab()
      emitToast("Moved $trashedCount item(s) to trash")
    }
  }

  fun batchStarSelected(files: List<CloudFile>) {
    val ids = _selectedIds.value
    val targets = files.filter { it.id in ids }
    if (targets.isEmpty()) return
    val makeStarred = targets.any { !it.starred }
    _selectedIds.value = emptySet()
    viewModelScope.launch {
      targets.forEach { file ->
        runCatching { repository.setStarred(file.id, makeStarred) }
      }
      refreshCurrentTab()
      emitToast(if (makeStarred) "Starred ${targets.size} item(s)" else "Removed star")
    }
  }

  fun restoreFromTrash(file: CloudFile) {
    viewModelScope.launch {
      try {
        repository.setTrashed(file.id, false)
        loadTrash()
        loadFolder(currentFolderId())
        emitToast("Restored “${file.name}”")
      } catch (e: Exception) {
        emitToast(e.localizedMessage ?: "Couldn't restore file")
      }
    }
  }

  fun deletePermanently(file: CloudFile) {
    viewModelScope.launch {
      try {
        repository.deletePermanently(file.id)
        loadTrash()
        emitToast("Deleted forever")
      } catch (e: Exception) {
        emitToast(e.localizedMessage ?: "Couldn't delete file")
      }
    }
  }

  fun emptyTrash() {
    viewModelScope.launch {
      try {
        repository.emptyTrash()
        loadTrash()
        loadHome()
        emitToast("Trash emptied")
      } catch (e: Exception) {
        emitToast(e.localizedMessage ?: "Couldn't empty trash")
      }
    }
  }

  fun toggleOfflineAvailability(file: CloudFile) {
    viewModelScope.launch {
      if (file.offlineStatus == com.example.model.OfflineSyncStatus.SAVED) {
        repository.removeOfflineFile(file.id)
        optimisticUpdateFile(file.copy(offlineStatus = com.example.model.OfflineSyncStatus.NONE, localOfflinePath = null))
        emitToast("Removed from offline files")
      } else {
        try {
          emitToast("Saving “${file.name}” for offline use…")
          val saved = repository.makeAvailableOffline(file)
          optimisticUpdateFile(saved)
          emitToast("Saved offline")
        } catch (e: Exception) {
          emitToast(e.localizedMessage ?: "Couldn't save file offline")
        }
      }
    }
  }

  fun exportFileToDevice(file: CloudFile, targetUri: Uri) {
    viewModelScope.launch {
      try {
        repository.exportFileToUri(file, targetUri)
        emitToast("Saved “${file.name}” to device")
      } catch (e: Exception) {
        emitToast(e.localizedMessage ?: "Download failed")
      }
    }
  }

  fun loadPermissions(fileId: String) {
    viewModelScope.launch {
      _permissionsState.value = ScreenDataState.Loading
      try {
        val perms = repository.listPermissions(fileId)
        _permissionsState.value = ScreenDataState.Success(perms)
      } catch (e: Exception) {
        _permissionsState.value = toErrorState(e)
      }
    }
  }

  fun inviteEmails(fileId: String, emails: List<String>, role: AccessRole) {
    if (emails.isEmpty()) return
    viewModelScope.launch {
      _isShareBusy.value = true
      var lastError: String? = null
      emails.forEach { email ->
        try {
          repository.inviteCollaborator(fileId, email, role)
        } catch (e: Exception) {
          lastError = e.localizedMessage ?: "Couldn't share. Check $email"
        }
      }
      _isShareBusy.value = false
      if (lastError != null) {
        emitToast(lastError!!)
      } else {
        emitToast("Invited ${emails.size} ${if (emails.size == 1) "person" else "people"}")
      }
      loadPermissions(fileId)
      loadShared()
    }
  }

  fun changePermissionRole(fileId: String, permissionId: String, role: AccessRole) {
    viewModelScope.launch {
      _isShareBusy.value = true
      try {
        repository.updatePermissionRole(fileId, permissionId, role)
        loadPermissions(fileId)
        loadShared()
      } catch (e: Exception) {
        emitToast(e.localizedMessage ?: "Couldn't update role")
      } finally {
        _isShareBusy.value = false
      }
    }
  }

  fun removePermission(fileId: String, permissionId: String) {
    viewModelScope.launch {
      _isShareBusy.value = true
      try {
        repository.removePermission(fileId, permissionId)
        loadPermissions(fileId)
        loadShared()
        emitToast("Access removed")
      } catch (e: Exception) {
        emitToast(e.localizedMessage ?: "Couldn't remove access")
      } finally {
        _isShareBusy.value = false
      }
    }
  }

  fun setAnyoneLinkAccess(fileId: String, enabled: Boolean, role: AccessRole) {
    viewModelScope.launch {
      _isShareBusy.value = true
      try {
        val updated = repository.setAnyoneLinkAccess(fileId, enabled, role)
        _permissionsState.value = ScreenDataState.Success(updated)
        loadShared()
      } catch (e: Exception) {
        emitToast(e.localizedMessage ?: "Couldn't update link access")
      } finally {
        _isShareBusy.value = false
      }
    }
  }

  fun selectDetailFile(file: CloudFile?) {
    _selectedDetailFile.value = file
  }

  fun refreshCacheSize() {
    viewModelScope.launch {
      _cacheSizeBytes.value = repository.getViewerCacheSizeBytes()
    }
  }

  fun clearCache() {
    viewModelScope.launch {
      val freed = repository.clearViewerCache()
      _cacheSizeBytes.value = 0L
      emitToast("Cleared ${FormatUtils.formatBytes(freed)} cache")
    }
  }

  fun emitToast(message: String, actionLabel: String? = null, onAction: (() -> Unit)? = null) {
    _toastEvents.tryEmit(ToastEvent(message, actionLabel, onAction))
  }

  private fun optimisticUpdateFile(updated: CloudFile) {
    fun replaceInList(state: ScreenDataState<List<CloudFile>>): ScreenDataState<List<CloudFile>> =
      if (state is ScreenDataState.Success) {
        ScreenDataState.Success(state.data.map { if (it.id == updated.id) updated else it })
      } else {
        state
      }

    _filesState.value = replaceInList(_filesState.value)
    _recentState.value = replaceInList(_recentState.value)
    _starredState.value = replaceInList(_starredState.value)
    _sharedState.value = replaceInList(_sharedState.value)
    if (_selectedDetailFile.value?.id == updated.id) {
      _selectedDetailFile.value = updated
    }
  }

  private fun toErrorState(e: Throwable): ScreenDataState.Error {
    return if (e is DriveApiException) {
      ScreenDataState.Error(
        message = e.message,
        code = e.code,
        isAuthExpired = e.isAuthExpired,
        isPermissionDenied = e.isPermissionDenied,
      )
    } else {
      ScreenDataState.Error(message = e.localizedMessage ?: "Something went wrong")
    }
  }

  private fun resolveUriMetadata(uri: Uri, overrideName: String?): Triple<String, Long, String> {
    if (uri.scheme == "file") {
      val f = uri.path?.let { File(it) }
      val name = overrideName ?: f?.name ?: "File_${System.currentTimeMillis()}"
      val mime = if (name.endsWith(".pdf", true)) "application/pdf" else "application/octet-stream"
      return Triple(name, f?.length() ?: 0L, mime)
    }
    var name = overrideName ?: "Upload_${System.currentTimeMillis()}"
    var size = 0L
    appContext.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)?.use { c ->
      if (c.moveToFirst()) {
        val nameIdx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        val sizeIdx = c.getColumnIndex(OpenableColumns.SIZE)
        if (overrideName == null && nameIdx >= 0 && !c.isNull(nameIdx)) {
          name = c.getString(nameIdx)
        }
        if (sizeIdx >= 0 && !c.isNull(sizeIdx)) {
          size = c.getLong(sizeIdx)
        }
      }
    }
    val mime = appContext.contentResolver.getType(uri) ?: guessMimeFromName(name)
    return Triple(name, size, mime)
  }

  private fun guessMimeFromName(name: String): String =
    when (name.substringAfterLast('.', "").lowercase()) {
      "pdf" -> "application/pdf"
      "jpg", "jpeg" -> "image/jpeg"
      "png" -> "image/png"
      "mp4" -> "video/mp4"
      "mp3" -> "audio/mpeg"
      "csv" -> "text/csv"
      "txt" -> "text/plain"
      "zip" -> "application/zip"
      else -> "application/octet-stream"
    }

  fun createTempCameraCapture(): Pair<Uri, File> = DocumentScannerUtil.createTempCameraImageUri(appContext)
}
