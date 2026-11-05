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
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import studio.rezofon.app.state.AppState
import studio.rezofon.app.state.SavedProfile
import studio.rezofon.app.ui.theme.LocalRzPalette

@Composable
fun SettingsScreen(state: AppState, onBack: () -> Unit) {
    val p = LocalRzPalette.current
    var exportOpen by remember { mutableStateOf<String?>(null) }
    var importOpen by remember { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp)
            .padding(bottom = 40.dp),
    ) {
        ScreenHeader(
            title = "Ayarlar",
            subtitle = "Profil, tema ve paylaşım",
            onBack = onBack,
        )

        BentoTile(Modifier.fillMaxWidth()) {
            Column {
                TileKicker("Görünüm")
                Spacer(Modifier.height(10.dp))
                SettingSwitch(
                    "Koyu tema",
                    "Gece kullanımı için önerilir",
                    state.darkMode,
                ) { state.toggleDark(it) }
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
                        TileKicker("Profiller")
                        Text("${state.profileList.size} kayıt", style = MaterialTheme.typography.titleLarge, color = p.text)
                    }
                    RzChip("İçe al", false, onClick = { importOpen = true })
                }
                Spacer(Modifier.height(8.dp))
                if (state.profileList.isEmpty()) {
                    Text(
                        "Henüz profil yok. Ekolayzer ekranından eğrinizi kaydedin.",
                        style = MaterialTheme.typography.bodySmall,
                        color = p.textDim,
                    )
                } else {
                    state.profileList.forEachIndexed { i, profile ->
                        ProfileRow(
                            profile = profile,
                            onLoad = { state.loadProfile(profile); onBack() },
                            onDelete = { state.deleteProfile(profile.id) },
                            onExport = { exportOpen = state.exportJson() },
                        )
                        if (i < state.profileList.size - 1) RzDivider()
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        BentoTile(Modifier.fillMaxWidth()) {
            Column {
                TileKicker("Veri")
                Spacer(Modifier.height(8.dp))
                Text(
                    "Rezofon hiçbir veri toplamaz. Profiller yalnızca cihazda saklanır ve JSON olarak " +
                        "dışa aktarılabilir; masaüstü sürümü de aynı biçimi okur.",
                    style = MaterialTheme.typography.bodySmall,
                    color = p.textDim,
                )
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    RzChip("JSON dışa aktar", false, onClick = { exportOpen = state.exportJson() })
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        BentoTile(Modifier.fillMaxWidth(), tint = p.amber.copy(alpha = 0.06f)) {
            Column {
                TileKicker("Bilinen sınırlar")
                Spacer(Modifier.height(6.dp))
                listOf(
                    "JAM+ 3.5 W RMS mono sürücü ve pasif radyatör kullanır; 100 Hz altı fiziksel olarak sınırlıdır.",
                    "Hoparlörün kendi DSP'si yok; tüm işleme telefon tarafında yapılır.",
                    "Global (sistem geneli) etki bazı Android sürümlerinde kısıtlıdır.",
                    "Bekçi servisi agresif pil optimizasyonundan etkilenebilir.",
                ).forEach {
                    Row(Modifier.padding(vertical = 3.dp)) {
                        Text("·", color = p.amber, style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.width(7.dp))
                        Text(it, style = MaterialTheme.typography.bodySmall, color = p.textDim)
                    }
                }
            }
        }
    }

    if (exportOpen != null) {
        TextDialog(
            title = "Profil JSON",
            body = exportOpen ?: "",
            onDismiss = { exportOpen = null },
        )
    }
    if (importOpen) {
        var raw by remember { mutableStateOf("") }
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { importOpen = false },
            containerColor = p.surface,
            shape = RoundedCornerShape(28.dp),
            title = { Text("JSON içe al", style = MaterialTheme.typography.titleLarge, color = p.text) },
            text = {
                androidx.compose.material3.OutlinedTextField(
                    value = raw,
                    onValueChange = { raw = it },
                    minLines = 4,
                    shape = RoundedCornerShape(16.dp),
                    placeholder = { Text("{\"gains\":[...]}", color = p.textDim) },
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    state.importJson(raw)
                    importOpen = false
                }) { Text("Uygula", color = p.lime) }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { importOpen = false }) {
                    Text("Vazgeç", color = p.textDim)
                }
            },
        )
    }
}

@Composable
private fun SettingSwitch(
    title: String,
    caption: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
) {
    val p = LocalRzPalette.current
    Row(
        Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = p.text)
            Text(caption, style = MaterialTheme.typography.bodySmall, color = p.textDim)
        }
        RzToggle(checked, onChange)
    }
}

@Composable
private fun ProfileRow(
    profile: SavedProfile,
    onLoad: () -> Unit,
    onDelete: () -> Unit,
    onExport: () -> Unit,
) {
    val p = LocalRzPalette.current
    Row(
        Modifier.fillMaxWidth().padding(vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(p.lime.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                profile.name.take(1).uppercase(),
                style = MaterialTheme.typography.labelLarge,
                color = p.lime,
            )
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(profile.name, style = MaterialTheme.typography.titleMedium, color = p.text)
            Text(
                "punch ${(profile.bass * 100).toInt()} · sub ${"%.1f".format(profile.sub)} dB",
                style = MaterialTheme.typography.bodySmall,
                color = p.textDim,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            RzChip("Yükle", false, onClick = onLoad)
            RzChip("Sil", false, onClick = onDelete)
        }
    }
}

@Composable
private fun TextDialog(title: String, body: String, onDismiss: () -> Unit) {
    val p = LocalRzPalette.current
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = p.surface,
        shape = RoundedCornerShape(28.dp),
        title = { Text(title, style = MaterialTheme.typography.titleLarge, color = p.text) },
        text = {
            Text(
                body,
                style = MaterialTheme.typography.bodySmall,
                color = p.textDim,
                modifier = Modifier
                    .height(220.dp)
                    .verticalScroll(rememberScrollState()),
            )
        },
        confirmButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) {
                Text("Kapat", color = p.lime)
            }
        },
    )
}
