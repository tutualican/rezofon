package studio.rezofon.app.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.sin
import studio.rezofon.app.audio.formatDb
import studio.rezofon.app.audio.previewGainAt
import studio.rezofon.app.ui.theme.LocalRzPalette
import studio.rezofon.app.ui.theme.RzMotion

@Composable
fun pressBounce(interaction: MutableInteractionSource? = null): Modifier {
    val source = interaction ?: remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.962f else 1f,
        animationSpec = RzMotion.press,
        label = "press",
    )
    return Modifier.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

@Composable
fun BentoTile(
    modifier: Modifier = Modifier,
    tint: Color? = null,
    accent: Color? = null,
    onClick: (() -> Unit)? = null,
    content: @Composable androidx.compose.foundation.layout.BoxScope.() -> Unit,
) {
    val p = LocalRzPalette.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.975f else 1f,
        animationSpec = RzMotion.press,
        label = "tile",
    )
    val shape = RoundedCornerShape(26.dp)
    Box(
        modifier = modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(shape)
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        tint?.copy(alpha = 0.20f) ?: p.surface,
                        (accent ?: p.surface).copy(alpha = if (tint != null) 0.10f else 1f),
                    ),
                    start = Offset.Zero,
                    end = Offset(420f, 640f),
                ),
            )
            .border(1.dp, p.outline, shape)
            .then(
                if (onClick != null) {
                    Modifier.clickable(interactionSource = interaction, indication = null, onClick = onClick)
                } else Modifier,
            )
            .padding(18.dp),
        content = content,
    )
}

@Composable
fun TileKicker(text: String, modifier: Modifier = Modifier, color: Color? = null) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = color ?: LocalRzPalette.current.textDim,
        modifier = modifier,
        letterSpacing = 1.6.sp,
    )
}

@Composable
fun SectionLabel(text: String, trailing: String? = null) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        TileKicker(text)
        if (trailing != null) {
            Text(trailing, style = MaterialTheme.typography.labelSmall, color = LocalRzPalette.current.textDim)
        }
    }
}

/** Live-looking level bars. Purely decorative, but it reacts to playback state. */
@Composable
fun WaveBars(
    modifier: Modifier = Modifier,
    active: Boolean,
    bars: Int = 28,
    color: Color = LocalRzPalette.current.lime,
    amplitude: Float = 1f,
) {
    val transition = rememberInfiniteTransition(label = "wave")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(2600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "phase",
    )
    Canvas(modifier) {
        val gap = size.width / bars
        val w = gap * 0.42f
        val minH = size.height * 0.08f
        for (i in 0 until bars) {
            val n = i.toFloat() / bars
            val jitter = 0.35f + 0.65f * abs(sin(phase * 1.7f + n * 9.2f)) *
                abs(cos(phase * 0.6f + n * 3.1f))
            val h = minH + (size.height - minH) * jitter * amplitude
            val x = i * gap + (gap - w) / 2f
            val alpha = if (active) 0.85f else 0.22f
            drawRoundRect(
                brush = Brush.verticalGradient(
                    listOf(color.copy(alpha = alpha), color.copy(alpha = alpha * 0.25f)),
                ),
                topLeft = Offset(x, (size.height - h) / 2f),
                size = Size(w, h),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(w / 2f),
            )
        }
    }
}

