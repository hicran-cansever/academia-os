package com.academiaos.app

import android.Manifest
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.SystemClock
import android.provider.CalendarContract
import android.text.SpannableString
import android.text.Spanned
import android.text.style.StrikethroughSpan
import android.util.TypedValue
import android.view.View
import android.widget.RemoteViews
import androidx.core.content.ContextCompat
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale

object WidgetUpdater {
    private val TR: Locale = Locale.forLanguageTag("tr-TR")
    private val IVORY = Color.parseColor("#EFE6D2")
    private val IVORY_SOFT = Color.parseColor("#CFC5AF")
    private val MUTED = Color.parseColor("#9C937F")
    private val INK = Color.parseColor("#121410")

    private val PROVIDERS: List<Pair<Class<*>, (Context) -> RemoteViews>> = listOf(
        ClockWidget::class.java to ::clock,
        CalendarWidget::class.java to ::calendar,
        TasksWidget::class.java to ::tasks,
        StudyWidget::class.java to ::study,
        ReadingWidget::class.java to ::reading,
        FocusWidget::class.java to ::focus,
    )

    fun updateAll(c: Context) {
        val m = AppWidgetManager.getInstance(c)
        for ((cls, build) in PROVIDERS) {
            val ids = m.getAppWidgetIds(ComponentName(c, cls))
            if (ids.isNotEmpty()) m.updateAppWidget(ids, build(c))
        }
    }

