package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.GalleryViewModel
import com.example.ui.ScreenState
import com.example.ui.UnlockState
import com.example.ui.screens.GalleryScreen
import com.example.ui.screens.PCConnectionScreen
import com.example.ui.screens.PasscodeScreen
import com.example.ui.screens.PhotoDetailScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val viewModel: GalleryViewModel = viewModel()
                
                val unlockState by viewModel.unlockState.collectAsStateWithLifecycle()
                val pinBuffer by viewModel.pinBuffer.collectAsStateWithLifecycle()
                val setupFirstPin by viewModel.setupFirstPin.collectAsStateWithLifecycle()
                val securityError by viewModel.securityError.collectAsStateWithLifecycle()
                
                val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
                val hostIp by viewModel.hostIp.collectAsStateWithLifecycle()
                val hostPort by viewModel.hostPort.collectAsStateWithLifecycle()
                val isConnecting by viewModel.isConnecting.collectAsStateWithLifecycle()
                val isConnected by viewModel.isConnected.collectAsStateWithLifecycle()
                val connectionMessage by viewModel.connectionMessage.collectAsStateWithLifecycle()
                
                val photos by viewModel.syncedPhotos.collectAsStateWithLifecycle()
                val selectedPhoto by viewModel.selectedPhoto.collectAsStateWithLifecycle()
                val downloadProgress by viewModel.downloadProgress.collectAsStateWithLifecycle()
                val photoErrorMessage by viewModel.photoErrorMessage.collectAsStateWithLifecycle()

                // Hardware System Back Handler
                BackHandler(enabled = unlockState is UnlockState.Unlocked && currentScreen !is ScreenState.Gallery) {
                    viewModel.navigateTo(ScreenState.Gallery)
                }

                Surface(modifier = Modifier.fillMaxSize()) {
                    Crossfade(targetState = unlockState, label = "UnlockCrossfade") { state ->
                        when (state) {
                            UnlockState.Checking -> {
                                // Quiet background during load check
                            }
                            UnlockState.NeedsSetup, UnlockState.Locked -> {
                                PasscodeScreen(
                                    unlockState = state,
                                    pinBuffer = pinBuffer,
                                    setupFirstPin = setupFirstPin,
                                    securityError = securityError,
                                    onPinDigit = viewModel::onPinDigit,
                                    onPinDelete = viewModel::onPinDelete
                                )
                            }
                            UnlockState.Unlocked -> {
                                Crossfade(targetState = when (currentScreen) {
                                    ScreenState.Gallery -> "gallery"
                                    ScreenState.PCConnection -> "pc_connection"
                                    is ScreenState.PhotoDetail -> "photo_detail"
                                }, label = "ScreenCrossfade") { dest ->
                                    when (dest) {
                                        "gallery" -> {
                                            GalleryScreen(
                                                photos = photos,
                                                isConnected = isConnected,
                                                isSyncing = isConnecting,
                                                connectionMessage = connectionMessage,
                                                downloadProgress = downloadProgress,
                                                getThumbnailUrl = viewModel::getThumbnailUrl,
                                                onSyncClick = viewModel::syncPhotos,
                                                onConfigureClick = { viewModel.navigateTo(ScreenState.PCConnection) },
                                                onLockClick = viewModel::lockApp,
                                                onPhotoClick = { id -> viewModel.navigateTo(ScreenState.PhotoDetail(id)) },
                                                onDownloadClick = viewModel::downloadPhoto,
                                                onDismissMessage = viewModel::dismissConnectionMessage
                                            )
                                        }
                                        "pc_connection" -> {
                                            PCConnectionScreen(
                                                hostIp = hostIp,
                                                hostPort = hostPort,
                                                isConnecting = isConnecting,
                                                isConnected = isConnected,
                                                connectionMessage = connectionMessage,
                                                onIpChange = viewModel::updateHostIp,
                                                onPortChange = viewModel::updateHostPort,
                                                onConnectClick = viewModel::connectToHostPC,
                                                onDisconnectClick = viewModel::disconnectFromHost,
                                                onBackClick = { viewModel.navigateTo(ScreenState.Gallery) },
                                                onDismissMessage = viewModel::dismissConnectionMessage
                                            )
                                        }
                                        "photo_detail" -> {
                                            PhotoDetailScreen(
                                                photos = photos,
                                                selectedPhoto = selectedPhoto,
                                                downloadProgress = downloadProgress,
                                                photoErrorMessage = photoErrorMessage,
                                                onBackClick = { viewModel.navigateTo(ScreenState.Gallery) },
                                                onDownloadClick = { photo -> viewModel.downloadPhoto(photo) },
                                                onDeleteLocalClick = { photo -> viewModel.deleteLocalDownload(photo) },
                                                onDismissError = viewModel::dismissPhotoError,
                                                onPhotoSelected = { photo -> viewModel.navigateTo(ScreenState.PhotoDetail(photo.id)) },
                                                getThumbnailUrl = viewModel::getThumbnailUrl
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
