package studio.rezofon.app.state

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class SavedProfile(
    val id: String,
    val name: String,
    val gains: FloatArray,
    val bass: Float,
    val sub: Float,
    val loudness: Float,
)

class Prefs(context: Context) {
    private val sp = context.getSharedPreferences("Rezofon", Context.MODE_PRIVATE)

    var eqEnabled: Boolean
        get() = sp.getBoolean("eq_enabled", true)
        set(v) = sp.edit().putBoolean("eq_enabled", v).apply()

    var gains: FloatArray
        get() {
            val raw = sp.getString("gains", null) ?: return FloatArray(10)
            return raw.split(",").mapNotNull { it.toFloatOrNull() }.toFloatArray().let {
                if (it.size == 10) it else FloatArray(10)
            }
        }
        set(v) = sp.edit().putString("gains", v.joinToString(",")).apply()

    var bassEnabled: Boolean
        get() = sp.getBoolean("bass_enabled", true)
        set(v) = sp.edit().putBoolean("bass_enabled", v).apply()

    var bass: Float
        get() = sp.getFloat("bass", 0.3f)
        set(v) = sp.edit().putFloat("bass", v).apply()

    var sub: Float
        get() = sp.getFloat("sub", 3f)
        set(v) = sp.edit().putFloat("sub", v).apply()

    var loudness: Float
        get() = sp.getFloat("loudness", 0.2f)
        set(v) = sp.edit().putFloat("loudness", v).apply()

    var virtualizer: Float
        get() = sp.getFloat("virtualizer", 0f)
        set(v) = sp.edit().putFloat("virtualizer", v).apply()

    var preamp: Float
        get() = sp.getFloat("preamp", 0f)
        set(v) = sp.edit().putFloat("preamp", v).apply()

    var globalMode: Boolean
        get() = sp.getBoolean("global_mode", true)
        set(v) = sp.edit().putBoolean("global_mode", v).apply()

    var hasGains: Boolean
        get() = sp.getBoolean("has_gains", false)
        set(v) = sp.edit().putBoolean("has_gains", v).apply()

    var dark: Boolean
        get() = sp.getBoolean("dark", true)
        set(v) = sp.edit().putBoolean("dark", v).apply()

    var guardEnabled: Boolean
        get() = sp.getBoolean("guard_enabled", false)
        set(v) = sp.edit().putBoolean("guard_enabled", v).apply()

    var guardPulse: Boolean
        get() = sp.getBoolean("guard_pulse", true)
        set(v) = sp.edit().putBoolean("guard_pulse", v).apply()

    var guardInterval: Int
        get() = sp.getInt("guard_interval", 15)
        set(v) = sp.edit().putInt("guard_interval", v).apply()

    var presetId: String
        get() = sp.getString("preset_id", "jam_balance") ?: "jam_balance"
        set(v) = sp.edit().putString("preset_id", v).apply()

    var profiles: List<SavedProfile>
        get() {
            val raw = sp.getString("profiles", null) ?: return emptyList()
            return runCatching {
                val arr = JSONArray(raw)
                (0 until arr.length()).map { i ->
                    val o = arr.getJSONObject(i)
                    SavedProfile(
                        id = o.optString("id"),
                        name = o.optString("name"),
                        gains = o.optJSONArray("gains")?.let { g ->
                            FloatArray(g.length()) { idx -> g.getDouble(idx).toFloat() }
                        } ?: FloatArray(10),
                        bass = o.optDouble("bass", 0.3).toFloat(),
                        sub = o.optDouble("sub", 3.0).toFloat(),
                        loudness = o.optDouble("loudness", 0.2).toFloat(),
                    )
                }
            }.getOrDefault(emptyList())
        }
        set(v) {
            val arr = JSONArray()
            v.forEach { p ->
                arr.put(
                    JSONObject().apply {
                        put("id", p.id)
                        put("name", p.name)
                        put("gains", JSONArray(p.gains.toList()))
                        put("bass", p.bass.toDouble())
                        put("sub", p.sub.toDouble())
                        put("loudness", p.loudness.toDouble())
                    },
                )
            }
            sp.edit().putString("profiles", arr.toString()).apply()
        }
}
