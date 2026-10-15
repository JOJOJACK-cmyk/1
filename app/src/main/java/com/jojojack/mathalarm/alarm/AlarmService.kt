package com.jojojack.mathalarm.alarm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.jojojack.mathalarm.R
import com.jojojack.mathalarm.data.Alarm
import com.jojojack.mathalarm.data.AlarmRepository
import com.jojojack.mathalarm.ring.AlarmRingActivity

/** 알람 소리와 진동을 재생하고, 문제 풀이 화면을 띄우는 알림을 유지한다. */
class AlarmService : Service() {
    private var player: MediaPlayer? = null
    private var vibrator: Vibrator? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val alarmId = intent?.getIntExtra(AlarmScheduler.EXTRA_ALARM_ID, -1) ?: -1
        val problemCount = AlarmRepository(this).get(alarmId)?.problemCount
            ?: ringingProblemCount
            ?: Alarm.DEFAULT_PROBLEM_COUNT
        ringingProblemCount = problemCount

        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        // 이미 울리는 중에 다른 알람이 오면 소리는 그대로 이어간다.
        if (player == null) startSound()
        if (vibrator == null) startVibration()
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        ringingProblemCount = null
        player?.run {
            runCatching { stop() }
            release()
        }
        player = null
        vibrator?.cancel()
        vibrator = null
        super.onDestroy()
    }

    private fun startSound() {
        val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            ?: return
        player = try {
            MediaPlayer().apply {
                setAudioAttributes(ALARM_AUDIO_ATTRIBUTES)
                setDataSource(this@AlarmService, uri)
                setWakeMode(this@AlarmService, PowerManager.PARTIAL_WAKE_LOCK)
                isLooping = true
                prepare()
                start()
            }
        } catch (e: Exception) {
            Log.e(TAG, "알람 소리를 재생하지 못했습니다", e)
            null
        }
    }

    @Suppress("DEPRECATION")
    private fun startVibration() {
        val vib = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            getSystemService(VibratorManager::class.java).defaultVibrator
        } else {
            getSystemService(Vibrator::class.java)
        }
        if (vib == null || !vib.hasVibrator()) return
        vib.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 800, 600), 0), ALARM_AUDIO_ATTRIBUTES)
        vibrator = vib
    }

    private fun buildNotification(): android.app.Notification {
        val manager = getSystemService(NotificationManager::class.java)
        if (manager.getNotificationChannel(CHANNEL_ID) == null) {
            val channel = NotificationChannel(CHANNEL_ID, "알람", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "울리고 있는 알람"
                // 소리와 진동은 서비스가 직접 재생한다.
                setSound(null, null)
                enableVibration(false)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            }
            manager.createNotificationChannel(channel)
        }

        val ringIntent = Intent(this, AlarmRingActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_USER_ACTION)
        val pendingRing = PendingIntent.getActivity(
            this,
            0,
            ringIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_alarm)
            .setContentTitle("알람이 울리고 있어요")
            .setContentText("문제를 풀어야 알람이 꺼져요")
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setContentIntent(pendingRing)
            .setFullScreenIntent(pendingRing, true)
            .build()
    }

    companion object {
        private const val TAG = "AlarmService"
        private const val CHANNEL_ID = "alarm_ringing"
        private const val NOTIFICATION_ID = 1

        private val ALARM_AUDIO_ATTRIBUTES: AudioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        /** 알람이 울리는 중이면 풀어야 할 문제 수, 아니면 null. */
        @Volatile
        var ringingProblemCount: Int? = null
            private set
    }
}
