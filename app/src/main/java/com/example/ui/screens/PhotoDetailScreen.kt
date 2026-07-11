package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.SubcomposeAsyncImage
import com.example.data.SyncedPhoto
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

// Exoplayer imports
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.PlaybackParameters
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

@kotlin.OptIn(ExperimentalAnimationApi::class, ExperimentalMaterial3Api::class, androidx.media3.common.util.UnstableApi::class)
@Composable
fun PhotoDetailScreen(
    photos: List<SyncedPhoto>,
    selectedPhoto: SyncedPhoto?,
    downloadProgress: Map<String, Float>,
    photoErrorMessage: String?,
    onBackClick: () -> Unit,
    onDownloadClick: (SyncedPhoto) -> Unit,
    onDeleteLocalClick: (SyncedPhoto) -> Unit,
    onDismissError: () -> Unit,
    onPhotoSelected: (SyncedPhoto) -> Unit,
    getThumbnailUrl: (String) -> String
) {
    val scope = rememberCoroutineScope()
    
    // Find initial index of currently selected photo
    val initialIndex = remember(photos) {
        val idx = photos.indexOfFirst { it.id == selectedPhoto?.id }
        if (idx >= 0) idx else 0
    }
    
    val pagerState = rememberPagerState(initialPage = initialIndex, pageCount = { photos.size })
    val lazyListState = rememberLazyListState()
    
    // UI visibility controls (tapping toggles layout visibility)
    var isUiVisible by remember { mutableStateOf(true) }

    // Bottom sheet details expanded state
    var isExpanded by remember { mutableStateOf(false) }

    // Bidirectional sync: Pager change -> Selected Photo state in ViewModel
    LaunchedEffect(pagerState.currentPage) {
        if (photos.isNotEmpty() && pagerState.currentPage in photos.indices) {
            val pagePhoto = photos[pagerState.currentPage]
            if (pagePhoto.id != selectedPhoto?.id) {
                onPhotoSelected(pagePhoto)
            }
        }
    }

    // Bidirectional sync: External Selected Photo change -> Pager change
    LaunchedEffect(selectedPhoto) {
        if (selectedPhoto != null && photos.isNotEmpty()) {
            val targetIndex = photos.indexOfFirst { it.id == selectedPhoto.id }
            if (targetIndex >= 0 && targetIndex != pagerState.currentPage) {
                pagerState.scrollToPage(targetIndex)
            }
        }
    }

    // Center current thumbnail in the filmstrip
    LaunchedEffect(pagerState.currentPage) {
        if (photos.isNotEmpty() && pagerState.currentPage in photos.indices) {
            lazyListState.animateScrollToItem(pagerState.currentPage)
        }
    }

    // Main immersive overlay wrapper
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (photos.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else {
            // 1. Fullscreen Horizontal Pager with Pinch-to-Zoom & Pan and double tap zoom states
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectVerticalDragGestures(
                            onVerticalDrag = { change, dragAmount ->
                                change.consume()
                                if (dragAmount < -15) { // Swipe upwards triggers specs sheet
                                    isExpanded = true
                                } else if (dragAmount > 15) { // Swipe downwards hides specs sheet
                                    isExpanded = false
                                }
                            }
                        )
                    }
            ) { pageIndex ->
                if (pageIndex in photos.indices) {
                    val pagePhoto = photos[pageIndex]

                    // Gestures states local to each pager item
                    var scale by remember { mutableStateOf(1f) }
                    var offset by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }

                    // Reset zoom when screen scrolls or pager shifts
                    LaunchedEffect(pagerState.currentPage) {
                        if (pagerState.currentPage != pageIndex) {
                            scale = 1f
                            offset = androidx.compose.ui.geometry.Offset.Zero
                        }
                    }

                    val isVideo = remember(pagePhoto.filename) {
                        val ext = pagePhoto.filename.substringAfterLast('.', "").lowercase()
                        ext in setOf("mp4", "mkv", "3gp", "webm", "avi", "mov", "m4v")
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black)
                            .pointerInput(Unit) {
                                detectTransformGestures { _, pan, zoom, _ ->
                                    val newScale = (scale * zoom).coerceIn(1f, 5f)
                                    if (newScale > 1f) {
                                        scale = newScale
                                        offset = androidx.compose.ui.geometry.Offset(
                                            x = offset.x + pan.x * scale,
                                            y = offset.y + pan.y * scale
                                        )
                                    } else {
                                        scale = 1f
                                        offset = androidx.compose.ui.geometry.Offset.Zero
                                    }
                                }
                            }
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onDoubleTap = {
                                        if (scale > 1f) {
                                            scale = 1f
                                            offset = androidx.compose.ui.geometry.Offset.Zero
                                        } else {
                                            scale = 2.5f
                                        }
                                    },
                                    onTap = {
                                        isUiVisible = !isUiVisible
                                    }
                                )
                            }
                    ) {
                        // Scaled Image/Player Container
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer(
                                    scaleX = scale,
                                    scaleY = scale,
                                    translationX = offset.x,
                                    translationY = offset.y
                                )
                                .then(
                                    // Offset display area when UI overlays are active to avoid visual overlap
                                    if (isUiVisible && scale == 1f) Modifier.padding(bottom = 120.dp) else Modifier
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            val mediaSource = if (pagePhoto.isDownloaded && pagePhoto.localUri != null) {
                                pagePhoto.localUri
                            } else {
                                pagePhoto.remoteUrl
                            }

                            if (isVideo) {
                                VideoPlayer(
                                    videoUrl = mediaSource,
                                    isActive = pagerState.currentPage == pageIndex,
                                    onTap = { isUiVisible = !isUiVisible }
                                )
                            } else {
                                SubcomposeAsyncImage(
                                    model = mediaSource,
                                    contentDescription = pagePhoto.filename,
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier.fillMaxSize(),
                                    loading = {
                                        Box(
                                            modifier = Modifier.fillMaxSize(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                                        }
                                    },
                                    error = {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth(0.85f)
                                                .height(260.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(Color(0xFF281111))
                                                .border(1.dp, Color(0xFF5A1E1E), RoundedCornerShape(12.dp)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.BrokenImage,
                                                    contentDescription = "Load Error",
                                                    tint = Color(0xFFEF5350),
                                                    modifier = Modifier.size(48.dp)
                                                )
                                                Text(
                                                    text = "Cannot view PC source.\nVerify shared network.",
                                                    style = MaterialTheme.typography.bodySmall.copy(
                                                        color = Color(0xFFEF5350),
                                                        fontWeight = FontWeight.Medium,
                                                        textAlign = TextAlign.Center
                                                    )
                                                )
                                            }
                                        }
                                    }
                                )
                            }
                        }

                        // Floating Remote/Offline Indicator Tag
                        if (isUiVisible && scale == 1f) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .statusBarsPadding()
                                    .padding(top = 64.dp, end = 16.dp),
                                contentAlignment = Alignment.TopEnd
                            ) {
                                val stateLabel = if (pagePhoto.isDownloaded) {
                                    if (isVideo) "Offline Video" else "Offline Photo"
                                } else {
                                    if (isVideo) "Remote PC Video" else "Remote PC Photo"
                                }
                                val stateColor = if (pagePhoto.isDownloaded) Color(0xFF2ECC71).copy(alpha = 0.85f) else Color(0xFFFF9800).copy(alpha = 0.85f)
                                
                                Text(
                                    text = stateLabel,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Black,
                                        fontSize = 10.sp,
                                        color = Color.White
                                    ),
                                    modifier = Modifier
                                        .background(stateColor, RoundedCornerShape(4.dp))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }

            // 2. Translucent Floating Top App Bar Overlay
            AnimatedVisibility(
                visible = isUiVisible,
                enter = fadeIn() + slideInVertically(initialOffsetY = { -it }),
                exit = fadeOut() + slideOutVertically(targetOffsetY = { -it }),
                modifier = Modifier.align(Alignment.TopCenter)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xAA080B10))
                        .statusBarsPadding()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.testTag("detail_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = selectedPhoto?.filename ?: "Media Viewer",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // 3. Translucent Floating Bottom Filmstrip Overlay
            AnimatedVisibility(
                visible = isUiVisible && !isExpanded,
                enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
                exit = fadeOut() + slideOutVertically(targetOffsetY = { it }),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xCC080B10))
                        .navigationBarsPadding()
                        .padding(top = 10.dp, bottom = 12.dp)
                ) {
                    // Filename Label
                    selectedPhoto?.let { currentPhoto ->
                        Text(
                            text = currentPhoto.filename,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color.White.copy(alpha = 0.85f),
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 11.sp,
                                textAlign = TextAlign.Center
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Filmstrip Layout
                    LazyRow(
                        state = lazyListState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        itemsIndexed(photos, key = { _, p -> p.id }) { index, photo ->
                            val isCurrent = photo.id == selectedPhoto?.id
                            val thumbnailSource = if (photo.isDownloaded && photo.localUri != null) {
                                photo.localUri
                            } else {
                                getThumbnailUrl(photo.id)
                            }
                            
                            Box(
                                modifier = Modifier
                                    .width(44.dp)
                                    .height(58.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .border(
                                        width = if (isCurrent) 2.5.dp else 1.dp,
                                        color = if (isCurrent) MaterialTheme.colorScheme.primary else Color(0xFF252D3F),
                                        shape = RoundedCornerShape(6.dp)
                                    )
                                    .clickable {
                                        scope.launch {
                                            pagerState.animateScrollToPage(index)
                                        }
                                    }
                            ) {
                                SubcomposeAsyncImage(
                                    model = thumbnailSource,
                                    contentDescription = "Filmstrip preview",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize(),
                                    loading = {
                                        Box(
                                            modifier = Modifier.fillMaxSize().background(Color(0xFF161A24)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(10.dp),
                                                strokeWidth = 1.dp,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Handle/Trigger drag up
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isExpanded = true }
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowUp,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Tap or drag up for specifications",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                        )
                    }
                }
            }

            // 4. Sliding Specifications Drawer (Bottom Sheet)
            AnimatedVisibility(
                visible = isExpanded,
                enter = slideInVertically(
                    initialOffsetY = { it },
                    animationSpec = spring(dampingRatio = 0.85f, stiffness = 300f)
                ) + fadeIn(),
                exit = slideOutVertically(
                    targetOffsetY = { it },
                    animationSpec = spring(dampingRatio = 0.85f, stiffness = 300f)
                ) + fadeOut(),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                selectedPhoto?.let { photo ->
                    val isPhotoDownloading = downloadProgress.containsKey(photo.id)
                    val photoDownloadPercent = downloadProgress[photo.id] ?: 0f

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(0.68f)
                            .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                            .border(1.dp, Color(0xFF222B3F), RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)),
                        color = Color(0xFF0F111A)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .pointerInput(Unit) {
                                    detectVerticalDragGestures(
                                        onVerticalDrag = { change, dragAmount ->
                                            change.consume()
                                            if (dragAmount > 15) { // Drag down collapses specifications
                                                isExpanded = false
                                            }
                                        }
                                    )
                                }
                        ) {
                            // Top Drag Handle Panel
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { isExpanded = false }
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(width = 40.dp, height = 4.dp)
                                            .clip(RoundedCornerShape(2.dp))
                                            .background(Color(0xFF374151))
                                    )
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowDown,
                                        contentDescription = "Collapse panel",
                                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            // Info details scroll area
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState())
                                    .navigationBarsPadding()
                                    .padding(horizontal = 20.dp, vertical = 4.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Text(
                                    text = photo.filename,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    ),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )

                                // Photo Errors block
                                AnimatedVisibility(
                                    visible = !photoErrorMessage.isNullOrEmpty(),
                                    enter = fadeIn() + expandVertically(),
                                    exit = fadeOut() + shrinkVertically()
                                ) {
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = Color(0xFF2E1515)),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .border(1.dp, Color(0xFFEF5350).copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Icon(imageVector = Icons.Default.ErrorOutline, contentDescription = "Error icon", tint = Color(0xFFE57373))
                                            Text(
                                                text = photoErrorMessage ?: "",
                                                style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFFFCDD2)),
                                                modifier = Modifier.weight(1f)
                                            )
                                            IconButton(onClick = onDismissError, modifier = Modifier.size(24.dp)) {
                                                Icon(imageVector = Icons.Default.Close, contentDescription = "Close error", tint = Color(0xFFFFCDD2))
                                            }
                                        }
                                    }
                                }

                                // Actions (Sync / download controls)
                                Box(modifier = Modifier.fillMaxWidth()) {
                                    if (photo.isDownloaded) {
                                        OutlinedButton(
                                            onClick = { onDeleteLocalClick(photo) },
                                            border = BorderStroke(1.dp, Color(0xFFEF5350)),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(48.dp)
                                                .testTag("delete_local_cache_button"),
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF5350))
                                        ) {
                                            Icon(imageVector = Icons.Default.DeleteForever, contentDescription = "Delete local copy")
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Delete Local Cache (Free Space)", fontWeight = FontWeight.Bold)
                                        }
                                    } else {
                                        Button(
                                            onClick = { onDownloadClick(photo) },
                                            enabled = !isPhotoDownloading,
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(48.dp)
                                                .testTag("download_action_button"),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = MaterialTheme.colorScheme.primary,
                                                contentColor = MaterialTheme.colorScheme.onPrimary
                                            )
                                        ) {
                                            if (isPhotoDownloading) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                                ) {
                                                    CircularProgressIndicator(
                                                        progress = { photoDownloadPercent },
                                                        modifier = Modifier.size(20.dp),
                                                        strokeWidth = 2.dp,
                                                        color = MaterialTheme.colorScheme.onPrimary,
                                                        trackColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.3f)
                                                    )
                                                    Text(
                                                        text = "Synchronizing... ${(photoDownloadPercent * 100).toInt()}%",
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            } else {
                                                Icon(imageVector = Icons.Default.CloudDownload, contentDescription = "Download button icon")
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text("Download Offline Media Copy", fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }

                                // Specifications details card
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF131722)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(1.dp, Color(0xFF222B3F), RoundedCornerShape(12.dp)),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Text(
                                            text = "Media Specifications",
                                            style = MaterialTheme.typography.titleSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        )

                                        Divider(color = Color(0xFF222B3F), thickness = 1.dp)

                                        MetadataField(label = "Filename", value = photo.filename)
                                        MetadataField(label = "Resolution Scope", value = photo.description ?: "Default Aspect Ratio")
                                        MetadataField(label = "File Size", value = formatBytes(photo.size))
                                        MetadataField(label = "PC Creation Time", value = formatTimestamp(photo.timestamp))
                                        MetadataField(label = "Locker Protection", value = "AES Private Host Cache")
                                    }
                                }

                                // Encryption Safety alert card
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1C132E).copy(alpha = 0.5f)),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(1.dp, Color(0xFF5B3C88).copy(alpha = 0.3f), RoundedCornerShape(8.dp)),
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.Security, contentDescription = "Encrypted Security Icon", tint = Color(0xFF9E77E3))
                                        Text(
                                            text = "This file is stored securely inside the app's encrypted vault directory. It is isolated from standard galleries on this phone.",
                                            style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFD4C7F0), lineHeight = 16.sp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(36.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MetadataField(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
            )
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.End
            ),
            modifier = Modifier.weight(1f).padding(start = 16.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "0 Bytes"
    val units = arrayOf("Bytes", "KB", "MB", "GB")
    val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
    return String.format(Locale.US, "%.2f %s", bytes / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
}

private fun formatTimestamp(timestamp: Long): String {
    return try {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val netDate = Date(timestamp)
        sdf.format(netDate)
    } catch (e: Exception) {
        "Unknown"
    }
}

@kotlin.OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
fun VideoPlayer(
    videoUrl: String,
    isActive: Boolean, // Play only if currently active page in Pager
    onTap: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    
    // Playback control states
    var isPlaying by remember { mutableStateOf(true) }
    var isLooping by remember { mutableStateOf(true) }
    var playSpeed by remember { mutableStateOf(1.0f) }

    // ExoPlayer creation
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            repeatMode = Player.REPEAT_MODE_ALL
            playWhenReady = true
        }
    }

    // Media trigger on load
    LaunchedEffect(videoUrl) {
        val uri = if (videoUrl.startsWith("/")) {
            android.net.Uri.fromFile(java.io.File(videoUrl))
        } else {
            android.net.Uri.parse(videoUrl)
        }
        val mediaItem = MediaItem.fromUri(uri)
        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()
    }

    // Handle play/pause with pager focus swiping
    LaunchedEffect(isActive) {
        if (isActive) {
            exoPlayer.play()
            isPlaying = true
        } else {
            exoPlayer.pause()
            isPlaying = false
        }
    }

    // Sync speed state
    LaunchedEffect(playSpeed) {
        exoPlayer.playbackParameters = PlaybackParameters(playSpeed)
    }

    // Sync looping state
    LaunchedEffect(isLooping) {
        exoPlayer.repeatMode = if (isLooping) Player.REPEAT_MODE_ALL else Player.REPEAT_MODE_OFF
    }

    // Clean disposal
    DisposableEffect(videoUrl) {
        onDispose {
            exoPlayer.release()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    useController = false
                    player = exoPlayer
                    setBackgroundColor(android.graphics.Color.BLACK)
                    layoutParams = android.view.ViewGroup.LayoutParams(
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                    indication = null
                ) { onTap() }
        )

        // Overlay transparent minimal controllers
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 24.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            Row(
                modifier = Modifier
                    .background(Color(0x9A080A0F), RoundedCornerShape(20.dp))
                    .border(BorderStroke(1.dp, Color(0x30FFFFFF)), RoundedCornerShape(20.dp))
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Play/Pause Action
                IconButton(
                    onClick = {
                        if (isPlaying) exoPlayer.pause() else exoPlayer.play()
                        isPlaying = !isPlaying
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Play/pause button control",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Loop toggle control
                IconButton(
                    onClick = { isLooping = !isLooping },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Loop,
                        contentDescription = "Loop toggle control switcher",
                        tint = if (isLooping) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.5f),
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Speed Cycle pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0x22FFFFFF))
                        .clickable {
                            playSpeed = when (playSpeed) {
                                1.0f -> 1.25f
                                1.25f -> 1.5f
                                1.5f -> 2.0f
                                2.0f -> 0.5f
                                else -> 1.0f
                            }
                        }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "${playSpeed}x",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                }
            }
        }
    }
}
