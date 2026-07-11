package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PCConnectionScreen(
    hostIp: String,
    hostPort: String,
    isConnecting: Boolean,
    isConnected: Boolean,
    connectionMessage: String?,
    onIpChange: (String) -> Unit,
    onPortChange: (String) -> Unit,
    onConnectClick: () -> Unit,
    onDisconnectClick: () -> Unit,
    onBackClick: () -> Unit,
    onDismissMessage: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("PC Sync Configuration", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)) },
                navigationIcon = {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.testTag("connection_back_button")
                    ) {
                        Icon(imageVector = Icons.AutoMirrored.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF0D0E15),
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        containerColor = Color(0xFF0A0C10)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Introductory Card explaining the app's exclusive connection
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22)),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFF30363D), RoundedCornerShape(12.dp)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LaptopMac,
                            contentDescription = "Exclusive PC",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Text(
                            text = "WinUI 3 Photos Companion",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "This application operates exclusively as a client sync tool for your custom WinUI 3 Photos desktop software. Ensure the Windows app server is active and running on the same local network.",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                            lineHeight = 20.sp
                        )
                    )
                }
            }

            // Connection Status indicator
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isConnected) Color(0xFF1E4620).copy(alpha = 0.4f) else Color(0xFF4C3000).copy(alpha = 0.3f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        color = if (isConnected) MaterialTheme.colorScheme.tertiary.copy(alpha = 0.4f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(12.dp)
                    ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isConnected) Color(0xFF2ECC71) else Color(0xFFFF9800))
                    )
                    
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isConnected) "CONNECTED" else "PC HOST DISCONNECTED",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Black,
                                color = if (isConnected) Color(0xFF4AF852) else Color(0xFFFFC107)
                            )
                        )
                        Text(
                            text = if (isConnected) {
                                "Seamless sync enabled at $hostIp:$hostPort"
                            } else {
                                "Enter host IP address and port below to connect."
                            },
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
                            )
                        )
                    }
                }
            }

            // Connection Parameter Inputs
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF12161F)),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFF222B3F), RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Server Configuration",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )

                    // IP Address field
                    TextField(
                        value = hostIp,
                        onValueChange = onIpChange,
                        label = { Text("PC IP Address (e.g. 192.168.1.5)") },
                        leadingIcon = { Icon(imageVector = Icons.Default.Language, contentDescription = "IP") },
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFF0A0D15),
                            unfocusedContainerColor = Color(0xFF0D101A),
                            focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                            unfocusedIndicatorColor = Color(0xFF252D3F)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("ip_input_field"),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Next
                        )
                    )

                    // Port field
                    TextField(
                        value = hostPort,
                        onValueChange = onPortChange,
                        label = { Text("Server Port Number") },
                        leadingIcon = { Icon(imageVector = Icons.Default.Power, contentDescription = "Port") },
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFF0A0D15),
                            unfocusedContainerColor = Color(0xFF0D101A),
                            focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                            unfocusedIndicatorColor = Color(0xFF252D3F)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("port_input_field"),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Done
                        )
                    )
                }
            }

            // Status feedback bar
            AnimatedVisibility(
                visible = !connectionMessage.isNullOrEmpty(),
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (isConnected) Color(0xFF1B5E20).copy(alpha = 0.15f) else Color(0xFFE53935).copy(alpha = 0.15f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            1.dp,
                            if (isConnected) Color(0xFF4CAF50).copy(alpha = 0.3f) else Color(0xFFEF5350).copy(alpha = 0.3f),
                            RoundedCornerShape(8.dp)
                        )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = if (isConnected) Icons.Default.CheckCircle else Icons.Default.ErrorOutline,
                            contentDescription = "Status Response",
                            tint = if (isConnected) Color(0xFF81C784) else Color(0xFFE57373)
                        )
                        Text(
                            text = connectionMessage ?: "",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = if (isConnected) Color(0xFFC8E6C9) else Color(0xFFFFCDD2)
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = onDismissMessage, modifier = Modifier.size(24.dp)) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Dismiss",
                                tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                            )
                        }
                    }
                }
            }

            // Connection action button triggering
            Button(
                onClick = onConnectClick,
                enabled = !isConnecting,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("connect_action_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isConnecting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Icon(imageVector = Icons.Default.PlayArrow, contentDescription = "Connect")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isConnected) "Re-test & Save" else "Establish Sync Pipeline",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }

            if (isConnected) {
                OutlinedButton(
                    onClick = onDisconnectClick,
                    border = BorderStroke(1.dp, Color(0xFFE53935)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("disconnect_action_button"),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFFEF5350)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.CloudOff, contentDescription = "Disconnect")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Disconnect & Flush Cached Meta",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
