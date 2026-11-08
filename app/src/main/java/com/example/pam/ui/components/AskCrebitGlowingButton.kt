package com.example.pam.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

/**
 * Animated glowing circular Ask Crebit AI button or emblem.
 * Features an infinite pulsing radial halo glow and a vibrant Indigo-Purple-Fuchsia gradient surface.
 * When [showSpinningRing] is enabled, it also features an edge shimmer ring.
 */
@Composable
fun AskCrebitGlowingButton(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    showSpinningRing: Boolean = (onClick != null)
) {
    val infiniteTransition = rememberInfiniteTransition(label = "AskCrebitAnim")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.98f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.75f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        // Pulsing Outer Halo Glow
        Box(
            modifier = Modifier
                .size(54.dp)
                .scale(pulseScale)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF8B5CF6).copy(alpha = glowAlpha),
                            Color(0xFFEC4899).copy(alpha = glowAlpha * 0.45f),
                            Color.Transparent
                        )
                    )
                )
        )

        // Spinning Gradient Shimmer Ring (optional)
        if (showSpinningRing) {
            val glowRotation by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 360f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 3500, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                ),
                label = "glowRotation"
            )
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .graphicsLayer { rotationZ = glowRotation }
                    .border(
                        BorderStroke(
                            width = 2.dp,
                            brush = Brush.sweepGradient(
                                listOf(
                                    Color(0xFF6366F1), // Indigo
                                    Color(0xFF8B5CF6), // Purple
                                    Color(0xFFEC4899), // Pink
                                    Color(0xFF38BDF8), // Cyan
                                    Color(0xFF6366F1)  // Loop
                                )
                            )
                        ),
                        shape = CircleShape
                    )
            )
        }

        // Circular Button / Badge Surface
        if (onClick != null) {
            Surface(
                onClick = onClick,
                shape = CircleShape,
                color = Color.Transparent,
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(
                                Color(0xFF6366F1), // Indigo
                                Color(0xFF8B5CF6), // Purple
                                Color(0xFFD946EF)  // Fuchsia
                            )
                        )
                    )
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "Ask Crebit AI",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(
                                Color(0xFF6366F1), // Indigo
                                Color(0xFF8B5CF6), // Purple
                                Color(0xFFD946EF)  // Fuchsia
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = "Ask Crebit AI",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

