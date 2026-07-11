package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_settings")
data class AppSetting(
    @PrimaryKey val key: String,
    val value: String
)

@Entity(tableName = "synced_photos")
data class SyncedPhoto(
    @PrimaryKey val id: String,
    val filename: String,
    val timestamp: Long,
    val size: Long,
    val remoteUrl: String,
    val localUri: String? = null,
    val description: String? = null,
    val isDownloaded: Boolean = false
)
