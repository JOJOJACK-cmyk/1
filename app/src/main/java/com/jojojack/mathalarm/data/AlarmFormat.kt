package com.jojojack.mathalarm.data

import java.util.Calendar

/** 화면 표시용 문자열 도우미. */
object AlarmFormat {
    /** 월요일부터 일요일 순서. */
    val WEEK_ORDER = listOf(
        Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY, Calendar.THURSDAY,
        Calendar.FRIDAY, Calendar.SATURDAY, Calendar.SUNDAY,
    )

    private val WEEKDAYS = setOf(
        Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY, Calendar.THURSDAY, Calendar.FRIDAY,
    )
    private val WEEKEND = setOf(Calendar.SATURDAY, Calendar.SUNDAY)

    fun dayLabel(day: Int): String = when (day) {
        Calendar.MONDAY -> "월"
        Calendar.TUESDAY -> "화"
        Calendar.WEDNESDAY -> "수"
        Calendar.THURSDAY -> "목"
        Calendar.FRIDAY -> "금"
        Calendar.SATURDAY -> "토"
        Calendar.SUNDAY -> "일"
        else -> "?"
    }

    fun repeatText(days: Set<Int>): String = when {
        days.isEmpty() -> "한 번"
        days.size == 7 -> "매일"
        days == WEEKDAYS -> "주중"
        days == WEEKEND -> "주말"
        else -> WEEK_ORDER.filter { it in days }.joinToString(" ") { dayLabel(it) }
    }

    fun amPm(hour: Int): String = if (hour < 12) "오전" else "오후"

    /** 예: "7:05" (12시간제). */
    fun clockText(hour: Int, minute: Int): String {
        val h12 = if (hour % 12 == 0) 12 else hour % 12
        return "%d:%02d".format(h12, minute)
    }

    /** 예: "8시간 30분 후에 울려요". */
    fun untilText(millis: Long): String {
        val totalMinutes = ((millis + 59_999) / 60_000).coerceAtLeast(1)
        val days = totalMinutes / (24 * 60)
        val hours = (totalMinutes / 60) % 24
        val minutes = totalMinutes % 60
        val parts = buildList {
            if (days > 0) add("${days}일")
            if (hours > 0) add("${hours}시간")
            if (minutes > 0) add("${minutes}분")
        }
        return parts.joinToString(" ") + " 후에 울려요"
    }
}
