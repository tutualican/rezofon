package studio.rezofon.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import studio.rezofon.app.audio.RzBandLabels
import studio.rezofon.app.audio.RzPresets
import studio.rezofon.app.audio.formatDb
import studio.rezofon.app.audio.suggestedPreamp
import studio.rezofon.app.state.AppState
import studio.rezofon.app.ui.theme.LocalRzPalette

@Composable
fun EqScreen(state: AppState) {
    val p = LocalRzPalette.current
    val params = state.params
    var comparing by remember { mutableStateOf(false) }
    var saveDialog by remember { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp)
            .padding(bottom = 118.dp),
    ) {
        ScreenHeader(
            title = "Ekolayzer",
            subtitle = "10 bant · ±12 dB · logaritmik eğri",
            trailing = {
                RzToggle(
                    checked = params.enabled,
                    onChange = { state.commit(params.copyWith(enabled = it)) },
                )
            },
        )

        BentoTile(Modifier.fillMaxWidth(), tint = p.lime.copy(alpha = 0.06f)) {
            Column {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TileKicker("Yanıt eğrisi")
                    Text(
                        if (comparing) "BYPASS" else formatDb(params.preampDb) + " preamp",
                        style = MaterialTheme.typography.labelLarge,
                        color = if (comparing) p.amber else p.textDim,
                    )
                }
                Spacer(Modifier.height(4.dp))
                CurveGraph(
                    gains = params.gainsDb,
                    subShelfDb = params.subShelfDb,
                    bass = params.bassBoost,
                    modifier = Modifier.fillMaxWidth(),
                    color = p.lime,
                    secondary = p.cyan,
                    showLabels = true,
                    onScrub = { index, db -> state.setGain(index, db) },
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        BentoTile(Modifier.fillMaxWidth()) {
            Column {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TileKicker("Bant kazançları")
                    Text(
                        "sürükle · ±12 dB",
                        style = MaterialTheme.typography.labelSmall,
                        color = p.textDim,
                    )
                }
                Spacer(Modifier.height(10.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    RzBandLabels.forEachIndexed { i, label ->
                        BandFader(
                            index = i,
                            label = label,
                            value = params.gainsDb[i],
                            accent = when {
                                i < 3 -> p.amber
                                i < 7 -> p.lime
                                else -> p.cyan
                            },
                            onChange = { state.setGain(i, it) },
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        BentoTile(Modifier.fillMaxWidth()) {
            Column {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    TileKicker("Ön kazanç")
                    Text(
                        formatDb(params.preampDb),
                        style = MaterialTheme.typography.labelLarge,
                        color = p.text,
                    )
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    "3.5 W sürücüde tepe noktaları kırpmamak için otomatik öneri: " +
                        formatDb(suggestedPreamp(params.gainsDb, params.subShelfDb, params.bassBoost)),
                    style = MaterialTheme.typography.bodySmall,
                    color = p.textDim,
                )
                Spacer(Modifier.height(8.dp))
                RzBar(
                    value = params.preampDb,
                    onChange = { state.commit(params.copyWith(preampDb = it)) },
                    min = -12f,
                    max = 0f,
                    accent = p.cyan,
                )
                Spacer(Modifier.height(6.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ActionPill(
                        text = "Düzleştir",
                        icon = Icons.Rounded.RestartAlt,
                        modifier = Modifier.weight(1f),
                        onClick = { state.flatten() },
                    )
                    ActionPill(
                        text = "Profil kaydet",
                        icon = Icons.Rounded.Save,
                        accent = p.lime,
                        modifier = Modifier.weight(1f),
                        onClick = { saveDialog = true },
                    )
                }
            }
        }

        SectionLabel("Hazır eğriler", "dokun ve uygula")
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            RzPresets.forEach { preset ->
                RzChip(
                    text = preset.name,
                    selected = preset.id == state.activePresetId,
                    onClick = { state.applyPreset(preset) },
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            RzPresets.firstOrNull { it.id == state.activePresetId }?.tagline
                ?: "Kendi eğrin · profiller paylaşılabilir",
            style = MaterialTheme.typography.bodySmall,
            color = p.textDim,
            modifier = Modifier.padding(horizontal = 4.dp),
        )

        Spacer(Modifier.height(12.dp))

        HoldToCompare(
            enabled = params.enabled,
            active = comparing,
            onCompare = { pressed ->
                comparing = pressed
                state.engine.apply(params.copyWith(enabled = params.enabled && !pressed))
            },
        )
    }

    if (saveDialog) {
        NameDialog(
            title = "Profili kaydet",
            hint = "Örn. Balkon, Gece, Murat'ın ayarı",
            onDismiss = { saveDialog = false },
            onConfirm = { name ->
                state.saveProfile(name)
                saveDialog = false
            },
        )
    }
}

@Composable
private fun BandFader(
    index: Int,
    label: String,
    value: Float,
    accent: Color,
    onChange: (Float) -> Unit,
) {
    val p = LocalRzPalette.current
    val latest by rememberUpdatedState(onChange)
    val h = 168.dp
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            (if (value > 0.05f) "+" else "") + "%.0f".format(value),
            style = MaterialTheme.typography.labelSmall,
            color = if (value > 0.05f) accent else p.textDim,
        )
        Spacer(Modifier.height(6.dp))
        Box(
            Modifier
                .width(26.dp)
                .height(h)
                .pointerInput(Unit) {
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        fun apply(y: Float) {
                            val frac = 1f - (y / size.height.toFloat()).coerceIn(0f, 1f)
                            latest((frac * 24f - 12f).coerceIn(-12f, 12f))
                        }
                        apply(down.position.y)
                        down.consume()
                        while (true) {
                            val e = awaitPointerEvent()
                            val ch = e.changes.firstOrNull() ?: break
                            if (!ch.pressed) break
                            apply(ch.position.y)
                            ch.consume()
                        }
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            Canvas(Modifier.fillMaxSize()) {
                val cx = size.width / 2f
                drawLine(
                    p.surfaceSunken,
                    Offset(cx, 0f),
                    Offset(cx, size.height),
                    strokeWidth = 6.dp.toPx(),
                    cap = StrokeCap.Round,
                )
                val zeroY = size.height * 0.5f
                val valueY = size.height * (12f - value) / 24f
                drawLine(
                    brush = Brush.verticalGradient(listOf(accent, accent.copy(alpha = 0.45f))),
                    start = Offset(cx, minOf(zeroY, valueY)),
                    end = Offset(cx, maxOf(zeroY, valueY)),
                    strokeWidth = 6.dp.toPx(),
                    cap = StrokeCap.Round,
                )
                drawLine(
                    p.textDim.copy(alpha = 0.25f),
                    Offset(cx - 7.dp.toPx(), zeroY),
                    Offset(cx + 7.dp.toPx(), zeroY),
                    strokeWidth = 1.dp.toPx(),
                )
                drawCircle(accent, 8.dp.toPx(), Offset(cx, valueY))
                drawCircle(p.bg, 3.dp.toPx(), Offset(cx, valueY))
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
            color = p.textDim,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(30.dp),
        )
    }
}

@Composable
private fun ActionPill(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    accent: Color = Color.Transparent,
) {
    val p = LocalRzPalette.current
    val bg = if (accent == Color.Transparent) p.surfaceAlt else accent
    val fg = if (accent == Color.Transparent) p.text else Color(0xFF14160F)
    val inter = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    Row(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .background(bg)
            .clickable(interactionSource = inter, indication = null, onClick = onClick)
            .then(pressBounce(inter))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = fg, modifier = Modifier.size(17.dp))
        Spacer(Modifier.width(8.dp))
        Text(text, style = MaterialTheme.typography.labelLarge, color = fg)
    }
}

@Composable
private fun HoldToCompare(
    enabled: Boolean,
    onCompare: (Boolean) -> Unit,
    active: Boolean,
) {
    val p = LocalRzPalette.current
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(if (active) p.text else p.surfaceSunken)
            .border(1.dp, p.outline, RoundedCornerShape(20.dp))
            .pointerInput(enabled) {
                awaitEachGesture {
                    awaitFirstDown().consume()
                    onCompare(true)
                    while (true) {
                        val e = awaitPointerEvent()
                        if (e.changes.none { it.pressed }) break
                    }
                    onCompare(false)
                }
            }
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            if (active) "Bypass — bırakınca geri döner" else "Basılı tut · EQ'yu bypass et",
            style = MaterialTheme.typography.labelLarge,
            color = if (active) p.bg else p.textDim,
        )
    }
}
