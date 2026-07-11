package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.GalleryRepository
import com.example.data.SyncedPhoto
import com.example.network.WinUiSyncService
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File

sealed interface UnlockState {
    object Checking : UnlockState
    object NeedsSetup : UnlockState
    object Locked : UnlockState
    object Unlocked : UnlockState
}

sealed interface ScreenState {
    object Gallery : ScreenState
    data class PhotoDetail(val photoId: String) : ScreenState
    object PCConnection : ScreenState
}

class GalleryViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val repository = GalleryRepository(db.appSettingDao(), db.syncedPhotoDao())
    private val syncService = WinUiSyncService(application)

    // Unlock & Security state
    private val _unlockState = MutableStateFlow<UnlockState>(UnlockState.Checking)
    val unlockState: StateFlow<UnlockState> = _unlockState.asStateFlow()

    private val _pinBuffer = MutableStateFlow("")
    val pinBuffer: StateFlow<String> = _pinBuffer.asStateFlow()

    private val _setupFirstPin = MutableStateFlow<String?>(null)
    val setupFirstPin: StateFlow<String?> = _setupFirstPin.asStateFlow()

    private val _securityError = MutableStateFlow<String?>(null)
    val securityError: StateFlow<String?> = _securityError.asStateFlow()

    // Screen navigation state
    private val _currentScreen = MutableStateFlow<ScreenState>(ScreenState.Gallery)
    val currentScreen: StateFlow<ScreenState> = _currentScreen.asStateFlow()

    // Host app connectivity state
    private val _hostIp = MutableStateFlow("")
    val hostIp: StateFlow<String> = _hostIp.asStateFlow()

    private val _hostPort = MutableStateFlow("5000") // Default port is common
    val hostPort: StateFlow<String> = _hostPort.asStateFlow()

    private val _isConnecting = MutableStateFlow(false)
    val isConnecting: StateFlow<Boolean> = _isConnecting.asStateFlow()

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _connectionMessage = MutableStateFlow<String?>(null)
    val connectionMessage: StateFlow<String?> = _connectionMessage.asStateFlow()

    // Photos data
    val syncedPhotos: StateFlow<List<SyncedPhoto>> = repository.allPhotos
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Thumbnail builder helper
    fun getThumbnailUrl(photoId: String): String {
        return syncService.getThumbnailUrl(_hostIp.value, _hostPort.value, photoId)
    }

    // Detail photo selection
    private val _selectedPhoto = MutableStateFlow<SyncedPhoto?>(null)
    val selectedPhoto: StateFlow<SyncedPhoto?> = _selectedPhoto.asStateFlow()

    // Photo download progresses (photoId -> Float (0..1))
    private val _downloadProgress = MutableStateFlow<Map<String, Float>>(emptyMap())
    val downloadProgress: StateFlow<Map<String, Float>> = _downloadProgress.asStateFlow()

    // Image loading errors specifically
    private val _photoErrorMessage = MutableStateFlow<String?>(null)
    val photoErrorMessage: StateFlow<String?> = _photoErrorMessage.asStateFlow()

    init {
        checkSecuritySetup()
        loadConnectionDetails()
    }

    // Checking if a passcode has already been configured
    private fun checkSecuritySetup() {
        viewModelScope.launch {
            if (repository.isPasscodeSet()) {
                _unlockState.value = UnlockState.Locked
            } else {
                _unlockState.value = UnlockState.NeedsSetup
            }
        }
    }

    // Loading prior IP and Port connection configuration from DB
    private fun loadConnectionDetails() {
        viewModelScope.launch {
            val ip = repository.getSettingDirect("host_ip") ?: ""
            val port = repository.getSettingDirect("host_port") ?: "5000"
            _hostIp.value = ip
            _hostPort.value = port

            if (ip.isNotEmpty() && port.isNotEmpty()) {
                // Background quiet connection test
                val success = syncService.testConnection(ip, port)
                _isConnected.value = success
            }
        }
    }

    // Handle pin button input (digits 0-9, delete, clear)
    fun onPinDigit(digit: String) {
        _securityError.value = null
        val current = _pinBuffer.value
        if (current.length < 4) {
            val updated = current + digit
            _pinBuffer.value = updated
            
            // Check auto-completion triggers
            if (updated.length == 4) {
                handlePinCompletion(updated)
            }
        }
    }

    fun onPinDelete() {
        _securityError.value = null
        val current = _pinBuffer.value
        if (current.isNotEmpty()) {
            _pinBuffer.value = current.dropLast(1)
        }
    }

    private fun handlePinCompletion(pin: String) {
        viewModelScope.launch {
            when (val state = _unlockState.value) {
                is UnlockState.NeedsSetup -> {
                    val first = _setupFirstPin.value
                    if (first == null) {
                        // First pin entered, cache it and prompt to retest
                        _setupFirstPin.value = pin
                        _pinBuffer.value = ""
                    } else {
                        // Confirming pin
                        if (pin == first) {
                            val success = repository.setupPasscode(pin)
                            if (success) {
                                _unlockState.value = UnlockState.Unlocked
                                _pinBuffer.value = ""
                                _setupFirstPin.value = null
                            } else {
                                _securityError.value = "Failed to secure passcode."
                                _pinBuffer.value = ""
                                _setupFirstPin.value = null
                            }
                        } else {
                            _securityError.value = "Passcodes do not match. Restarting."
                            _setupFirstPin.value = null
                            _pinBuffer.value = ""
                        }
                    }
                }
                is UnlockState.Locked -> {
                    val valid = repository.verifyPasscode(pin)
                    if (valid) {
                        _unlockState.value = UnlockState.Unlocked
                        _pinBuffer.value = ""
                    } else {
                        _securityError.value = "Invalid security passcode."
                        _pinBuffer.value = ""
                    }
                }
                else -> {}
            }
        }
    }

    // Lock the application again
    fun lockApp() {
        _unlockState.value = UnlockState.Locked
        _pinBuffer.value = ""
        _setupFirstPin.value = null
    }

    // Connection configuration updates
    fun updateHostIp(ip: String) {
        _hostIp.value = ip
        _connectionMessage.value = null
    }

    fun updateHostPort(port: String) {
        _hostPort.value = port
        _connectionMessage.value = null
    }

    // Save configuration and establish connection to WinUI 3 Host app
    fun connectToHostPC() {
        val ip = _hostIp.value.trim()
        val port = _hostPort.value.trim()
        
        if (ip.isEmpty()) {
            _connectionMessage.value = "Please enter server IP address."
            return
        }

        viewModelScope.launch {
            _isConnecting.value = true
            _connectionMessage.value = null
            
            val connected = syncService.testConnection(ip, port)
            _isConnected.value = connected
            _isConnecting.value = false

            if (connected) {
                repository.saveSetting("host_ip", ip)
                repository.saveSetting("host_port", port)
                _connectionMessage.value = "Connected successfully to Windows Photos app!"
                
                // Trigger auto sync of metadata immediately
                syncPhotos()
            } else {
                _connectionMessage.value = "Cannot connect. Verify host app is running on the PC and connected to the same network."
            }
        }
    }

    // Wipe configuration and connectivity
    fun disconnectFromHost() {
        viewModelScope.launch {
            _isConnected.value = false
            _connectionMessage.value = "Disconnected from server."
            repository.deleteSetting("host_ip")
            repository.deleteSetting("host_port")
            repository.clearAllSynced()
        }
    }

    // Sync metadata from connected PC
    fun syncPhotos() {
        val ip = _hostIp.value
        val port = _hostPort.value
        if (ip.isEmpty()) return

        viewModelScope.launch {
            _isConnecting.value = true
            val remotePhotos = syncService.fetchPhotosFromPC(ip, port)
            
            if (remotePhotos.isNotEmpty()) {
                // Incorporate with existing local details to avoid wiping localUri of already downloaded photos
                val currentLocalList = syncedPhotos.value.associateBy { it.id }
                val mergedList = remotePhotos.map { remotePhoto ->
                    val existing = currentLocalList[remotePhoto.id]
                    if (existing?.isDownloaded == true) {
                        remotePhoto.copy(
                            localUri = existing.localUri,
                            isDownloaded = true
                        )
                    } else {
                        remotePhoto
                    }
                }
                repository.insertOrUpdatePhotos(mergedList)
                _connectionMessage.value = "Syne completed! ${_currentScreen.value} synced."
            } else if (!_isConnected.value) {
                _connectionMessage.value = "Not connected. Re-establishing connection..."
                val connected = syncService.testConnection(ip, port)
                _isConnected.value = connected
                if (connected) {
                    val retriedPhotos = syncService.fetchPhotosFromPC(ip, port)
                    repository.insertOrUpdatePhotos(retriedPhotos)
                } else {
                    _connectionMessage.value = "Sync failed. PC Host app is unreachable."
                }
            } else {
                _connectionMessage.value = "Sync completed. No new photos or PC is empty."
            }
            _isConnecting.value = false
        }
    }

    // Download a specific photo to private storage
    fun downloadPhoto(photo: SyncedPhoto) {
        val ip = _hostIp.value
        val port = _hostPort.value
        if (ip.isEmpty() || photo.isDownloaded) return

        viewModelScope.launch {
            _downloadProgress.value = _downloadProgress.value + (photo.id to 0.01f)
            
            val file = syncService.downloadPhoto(
                ip = ip,
                port = port,
                id = photo.id,
                filename = photo.filename
            ) { progress ->
                _downloadProgress.value = _downloadProgress.value + (photo.id to progress)
            }

            if (file != null && file.exists()) {
                val absoluteUri = file.absolutePath
                repository.updatePhotoLocale(photo.id, absoluteUri)
                
                // If this is the active detailed photo, update details view state
                if (_selectedPhoto.value?.id == photo.id) {
                    val updated = repository.getPhotoById(photo.id)
                    _selectedPhoto.value = updated
                }
            } else {
                _photoErrorMessage.value = "Could not download ${photo.filename}."
            }
            _downloadProgress.value = _downloadProgress.value - photo.id
        }
    }

    // Delete a local file download and revert status back to online-only
    fun deleteLocalDownload(photo: SyncedPhoto) {
        viewModelScope.launch {
            photo.localUri?.let { path ->
                val file = File(path)
                if (file.exists()) {
                    file.delete()
                }
            }
            repository.updatePhotoLocale(photo.id, null)
            if (_selectedPhoto.value?.id == photo.id) {
                val updated = repository.getPhotoById(photo.id)
                _selectedPhoto.value = updated
            }
        }
    }

    // Screen state switches
    fun navigateTo(screen: ScreenState) {
        _currentScreen.value = screen
        _photoErrorMessage.value = null
        _connectionMessage.value = null
        if (screen is ScreenState.PhotoDetail) {
            viewModelScope.launch {
                _selectedPhoto.value = repository.getPhotoById(screen.photoId)
            }
        } else {
            _selectedPhoto.value = null
        }
    }

    fun dismissPhotoError() {
        _photoErrorMessage.value = null
    }

    fun dismissConnectionMessage() {
        _connectionMessage.value = null
    }
}
