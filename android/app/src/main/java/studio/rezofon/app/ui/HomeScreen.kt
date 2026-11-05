package studio.rezofon.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowOutward
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import studio.rezofon.app.audio.EqPreset
import studio.rezofon.app.audio.formatDb
import studio.rezofon.app.state.AppState
import studio.rezofon.app.ui.theme.LocalRzPalette

@Composable
fun HomeScreen(
    state: AppState,
    onOpenEq: () -> Unit,
    onOpenBass: () -> Unit,
    onOpenDevice: () -> Unit,
    onOpenWorkshop: () -> Unit,
    onOpenGuard: () -> Unit,
    onOpenSettings: () -> Unit,
    onApplyPreset: (EqPreset) -> Unit,
) {
    val p = LocalRzPalette.current
    val snap = state.snapshot
    val params = state.params

    Column(
        Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp)
            .padding(bottom = 118.dp),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text(
                    "REZOFON",
                    style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 4.5.sp),
                    color = p.lime,
                )
                Text(
                    "Ses atölyesi",
                    style = MaterialTheme.typography.displaySmall,
                    color = p.text,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconPill(Icons.Rounded.Refresh, "Yenile", onClick = {
                    state.reconnectDevice()
                }, bg = p.surfaceAlt, tint = p.textDim)
                IconPill(Icons.Rounded.Settings, "Ayarlar", onClick = onOpenSettings, bg = p.surfaceAlt, tint = p.textDim)
            }
        }

        // Hero: the link itself
        BentoTile(
            modifier = Modifier.fillMaxWidth(),
            tint = p.lime.copy(alpha = 0.10f),
            accent = p.cyan.copy(alpha = 0.05f),
            onClick = onOpenDevice,
        ) {
            Column {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top,
                ) {
                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            StatusDot(if (snap.a2dpConnected) p.lime else p.amber)
                            Spacer(Modifier.width(7.dp))
                            TileKicker(if (snap.a2dpConnected) "Ses hattı açık" else "Hat bekliyor")
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            snap.name,
                            style = MaterialTheme.typography.headlineMedium.copy(fontSize = 27.sp),
                            color = p.text,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            snap.address.ifBlank { "eşleşme bekleniyor" },
                            style = MaterialTheme.typography.bodySmall,
                            color = p.textDim,
                        )
                    }
                    Text(
                        snap.codec,
                        style = MaterialTheme.typography.labelLarge,
                        color = p.lime,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(p.lime.copy(alpha = 0.12f))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                    )
                }
                Spacer(Modifier.height(14.dp))
                WaveBars(
                    Modifier.fillMaxWidth().height(52.dp),
                    active = snap.a2dpConnected || snap.hfpConnected,
                    bars = 34,
                    color = if (snap.a2dpConnected) p.lime else p.textDim,
                )
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MiniTag("A2DP", snap.a2dpConnected)
                    MiniTag("HFP", snap.hfpConnected)
                    MiniTag("AVRCP", snap.avrcpConnected)
                }
                Spacer(Modifier.height(12.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        snap.codecDetail,
                        style = MaterialTheme.typography.bodySmall,
                        color = p.textDim,
                    )
                    Text(
                        "Cihaz detayı →",
                        style = MaterialTheme.typography.labelLarge,
                        color = p.text,
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            BentoTile(
                modifier = Modifier.weight(1.45f).height(196.dp),
                tint = p.lime.copy(alpha = 0.06f),
                onClick = onOpenEq,
            ) {
                Column(Modifier.fillMaxWidth()) {
                    TileKicker("10 bant ekolayzer")
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            state.activePresetId
                                .let { id -> studio.rezofon.app.audio.RzPresets.firstOrNull { it.id == id }?.name }
                                ?: "Özel eğri",
                            style = MaterialTheme.typography.titleLarge,
                            color = p.text,
                        )
                    }
                    Spacer(Modifier.height(2.dp))
                    Text(
                        if (params.enabled) formatDb(params.preampDb) + " preamp" else "devre dışı",
                        style = MaterialTheme.typography.bodySmall,
                        color = p.textDim,
                    )
                    Spacer(Modifier.height(8.dp))
                    CurveGraph(
                        gains = params.gainsDb,
                        subShelfDb = params.subShelfDb,
                        bass = params.bassBoost,
                        modifier = Modifier.fillMaxWidth(),
                        color = p.lime,
                        secondary = p.cyan,
                    )
                }
            }

            BentoTile(
                modifier = Modifier.weight(1f).height(196.dp),
                tint = if (params.enabled) p.lime.copy(alpha = 0.10f) else p.danger.copy(alpha = 0.06f),
            ) {
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.SpaceBetween) {
                    TileKicker("Motor")
                    Spacer(Modifier.height(8.dp))
                    Box(
                        Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(if (params.enabled) p.lime else p.surfaceSunken),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Rounded.Bolt,
                            null,
                            tint = if (params.enabled) Color(0xFF14160F) else p.textDim,
                            modifier = Modifier.size(26.dp),
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        if (params.enabled) "Açık" else "Kapalı",
                        style = MaterialTheme.typography.titleLarge,
                        color = p.text,
                    )
                    Text(
                        if (state.engine.systemModeWorking) "Sistem etkisi" else "Oturum etkisi",
                        style = MaterialTheme.typography.bodySmall,
                        color = p.textDim,
                    )
                    Spacer(Modifier.height(6.dp))
                    RzToggle(
                        checked = params.enabled,
                        onChange = { state.commit(params.copyWith(enabled = it)) },
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            BentoTile(
                modifier = Modifier.weight(1f).height(196.dp),
                tint = p.amber.copy(alpha = 0.07f),
                onClick = onOpenBass,
            ) {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        TileKicker("Bas derinliği")
                        Text("↗", color = p.textDim, style = MaterialTheme.typography.bodyMedium)
                    }
                    Spacer(Modifier.height(2.dp))
                    Knob(
                        value = params.bassBoost,
                        onChange = { state.commit(params.copyWith(bassBoost = it)) },
                        valueText = "${(params.bassBoost * 100).toInt()}",
                        label = "punch",
                        accent = p.amber,
                        size = 116.dp,
                    )
                }
            }
            BentoTile(
                modifier = Modifier.weight(1f).height(196.dp),
                tint = p.violet.copy(alpha = 0.07f),
                onClick = onOpenBass,
            ) {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        TileKicker("Sub eşiği")
                        Text("↗", color = p.textDim, style = MaterialTheme.typography.bodyMedium)
                    }
                    Spacer(Modifier.height(2.dp))
                    Knob(
                        value = params.subShelfDb,
                        onChange = { state.commit(params.copyWith(subShelfDb = it, preampDb = studio.rezofon.app.audio.suggestedPreamp(params.gainsDb, it, params.bassBoost))) },
                        min = 0f,
                        max = 9f,
                        valueText = formatDb(params.subShelfDb).replace(" dB", ""),
                        label = "shelf",
                        accent = p.violet,
                        size = 116.dp,
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        BentoTile(
            modifier = Modifier.fillMaxWidth(),
            tint = if (state.guardRunning) p.cyan.copy(alpha = 0.10f) else p.surface,
            onClick = onOpenGuard,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (state.guardRunning) p.cyan else p.surfaceSunken),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Rounded.Timer,
                        null,
                        tint = if (state.guardRunning) Color(0xFF07201D) else p.textDim,
                        modifier = Modifier.size(23.dp),
                    )
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text("Hat bekçisi", style = MaterialTheme.typography.titleLarge, color = p.text)
                    Text(
                        if (state.guardRunning) {
                            "Uyku sayacı sıfırlanıyor · ${state.prefs.guardInterval} sn"
                        } else {
                            "Sessizlikte 15 dk sonra kapanmayı engelle"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = p.textDim,
                    )
                }
                RzToggle(
                    checked = state.guardRunning,
                    onChange = { state.setGuard(it) },
                )
            }
        }

        SectionLabel("Hazır eğriler", "${studio.rezofon.app.audio.RzPresets.size} profil")
        Row(
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            studio.rezofon.app.audio.RzPresets.forEach { preset ->
                RzChip(
                    text = preset.name,
                    selected = preset.id == state.activePresetId,
                    onClick = { onApplyPreset(preset) },
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            BentoTile(
                modifier = Modifier.weight(1f),
                onClick = onOpenDevice,
            ) {
                Column {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        TileKicker("Bağlantı")
                        Icon(Icons.Rounded.ArrowOutward, null, tint = p.textDim, modifier = Modifier.size(16.dp))
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        if (snap.a2dpConnected) "Kararlı" else "Kopuk",
                        style = MaterialTheme.typography.titleLarge,
                        color = if (snap.a2dpConnected) p.lime else p.amber,
                    )
                    Text(
                        "Kodek, profil ve sürücü bilgisi",
                        style = MaterialTheme.typography.bodySmall,
                        color = p.textDim,
                    )
                }
            }
            BentoTile(
                modifier = Modifier.weight(1f),
                tint = p.violet.copy(alpha = 0.08f),
                onClick = onOpenWorkshop,
            ) {
                Column {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        TileKicker("Atölye")
                        Icon(Icons.Rounded.ArrowOutward, null, tint = p.textDim, modifier = Modifier.size(16.dp))
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "Derin ayarlar",
                        style = MaterialTheme.typography.titleLarge,
                        color = p.text,
                    )
                    Text(
                        "Test sinyali, sistem modu, kodek",
                        style = MaterialTheme.typography.bodySmall,
                        color = p.textDim,
                    )
                }
            }
        }
    }
}

@Composable
private fun MiniTag(text: String, on: Boolean) {
    val p = LocalRzPalette.current
    Row(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(if (on) p.lime.copy(alpha = 0.14f) else p.surfaceSunken)
            .padding(horizontal = 9.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StatusDot(if (on) p.lime else p.textDim, 5.dp)
        Spacer(Modifier.width(5.dp))
        Text(
            text,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = if (on) p.lime else p.textDim,
        )
    }
}
