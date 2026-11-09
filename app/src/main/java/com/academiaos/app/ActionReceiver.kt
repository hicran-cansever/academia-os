package com.academiaos.app

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/** Widget dokunuşları, odak alarmı ve günlük yenileme buraya gelir. */
class ActionReceiver : BroadcastReceiver() {
    companion object {
        const val TOGGLE_TASK = "com.academiaos.app.TOGGLE_TASK"
        const val FOCUS_MAIN = "com.academiaos.app.FOCUS_MAIN"
        const val FOCUS_RESET = "com.academiaos.app.FOCUS_RESET"
        const val FOCUS_DONE = "com.academiaos.app.FOCUS_DONE"
        const val TICK = "com.academiaos.app.TICK"
    }

    override fun onReceive(c: Context, intent: Intent) {
        when (intent.action) {
            TOGGLE_TASK -> {
                Store.toggleTask(c, intent.getIntExtra("index", -1))
                WidgetUpdater.updateAll(c)
            }
            FOCUS_MAIN -> Focus.main(c)
            FOCUS_RESET -> Focus.reset(c)
            FOCUS_DONE -> Focus.complete(c)
            Intent.ACTION_BOOT_COMPLETED -> {
                // Yeniden başlatmada sayaç zamanı sıfırlanır; yarım kalan seans iptal edilir.
                if (Store.focusState(c) == "running") Store.setFocus(c, "idle", 0, 0)
                Scheduler.scheduleMidnight(c)
                WidgetUpdater.updateAll(c)
            }
            else -> {
                // TICK, saat/saat dilimi/dil değişimi, uygulama güncellemesi
                Scheduler.scheduleMidnight(c)
                WidgetUpdater.updateAll(c)
            }
        }
    }
}

/**
 * Günde yalnızca birkaç kez uyanır: gece yarısı (takvim, çalışma çubukları) ve selamlama
 * değiştiğinde (05, 12, 18, 23). Kesin olmayan alarm kullanır, pil dostudur.
 */
object Scheduler {
    private val HOURS = listOf(0, 5, 12, 18, 23)

    fun scheduleMidnight(c: Context) {
        val zone = ZoneId.systemDefault()
        val now = LocalTime.now()
        val nextHour = HOURS.firstOrNull { it > now.hour }
        val date = if (nextHour == null) LocalDate.now().plusDays(1) else LocalDate.now()
        val at = date.atTime(nextHour ?: 0, 0, 30).atZone(zone).toInstant().toEpochMilli()
        val pi = PendingIntent.getBroadcast(
            c, 800, Intent(c, ActionReceiver::class.java).setAction(ActionReceiver.TICK),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        c.getSystemService(AlarmManager::class.java).set(AlarmManager.RTC, at, pi)
    }
}
