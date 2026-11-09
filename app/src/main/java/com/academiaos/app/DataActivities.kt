package com.academiaos.app

import android.os.Bundle
import android.view.Gravity
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.academiaos.app.Ui.button
import com.academiaos.app.Ui.cap
import com.academiaos.app.Ui.dp
import com.academiaos.app.Ui.field
import com.academiaos.app.Ui.page
import com.academiaos.app.Ui.row
import com.academiaos.app.Ui.text
import com.academiaos.app.Ui.title

/** Görevler: ekle, işaretle, sil. Widget'taki satıra dokunmak da işaretler. */
class TasksActivity : AppCompatActivity() {
    private lateinit var listBox: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        page {
            title("Görevler")
            text("Widget'ta en fazla 5 görev görünür; önce yapılmamışlar.", 13f)
            val input = field("Yeni görev")
            button("Ekle", primary = true) {
                val t = input.text.toString().trim()
                if (t.isNotEmpty()) {
                    Store.saveTasks(this@TasksActivity, Store.tasks(this@TasksActivity) + Store.Task(t, false))
                    input.setText(""); changed()
                }
            }
            cap("Liste")
            listBox = LinearLayout(this@TasksActivity).apply { orientation = LinearLayout.VERTICAL }
            addView(listBox)
            button("Tamamlananları temizle") {
                Store.saveTasks(this@TasksActivity, Store.tasks(this@TasksActivity).filterNot { it.done }); changed()
            }
        }
        draw()
    }

    private fun changed() { WidgetUpdater.updateAll(this); draw() }

    private fun draw() {
        listBox.removeAllViews()
        val tasks = Store.tasks(this)
        if (tasks.isEmpty()) listBox.text("Henüz görev yok.", 14f)
        tasks.forEachIndexed { i, t ->
            listBox.row {
                val cb = CheckBox(context).apply {
                    text = t.text; isChecked = t.done; setTextColor(Ui.IVORY); typeface = Ui.SERIF; textSize = 16f
                    buttonTintList = android.content.res.ColorStateList.valueOf(Ui.GOLD)
                    setOnCheckedChangeListener { _, _ -> Store.toggleTask(this@TasksActivity, i); changed() }
                }
                addView(cb, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
                val del = TextView(context).apply {
                    text = "Sil"; setTextColor(Ui.MUTED); typeface = Ui.SERIF; textSize = 14f
                    gravity = Gravity.CENTER; minWidth = dp(56); minHeight = dp(44)
                    setOnClickListener {
                        val l = Store.tasks(this@TasksActivity); if (i in l.indices) { l.removeAt(i); Store.saveTasks(this@TasksActivity, l) }
                        changed()
                    }
                }
                addView(del)
            }
        }
    }
}

/** Okunan kitap ve günlük hedef. */
class ReadingActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val r = Store.reading(this)
        page {
            title("Okuduğum kitap")
            cap("Kitap")
            val title = field("Kitap adı", r.title)
            val author = field("Yazar", r.author)
            cap("Sayfalar")
            val total = field("Toplam sayfa", if (r.total > 0) r.total.toString() else "", number = true)
            val current = field("Şu an kaçıncı sayfadasın", if (r.current > 0) r.current.toString() else "", number = true)
            val goal = field("Günlük hedef (sayfa)", r.goal.toString(), number = true)
            fun save(add: Int = 0) {
                val cur = (current.text.toString().toIntOrNull() ?: 0) + add
                current.setText(cur.toString())
                Store.saveReading(this@ReadingActivity, Store.Reading(
                    title.text.toString().trim(), author.text.toString().trim(),
                    total.text.toString().toIntOrNull() ?: 0, cur, goal.text.toString().toIntOrNull() ?: 20,
                ))
                WidgetUpdater.updateAll(this@ReadingActivity)
                Toast.makeText(this@ReadingActivity, "Kaydedildi · bugün ${Store.pagesToday(this@ReadingActivity)} sayfa", Toast.LENGTH_SHORT).show()
            }
            row {
                for (n in listOf(5, 10, 20)) {
                    val b = com.google.android.material.button.MaterialButton(context).apply {
                        text = "+$n"; isAllCaps = false; typeface = Ui.SERIF
                        backgroundTintList = android.content.res.ColorStateList.valueOf(Ui.SURFACE); setTextColor(Ui.IVORY)
                        setOnClickListener { save(n) }
                    }
                    addView(b, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { marginEnd = dp(8) })
                }
            }
            button("Kaydet", primary = true) { save() }
        }
    }
}

/** Dersler, şu anki ders, odak süresi ve elle çalışma ekleme. */
class SubjectsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        page {
            title("Dersler ve odak")
            cap("Ders listesi")
            text("Virgülle ayır.", 13f)
            val subjects = field("Analiz, Algoritmalar, İngilizce", Store.subjects(this@SubjectsActivity).joinToString(", "))
            cap("Şu an çalıştığım ders")
            val group = RadioGroup(this@SubjectsActivity)
            addView(group)
            fun drawRadios() {
                group.removeAllViews()
                val cur = Store.currentSubject(this@SubjectsActivity)
                Store.subjects(this@SubjectsActivity).forEach { s ->
                    val rb = RadioButton(this@SubjectsActivity).apply {
                        text = s; setTextColor(Ui.IVORY); typeface = Ui.SERIF; textSize = 16f
                        buttonTintList = android.content.res.ColorStateList.valueOf(Ui.GOLD)
                        id = android.view.View.generateViewId()
                    }
                    group.addView(rb)
                    if (s == cur) rb.isChecked = true
                    rb.setOnClickListener { Store.setCurrentSubject(this@SubjectsActivity, s); WidgetUpdater.updateAll(this@SubjectsActivity) }
                }
            }
            drawRadios()
            cap("Odak süresi (dakika)")
            val dur = field("25", Store.focusMinutes(this@SubjectsActivity).toString(), number = true)
            button("Kaydet", primary = true) {
                val list = subjects.text.toString().split(",").map { it.trim() }.filter { it.isNotEmpty() }
                Store.saveSubjects(this@SubjectsActivity, list)
                if (Store.currentSubject(this@SubjectsActivity) !in list && list.isNotEmpty())
                    Store.setCurrentSubject(this@SubjectsActivity, list[0])
                dur.text.toString().toIntOrNull()?.let { Store.setFocusMinutes(this@SubjectsActivity, it) }
                WidgetUpdater.updateAll(this@SubjectsActivity)
                drawRadios()
                Toast.makeText(this@SubjectsActivity, "Kaydedildi", Toast.LENGTH_SHORT).show()
            }
            cap("Elle ekle")
            text("Odak sayacını kullanmadığın çalışmaları şu anki derse ekle.", 13f)
            row {
                for (n in listOf(15, 30, 60)) {
                    val b = com.google.android.material.button.MaterialButton(context).apply {
                        text = "+$n dk"; isAllCaps = false; typeface = Ui.SERIF
                        backgroundTintList = android.content.res.ColorStateList.valueOf(Ui.SURFACE); setTextColor(Ui.IVORY)
                        setOnClickListener {
                            Store.addMinutes(this@SubjectsActivity, Store.currentSubject(this@SubjectsActivity), n)
                            WidgetUpdater.updateAll(this@SubjectsActivity)
                            Toast.makeText(this@SubjectsActivity, "${Store.currentSubject(this@SubjectsActivity)} +$n dk", Toast.LENGTH_SHORT).show()
                        }
                    }
                    addView(b, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { marginEnd = dp(8) })
                }
            }
        }
    }
}
