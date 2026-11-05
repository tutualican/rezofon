package studio.rezofon.app.audio

import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.pow

/** The ten bands Rezofon exposes. Frequencies in Hz. */
val RzBandFrequencies = floatArrayOf(31f, 62f, 125f, 250f, 500f, 1000f, 2000f, 4000f, 8000f, 16000f)

val RzBandLabels = arrayOf("31", "62", "125", "250", "500", "1k", "2k", "4k", "8k", "16k")

/** Everything the sound engine needs to know, decoupled from the UI. */
data class AudioParams(
    val enabled: Boolean = true,
    val gainsDb: FloatArray = FloatArray(10),
    val bassBoost: Float = 0.35f,
    val bassEnabled: Boolean = true,
    val subShelfDb: Float = 3f,
    val loudness: Float = 0.25f,
    val virtualizer: Float = 0f,
    val preampDb: Float = 0f,
) {
    fun copyWith(
        enabled: Boolean = this.enabled,
        gainsDb: FloatArray = this.gainsDb,
        bassBoost: Float = this.bassBoost,
        bassEnabled: Boolean = this.bassEnabled,
        subShelfDb: Float = this.subShelfDb,
        loudness: Float = this.loudness,
        virtualizer: Float = this.virtualizer,
        preampDb: Float = this.preampDb,
    ) = AudioParams(enabled, gainsDb, bassBoost, bassEnabled, subShelfDb, loudness, virtualizer, preampDb)
}

/**
 * Interpolates the user's ten-band curve at an arbitrary frequency.
 * Interpolation happens in log-frequency space, which matches how the ear
 * (and the speaker's passive radiator) behaves far better than linear space.
 */
fun interpolateGain(gainsDb: FloatArray, freqHz: Float): Float {
    if (freqHz <= RzBandFrequencies.first()) return gainsDb.first()
    if (freqHz >= RzBandFrequencies.last()) return gainsDb.last()
    var i = 0
    while (i < RzBandFrequencies.size - 2 && freqHz > RzBandFrequencies[i + 1]) i++
    val f0 = RzBandFrequencies[i]
    val f1 = RzBandFrequencies[i + 1]
    val t = (log10(freqHz.toDouble()) - log10(f0.toDouble())) /
        (log10(f1.toDouble()) - log10(f0.toDouble()))
    return (gainsDb[i] + (gainsDb[i + 1] - gainsDb[i]) * t).toFloat()
}

/** Approximate shape of the JAM+'s bass lift, used only for the on-screen curve preview. */
fun previewGainAt(gainsDb: FloatArray, freqHz: Float, subShelfDb: Float, bass: Float): Float {
    val base = interpolateGain(gainsDb, freqHz)
    val shelf = subShelfDb * (1f / (1f + (freqHz / 95f).pow(2.4f)))
    val punch = bass * 6f * (1f / (1f + (freqHz / 170f).pow(2.0f)))
    return base + shelf + punch
}

/**
 * Cheap headroom estimator. Boosting several bands at once costs level, so the
 * engine trims the pre-amp instead of letting the small 3.5 W amp clip.
 */
fun suggestedPreamp(gainsDb: FloatArray, subShelfDb: Float, bass: Float): Float {
    val peak = gainsDb.maxOrNull() ?: 0f
    val extra = subShelfDb * 0.8f + bass * 5f
    val total = peak + extra
    return if (total <= 3f) 0f else -(total - 3f).coerceAtMost(9f)
}

fun formatDb(v: Float): String {
    val rounded = (v * 10).toInt() / 10f
    return when {
        abs(rounded) < 0.05f -> "0.0 dB"
        rounded > 0 -> "+${rounded} dB"
        else -> "$rounded dB"
    }
}

data class EqPreset(
    val id: String,
    val name: String,
    val tagline: String,
    val gainsDb: FloatArray,
    val bass: Float,
    val sub: Float,
    val loudness: Float,
)

/**
 * Presets tuned for a 3.5 W mono driver with a passive radiator and a usable
 * range from roughly 100 Hz to 16 kHz. Bands below 100 Hz are shaping hints:
 * the driver cannot reproduce them, but the shelf still adds body and the
 * pre-amp keeps the amp out of clipping.
 */
val RzPresets: List<EqPreset> = listOf(
    EqPreset(
        "jam_balance", "JAM+ Dengesi", "Fabrika dengesi, hafif canlılık",
        floatArrayOf(4.5f, 3.5f, 2f, 0.5f, -0.5f, 0f, 0.5f, 1f, 1.5f, 2f),
        bass = 0.30f, sub = 3f, loudness = 0.20f,
    ),
    EqPreset(
        "deep_bass", "Derin Bas", "Pasif radyatörü uyandır",
        floatArrayOf(7.5f, 6.5f, 4.5f, 1.5f, -1.5f, -1f, 0f, 1f, 1.5f, 2f),
        bass = 0.72f, sub = 7f, loudness = 0.35f,
    ),
    EqPreset(
        "clean_punch", "Temiz Vuruş", "Bas var, bulanıklık yok",
        floatArrayOf(2f, 4.5f, 4f, 0f, -2f, -1f, 0.5f, 2f, 2.5f, 2f),
        bass = 0.55f, sub = 2f, loudness = 0.25f,
    ),
    EqPreset(
        "vocal_front", "Vokal Önde", "Podcast ve sözlü içerik",
        floatArrayOf(-1f, -1.5f, -1f, 0.5f, 1.5f, 2.5f, 3f, 2f, 0.5f, -0.5f),
        bass = 0.10f, sub = 0f, loudness = 0.15f,
    ),
    EqPreset(
        "open_air", "Açık Hava", "Bahçe ve balkon için parlaklık",
        floatArrayOf(3f, 3f, 1.5f, 0f, 0.5f, 1f, 2.5f, 3.5f, 4f, 3.5f),
        bass = 0.45f, sub = 4f, loudness = 0.30f,
    ),
    EqPreset(
        "night", "Gece Modu", "Düşük seansta denge",
        floatArrayOf(6f, 5f, 3f, 1f, 0.5f, 1f, 1.5f, 1f, 0f, -2f),
        bass = 0.40f, sub = 5f, loudness = 0.75f,
    ),
    EqPreset(
        "electronic", "Elektronik", "Kick ve bas hattı",
        floatArrayOf(6f, 5.5f, 3f, -0.5f, -1.5f, 0.5f, 1.5f, 2.5f, 3f, 2.5f),
        bass = 0.65f, sub = 6f, loudness = 0.30f,
    ),
    EqPreset(
        "rock", "Rock", "Gitar gövdesi ve davul",
        floatArrayOf(3f, 4f, 3f, 1f, -1f, -0.5f, 1.5f, 3f, 3.5f, 2.5f),
        bass = 0.45f, sub = 3f, loudness = 0.25f,
    ),
)

val FlatGains = FloatArray(10)
