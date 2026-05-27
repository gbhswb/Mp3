package com.example.ui

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.MediaItem
import com.example.data.MediaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MediaViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val repository = MediaRepository(application, database.mediaDao())
    private val prefs: SharedPreferences = application.getSharedPreferences("mr_player_settings", Context.MODE_PRIVATE)
    private val isScanning = java.util.concurrent.atomic.AtomicBoolean(false)

    // Search Query state
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    // Selected folder string (or null if browsing root folder tree)
    private val _selectedFolder = MutableStateFlow<String?>(null)
    val selectedFolder = _selectedFolder.asStateFlow()

    // General Sorting
    private val _sortBy = MutableStateFlow(SortOption.DATE_ADDED_DESC)
    val sortBy = _sortBy.asStateFlow()

    // Favorite toggle filter
    private val _showFavoritesOnly = MutableStateFlow(false)
    val showFavoritesOnly = _showFavoritesOnly.asStateFlow()

    // --- Dynamic Layout Options ---
    private val _mrFlixLayoutStyle = MutableStateFlow(prefs.getInt("mr_flix_layout", 0)) // 0: 3D Stack, 1: Grid, 2: List
    val mrFlixLayoutStyle = _mrFlixLayoutStyle.asStateFlow()

    private val _vidsLayoutStyle = MutableStateFlow(prefs.getInt("vids_layout", 1)) // 0: Large 1-Col, 1: Compact 2-Col, 2: Horizontal List
    val vidsLayoutStyle = _vidsLayoutStyle.asStateFlow()

    private val _libraryLayoutStyle = MutableStateFlow(prefs.getInt("library_layout", 0)) // 0: Folder Grid, 1: Folder List
    val libraryLayoutStyle = _libraryLayoutStyle.asStateFlow()

    // --- Library Browser Rules ---
    private val _librarySortOption = MutableStateFlow(prefs.getInt("library_sort", 0)) // 0: Name A-Z, 1: Name Z-A, 2: Last Modified, 3: Video Count
    val librarySortOption = _librarySortOption.asStateFlow()

    private val _showHiddenFolders = MutableStateFlow(prefs.getBoolean("show_hidden_folders", false))
    val showHiddenFolders = _showHiddenFolders.asStateFlow()

    private val _pinnedFolders = MutableStateFlow(prefs.getStringSet("pinned_folders", emptySet()) ?: emptySet())
    val pinnedFolders = _pinnedFolders.asStateFlow()

    private val _hiddenFolders = MutableStateFlow(prefs.getStringSet("hidden_folders", emptySet()) ?: emptySet())
    val hiddenFolders = _hiddenFolders.asStateFlow()

    // --- Privacy Vault States ---
    private val _vaultPin = MutableStateFlow(prefs.getString("vault_pin", "") ?: "")
    val vaultPin = _vaultPin.asStateFlow()

    private val _isVaultUnlocked = MutableStateFlow(false)
    val isVaultUnlocked = _isVaultUnlocked.asStateFlow()

    private val _isFingerprintEnabled = MutableStateFlow(prefs.getBoolean("vault_biometric", false))
    val isFingerprintEnabled = _isFingerprintEnabled.asStateFlow()

    // --- Customizable Player Preferences ---
    private val _playerThemeColor = MutableStateFlow(prefs.getInt("player_theme_color", 0)) // 0: Neon Green, 1: Hot Pink, 2: Electric Cyan, 3: Sun Amber, 4: Crimson Red, 5: Royal Blue
    val playerThemeColor = _playerThemeColor.asStateFlow()

    private val _playerControlsLayout = MutableStateFlow(prefs.getInt("player_controls_layout", 1)) // 0: Classic Center, 1: Compact Bottom
    val playerControlsLayout = _playerControlsLayout.asStateFlow()

    fun setPlayerThemeColor(colorIndex: Int) {
        _playerThemeColor.value = colorIndex
        prefs.edit().putInt("player_theme_color", colorIndex).apply()
    }

    fun setPlayerControlsLayout(layoutIndex: Int) {
        _playerControlsLayout.value = layoutIndex
        prefs.edit().putInt("player_controls_layout", layoutIndex).apply()
    }

    // --- Customizable Scanner & Player Layout Customizations ---
    private val _mrFlixScanFolder = MutableStateFlow(prefs.getString("mr_flix_scan_folder", "Flex") ?: "Flex")
    val mrFlixScanFolder = _mrFlixScanFolder.asStateFlow()

    private val _vidsScanFolder = MutableStateFlow(prefs.getString("vids_scan_folder", "Vids") ?: "Vids")
    val vidsScanFolder = _vidsScanFolder.asStateFlow()

    private val _isSeekBarBelowButtons = MutableStateFlow(prefs.getBoolean("seekbar_below_buttons", false))
    val isSeekBarBelowButtons = _isSeekBarBelowButtons.asStateFlow()

    private val _mrFlixCategoryFilter = MutableStateFlow(prefs.getInt("mr_flix_category_filter", 0)) // 0: All, 1: Movies Only, 2: Series Only
    val mrFlixCategoryFilter = _mrFlixCategoryFilter.asStateFlow()

    // Tab-specific filter/sort options added dynamically to meet user needs
    private val _mrFlixSortOption = MutableStateFlow(prefs.getInt("mrFlixSortOption", 0)) // 0: Name A-Z, 1: Name Z-A, 2: Rating, 3: Duration, 4: Size
    val mrFlixSortOption = _mrFlixSortOption.asStateFlow()

    private val _vidsSortOption = MutableStateFlow(prefs.getInt("vidsSortOption", 0)) // 0: Name A-Z, 1: Name Z-A, 2: Duration, 3: Size, 4: Date Added
    val vidsSortOption = _vidsSortOption.asStateFlow()

    private val _vidsDurationFilter = MutableStateFlow(prefs.getInt("vidsDurationFilter", 0)) // 0: All, 1: Short Clips (<1m), 2: Long Videos (>10m)
    val vidsDurationFilter = _vidsDurationFilter.asStateFlow()

    private val _vidsCategoryFilter = MutableStateFlow(prefs.getInt("vidsCategoryFilter", 0)) // 0: All, 1: Favorites Only
    val vidsCategoryFilter = _vidsCategoryFilter.asStateFlow()

    private val _libraryFilterMode = MutableStateFlow(prefs.getInt("libraryFilterMode", 0)) // 0: Folders Grid, 1: All Video Files List, 2: Folders inside Folders / Nested Files
    val libraryFilterMode = _libraryFilterMode.asStateFlow()

    // Format Filters (Video & Audio)
    private val _videoFormatFilter = MutableStateFlow(prefs.getStringSet("video_format_filter", emptySet()) ?: emptySet())
    val videoFormatFilter = _videoFormatFilter.asStateFlow()

    private val _audioFormatFilter = MutableStateFlow(prefs.getStringSet("audio_format_filter", emptySet()) ?: emptySet())
    val audioFormatFilter = _audioFormatFilter.asStateFlow()

    fun getMediaExtension(item: MediaItem): String {
        val path = item.path.lowercase()
        if (path.endsWith(".mp4")) return "mp4"
        if (path.endsWith(".mkv")) return "mkv"
        if (path.endsWith(".webm")) return "webm"
        if (path.endsWith(".3gp")) return "3gp"
        if (path.endsWith(".avi")) return "avi"
        if (path.endsWith(".mp3")) return "mp3"
        if (path.endsWith(".aac")) return "aac"
        if (path.endsWith(".m4a")) return "m4a"
        if (path.endsWith(".wav")) return "wav"
        
        // Fallback to title
        val title = item.title.lowercase()
        if (title.contains(".mp4") || title.contains(" mp4")) return "mp4"
        if (title.contains(".mkv") || title.contains(" mkv")) return "mkv"
        if (title.contains(".webm") || title.contains(" webm")) return "webm"
        if (title.contains(".avi") || title.contains(" avi")) return "avi"
        if (title.contains(".mp3") || title.contains(" mp3")) return "mp3"
        if (title.contains(".aac") || title.contains(" aac")) return "aac"
        if (title.contains(".m4a") || title.contains(" m4a")) return "m4a"
        if (title.contains(".wav") || title.contains(" wav")) return "wav"
        
        return "mp4" // Default
    }

    fun toggleVideoFormat(format: String) {
        val updated = _videoFormatFilter.value.toMutableSet()
        val formatLower = format.lowercase()
        if (updated.contains(formatLower)) {
            updated.remove(formatLower)
        } else {
            updated.add(formatLower)
        }
        _videoFormatFilter.value = updated
        prefs.edit().putStringSet("video_format_filter", updated).apply()
    }

    fun toggleAudioFormat(format: String) {
        val updated = _audioFormatFilter.value.toMutableSet()
        val formatLower = format.lowercase()
        if (updated.contains(formatLower)) {
            updated.remove(formatLower)
        } else {
            updated.add(formatLower)
        }
        _audioFormatFilter.value = updated
        prefs.edit().putStringSet("audio_format_filter", updated).apply()
    }

    fun clearFormatFilters() {
        _videoFormatFilter.value = emptySet()
        _audioFormatFilter.value = emptySet()
        prefs.edit().remove("video_format_filter").remove("audio_format_filter").apply()
    }

    fun setMrFlixSortOption(option: Int) {
        _mrFlixSortOption.value = option
        prefs.edit().putInt("mrFlixSortOption", option).apply()
    }

    fun setVidsSortOption(option: Int) {
        _vidsSortOption.value = option
        prefs.edit().putInt("vidsSortOption", option).apply()
    }

    fun setVidsDurationFilter(filter: Int) {
        _vidsDurationFilter.value = filter
        prefs.edit().putInt("vidsDurationFilter", filter).apply()
    }

    fun setVidsCategoryFilter(filter: Int) {
        _vidsCategoryFilter.value = filter
        prefs.edit().putInt("vidsCategoryFilter", filter).apply()
    }

    fun setLibraryFilterMode(mode: Int) {
        _libraryFilterMode.value = mode
        prefs.edit().putInt("libraryFilterMode", mode).apply()
    }

    fun setMrFlixScanFolder(folderName: String) {
        val trimmed = folderName.trim()
        if (trimmed.isNotEmpty()) {
            _mrFlixScanFolder.value = trimmed
            prefs.edit().putString("mr_flix_scan_folder", trimmed).apply()
            scanMedia()
        }
    }

    fun setVidsScanFolder(folderName: String) {
        val trimmed = folderName.trim()
        if (trimmed.isNotEmpty()) {
            _vidsScanFolder.value = trimmed
            prefs.edit().putString("vids_scan_folder", trimmed).apply()
            scanMedia()
        }
    }

    fun setSeekBarBelowButtons(below: Boolean) {
        _isSeekBarBelowButtons.value = below
        prefs.edit().putBoolean("seekbar_below_buttons", below).apply()
    }

    fun setMrFlixCategoryFilter(filter: Int) {
        _mrFlixCategoryFilter.value = filter
        prefs.edit().putInt("mr_flix_category_filter", filter).apply()
    }

    // Unsaved temporary batch multi-selection
    private val _selectedItemIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedItemIds = _selectedItemIds.asStateFlow()

    // --- State Stream of raw items ---
    val rawMediaList = repository.allMedia.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Flow reflecting folders (non-empty folders, non-secure)
    val folders = repository.allFolders.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Central Filtered Media Flow
    val mediaList: StateFlow<List<MediaItem>> = combine(
        rawMediaList,
        _searchQuery,
        _selectedFolder,
        _showFavoritesOnly,
        combine(_sortBy, _videoFormatFilter, _audioFormatFilter) { sort, vFmts, aFmts ->
            Triple(sort, vFmts, aFmts)
        }
    ) { allItems, query, folder, favOnly, triple ->
        val sort = triple.first
        val videoFormats = triple.second
        val audioFormats = triple.third

        // Exclude secure vault files from main feed
        var filtered = allItems.filter { !it.isSecure }

        // Format selector logic
        val hasVideoFilters = videoFormats.isNotEmpty()
        val hasAudioFilters = audioFormats.isNotEmpty()
        if (hasVideoFilters || hasAudioFilters) {
            filtered = filtered.filter { item ->
                val f = getMediaExtension(item)
                (hasVideoFilters && videoFormats.contains(f)) || (hasAudioFilters && audioFormats.contains(f))
            }
        }

        if (query.isNotEmpty()) {
            filtered = filtered.filter {
                it.title.contains(query, ignoreCase = true) ||
                        it.folder.contains(query, ignoreCase = true)
            }
        }

        if (folder != null) {
            filtered = filtered.filter { it.folder == folder }
        }

        if (favOnly) {
            filtered = filtered.filter { it.isFavorite }
        }

        when (sort) {
            SortOption.TITLE_ASC -> filtered.sortedBy { it.title.lowercase() }
            SortOption.TITLE_DESC -> filtered.sortedByDescending { it.title.lowercase() }
            SortOption.DATE_ADDED_DESC -> filtered.sortedByDescending { it.dateAdded }
            SortOption.SIZE_DESC -> filtered.sortedByDescending { it.size }
            SortOption.DURATION_DESC -> filtered.sortedByDescending { it.duration }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    init {
        scanMedia()
    }

    fun scanMedia() {
        if (isScanning.getAndSet(true)) {
            // Already scanning, skip to prevent SQLite write conflicts & deadlocks!
            return
        }
        viewModelScope.launch {
            try {
                repository.scanLocalVideos(
                    mrFlixFolder = _mrFlixScanFolder.value,
                    vidsFolder = _vidsScanFolder.value
                )
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isScanning.set(false)
            }
        }
    }

    // --- Set Preferences & Modes ---
    fun setMrFlixLayoutStyle(style: Int) {
        _mrFlixLayoutStyle.value = style
        prefs.edit().putInt("mr_flix_layout", style).apply()
    }

    fun setVidsLayoutStyle(style: Int) {
        _vidsLayoutStyle.value = style
        prefs.edit().putInt("vids_layout", style).apply()
    }

    fun setLibraryLayoutStyle(style: Int) {
        _libraryLayoutStyle.value = style
        prefs.edit().putInt("library_layout", style).apply()
    }

    fun setLibrarySortOption(option: Int) {
        _librarySortOption.value = option
        prefs.edit().putInt("library_sort", option).apply()
    }

    fun toggleShowHiddenFolders() {
        val nextValue = !_showHiddenFolders.value
        _showHiddenFolders.value = nextValue
        prefs.edit().putBoolean("show_hidden_folders", nextValue).apply()
    }

    fun togglePinFolder(folder: String) {
        val updated = _pinnedFolders.value.toMutableSet()
        if (updated.contains(folder)) {
            updated.remove(folder)
        } else {
            updated.add(folder)
        }
        _pinnedFolders.value = updated
        prefs.edit().putStringSet("pinned_folders", updated).apply()
    }

    fun toggleHideFolder(folder: String) {
        val updated = _hiddenFolders.value.toMutableSet()
        if (updated.contains(folder)) {
            updated.remove(folder)
        } else {
            updated.add(folder)
        }
        _hiddenFolders.value = updated
        prefs.edit().putStringSet("hidden_folders", updated).apply()
    }

    // --- Secure Folder Actions ---
    fun setupVaultPin(pin: String) {
        _vaultPin.value = pin
        prefs.edit().putString("vault_pin", pin).apply()
        _isVaultUnlocked.value = true
    }

    fun unlockVault(pin: String): Boolean {
        return if (_vaultPin.value == pin) {
            _isVaultUnlocked.value = true
            true
        } else {
            false
        }
    }

    fun lockVault() {
        _isVaultUnlocked.value = false
    }

    suspend fun moveToSecure(item: MediaItem) {
        repository.moveToSecure(item)
    }

    suspend fun restoreFromSecure(item: MediaItem) {
        repository.restoreFromSecure(item)
    }

    fun toggleFingerprint() {
        val next = !_isFingerprintEnabled.value
        _isFingerprintEnabled.value = next
        prefs.edit().putBoolean("vault_biometric", next).apply()
    }

    fun resetVaultSettings() {
        _vaultPin.value = ""
        _isVaultUnlocked.value = false
        _isFingerprintEnabled.value = false
        prefs.edit().remove("vault_pin").remove("vault_biometric").apply()

        // Unsecure all files on reset
        viewModelScope.launch {
            val secureItems = rawMediaList.value.filter { it.isSecure }
            secureItems.forEach { item ->
                database.mediaDao().updateMedia(item.copy(isSecure = false))
            }
        }
    }

    // --- Search & Standard Queries ---
    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSortBy(sortOption: SortOption) {
        _sortBy.value = sortOption
    }

    fun setFolder(folderName: String?) {
        _selectedFolder.value = folderName
    }

    fun toggleFavoritesOnly() {
        _showFavoritesOnly.value = !_showFavoritesOnly.value
    }

    fun toggleFavorite(mediaItem: MediaItem) {
        viewModelScope.launch {
            repository.toggleFavorite(mediaItem)
        }
    }

    fun savePlaybackPosition(id: Long, position: Long) {
        viewModelScope.launch {
            repository.savePlaybackPosition(id, position)
        }
    }

    fun deleteMediaItem(mediaItem: MediaItem) {
        viewModelScope.launch {
            repository.deleteVideo(mediaItem)
        }
    }

    fun markAsPlayed(itemId: Long) {
        viewModelScope.launch {
            val item = rawMediaList.value.find { it.id == itemId }
            if (item != null && item.isNew) {
                database.mediaDao().updateMedia(item.copy(isNew = false))
            }
        }
    }

    // --- Multi-Selection Batch Actions ---
    fun toggleSelection(itemId: Long) {
        val updated = _selectedItemIds.value.toMutableSet()
        if (updated.contains(itemId)) {
            updated.remove(itemId)
        } else {
            updated.add(itemId)
        }
        _selectedItemIds.value = updated
    }

    fun clearSelections() {
        _selectedItemIds.value = emptySet()
    }

    fun batchMoveToSecure() {
        viewModelScope.launch {
            _selectedItemIds.value.forEach { id ->
                val item = rawMediaList.value.find { it.id == id }
                if (item != null) {
                    repository.moveToSecure(item)
                }
            }
            clearSelections()
        }
    }

    fun batchRestoreFromSecure() {
        viewModelScope.launch {
            _selectedItemIds.value.forEach { id ->
                val item = rawMediaList.value.find { it.id == id }
                if (item != null && item.isSecure) {
                    repository.restoreFromSecure(item)
                }
            }
            clearSelections()
        }
    }

    fun batchDelete() {
        viewModelScope.launch {
            _selectedItemIds.value.forEach { id ->
                val item = rawMediaList.value.find { it.id == id }
                if (item != null) {
                    repository.deleteVideo(item)
                }
            }
            clearSelections()
        }
    }

    fun clearCache() {
        viewModelScope.launch {
            database.mediaDao().clearAll()
            scanMedia()
        }
    }
}

enum class SortOption(val displayName: String) {
    TITLE_ASC("Title (A-Z)"),
    TITLE_DESC("Title (Z-A)"),
    DATE_ADDED_DESC("Recently Added"),
    SIZE_DESC("File Size (Large)"),
    DURATION_DESC("Duration (Long)")
}
