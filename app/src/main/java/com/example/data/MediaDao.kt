package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface MediaDao {
    @Query("SELECT * FROM media_items ORDER BY dateAdded DESC")
    fun getAllMedia(): Flow<List<MediaItem>>

    @Query("SELECT * FROM media_items")
    suspend fun getAllMediaList(): List<MediaItem>

    @Query("SELECT * FROM media_items WHERE folder = :folderName ORDER BY title ASC")
    fun getMediaInFolder(folderName: String): Flow<List<MediaItem>>

    @Query("SELECT DISTINCT folder FROM media_items ORDER BY folder ASC")
    fun getFolders(): Flow<List<String>>

    @Query("SELECT * FROM media_items WHERE isFavorite = 1 ORDER BY dateAdded DESC")
    fun getFavorites(): Flow<List<MediaItem>>

    @Query("SELECT * FROM media_items WHERE title LIKE '%' || :query || '%' OR folder LIKE '%' || :query || '%'")
    fun searchMedia(query: String): Flow<List<MediaItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMedia(media: List<MediaItem>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSingleMedia(mediaItem: MediaItem): Long

    @Update
    suspend fun updateMedia(mediaItem: MediaItem)

    @Query("UPDATE media_items SET lastPlaybackPosition = :position WHERE id = :id")
    suspend fun updatePlaybackPosition(id: Long, position: Long)

    @Query("UPDATE media_items SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun updateFavorite(id: Long, isFavorite: Boolean)

    @Query("DELETE FROM media_items")
    suspend fun clearAll()

    @Delete
    suspend fun delete(mediaItem: MediaItem)
}
