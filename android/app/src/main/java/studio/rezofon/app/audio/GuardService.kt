package studio.rezofon.app.audio

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import studio.rezofon.app.MainActivity
import studio.rezofon.app.R

/**
 * Keeps the Bluetooth audio link awake so the JAM+ does not drop into standby.
 *
 * The speaker's own idle timer reacts to the audio stream, not to the phone's
 * screen. So the guard holds a real (silent) media stream open. Two duty
 * cycles: continuous for maximum reliability, pulsed for battery life.
 */
class GuardService : Service() {

    companion object {
        const val ACTION_START = "studio.Rezofon.guard.START"
        const val ACTION_STOP = "studio.Rezofon.guard.STOP"
        const val EXTRA_INTERVAL = "interval_seconds"
        const val EXTRA_PULSE = "pulse"
        const val CHANNEL_ID = "Rezofon_guard"
        const val NOTIFICATION_ID = 4310

        fun start(context: Context, intervalSeconds: Int, pulse: Boolean) {
            val intent = Intent(context, GuardService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_INTERVAL, intervalSeconds)
                putExtra(EXTRA_PULSE, pulse)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, GuardService::class.java))
        }
    }

    @Volatile
    private var running = false
    private var worker: Thread? = null
    private var track: AudioTrack? = null
    private var pulse = false
    private var intervalMs = 15_000L

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
        }
        pulse = intent?.getBooleanExtra(EXTRA_PULSE, false) ?: false
        intervalMs = ((intent?.getIntExtra(EXTRA_INTERVAL, 15) ?: 15).coerceIn(5, 300)) * 1000L
        startForeground(NOTIFICATION_ID, buildNotification())
        if (!running) startEngine()
        return START_STICKY
    }

    private fun startEngine() {
        running = true
        worker = Thread({
            val minBuffer = AudioTrack.getMinBufferSize(
                48_000,
                AudioFormat.CHANNEL_OUT_STEREO,
                AudioFormat.ENCODING_PCM_16BIT,
            ).coerceAtLeast(4096)

            val audioTrack = runCatching {
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
                            .setSampleRate(48_000)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                            .build(),
                    )
                    .setBufferSizeInBytes(minBuffer * 2)
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build()
            }.getOrNull() ?: return@Thread

            track = audioTrack
            val zeros = ShortArray(minBuffer / 2)
            runCatching { audioTrack.play() }
            var nextPauseAt = System.currentTimeMillis() + 900L

            while (running) {
                runCatching { audioTrack.write(zeros, 0, zeros.size) }
                if (pulse) {
                    val now = System.currentTimeMillis()
                    if (now >= nextPauseAt) {
                        runCatching { audioTrack.pause() }
                        val sleepFor = (intervalMs - 900L).coerceAtLeast(500L)
                        var slept = 0L
                        while (running && slept < sleepFor) {
                            Thread.sleep(200L)
                            slept += 200L
                        }
                        runCatching { audioTrack.play() }
                        nextPauseAt = System.currentTimeMillis() + 900L
                    }
                }
            }
            runCatching { audioTrack.stop() }
            runCatching { audioTrack.release() }
        }, "Rezofon-guard").apply { isDaemon = true; start() }
    }

    override fun onDestroy() {
        running = false
        worker?.interrupt()
        worker = null
        runCatching { track?.release() }
        track = null
        super.onDestroy()
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.guard_channel_name),
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = getString(R.string.guard_channel_desc)
                setShowBadge(false)
            }
            getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val open = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val stop = PendingIntent.getService(
            this,
            1,
            Intent(this, GuardService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_guard)
            .setContentTitle("Ses hattı açık")
            .setContentText(
                if (pulse) "Nabız modu · ${intervalMs / 1000} sn'de bir hat tazeleniyor"
                else "JAM+ bekleme zamanlayıcısına karşı sürekli hat",
            )
            .setOngoing(true)
            .setContentIntent(open)
            .addAction(0, "Durdur", stop)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }
}
