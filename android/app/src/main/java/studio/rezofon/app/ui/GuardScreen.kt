package studio.rezofon.app.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import studio.rezofon.app.state.AppState
import studio.rezofon.app.ui.theme.LocalRzPalette

@Composable
fun GuardScreen(state: AppState, onBack: () -> Unit) {
    val p = LocalRzPalette.current
    val prefs = state.prefs
    var elapsed by remember { mutableIntStateOf(0) }

    LaunchedEffect(state.guardRunning) {
        elapsed = 0
        while (state.guardRunning) {
            delay(1000)
            elapsed += 1
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp)
            .padding(bottom = 40.dp),
    ) {
        ScreenHeader(
            title = "Hat bekçisi",
            subtitle = "Otomatik kapanmaya karşı",
            onBack = onBack,
            trailing = {
                RzToggle(
                    checked = state.guardRunning,
                    onChange = { state.setGuard(it) },
                )
            },
        )

        BentoTile(
            Modifier.fillMaxWidth(),
            tint = if (state.guardRunning) p.cyan.copy(alpha = 0.12f) else p.surface,
        ) {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(210.dp), contentAlignment = Alignment.Center) {
                    GuardDial(
                        running = state.guardRunning,
                        intervalSeconds = prefs.guardInterval,
                        pulse = prefs.guardPulse,
                        accent = p.cyan,
                    )
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            if (state.guardRunning) formatElapsed(elapsed) else "15:00",
                            style = MaterialTheme.typography.displayMedium,
                            color = p.text,
                        )
                        Text(
                            if (state.guardRunning) "hat açık" else "hoparlör uyku penceresi",
                            style = MaterialTheme.typography.labelSmall,
                            color = p.textDim,
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    if (state.guardRunning) {
                        "Sessiz medya akışı gönderiliyor · ${if (prefs.guardPulse) "nabız" else "sürekli"} mod"
                    } else {
                        "Kapalıyken JAM+ 15 dakika sessizlikte uykuya geçer"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = p.textDim,
                )
                Spacer(Modifier.height(14.dp))
                GuardButton(
                    running = state.guardRunning,
                    onToggle = { state.setGuard(!state.guardRunning) },
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        BentoTile(Modifier.fillMaxWidth()) {
            Column {
                TileKicker("Mod")
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    RzChip(
                        text = "Nabız",
                        selected = prefs.guardPulse,
                        onClick = { state.updateGuard(pulse = true, interval = prefs.guardInterval) },
                    )
                    RzChip(
                        text = "Sürekli",
                        selected = !prefs.guardPulse,
                        onClick = { state.updateGuard(pulse = false, interval = prefs.guardInterval) },
                    )
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    if (prefs.guardPulse) {
                        "Nabız modu: hat kısa aralıklarla tazelenir. Pil tüketimi düşük, çoğu yazılımda yeterli."
                    } else {
                        "Sürekli mod: hat hiç kapanmaz. En güvenilir seçenek, pil tüketimi biraz daha yüksek."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = p.textDim,
                )
                Spacer(Modifier.height(14.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    TileKicker("Tazeleme aralığı")
                    Text("${prefs.guardInterval} sn", style = MaterialTheme.typography.labelLarge, color = p.cyan)
                }
                RzBar(
                    value = prefs.guardInterval.toFloat(),
                    min = 5f,
                    max = 120f,
                    accent = p.cyan,
                    onChange = { state.updateGuard(prefs.guardPulse, it.toInt()) },
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("5 sn", style = MaterialTheme.typography.labelSmall, color = p.textDim)
                    Text("120 sn", style = MaterialTheme.typography.labelSmall, color = p.textDim)
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        InfoNote(
            "Nasıl çalışır: JAM+ kendi uyku sayacını gelen ses akışına göre tutar. Rezofon, ses çıkarmayan " +
                "bir medya akışını açık tutarak sayacın sıfırlanmasını sağlar. Hoparlörün ürün yazılımına " +
                "müdahale edilmez, bu yüzden cihaz garantisi ve pil sağlığı etkilenmez.",
        )

        Spacer(Modifier.height(10.dp))
        InfoNote(
            "Not: Bu bir arka plan servisidir. Bildirim izni verilmezse servis çalışır ama durum bildirimi " +
                "görünmez. Pil optimizasyonu agresif olan üreticilerde servisi istisnaya eklemeniz gerekebilir.",
            accent = p.amber,
        )
    }
}

private fun formatElapsed(sec: Int): String {
    val h = sec / 3600
    val m = (sec % 3600) / 60
    val s = sec % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
}

@Composable
private fun GuardDial(running: Boolean, intervalSeconds: Int, pulse: Boolean, accent: Color) {
    val p = LocalRzPalette.current
    val transition = rememberInfiniteTransition(label = "guard")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(if (pulse) intervalSeconds * 1000 else 6000, easing = LinearEasing),
        ),
        label = "guardT",
    )
    Canvas(Modifier.fillMaxSize()) {
        val stroke = 10.dp.toPx()
        val inset = stroke / 2 + 6.dp.toPx()
        val arcSize = androidx.compose.ui.geometry.Size(
            size.width - inset * 2,
            size.height - inset * 2,
        )
        val topLeft = Offset(inset, inset)
        drawArc(
            color = p.surfaceSunken,
            startAngle = -90f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(stroke, cap = StrokeCap.Round),
        )
        if (running) {
            drawArc(
                brush = Brush.sweepGradient(listOf(accent.copy(alpha = 0.25f), accent, accent.copy(alpha = 0.25f))),
                startAngle = -90f,
                sweepAngle = 360f * t,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(stroke, cap = StrokeCap.Round),
            )
            val angle = Math.toRadians((-90f + 360f * t).toDouble())
            val r = arcSize.width / 2f - stroke / 2f
            val cx = topLeft.x + arcSize.width / 2f
            val cy = topLeft.y + arcSize.height / 2f
            drawCircle(
                color = accent,
                radius = 6.dp.toPx(),
                center = Offset(
                    (cx + r * kotlin.math.cos(angle)).toFloat(),
                    (cy + r * kotlin.math.sin(angle)).toFloat(),
                ),
            )
        }
    }
}

@Composable
private fun GuardButton(running: Boolean, onToggle: () -> Unit) {
    val p = LocalRzPalette.current
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(if (running) p.surfaceAlt else p.lime)
            .then(pressBounce())
            .clickableRipple(onToggle)
            .padding(vertical = 15.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            if (running) "Bekçiyi durdur" else "Bekçiyi başlat",
            style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 1.sp),
            color = if (running) p.text else Color(0xFF14160F),
        )
    }
}

private fun Modifier.clickableRipple(onClick: () -> Unit): Modifier =
    this.then(Modifier.clickable(onClick = onClick))
