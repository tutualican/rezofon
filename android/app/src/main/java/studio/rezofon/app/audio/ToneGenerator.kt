package studio.rezofon.app.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlin.math.PI
import kotlin.math.log2
import kotlin.math.pow
import kotlin.math.sin

enum class ToneKind(val label: String) {
    Sweep("Log tarama"),
    Low("Derin bas"),
    Mid("Referans 1k"),
}

/**
 * Test signal generator. Not a music player: it exists so the user can hear
 * whether the EQ curve is actually being applied on the current output route.
 * It owns its own audio session, which is the session the engine attaches to
 * when the system-wide session is locked down by the OEM.
 */
class ToneGenerator {

    var running by mutableStateOf(false)
        private set
    var kind by mutableStateOf(ToneKind.Sweep)
        private set
    var sessionId by mutableStateOf(0)
        private set

    private var track: AudioTrack? = null
    private var worker: Thread? = null

    fun start(kind: ToneKind, onSession: (Int) -> Unit) {
        stop()
        this.kind = kind
        val sampleRate = 48_000
        val minBuf = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_STEREO,
            AudioFormat.ENCODING_PCM_16BIT,
        ).coerceAtLeast(8192)

        val t = runCatching {
            AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build(),
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                        .build(),
                )
                .setBufferSizeInBytes(minBuf * 2)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()
        }.getOrNull() ?: return

        track = t
        sessionId = t.audioSessionId
        running = true
        onSession(t.audioSessionId)

        worker = Thread({
            runCatching { t.play() }
            val buf = ShortArray(minBuf / 2)
            var phase = 0.0
            var n = 0L
            val total = sampleRate * 12
            while (running) {
                val freq = when (kind) {
                    ToneKind.Sweep -> sweepFreq(n, total)
                    ToneKind.Low -> 62.0
                    ToneKind.Mid -> 1000.0
                }
                for (i in buf.indices step 2) {
                    phase += 2.0 * PI * freq / sampleRate
                    if (phase > 2.0 * PI) phase -= 2.0 * PI
                    val ramp = fade(n + i, total)
                    val v = (sin(phase) * 0.14 * ramp * Short.MAX_VALUE).toInt()
                        .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                        .toShort()
                    buf[i] = v
                    buf[i + 1] = v
                }
                n += buf.size
                if (n >= total) n = 0
                runCatching { t.write(buf, 0, buf.size) }
            }
            runCatching { t.stop() }
            runCatching { t.release() }
        }, "rezofon-tone").apply { isDaemon = true; start() }
    }

    private fun sweepFreq(n: Long, total: Int): Double {
        val t = (n % total).toDouble() / total
        val f0 = 40.0
        val f1 = 16_000.0
        return f0 * (f1 / f0).pow(t)
    }

    private fun fade(n: Long, total: Int): Double {
        val pos = n % total
        val edge = 1200.0
        val a = (pos / edge).coerceAtMost(1.0)
        val b = ((total - pos) / edge).coerceAtMost(1.0)
        return minOf(a, b)
    }

    fun stop() {
        running = false
        worker?.interrupt()
        worker = null
        runCatching { track?.pause() }
        runCatching { track?.flush() }
        runCatching { track?.release() }
        track = null
        sessionId = 0
    }

    fun describeOctaveSpan(): String {
        // Purely informational: what the sweep covers.
        return "40 Hz → 16 kHz · ${"%.1f".format(log2(16_000.0 / 40.0))} oktav"
    }
}
