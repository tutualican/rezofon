package studio.rezofon.app.audio

import android.content.Context
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.media.audiofx.Virtualizer
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlin.math.roundToInt

/**
 * Thin, honest wrapper around Android's audio effect framework.
 *
 * Android does not give third-party apps a hook into a Bluetooth speaker's own
 * DSP, so Rezofon processes the stream on the phone side. That means two
 * modes:
 *  - Oturum modu: effects attach to Rezofon's own player session. Always
 *    works, fully verifiable.
 *  - Sistem modu: effects attach to output session 0. Some OEM builds allow it,
 *    some do not; the result is reported instead of being faked.
 */
class SoundEngine(private val context: Context) {

    var status: String by mutableStateOf("Motor kapalı")
        private set
    var systemModeWorking: Boolean by mutableStateOf(false)
        private set
    var attachedSession: Int = -1
        private set
    var deviceBands: Int = 0
        private set
    var deviceBandRange: String = "—"
        private set
    var effectChain: String by mutableStateOf("—")
        private set
    var lastError: String? by mutableStateOf(null)
        private set

    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null
    private var loudness: LoudnessEnhancer? = null
    private var pending: AudioParams = AudioParams()

    fun attach(sessionId: Int, params: AudioParams): Boolean {
        if (sessionId == attachedSession && equalizer != null) {
            apply(params)
            return true
        }
        release()
        pending = params
        attachedSession = sessionId
        val chain = mutableListOf<String>()
        lastError = null

        equalizer = runCatching {
            Equalizer(0, sessionId).also { eq ->
                eq.bandLevelRange
                deviceBands = eq.numberOfBands.toInt()
                val lo = eq.bandLevelRange[0] / 100f
                val hi = eq.bandLevelRange[1] / 100f
                deviceBandRange = "${lo.roundToInt()} … +${hi.roundToInt()} dB"
                eq.enabled = params.enabled
                chain += "10-bant EQ"
            }
        }.onFailure { lastError = "EQ: ${it.message}" }.getOrNull()

        bassBoost = runCatching {
            BassBoost(0, sessionId).also { chain += "Bas güçlendirici" }
        }.onFailure { lastError = "BassBoost: ${it.message}" }.getOrNull()

        virtualizer = runCatching {
            Virtualizer(0, sessionId).also { chain += "Genişletici" }
        }.onFailure { lastError = "Virtualizer: ${it.message}" }.getOrNull()

        loudness = runCatching {
            LoudnessEnhancer(sessionId).also { chain += "Algısal yükseklik" }
        }.onFailure { lastError = "Loudness: ${it.message}" }.getOrNull()

        effectChain = if (chain.isEmpty()) "etki yok" else chain.joinToString(" · ")
        status = if (equalizer != null) {
            if (sessionId == 0) "Sistem çıkışına bağlı" else "Oturuma bağlı (#$sessionId)"
        } else {
            "Etkiler açılamadı"
        }
        apply(params)
        return equalizer != null
    }

    fun attachSystem(params: AudioParams): Boolean {
        // Android 10+ restricts global sessions; try, then report the truth.
        val ok = attach(0, params)
        systemModeWorking = ok && equalizer != null
        if (!ok) status = "Sistem modu bu cihazda kapalı"
        return ok
    }

    fun apply(params: AudioParams) {
        pending = params
        val eq = equalizer ?: return
        runCatching {
            eq.enabled = params.enabled
            val minMb = eq.bandLevelRange[0].toInt()
            val maxMb = eq.bandLevelRange[1].toInt()
            for (b in 0 until eq.numberOfBands.toInt()) {
                val centerHz = eq.getCenterFreq(b.toShort()) / 1000f
                var targetDb = interpolateGain(params.gainsDb, centerHz)
                // A gentle low shelf mirrors the Sub Eşiği control on the device bands.
                if (centerHz < 200f) {
                    targetDb += params.subShelfDb * (1f - centerHz / 200f) * 0.7f
                }
                val mb = (targetDb * 100).roundToInt().coerceIn(minMb, maxMb)
                eq.setBandLevel(b.toShort(), mb.toShort())
            }
        }.onFailure { lastError = "EQ apply: ${it.message}" }

        runCatching {
            bassBoost?.let { bb ->
                bb.enabled = params.enabled && params.bassEnabled && params.bassBoost > 0.01f
                if (bb.strengthSupported) bb.setStrength((params.bassBoost * 1000).roundToInt().toShort())
            }
        }.onFailure { lastError = "BassBoost apply: ${it.message}" }

        runCatching {
            virtualizer?.let { v ->
                v.enabled = params.enabled && params.virtualizer > 0.01f
                if (v.strengthSupported) v.setStrength((params.virtualizer * 1000).roundToInt().toShort())
            }
        }.onFailure { lastError = "Virtualizer apply: ${it.message}" }

        runCatching {
            loudness?.let { l ->
                val gainMb = (params.loudness * 1200).roundToInt()
                l.setTargetGain(gainMb)
                l.enabled = params.enabled && gainMb > 50
            }
        }.onFailure { lastError = "Loudness apply: ${it.message}" }
    }

    val currentParams: AudioParams get() = pending

    fun release() {
        runCatching { equalizer?.release() }
        runCatching { bassBoost?.release() }
        runCatching { virtualizer?.release() }
        runCatching { loudness?.release() }
        equalizer = null
        bassBoost = null
        virtualizer = null
        loudness = null
        attachedSession = -1
        systemModeWorking = false
        status = "Motor kapalı"
        effectChain = "—"
    }

    fun sessionModeOnly() {
        release()
        status = "Yalnızca test sinyali"
        effectChain = "test sinyali oturumu"
    }

    fun diagnostics(): List<Pair<String, String>> = listOf(
        "Oturum" to if (attachedSession >= 0) "#$attachedSession" else "bağlı değil",
        "Cihaz bant sayısı" to if (deviceBands > 0) "$deviceBands" else "—",
        "Cihaz bant aralığı" to deviceBandRange,
        "Etki zinciri" to effectChain,
        "Sistem modu" to if (systemModeWorking) "erişilebilir" else "kısıtlı",
        "Son hata" to (lastError ?: "yok"),
    )
}
