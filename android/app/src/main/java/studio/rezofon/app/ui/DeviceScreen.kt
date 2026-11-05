package studio.rezofon.app.ui

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import studio.rezofon.app.state.AppState
import studio.rezofon.app.ui.theme.LocalRzPalette

@Composable
fun DeviceScreen(state: AppState, onOpenGuard: () -> Unit) {
    val p = LocalRzPalette.current
    val snap = state.snapshot

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp)
            .padding(bottom = 118.dp),
    ) {
        ScreenHeader(
            title = "Cihaz",
            subtitle = "Bağlantı, kodek ve sürücü",
            trailing = {
                IconPill(Icons.Rounded.Refresh, "Yeniden bağla", onClick = {
                    state.reconnectDevice()
                }, bg = p.surfaceAlt, tint = p.lime)
            },
        )

        BentoTile(Modifier.fillMaxWidth(), tint = p.lime.copy(alpha = 0.09f)) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(58.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (snap.a2dpConnected) p.lime else p.surfaceSunken),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Rounded.Bluetooth,
                            null,
                            tint = if (snap.a2dpConnected) Color(0xFF14160F) else p.textDim,
                            modifier = Modifier.size(28.dp),
                        )
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(snap.name, style = MaterialTheme.typography.titleLarge, color = p.text)
                        Text(
                            when {
                                snap.a2dpConnected -> "Stereo profil aktif · medya hazır"
                                snap.hfpConnected -> "Yalnızca görüşme profili"
                                snap.enabled -> "Bağlı değil · eşleşmiş olabilir"
                                else -> "Bluetooth kapalı"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = p.textDim,
                        )
                    }
                }
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    RzChip("A2DP", snap.a2dpConnected, onClick = {})
                    RzChip("HFP", snap.hfpConnected, onClick = {})
                    RzChip("AVRCP", snap.avrcpConnected, onClick = {})
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        BentoTile(Modifier.fillMaxWidth()) {
            Column {
                TileKicker("Kodek")
                Spacer(Modifier.height(6.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(snap.codec, style = MaterialTheme.typography.headlineMedium, color = p.lime)
                    Text(snap.codecDetail, style = MaterialTheme.typography.bodySmall, color = p.textDim)
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    "Kodek seçimi Android Bluetooth yığını tarafından yapılır. A2DP kaynağı SBC/AAC ise " +
                        "JAM+ gelen akışı doğrudan yükseltir; cihazın kendi DSP'si yoktur.",
                    style = MaterialTheme.typography.bodySmall,
                    color = p.textDim,
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        BentoTile(Modifier.fillMaxWidth()) {
            Column {
                TileKicker("Bağlantı defteri")
                Spacer(Modifier.height(4.dp))
                MetricRow("Adres", snap.address.ifBlank { "—" })
                RzDivider()
                MetricRow("Eşleşmiş", if (snap.bonded) "evet" else "hayır")
                RzDivider()
                MetricRow("İzin", if (snap.permissionGranted) "verildi" else "bekliyor", valueColor = if (snap.permissionGranted) p.lime else p.amber)
                RzDivider()
                snap.audioProps.forEach { (k, v) ->
                    MetricRow(k, v)
                    RzDivider()
                }
                MetricRow("Android", "API ${snap.sdk}")
            }
        }

        if (snap.hint.isNotBlank()) {
            Spacer(Modifier.height(12.dp))
            InfoNote(snap.hint, accent = p.amber)
        }

        Spacer(Modifier.height(12.dp))

        BentoTile(Modifier.fillMaxWidth(), onClick = onOpenGuard) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(15.dp))
                        .background(p.cyan.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.Timer, null, tint = p.cyan, modifier = Modifier.size(22.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("15 dakika uyku sayacı", style = MaterialTheme.typography.titleLarge, color = p.text)
                    Text(
                        "Hat bekçisi ile sessizlikte otomatik kapanmayı engelle",
                        style = MaterialTheme.typography.bodySmall,
                        color = p.textDim,
                    )
                }
                Text("→", style = MaterialTheme.typography.titleLarge, color = p.cyan)
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
                    TileKicker("Sürücü ve ürün yazılımı")
                    Text(
                        "bilgi",
                        style = MaterialTheme.typography.labelSmall,
                        color = p.textDim,
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "Grundig JAM+ için herkese açık bir OTA/firmware kanalı bulunmuyor; üretici bu model için " +
                        "mobil uygulama desteğini de kaldırdı. Bu yüzden Rezofon hiçbir \"güncelleme\" vaadi vermez — " +
                        "bunun yerine telefon tarafındaki ses işleme zincirini, kodek bilgisini ve bağlantı " +
                        "sağlığını ölçüp raporlar.",
                    style = MaterialTheme.typography.bodySmall,
                    color = p.textDim,
                )
                Spacer(Modifier.height(10.dp))
                MetricRow("Sürücü katmanı", "Android AudioEffect")
                RzDivider()
                MetricRow("EQ motoru", state.engine.status)
                RzDivider()
                MetricRow("Etki zinciri", state.engine.effectChain)
                RzDivider()
                MetricRow("Cihaz bantları", "${state.engine.deviceBands} · ${state.engine.deviceBandRange}")
            }
        }
    }
}
