package studio.rezofon.app.state

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import studio.rezofon.app.audio.AudioParams
import studio.rezofon.app.audio.BtSnapshot
import studio.rezofon.app.audio.DeviceInspector
import studio.rezofon.app.audio.EqPreset
import studio.rezofon.app.audio.FlatGains
import studio.rezofon.app.audio.GuardService
import studio.rezofon.app.audio.RzPresets
import studio.rezofon.app.audio.SoundEngine
import studio.rezofon.app.audio.suggestedPreamp

class AppState(private val context: Context) {

    val prefs = Prefs(context)
    val engine = SoundEngine(context)

    var params by mutableStateOf(loadParams())
        private set

    var snapshot by mutableStateOf(BtSnapshot())
        private set

    var guardRunning by mutableStateOf(prefs.guardEnabled)
        private set

    var profileList by mutableStateOf(prefs.profiles)
        private set

    var activePresetId by mutableStateOf(prefs.presetId)
        private set

    var engineBusy by mutableStateOf(false)
        private set

    var notice by mutableStateOf<String?>(null)
        private set

    var globalMode by mutableStateOf(prefs.globalMode)
        private set

    var darkMode by mutableStateOf(prefs.dark)
        private set

    fun toggleDark(on: Boolean) {
        darkMode = on
        prefs.dark = on
    }

    private fun loadParams(): AudioParams {
        val savedGains = prefs.gains
        val base = if (prefs.hasGains) savedGains else {
            RzPresets.firstOrNull { it.id == prefs.presetId }?.gainsDb ?: FlatGains.copyOf()
        }
        return AudioParams(
            enabled = prefs.eqEnabled,
            gainsDb = base,
            bassBoost = prefs.bass,
            bassEnabled = prefs.bassEnabled,
            subShelfDb = prefs.sub,
            loudness = prefs.loudness,
            virtualizer = prefs.virtualizer,
            preampDb = prefs.preamp,
        )
    }

    fun commit(next: AudioParams) {
        params = next
        prefs.eqEnabled = next.enabled
        prefs.gains = next.gainsDb
        prefs.hasGains = true
        prefs.bass = next.bassBoost
        prefs.bassEnabled = next.bassEnabled
        prefs.sub = next.subShelfDb
        prefs.loudness = next.loudness
        prefs.virtualizer = next.virtualizer
        prefs.preamp = next.preampDb
        engine.apply(next)
    }

    fun setGain(index: Int, db: Float) {
        val g = params.gainsDb.copyOf()
        g[index] = db.coerceIn(-12f, 12f)
        activePresetId = ""
        prefs.presetId = ""
        commit(params.copyWith(gainsDb = g, preampDb = suggestedPreamp(g, params.subShelfDb, params.bassBoost)))
    }

    fun applyPreset(preset: EqPreset) {
        activePresetId = preset.id
        prefs.presetId = preset.id
        commit(
            params.copyWith(
                gainsDb = preset.gainsDb.copyOf(),
                bassBoost = preset.bass,
                subShelfDb = preset.sub,
                loudness = preset.loudness,
                enabled = true,
                preampDb = suggestedPreamp(preset.gainsDb, preset.sub, preset.bass),
            ),
        )
    }

    fun flatten() {
        activePresetId = ""
        prefs.presetId = ""
        commit(
            params.copyWith(
                gainsDb = FloatArray(10),
                preampDb = 0f,
            ),
        )
    }

    suspend fun connectEngine() {
        engineBusy = true
        val ok = withContext(Dispatchers.Default) {
            if (globalMode) {
                engine.attachSystem(params)
            } else {
                engine.sessionModeOnly()
                true
            }
        }
        engineBusy = false
        if (!ok) {
            notice = "Ses motoru bu cihazda sistem çıkışına bağlanamadı. Atölye'den test sinyaliyle deneyin."
        }
    }

    fun toggleGlobalMode(enabled: Boolean) {
        globalMode = enabled
        prefs.globalMode = enabled
    }

    suspend fun refreshDevice() {
        snapshot = withContext(Dispatchers.IO) { DeviceInspector.snapshot(context) }
    }

    fun reconnectDevice() {
        Thread { DeviceInspector.reconnect(context) }.start()
    }

    fun setGuard(on: Boolean, pulse: Boolean = prefs.guardPulse, interval: Int = prefs.guardInterval) {
        guardRunning = on
        prefs.guardEnabled = on
        prefs.guardPulse = pulse
        prefs.guardInterval = interval
        if (on) {
            GuardService.start(context, interval, pulse)
        } else {
            GuardService.stop(context)
        }
    }

    fun updateGuard(pulse: Boolean, interval: Int) {
        prefs.guardPulse = pulse
        prefs.guardInterval = interval
        if (guardRunning) GuardService.start(context, interval, pulse)
    }

    fun saveProfile(name: String) {
        val entry = SavedProfile(
            id = "u" + System.currentTimeMillis(),
            name = name.ifBlank { "Profil ${profileList.size + 1}" },
            gains = params.gainsDb.copyOf(),
            bass = params.bassBoost,
            sub = params.subShelfDb,
            loudness = params.loudness,
        )
        profileList = profileList + entry
        prefs.profiles = profileList
        notice = "\"${entry.name}\" kaydedildi"
    }

    fun deleteProfile(id: String) {
        profileList = profileList.filterNot { it.id == id }
        prefs.profiles = profileList
    }

    fun loadProfile(p: SavedProfile) {
        activePresetId = ""
        commit(
            params.copyWith(
                gainsDb = p.gains.copyOf(),
                bassBoost = p.bass,
                subShelfDb = p.sub,
                loudness = p.loudness,
                preampDb = suggestedPreamp(p.gains, p.sub, p.bass),
            ),
        )
    }

    /** Exchange format shared with the desktop build. */
    fun exportJson(): String {
        val o = JSONObject().apply {
            put("format", "rezofon.profile/1")
            put("device", snapshot.name)
            put("gains", JSONArray(params.gainsDb.toList()))
            put("bass", params.bassBoost.toDouble())
            put("sub", params.subShelfDb.toDouble())
            put("loudness", params.loudness.toDouble())
            put("preamp", params.preampDb.toDouble())
            put("enabled", params.enabled)
        }
        return o.toString(2)
    }

    fun importJson(raw: String): Boolean = runCatching {
        val o = JSONObject(raw)
        if (!o.has("gains")) return false
        val arr = o.getJSONArray("gains")
        val gains = FloatArray(10) { i -> arr.optDouble(i, 0.0).toFloat() }
        commit(
            params.copyWith(
                gainsDb = gains,
                bassBoost = o.optDouble("bass", params.bassBoost.toDouble()).toFloat(),
                subShelfDb = o.optDouble("sub", params.subShelfDb.toDouble()).toFloat(),
                loudness = o.optDouble("loudness", params.loudness.toDouble()).toFloat(),
                preampDb = o.optDouble("preamp", params.preampDb.toDouble()).toFloat(),
            ),
        )
        true
    }.getOrDefault(false)

    fun consumeNotice(): String? {
        val n = notice
        notice = null
        return n
    }
}
