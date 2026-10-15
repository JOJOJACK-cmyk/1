package com.jojojack.mathalarm.data

import android.content.Context
import androidx.core.content.edit
import org.json.JSONArray
import org.json.JSONObject

/** 알람 목록을 SharedPreferences에 JSON으로 저장한다. */
class AlarmRepository(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getAll(): List<Alarm> {
        val array = JSONArray(prefs.getString(KEY_ALARMS, "[]"))
        return (0 until array.length())
            .map { fromJson(array.getJSONObject(it)) }
            .sortedWith(compareBy({ it.hour }, { it.minute }))
    }

    fun get(id: Int): Alarm? = getAll().firstOrNull { it.id == id }

    fun nextId(): Int = (getAll().maxOfOrNull { it.id } ?: 0) + 1

    /** 같은 id가 있으면 덮어쓰고, 없으면 추가한다. */
    fun save(alarm: Alarm) {
        write(getAll().filterNot { it.id == alarm.id } + alarm)
    }

    fun delete(id: Int) {
        write(getAll().filterNot { it.id == id })
    }

    private fun write(alarms: List<Alarm>) {
        val array = JSONArray()
        alarms.forEach { array.put(toJson(it)) }
        // 리시버에서도 호출되므로 프로세스가 끝나기 전에 확실히 기록한다.
        prefs.edit(commit = true) { putString(KEY_ALARMS, array.toString()) }
    }

    private fun toJson(alarm: Alarm) = JSONObject().apply {
        put("id", alarm.id)
        put("hour", alarm.hour)
        put("minute", alarm.minute)
        put("repeatDays", JSONArray(alarm.repeatDays.sorted()))
        put("enabled", alarm.enabled)
        put("problemCount", alarm.problemCount)
        alarm.ringtoneUri?.let { put("ringtoneUri", it) }
    }

    private fun fromJson(json: JSONObject): Alarm {
        val days = json.optJSONArray("repeatDays") ?: JSONArray()
        return Alarm(
            id = json.getInt("id"),
            hour = json.getInt("hour"),
            minute = json.getInt("minute"),
            repeatDays = (0 until days.length()).map { days.getInt(it) }.toSet(),
            enabled = json.optBoolean("enabled", true),
            problemCount = json.optInt("problemCount", Alarm.DEFAULT_PROBLEM_COUNT),
            ringtoneUri = if (json.has("ringtoneUri")) json.getString("ringtoneUri") else null,
        )
    }

    private companion object {
        const val PREFS_NAME = "alarms"
        const val KEY_ALARMS = "alarms"
    }
}
