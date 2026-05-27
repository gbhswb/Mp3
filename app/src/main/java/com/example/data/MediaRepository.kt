package com.example.data

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.media.MediaScannerConnection
import android.provider.MediaStore
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class MediaRepository(
    private val context: Context,
    private val mediaDao: MediaDao
) {
    val allMedia: Flow<List<MediaItem>> = mediaDao.getAllMedia()
    val allFolders: Flow<List<String>> = mediaDao.getFolders()
    val favorites: Flow<List<MediaItem>> = mediaDao.getFavorites()

    fun getMediaInFolder(folder: String): Flow<List<MediaItem>> {
        return mediaDao.getMediaInFolder(folder)
    }

    fun searchMedia(query: String): Flow<List<MediaItem>> {
        return mediaDao.searchMedia(query)
    }

    suspend fun toggleFavorite(mediaItem: MediaItem) {
        mediaDao.updateFavorite(mediaItem.id, !mediaItem.isFavorite)
    }

    suspend fun savePlaybackPosition(id: Long, position: Long) {
        mediaDao.updatePlaybackPosition(id, position)
    }

    suspend fun deleteVideo(mediaItem: MediaItem) {
        mediaDao.delete(mediaItem)
    }

    suspend fun scanLocalVideos(
        mrFlixFolder: String = "Flex",
        vidsFolder: String = "Vids"
    ) = withContext(Dispatchers.IO) {
        try {
            val resolver: ContentResolver = context.contentResolver
            val localVideos = mutableListOf<MediaItem>()

            val uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            val projection = arrayOf(
                MediaStore.Video.Media._ID,
                MediaStore.Video.Media.DISPLAY_NAME,
                MediaStore.Video.Media.DATA,
                MediaStore.Video.Media.DURATION,
                MediaStore.Video.Media.SIZE,
                MediaStore.Video.Media.BUCKET_DISPLAY_NAME,
                MediaStore.Video.Media.MIME_TYPE
            )

            try {
                resolver.query(uri, projection, null, null, null)?.use { cursor ->
                    val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                    val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
                    val pathColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA)
                    val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
                    val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
                    val bucketColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.BUCKET_DISPLAY_NAME)
                    val mimeColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.MIME_TYPE)

                    while (cursor.moveToNext()) {
                        val id = cursor.getLong(idColumn)
                        val name = cursor.getString(nameColumn) ?: "Video $id"
                        val path = cursor.getString(pathColumn) ?: ""
                        val duration = cursor.getLong(durationColumn)
                        val size = cursor.getLong(sizeColumn)
                        val mime = cursor.getString(mimeColumn)

                        // EXCLUDE secure vault folders or .locked files from device scanner index
                        if (path.contains(".secure_vault", ignoreCase = true) || path.endsWith(".locked", ignoreCase = true)) {
                            continue
                        }

                        // Group by BUCKET_DISPLAY_NAME, with fallback to parent directory name or "Videos"
                        var folder = cursor.getString(bucketColumn)
                        if (folder.isNullOrEmpty() && path.isNotEmpty()) {
                            val file = java.io.File(path)
                            folder = file.parentFile?.name
                        }
                        if (folder.isNullOrEmpty()) {
                            folder = "Videos"
                        }

                        val contentUri = ContentUris.withAppendedId(
                            MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id
                        ).toString()

                        val ytid = extractYoutubeId(name)
                        val thumb = if (ytid != null) {
                            "https://img.youtube.com/vi/$ytid/maxresdefault.jpg"
                        } else {
                            contentUri
                        }

                        // Smart detection of Mr. Flix / Flex folders
                        val isMrFlix = folder.contains(mrFlixFolder, ignoreCase = true) || 
                                       path.contains(mrFlixFolder, ignoreCase = true) ||
                                       folder.contains("Flex", ignoreCase = true) || 
                                       path.contains("Flex", ignoreCase = true) ||
                                       folder.contains("Mr. Will Flex", ignoreCase = true) || 
                                       path.contains("Mr. Will Flex", ignoreCase = true) ||
                                       folder.contains("Mr. Flix", ignoreCase = true) || 
                                       path.contains("Mr. Flix", ignoreCase = true)

                        var series: String? = null
                        var seasonNum: Int? = null
                        var epNum: Int? = null

                        if (isMrFlix) {
                            folder = mrFlixFolder
                            val parts = path.split("/")
                            val idx = parts.indexOfLast { 
                                it.equals(mrFlixFolder, ignoreCase = true) || 
                                it.equals("Flex", ignoreCase = true) || 
                                it.equals("Mr. Will Flex", ignoreCase = true) || 
                                it.equals("Mr. Flix", ignoreCase = true)
                            }
                            if (idx != -1 && idx < parts.size - 2) {
                                series = parts[idx + 1]
                                epNum = parseEpisodeNumber(name)
                                seasonNum = parseSeasonNumber(name) ?: 1
                            }
                        } else {
                            // Dynamic matching for custom Vids folders
                            val isCustomVids = folder.contains(vidsFolder, ignoreCase = true) || 
                                               path.contains(vidsFolder, ignoreCase = true)
                            if (isCustomVids) {
                                folder = vidsFolder
                            }
                        }

                        localVideos.add(
                            MediaItem(
                                title = name,
                                path = path,
                                duration = duration,
                                size = size,
                                folder = folder,
                                mimeType = mime,
                                thumbnailUri = thumb,
                                seriesName = series,
                                seasonNumber = seasonNum,
                                episodeNumber = epNum
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e("MediaRepository", "Error scanning videos: ${e.message}")
            }

            val dbVideos = mediaDao.getAllMediaList()
            val dbPaths = dbVideos.map { it.path }.toSet()
            val localPaths = localVideos.map { it.path }.toSet()

            // 1. Find videos that are in local but not in database yet
            val newVideos = localVideos.filter { it.path !in dbPaths }
            if (newVideos.isNotEmpty()) {
                mediaDao.insertMedia(newVideos)
            }

            // 2. Find videos that were scanned before but are no longer present on device storage (exclude secure items)
            val removedVideos = dbVideos.filter { !it.path.startsWith("http") && !it.isSecure && it.path !in localPaths }
            for (removed in removedVideos) {
                mediaDao.delete(removed)
            }

            // 3. If there is absolutely no movies/videos left (e.g. fresh app or no storage permissions/no films), prepopulate samples
            val remainingDbVideos = mediaDao.getAllMediaList()
            if (remainingDbVideos.isEmpty()) {
                prepopulateSamples()
            }
            Unit
        } catch (e: Exception) {
            Log.e("MediaRepository", "Fatal error scanning videos: ${e.message}", e)
            Unit
        }
    }

    private fun extractYoutubeId(fileName: String): String? {
        val regex = "\\[([a-zA-Z0-9_-]{11})\\]".toRegex()
        return regex.find(fileName)?.groups?.get(1)?.value
    }

    private fun parseEpisodeNumber(fileName: String): Int? {
        val regex = "(?i)E(\\d+)".toRegex()
        val match = regex.find(fileName)
        if (match != null) return match.groups[1]?.value?.toIntOrNull()
        val numRegex = "(\\d+)".toRegex()
        return numRegex.find(fileName)?.groups?.get(1)?.value?.toIntOrNull()
    }

    private fun parseSeasonNumber(fileName: String): Int? {
        val regex = "(?i)S(\\d+)".toRegex()
        return regex.find(fileName)?.groups?.get(1)?.value?.toIntOrNull()
    }

    private suspend fun prepopulateSamples() {
        val samples = listOf(
            // --- Flex (Movies: 2:3 Aspect ratio posters) ---
            MediaItem(
                title = "Modern Architecture & Design",
                path = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/Sintel.mp4",
                duration = 8100 * 1000L, // 2H 15M
                size = 185 * 1024 * 1024L,
                folder = "Flex",
                mimeType = "video/mp4",
                thumbnailUri = "https://lh3.googleusercontent.com/aida-public/AB6AXuCfSoexgtKiJONAA4OPH_KD429tD5z4y7FY9Dce3c96OVijGHw3wTcYcypRSpVxr4Z5kxs-Jd3pNWdRNHZzayCoj1Q4tj35d7zmJ2V957AUNWEWVQim5V7Em61qTxK5LmJ9bXZqXovwXUNEcqdvDW6nY6LRIsHQhV4sNufx0C1z_0BCLWhfUG0MEhuWa5bcokP393nuz9DdOX_0_nbwrYfUnI4HygRkMOHF3Ca59_xIlig8X3xSLcNY7YKjATucKnwdEU7q6P0cGq9Z",
                isNew = true,
                seriesName = null
            ),
            MediaItem(
                title = "Arrakis Rising",
                path = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4",
                duration = 10140 * 1000L,
                size = 210 * 1024 * 1024L,
                folder = "Flex",
                mimeType = "video/mp4",
                thumbnailUri = "https://lh3.googleusercontent.com/aida-public/AB6AXuAMLbFWvu7zM2ezTZViVq52a-RPlLE3wSoiLqDDNZi0jC2ahp0McgV7vxix4MSc9njuKeVYhkvrw2bVRMMZI1p3SLkzqvpIwYbqMJ_196dYQhGftX9elcvvCvzTeQUJT5EbjiwmhuM30GkYomgl_phV-7qJvP9uDO2zIMxoSlyOxrlqLaMAE-vCSiAKBva7WtDxb7jr2rtH78ak0Jm-ejyAGwr-4ZkU8Wq0xH85QzJmECwLPEVXSJUPILEV25azzSKeQX_RtwCjWC6w",
                isNew = true,
                seriesName = null
            ),
            MediaItem(
                title = "Project Zero",
                path = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
                duration = 9800 * 1000L,
                size = 230 * 1024 * 1024L,
                folder = "Flex",
                mimeType = "video/mp4",
                thumbnailUri = "https://lh3.googleusercontent.com/aida-public/AB6AXuCdE8Z192rQtagSP_5lBl4oD8EOXMyeMV_JlEkP6jclo9awv3gPbPFJ3rsfGuFCfOsqxB_dvWp0_Gbf5o4_kwLpiRxUkfuRQw5vIVt8VYkDEQ4PPtvxGWDRhg0ogH-J1_ugGjtelALxQfTc-QMh_jWFuJKYt4hmEoMSj0xY-KejVaZ1hngVJScS16zvp_ZzivgP5vFdCV9-eyYIPy1r_KUSOQkcKlOKSkN4bWS1Dw4ptzpdJaWx_rgPNNzzqzVGWfDfZXXbkWVMIQp7",
                isNew = true,
                seriesName = null
            ),
            MediaItem(
                title = "The Matrix (1999)",
                path = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4",
                duration = 8160 * 1000L,
                size = 172 * 1024 * 1024L,
                folder = "Flex",
                mimeType = "video/mp4",
                thumbnailUri = "https://images.unsplash.com/photo-1526374965328-7f61d4dc18c5?w=560&auto=format&fit=crop",
                isNew = false,
                seriesName = null
            ),

            // --- Flex Series (Grouped under "Stranger Things") ---
            MediaItem(
                title = "Stranger Things - S01E01 - The Vanishing of Will Byers",
                path = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
                duration = 2880 * 1000L,
                size = 45 * 1024 * 1024L,
                folder = "Flex",
                mimeType = "video/mp4",
                thumbnailUri = "https://images.unsplash.com/photo-1509281373149-e957c6296406?w=560&auto=format&fit=crop",
                isNew = true,
                seriesName = "Stranger Things",
                seasonNumber = 1,
                episodeNumber = 1
            ),
            MediaItem(
                title = "Stranger Things - S01E02 - The Weirdo on Maple Street",
                path = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
                duration = 3120 * 1000L,
                size = 48 * 1024 * 1024L,
                folder = "Flex",
                mimeType = "video/mp4",
                thumbnailUri = "https://images.unsplash.com/photo-1509281373149-e957c6296406?w=560&auto=format&fit=crop",
                isNew = true,
                seriesName = "Stranger Things",
                seasonNumber = 1,
                episodeNumber = 2
            ),
            MediaItem(
                title = "Stranger Things - S01E03 - Holly Jolly",
                path = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
                duration = 2940 * 1000L,
                size = 42 * 1024 * 1024L,
                folder = "Flex",
                mimeType = "video/mp4",
                thumbnailUri = "https://images.unsplash.com/photo-1509281373149-e957c6296406?w=560&auto=format&fit=crop",
                isNew = true,
                seriesName = "Stranger Things",
                seasonNumber = 1,
                episodeNumber = 3
            ),

            // --- TV Show: Cosmos ---
            MediaItem(
                title = "Cosmos - S01E01 - Standing Up in the Milky Way",
                path = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4",
                duration = 2700 * 1000L,
                size = 38 * 1024 * 1024L,
                folder = "Flex",
                mimeType = "video/mp4",
                thumbnailUri = "https://images.unsplash.com/photo-1462331940025-496dfbfc7564?w=560&auto=format&fit=crop",
                isNew = false,
                seriesName = "Cosmos Spatial",
                seasonNumber = 1,
                episodeNumber = 1
            ),
            MediaItem(
                title = "Cosmos - S01E02 - What Molecules Do",
                path = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4",
                duration = 2750 * 1000L,
                size = 40 * 1024 * 1024L,
                folder = "Flex",
                mimeType = "video/mp4",
                thumbnailUri = "https://images.unsplash.com/photo-1462331940025-496dfbfc7564?w=560&auto=format&fit=crop",
                isNew = true,
                seriesName = "Cosmos Spatial",
                seasonNumber = 1,
                episodeNumber = 2
            ),

            // --- YouTube Downloads (Vids Section: Horizontal 16:9 thumbnails with YT ID extraction) ---
            MediaItem(
                title = "Lofi Hip Hop Radio 🎧 beats to study/relax [dQw4w9WgXcQ]",
                path = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
                duration = 1800 * 1000L,
                size = 112 * 1024 * 1024L,
                folder = "Vids",
                mimeType = "video/mp4",
                thumbnailUri = "https://img.youtube.com/vi/dQw4w9WgXcQ/maxresdefault.jpg",
                isNew = true
            ),
            MediaItem(
                title = "MKBHD - Tech of the Future! [9bZkp7q19f0]",
                path = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4",
                duration = 840 * 1000L,
                size = 62 * 1024 * 1024L,
                folder = "Vids",
                mimeType = "video/mp4",
                thumbnailUri = "https://img.youtube.com/vi/9bZkp7q19f0/maxresdefault.jpg",
                isNew = true
            ),
            MediaItem(
                title = "SpaceX Crew-8 Launch Telemetry Official Live Stream [lW_S9_W_1E8]",
                path = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
                duration = 1500 * 1000L,
                size = 94 * 1024 * 1024L,
                folder = "Vids",
                mimeType = "video/mp4",
                thumbnailUri = "https://img.youtube.com/vi/lW_S9_W_1E8/maxresdefault.jpg",
                isNew = false
            ),
            MediaItem(
                title = "Travel Vlog - Exploring Tokyo Ancient Streets 2026",
                path = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/Sintel.mp4",
                duration = 620 * 1000L,
                size = 55 * 1024 * 1024L,
                folder = "Vids",
                mimeType = "video/mp4",
                thumbnailUri = "https://images.unsplash.com/photo-1501785888041-af3ef285b470?w=500&auto=format&fit=crop",
                isNew = true
            ),

            // --- Other Library Folders (Camera, Downloads, etc.) ---
            MediaItem(
                title = "IMG_6820_Summer_Pool.mp4",
                path = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
                duration = 12 * 1000L,
                size = 18 * 1024 * 1024L,
                folder = "Camera",
                mimeType = "video/mp4",
                thumbnailUri = "https://images.unsplash.com/photo-1519046904884-53103b34b206?w=500&auto=format&fit=crop",
                isNew = true
            ),
            MediaItem(
                title = "IMG_6991_Graduation_Ceremony.mp4",
                path = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
                duration = 153 * 1000L,
                size = 28 * 1024 * 1024L,
                folder = "Camera",
                mimeType = "video/mp4",
                thumbnailUri = "https://images.unsplash.com/photo-1523050854058-8df90110c9f1?w=500&auto=format&fit=crop",
                isNew = false
            ),
            MediaItem(
                title = "WhatsApp_Video_2026-05-18.mp4",
                path = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
                duration = 45 * 1000L,
                size = 4 * 1024 * 1024L,
                folder = "WhatsApp Video",
                mimeType = "video/mp4",
                thumbnailUri = "https://images.unsplash.com/photo-1611162617213-7d7a39e9b1d7?w=500&auto=format&fit=crop",
                isNew = true
            ),
            MediaItem(
                title = "Nature Wildlife Overview.mkv",
                path = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mkv",
                duration = 450 * 1000L,
                size = 120 * 1024 * 1024L,
                folder = "Vids",
                mimeType = "video/x-matroska",
                thumbnailUri = "https://images.unsplash.com/photo-1546182990-dffeafbe841d?w=500&auto=format&fit=crop",
                isNew = true
            ),
            MediaItem(
                title = "Cyberpunk Neo Cityscape.webm",
                path = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.webm",
                duration = 320 * 1000L,
                size = 35 * 1024 * 1024L,
                folder = "Vids",
                mimeType = "video/webm",
                thumbnailUri = "https://images.unsplash.com/photo-1515621061946-eff1c2a352bd?w=500&auto=format&fit=crop",
                isNew = true
            ),
            MediaItem(
                title = "Classic Retro Commercial.avi",
                path = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.avi",
                duration = 180 * 1000L,
                size = 48 * 1024 * 1024L,
                folder = "Vids",
                mimeType = "video/x-msvideo",
                thumbnailUri = "https://images.unsplash.com/photo-1542204172-e70528091b50?w=500&auto=format&fit=crop",
                isNew = false
            ),
            // --- Audio Samples ---
            MediaItem(
                title = "Synthwave Sunset Beat.mp3",
                path = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3",
                duration = 372 * 1000L,
                size = 8 * 1024 * 1024L,
                folder = "My Audios",
                mimeType = "audio/mpeg",
                thumbnailUri = "https://images.unsplash.com/photo-1614613535308-eb5fbd3d2c17?w=500&auto=format&fit=crop",
                isNew = true
            ),
            MediaItem(
                title = "Acoustic Breeze Lounge.aac",
                path = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3",
                duration = 425 * 1000L,
                size = 9 * 1024 * 1024L,
                folder = "My Audios",
                mimeType = "audio/aac",
                thumbnailUri = "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=500&auto=format&fit=crop",
                isNew = true
            ),
            MediaItem(
                title = "Chill Lofi Coffee Beats.m4a",
                path = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-3.mp3",
                duration = 312 * 1000L,
                size = 7 * 1024 * 1024L,
                folder = "Downloads",
                mimeType = "audio/mp4",
                thumbnailUri = "https://images.unsplash.com/photo-1487180142328-054b783fc471?w=500&auto=format&fit=crop",
                isNew = true
            ),
            MediaItem(
                title = "Dramatic Orchestral Entrance.wav",
                path = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-4.mp3",
                duration = 295 * 1000L,
                size = 32 * 1024 * 1024L,
                folder = "Downloads",
                mimeType = "audio/wav",
                thumbnailUri = "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=500&auto=format&fit=crop",
                isNew = false
            )
        )
        val mappedSamples = samples.map {
            if (it.folder == "Mr. Flix") {
                it.copy(folder = "Mr. Will Flex")
            } else {
                it
            }
        }
        mediaDao.insertMedia(mappedSamples)
    }

    suspend fun moveToSecure(item: MediaItem) = withContext(Dispatchers.IO) {
        val originalPath = item.path
        if (originalPath.startsWith("http")) {
            val updated = item.copy(
                isSecure = true,
                originalPath = originalPath,
                path = ".secure_vault/.${item.title}"
            )
            mediaDao.updateMedia(updated)
            return@withContext
        }

        try {
            val sourceFile = java.io.File(originalPath)
            if (sourceFile.exists()) {
                val vaultDir = java.io.File(context.filesDir, "secure_vault")
                if (!vaultDir.exists()) vaultDir.mkdirs()

                val secureFile = java.io.File(vaultDir, ".sec_${System.currentTimeMillis()}_${sourceFile.name}")
                
                // Use fast, secure 8KB header manipulation
                val isSuccess = VaultManager.secureHeader(sourceFile, secureFile)
                if (isSuccess) {
                    // Delete original file to hide from system galleries and media scanners
                    sourceFile.delete()

                    // Notify media scanner of deleted file
                    MediaScannerConnection.scanFile(context, arrayOf(originalPath), null, null)

                    val updated = item.copy(
                        isSecure = true,
                        originalPath = originalPath,
                        path = secureFile.absolutePath
                    )
                    mediaDao.updateMedia(updated)
                } else {
                    Log.e("MediaRepository", "Failed to encrypt file header for ${sourceFile.name}")
                }
            } else {
                val updated = item.copy(
                    isSecure = true,
                    originalPath = originalPath,
                    path = ".secure_vault/.${item.title}"
                )
                mediaDao.updateMedia(updated)
            }
        } catch (e: Exception) {
            Log.e("MediaRepository", "Error moving file to vault: ${e.message}", e)
        }
    }

    suspend fun restoreFromSecure(item: MediaItem) = withContext(Dispatchers.IO) {
        val originalPath = item.originalPath ?: return@withContext
        val securePath = item.path

        if (originalPath.startsWith("http")) {
            val updated = item.copy(
                isSecure = false,
                path = originalPath
            )
            mediaDao.updateMedia(updated)
            return@withContext
        }

        try {
            val secureFile = java.io.File(securePath)
            if (secureFile.exists()) {
                val destFile = java.io.File(originalPath)
                val parentDir = destFile.parentFile
                if (parentDir != null && !parentDir.exists()) {
                     parentDir.mkdirs()
                }

                // Restore file from corrupted header state back to healthy
                val isSuccess = VaultManager.restoreHeader(secureFile, destFile)
                if (isSuccess) {
                    // Delete secure file
                    secureFile.delete()

                    // Notify media scanner of newly added restored file
                    MediaScannerConnection.scanFile(context, arrayOf(destFile.absolutePath), null, null)

                    val updated = item.copy(
                        isSecure = false,
                        path = originalPath
                    )
                    mediaDao.updateMedia(updated)
                } else {
                    Log.e("MediaRepository", "Failed to restore file header for ${secureFile.name}")
                }
            } else {
                val updated = item.copy(
                    isSecure = false,
                    path = originalPath
                )
                mediaDao.updateMedia(updated)
            }
        } catch (e: Exception) {
            Log.e("MediaRepository", "Error restoring file from vault: ${e.message}", e)
        }
    }
}
