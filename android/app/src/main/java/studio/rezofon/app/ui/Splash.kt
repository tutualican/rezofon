package studio.rezofon.app.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import studio.rezofon.app.ui.theme.LocalRzPalette

/**
 * A short, physics-driven intro. Deliberately under one second: the mark is
 * already on screen from frame one, so the app never feels like it is waiting
 * for an animation to finish.
 */
@Composable
fun SplashOverlay(onDone: () -> Unit) {
    val p = LocalRzPalette.current
    val enter = remember { Animatable(0f) }
    val bars = remember { List(4) { Animatable(0f) } }
    val exit = remember { Animatable(1f) }

    LaunchedEffect(Unit) {
        enter.animateTo(1f, tween(300, easing = FastOutSlowInEasing))
        kotlinx.coroutines.coroutineScope {
            bars.forEachIndexed { i, a ->
                launch {
                    kotlinx.coroutines.delay(i * 55L)
                    a.animateTo(
                        1f,
                        spring(dampingRatio = 0.48f, stiffness = 720f, visibilityThreshold = 0.001f),
                    )
                }
            }
        }
        kotlinx.coroutines.delay(130)
        exit.animateTo(0f, tween(200, easing = FastOutSlowInEasing))
        onDone()
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(p.bg)
            .graphicsLayer { alpha = exit.value },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(340.dp)
                .blur(80.dp)
                .background(
                    Brush.radialGradient(
                        listOf(p.lime.copy(alpha = 0.20f * enter.value), Color.Transparent),
                    ),
                    RoundedCornerShape(50),
                ),
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                val heights = listOf(14.dp, 30.dp, 46.dp, 24.dp)
                val colors = listOf(p.lime, p.lime, p.amber, p.lime)
                heights.forEachIndexed { i, h ->
                    val a = bars[i].value
                    Box(
                        Modifier
                            .width(7.dp)
                            .height((h.value * (0.25f + 0.75f * a)).dp)
                            .clip(RoundedCornerShape(50))
                            .background(colors[i].copy(alpha = 0.35f + 0.65f * a)),
                    )
                }
            }
            Spacer(Modifier.height(18.dp))
            Text(
                "REZOFON",
                style = MaterialTheme.typography.displaySmall.copy(
                    letterSpacing = 6.sp,
                    fontWeight = FontWeight.Medium,
                ),
                color = p.text,
                modifier = Modifier.graphicsLayer {
                    alpha = enter.value
                    translationY = (1f - enter.value) * 18f
                },
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "JAM+ SES ATÖLYESİ",
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 2.4.sp),
                color = p.textDim,
                modifier = Modifier.graphicsLayer { alpha = enter.value * 0.9f },
            )
        }
        Canvas(Modifier.fillMaxSize()) {
            val y = size.height * 0.5f + 120.dp.toPx()
            drawLine(
                brush = Brush.horizontalGradient(
                    listOf(Color.Transparent, p.lime.copy(alpha = 0.35f * enter.value), Color.Transparent),
                ),
                start = Offset(size.width * 0.28f, y),
                end = Offset(size.width * 0.72f, y),
                strokeWidth = 1.5.dp.toPx(),
            )
            drawRoundRect(
                color = p.lime.copy(alpha = 0.6f * enter.value),
                topLeft = Offset(size.width * 0.5f - 2.dp.toPx(), y - 4.dp.toPx()),
                size = Size(4.dp.toPx(), 8.dp.toPx()),
                cornerRadius = CornerRadius(2.dp.toPx()),
            )
        }
    }
}
