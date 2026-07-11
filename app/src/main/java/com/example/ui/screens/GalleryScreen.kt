package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import com.example.data.SyncedPhoto

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryScreen(
    photos: List<SyncedPhoto>,
    isConnected: Boolean,
    isSyncing: Boolean,
    connectionMessage: String?,
    downloadProgress: Map<String, Float>,
    getThumbnailUrl: (String) -> String,
    onSyncClick: () -> Unit,
    onConfigureClick: () -> Unit,
    onLockClick: () -> Unit,
    onPhotoClick: (String) -> Unit,
    onDownloadClick: (SyncedPhoto) -> Unit,
    onDismissMessage: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Secure Lock Icon
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = "Secured",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = "WinUI Sync Vault",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                },
                actions = {
                    // Manual sync trigger
                    IconButton(
                        onClick = onSyncClick,
                        enabled = !isSyncing,
                        modifier = Modifier.testTag("sync_action_trigger")
                    ) {
                        if (isSyncing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            Icon(imageVector = Icons.Default.Sync, contentDescription = "Sync PC Photos", tint = MaterialTheme.colorScheme.onBackground)
                        }
                    }

                    // Configuration page link
                    IconButton(
                        onClick = onConfigureClick,
                        modifier = Modifier.testTag("settings_destination_button")
                    ) {
                        Icon(imageVector = Icons.Default.Settings, contentDescription = "Network Config", tint = MaterialTheme.colorScheme.onBackground)
                    }

                    // Strict Locker Action
                    IconButton(
                        onClick = onLockClick,
                        modifier = Modifier.testTag("manual_lock_button")
                    ) {
                        Icon(imageVector = Icons.Default.Lock, contentDescription = "Lock Applet Immediately", tint = MaterialTheme.colorScheme.primary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF0D0E15),
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    actionIconContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        containerColor = Color(0xFF0A0C10)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Quiet Banner Status Area
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF12161F))
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (isConnected) Color(0xFF2ECC71) else Color(0xFFE74C3C))
                    )
                    Text(
                        text = if (isConnected) "PC Connected" else "PC Disconnected (Offline View)",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = if (isConnected) Color(0xFF81C784) else Color(0xFFEF5350),
                            fontWeight = FontWeight.Bold
                        )
                    )
                }

                Text(
                    text = "${photos.size} Synced items",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                    )
                )
            }

            // Sync Feedback popups
            AnimatedVisibility(
                visible = !connectionMessage.isNullOrEmpty(),
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Sync Alert",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = connectionMessage ?: "",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onBackground,
                            fontWeight = FontWeight.Medium
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onDismissMessage, modifier = Modifier.size(24.dp)) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close Banner",
                            tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            if (photos.isEmpty()) {
                // Beautiful Minimal Empty State
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PhotoLibrary,
                        contentDescription = "Empty Sync Vault",
                        tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.15f),
                        modifier = Modifier.size(96.dp)
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        text = "Sync Vault holds no items",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        ),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Connect to your custom WinUI 3 Photos desktop app over Wi-Fi, then sync to download your shots securely of physical PC library.",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                        ),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = onConfigureClick,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("setup_shortcut_button")
                    ) {
                        Icon(imageVector = Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Connect PC Now")
                    }
                }
            } else {
                // Adaptive Staggered Grid for gallery aesthetics
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 110.dp),
                    contentPadding = PaddingValues(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("photos_grid_parent")
                ) {
                    items(photos, key = { it.id }) { photo ->
                        PhotoGridTile(
                            photo = photo,
                            isDownloading = downloadProgress.containsKey(photo.id),
                            downloadPercent = downloadProgress[photo.id] ?: 0f,
                            getThumbnailUrl = getThumbnailUrl,
                            onPhotoClick = { onPhotoClick(photo.id) },
                            onDownloadClick = { onDownloadClick(photo) }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PhotoGridTile(
    photo: SyncedPhoto,
    isDownloading: Boolean,
    downloadPercent: Float,
    getThumbnailUrl: (String) -> String,
    onPhotoClick: () -> Unit,
    onDownloadClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF161A24)),
        modifier = Modifier
            .aspectRatio(1f)
            .border(
                width = 1.dp,
                color = if (photo.isDownloaded) MaterialTheme.colorScheme.tertiary.copy(alpha = 0.3f) else Color(0xFF252D3F),
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onPhotoClick)
            .testTag("photo_item_${photo.id}")
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            val thumbnailSource = if (photo.isDownloaded && photo.localUri != null) {
                photo.localUri
            } else {
                getThumbnailUrl(photo.id)
            }

            // Image loader
            SubcomposeAsyncImage(
                model = thumbnailSource,
                contentDescription = photo.filename,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                loading = {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF0F1219)),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                        )
                    }
                },
                error = {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF2D1414)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.BrokenImage,
                            contentDescription = "Sync Error",
                            tint = Color(0xFFEF5350).copy(alpha = 0.7f),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            )

            // Sync Status Badge Indicator
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(6.dp),
                contentAlignment = Alignment.TopEnd
            ) {
                if (photo.isDownloaded) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .background(Color(0xFF2ECC71), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.OfflinePin,
                            contentDescription = "Offline Available",
                            tint = Color.White,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                } else if (isDownloading) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .background(MaterialTheme.colorScheme.secondary, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            progress = { downloadPercent },
                            modifier = Modifier.size(14.dp),
                            strokeWidth = 1.8.dp,
                            color = Color.White,
                            trackColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f),
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                            .clickable { onDownloadClick() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudDownload,
                            contentDescription = "Remote Only - Tap to Sync Locally",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }
            }

            // Filename label overlay at the bottom for scanning ease
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(Color.Black.copy(alpha = 0.5f))
                    .padding(horizontal = 6.dp, vertical = 4.dp)
            ) {
                Text(
                    text = photo.filename,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Medium
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
