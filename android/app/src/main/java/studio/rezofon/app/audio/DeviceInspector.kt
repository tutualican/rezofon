package studio.rezofon.app.audio

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioManager
import android.os.Build
import androidx.core.content.ContextCompat
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

data class BtSnapshot(
    val available: Boolean = false,
    val enabled: Boolean = false,
    val permissionGranted: Boolean = false,
    val name: String = "Hoparlör bulunamadı",
    val address: String = "",
    val bonded: Boolean = false,
    val a2dpConnected: Boolean = false,
    val hfpConnected: Boolean = false,
    val avrcpConnected: Boolean = false,
    val audioProps: List<Pair<String, String>> = emptyList(),
    val codec: String = "okunamadı",
    val codecDetail: String = "—",
    val sdk: Int = Build.VERSION.SDK_INT,
    val hint: String = "",
)

object DeviceInspector {

    private fun hasConnect(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) ==
            PackageManager.PERMISSION_GRANTED

    fun snapshot(context: Context): BtSnapshot {
        val manager = context.getSystemService(BluetoothManager::class.java)
        val adapter = manager?.adapter
        val am = context.getSystemService(AudioManager::class.java)
        val props = buildList {
            runCatching {
                add("Çıkış örnekleme" to "${am?.getProperty(AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE) ?: "?"} Hz")
            }
            runCatching {
                add("Çerçeve / tampon" to "${am?.getProperty(AudioManager.PROPERTY_OUTPUT_FRAMES_PER_BUFFER) ?: "?"}")
            }
            runCatching {
                val lp = am?.getProperty(AudioManager.PROPERTY_SUPPORT_AUDIO_SOURCE_UNPROCESSED)
                if (lp != null) add("İşlenmemiş kaynak" to if (lp == "true") "destekli" else "yok")
            }
            add("Android" to "${Build.VERSION.RELEASE} · API ${Build.VERSION.SDK_INT}")
            add("Üretici" to "${Build.MANUFACTURER} ${Build.MODEL}")
        }

        if (adapter == null) {
            return BtSnapshot(audioProps = props, hint = "Bu cihazda Bluetooth adaptörü görünmüyor.")
        }
        if (!hasConnect(context)) {
            return BtSnapshot(
                available = true,
                enabled = adapter.isEnabled,
                permissionGranted = false,
                audioProps = props,
                hint = "Bluetooth bilgisi için izin gerekiyor.",
            )
        }

        val device = runCatching {
            val manager = context.getSystemService(BluetoothManager::class.java)
            val connected = manager?.getConnectedDevices(BluetoothProfile.A2DP).orEmpty() +
                manager?.getConnectedDevices(BluetoothProfile.HEADSET).orEmpty()
            val pool = if (connected.isNotEmpty()) connected else adapter.bondedDevices?.toList().orEmpty()
            pool.firstOrNull { it.name?.contains("JAM", true) == true }
                ?: pool.firstOrNull { it.name?.contains("Grundig", true) == true }
                ?: pool.firstOrNull()
        }.getOrNull()

        fun stateOf(profile: Int): Boolean = runCatching {
            adapter.getProfileConnectionState(profile) == BluetoothProfile.STATE_CONNECTED
        }.getOrDefault(false)

        val a2dp = stateOf(BluetoothProfile.A2DP)
        val hfp = stateOf(BluetoothProfile.HEADSET)
        // AVRCP controller constant is hidden in the public SDK; the profile id is 11.
        val avrcp = runCatching { stateOf(11) }.getOrDefault(false)

        val codec = CodecReader.read(context, adapter, device)

        return BtSnapshot(
            available = true,
            enabled = adapter.isEnabled,
            permissionGranted = true,
            name = runCatching { device?.name }.getOrNull() ?: "Hoparlör bulunamadı",
            address = runCatching { device?.address }.getOrNull() ?: "",
            bonded = runCatching { device?.bondState == BluetoothDevice.BOND_BONDED }.getOrDefault(false),
            a2dpConnected = a2dp,
            hfpConnected = hfp,
            avrcpConnected = avrcp,
            audioProps = props,
            codec = codec.first,
            codecDetail = codec.second,
            hint = if (device == null) "Eşleşmiş cihaz yok. JAM+ ile önce Bluetooth eşleşmesi yapın." else "",
        )
    }

