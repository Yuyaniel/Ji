package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import kotlin.random.Random

data class ConfettiParticle(
    var x: Float,
    var y: Float,
    val color: Color,
    val size: Float,
    val shape: Int, // 0: Rect, 1: Circle, 2: Oval
    var vx: Float,
    var vy: Float,
    var rotation: Float,
    val rotationSpeed: Float,
    val gravity: Float = 0.35f,
    val drag: Float = 0.98f
)

@Composable
fun ConfettiOverlay(
    trigger: Any?,
    modifier: Modifier = Modifier,
    onFinished: () -> Unit = {}
) {
    if (trigger == null) return

    val progress = remember(trigger) { Animatable(0f) }

    // Spawn 80 creative particles
    val particles = remember(trigger) {
        List(80) {
            val fromLeft = Random.nextBoolean()
            // Launch from either bottom left or bottom right, or randomly at the bottom
            val startX = if (fromLeft) Random.nextFloat() * 100f else 800f + Random.nextFloat() * 100f
            val startY = 1600f // general bottom area

            // Shoot upwards and towards center
            val angle = if (fromLeft) {
                // Angle between -30 and -75 degrees (shooting right and up)
                Random.nextFloat() * (Math.PI / 3) + (Math.PI / 12)
            } else {
                // Angle between -105 and -150 degrees (shooting left and up)
                Random.nextFloat() * (Math.PI / 3) + (Math.PI * 7 / 12)
            }
            val speed = 25f + Random.nextFloat() * 20f
            val vx = (Math.cos(angle) * speed).toFloat()
            val vy = (-Math.sin(angle) * speed).toFloat()

            val size = dpToPx(8f + Random.nextFloat() * 16f)
            val shape = Random.nextInt(3)
            val color = Color(
                red = Random.nextFloat() * 0.4f + 0.6f,   // high luminance
                green = Random.nextFloat() * 0.6f + 0.4f,
                blue = Random.nextFloat() * 0.8f + 0.2f,
                alpha = 1f
            )

            ConfettiParticle(
                x = startX,
                y = startY,
                color = color,
                size = size,
                shape = shape,
                vx = vx,
                vy = vy,
                rotation = Random.nextFloat() * 360f,
                rotationSpeed = (Random.nextFloat() - 0.5f) * 15f
            )
        }
    }

    LaunchedEffect(trigger) {
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 1800, easing = LinearEasing)
        )
        onFinished()
    }

    if (progress.value < 1f) {
        Canvas(modifier = modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height

            // Initialize position centered at the bottom corners dynamically based on real canvas size
            if (progress.value == 0f) {
                particles.forEach { p ->
                    // Re-anchor start spots to canvas edges
                    if (p.x < 200f) {
                        p.x = Random.nextFloat() * 50f
                    } else {
                        p.x = canvasWidth - Random.nextFloat() * 50f
                    }
                    p.y = canvasHeight + 10f
                }
            }

            particles.forEach { p ->
                // Apply physics step
                p.x += p.vx
                p.y += p.vy
                p.vx *= p.drag
                p.vy = (p.vy + p.gravity) * p.drag
                p.rotation += p.rotationSpeed

                // Draw each pretty shape
                rotate(degrees = p.rotation, pivot = Offset(p.x, p.y)) {
                    when (p.shape) {
                        0 -> drawRect(
                            color = p.color,
                            topLeft = Offset(p.x - p.size / 2, p.y - p.size / 2),
                            size = Size(p.size, p.size * 0.6f)
                        )
                        1 -> drawCircle(
                            color = p.color,
                            radius = p.size / 2,
                            center = Offset(p.x, p.y)
                        )
                        else -> drawOval(
                            color = p.color,
                            topLeft = Offset(p.x - p.size / 2, p.y - p.size / 4),
                            size = Size(p.size, p.size / 2)
                        )
                    }
                }
            }
        }
    }
}

// Simple Helper instead of LocalDensity to keep it isolated and speed-safe in simple Canvas drawing
private fun dpToPx(dp: Float): Float {
    return dp * 2.75f // Approximate density scale for modern standard 440dpi devices
}
