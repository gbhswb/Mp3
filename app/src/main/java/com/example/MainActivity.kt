package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ui.MediaViewModel
import com.example.ui.screens.PlayerScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.VideosScreen
import com.example.ui.screens.VaultScreen
import com.example.ui.theme.MrPlayerTheme

class MainActivity : FragmentActivity() {

    private val viewModel: MediaViewModel by viewModels()

    private val permissionsToRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(
            Manifest.permission.READ_MEDIA_VIDEO,
            Manifest.permission.READ_MEDIA_IMAGES
        )
    } else {
        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            viewModel.scanMedia()
        } else {
            Toast.makeText(this, "Permission denied. Streaming samples will be used.", Toast.LENGTH_LONG).show()
            viewModel.scanMedia()
        }
    }

    private var mediaStoreObserver: android.database.ContentObserver? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // ALWAYS initialize the media repository by scanning (it will prepopulate samples if empty)
        // This guarantees that samples are ready immediately without waiting or blocking.
        viewModel.scanMedia()

        // Safely check storage permissions at startup without popping up blocking dialogues automatically on headless emulators.
        // Doing this ensures high-quality streaming preview works flawlessly.
        checkStoragePermissions(forceRequest = false)
        registerMediaStoreObserver()

        setContent {
            MrPlayerTheme {
                MainAppNavHost(viewModel = viewModel)
            }
        }
    }

    private fun registerMediaStoreObserver() {
        val hasPermission = permissionsToRequest.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
        if (!hasPermission) return // Do not register observer if permission is not granted yet

        try {
            val observer = object : android.database.ContentObserver(android.os.Handler(android.os.Looper.getMainLooper())) {
                override fun onChange(selfChange: Boolean) {
                    super.onChange(selfChange)
                    viewModel.scanMedia()
                }
            }
            contentResolver.registerContentObserver(
                android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                true,
                observer
            )
            mediaStoreObserver = observer
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaStoreObserver?.let {
            contentResolver.unregisterContentObserver(it)
        }
    }

    private fun checkStoragePermissions(forceRequest: Boolean = false) {
        val hasPermission = permissionsToRequest.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
        if (hasPermission) {
            viewModel.scanMedia()
        } else if (forceRequest) {
            requestPermissionLauncher.launch(permissionsToRequest)
        } else {
            // Safe fallback at startup: Don't block, proceed with prepopulating samples normally!
            viewModel.scanMedia()
        }
    }
}

@Composable
fun MainAppNavHost(
    viewModel: MediaViewModel,
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        NavHost(
            navController = navController,
            startDestination = "videos"
        ) {
            composable("videos") {
                VideosScreen(
                    viewModel = viewModel,
                    onNavigateToPlayer = { mediaId ->
                        navController.navigate("player/$mediaId")
                    },
                    onNavigateToSettings = {
                        navController.navigate("settings")
                    },
                    onNavigateToVault = {
                        navController.navigate("vault")
                    }
                )
            }

            composable(
                route = "player/{mediaId}",
                arguments = listOf(navArgument("mediaId") { type = NavType.LongType })
            ) { backStackEntry ->
                val mediaId = backStackEntry.arguments?.getLong("mediaId") ?: 0L
                PlayerScreen(
                    mediaId = mediaId,
                    viewModel = viewModel,
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }

            composable("settings") {
                SettingsScreen(
                    viewModel = viewModel,
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }

            composable("vault") {
                VaultScreen(
                    viewModel = viewModel,
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                    onNavigateToPlayer = { mediaId ->
                        navController.navigate("player/$mediaId")
                    }
                )
            }
        }
    }
}
