package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.UnlockState

@Composable
fun PasscodeScreen(
    unlockState: UnlockState,
    pinBuffer: String,
    setupFirstPin: String?,
    securityError: String?,
    onPinDigit: (String) -> Unit,
    onPinDelete: () -> Unit
) {
    val isSetupMode = unlockState is UnlockState.NeedsSetup

    // Background gradient for atmospheric warmth and premium look
    val backgroundBrush = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF0D0E15),
            Color(0xFF050608)
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundBrush)
            .safeDrawingPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Header Section
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            shape = CircleShape
                        )
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isSetupMode) Icons.Default.Security else Icons.Default.Lock,
                        contentDescription = "Lock Icon",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = if (isSetupMode) {
                        if (setupFirstPin == null) "Configure Passcode" else "Confirm Passcode"
                    } else {
                        "Vault Unlocking"
                    },
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    ),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = if (isSetupMode) {
                        if (setupFirstPin == null) {
                            "To preserve privacy, please establish a 4-digit access passcode."
                        } else {
                            "Re-enter the 4-digit passcode to verify accuracy."
                        }
                    } else {
                        "This gallery is locked. Enter your security passcode to proceed."
                    },
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    ),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            // Dots Indicator Zone
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(4) { index ->
                        val isFilled = index < pinBuffer.length
                        val dotColor by animateColorAsState(
                            targetValue = if (isFilled) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f)
                            },
                            animationSpec = spring(stiffness = Spring.StiffnessLow),
                            label = "dotColor"
                        )
                        val dotSize by animateDpAsState(
                            targetValue = if (isFilled) 18.dp else 14.dp,
                            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                            label = "dotSize"
                        )

                        Box(
                            modifier = Modifier
                                .size(dotSize)
                                .clip(CircleShape)
                                .background(dotColor)
                                .border(
                                    width = 1.dp,
                                    color = if (isFilled) Color.Transparent else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                                    shape = CircleShape
                                )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Error Message Area
                AnimatedVisibility(
                    visible = !securityError.isNullOrEmpty(),
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Text(
                        text = securityError ?: "",
                        color = Color(0xFFF44336),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Medium
                        ),
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .background(
                                color = Color(0xFFF44336).copy(alpha = 0.15f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                }
            }

            // Pin Pad (Number Keypad Grid)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val rowModifier = Modifier.fillMaxWidth(0.85f)
                
                Row(modifier = rowModifier, horizontalArrangement = Arrangement.SpaceBetween) {
                    PinButton(digit = "1", onClick = { onPinDigit("1") })
                    PinButton(digit = "2", onClick = { onPinDigit("2") })
                    PinButton(digit = "3", onClick = { onPinDigit("3") })
                }
                Row(modifier = rowModifier, horizontalArrangement = Arrangement.SpaceBetween) {
                    PinButton(digit = "4", onClick = { onPinDigit("4") })
                    PinButton(digit = "5", onClick = { onPinDigit("5") })
                    PinButton(digit = "6", onClick = { onPinDigit("6") })
                }
                Row(modifier = rowModifier, horizontalArrangement = Arrangement.SpaceBetween) {
                    PinButton(digit = "7", onClick = { onPinDigit("7") })
                    PinButton(digit = "8", onClick = { onPinDigit("8") })
                    PinButton(digit = "9", onClick = { onPinDigit("9") })
                }
                Row(modifier = rowModifier, horizontalArrangement = Arrangement.SpaceBetween) {
                    // Empty spacer button for keypad symmetry
                    Spacer(modifier = Modifier.size(72.dp))
                    PinButton(digit = "0", onClick = { onPinDigit("0") })
                    // Delete backspace button
                    PinIconButton(
                        tag = "pin_delete_button",
                        icon = Icons.Default.Delete,
                        contentDescription = "Delete",
                        onClick = onPinDelete
                    )
                }
            }
        }
    }
}

@Composable
fun PinButton(
    digit: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(72.dp)
            .clip(CircleShape)
            .background(Color(0xFF161A24))
            .border(
                width = 1.dp,
                color = Color(0xFF252D3F),
                shape = CircleShape
            )
            .clickable(onClick = onClick)
            .testTag("pin_key_$digit"),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = digit,
            style = MaterialTheme.typography.headlineLarge.copy(
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )
        )
    }
}

@Composable
fun PinIconButton(
    tag: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(72.dp)
            .clip(CircleShape)
            .background(Color(0xFF161A24).copy(alpha = 0.5f))
            .border(
                width = 1.dp,
                color = Color(0xFF252D3F).copy(alpha = 0.5f),
                shape = CircleShape
            )
            .clickable(onClick = onClick)
            .testTag(tag),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
            modifier = Modifier.size(26.dp)
        )
    }
}
