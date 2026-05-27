package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MediaViewModel
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.GlassBackground
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.NeonGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: MediaViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val mediaList by viewModel.rawMediaList.collectAsState()
    val folders by viewModel.folders.collectAsState()
    val scrollState = rememberScrollState()

    // Preferences states
    val mrFlixLayout by viewModel.mrFlixLayoutStyle.collectAsState()
    val vidsLayout by viewModel.vidsLayoutStyle.collectAsState()
    val libraryLayout by viewModel.libraryLayoutStyle.collectAsState()
    val librarySort by viewModel.librarySortOption.collectAsState()

    // Security Info
    val vaultPinSaved by viewModel.vaultPin.collectAsState()
    val isBiometricEnabled by viewModel.isFingerprintEnabled.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "APP SETTINGS & LAYOUTS",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp
                        )
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("settings_back_button")
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Go Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Layout & Display Customizations Part
            Text(
                text = "LAYOUTS & APPEARANCE",
                style = MaterialTheme.typography.labelLarge,
                color = ElectricCyan,
                fontWeight = FontWeight.Bold
            )

            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = GlassBackground),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, GlassBorder, RoundedCornerShape(12.dp))
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    SettingsSegmentSelector(
                        title = "Mr. Flix Display Mode",
                        options = listOf("3D STACK", "GRID VIEW", "DETAILED"),
                        selectedIdx = mrFlixLayout,
                        onOptionSelected = { viewModel.setMrFlixLayoutStyle(it) }
                    )

                    SettingsSegmentSelector(
                        title = "Vids Grid Layout",
                        options = listOf("LARGE 1-COL", "COMPACT 2-COL", "LIST VIEW"),
                        selectedIdx = vidsLayout,
                        onOptionSelected = { viewModel.setVidsLayoutStyle(it) }
                    )

                    SettingsSegmentSelector(
                        title = "Library Folder View",
                        options = listOf("GRID OF FOLDERS", "LIST OF FOLDERS"),
                        selectedIdx = libraryLayout,
                        onOptionSelected = { viewModel.setLibraryLayoutStyle(it) }
                    )

                    SettingsSegmentSelector(
                        title = "Sort Folders By",
                        options = listOf("NAME A-Z", "NAME Z-A", "DATE", "COUNT"),
                        selectedIdx = librarySort,
                        onOptionSelected = { viewModel.setLibrarySortOption(it) }
                    )
                }
            }

            // Security Settings Part
            Text(
                text = "PRIVACY & SECURITY",
                style = MaterialTheme.typography.labelLarge,
                color = NeonGreen,
                fontWeight = FontWeight.Bold
            )

            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = GlassBackground),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, GlassBorder, RoundedCornerShape(12.dp))
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val statusText = if (vaultPinSaved.isEmpty()) "Not Set up yet" else "Active (Locked)"
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Secure Folder Lock", color = Color.White, style = MaterialTheme.typography.bodyMedium)
                        Text(statusText, color = if (vaultPinSaved.isEmpty()) Color.Red else NeonGreen, fontWeight = FontWeight.Bold)
                    }

                    if (vaultPinSaved.isNotEmpty()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Biometric Fingerprint Lock", color = Color.White, style = MaterialTheme.typography.bodyMedium)
                            Switch(
                                checked = isBiometricEnabled,
                                onCheckedChange = { viewModel.toggleFingerprint() },
                                colors = SwitchDefaults.colors(checkedThumbColor = NeonGreen)
                            )
                        }

                        Button(
                            onClick = {
                                viewModel.resetVaultSettings()
                                Toast.makeText(context, "Secure vault settings completely flushed and reset!", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Reset PIN & Restore Hidden Files", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Text(
                            "Secure Folder PIN can be configured by launching Secure Vault item on the Library view.",
                            fontSize = 11.sp,
                            color = Color.LightGray
                        )
                    }
                }
            }

            // Caching Database Stats Block
            Text(
                text = "DATABASE STATS",
                style = MaterialTheme.typography.labelLarge,
                color = NeonGreen,
                fontWeight = FontWeight.Bold
            )

            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = GlassBackground),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, GlassBorder, RoundedCornerShape(12.dp))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatRow(label = "Total Indexed Videos", value = "${mediaList.size}")
                    StatRow(label = "Grouped Folders", value = "${folders.size}")
                    StatRow(label = "Favorite Marked", value = "${mediaList.count { it.isFavorite }}")
                    StatRow(label = "Secure Vault Files", value = "${mediaList.count { it.isSecure }}")
                }
            }

            // Scanning Tools Options Block
            Text(
                text = "DEVELOPER CONTROLS",
                style = MaterialTheme.typography.labelLarge,
                color = NeonGreen,
                fontWeight = FontWeight.Bold
            )

            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = GlassBackground),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, GlassBorder, RoundedCornerShape(12.dp))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Manual Storage Rescan Action Button
                    Button(
                        onClick = { viewModel.scanMedia() },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonGreen),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("rescan_storage_button")
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.Black)
                            Text("Trigger Android Storage Rescan", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Database wipe & format Action Button
                    Button(
                        onClick = { viewModel.clearCache() },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("clear_db_cache_button")
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, tint = Color.White)
                            Text("Flush Room Database Cache", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Engine / Framework Specifications
            Text(
                text = "SOFTWARE SPECS",
                style = MaterialTheme.typography.labelLarge,
                color = ElectricCyan,
                fontWeight = FontWeight.Bold
            )

            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = GlassBackground),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, GlassBorder, RoundedCornerShape(12.dp))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Mr Player Engine v1.0",
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                    Text(
                        text = "Built on highly modular hardware acceleration pipelines. Decodes hardware AVC/HEVC streams smoothly using Media3 ExoPlayer decoders, persist reactive schemas inside local SQLite files via Android Room ORM.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun SettingsSegmentSelector(
    title: String,
    options: List<String>,
    selectedIdx: Int,
    onOptionSelected: (Int) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, style = MaterialTheme.typography.bodyMedium, color = Color.White, fontWeight = FontWeight.SemiBold)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                .border(1.dp, GlassBorder, RoundedCornerShape(10.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            options.forEachIndexed { idx, opt ->
                val isSelected = selectedIdx == idx
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) NeonGreen else Color.Transparent)
                        .clickable { onOptionSelected(idx) }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = opt,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) Color.Black else Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun StatRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, color = Color.White, style = MaterialTheme.typography.bodyMedium)
        Text(text = value, color = ElectricCyan, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
    }
}