/** The EQ curve preview: log-frequency axis, filled response area, band dots. */
@Composable
fun CurveGraph(
    gains: FloatArray,
    subShelfDb: Float,
    bass: Float,
    modifier: Modifier = Modifier,
    color: Color = LocalRzPalette.current.lime,
    secondary: Color = LocalRzPalette.current.amber,
    showLabels: Boolean = false,
    interactiveIndex: Int? = null,
    onScrub: ((Int, Float) -> Unit)? = null,
) {
    val p = LocalRzPalette.current
    val density = LocalDensity.current
    val values = remember(gains, subShelfDb, bass) {
        FloatArray(72) { i ->
            val t = i / 71f
            val f = 20f * (20000f / 20f).pow(t)
            previewGainAt(gains, f, subShelfDb, bass)
        }
    }
    Column(modifier) {
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(if (showLabels) 148.dp else 92.dp)
                .then(
                    if (onScrub != null) {
                        Modifier.pointerInput(Unit) {
                            awaitEachGesture {
                                val down = awaitFirstDown()
                                val idx = ((down.position.x / size.width) * 10).toInt().coerceIn(0, 9)
                                onScrub(idx, gains[idx])
                                down.consume()
                                while (true) {
                                    val event = awaitPointerEvent()
                                    val ch = event.changes.firstOrNull() ?: break
                                    if (!ch.pressed) break
                                    val i2 = ((ch.position.x / size.width) * 10).toInt().coerceIn(0, 9)
                                    val frac = 1f - (ch.position.y / size.height)
                                    val db = (frac * 24f - 12f).coerceIn(-12f, 12f)
                                    onScrub(i2, db)
                                    ch.consume()
                                }
                            }
                        }
                    } else Modifier,
                ),
        ) {
            val w = size.width
            val h = size.height
            val pad = 10f
            val usable = h - pad * 2
            fun yOf(db: Float) = pad + (12f - db) / 24f * usable

            // grid
            for (i in 0..4) {
                val y = pad + usable * i / 4f
                drawLine(p.outline.copy(alpha = 0.6f), Offset(0f, y), Offset(w, y), 1f)
            }
            drawLine(
                p.textDim.copy(alpha = 0.22f),
                Offset(0f, yOf(0f)),
                Offset(w, yOf(0f)),
                1.2f,
            )

            val path = Path()
            val fill = Path()
            values.forEachIndexed { i, db ->
                val x = w * i / (values.size - 1)
                val y = yOf(db)
                if (i == 0) {
                    path.moveTo(x, y)
                    fill.moveTo(x, h)
                    fill.lineTo(x, y)
                } else {
                    path.lineTo(x, y)
                    fill.lineTo(x, y)
                }
            }
            fill.lineTo(w, h)
            fill.close()

            drawPath(
                fill,
                brush = Brush.verticalGradient(
                    listOf(color.copy(alpha = 0.30f), color.copy(alpha = 0.02f)),
                ),
            )
            drawPath(
                path,
                brush = Brush.horizontalGradient(listOf(secondary, color)),
                style = Stroke(width = with(density) { 2.5.dp.toPx() }, cap = StrokeCap.Round),
            )

            if (showLabels) {
                gains.forEachIndexed { i, db ->
                    val x = w * (i + 0.5f) / 10f
                    val y = yOf(db + previewGainAt(gains, freqOfIndex(i), subShelfDb, bass) - gains[i])
                    drawCircle(color = if (interactiveIndex == i) secondary else color, radius = 4.2f, center = Offset(x, y))
                    drawCircle(color = p.bg, radius = 1.8f, center = Offset(x, y))
                }
            }
        }
        if (showLabels) {
            Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                listOf("31", "62", "125", "250", "500", "1k", "2k", "4k", "8k", "16k").forEach {
                    Text(
                        it,
                        style = MaterialTheme.typography.labelSmall,
                        color = p.textDim,
                        modifier = Modifier.width(22.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                }
            }
        }
    }
}

private fun freqOfIndex(i: Int): Float = 31f * 2f.pow(i.toFloat())

@Composable
fun RzToggle(
    checked: Boolean,
    onChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val p = LocalRzPalette.current
    val offset by animateFloatAsState(
        targetValue = if (checked) 1f else 0f,
        animationSpec = RzMotion.tile,
        label = "toggle",
    )
    val trackPx = with(LocalDensity.current) { 20.dp.toPx() }
    val w = 50.dp
    val h = 30.dp
    Box(
        modifier
            .size(w, h)
            .clip(RoundedCornerShape(30.dp))
            .background(
                if (checked) Brush.horizontalGradient(listOf(p.lime, p.lime.copy(alpha = 0.75f)))
                else Brush.horizontalGradient(listOf(p.surfaceSunken, p.surfaceSunken)),
            )
            .border(1.dp, if (checked) Color.Transparent else p.outline, RoundedCornerShape(30.dp))
            .clickable(enabled = enabled) { onChange(!checked) },
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            Modifier
                .padding(horizontal = 4.dp)
                .graphicsLayer { translationX = offset * trackPx }
                .size(22.dp)
                .clip(RoundedCornerShape(50))
                .background(if (checked) Color(0xFF14160F) else p.textDim),
        )
    }
}