    /** Best-effort A2DP codec read. Public API is inconsistent across OEM builds. */
    private object CodecReader {
        fun read(context: Context, adapter: BluetoothAdapter, device: BluetoothDevice?): Pair<String, String> {
            if (device == null) return "—" to "cihaz yok"
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return "—" to "API < 26"
            val latch = CountDownLatch(1)
            var result: Pair<String, String> = "okunamadı" to "sistem API'si kapalı"
            val executor = Executors.newSingleThreadExecutor()
            runCatching {
                val listener = object : BluetoothProfile.ServiceListener {
                    override fun onServiceConnected(profile: Int, proxy: BluetoothProfile?) {
                        try {
                            if (proxy == null) return
                            val status = proxy.javaClass
                                .getMethod("getCodecStatus", BluetoothDevice::class.java)
                                .invoke(proxy, device)
                            val config = status?.javaClass?.getMethod("getCodecConfig")?.invoke(status)
                            if (config != null) {
                                val codecType = config.javaClass.getMethod("getCodecType").invoke(config) as Int
                                val rate = config.javaClass.getMethod("getSampleRate").invoke(config) as Int
                                val bits = config.javaClass.getMethod("getBitsPerSample").invoke(config) as Int
                                val channels = config.javaClass.getMethod("getChannelMode").invoke(config) as Int
                                val codecName = when (codecType) {
                                    1 -> "SBC"
                                    2 -> "AAC"
                                    3 -> "aptX"
                                    4 -> "aptX HD"
                                    5 -> "LDAC"
                                    else -> "codec $codecType"
                                }
                                val rateText = when (rate) {
                                    1 -> "44.1 kHz"; 2 -> "48 kHz"; 4 -> "88.2 kHz"; 8 -> "96 kHz"
                                    else -> "?"
                                }
                                val bitText = when (bits) {
                                    1 -> "16 bit"; 2 -> "24 bit"; 4 -> "32 bit"; else -> "?"
                                }
                                val chText = when (channels) {
                                    1 -> "mono"; 2 -> "stereo"; else -> "?"
                                }
                                result = codecName to "$rateText · $bitText · $chText"
                            }
                        } catch (t: Throwable) {
                            result = "okunamadı" to (t.message ?: "kısıtlı")
                        } finally {
                            runCatching { adapter.closeProfileProxy(BluetoothProfile.A2DP, proxy) }
                            latch.countDown()
                        }
                    }

                    override fun onServiceDisconnected(profile: Int) {
                        latch.countDown()
                    }
                }
                if (!adapter.getProfileProxy(context, listener, BluetoothProfile.A2DP)) {
                    latch.countDown()
                }
            }.onFailure { latch.countDown() }
            runCatching { latch.await(1200, TimeUnit.MILLISECONDS) }
            executor.shutdownNow()
            return result
        }
    }

    fun reconnect(context: Context) {
        if (!hasConnect(context)) return
        val adapter = context.getSystemService(BluetoothManager::class.java)?.adapter ?: return
        runCatching {
            val device = adapter.bondedDevices?.firstOrNull {
                it.name?.contains("JAM", true) == true || it.name?.contains("Grundig", true) == true
            } ?: return
            val latch = CountDownLatch(1)
            val listener = object : BluetoothProfile.ServiceListener {
                override fun onServiceConnected(profile: Int, proxy: BluetoothProfile?) {
                    runCatching {
                        val cls = proxy?.javaClass ?: return
                        cls.getMethod("disconnect", BluetoothDevice::class.java).invoke(proxy, device)
                        Thread.sleep(600)
                        cls.getMethod("connect", BluetoothDevice::class.java).invoke(proxy, device)
                    }
                    runCatching { proxy?.let { adapter.closeProfileProxy(BluetoothProfile.A2DP, it) } }
                    latch.countDown()
                }

                override fun onServiceDisconnected(profile: Int) = latch.countDown()
            }
            if (adapter.getProfileProxy(context, listener, BluetoothProfile.A2DP)) {
                latch.await(3, TimeUnit.SECONDS)
            }
        }
    }
}
