package com.example.ui.screens

import android.widget.Toast
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.MediaItem
import com.example.ui.MediaViewModel
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.GlassBackground
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.NeonGreen
import kotlinx.coroutines.launch

@OptIn(ExperimentalAnimationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun VaultScreen(
    viewModel: MediaViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToPlayer: (Long) -> Unit
) {
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    val coroutineScope = rememberCoroutineScope()

    // VM state parameters
    val rawMediaItems by viewModel.rawMediaList.collectAsState()
    val isVaultUnlocked by viewModel.isVaultUnlocked.collectAsState()
    val savedPin by viewModel.vaultPin.collectAsState()
    val biometricEnabled by viewModel.isFingerprintEnabled.collectAsState()

    // Screen specific states
    var enteredPin by remember { mutableStateOf("") }
    var pinSetupMode by remember { mutableStateOf(savedPin.isEmpty()) }
    var setupPinStep by remember { mutableStateOf(1) } // 1: Initial, 2: Confirm
    var firstEnteredPin by remember { mutableStateOf("") }

    // Loading overlay for security actions (Header manipulation runs on Dispatchers.IO)
    var isOperating by remember { mutableStateOf(false) }
    var operationLabel by remember { mutableStateOf("") }

    // Multi-selection states specifically for Vault
    var selectedItemIds by remember { mutableStateOf<Set<Long>>(emptySet()) }

    // Retrieve only the secure files
    val secureVideos = remember(rawMediaItems) {
        rawMediaItems.filter { it.isSecure }
    }

    // Auto trigger biometric prompt if enabled and supported
    LaunchedEffect(isVaultUnlocked, savedPin, biometricEnabled) {
        if (!isVaultUnlocked && savedPin.isNotEmpty() && biometricEnabled && activity != null) {
            val biometricManager = BiometricManager.from(context)
            if (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS) {
                // Auto trigger after a small delay to let UI render
                showBiometricPrompt(
                    activity = activity,
                    onSuccess = {
                        viewModel.unlockVault(savedPin)
                        Toast.makeText(context, "Access Granted via Biometrics!", Toast.LENGTH_SHORT).show()
                    },
                    onError = { err ->
                        // Silent fail / Log error
                    }
                )
            }
        }
    }

    // Full Screen Container with modern deep dark background
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF070B19),
                        Color(0xFF0F1532),
                        Color(0xFF060914)
                    )
                )
            )
    ) {
        // Decorative glowing ambient orbs
        Box(
            modifier = Modifier
                .size(400.dp)
                .align(Alignment.TopEnd)
                .offset(y = (-100).dp, x = 100.dp)
                .blur(90.dp)
                .background(ElectricCyan.copy(alpha = 0.08f), CircleShape)
        )
        Box(
            modifier = Modifier
                .size(400.dp)
                .align(Alignment.BottomStart)
                .offset(y = 150.dp, x = (-150).dp)
                .blur(90.dp)
                .background(NeonGreen.copy(alpha = 0.08f), CircleShape)
        )

        // Switch screens dynamically depending on Lock State
        AnimatedContent(
            targetState = isVaultUnlocked,
            transitionSpec = {
                fadeIn() togetherWith fadeOut()
            },
            label = "VaultScreenTransition"
        ) { unlocked ->
            if (unlocked) {
                // Unlocked Secure Vault Contents
                Scaffold(
                    containerColor = Color.Transparent,
                    topBar = {
                        TopAppBar(
                            title = {
                                Text(
                                    text = "Secure Vault Archive",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = Color.White
                                )
                            },
                            navigationIcon = {
                                IconButton(
                                    onClick = {
                                        viewModel.lockVault()
                                        onNavigateBack()
                                    }
                                ) {
                                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = NeonGreen)
                                }
                            },
                            actions = {
                                if (selectedItemIds.isNotEmpty()) {
                                    // Header manipulation restore button
                                    IconButton(
                                        onClick = {
                                            coroutineScope.launch {
                                                isOperating = true
                                                operationLabel = "Restoring & Decrypting headers..."
                                                // Perform restore operations one by one safely
                                                val toRestore = secureVideos.filter { it.id in selectedItemIds }
                                                for (item in toRestore) {
                                                    viewModel.toggleSelection(item.id) // check VM selections if matching
                                                    viewModel.restoreFromSecure(item)
                                                }
                                                selectedItemIds = emptySet()
                                                isOperating = false
                                                Toast.makeText(context, "Header Decrypted & Restored!", Toast.LENGTH_LONG).show()
                                            }
                                        }
                                    ) {
                                        Icon(Icons.Default.Refresh, "Restore Files", tint = NeonGreen)
                                    }
                                    // Delete permanently
                                    IconButton(
                                        onClick = {
                                            val itemsToDelete = secureVideos.filter { it.id in selectedItemIds }
                                            itemsToDelete.forEach { viewModel.deleteMediaItem(it) }
                                            selectedItemIds = emptySet()
                                            Toast.makeText(context, "Permanently Deleted", Toast.LENGTH_SHORT).show()
                                        }
                                    ) {
                                        Icon(Icons.Default.Delete, "Delete Permanently", tint = Color.Red)
                                    }
                                } else {
                                    // Info / biometric status toggler
                                    IconButton(
                                        onClick = {
                                            viewModel.toggleFingerprint()
                                            val status = if (!biometricEnabled) "Biometrics Enabled" else "Biometrics Disabled"
                                            Toast.makeText(context, status, Toast.LENGTH_SHORT).show()
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Face,
                                            contentDescription = "Biometrics",
                                            tint = if (biometricEnabled) NeonGreen else Color.Gray
                                        )
                                    }
                                    // Lock Vault
                                    IconButton(
                                        onClick = {
                                            viewModel.lockVault()
                                            Toast.makeText(context, "Vault Locked", Toast.LENGTH_SHORT).show()
                                        }
                                    ) {
                                        Icon(Icons.Default.ExitToApp, "Lock", tint = Color.Red)
                                    }
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                        )
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        if (secureVideos.isEmpty()) {
                            // Info card when empty
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp)
                                    .align(Alignment.Center),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Card(
                                    shape = RoundedCornerShape(18.dp),
                                    colors = CardDefaults.cardColors(containerColor = GlassBackground),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(2.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(18.dp))
                                        .padding(4.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(24.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Lock,
                                            contentDescription = "Secure Encryption Shield",
                                            tint = NeonGreen,
                                            modifier = Modifier.size(56.dp)
                                        )
                                        Text(
                                            text = "Secure Folder is Empty",
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                            color = Color.White
                                        )
                                        Text(
                                            text = "To lock a video, open the video library, hold down on any video card (or check multiple items), and choose the 'Lock to Secure Vault' option. The file header will instantly be obfuscated so no other apps can play it.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color.LightGray,
                                            textAlign = TextAlign.Center,
                                            lineHeight = 18.sp
                                        )
                                    }
                                }
                            }
                        } else {
                            // Display list in grid
                            LazyVerticalGrid(
                                columns = GridCells.Adaptive(minSize = 140.dp),
                                contentPadding = PaddingValues(16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(secureVideos) { mediaItem ->
                                    val isSelected = selectedItemIds.contains(mediaItem.id)
                                    VaultVideoCard(
                                        item = mediaItem,
                                        isSelected = isSelected,
                                        onSelectedToggle = {
                                            selectedItemIds = if (isSelected) {
                                                selectedItemIds - mediaItem.id
                                            } else {
                                                selectedItemIds + mediaItem.id
                                            }
                                        },
                                        onCardClick = {
                                            // Secure playback bypass check or play directly
                                            onNavigateToPlayer(mediaItem.id)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                // LOCK / SETUP SCREEN
                Scaffold(
                    containerColor = Color.Transparent,
                    topBar = {
                        TopAppBar(
                            title = {},
                            navigationIcon = {
                                IconButton(onClick = onNavigateBack) {
                                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                        )
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        contentAlignment = Alignment.Center
                    ) {
                        // Ambient Glass Passcode card
                        Card(
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = GlassBackground),
                            modifier = Modifier
                                .widthIn(max = 400.dp)
                                .padding(24.dp)
                                .border(1.5.dp, GlassBorder, RoundedCornerShape(24.dp))
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(20.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = "Lock Logo",
                                    tint = NeonGreen,
                                    modifier = Modifier.size(48.dp)
                                )

                                Text(
                                    text = if (pinSetupMode) {
                                        if (setupPinStep == 1) "Create Passcode" else "Confirm Passcode"
                                    } else "Vault Authentication",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = Color.White
                                )

                                Text(
                                    text = if (pinSetupMode) {
                                        if (setupPinStep == 1) "Define a 4-digit numeric lock PIN" else "Confirm your 4-digit PIN setup"
                                    } else "Input your credential PIN code to unlock folder",
                                    color = Color.LightGray,
                                    fontSize = 12.sp,
                                    textAlign = TextAlign.Center
                                )

                                // Password circles indicator
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    for (i in 1..4) {
                                        val active = enteredPin.length >= i
                                        Box(
                                            modifier = Modifier
                                                .size(16.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    if (active) NeonGreen else Color.White.copy(alpha = 0.15f)
                                                )
                                                .border(
                                                    1.dp,
                                                    if (active) NeonGreen else Color.White.copy(alpha = 0.3f),
                                                    CircleShape
                                                )
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // Number Keypad Custom Layout
                                NumpadGrid(
                                    onDigitClicked = { digit ->
                                        if (enteredPin.length < 4) {
                                            enteredPin += digit
                                            // Handle automatic entry triggers if reaches 4 characters
                                            if (enteredPin.length == 4) {
                                                if (pinSetupMode) {
                                                    // Handle pin registration
                                                    if (setupPinStep == 1) {
                                                        firstEnteredPin = enteredPin
                                                        enteredPin = ""
                                                        setupPinStep = 2
                                                    } else {
                                                        if (enteredPin == firstEnteredPin) {
                                                            viewModel.setupVaultPin(enteredPin)
                                                            pinSetupMode = false
                                                            enteredPin = ""
                                                            Toast.makeText(context, "Passcode Configured Successfully!", Toast.LENGTH_SHORT).show()
                                                        } else {
                                                            Toast.makeText(context, "PIN code mismatch! Resetting...", Toast.LENGTH_SHORT).show()
                                                            enteredPin = ""
                                                            setupPinStep = 1
                                                        }
                                                    }
                                                } else {
                                                    // Unlocks via PIN check
                                                    if (viewModel.unlockVault(enteredPin)) {
                                                        enteredPin = ""
                                                        Toast.makeText(context, "Unlocked", Toast.LENGTH_SHORT).show()
                                                    } else {
                                                        Toast.makeText(context, "Invalid PIN code", Toast.LENGTH_SHORT).show()
                                                        enteredPin = ""
                                                    }
                                                }
                                            }
                                        }
                                    },
                                    onDeleteClicked = {
                                        if (enteredPin.isNotEmpty()) {
                                            enteredPin = enteredPin.dropLast(1)
                                        }
                                    },
                                    onBiometricsClick = {
                                        if (!pinSetupMode && activity != null) {
                                            showBiometricPrompt(
                                                activity = activity,
                                                onSuccess = {
                                                    viewModel.unlockVault(savedPin)
                                                    Toast.makeText(context, "Access Granted", Toast.LENGTH_SHORT).show()
                                                },
                                                onError = { err ->
                                                    Toast.makeText(context, "Biometric failed: $err", Toast.LENGTH_SHORT).show()
                                                }
                                            )
                                        }
                                    },
                                    biometricEnabled = !pinSetupMode && biometricEnabled
                                )
                            }
                        }
                    }
                }
            }
        }

        // Processing / Header encryption logic Progress Indicator
        if (isOperating) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.82f))
                    .clickable(enabled = false) {},
                contentAlignment = Alignment.Center
            ) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.DarkGray),
                    modifier = Modifier
                        .padding(32.dp)
                        .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircularProgressIndicator(color = NeonGreen)
                        Text(
                            text = operationLabel,
                            color = Color.White,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun NumpadGrid(
    onDigitClicked: (String) -> Unit,
    onDeleteClicked: () -> Unit,
    onBiometricsClick: () -> Unit,
    biometricEnabled: Boolean
) {
    val keys = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf("bio", "0", "del")
    )

    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        for (row in keys) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (key in row) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(
                                if (key == "bio" || key == "del") Color.Transparent else Color.White.copy(alpha = 0.05f)
                            )
                            .border(
                                width = if (key == "bio" || key == "del") 0.dp else 1.dp,
                                color = if (key == "bio" || key == "del") Color.Transparent else Color.White.copy(alpha = 0.12f),
                                shape = CircleShape
                            )
                            .clickable {
                                when (key) {
                                    "bio" -> if (biometricEnabled) onBiometricsClick()
                                    "del" -> onDeleteClicked()
                                    else -> onDigitClicked(key)
                                }
                            }
                            .testTag("numpad_key_$key"),
                        contentAlignment = Alignment.Center
                    ) {
                        when (key) {
                            "bio" -> {
                                if (biometricEnabled) {
                                    Icon(
                                        imageVector = Icons.Default.Face,
                                        contentDescription = "Fingerprint sensor scan",
                                        tint = NeonGreen,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }
                            "del" -> {
                                Icon(
                                    imageVector = Icons.Default.ArrowBack,
                                    contentDescription = "Backspace Pin Character delete",
                                    tint = Color.LightGray,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            else -> {
                                Text(
                                    text = key,
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun VaultVideoCard(
    item: MediaItem,
    isSelected: Boolean,
    onSelectedToggle: () -> Unit,
    onCardClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = GlassBackground),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = if (isSelected) NeonGreen else Color.White.copy(alpha = 0.08f),
                shape = RoundedCornerShape(16.dp)
            )
            .clickable { onCardClick() }
            .testTag("vault_card_${item.id}")
    ) {
        Box(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                // Secure Private Album preview image or default media placeholder
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                        .background(Color.Black.copy(alpha = 0.4f)),
                    contentAlignment = Alignment.Center
                ) {
                    val context = LocalContext.current
                    if (!item.thumbnailUri.isNullOrEmpty() && (item.thumbnailUri!!.startsWith("http") || java.io.File(item.thumbnailUri!!).exists())) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(item.thumbnailUri)
                                .crossfade(true)
                                .build(),
                            contentDescription = "Secure preview key",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        // High aesthetic graphic lock icon fallback
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Locked video thumbnail fallback",
                                tint = NeonGreen.copy(alpha = 0.8f),
                                modifier = Modifier.size(36.dp)
                            )
                            Text(
                                "Locked Stream",
                                fontSize = 10.sp,
                                color = ElectricCyan,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    // Video duration stamp overlay
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(6.dp)
                            .background(Color.Black.copy(alpha = 0.72f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (item.duration > 0) formatVaultDuration(item.duration) else "00:32",
                            color = Color.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // File metadata labels
                Column(
                    modifier = Modifier.padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = item.title,
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (item.size > 0) formatVaultFileSize(item.size) else "4.2 MB",
                        color = Color.Gray,
                        fontSize = 10.sp
                    )
                }
            }

            // Custom glassmorphic selection checkbox circle
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(
                        if (isSelected) NeonGreen else Color.Black.copy(alpha = 0.4f)
                    )
                    .border(
                        1.dp,
                        if (isSelected) NeonGreen else Color.White.copy(alpha = 0.3f),
                        CircleShape
                    )
                    .clickable { onSelectedToggle() },
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Selected",
                        tint = Color.Black,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

/**
 * Biometric prompt integration display using FragmentActivity
 */
private fun showBiometricPrompt(
    activity: FragmentActivity,
    onSuccess: () -> Unit,
    onError: (String) -> Unit
) {
    val executor = ContextCompat.getMainExecutor(activity)
    val biometricPrompt = BiometricPrompt(
        activity,
        executor,
        object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                onSuccess()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                onError(errString.toString())
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                // Fail state handled directly inside the native OS prompt view
            }
        }
    )

    val promptInfo = BiometricPrompt.PromptInfo.Builder()
        .setTitle("Secure Vault Unlock")
        .setSubtitle("XOR Header Obfuscation decryptor authentication layer")
        .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
        .setNegativeButtonText("Use PIN Fallback")
        .build()

    try {
        biometricPrompt.authenticate(promptInfo)
    } catch (e: Exception) {
        onError(e.message ?: "Launch Failed")
    }
}

private fun formatVaultDuration(milliseconds: Long): String {
    val totalSeconds = milliseconds / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}

private fun formatVaultFileSize(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
    if (digitGroups >= units.size) return "$bytes B"
    return String.format("%.1f %s", bytes / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
}