@Composable
fun RzChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val p = LocalRzPalette.current
    val inter = remember { MutableInteractionSource() }
    val pressed by inter.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.94f else 1f, RzMotion.press, label = "chip")
    Box(
        modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(50))
            .background(if (selected) p.lime else p.surfaceAlt)
            .border(1.dp, if (selected) Color.Transparent else p.outline, RoundedCornerShape(50))
            .clickable(interactionSource = inter, indication = null, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) Color(0xFF14160F) else p.text,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * Rotary control with a spring-loaded thumb. Drag vertically to change.
 * Used for Bass, Sub and Loudness so the three "feel" controls read as one family.
 */
@Composable
fun Knob(
    value: Float,
    onChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "",
    valueText: String = "",
    accent: Color = LocalRzPalette.current.lime,
    size: Dp = 108.dp,
    min: Float = 0f,
    max: Float = 1f,
) {
    val p = LocalRzPalette.current
    val latest by rememberUpdatedState(onChange)
    val norm = ((value - min) / (max - min)).coerceIn(0f, 1f)
    val animated by animateFloatAsState(norm, RzMotion.glide, label = "knob")
    val trackPx = with(LocalDensity.current) { size.toPx() }
    Box(
        modifier
            .size(size)
            .pointerInput(min, max) {
                awaitEachGesture {
                    awaitFirstDown().consume()
                    var startY = 0f
                    var startVal = 0f
                    var started = false
                    while (true) {
                        val e = awaitPointerEvent()
                        val ch = e.changes.firstOrNull() ?: break
                        if (!ch.pressed) break
                        if (!started) {
                            startY = ch.position.y
                            startVal = norm
                            started = true
                        } else {
                            val delta = (startY - ch.position.y) / trackPx * 1.6f
                            latest((startVal + delta).coerceIn(0f, 1f) * (max - min) + min)
                        }
                        ch.consume()
                    }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 11.dp.toPx()
            val inset = stroke / 2 + 3.dp.toPx()
            val arcSize = Size(this.size.width - inset * 2, this.size.height - inset * 2)
            val topLeft = Offset(inset, inset)
            val start = 135f
            val sweep = 270f
            drawArc(
                color = p.surfaceSunken,
                startAngle = start,
                sweepAngle = sweep,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(stroke, cap = StrokeCap.Round),
            )
            drawArc(
                brush = Brush.sweepGradient(listOf(accent.copy(alpha = 0.5f), accent, accent)),
                startAngle = start,
                sweepAngle = sweep * animated,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(stroke, cap = StrokeCap.Round),
            )
            val angle = Math.toRadians((start + sweep * animated).toDouble())
            val r = (arcSize.width / 2f) - stroke / 2f
            val cx = topLeft.x + arcSize.width / 2f
            val cy = topLeft.y + arcSize.height / 2f
            val knob = Offset(
                (cx + r * cos(angle)).toFloat(),
                (cy + r * sin(angle)).toFloat(),
            )
            drawCircle(accent, 5.5.dp.toPx(), knob)
            drawCircle(p.bg, 2.2.dp.toPx(), knob)
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                valueText.ifBlank { "${(value * 100).toInt()}%" },
                style = MaterialTheme.typography.headlineMedium,
                color = p.text,
            )
            if (label.isNotBlank()) {
                Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, color = p.textDim)
            }
        }
    }
}