    // ---------- yardımcılar
    private fun openActivity(c: Context, cls: Class<*>, req: Int): PendingIntent =
        PendingIntent.getActivity(
            c, req, Intent(c, cls).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

    fun action(c: Context, action: String, req: Int, index: Int = -1): PendingIntent =
        PendingIntent.getBroadcast(
            c, req,
            Intent(c, ActionReceiver::class.java).setAction(action).putExtra("index", index),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

    private fun themed(c: Context, v: RemoteViews) {
        v.setInt(R.id.card, "setBackgroundResource", Store.theme(c).card)
    }

    private fun hm(minutes: Int): String {
        val h = minutes / 60; val m = minutes % 60
        return when {
            h == 0 -> "$m dk"
            m == 0 -> "$h sa"
            else -> "$h sa $m dk"
        }
    }

    // ---------- Saat
    private fun clock(c: Context): RemoteViews {
        val v = RemoteViews(c.packageName, R.layout.widget_clock)
        val hour = java.time.LocalTime.now().hour
        val note = when (hour) {
            in 5..11 -> "günaydın"
            in 12..17 -> "iyi çalışmalar"
            in 18..22 -> "iyi akşamlar"
            else -> "iyi geceler"
        }
        v.setTextViewText(R.id.clock_note, note)
        v.setOnClickPendingIntent(R.id.root, openActivity(c, MainActivity::class.java, 10))
        return v
    }

    // ---------- Takvim
    private fun eventDays(c: Context, first: LocalDate): Set<Int> {
        if (ContextCompat.checkSelfPermission(c, Manifest.permission.READ_CALENDAR)
            != PackageManager.PERMISSION_GRANTED
        ) return emptySet()
        val zone = ZoneId.systemDefault()
        val start = first.atStartOfDay(zone).toInstant().toEpochMilli()
        val end = first.plusMonths(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val uri = CalendarContract.Instances.CONTENT_URI.buildUpon().also {
            ContentUris.appendId(it, start); ContentUris.appendId(it, end)
        }.build()
        val out = HashSet<Int>()
        try {
            c.contentResolver.query(uri, arrayOf(CalendarContract.Instances.BEGIN), null, null, null)
                ?.use { cur ->
                    while (cur.moveToNext()) {
                        val d = Instant.ofEpochMilli(cur.getLong(0)).atZone(zone).toLocalDate()
                        if (d.month == first.month) out.add(d.dayOfMonth)
                    }
                }
        } catch (_: Exception) { }
        return out
    }

    private fun calendar(c: Context): RemoteViews {
        val v = RemoteViews(c.packageName, R.layout.widget_calendar)
        themed(c, v)
        val today = LocalDate.now()
        val first = today.withDayOfMonth(1)
        val offset = first.dayOfWeek.value - 1
        val len = today.lengthOfMonth()
        val month = today.month.getDisplayName(TextStyle.FULL_STANDALONE, TR)
            .replaceFirstChar { it.titlecase(TR) }
        v.setTextViewText(R.id.cal_month, month)
        v.setTextViewText(R.id.cal_year, today.year.toString())
        val events = eventDays(c, first)
        val dots = intArrayOf(R.drawable.day_event_coral, R.drawable.day_event_blue, R.drawable.day_event_green)
        var k = 0
        for (i in 0 until 42) {
            val id = c.resources.getIdentifier("d$i", "id", c.packageName)
            val day = i - offset + 1
            if (day in 1..len) {
                v.setTextViewText(id, day.toString())
                when {
                    day == today.dayOfMonth -> {
                        v.setInt(id, "setBackgroundResource", R.drawable.day_today)
                        v.setTextColor(id, INK)
                    }
                    day in events -> {
                        v.setInt(id, "setBackgroundResource", dots[k++ % dots.size])
                        v.setTextColor(id, IVORY_SOFT)
                    }
                    else -> {
                        v.setInt(id, "setBackgroundResource", R.drawable.day_none)
                        v.setTextColor(id, IVORY_SOFT)
                    }
                }
            } else {
                v.setTextViewText(id, "")
                v.setInt(id, "setBackgroundResource", R.drawable.day_none)
            }
        }
        val open = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_CALENDAR)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        v.setOnClickPendingIntent(
            R.id.root,
            PendingIntent.getActivity(c, 20, open, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT),
        )
        return v
    }

    // ---------- Görevler
    private fun tasks(c: Context): RemoteViews {
        val v = RemoteViews(c.packageName, R.layout.widget_tasks)
        themed(c, v)
        val all = Store.tasks(c)
        val shown = Store.tasksForWidget(c).take(5)
        val done = all.count { it.done }
        v.setTextViewText(R.id.tasks_count, if (all.isEmpty()) "" else "$done / ${all.size}")
        v.setProgressBar(R.id.tasks_progress, 100, if (all.isEmpty()) 0 else done * 100 / all.size, false)
        v.setViewVisibility(R.id.tasks_empty, if (all.isEmpty()) View.VISIBLE else View.GONE)
        for (i in 0 until 5) {
            val row = c.resources.getIdentifier("task_row$i", "id", c.packageName)
            val chk = c.resources.getIdentifier("task_check$i", "id", c.packageName)
            val txt = c.resources.getIdentifier("task_text$i", "id", c.packageName)
            val item = shown.getOrNull(i)
            if (item == null) {
                v.setViewVisibility(row, View.GONE); continue
            }
            val (index, t) = item
            v.setViewVisibility(row, View.VISIBLE)
            v.setImageViewResource(chk, if (t.done) R.drawable.check_on else R.drawable.check_off)
            if (t.done) {
                val s = SpannableString(t.text)
                s.setSpan(StrikethroughSpan(), 0, s.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                v.setTextViewText(txt, s); v.setTextColor(txt, MUTED)
            } else {
                v.setTextViewText(txt, t.text); v.setTextColor(txt, IVORY)
            }
            v.setOnClickPendingIntent(row, action(c, ActionReceiver.TOGGLE_TASK, 300 + index, index))
        }
        val open = openActivity(c, TasksActivity::class.java, 30)
        v.setOnClickPendingIntent(R.id.tasks_title, open)
        v.setOnClickPendingIntent(R.id.tasks_empty, open)
        v.setOnClickPendingIntent(R.id.tasks_bird, open)
        return v
    }

    // ---------- Çalışma (son 7 gün)
    private val DAY_TR = mapOf(
        DayOfWeek.MONDAY to "Pt", DayOfWeek.TUESDAY to "Sa", DayOfWeek.WEDNESDAY to "Ça",
        DayOfWeek.THURSDAY to "Pe", DayOfWeek.FRIDAY to "Cu", DayOfWeek.SATURDAY to "Ct", DayOfWeek.SUNDAY to "Pz",
    )

    private fun study(c: Context): RemoteViews {
        val v = RemoteViews(c.packageName, R.layout.widget_study)
        val theme = Store.theme(c)
        themed(c, v)
        val today = LocalDate.now()
        val days = (6 downTo 0).map { today.minusDays(it.toLong()) }
        val mins = days.map { Store.minutesOn(c, it.toString()) }
        val max = maxOf(60, mins.maxOrNull() ?: 0)
        v.setTextViewText(R.id.study_total, hm(mins.sum()))
        days.forEachIndexed { i, d ->
            val bar = c.resources.getIdentifier("bar$i", "id", c.packageName)
            val lab = c.resources.getIdentifier("lab$i", "id", c.packageName)
            val isToday = i == 6
            val h = 4f + 52f * mins[i] / max
            v.setViewLayoutHeight(bar, h, TypedValue.COMPLEX_UNIT_DIP)
            v.setImageViewResource(
                bar, when {
                    mins[i] == 0 -> R.drawable.bar_empty
                    isToday -> R.drawable.bar_today
                    else -> theme.bar
                },
            )
            v.setTextViewText(lab, if (isToday) "Bugün" else DAY_TR[d.dayOfWeek])
            v.setTextColor(lab, if (isToday) IVORY else MUTED)
        }
        val subj = Store.subjectMinutes(c).take(3)
        v.setTextViewText(
            R.id.study_subjects,
            if (subj.isEmpty()) "Odak seansı bitirince burada görünür"
            else subj.joinToString("  ·  ") { "${it.first} ${hm(it.second)}" },
        )
        v.setOnClickPendingIntent(R.id.root, openActivity(c, SubjectsActivity::class.java, 40))
        return v
    }

    // ---------- Okuma
    private fun reading(c: Context): RemoteViews {
        val v = RemoteViews(c.packageName, R.layout.widget_reading)
        themed(c, v)
        val r = Store.reading(c)
        if (r.title.isBlank()) {
            v.setTextViewText(R.id.read_title, "Kitabını ekle")
            v.setTextViewText(R.id.read_author, "Dokun ve okuduğun kitabı yaz")
            v.setProgressBar(R.id.read_progress, 100, 0, false)
            v.setTextViewText(R.id.read_pct, ""); v.setTextViewText(R.id.read_today, "")
        } else {
            val pct = if (r.total > 0) (r.current * 100 / r.total).coerceIn(0, 100) else 0
            v.setTextViewText(R.id.read_title, r.title)
            v.setTextViewText(R.id.read_author, r.author)
            v.setProgressBar(R.id.read_progress, 100, pct, false)
            v.setTextViewText(R.id.read_pct, "%$pct")
            v.setTextViewText(R.id.read_today, "bugün ${Store.pagesToday(c)} / ${r.goal} sayfa")
        }
        v.setOnClickPendingIntent(R.id.root, openActivity(c, ReadingActivity::class.java, 50))
        return v
    }

    // ---------- Odak
    private fun focus(c: Context): RemoteViews {
        val v = RemoteViews(c.packageName, R.layout.widget_focus)
        themed(c, v)
        val state = Store.focusState(c)
        val now = SystemClock.elapsedRealtime()
        val n = Store.sessionsToday(c)
        v.setTextViewText(R.id.focus_cap, if (n == 0) "Odak" else "Odak · bugün $n seans")
        v.setTextViewText(R.id.focus_subject, Store.currentSubject(c))
        v.setChronometerCountDown(R.id.focus_timer, true)
        when (state) {
            "running" -> {
                v.setChronometer(R.id.focus_timer, Store.focusEnd(c), null, true)
                v.setTextViewText(R.id.focus_main, "Duraklat")
            }
            "paused" -> {
                v.setChronometer(R.id.focus_timer, now + Store.focusRemaining(c), null, false)
                v.setTextViewText(R.id.focus_main, "Devam")
            }
            else -> {
                v.setChronometer(R.id.focus_timer, now + Store.focusMinutes(c) * 60_000L, null, false)
                v.setTextViewText(R.id.focus_main, "Başlat")
            }
        }
        v.setOnClickPendingIntent(R.id.focus_main, action(c, ActionReceiver.FOCUS_MAIN, 60))
        v.setOnClickPendingIntent(R.id.focus_reset, action(c, ActionReceiver.FOCUS_RESET, 61))
        v.setOnClickPendingIntent(R.id.focus_subject, openActivity(c, SubjectsActivity::class.java, 62))
        return v
    }
}
