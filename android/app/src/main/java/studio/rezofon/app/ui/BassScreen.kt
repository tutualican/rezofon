package studio.rezofon.app.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import studio.rezofon.app.audio.formatDb
import studio.rezofon.app.audio.suggestedPreamp
import studio.rezofon.app.state.AppState
import studio.rezofon.app.ui.theme.LocalRzPalette
import kotlin.math.PI
import kotlin.math.sin

@Composable
fun BassScreen(state: AppState) {
    val p = LocalRzPalette.current
    val params = state.params

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp)
            .padding(bottom = 118.dp),
    ) {
        ScreenHeader(
            title = "Bas ve gövde",
            subtitle = "Pasif radyatör · punch · eşik",
            trailing = { MiniStat("${(params.bassBoost * 100).toInt()}%", "güç", p.amber) },
        )

        BentoTile(
            Modifier.fillMaxWidth(),
            tint = p.amber.copy(alpha = 0.10f),
            accent = p.violet.copy(alpha = 0.06f),
        ) {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                RadiatorVisual(
                    energy = (params.bassBoost * 0.7f + params.subShelfDb / 9f * 0.3f).coerceIn(0f, 1f),
                    active = params.enabled && params.bassEnabled,
                    accent = p.amber,
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    MiniStat(formatDb(params.subShelfDb), "sub eşiği", p.violet)
                    MiniStat("${(params.bassBoost * 100).toInt()}", "punch", p.amber)
                    MiniStat(formatDb(params.preampDb), "headroom", p.textDim)
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
                    Column {
                        TileKicker("Bas derinliği")
                        Text("Derinlik", style = MaterialTheme.typography.titleLarge, color = p.text)
                    }
                    RzToggle(
                        checked = params.bassEnabled,
                        onChange = {
                            state.commit(
                                params.copyWith(
                                    bassEnabled = it,
                                    preampDb = suggestedPreamp(params.gainsDb, params.subShelfDb, params.bassBoost),
                                ),
                            )
                        },
                    )
                }
                Spacer(Modifier.height(6.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Knob(
                        value = params.bassBoost,
                        onChange = {
                            state.commit(
                                params.copyWith(
                                    bassBoost = it,
                                    preampDb = suggestedPreamp(params.gainsDb, params.subShelfDb, it),
                                ),
                            )
                        },
                        valueText = "${(params.bassBoost * 100).toInt()}",
                        label = "punch",
                        accent = p.amber,
                        size = 132.dp,
                    )
                    Spacer(Modifier.size(18.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            "Pasif radyatör hareketini artırır. 100 Hz altında sürücü sınırlı olduğu için " +
                                "etki daha çok 100–250 Hz gövde olarak duyulur.",
                            style = MaterialTheme.typography.bodySmall,
                            color = p.textDim,
                        )
                        Spacer(Modifier.height(10.dp))
                        Text(
                            "Önerilen üst sınır: %78",
                            style = MaterialTheme.typography.labelSmall,
                            color = p.amber.copy(alpha = 0.9f),
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
                    Column {
                        TileKicker("Sub eşiği")
                        Text("Düşük frekans rafı", style = MaterialTheme.typography.titleLarge, color = p.text)
                    }
                    Text(formatDb(params.subShelfDb), style = MaterialTheme.typography.titleLarge, color = p.violet)
                }
                Spacer(Modifier.height(8.dp))
                RzBar(
                    value = params.subShelfDb,
                    min = 0f,
                    max = 9f,
                    accent = p.violet,
                    onChange = {
                        state.commit(
                            params.copyWith(
                                subShelfDb = it,
                                preampDb = suggestedPreamp(params.gainsDb, it, params.bassBoost),
                            ),
                        )
                    },
                )
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("0 dB · temiz", style = MaterialTheme.typography.labelSmall, color = p.textDim)
                    Text("+9 dB · maksimum", style = MaterialTheme.typography.labelSmall, color = p.textDim)
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
                    Column(Modifier.weight(1f)) {
                        TileKicker("Gece yüksekliği")
                        Text("Algısal eşitleme", style = MaterialTheme.typography.titleLarge, color = p.text)
                    }
                    Text("${(params.loudness * 100).toInt()}%", style = MaterialTheme.typography.titleLarge, color = p.cyan)
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    "Düşük seste bas ve tiz algısını dengeler. Yüksek seste kapalı tutmak daha temiz sonuç verir.",
                    style = MaterialTheme.typography.bodySmall,
                    color = p.textDim,
                )
                Spacer(Modifier.height(8.dp))
                RzBar(
                    value = params.loudness,
                    accent = p.cyan,
                    onChange = { state.commit(params.copyWith(loudness = it)) },
                )
            }
        }

        Spacer(Modifier.height(12.dp))
        InfoNote(
            "Kırpma koruması: bas ve sub yükseldikçe ön kazanç otomatik düşürülür. " +
                "Duyduğunuz ses kısık geliyorsa preamp'i Ekolayzer ekranından elle yükseltebilirsiniz — " +
                "ama 3.5 W sürücüde distorsiyon riski artar.",
            accent = p.amber,
        )
    }
}

@Composable
private fun RadiatorVisual(energy: Float, active: Boolean, accent: androidx.compose.ui.graphics.Color) {
    val p = LocalRzPalette.current
    val transition = rememberInfiniteTransition(label = "rad")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(1200, easing = LinearEasing), RepeatMode.Restart),
        label = "radT",
    )
    Box(Modifier.fillMaxWidth().height(172.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val base = 52.dp.toPx()
            val amp = if (active) energy else 0.05f
            for (ring in 0..3) {
                val phase = sin(t + ring * 0.8f)
                val r = base + ring * 17.dp.toPx() + phase * (6.dp.toPx() * amp)
                drawCircle(
                    color = accent.copy(alpha = (0.45f - ring * 0.09f) * (0.4f + amp)),
                    radius = r,
                    center = androidx.compose.ui.geometry.Offset(cx, cy),
                    style = Stroke(width = 1.4.dp.toPx()),
                )
            }
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(accent.copy(alpha = 0.55f * (0.35f + amp)), accent.copy(alpha = 0.05f)),
                    center = androidx.compose.ui.geometry.Offset(cx, cy),
                    radius = base * 1.4f,
                ),
                radius = base,
                center = androidx.compose.ui.geometry.Offset(cx, cy),
            )
            drawCircle(color = p.bg.copy(alpha = 0.55f), radius = base * 0.42f, center = androidx.compose.ui.geometry.Offset(cx, cy))
            drawCircle(
                color = accent.copy(alpha = 0.9f),
                radius = 4.dp.toPx() + 2.dp.toPx() * amp,
                center = androidx.compose.ui.geometry.Offset(cx, cy),
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(96.dp))
            Text(
                if (active) "RADYATÖR AKTİF" else "RADYATÖR BEKLEMEDE",
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 2.sp),
                color = p.textDim,
            )
        }
    }
}
