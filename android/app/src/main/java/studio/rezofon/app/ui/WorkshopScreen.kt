package studio.rezofon.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import studio.rezofon.app.audio.ToneGenerator
import studio.rezofon.app.audio.ToneKind
import studio.rezofon.app.state.AppState
import studio.rezofon.app.ui.theme.LocalRzPalette

@Composable
fun WorkshopScreen(
    state: AppState,
    tone: ToneGenerator,
    onOpenGuard: () -> Unit,
) {
    val p = LocalRzPalette.current

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp)
            .padding(bottom = 118.dp),
    ) {
        ScreenHeader(
            title = "Atölye",
            subtitle = "Motor, kodek ve doğrulama",
        )

        BentoTile(Modifier.fillMaxWidth(), tint = p.violet.copy(alpha = 0.09f)) {
            Column {
                TileKicker("Test sinyali")
                Spacer(Modifier.height(6.dp))
                Text(
                    "Ekolayzerin gerçekten uygulandığını kulakla doğrulamak için kısa bir sinyal. " +
                        "Müzik çalar değildir, kendi ses oturumunu açar.",
                    style = MaterialTheme.typography.bodySmall,
                    color = p.textDim,
                )
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ToneKind.entries.forEach { k ->
                        RzChip(
                            text = k.label,
                            selected = tone.running && tone.kind == k,
                            onClick = {
                                if (tone.running && tone.kind == k) {
                                    tone.stop()
                                } else {
                                    tone.start(k) { sid ->
                                        state.engine.attach(sid, state.params)
                                    }
                                }
                            },
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        if (tone.running) "çalıyor · oturum #${tone.sessionId}" else tone.describeOctaveSpan(),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (tone.running) p.lime else p.textDim,
                    )
                    Row(
                        Modifier
                            .clip(RoundedCornerShape(50))
                            .background(if (tone.running) p.danger else p.lime)
                            .clickableRipple {
                                if (tone.running) tone.stop()
                                else tone.start(ToneKind.Sweep) { sid -> state.engine.attach(sid, state.params) }
                            }
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            if (tone.running) Icons.Rounded.Stop else Icons.Rounded.PlayArrow,
                            null,
                            tint = if (tone.running) Color.White else Color(0xFF14160F),
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            if (tone.running) "Durdur" else "Sinyal ver",
                            style = MaterialTheme.typography.labelLarge,
                            color = if (tone.running) Color.White else Color(0xFF14160F),
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
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        TileKicker("Sistem geneli")
                        Text("Tüm uygulamalara uygula", style = MaterialTheme.typography.titleLarge, color = p.text)
                    }
                    RzToggle(
                        checked = state.globalMode,
                        onChange = { state.toggleGlobalMode(it) },
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    "Android 10'dan itibaren üçüncü parti uygulamaların global ses oturumuna (0) etki " +
                        "bağlaması kısıtlanabilir. Rezofon bunu gizlemez: bağlanamazsa durumu burada " +
                        "bildirir ve oturum modunda çalışmaya devam eder.",
                    style = MaterialTheme.typography.bodySmall,
                    color = p.textDim,
                )
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    RzChip(
                        text = "Yeniden bağla",
                        selected = false,
                        onClick = {
                            if (state.globalMode) state.engine.attachSystem(state.params)
                            else state.engine.sessionModeOnly()
                        },
                    )
                    RzChip(
                        text = if (state.engine.systemModeWorking) "sistem etkisi: açık" else "sistem etkisi: kısıtlı",
                        selected = state.engine.systemModeWorking,
                        onClick = {},
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        BentoTile(Modifier.fillMaxWidth()) {
            Column {
                TileKicker("Teşhis")
                Spacer(Modifier.height(4.dp))
                state.engine.diagnostics().forEachIndexed { i, (k, v) ->
                    MetricRow(k, v)
                    if (i < state.engine.diagnostics().size - 1) RzDivider()
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        BentoTile(Modifier.fillMaxWidth(), tint = p.cyan.copy(alpha = 0.06f)) {
            Column {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        TileKicker("Özellik paneli")
                        Text("Gelişmiş davranışlar", style = MaterialTheme.typography.titleLarge, color = p.text)
                    }
                }
                Spacer(Modifier.height(10.dp))
                AdvancedRow(
                    "Genişletici",
                    "Stereo genişlik simülasyonu. Mono sürücüde etkisi sınırlıdır.",
                ) {
                    RzToggle(
                        checked = state.params.virtualizer > 0.05f,
                        onChange = { on ->
                            state.commit(state.params.copyWith(virtualizer = if (on) 0.45f else 0f))
                        },
                    )
                }
                RzDivider()
                AdvancedRow(
                    "Kırpma koruması",
                    "Bas yükseldikçe ön kazancı otomatik kısar.",
                ) {
                    RzChip("etkin", true, onClick = {})
                }
                RzDivider()
                AdvancedRow(
                    "Hat bekçisi",
                    "Sessizlikte otomatik kapanmayı engeller.",
                ) {
                    RzToggle(
                        checked = state.guardRunning,
                        onChange = { state.setGuard(it) },
                    )
                }
                RzDivider()
                AdvancedRow(
                    "Derin ayarlar",
                    "Bekçi modu, aralık ve açıklamalar.",
                ) {
                    RzChip("aç", false, onClick = onOpenGuard)
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        BentoTile(Modifier.fillMaxWidth()) {
            Column {
                TileKicker("Hakkında")
                Spacer(Modifier.height(8.dp))
                Text(
                    "Rezofon, Grundig JAM+ gibi kendi DSP'si olmayan Bluetooth hoparlörler için " +
                        "telefon tarafında çalışan bir ses atölyesidir. Hiçbir veri toplamaz, hiçbir " +
                        "ağ bağlantısı kurmaz.",
                    style = MaterialTheme.typography.bodySmall,
                    color = p.textDim,
                )
                Spacer(Modifier.height(10.dp))
                MetricRow("Sürüm", "1.0.0 (1)")
                RzDivider()
                MetricRow("Paket", "studio.rezofon.app")
                RzDivider()
                MetricRow("Lisans", "MIT")
            }
        }
    }
}

@Composable
private fun AdvancedRow(
    title: String,
    caption: String,
    control: @Composable () -> Unit,
) {
    val p = LocalRzPalette.current
    Row(
        Modifier.fillMaxWidth().padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = p.text)
            Text(caption, style = MaterialTheme.typography.bodySmall, color = p.textDim)
        }
        Spacer(Modifier.width(10.dp))
        control()
    }
}

private fun Modifier.clickableRipple(onClick: () -> Unit): Modifier =
    this.then(Modifier.clickable(onClick = onClick))
