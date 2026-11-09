package com.academiaos.app

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

/** Tüm veriler cihazda, SharedPreferences içinde tutulur. İnternet yok, hesap yok. */
object Store {
    private fun p(c: Context): SharedPreferences =
        c.getSharedPreferences("academia", Context.MODE_PRIVATE)

    fun today(): String = LocalDate.now().toString()

    // ---------- Tema
    fun themeId(c: Context): String? = p(c).getString("theme", null)
    fun setThemeId(c: Context, id: String) = p(c).edit().putString("theme", id).apply()
    fun theme(c: Context): AcademiaTheme = Themes.byId(themeId(c))

    // ---------- Görevler
    data class Task(val text: String, val done: Boolean)

    fun tasks(c: Context): MutableList<Task> {
        val raw = p(c).getString("tasks", null) ?: return mutableListOf()
        val arr = JSONArray(raw)
        return MutableList(arr.length()) { i ->
            val o = arr.getJSONObject(i)
            Task(o.optString("t"), o.optBoolean("d"))
        }
    }

    fun saveTasks(c: Context, list: List<Task>) {
        val arr = JSONArray()
        list.forEach { arr.put(JSONObject().put("t", it.text).put("d", it.done)) }
        p(c).edit().putString("tasks", arr.toString()).apply()
    }

    /** Widget'ta gösterilen sıra: önce yapılmamışlar, sonra yapılmışlar. */
    fun tasksForWidget(c: Context): List<Pair<Int, Task>> =
        tasks(c).withIndex().sortedBy { it.value.done }.map { it.index to it.value }

    fun toggleTask(c: Context, index: Int) {
        val list = tasks(c)
        if (index in list.indices) {
            list[index] = list[index].copy(done = !list[index].done)
            saveTasks(c, list)
        }
    }

    // ---------- Okuma
    data class Reading(
        val title: String, val author: String,
        val total: Int, val current: Int, val goal: Int,
    )

    fun reading(c: Context): Reading = with(p(c)) {
        Reading(
            getString("r_title", "") ?: "",
            getString("r_author", "") ?: "",
            getInt("r_total", 0),
            getInt("r_current", 0),
            getInt("r_goal", 20),
        )
    }

    fun saveReading(c: Context, r: Reading) {
        rollReadingDay(c)
        p(c).edit()
            .putString("r_title", r.title).putString("r_author", r.author)
            .putInt("r_total", r.total).putInt("r_current", r.current).putInt("r_goal", r.goal)
            .apply()
    }

    /** Gün değiştiyse "bugün okunan" sayacını sıfırlar. */
    private fun rollReadingDay(c: Context) {
        val pr = p(c)
        if (pr.getString("r_day", null) != today()) {
            pr.edit().putString("r_day", today()).putInt("r_day_start", pr.getInt("r_current", 0)).apply()
        }
    }

    fun pagesToday(c: Context): Int {
        rollReadingDay(c)
        val pr = p(c)
        return (pr.getInt("r_current", 0) - pr.getInt("r_day_start", 0)).coerceAtLeast(0)
    }

    // ---------- Dersler ve çalışma kaydı
    fun subjects(c: Context): List<String> {
        val raw = p(c).getString("subjects", null)
            ?: return listOf("Analiz", "Algoritmalar", "İngilizce")
        val arr = JSONArray(raw)
        return List(arr.length()) { arr.getString(it) }.filter { it.isNotBlank() }
    }

    fun saveSubjects(c: Context, list: List<String>) {
        val arr = JSONArray(); list.forEach { arr.put(it) }
        p(c).edit().putString("subjects", arr.toString()).apply()
    }

    fun currentSubject(c: Context): String =
        p(c).getString("subject", null) ?: subjects(c).firstOrNull() ?: "Genel"

    fun setCurrentSubject(c: Context, s: String) = p(c).edit().putString("subject", s).apply()

    private fun log(c: Context): JSONObject = JSONObject(p(c).getString("log", "{}") ?: "{}")

    fun addMinutes(c: Context, subject: String, minutes: Int, date: String = today()) {
        val l = log(c)
        val day = l.optJSONObject(date) ?: JSONObject()
        day.put(subject, day.optInt(subject, 0) + minutes)
        l.put(date, day)
        p(c).edit().putString("log", l.toString()).apply()
    }

    fun minutesOn(c: Context, date: String): Int {
        val day = log(c).optJSONObject(date) ?: return 0
        var sum = 0
        day.keys().forEach { sum += day.optInt(it, 0) }
        return sum
    }

    fun subjectMinutes(c: Context, days: Int = 7): List<Pair<String, Int>> {
        val l = log(c)
        val totals = linkedMapOf<String, Int>()
        val now = LocalDate.now()
        for (i in 0 until days) {
            val day = l.optJSONObject(now.minusDays(i.toLong()).toString()) ?: continue
            day.keys().forEach { k -> totals[k] = (totals[k] ?: 0) + day.optInt(k, 0) }
        }
        return totals.entries.sortedByDescending { it.value }.map { it.key to it.value }
    }

    // ---------- Odak (pomodoro)
    fun focusMinutes(c: Context): Int = p(c).getInt("f_dur", 25)
    fun setFocusMinutes(c: Context, m: Int) = p(c).edit().putInt("f_dur", m.coerceIn(5, 120)).apply()

    /** state: idle | running | paused. end = bitiş anı (elapsedRealtime). rem = kalan ms (duraklatılmışken). */
    fun focusState(c: Context): String = p(c).getString("f_state", "idle") ?: "idle"
    fun focusEnd(c: Context): Long = p(c).getLong("f_end", 0)
    fun focusRemaining(c: Context): Long = p(c).getLong("f_rem", 0)
    fun focusLength(c: Context): Long = p(c).getLong("f_len", focusMinutes(c) * 60_000L)

    fun setFocus(c: Context, state: String, end: Long, remaining: Long, length: Long = focusLength(c)) {
        p(c).edit().putString("f_state", state).putLong("f_end", end)
            .putLong("f_rem", remaining).putLong("f_len", length).apply()
    }

    fun sessionsToday(c: Context): Int =
        if (p(c).getString("f_day", null) == today()) p(c).getInt("f_count", 0) else 0

    fun addSession(c: Context) {
        val n = sessionsToday(c) + 1
        p(c).edit().putString("f_day", today()).putInt("f_count", n).apply()
    }

    // ---------- Widget sabitleme zinciri
    fun pinChainLast(c: Context): String? = p(c).getString("pin_last", null)
    fun setPinChainLast(c: Context, v: String?) = p(c).edit().putString("pin_last", v).apply()
}