/** Horizontal gain slider used for pre-amp and timers. */
@Composable
fun RzBar(
    value: Float,
    onChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    min: Float = 0f,
    max: Float = 1f,
    accent: Color = LocalRzPalette.current.lime,
    height: Dp = 34.dp,
) {
    val p = LocalRzPalette.current
    val latest by rememberUpdatedState(onChange)
    val norm = ((value - min) / (max - min)).coerceIn(0f, 1f)
    val animated by animateFloatAsState(norm, RzMotion.glide, label = "bar")
    Box(
        modifier
            .fillMaxWidth()
            .height(height)
            .pointerInput(min, max) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    latest((down.position.x / size.width).coerceIn(0f, 1f) * (max - min) + min)
                    down.consume()
                    while (true) {
                        val e = awaitPointerEvent()
                        val ch = e.changes.firstOrNull() ?: break
                        if (!ch.pressed) break
                        latest((ch.position.x / size.width).coerceIn(0f, 1f) * (max - min) + min)
                        ch.consume()
                    }
                }
            },
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(12.dp)
                .clip(RoundedCornerShape(50))
                .background(p.surfaceSunken),
        )
        Box(
            Modifier
                .fillMaxWidth(animated.coerceAtLeast(0.02f))
                .height(12.dp)
                .clip(RoundedCornerShape(50))
                .background(Brush.horizontalGradient(listOf(accent.copy(alpha = 0.55f), accent))),
        )
        Box(
            Modifier
                .graphicsLayer { translationX = (animated * (size.width - 26.dp.toPx())).coerceAtLeast(0f) }
                .size(26.dp)
                .clip(RoundedCornerShape(50))
                .background(p.text)
                .border(4.dp, p.bg, RoundedCornerShape(50)),
        )
    }
}

@Composable
fun StatusDot(color: Color, size: Dp = 7.dp) {
    Box(
        Modifier
            .size(size)
            .clip(RoundedCornerShape(50))
            .background(color),
    )
}

@Composable
fun MetricRow(key: String, value: String, mono: Boolean = true, valueColor: Color? = null) {
    val p = LocalRzPalette.current
    Row(
        Modifier.fillMaxWidth().padding(vertical = 7.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(key, style = MaterialTheme.typography.bodyMedium, color = p.textDim)
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = valueColor ?: p.text,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 12.dp),
        )
    }
}

fun gainText(v: Float): String = formatDb(v)

@Composable
fun RzDivider() {
    Box(
        Modifier
            .fillMaxWidth()
            .height(1.dp)
            .padding(vertical = 0.dp)
            .background(LocalRzPalette.current.outline),
    )
}

@Composable
fun MiniStat(value: String, label: String, accent: Color = LocalRzPalette.current.text) {
    Column {
        Text(value, style = MaterialTheme.typography.titleLarge, color = accent)
        Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, color = LocalRzPalette.current.textDim)
    }
}

@Composable
fun IconPill(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = LocalRzPalette.current.text,
    bg: Color = LocalRzPalette.current.surfaceAlt,
) {
    Box(
        modifier
            .defaultMinSize(minWidth = 38.dp, minHeight = 38.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 9.dp),
        contentAlignment = Alignment.Center,
    ) {
        androidx.compose.material3.Icon(icon, contentDescription, tint = tint, modifier = Modifier.size(20.dp))
    }
}

/** Decorative background wash. */
@Composable
fun RzBackdrop(content: @Composable androidx.compose.foundation.layout.BoxScope.() -> Unit) {
    val p = LocalRzPalette.current
    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(p.bg, p.bg, if (p.isDark) Color(0xFF0B0D12) else p.surfaceAlt.copy(alpha = 0.5f)),
                ),
            ),
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(p.lime.copy(alpha = if (p.isDark) 0.09f else 0.16f), Color.Transparent),
                        radius = 900f,
                        center = Offset(120f, -60f),
                    ),
                ),
        )
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(p.amber.copy(alpha = if (p.isDark) 0.06f else 0.10f), Color.Transparent),
                        radius = 760f,
                        center = Offset(900f, 1400f),
                    ),
                ),
        )
        content()
    }
}
