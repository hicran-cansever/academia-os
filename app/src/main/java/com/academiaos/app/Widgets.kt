package com.academiaos.app

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.os.Bundle

/** Tüm widget'lar aynı yerden güncellenir; her biri yalnızca kendi verisi değişince yeniden çizilir. */
abstract class BaseWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        WidgetUpdater.updateAll(context)
    }

    override fun onAppWidgetOptionsChanged(
        context: Context, manager: AppWidgetManager, id: Int, newOptions: Bundle,
    ) {
        WidgetUpdater.updateAll(context)
    }

    override fun onEnabled(context: Context) {
        Scheduler.scheduleMidnight(context)
    }
}

class ClockWidget : BaseWidget()
class CalendarWidget : BaseWidget()
class TasksWidget : BaseWidget()
class StudyWidget : BaseWidget()
class ReadingWidget : BaseWidget()
class FocusWidget : BaseWidget()
