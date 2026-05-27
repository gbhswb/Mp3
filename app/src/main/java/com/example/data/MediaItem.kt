package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "media_items")
data class MediaItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val path: String,
    val duration: Long,
    val size: Long,
    val folder: String,
    val mimeType: String?,
    val thumbnailUri: String? = null,
    val lastPlaybackPosition: Long = 0,
    val isFavorite: Boolean = false,
    val dateAdded: Long = System.currentTimeMillis(),
    val isSecure: Boolean = false,
    val originalPath: String? = null,
    val isNew: Boolean = true,
    val seriesName: String? = null,
    val episodeNumber: Int? = null,
    val seasonNumber: Int? = null
)

