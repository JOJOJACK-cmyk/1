package com.jojojack.mathalarm.data

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class AlarmTest {
    private val zone = TimeZone.getTimeZone("Asia/Seoul")

    /** 2026-10-15(목) 기준 시각. */
    private fun at(day: Int, hour: Int, minute: Int): Calendar = Calendar.getInstance(zone).apply {
        clear()
        set(2026, Calendar.OCTOBER, day, hour, minute, 0)
    }

    @Test
    fun oneShot_laterToday() {
        val now = at(15, 6, 0)
        val alarm = Alarm(id = 1, hour = 7, minute = 30)
        assertEquals(at(15, 7, 30).timeInMillis, alarm.nextTriggerMillis(now))
    }

    @Test
    fun oneShot_alreadyPassed_isTomorrow() {
        val now = at(15, 8, 0)
        val alarm = Alarm(id = 1, hour = 7, minute = 30)
        assertEquals(at(16, 7, 30).timeInMillis, alarm.nextTriggerMillis(now))
    }

    @Test
    fun oneShot_exactlyNow_isTomorrow() {
        val now = at(15, 7, 30)
        val alarm = Alarm(id = 1, hour = 7, minute = 30)
        assertEquals(at(16, 7, 30).timeInMillis, alarm.nextTriggerMillis(now))
    }

    @Test
    fun repeating_skipsToNextSelectedDay() {
        // 목요일 08:00, 월요일만 반복 → 10/19(월)
        val now = at(15, 8, 0)
        val alarm = Alarm(id = 1, hour = 7, minute = 0, repeatDays = setOf(Calendar.MONDAY))
        assertEquals(at(19, 7, 0).timeInMillis, alarm.nextTriggerMillis(now))
    }

    @Test
    fun repeating_sameDayLaterToday() {
        val now = at(15, 6, 0)
        val alarm = Alarm(id = 1, hour = 7, minute = 0, repeatDays = setOf(Calendar.THURSDAY))
        assertEquals(at(15, 7, 0).timeInMillis, alarm.nextTriggerMillis(now))
    }

    @Test
    fun repeating_sameDayPassed_isNextWeek() {
        val now = at(15, 8, 0)
        val alarm = Alarm(id = 1, hour = 7, minute = 0, repeatDays = setOf(Calendar.THURSDAY))
        assertEquals(at(22, 7, 0).timeInMillis, alarm.nextTriggerMillis(now))
    }

    @Test
    fun repeatText() {
        assertEquals("한 번", AlarmFormat.repeatText(emptySet()))
        assertEquals("매일", AlarmFormat.repeatText((1..7).toSet()))
        assertEquals("주말", AlarmFormat.repeatText(setOf(Calendar.SATURDAY, Calendar.SUNDAY)))
        assertEquals("월 수 일", AlarmFormat.repeatText(setOf(Calendar.SUNDAY, Calendar.WEDNESDAY, Calendar.MONDAY)))
    }

    @Test
    fun clockAndUntilText() {
        assertEquals("12:05", AlarmFormat.clockText(0, 5))
        assertEquals("1:30", AlarmFormat.clockText(13, 30))
        assertEquals("8시간 30분 후에 울려요", AlarmFormat.untilText((8 * 60 + 30) * 60_000L))
        assertEquals("1분 후에 울려요", AlarmFormat.untilText(10_000L))
    }
}
