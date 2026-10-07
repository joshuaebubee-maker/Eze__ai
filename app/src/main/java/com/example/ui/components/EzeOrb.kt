package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.OrbBlue
import com.example.ui.theme.OrbCyan
import com.example.ui.theme.OrbGreen
import com.example.ui.theme.OrbMagenta
import com.example.ui.theme.OrbRed
import com.example.ui.theme.OrbViolet
import kotlin.math.cos
import kotlin.math.sin

enum class OrbState {
    IDLE,
    LISTENING,
    THINKING,
    SPEAKING,
    ERROR
}

@Composable
fun EzeOrb(
    state: OrbState,
    modifier: Modifier = Modifier,
    size: Dp = 150.dp,
    onClick: () -> Unit = {}
) {
    val infiniteTransition = rememberInfiniteTransition(label = "orb_anim")

    // Idle breathing pulse
    val idlePulse by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "idle_pulse"
    )

    // Fast rotation for thinking
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                when (state) {
                    OrbState.THINKING -> 2500
                    OrbState.LISTENING -> 4000
                    OrbState.SPEAKING -> 3000
                    else -> 8000
                },
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    // Speaking vibration ripple
    val rippleScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ripple"
    )

    val rippleAlpha by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ripple_alpha"
    )

    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .size(size)
            .testTag("eze_orb")
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val center = Offset(this.size.width / 2f, this.size.height / 2f)
            val baseRadius = this.size.minDimension / 2.7f

            val (primaryColor, secondaryColor, outerGlowColor) = when (state) {
                OrbState.IDLE -> Triple(OrbCyan, OrbViolet, OrbBlue.copy(alpha = 0.4f))
                OrbState.LISTENING -> Triple(OrbCyan, OrbGreen, OrbCyan.copy(alpha = 0.6f))
                OrbState.THINKING -> Triple(OrbMagenta, OrbViolet, OrbMagenta.copy(alpha = 0.5f))
                OrbState.SPEAKING -> Triple(OrbCyan, OrbBlue, OrbViolet.copy(alpha = 0.6f))
                OrbState.ERROR -> Triple(OrbRed, OrbMagenta, OrbRed.copy(alpha = 0.4f))
            }

            // 1. External Ripples for Speaking / Listening
            if (state == OrbState.SPEAKING || state == OrbState.LISTENING) {
                drawCircle(
                    color = outerGlowColor.copy(alpha = rippleAlpha),
                    radius = baseRadius * rippleScale,
                    center = center,
                    style = Stroke(width = 3.dp.toPx())
                )
            }

            // 2. Diffuse Ambient Outer Glow
            val glowRadius = baseRadius * (if (state == OrbState.IDLE) idlePulse * 1.25f else 1.3f)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(outerGlowColor, Color.Transparent),
                    center = center,
                    radius = glowRadius
                ),
                radius = glowRadius,
                center = center
            )

            // 3. Shifting Plasma Core
            val rad = Math.toRadians(rotation.toDouble())
            val shiftX = (cos(rad) * (baseRadius * 0.25f)).toFloat()
            val shiftY = (sin(rad) * (baseRadius * 0.25f)).toFloat()
            val coreOffset = Offset(center.x + shiftX, center.y + shiftY)

            val currentScale = if (state == OrbState.IDLE) idlePulse else 1.0f
            val coreRadius = baseRadius * currentScale

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.9f),
                        primaryColor,
                        secondaryColor,
                        Color.Transparent
                    ),
                    center = coreOffset,
                    radius = coreRadius
                ),
                radius = coreRadius,
                center = center
            )

            // 4. Subtle Inner Ring
            drawCircle(
                brush = Brush.sweepGradient(
                    colors = listOf(
                        primaryColor.copy(alpha = 0.8f),
                        secondaryColor.copy(alpha = 0.8f),
                        outerGlowColor.copy(alpha = 0.8f),
                        primaryColor.copy(alpha = 0.8f)
                    ),
                    center = center
                ),
                radius = coreRadius * 0.95f,
                center = center,
                style = Stroke(width = 2.dp.toPx())
            )
        }
    }
}
