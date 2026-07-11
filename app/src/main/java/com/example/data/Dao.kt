package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AppSettingDao {
    @Query("SELECT * FROM app_settings WHERE key = :key")
    fun getSetting(key: String): Flow<AppSetting?>

    @Query("SELECT * FROM app_settings WHERE key = :key")
    suspend fun getSettingDirect(key: String): AppSetting?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSetting(setting: AppSetting)

    @Query("DELETE FROM app_settings WHERE key = :key")
    suspend fun deleteSetting(key: String)
}

@Dao
interface SyncedPhotoDao {
    @Query("SELECT * FROM synced_photos ORDER BY timestamp DESC")
    fun getAllPhotos(): Flow<List<SyncedPhoto>>

    @Query("SELECT * FROM synced_photos WHERE id = :id")
    suspend fun getPhotoById(id: String): SyncedPhoto?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPhotos(photos: List<SyncedPhoto>)

    @Update
    suspend fun updatePhoto(photo: SyncedPhoto)

    @Query("DELETE FROM synced_photos WHERE id = :id")
    suspend fun deletePhotoById(id: String)

    @Query("DELETE FROM synced_photos")
    suspend fun clearPhotos()
}
