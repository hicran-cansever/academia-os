package com.academiaos.app

import android.app.WallpaperManager
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.provider.Settings

/** "Uygula" tuşunun yaptığı her şey. Arka planda çalışır. */
object ThemeApplier {
    data class Result(val wallpaper: Boolean, val iconsSaved: Int, val error: String?)

    fun apply(c: Context, theme: AcademiaTheme): Result {
        var wallOk = false
        var err: String? = null
        try {
            val wm = WallpaperManager.getInstance(c)
            c.resources.openRawResource(theme.wallpaper).use { s ->
                wm.setStream(s, null, true, WallpaperManager.FLAG_SYSTEM)
            }
            c.resources.openRawResource(theme.wallpaper).use { s ->
                wm.setStream(s, null, true, WallpaperManager.FLAG_LOCK)
            }
            wallOk = true
        } catch (e: Exception) {
            err = e.message
        }
        Store.setThemeId(c, theme.id)
        WidgetUpdater.updateAll(c)
        Scheduler.scheduleMidnight(c)
        val icons = IconExporter.export(c)
        return Result(wallOk, icons, err)
    }

    fun restoreDefaultWallpaper(c: Context) {
        WallpaperManager.getInstance(c).clear(WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK)
    }
}

/** 16 ikonu Galeri > Pictures/AcademiaOS/Ikonlar klasörüne kaydeder; Theme Park oradan seçer. */
object IconExporter {
    val ICONS = listOf(
        "samsung_notes" to R.drawable.icon_samsung_notes,
        "samsung_calendar" to R.drawable.icon_samsung_calendar,
        "samsung_reminder" to R.drawable.icon_samsung_reminder,
        "my_files" to R.drawable.icon_my_files,
        "gallery" to R.drawable.icon_gallery,
        "chrome" to R.drawable.icon_chrome,
        "youtube" to R.drawable.icon_youtube,
        "moodreader" to R.drawable.icon_moodreader,
        "flexcil" to R.drawable.icon_flexcil,
        "samsung_health" to R.drawable.icon_samsung_health,
        "settings" to R.drawable.icon_settings,
        "calculator" to R.drawable.icon_calculator,
        "camera" to R.drawable.icon_camera,
        "play_store" to R.drawable.icon_play_store,
        "clock" to R.drawable.icon_clock,
        "penup" to R.drawable.icon_penup,
    )
    private val DIR = Environment.DIRECTORY_PICTURES + "/AcademiaOS/Ikonlar/"

    private fun existing(c: Context): Set<String> {
        val out = HashSet<String>()
        c.contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            arrayOf(MediaStore.Images.Media.DISPLAY_NAME),
            "${MediaStore.Images.Media.RELATIVE_PATH}=?", arrayOf(DIR), null,
        )?.use { cur -> while (cur.moveToNext()) out.add(cur.getString(0)) }
        return out
    }

    fun export(c: Context): Int {
        val have = try { existing(c) } catch (_: Exception) { emptySet() }
        var n = 0
        for ((name, res) in ICONS) {
            val file = "academia_$name.png"
            if (file in have) { n++; continue }
            try {
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, file)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                    put(MediaStore.Images.Media.RELATIVE_PATH, DIR)
                }
                val uri = c.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: continue
                c.contentResolver.openOutputStream(uri)?.use { out ->
                    c.resources.openRawResource(res).use { it.copyTo(out) }
                }
                n++
            } catch (_: Exception) { }
        }
        return n
    }
}

/** Ana ekrana widget ekleme pencerelerini sırayla açar. */
object PinHelper {
    private val ORDER = listOf(
        ClockWidget::class.java, CalendarWidget::class.java, TasksWidget::class.java,
        FocusWidget::class.java, StudyWidget::class.java, ReadingWidget::class.java,
    )

    fun missing(c: Context): List<ComponentName> {
        val m = AppWidgetManager.getInstance(c)
        return ORDER.map { ComponentName(c, it) }.filter { m.getAppWidgetIds(it).isEmpty() }
    }

    /** Sıradaki eksik widget için ekleme penceresini açar. Açtıysa true döner. */
    fun requestNext(c: Context): Boolean {
        val m = AppWidgetManager.getInstance(c)
        if (!m.isRequestPinAppWidgetSupported) return false
        val next = missing(c).firstOrNull() ?: run { Store.setPinChainLast(c, null); return false }
        // Kullanıcı bir önceki pencereyi iptal ettiyse zinciri durdur.
        if (Store.pinChainLast(c) == next.className) { Store.setPinChainLast(c, null); return false }
        Store.setPinChainLast(c, next.className)
        return m.requestPinAppWidget(next, null, null)
    }

    fun startChain(c: Context): Boolean {
        Store.setPinChainLast(c, null)
        return requestNext(c)
    }
}

/** Uygulamaların izin verilmeyen işler için kullanıcıyı doğru ekrana götürür. */
object SystemLinks {
    private const val THEME_PARK = "com.samsung.android.themedesigner"
    private const val GOOD_LOCK = "com.samsung.android.goodlock"

    fun openThemePark(c: Context): Boolean {
        c.packageManager.getLaunchIntentForPackage(THEME_PARK)?.let {
            c.startActivity(it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); return true
        }
        val store = Intent(Intent.ACTION_VIEW, Uri.parse("samsungapps://ProductDetail/$THEME_PARK"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return try { c.startActivity(store); true } catch (_: Exception) {
            c.packageManager.getLaunchIntentForPackage(GOOD_LOCK)?.let {
                c.startActivity(it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); true
            } ?: false
        }
    }

    fun themeParkInstalled(c: Context): Boolean =
        c.packageManager.getLaunchIntentForPackage(THEME_PARK) != null

    fun openDisplaySettings(c: Context) {
        c.startActivity(Intent(Settings.ACTION_DISPLAY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    fun openGallery(c: Context) {
        val i = Intent(Intent.ACTION_VIEW).setType("image/*").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try { c.startActivity(i) } catch (_: Exception) { }
    }
}
