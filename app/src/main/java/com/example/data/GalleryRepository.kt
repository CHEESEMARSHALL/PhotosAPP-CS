package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.security.MessageDigest

class GalleryRepository(
    private val appSettingDao: AppSettingDao,
    private val syncedPhotoDao: SyncedPhotoDao
) {
    val allPhotos: Flow<List<SyncedPhoto>> = syncedPhotoDao.getAllPhotos()

    // Configuration Flows
    fun getSetting(key: String): Flow<String?> = 
        appSettingDao.getSetting(key).map { it?.value }

    suspend fun getSettingDirect(key: String): String? =
        appSettingDao.getSettingDirect(key)?.value

    suspend fun saveSetting(key: String, value: String) {
        appSettingDao.insertSetting(AppSetting(key, value))
    }

    suspend fun deleteSetting(key: String) {
        appSettingDao.deleteSetting(key)
    }

    // Passcode Verification & Security
    suspend fun isPasscodeSet(): Boolean {
        val hash = getSettingDirect("passcode_hash")
        return !hash.isNullOrEmpty()
    }

    suspend fun setupPasscode(passcode: String): Boolean {
        if (passcode.length < 4) return false
        val hashed = hashString(passcode)
        saveSetting("passcode_hash", hashed)
        saveSetting("is_setup_complete", "true")
        return true
    }

    suspend fun verifyPasscode(passcode: String): Boolean {
        val savedHash = getSettingDirect("passcode_hash") ?: return false
        return hashString(passcode) == savedHash
    }

    // Photo Synchronization
    suspend fun insertOrUpdatePhotos(photos: List<SyncedPhoto>) {
        syncedPhotoDao.insertPhotos(photos)
    }

    suspend fun updatePhotoLocale(id: String, localUri: String?) {
        val photo = syncedPhotoDao.getPhotoById(id)
        if (photo != null) {
            syncedPhotoDao.updatePhoto(photo.copy(localUri = localUri, isDownloaded = localUri != null))
        }
    }

    suspend fun getPhotoById(id: String): SyncedPhoto? {
        return syncedPhotoDao.getPhotoById(id)
    }

    suspend fun clearAllSynced() {
        syncedPhotoDao.clearPhotos()
    }

    private fun hashString(input: String): String {
        val bytes = input.toByteArray()
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(bytes)
        return digest.fold("") { str, it -> str + "%02x".format(it) }
    }
}
