package com.academiaos.app

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

/**
 * Pomodoro. Pil dostu: widget saniye saniye güncellenmez, geri sayımı sistemin kendi
 * Chronometer'ı çizer. Bitişte tek bir alarm uyanır.
 */
object Focus {
    private const val CHANNEL = "focus"
    private const val REQ_DONE = 900

    private fun donePi(c: Context): PendingIntent = PendingIntent.getBroadcast(
        c, REQ_DONE, Intent(c, ActionReceiver::class.java).setAction(ActionReceiver.FOCUS_DONE),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    private fun schedule(c: Context, endElapsed: Long) {
        val am = c.getSystemService(AlarmManager::class.java)
        val pi = donePi(c)
        if (am.canScheduleExactAlarms()) {
            am.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, endElapsed, pi)
        } else {
            am.setAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, endElapsed, pi)
        }
    }

    private fun cancel(c: Context) = c.getSystemService(AlarmManager::class.java).cancel(donePi(c))

    fun main(c: Context) {
        val now = SystemClock.elapsedRealtime()
        when (Store.focusState(c)) {
            "running" -> {
                Store.setFocus(c, "paused", 0, (Store.focusEnd(c) - now).coerceAtLeast(0))
                cancel(c)
            }
            "paused" -> {
                val end = now + Store.focusRemaining(c)
                Store.setFocus(c, "running", end, 0)
                schedule(c, end)
            }
            else -> {
                val len = Store.focusMinutes(c) * 60_000L
                val end = now + len
                Store.setFocus(c, "running", end, 0, len)
                schedule(c, end)
            }
        }
        WidgetUpdater.updateAll(c)
    }

    fun reset(c: Context) {
        cancel(c)
        Store.setFocus(c, "idle", 0, 0)
        WidgetUpdater.updateAll(c)
    }

    fun complete(c: Context) {
        if (Store.focusState(c) != "running") return
        val minutes = (Store.focusLength(c) / 60_000L).toInt()
        val subject = Store.currentSubject(c)
        Store.addMinutes(c, subject, minutes)
        Store.addSession(c)
        Store.setFocus(c, "idle", 0, 0)
        WidgetUpdater.updateAll(c)
        notifyDone(c, subject, minutes)
    }

    private fun notifyDone(c: Context, subject: String, minutes: Int) {
        val nm = c.getSystemService(NotificationManager::class.java)
        if (nm.getNotificationChannel(CHANNEL) == null) {
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL, "Odak seansları", NotificationManager.IMPORTANCE_DEFAULT),
            )
        }
        if (!NotificationManagerCompat.from(c).areNotificationsEnabled()) return
        val n = NotificationCompat.Builder(c, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Seans tamamlandı")
            .setContentText("$subject · $minutes dk kaydedildi. Kısa bir mola ver.")
            .setAutoCancel(true)
            .build()
        try {
            NotificationManagerCompat.from(c).notify(1, n)
        } catch (_: SecurityException) { }
    }
}
