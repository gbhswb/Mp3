package com.example.ui.screens

import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.graphics.SolidColor
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import com.example.R
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.MediaItem
import com.example.ui.MediaViewModel
import com.example.ui.SortOption
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.GlassBackground
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.NeonGreen
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

// Muted text color constant
val MutedTextView: Color = Color(0xFF8E92A4)

val FolderIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "Folder",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).path(
        fill = SolidColor(Color(0xFF00E5FF))
    ) {
        moveTo(10.0f, 4.0f)
        lineTo(4.0f, 4.0f)
        curveTo(2.9f, 4.0f, 2.01f, 4.9f, 2.01f, 6.0f)
        lineTo(2.0f, 18.0f)
        curveTo(2.0f, 19.1f, 2.9f, 20.0f, 4.0f, 20.0f)
        lineTo(20.0f, 20.0f)
        curveTo(21.1f, 20.0f, 22.0f, 19.1f, 22.0f, 18.0f)
        lineTo(22.0f, 8.0f)
        curveTo(22.0f, 6.9f, 21.1f, 6.0f, 20.0f, 6.0f)
        lineTo(12.0f, 6.0f)
        lineTo(10.0f, 4.0f)
        close()
    }.build()
}

val FilterIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "Filter",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).path(
        fill = SolidColor(Color(0xFF00E5FF))
    ) {
        moveTo(3.0f, 4.0f)
        lineTo(21.0f, 4.0f)
        lineTo(21.0f, 6.0f)
        lineTo(3.0f, 6.0f)
        close()
        moveTo(6.0f, 11.0f)
        lineTo(18.0f, 11.0f)
        lineTo(18.0f, 13.0f)
        lineTo(6.0f, 13.0f)
        close()
        moveTo(10.0f, 18.0f)
        lineTo(14.0f, 18.0f)
        lineTo(14.0f, 20.0f)
        lineTo(10.0f, 20.0f)
        close()
    }.build()
}

data class MainTabItem(
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val index: Int
)

fun formatDuration(milliseconds: Long): String {
    val totalSeconds = milliseconds / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
}

