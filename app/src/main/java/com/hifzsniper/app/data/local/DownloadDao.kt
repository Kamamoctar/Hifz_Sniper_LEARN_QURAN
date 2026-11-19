package com.hifzsniper.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadDao {
    @Query("SELECT * FROM download_item ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<DownloadItemEntity>>

    @Query("SELECT * FROM download_item WHERE id = :id")
    suspend fun get(id: String): DownloadItemEntity?

    @Query("SELECT * FROM download_item WHERE resourceKey = :resourceKey LIMIT 1")
    fun observeByResourceKey(resourceKey: String): Flow<DownloadItemEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: DownloadItemEntity)

    @Update
    suspend fun update(item: DownloadItemEntity)
}
