package com.jojojack.mathalarm.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.jojojack.mathalarm.data.AlarmRepository

/** 알람 시각이 되면 호출된다. 다음 알람을 준비하고 알람 서비스를 시작한다. */
class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val alarmId = intent.getIntExtra(AlarmScheduler.EXTRA_ALARM_ID, -1)
        val repository = AlarmRepository(context)
        val alarm = repository.get(alarmId) ?: return
        if (!alarm.enabled) return

        if (alarm.isRepeating) {
            AlarmScheduler.schedule(context, alarm)
        } else {
            repository.save(alarm.copy(enabled = false))
        }

        val serviceIntent = Intent(context, AlarmService::class.java)
            .putExtra(AlarmScheduler.EXTRA_ALARM_ID, alarmId)
        ContextCompat.startForegroundService(context, serviceIntent)
    }
}