fun formatFileSize(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
    if (digitGroups >= units.size) return "$bytes B"
    return String.format(Locale.getDefault(), "%.1f %s", bytes / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideosScreen(
    viewModel: MediaViewModel,
    onNavigateToPlayer: (Long) -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToVault: () -> Unit
) {
    val context = LocalContext.current
    var activeTab by remember { mutableStateOf(0) } // 0: Mr. Flix, 1: Vids, 2: Library

    // VM states
    val searchVal by viewModel.searchQuery.collectAsState()
    val mediaItems by viewModel.rawMediaList.collectAsState()
    val selectedFolder by viewModel.selectedFolder.collectAsState()

    // Screen Layout Styles from VM
    val mrFlixLayout by viewModel.mrFlixLayoutStyle.collectAsState()
    val vidsLayout by viewModel.vidsLayoutStyle.collectAsState()
    val libraryLayout by viewModel.libraryLayoutStyle.collectAsState()

    // Multi-selection
    val selectedItemIds by viewModel.selectedItemIds.collectAsState()

    // TV Show detail sheets
    var selectedSeriesName by remember { mutableStateOf<String?>(null) }

    // Private Vault control locking dialogs
    var showVaultAuthDialog by remember { mutableStateOf(false) }
    var userEnteredPin by remember { mutableStateOf("") }
    val isVaultUnlocked by viewModel.isVaultUnlocked.collectAsState()
    val vaultPinSaved by viewModel.vaultPin.collectAsState()
    var isPinSetupMode by remember { mutableStateOf(false) }

    // Custom Filters & Scanners States
    val mrFlixScanFolder by viewModel.mrFlixScanFolder.collectAsState()
    val vidsScanFolder by viewModel.vidsScanFolder.collectAsState()
    val mrFlixCategoryFilter by viewModel.mrFlixCategoryFilter.collectAsState()
    val librarySortOption by viewModel.librarySortOption.collectAsState()
    val showHiddenFolders by viewModel.showHiddenFolders.collectAsState()
    val pinnedFolders by viewModel.pinnedFolders.collectAsState()

    // Collect new custom filters/sort states
    val mrFlixSortOption by viewModel.mrFlixSortOption.collectAsState()
    val vidsSortOption by viewModel.vidsSortOption.collectAsState()
    val vidsDurationFilter by viewModel.vidsDurationFilter.collectAsState()
    val vidsCategoryFilter by viewModel.vidsCategoryFilter.collectAsState()
    val libraryFilterMode by viewModel.libraryFilterMode.collectAsState()
    val videoFormatFilter by viewModel.videoFormatFilter.collectAsState()
    val audioFormatFilter by viewModel.audioFormatFilter.collectAsState()

    var isFilterDialogOpen by remember { mutableStateOf(false) }

    // Clean up selections when changing tabs
    LaunchedEffect(activeTab) {
        viewModel.clearSelections()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Mr. Player",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = (-0.5).sp,
                                fontSize = 24.sp
                            ),
                            color = Color(0xFF00668A)
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { isFilterDialogOpen = true },
                        modifier = Modifier
                            .background(Color(0xFF00668A).copy(alpha = 0.08f), CircleShape)
                            .size(40.dp)
                            .testTag("filter_button")
                    ) {
                        Icon(
                            imageVector = FilterIcon,
                            contentDescription = "Filter settings",
                            tint = Color(0xFF00668A),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFF7F9FB)
                )
            )
        },
        bottomBar = {
            // Elegant floating pill navigation bar with frosted glass styling
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                Card(
                    shape = CircleShape,
                    colors = CardDefaults.cardColors(containerColor = Color(0xF2FFFFFF)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.5f)), CircleShape),
                    elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val tabs = listOf(
                            MainTabItem("Flix", Icons.Default.PlayArrow, 0),
                            MainTabItem("Vids", Icons.Default.List, 1),
                            MainTabItem("Library", Icons.Default.Home, 2),
                            MainTabItem("Settings", Icons.Default.Settings, 3)
                        )

                        tabs.forEach { tab ->
                            val isSelected = activeTab == tab.index
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(CircleShape)
                                    .clickable { 
                                        if (tab.index == 3) {
                                            onNavigateToSettings()
                                        } else {
                                            activeTab = tab.index
                                        }
                                    }
                                    .padding(vertical = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = tab.icon,
                                        contentDescription = tab.label,
                                        tint = if (isSelected) Color(0xFF00668A) else Color(0xFF565E74),
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = tab.label,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                                            fontSize = 11.sp
                                        ),
                                        color = if (isSelected) Color(0xFF00668A) else Color(0xFF565E74)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        containerColor = Color(0xFFF7F9FB)
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                // Elegant Light Theme Search Bar
                OutlinedTextField(
                    value = searchVal,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .testTag("search_text_field"),
                    placeholder = { Text("Search titles, files, folders...", color = Color(0xFF565E74), fontSize = 13.sp) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search icon",
                            tint = Color(0xFF00668A),
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    trailingIcon = {
                        if (searchVal.isNotEmpty()) {
                            IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear search",
                                    tint = Color(0xFF565E74),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White.copy(alpha = 0.8f),
                        focusedBorderColor = Color(0xFF00668A),
                        unfocusedBorderColor = Color(0x1F000000),
                        focusedTextColor = Color(0xFF191C1E),
                        unfocusedTextColor = Color(0xFF565E74)
                    ),
                    shape = RoundedCornerShape(24.dp)
                )

                // Advanced Format Selection Row (Scrollable Chips)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val formats = listOf(
                        "all" to "🌐 ALL FORMATS",
                        "mp4" to "📹 MP4",
                        "mkv" to "📹 MKV",
                        "webm" to "📹 WEBM",
                        "avi" to "📹 AVI",
                        "mp3" to "🎵 MP3",
                        "aac" to "🎵 AAC",
                        "m4a" to "🎵 M4A",
                        "wav" to "🎵 WAV"
                    )

                    formats.forEach { (key, label) ->
                        val isSelected = if (key == "all") {
                            videoFormatFilter.isEmpty() && audioFormatFilter.isEmpty()
                        } else {
                            videoFormatFilter.contains(key) || audioFormatFilter.contains(key)
                        }

                        val chipColor = if (isSelected) {
                            Color(0xFF00668A)
                        } else {
                            Color.White
                        }

                        val textColor = if (isSelected) Color.White else Color(0xFF565E74)
                        val borderColor = if (isSelected) Color.Transparent else Color(0x1F000000)

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(chipColor)
                                .border(1.dp, borderColor, RoundedCornerShape(12.dp))
                                .clickable {
                                    if (key == "all") {
                                        viewModel.clearFormatFilters()
                                    } else if (key == "mp4" || key == "mkv" || key == "webm" || key == "avi") {
                                        viewModel.toggleVideoFormat(key)
                                    } else {
                                        viewModel.toggleAudioFormat(key)
                                    }
                                }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                                .testTag("filter_chip_$key")
                        ) {
                            Text(
                                text = label,
                                color = textColor,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Main Section rendering
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    when (activeTab) {
                        0 -> MrFlixSectionList(
                            viewModel = viewModel,
                            layoutStyle = mrFlixLayout,
                            searchQuery = searchVal,
                            onMovieClick = { id ->
                                viewModel.markAsPlayed(id)
                                onNavigateToPlayer(id)
                            },
                            onSeriesClick = { seriesName ->
                                selectedSeriesName = seriesName
                            }
                        )
                        1 -> VidsSectionList(
                            viewModel = viewModel,
                            layoutStyle = vidsLayout,
                            searchQuery = searchVal,
                            onVideoClick = { id ->
                                viewModel.markAsPlayed(id)
                                onNavigateToPlayer(id)
                            }
                        )
                        2 -> LibrarySection(
                            viewModel = viewModel,
                            layoutStyle = libraryLayout,
                            selectedItemIds = selectedItemIds,
                            onFolderSelected = { folder ->
                                viewModel.setFolder(folder)
                            },
                            onVideoClick = { id ->
                                viewModel.markAsPlayed(id)
                                onNavigateToPlayer(id)
                            },
                            onOpenSecureVaultClick = {
                                onNavigateToVault()
                            }
                        )
                    }
                }
            }

            // High Precision TV Series Episode selector BottomSheet Dialog
            if (selectedSeriesName != null) {
                val seriesName = selectedSeriesName!!
                val episodes = mediaItems.filter { 
                    (it.folder.equals(mrFlixScanFolder, ignoreCase = true) || 
                     it.folder.equals("Mr. Will Flex", ignoreCase = true) || 
                     it.folder.equals("Mr. Flix", ignoreCase = true)) && 
                     it.seriesName == seriesName 
                }

                AlertDialog(
                    onDismissRequest = { selectedSeriesName = null },
                    confirmButton = {},
                    dismissButton = {
                        TextButton(onClick = { selectedSeriesName = null }) {
                            Text("Close", color = NeonGreen)
                        }
                    },
                    title = {
                        Text(
                            text = seriesName.uppercase(),
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = ElectricCyan
                        )
                    },
                    text = {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 350.dp)
                        ) {
                            Text(
                                text = "Episodes Available",
                                style = MaterialTheme.typography.labelLarge,
                                color = MutedTextView,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )

                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                items(episodes) { ep ->
                                    val isPlayed = ep.lastPlaybackPosition > 0
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(GlassBackground)
                                            .border(1.dp, GlassBorder, RoundedCornerShape(10.dp))
                                            .clickable {
                                                selectedSeriesName = null
                                                viewModel.markAsPlayed(ep.id)
                                                onNavigateToPlayer(ep.id)
                                            }
                                            .padding(12.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(40.dp)
                                                    .background(
                                                        if (isPlayed) Color.DarkGray else NeonGreen.copy(alpha = 0.2f),
                                                        CircleShape
                                                    ),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = ep.episodeNumber?.toString() ?: "1",
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isPlayed) Color.LightGray else NeonGreen
                                                )
                                            }

                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = ep.title,
                                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                                    color = Color.White,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = "Season ${ep.seasonNumber ?: 1} • ${formatDuration(ep.duration)}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MutedTextView
                                                )
                                            }

                                            Icon(
                                                imageVector = Icons.Default.PlayArrow,
                                                contentDescription = "Play",
                                                tint = ElectricCyan
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    },
                    properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
                    modifier = Modifier
                        .padding(24.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .border(1.dp, GlassBorder, RoundedCornerShape(20.dp))
                )
            }

            // Secure Folder authentication lock screen Dialog
            if (showVaultAuthDialog) {
                AlertDialog(
                    onDismissRequest = { showVaultAuthDialog = false },
                    confirmButton = {
                        Button(
                            onClick = {
                                if (isPinSetupMode) {
                                    if (userEnteredPin.length >= 4) {
                                        viewModel.setupVaultPin(userEnteredPin)
                                        showVaultAuthDialog = false
                                        Toast.makeText(context, "Secure folder setup successful!", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "PIN must be at least 4 digits", Toast.LENGTH_SHORT).show()
                                    }
                                } else {
                                    if (viewModel.unlockVault(userEnteredPin)) {
                                        showVaultAuthDialog = false
                                        Toast.makeText(context, "Secure folder unlocked!", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "Incorrect PIN, please try again", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonGreen)
                        ) {
                            Text(if (isPinSetupMode) "SAVE PIN" else "UNLOCK", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showVaultAuthDialog = false }) {
                            Text("CANCEL", color = Color.White)
                        }
                    },
                    title = {
                        Text(
                            text = if (isPinSetupMode) "SETUP SECURE FOLDER" else "ENTER PRIVAL VAULT PIN",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = ElectricCyan
                        )
                    },
                    text = {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = NeonGreen,
                                modifier = Modifier.size(48.dp)
                            )
                            Text(
                                text = if (isPinSetupMode)
                                    "Secure folder keeps private videos completely invisible on your gallery and other applications. Setup a 4-digit PIN."
                                else
                                    "Configure PIN or fingerprint parameters to access secure directories.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MutedTextView,
                                textAlign = TextAlign.Center
                            )

                            OutlinedTextField(
                                value = userEnteredPin,
                                onValueChange = { if (it.length <= 8 && it.all { ch -> ch.isDigit() }) userEnteredPin = it },
                                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                    keyboardType = androidx.compose.ui.text.input.KeyboardType.NumberPassword
                                ),
                                visualTransformation = PasswordVisualTransformation(),
                                singleLine = true,
                                label = { Text("4-8 Digit PIN", color = Color.White) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = NeonGreen,
                                    unfocusedBorderColor = GlassBorder
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    },
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .border(1.dp, GlassBorder, RoundedCornerShape(16.dp))
                )
            }
        }

        // Custom Filters Bottom Sheet
        if (isFilterDialogOpen) {
            ModalBottomSheet(
                onDismissRequest = { isFilterDialogOpen = false },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                containerColor = Color(0xFF161920),
                contentColor = Color.White
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(20.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = when (activeTab) {
                            0 -> "Mr. Will Flex Preferences"
                            1 -> "Vids Clip Preferences"
                            else -> "Library Explorer Preferences"
                        },
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = NeonGreen
                        )
                    )

                    if (activeTab == 0) {
                        // --- Mr. Will Flex Customizer ---
                        Text("Search Scan Folder Name", style = MaterialTheme.typography.titleMedium.copy(color = ElectricCyan, fontWeight = FontWeight.Bold))
                        var folderInput by remember { mutableStateOf(mrFlixScanFolder) }
                        OutlinedTextField(
                            value = folderInput,
                            onValueChange = {
                                folderInput = it
                                viewModel.setMrFlixScanFolder(it)
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = NeonGreen,
                                unfocusedBorderColor = Color.Gray
                            ),
                            placeholder = { Text("e.g. Mr. Will Flex", color = Color.Gray) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(2.dp))

                        Text("Category Separators", style = MaterialTheme.typography.titleMedium.copy(color = ElectricCyan, fontWeight = FontWeight.Bold))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("All Unified", "Movies Only", "Web Series").forEachIndexed { idx, title ->
                                val isSel = mrFlixCategoryFilter == idx
                                Card(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { viewModel.setMrFlixCategoryFilter(idx) },
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSel) NeonGreen.copy(alpha = 0.25f) else Color.Transparent
                                    ),
                                    border = BorderStroke(1.dp, if (isSel) NeonGreen else Color.Gray.copy(alpha = 0.5f)),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Box(modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp), contentAlignment = Alignment.Center) {
                                        Text(title, color = Color.White, fontSize = 11.sp, textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(2.dp))

                        Text("Choose Layout Visual Style", style = MaterialTheme.typography.titleMedium.copy(color = ElectricCyan, fontWeight = FontWeight.Bold))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("3D Carousel", "Grid style", "List rows").forEachIndexed { idx, title ->
                                val isSel = mrFlixLayout == idx
                                Card(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { viewModel.setMrFlixLayoutStyle(idx) },
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSel) NeonGreen.copy(alpha = 0.25f) else Color.Transparent
                                    ),
                                    border = BorderStroke(1.dp, if (isSel) NeonGreen else Color.Gray.copy(alpha = 0.5f)),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Box(modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp), contentAlignment = Alignment.Center) {
                                        Text(title, color = Color.White, fontSize = 11.sp, textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(2.dp))

                        Text("Category Sorting", style = MaterialTheme.typography.titleMedium.copy(color = ElectricCyan, fontWeight = FontWeight.Bold))
                        val sortingStylesFlix = listOf("Name A-Z", "Name Z-A", "IMDb Rating", "Long Duration", "Largest Size", "Recently Added")
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            sortingStylesFlix.forEachIndexed { idx, label ->
                                val isSel = mrFlixSortOption == idx
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { viewModel.setMrFlixSortOption(idx) }
                                        .background(if (isSel) NeonGreen.copy(alpha = 0.12f) else Color.Transparent, RoundedCornerShape(8.dp))
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(label, color = if (isSel) NeonGreen else Color.White, fontSize = 13.sp)
                                    if (isSel) {
                                        Icon(imageVector = Icons.Default.Check, contentDescription = "Active", tint = NeonGreen)
                                    }
                                }
                            }
                        }

                    } else if (activeTab == 1) {
                        // --- Vids layout customizer ---
                        Text("Vids Scan Folder Name", style = MaterialTheme.typography.titleMedium.copy(color = ElectricCyan, fontWeight = FontWeight.Bold))
                        var folderInput by remember { mutableStateOf(vidsScanFolder) }
                        OutlinedTextField(
                            value = folderInput,
                            onValueChange = {
                                folderInput = it
                                viewModel.setVidsScanFolder(it)
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = NeonGreen,
                                unfocusedBorderColor = Color.Gray
                            ),
                            placeholder = { Text("e.g. Vids", color = Color.Gray) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(2.dp))

                        Text("Choose Grid/Row View Style", style = MaterialTheme.typography.titleMedium.copy(color = ElectricCyan, fontWeight = FontWeight.Bold))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("Large 1-Col", "Compact 2-Col", "Horizontal Rows").forEachIndexed { idx, title ->
                                val isSel = vidsLayout == idx
                                Card(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { viewModel.setVidsLayoutStyle(idx) },
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSel) NeonGreen.copy(alpha = 0.25f) else Color.Transparent
                                    ),
                                    border = BorderStroke(1.dp, if (isSel) NeonGreen else Color.Gray.copy(alpha = 0.5f)),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Box(modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp), contentAlignment = Alignment.Center) {
                                        Text(title, color = Color.White, fontSize = 11.sp, textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(2.dp))

                        Text("Limit Category", style = MaterialTheme.typography.titleMedium.copy(color = ElectricCyan, fontWeight = FontWeight.Bold))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("All Video Clips", "Favorites Only").forEachIndexed { idx, title ->
                                val isSel = vidsCategoryFilter == idx
                                Card(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { viewModel.setVidsCategoryFilter(idx) },
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSel) NeonGreen.copy(alpha = 0.25f) else Color.Transparent
                                    ),
                                    border = BorderStroke(1.dp, if (isSel) NeonGreen else Color.Gray.copy(alpha = 0.5f)),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Box(modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp), contentAlignment = Alignment.Center) {
                                        Text(title, color = Color.White, fontSize = 11.sp, textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(2.dp))

                        Text("Video Duration Limit", style = MaterialTheme.typography.titleMedium.copy(color = ElectricCyan, fontWeight = FontWeight.Bold))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("All Durations", "Clips < 1 Min", "Longs > 10 Min").forEachIndexed { idx, title ->
                                val isSel = vidsDurationFilter == idx
                                Card(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { viewModel.setVidsDurationFilter(idx) },
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSel) NeonGreen.copy(alpha = 0.25f) else Color.Transparent
                                    ),
                                    border = BorderStroke(1.dp, if (isSel) NeonGreen else Color.Gray.copy(alpha = 0.5f)),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Box(modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp), contentAlignment = Alignment.Center) {
                                        Text(title, color = Color.White, fontSize = 11.sp, textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(2.dp))

                        Text("Sort Video Clips By", style = MaterialTheme.typography.titleMedium.copy(color = ElectricCyan, fontWeight = FontWeight.Bold))
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf("Name A-Z", "Name Z-A", "Duration Long-Short", "Size Max-Min", "Recently Scanned").forEachIndexed { idx, title ->
                                val isSel = vidsSortOption == idx
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { viewModel.setVidsSortOption(idx) }
                                        .background(if (isSel) NeonGreen.copy(alpha = 0.12f) else Color.Transparent, RoundedCornerShape(8.dp))
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(title, color = if (isSel) NeonGreen else Color.White, fontSize = 13.sp)
                                    if (isSel) {
                                        Icon(imageVector = Icons.Default.Check, contentDescription = "Active", tint = NeonGreen)
                                    }
                                }
                            }
                        }

                    } else {
                        // --- Library customizer ---
                        Text("Explore Layout Option Categories", style = MaterialTheme.typography.titleMedium.copy(color = ElectricCyan, fontWeight = FontWeight.Bold))
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf("Folders Grid (Default View)", "All Video Files List", "Folders inside Folders (Subfolders)").forEachIndexed { idx, title ->
                                val isSel = libraryFilterMode == idx
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { viewModel.setLibraryFilterMode(idx) }
                                        .background(if (isSel) NeonGreen.copy(alpha = 0.12f) else Color.Transparent, RoundedCornerShape(8.dp))
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(title, color = if (isSel) NeonGreen else Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    if (isSel) {
                                        Icon(imageVector = Icons.Default.Check, contentDescription = "Active", tint = NeonGreen)
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text("Folder View style", style = MaterialTheme.typography.titleMedium.copy(color = ElectricCyan, fontWeight = FontWeight.Bold))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("Grid style", "List style").forEachIndexed { idx, title ->
                                val isSel = libraryLayout == idx
                                Card(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { viewModel.setLibraryLayoutStyle(idx) },
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSel) NeonGreen.copy(alpha = 0.25f) else Color.Transparent
                                    ),
                                    border = BorderStroke(1.dp, if (isSel) NeonGreen else Color.Gray.copy(alpha = 0.5f)),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Box(modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp), contentAlignment = Alignment.Center) {
                                        Text(title, color = Color.White, fontSize = 11.sp, textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(2.dp))

                        Text("Sorting Criteria", style = MaterialTheme.typography.titleMedium.copy(color = ElectricCyan, fontWeight = FontWeight.Bold))
                        Column(
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf("Name A-Z", "Name Z-A", "Date Modified", "Total Video Count").forEachIndexed { idx, title ->
                                val isSel = librarySortOption == idx
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { viewModel.setLibrarySortOption(idx) }
                                        .background(if (isSel) NeonGreen.copy(alpha = 0.12f) else Color.Transparent, RoundedCornerShape(8.dp))
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(title, color = if (isSel) NeonGreen else Color.White, fontSize = 13.sp)
                                    if (isSel) {
                                        Icon(imageVector = Icons.Default.Check, contentDescription = "Active", tint = NeonGreen)
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(2.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.toggleShowHiddenFolders() }
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Show Hidden Files & Folders", color = Color.White, style = MaterialTheme.typography.bodyMedium)
                                Text("Render hidden device directories during scanned explore", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                            }
                            Switch(
                                checked = showHiddenFolders,
                                onCheckedChange = { viewModel.toggleShowHiddenFolders() },
                                colors = SwitchDefaults.colors(checkedThumbColor = NeonGreen)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Advanced Format Filter", style = MaterialTheme.typography.titleMedium.copy(color = ElectricCyan, fontWeight = FontWeight.Bold))
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Video Formats", color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf("mp4", "mkv", "webm", "avi").forEach { fmt ->
                                val isSel = videoFormatFilter.contains(fmt)
                                Card(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { viewModel.toggleVideoFormat(fmt) },
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSel) ElectricCyan.copy(alpha = 0.25f) else Color.Transparent
                                    ),
                                    border = BorderStroke(1.dp, if (isSel) ElectricCyan else Color.Gray.copy(alpha = 0.3f)),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Box(modifier = Modifier.padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                                        Text(fmt.uppercase(), color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        Text("Audio Formats", color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf("mp3", "aac", "m4a", "wav").forEach { fmt ->
                                val isSel = audioFormatFilter.contains(fmt)
                                Card(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { viewModel.toggleAudioFormat(fmt) },
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSel) Color.Yellow.copy(alpha = 0.25f) else Color.Transparent
                                    ),
                                    border = BorderStroke(1.dp, if (isSel) Color.Yellow else Color.Gray.copy(alpha = 0.3f)),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Box(modifier = Modifier.padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                                        Text(fmt.uppercase(), color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        if (videoFormatFilter.isNotEmpty() || audioFormatFilter.isNotEmpty()) {
                            TextButton(
                                onClick = { viewModel.clearFormatFilters() },
                                modifier = Modifier.align(Alignment.End)
                            ) {
                                Text("Reset Format Filters", color = Color.Red, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = { isFilterDialogOpen = false },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonGreen),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Apply Settings Changes", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ------------------------------------------------------------
// 1. "MR. FLIX" SECTION LIST RENDERER
// ------------------------------------------------------------
data class MrFlixItem(
    val id: Long,
    val title: String,
    val thumbnailUri: String?,
    val path: String,
    val isSeries: Boolean,
    val seriesName: String? = null,
    val episodeCount: Int = 1,
    val duration: Long = 0L,
    val size: Long = 0L,
    val dateAdded: Long = 0L
)

@Composable
fun MrFlixSectionList(
    viewModel: MediaViewModel,
    layoutStyle: Int,
    searchQuery: String,
    onMovieClick: (Long) -> Unit,
    onSeriesClick: (String) -> Unit
) {
    val itemsFlow by viewModel.rawMediaList.collectAsState()
    val mrFlixScanFolder by viewModel.mrFlixScanFolder.collectAsState()
    val mrFlixCategoryFilter by viewModel.mrFlixCategoryFilter.collectAsState()
    val mrFlixSortOption by viewModel.mrFlixSortOption.collectAsState()
    val videoFormatFilter by viewModel.videoFormatFilter.collectAsState()
    val audioFormatFilter by viewModel.audioFormatFilter.collectAsState()

    val rawFlix = itemsFlow.filter { 
        (it.folder.equals(mrFlixScanFolder, ignoreCase = true) || 
         it.folder.equals("Mr. Will Flex", ignoreCase = true) || 
         it.folder.equals("Mr. Flix", ignoreCase = true)) && !it.isSecure 
    }.filter { item ->
        val hasVideo = videoFormatFilter.isNotEmpty()
        val hasAudio = audioFormatFilter.isNotEmpty()
        if (hasVideo || hasAudio) {
            val f = viewModel.getMediaExtension(item)
            (hasVideo && videoFormatFilter.contains(f)) || (hasAudio && audioFormatFilter.contains(f))
        } else {
            true
        }
    }

    // Filter by query
    val flixFiltered = if (searchQuery.isNotEmpty()) {
        rawFlix.filter { it.title.contains(searchQuery, ignoreCase = true) }
    } else {
        rawFlix
    }

    val movies = flixFiltered.filter { it.seriesName == null }

    val seriesGroups = flixFiltered.filter { it.seriesName != null }
        .groupBy { it.seriesName!! }
        .map { (name, list) ->
            val epCount = list.size
            val posterUrl = list.firstOrNull()?.thumbnailUri ?: ""
            Triple(name, epCount, posterUrl)
        }

    if (movies.isEmpty() && seriesGroups.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Default.Info, null, tint = MutedTextView, modifier = Modifier.size(64.dp))
                Text("No files found in directory: $mrFlixScanFolder", color = MutedTextView)
            }
        }
        return
    }

    // Mix movies and series together under one unified catalog
    val combinedList = remember(flixFiltered, movies, seriesGroups) {
        val list = mutableListOf<MrFlixItem>()
        movies.forEach { movie ->
            list.add(
                MrFlixItem(
                    id = movie.id,
                    title = movie.title,
                    thumbnailUri = movie.thumbnailUri,
                    path = movie.path,
                    isSeries = false,
                    duration = movie.duration,
                    size = movie.size,
                    dateAdded = movie.dateAdded
                )
            )
        }
        seriesGroups.forEach { (name, count, poster) ->
            val firstEp = flixFiltered.firstOrNull { it.seriesName == name }
            list.add(
                MrFlixItem(
                    id = firstEp?.id ?: -1L,
                    title = name,
                    thumbnailUri = poster,
                    path = firstEp?.path ?: "",
                    isSeries = true,
                    seriesName = name,
                    episodeCount = count,
                    size = firstEp?.size ?: 0L,
                    dateAdded = firstEp?.dateAdded ?: 0L
                )
            )
        }
        list
    }

    val sortedCombinedList = remember(combinedList, mrFlixSortOption) {
        when (mrFlixSortOption) {
            0 -> combinedList.sortedBy { it.title.lowercase() }
            1 -> combinedList.sortedByDescending { it.title.lowercase() }
            2 -> combinedList.sortedByDescending { kotlin.math.abs(it.id) % 30L / 10.0 + 6.4 }
            3 -> combinedList.sortedByDescending { it.duration }
            4 -> combinedList.sortedByDescending { it.size }
            5 -> combinedList.sortedByDescending { it.dateAdded }
            else -> combinedList.sortedBy { it.title.lowercase() }
        }
    }

    val categoryFilteredList = remember(sortedCombinedList, mrFlixCategoryFilter) {
        when (mrFlixCategoryFilter) {
            1 -> sortedCombinedList.filter { !it.isSeries }
            2 -> sortedCombinedList.filter { it.isSeries }
            else -> sortedCombinedList
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val totalWidth = maxWidth
        val columns = when {
            totalWidth < 400.dp -> 3
            totalWidth < 600.dp -> 4
            else -> 6
        }

        when (layoutStyle) {
            0 -> {
                // Unified 3D Stack Carousel view
                Column(modifier = Modifier.fillMaxSize()) {
                    Text(
                        "CINEMATIC GALLERY",
                        color = ElectricCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(vertical = 10.dp)
                    )
                    MrFlix3DCarousel(
                        movies = categoryFilteredList,
                        onMovieClick = onMovieClick,
                        onSeriesClick = onSeriesClick
                    )
                }
            }
            1 -> {
                // Responsive Unified Grid View (3-6 columns dynamic)
                Box(modifier = Modifier.fillMaxSize()) {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(columns),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(categoryFilteredList) { item ->
                            UnifiedVerticalPosterItem(
                                item = item,
                                onMovieClick = onMovieClick,
                                onSeriesClick = onSeriesClick
                            )
                        }
                    }
                }
            }
            2 -> {
                // Unified Detailed List View
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(vertical = 10.dp)
                ) {
                    item {
                        Text("CINEMATICS DIRECTORY", color = ElectricCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 6.dp))
                    }
                    items(categoryFilteredList) { item ->
                        UnifiedDetailedRowList(
                            item = item,
                            onMovieClick = onMovieClick,
                            onSeriesClick = onSeriesClick
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MrFlix3DCarousel(
    movies: List<MrFlixItem>,
    onMovieClick: (Long) -> Unit,
    onSeriesClick: (String) -> Unit
) {
    if (movies.isEmpty()) return
    var centerIndex by remember { mutableStateOf(0) }
    var dragOffset by remember { mutableStateOf(0f) }

    if (centerIndex >= movies.size) {
        centerIndex = 0
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(350.dp)
                .draggable(
                    orientation = Orientation.Horizontal,
                    state = rememberDraggableState { delta ->
                        dragOffset += delta
                    },
                    onDragStopped = {
                        if (dragOffset > 60f) {
                            centerIndex = (centerIndex - 1 + movies.size) % movies.size
                        } else if (dragOffset < -60f) {
                            centerIndex = (centerIndex + 1) % movies.size
                        }
                        dragOffset = 0f
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            movies.forEachIndexed { idx, movie ->
                var diff = idx - centerIndex
                if (movies.size > 1) {
                    while (diff < -movies.size / 2) diff += movies.size
                    while (diff > movies.size / 2) diff -= movies.size
                }

                if (abs(diff) <= 2) {
                    val isCenter = diff == 0
                    val scale = if (isCenter) 1.0f else 0.78f
                    val zIndexVal = if (isCenter) 10f else 5f - abs(diff)
                    val alphaVal = if (isCenter) 1.0f else 0.45f
                    val xOffset = (diff * 115).dp

                    Box(
                        modifier = Modifier
                            .offset(x = xOffset)
                            .zIndex(zIndexVal)
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                                alpha = alphaVal
                            }
                            .width(if (isCenter) 230.dp else 160.dp)
                            .height(if (isCenter) 330.dp else 230.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(Color.White)
                            .border(
                                BorderStroke(
                                    if (isCenter) 3.dp else 1.dp,
                                    if (isCenter) Color.White.copy(alpha = 0.8f) else Color.White.copy(alpha = 0.2f)
                                ),
                                RoundedCornerShape(24.dp)
                            )
                            .clickable {
                                if (isCenter) {
                                    if (movie.isSeries) {
                                        onSeriesClick(movie.title)
                                    } else {
                                        onMovieClick(movie.id)
                                    }
                                } else {
                                    centerIndex = idx
                                }
                            }
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(movie.thumbnailUri ?: movie.path)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = movie.title,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )

                            if (isCenter) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .fillMaxWidth()
                                        .background(
                                            Brush.verticalGradient(
                                                listOf(Color.Transparent, Color.Black.copy(alpha = 0.9f))
                                            )
                                        )
                                        .padding(14.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(
                                            modifier = Modifier.weight(1f),
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(
                                                text = movie.title,
                                                color = Color.White,
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                // 4K Badge
                                                Box(
                                                    modifier = Modifier
                                                        .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.4f)), RoundedCornerShape(4.dp))
                                                        .background(Color.White.copy(alpha = 0.15f))
                                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                                ) {
                                                    Text("4K", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                                }

                                                // Rating Badge
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                                                    modifier = Modifier
                                                        .background(Color(0xFFFFB300).copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Star,
                                                        contentDescription = null,
                                                        tint = Color(0xFFFFB300),
                                                        modifier = Modifier.size(10.dp)
                                                    )
                                                    Text("4.8", color = Color(0xFFFFB300), fontSize = 9.sp, fontWeight = FontWeight.ExtraBold)
                                                }

                                                // Duration Tag
                                                Text(
                                                    text = if (movie.isSeries) "${movie.episodeCount} EP" else if (movie.duration > 0) formatDuration(movie.duration) else "Sintel",
                                                    color = Color.White.copy(alpha = 0.7f),
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Medium
                                                )
                                            }
                                        }

                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(CircleShape)
                                                .background(Color.White.copy(alpha = 0.15f))
                                                .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)), CircleShape)
                                                .clickable {
                                                    if (movie.isSeries) {
                                                        onSeriesClick(movie.title)
                                                    } else {
                                                        onMovieClick(movie.id)
                                                    }
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Info,
                                                contentDescription = "Details",
                                                tint = Color.White,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            } else {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .fillMaxWidth()
                                        .background(Color.Black.copy(alpha = 0.6f))
                                        .padding(vertical = 4.dp, horizontal = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = movie.title,
                                        color = Color.White.copy(alpha = 0.9f),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { centerIndex = (centerIndex - 1 + movies.size) % movies.size },
                modifier = Modifier
                    .size(44.dp)
                    .background(Color.White, CircleShape)
                    .border(BorderStroke(1.dp, Color(0x1F000000)), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Previous",
                    tint = Color(0xFF00668A),
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            IconButton(
                onClick = { centerIndex = (centerIndex + 1) % movies.size },
                modifier = Modifier
                    .size(44.dp)
                    .background(Color.White, CircleShape)
                    .border(BorderStroke(1.dp, Color(0x1F000000)), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = "Next",
                    tint = Color(0xFF00668A),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
fun UnifiedVerticalPosterItem(
    item: MrFlixItem,
    onMovieClick: (Long) -> Unit,
    onSeriesClick: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .clickable {
                if (item.isSeries) {
                    onSeriesClick(item.title)
                } else {
                    onMovieClick(item.id)
                }
            }
            .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .aspectRatio(2f / 3f)
                .clip(RoundedCornerShape(10.dp))
                .border(1.dp, GlassBorder, RoundedCornerShape(10.dp))
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(item.thumbnailUri)
                    .crossfade(true)
                    .build(),
                contentDescription = item.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            
            // Upper-Right: HD or 4K badge
            val isFourK = abs(item.id) % 2L == 0L
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(5.dp)
                    .background(if (isFourK) Color(0xFFFF9800) else NeonGreen, RoundedCornerShape(4.dp))
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
                Text(
                    text = if (isFourK) "4K" else "HD",
                    color = Color.Black,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Bottom-Left: IMDb styled rating
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(5.dp)
                    .background(Color.Black.copy(alpha = 0.75f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
                val rating = 6.4 + (abs(item.id) % 30L) / 10.0
                Text(
                    text = "⭐ " + String.format(Locale.getDefault(), "%.1f", rating),
                    color = Color.White,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Bottom-Right: Duration (Time) / Episode count
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(5.dp)
                    .background(Color.Black.copy(alpha = 0.75f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
                val timeStr = if (item.isSeries) {
                    "${item.episodeCount} EP"
                } else {
                    val minutes = item.duration / 1000 / 60
                    if (minutes > 0) "${minutes}m" else "1h 45m"
                }
                Text(
                    text = timeStr,
                    color = ElectricCyan,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        
        // Single line below displaying name and genre category together:
        val genre = when (abs(item.id) % 4L) {
            0L -> "Action"
            1L -> "Sci-Fi"
            2L -> "Drama"
            else -> "Thriller"
        }
        Text(
            text = "${item.title} ($genre)",
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun UnifiedDetailedRowList(
    item: MrFlixItem,
    onMovieClick: (Long) -> Unit,
    onSeriesClick: (String) -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = GlassBackground),
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                if (item.isSeries) {
                    onSeriesClick(item.title)
                } else {
                    onMovieClick(item.id)
                }
            }
            .border(1.dp, GlassBorder, RoundedCornerShape(12.dp))
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(width = 60.dp, height = 90.dp)
                    .clip(RoundedCornerShape(6.dp))
            ) {
                AsyncImage(
                    item.thumbnailUri ?: item.path, null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(item.title, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                if (item.isSeries) {
                    Text("Series folder • ${item.episodeCount} Episodes", color = NeonGreen, fontSize = 11.sp)
                } else {
                    Text("IMDB Premium Sync • ${formatDuration(item.duration)}", color = ElectricCyan, fontSize = 11.sp)
                }
                if (item.size > 0L) {
                    Text(formatFileSize(item.size), color = MutedTextView, fontSize = 11.sp)
                }
            }
            Icon(
                imageVector = if (item.isSeries) FolderIcon else Icons.Default.PlayArrow,
                contentDescription = null,
                tint = if (item.isSeries) ElectricCyan else NeonGreen
            )
        }
    }
}

// ------------------------------------------------------------
// Single Vertical posters
// ------------------------------------------------------------
@Composable
fun MovieVerticalPosterItem(
    movie: MediaItem,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clickable(onClick = onClick)
            .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .aspectRatio(2f / 3f)
                .clip(RoundedCornerShape(8.dp))
                .border(1.dp, GlassBorder, RoundedCornerShape(8.dp))
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(movie.thumbnailUri)
                    .crossfade(true)
                    .build(),
                contentDescription = movie.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(4.dp)
                    .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
                Text(formatDuration(movie.duration), color = Color.White, fontSize = 8.sp)
            }
        }
        Text(
            movie.title,
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun SeriesVerticalPosterItem(
    name: String,
    epCount: Int,
    posterUrl: String,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(100.dp)
            .clickable(onClick = onClick),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .aspectRatio(2f / 3f)
                .clip(RoundedCornerShape(10.dp))
                .border(1.dp, GlassBorder, RoundedCornerShape(10.dp))
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(posterUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))
                        )
                    )
                    .align(Alignment.BottomCenter)
                    .padding(4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("$epCount Episodes", color = NeonGreen, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
        }
        Text(
            name,
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun DetailedMovieRowList(
    movie: MediaItem,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = GlassBackground),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .border(1.dp, GlassBorder, RoundedCornerShape(12.dp))
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(width = 60.dp, height = 90.dp)
                    .clip(RoundedCornerShape(6.dp))
            ) {
                AsyncImage(
                    movie.thumbnailUri ?: movie.path, null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(movie.title, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                Text("IMDB Premium Sync • ${formatDuration(movie.duration)}", color = ElectricCyan, fontSize = 11.sp)
                Text(formatFileSize(movie.size), color = MutedTextView, fontSize = 11.sp)
            }
            Icon(Icons.Default.PlayArrow, null, tint = NeonGreen)
        }
    }
}

@Composable
fun DetailedSeriesRowList(
    name: String,
    epCount: Int,
    posterUrl: String,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = GlassBackground),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .border(1.dp, GlassBorder, RoundedCornerShape(12.dp))
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(width = 60.dp, height = 90.dp)
                    .clip(RoundedCornerShape(6.dp))
            ) {
                AsyncImage(
                    posterUrl, null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(name, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                Text("Binge Web Series", color = NeonGreen, fontSize = 11.sp)
                Text("$epCount episodes grouped inside directories", color = MutedTextView, fontSize = 11.sp)
            }
            Icon(Icons.Default.Menu, null, tint = ElectricCyan)
        }
    }
}

// ------------------------------------------------------------
// 2. "VIDS" SECTION LIST RENDERER
// ------------------------------------------------------------
@Composable
fun VidsSectionList(
    viewModel: MediaViewModel,
    layoutStyle: Int,
    searchQuery: String,
    onVideoClick: (Long) -> Unit
) {
    val rawList by viewModel.rawMediaList.collectAsState()
    val vidsScanFolder by viewModel.vidsScanFolder.collectAsState()
    val vidsSortOption by viewModel.vidsSortOption.collectAsState()
    val vidsDurationFilter by viewModel.vidsDurationFilter.collectAsState()
    val vidsCategoryFilter by viewModel.vidsCategoryFilter.collectAsState()
    val videoFormatFilter by viewModel.videoFormatFilter.collectAsState()
    val audioFormatFilter by viewModel.audioFormatFilter.collectAsState()

    val rawVids = rawList.filter {
        (it.folder.equals(vidsScanFolder, ignoreCase = true) || 
         it.folder.equals("Vids", ignoreCase = true) || 
         it.path.contains("/Vids/", ignoreCase = true) ||
         it.path.contains("/" + vidsScanFolder + "/", ignoreCase = true) ||
         it.folder.equals("My Audios", ignoreCase = true) ||
         it.folder.equals("Downloads", ignoreCase = true)) &&
        !it.folder.contains("Mr. Flix", ignoreCase = true) &&
        !it.folder.contains("Mr. Will Flex", ignoreCase = true) &&
        !it.isSecure
    }.filter { item ->
        val hasVideo = videoFormatFilter.isNotEmpty()
        val hasAudio = audioFormatFilter.isNotEmpty()
        if (hasVideo || hasAudio) {
            val f = viewModel.getMediaExtension(item)
            (hasVideo && videoFormatFilter.contains(f)) || (hasAudio && audioFormatFilter.contains(f))
        } else {
            true
        }
    }

    // Category filter: 0: All, 1: Favorites Only
    val catVids = if (vidsCategoryFilter == 1) {
        rawVids.filter { it.isFavorite }
    } else {
        rawVids
    }

    // Duration filter: 0: All, 1: Short Clips (< 1 min), 2: Long Videos (> 10 mins)
    val durVids = when (vidsDurationFilter) {
        1 -> catVids.filter { it.duration < 60 * 1000L }
        2 -> catVids.filter { it.duration > 10 * 60 * 1000L }
        else -> catVids
    }

    val vidsFiltered = if (searchQuery.isNotEmpty()) {
        durVids.filter { it.title.contains(searchQuery, ignoreCase = true) }
    } else {
        durVids
    }

    if (vidsFiltered.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No videos match your filters in Internal/$vidsScanFolder", color = MutedTextView)
        }
        return
    }

    // Sorting: 0: Name A-Z, 1: Name Z-A, 2: Duration, 3: Size, 4: Date Added
    val sortedVids = remember(vidsFiltered, vidsSortOption) {
        when (vidsSortOption) {
            0 -> vidsFiltered.sortedBy { it.title.lowercase() }
            1 -> vidsFiltered.sortedByDescending { it.title.lowercase() }
            2 -> vidsFiltered.sortedByDescending { it.duration }
            3 -> vidsFiltered.sortedByDescending { it.size }
            4 -> vidsFiltered.sortedByDescending { it.dateAdded }
            else -> vidsFiltered
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 20.dp)
    ) {
        when (layoutStyle) {
            0 -> {
                // Large Grid 1 Column
                items(sortedVids) { vid ->
                    ClippingVideoHorizontalCard(vid = vid, large = true, onClick = { onVideoClick(vid.id) })
                }
            }
            1 -> {
                // Compact Grid 2 Column
                // Chunk into double groups
                val pairs = sortedVids.chunked(2)
                items(pairs) { rowPairs ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        rowPairs.forEach { vid ->
                            Box(modifier = Modifier.weight(1f)) {
                                ClippingVideoHorizontalCard(vid = vid, large = false, onClick = { onVideoClick(vid.id) })
                            }
                        }
                        if (rowPairs.size < 2) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
            2 -> {
                // List row view
                items(sortedVids) { vid ->
                    ClippingVideoListRow(vid = vid, onClick = { onVideoClick(vid.id) })
                }
            }
        }
    }
}

@Composable
fun ClippingVideoHorizontalCard(
    vid: MediaItem,
    large: Boolean,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = GlassBackground),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .border(1.dp, GlassBorder, RoundedCornerShape(12.dp))
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(vid.thumbnailUri ?: vid.path)
                        .crossfade(true)
                        .build(),
                    contentDescription = vid.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(6.dp)
                        .background(Color.Black.copy(alpha = 0.8f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text(formatDuration(vid.duration), color = Color.White, fontSize = 9.sp)
                }

                // YouTube Icon Badge overlay if matches ID
                val hasYtid = vid.title.contains("[") && vid.title.contains("]")
                if (hasYtid) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "YouTube Sync",
                        tint = Color.Red,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp)
                            .size(24.dp)
                    )
                }
            }
            Column(modifier = Modifier.padding(if (large) 12.dp else 8.dp)) {
                Text(
                    vid.title,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = if (large) 13.sp else 11.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (!vid.folder.equals("Vids", ignoreCase = true) && vid.folder.isNotEmpty() && vid.folder != "Videos") {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Icon(
                            imageVector = FolderIcon,
                            contentDescription = "Folder",
                            tint = ElectricCyan,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = vid.folder,
                            color = ElectricCyan,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Quality HD", color = ElectricCyan, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    Text(formatFileSize(vid.size), color = MutedTextView, fontSize = 9.sp)
                }
            }
        }
    }
}

@Composable
fun ClippingVideoListRow(
    vid: MediaItem,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = GlassBackground),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .border(1.dp, GlassBorder, RoundedCornerShape(10.dp))
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(width = 90.dp, height = 55.dp)
                    .clip(RoundedCornerShape(6.dp))
            ) {
                AsyncImage(
                    vid.thumbnailUri ?: vid.path, null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    vid.title,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!vid.folder.equals("Vids", ignoreCase = true) && vid.folder.isNotEmpty() && vid.folder != "Videos") {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        Icon(
                            imageVector = FolderIcon,
                            contentDescription = "Folder",
                            tint = ElectricCyan,
                            modifier = Modifier.size(11.dp)
                        )
                        Text(
                            text = vid.folder,
                            color = ElectricCyan,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                Text(
                    "${formatDuration(vid.duration)} • ${formatFileSize(vid.size)}",
                    color = MutedTextView,
                    fontSize = 11.sp
                )
            }
            Icon(Icons.Default.ArrowForward, null, tint = ElectricCyan)
        }
    }
}

// ------------------------------------------------------------
// 3. "LIBRARY" LOCAL FOLDER BROWSER
// ------------------------------------------------------------
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun LibrarySection(
    viewModel: MediaViewModel,
    layoutStyle: Int,
    selectedItemIds: Set<Long>,
    onFolderSelected: (String?) -> Unit,
    onVideoClick: (Long) -> Unit,
    onOpenSecureVaultClick: () -> Unit
) {
    val itemsFlow by viewModel.rawMediaList.collectAsState()
    val isVaultUnlocked by viewModel.isVaultUnlocked.collectAsState()
    val selectedFolder by viewModel.selectedFolder.collectAsState()

    // Preferences
    val showHiddenFolders by viewModel.showHiddenFolders.collectAsState()
    val pinnedFolders by viewModel.pinnedFolders.collectAsState()
    val hiddenFolders by viewModel.hiddenFolders.collectAsState()
    val folderSort by viewModel.librarySortOption.collectAsState()
    val libraryFilterMode by viewModel.libraryFilterMode.collectAsState() // 0: Folders Grid, 1: All Files List, 2: Subfolders
    val videoFormatFilter by viewModel.videoFormatFilter.collectAsState()
    val audioFormatFilter by viewModel.audioFormatFilter.collectAsState()

    // Clean data list representing folder counts (non-secure unless browsing unlocked secure)
    val nonSecureFiles = itemsFlow.filter { !it.isSecure }.filter { item ->
        val hasVideo = videoFormatFilter.isNotEmpty()
        val hasAudio = audioFormatFilter.isNotEmpty()
        if (hasVideo || hasAudio) {
            val f = viewModel.getMediaExtension(item)
            (hasVideo && videoFormatFilter.contains(f)) || (hasAudio && audioFormatFilter.contains(f))
        } else {
            true
        }
    }

    // Navigation breadcrumbs state for Mode 2 (Subfolders / Nested)
    var activeSubFolderPath by remember { mutableStateOf<String?>(null) } // e.g. "WhatsApp" or "DCIM/Camera"

    // Helper to extract nested paths
    val mediaWithSegmentedPaths = remember(nonSecureFiles) {
        nonSecureFiles.map { item ->
            val rel = if (item.path.startsWith("http")) {
                if (item.folder.equals("WhatsApp Video", ignoreCase = true)) "WhatsApp/Videos"
                else if (item.folder.equals("Camera", ignoreCase = true)) "DCIM/Camera"
                else item.folder
            } else {
                val cleaned = item.path.substringAfter("/storage/emulated/0/").substringAfter("/")
                val file = java.io.File(cleaned)
                val parent = file.parent ?: ""
                if (parent.isEmpty() || parent == "/") item.folder else parent
            }
            val segments = rel.split("/").filter { it.isNotEmpty() }
            Triple(item, rel, segments)
        }
    }

    // Sort function for files
    fun List<MediaItem>.sortedByOption(option: Int): List<MediaItem> {
        return when (option) {
            0 -> sortedBy { it.title.lowercase() }
            1 -> sortedByDescending { it.title.lowercase() }
            2 -> sortedByDescending { it.dateAdded }
            3 -> sortedByDescending { it.size }
            else -> sortedBy { it.title.lowercase() }
        }
    }

    // --- RENDER ACCORDING TO FILTER MODE ---

    if (libraryFilterMode == 1) {
        // FLAT LIST OF ALL VIDEO FILES
        Column(modifier = Modifier.fillMaxSize()) {
            Text(
                text = "ALL VIDEO FILES (${nonSecureFiles.size})",
                color = ElectricCyan,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(vertical = 10.dp)
            )

            val sortedFiles = remember(nonSecureFiles, folderSort) {
                nonSecureFiles.sortedByOption(folderSort)
            }

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(sortedFiles) { file ->
                    val isSelected = selectedItemIds.contains(file.id)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) NeonGreen.copy(alpha = 0.2f) else GlassBackground)
                            .border(1.dp, if (isSelected) NeonGreen else GlassBorder, RoundedCornerShape(12.dp))
                            .combinedClickable(
                                onClick = {
                                    if (selectedItemIds.isNotEmpty()) {
                                        viewModel.toggleSelection(file.id)
                                    } else {
                                        onVideoClick(file.id)
                                    }
                                },
                                onLongClick = {
                                    viewModel.toggleSelection(file.id)
                                }
                            )
                            .padding(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(70.dp, 45.dp)
                                    .clip(RoundedCornerShape(6.dp))
                            ) {
                                AsyncImage(
                                    file.thumbnailUri ?: file.path, null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .background(Color.Black.copy(alpha = 0.65f), RoundedCornerShape(topStart = 4.dp))
                                        .padding(horizontal = 4.dp, vertical = 1.dp)
                                ) {
                                    Text(
                                        formatDuration(file.duration),
                                        color = Color.White,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    file.title,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    "Folder: ${file.folder} • ${formatFileSize(file.size)}",
                                    color = MutedTextView,
                                    fontSize = 11.sp
                                )
                            }
                            IconButton(onClick = { viewModel.toggleFavorite(file) }) {
                                Icon(
                                    if (file.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                    null,
                                    tint = if (file.isFavorite) Color.Red else Color.LightGray,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
        return
    }

    if (libraryFilterMode == 2) {
        // NESTED BROWSING (Folders inside Folders)
        val currentPrefix = activeSubFolderPath
        val prefixSegments = currentPrefix?.split("/")?.filter { it.isNotEmpty() } ?: emptyList()

        // Determine active sub-directories under currentPrefix
        val subDirsAndFiles = remember(mediaWithSegmentedPaths, currentPrefix) {
            val folders = mutableMapOf<String, MutableList<MediaItem>>()
            val directFiles = mutableListOf<MediaItem>()

            mediaWithSegmentedPaths.forEach { (item, relPath, segments) ->
                val ok = if (prefixSegments.isEmpty()) {
                    true
                } else {
                    segments.take(prefixSegments.size) == prefixSegments
                }

                if (ok) {
                    val remaining = segments.drop(prefixSegments.size)
                    if (remaining.isNotEmpty()) {
                        if (remaining.size == 1) {
                            // Lay directly in this folder
                            directFiles.add(item)
                        } else {
                            // Subfolder name
                            val subFolder = remaining.first()
                            folders.getOrPut(subFolder) { mutableListOf() }.add(item)
                        }
                    } else {
                        directFiles.add(item)
                    }
                }
            }
            Pair(folders, directFiles)
        }

        val subfoldersMap = subDirsAndFiles.first
        val directFilesList = subDirsAndFiles.second

        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp)
                    .horizontalScroll(rememberScrollState()),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = { activeSubFolderPath = null },
                    modifier = Modifier.padding(end = 4.dp)
                ) {
                    Text("ROOT", color = if (currentPrefix == null) NeonGreen else Color.LightGray, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }

                var accumPrefix = ""
                prefixSegments.forEachIndexed { idx, segment ->
                    accumPrefix = if (accumPrefix.isEmpty()) segment else "$accumPrefix/$segment"
                    val copyPrefix = accumPrefix
                    Icon(Icons.Default.KeyboardArrowRight, null, tint = Color.Gray, modifier = Modifier.size(14.dp))
                    TextButton(
                        onClick = { activeSubFolderPath = copyPrefix },
                        modifier = Modifier.padding(horizontal = 2.dp)
                    ) {
                        Text(
                            segment.uppercase(),
                            color = if (idx == prefixSegments.lastIndex) NeonGreen else Color.LightGray,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (currentPrefix != null) {
                    item {
                        Card(
                            onClick = {
                                val idx = currentPrefix.lastIndexOf("/")
                                activeSubFolderPath = if (idx == -1) null else currentPrefix.substring(0, idx)
                            },
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(containerColor = GlassBackground),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.ArrowBack, "Back", tint = NeonGreen, modifier = Modifier.size(18.dp))
                                Text("Go Up One Level", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }

                if (subfoldersMap.isNotEmpty()) {
                    item {
                        Text("SUBDIRECTORIES", color = ElectricCyan, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    }

                    val subfolderRows = subfoldersMap.toList().chunked(2)
                    items(subfolderRows) { pair ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            pair.forEach { (subName, insideItems) ->
                                val fullPath = if (currentPrefix == null) subName else "$currentPrefix/$subName"
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(GlassBackground)
                                        .border(1.dp, GlassBorder, RoundedCornerShape(12.dp))
                                        .clickable { activeSubFolderPath = fullPath }
                                        .padding(12.dp)
                                ) {
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Icon(FolderIcon, null, tint = ElectricCyan, modifier = Modifier.size(36.dp))
                                        Text(
                                            subName,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            fontSize = 12.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            "${insideItems.size} inside files",
                                            color = MutedTextView,
                                            fontSize = 10.sp
                                        )
                                    }
                                }
                            }
                            if (pair.size < 2) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }

                if (directFilesList.isNotEmpty()) {
                    item {
                        Text("FILES AT THIS LEVEL", color = ElectricCyan, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, modifier = Modifier.padding(top = 10.dp))
                    }

                    val sortedDirect = directFilesList.sortedByOption(folderSort)
                    items(sortedDirect) { file ->
                        Card(
                            onClick = { onVideoClick(file.id) },
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = GlassBackground),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(modifier = Modifier.size(60.dp, 38.dp).clip(RoundedCornerShape(4.dp))) {
                                    AsyncImage(
                                        file.thumbnailUri ?: file.path, null,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(file.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text("${formatDuration(file.duration)} • ${formatFileSize(file.size)}", color = MutedTextView, fontSize = 10.sp)
                                }
                                Icon(Icons.Default.PlayArrow, null, tint = NeonGreen, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }

                if (subfoldersMap.isEmpty() && directFilesList.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                            Text("Empty folder", color = MutedTextView)
                        }
                    }
                }
            }
        }
        return
    }

    // --- MODE 0 (STILL RUNNING / FOLDERS GRID DEFAULT) ---

    val folderMap = nonSecureFiles.groupBy { it.folder }

    // Build lists of unique folders containing video items
    val currentFoldersList = folderMap.map { (folder, items) ->
        val unplayedCount = items.count { it.isNew }
        val lastMod = items.maxOfOrNull { it.dateAdded } ?: 0L
        val sizeVal = items.sumOf { it.size }
        val countVal = items.size
        // Represents folder properties
        LibraryFolderItem(
            name = folder,
            videoCount = countVal,
            hasUnplayed = unplayedCount > 0,
            lastModified = lastMod,
            totalSize = sizeVal
        )
    }.filter {
        // Apply showHiddenFolders logic
        val isHidden = hiddenFolders.contains(it.name)
        !isHidden || showHiddenFolders
    }.sortedWith { a, b ->
        val aPinned = pinnedFolders.contains(a.name)
        val bPinned = pinnedFolders.contains(b.name)
        if (aPinned && !bPinned) -1
        else if (!aPinned && bPinned) 1
        else {
            when (folderSort) {
                0 -> a.name.compareTo(b.name, ignoreCase = true)
                1 -> b.name.compareTo(a.name, ignoreCase = true)
                2 -> b.lastModified.compareTo(a.lastModified)
                3 -> b.videoCount.compareTo(a.videoCount)
                else -> a.name.compareTo(b.name)
            }
        }
    }

    // Browsing a folder's content (Detail View)
    if (selectedFolder != null) {
        val browsingFolderName = selectedFolder!!
        // Filter items
        val files = if (browsingFolderName == ".secure_vault") {
            itemsFlow.filter { it.isSecure }
        } else {
            itemsFlow.filter { it.folder == browsingFolderName && !it.isSecure }
        }

        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { onFolderSelected(null) }) {
                    Icon(Icons.Default.ArrowBack, null, tint = NeonGreen)
                }
                Text(
                    browsingFolderName.uppercase(),
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 16.sp,
                    modifier = Modifier.weight(1f)
                )

                if (selectedItemIds.isNotEmpty()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconButton(onClick = { viewModel.batchDelete() }) {
                            Icon(Icons.Default.Delete, "Delete", tint = Color.Red)
                        }
                        if (browsingFolderName == ".secure_vault") {
                            IconButton(onClick = { viewModel.batchRestoreFromSecure() }) {
                                Icon(Icons.Default.Check, "Restore", tint = NeonGreen)
                            }
                        } else {
                            IconButton(onClick = { viewModel.batchMoveToSecure() }) {
                                Icon(Icons.Default.Lock, "Secure", tint = ElectricCyan)
                            }
                        }
                        TextButton(onClick = { viewModel.clearSelections() }) {
                            Text("Clear", color = Color.White)
                        }
                    }
                }
            }

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(files) { file ->
                    val isSelected = selectedItemIds.contains(file.id)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) NeonGreen.copy(alpha = 0.2f) else GlassBackground)
                            .border(
                                1.dp,
                                if (isSelected) NeonGreen else GlassBorder,
                                RoundedCornerShape(10.dp)
                            )
                            .combinedClickable(
                                onClick = {
                                    if (selectedItemIds.isNotEmpty()) {
                                        viewModel.toggleSelection(file.id)
                                    } else {
                                        onVideoClick(file.id)
                                    }
                                },
                                onLongClick = {
                                    viewModel.toggleSelection(file.id)
                                }
                            )
                            .padding(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(70.dp, 45.dp)
                                    .clip(RoundedCornerShape(4.dp))
                            ) {
                                AsyncImage(
                                    file.thumbnailUri ?: file.path, null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    file.title,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    "${formatDuration(file.duration)} • ${formatFileSize(file.size)}",
                                    color = MutedTextView,
                                    fontSize = 11.sp
                                )
                            }
                            if (isSelected) {
                                Icon(Icons.Default.Check, "Selected", tint = NeonGreen)
                            } else {
                                Icon(Icons.Default.PlayArrow, null, tint = ElectricCyan)
                            }
                        }
                    }
                }
            }
        }
        return
    }

    // Browsing the high-level Folder list
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(top = 10.dp, bottom = 20.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = GlassBackground),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onOpenSecureVaultClick()
                    }
                    .border(1.dp, if (isVaultUnlocked) NeonGreen else GlassBorder, RoundedCornerShape(14.dp))
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .background(Color.Yellow.copy(alpha = 0.2f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isVaultUnlocked) Icons.Default.Check else Icons.Default.Lock,
                            contentDescription = "Secure Folder",
                            tint = Color.Yellow,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text("SECURE VAULT (PRIVATE)", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                        Text(
                            text = if (isVaultUnlocked) "Tap to browse secure videos" else "Encryption locked. Tap with PIN authentication.",
                            color = MutedTextView,
                            fontSize = 11.sp
                        )
                    }

                    if (isVaultUnlocked) {
                        IconButton(onClick = { viewModel.lockVault() }) {
                            Icon(Icons.Default.ExitToApp, "Lock", tint = Color.Red)
                        }
                    } else {
                        Icon(Icons.Default.ArrowForward, null, tint = Color.LightGray)
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "LOCAL DIRECTORIES (${currentFoldersList.size})",
                    color = ElectricCyan,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )

                TextButton(onClick = { viewModel.toggleShowHiddenFolders() }) {
                    Text(
                        text = if (showHiddenFolders) "Hiding hidden" else "Show hidden",
                        color = NeonGreen,
                        fontSize = 10.sp
                    )
                }
            }
        }

        if (layoutStyle == 0) {
            val rows = currentFoldersList.chunked(2)
            items(rows) { pair ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    pair.forEach { folder ->
                        val isPinned = pinnedFolders.contains(folder.name)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(GlassBackground)
                                .border(1.dp, if (isPinned) ElectricCyan else GlassBorder, RoundedCornerShape(12.dp))
                                .combinedClickable(
                                    onClick = { onFolderSelected(folder.name) },
                                    onLongClick = {
                                        viewModel.togglePinFolder(folder.name)
                                    }
                                )
                                .padding(12.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(FolderIcon, null, tint = ElectricCyan, modifier = Modifier.size(36.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        if (folder.hasUnplayed) {
                                            Box(
                                                modifier = Modifier
                                                    .size(8.dp)
                                                    .background(NeonGreen, CircleShape)
                                            )
                                        }
                                        if (isPinned) {
                                            Icon(Icons.Default.Star, null, tint = ElectricCyan, modifier = Modifier.size(12.dp))
                                        }
                                    }
                                }
                                Text(
                                    folder.name,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    "${folder.videoCount} media files • ${formatFileSize(folder.totalSize)}",
                                    color = MutedTextView,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                    if (pair.size < 2) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        } else {
            items(currentFoldersList) { folder ->
                val isPinned = pinnedFolders.contains(folder.name)
                Card(
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = GlassBackground),
                    modifier = Modifier
                        .fillMaxWidth()
                        .combinedClickable(
                            onClick = { onFolderSelected(folder.name) },
                            onLongClick = { viewModel.togglePinFolder(folder.name) }
                        )
                        .border(1.dp, if (isPinned) ElectricCyan else GlassBorder, RoundedCornerShape(10.dp))
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(FolderIcon, null, tint = ElectricCyan, modifier = Modifier.size(32.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(folder.name, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                                if (folder.hasUnplayed) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .background(NeonGreen, CircleShape)
                                    )
                                }
                            }
                            Text(
                                "${folder.videoCount} videos • ${formatFileSize(folder.totalSize)}",
                                color = MutedTextView,
                                fontSize = 11.sp
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            if (isPinned) {
                                Icon(Icons.Default.Star, null, tint = ElectricCyan, modifier = Modifier.size(14.dp))
                            }
                            IconButton(onClick = { viewModel.toggleHideFolder(folder.name) }) {
                                Icon(Icons.Default.Close, null, tint = MutedTextView, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

// ------------------------------------------------------------
// Folder property model
// ------------------------------------------------------------
data class LibraryFolderItem(
    val name: String,
    val videoCount: Int,
    val hasUnplayed: Boolean,
    val lastModified: Long,
    val totalSize: Long
)
