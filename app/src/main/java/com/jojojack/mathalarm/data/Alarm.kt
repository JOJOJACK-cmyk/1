package com.jojojack.mathalarm.data

import java.util.Calendar

/**
 * 알람 하나.
 *
 * @param repeatDays 반복 요일 (Calendar.SUNDAY..Calendar.SATURDAY). 비어 있으면 한 번만 울린다.
 * @param problemCount 알람을 끄기 위해 맞혀야 하는 문제 수.
 * @param ringtoneUri 알람음 URI. null이면 기기 기본 알람음.
 */
data class Alarm(
    val id: Int,
    val hour: Int,
    val minute: Int,
    val repeatDays: Set<Int> = emptySet(),
    val enabled: Boolean = true,
    val problemCount: Int = DEFAULT_PROBLEM_COUNT,
    val ringtoneUri: String? = null,
) {
    val isRepeating: Boolean get() = repeatDays.isNotEmpty()

    /** [now] 이후 이 알람이 울릴 가장 빠른 시각(epoch millis). */
    fun nextTriggerMillis(now: Calendar = Calendar.getInstance()): Long {
        val candidate = (now.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        // 오늘부터 최대 7일 뒤까지 확인하면 반복 요일 중 하나는 반드시 걸린다.
        for (i in 0..7) {
            val dayMatches = !isRepeating || candidate.get(Calendar.DAY_OF_WEEK) in repeatDays
            if (candidate.after(now) && dayMatches) return candidate.timeInMillis
            candidate.add(Calendar.DAY_OF_YEAR, 1)
            candidate.set(Calendar.HOUR_OF_DAY, hour)
            candidate.set(Calendar.MINUTE, minute)
        }
        error("No trigger time found for alarm $id")
    }

    companion object {
        const val DEFAULT_PROBLEM_COUNT = 3
        const val MIN_PROBLEM_COUNT = 1
        const val MAX_PROBLEM_COUNT = 10
    }
}
