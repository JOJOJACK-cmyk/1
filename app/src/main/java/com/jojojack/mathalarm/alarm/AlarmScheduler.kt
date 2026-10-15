package com.jojojack.mathalarm.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.jojojack.mathalarm.MainActivity
import com.jojojack.mathalarm.data.Alarm
import com.jojojack.mathalarm.data.AlarmRepository

object AlarmScheduler {
    const val EXTRA_ALARM_ID = "alarm_id"

    fun canScheduleExact(context: Context): Boolean {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()
    }

    /** 알람을 등록하고 울릴 시각을 돌려준다. 정확한 알람 권한이 없으면 null. */
    fun schedule(context: Context, alarm: Alarm): Long? {
        if (!canScheduleExact(context)) return null
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        val triggerAt = alarm.nextTriggerMillis()
        val showIntent = PendingIntent.getActivity(
            context,
            alarm.id,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        // setAlarmClock은 절전(Doze) 모드에서도 정확히 울리고 상태바에 알람 아이콘을 표시한다.
        alarmManager.setAlarmClock(
            AlarmManager.AlarmClockInfo(triggerAt, showIntent),
            firePendingIntent(context, alarm.id),
        )
        return triggerAt
    }

    fun cancel(context: Context, alarmId: Int) {
        context.getSystemService(AlarmManager::class.java).cancel(firePendingIntent(context, alarmId))
    }

    fun rescheduleAll(context: Context) {
        AlarmRepository(context).getAll().filter { it.enabled }.forEach { schedule(context, it) }
    }

    private fun firePendingIntent(context: Context, alarmId: Int): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            alarmId,
            Intent(context, AlarmReceiver::class.java).putExtra(EXTRA_ALARM_ID, alarmId),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
}
