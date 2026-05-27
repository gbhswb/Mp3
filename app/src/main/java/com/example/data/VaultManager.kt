package com.example.data

import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * VaultManager implements high-performance "Header Manipulation" technique.
 * Only the first 8KB of the media file is XOR-encrypted/decrypted, turning it
 * into a corrupted data stream for system gallery apps and scanners, while
 * preserving ultra-fast operation speeds (virtually instantaneous for large videos).
 */
object VaultManager {
    private const val TAG = "VaultManager"
    private const val HEADER_SIZE = 8192 // 8KB
    private const val XOR_KEY: Byte = 0x7E.toByte() // Secure XOR Mask

    /**
     * Obfuscates the first 8KB of the source file and writes the corrupted output to the destination.
     * The original file should then be safely deleted.
     */
    suspend fun secureHeader(source: File, destination: File): Boolean = withContext(Dispatchers.IO) {
        try {
            if (!source.exists()) {
                Log.e(TAG, "Source file does not exist: ${source.absolutePath}")
                return@withContext false
            }

            // Ensure destination folder exists
            destination.parentFile?.let { parent ->
                if (!parent.exists()) {
                    parent.mkdirs()
                }
            }

            FileInputStream(source).use { fis ->
                FileOutputStream(destination).use { fos ->
                    val buffer = ByteArray(64 * 1024) // 64KB buffer for high speed
                    var bytesRead: Int
                    var isFirstChunk = true

                    while (fis.read(buffer).also { bytesRead = it } != -1) {
                        if (isFirstChunk && bytesRead > 0) {
                            isFirstChunk = false
                            // Corrupt only the first 8KB header using symmetric XOR mask
                            val limit = bytesRead.coerceAtMost(HEADER_SIZE)
                            for (i in 0 until limit) {
                                buffer[i] = (buffer[i].toInt() xor XOR_KEY.toInt()).toByte()
                            }
                        }
                        fos.write(buffer, 0, bytesRead)
                    }
                }
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error securing header of ${source.name}: ${e.message}", e)
            false
        }
    }

    /**
     * Decrypts/restores the header of the source (secure) file and writes the healthy media file to destination.
     */
    suspend fun restoreHeader(source: File, destination: File): Boolean = withContext(Dispatchers.IO) {
        // XOR is symmetric, so encryption and decryption algorithms are identical
        secureHeader(source, destination)
    }
}
