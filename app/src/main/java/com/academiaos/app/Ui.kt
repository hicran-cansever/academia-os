package com.academiaos.app

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.text.InputType
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView

/** Uygulama ekranları için küçük, tutarlı yapı taşları (XML yerine kodla). */
object Ui {
    val IVORY = Color.parseColor("#EFE6D2")
    val SOFT = Color.parseColor("#CFC5AF")
    val MUTED = Color.parseColor("#B3A992")
    val GOLD = Color.parseColor("#C9A86A")
    val INK = Color.parseColor("#0D0F0D")
    val SURFACE = Color.parseColor("#161915")
    val LINE = Color.parseColor("#42C9A86A")
    val SERIF: Typeface = Typeface.SERIF

    fun Context.dp(v: Int): Int =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v.toFloat(), resources.displayMetrics).toInt()

    fun AppCompatActivity.page(build: LinearLayout.() -> Unit): LinearLayout {
        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), dp(28), dp(24), dp(40))
        }
        val scroll = ScrollView(this).apply {
            setBackgroundColor(INK)
            isFillViewport = true
            addView(col)
        }
        setContentView(scroll)
        col.build()
        return col
    }

    fun LinearLayout.title(t: String, size: Float = 30f): TextView = text(t, size, IVORY).also { it.typeface = SERIF }

    fun LinearLayout.cap(t: String): TextView = text(t.uppercase(java.util.Locale.forLanguageTag("tr")), 11f, GOLD).apply {
        letterSpacing = 0.18f
        (layoutParams as LinearLayout.LayoutParams).topMargin = context.dp(22)
    }

    fun LinearLayout.text(t: String, size: Float = 15f, color: Int = SOFT): TextView {
        val v = TextView(context).apply {
            text = t; textSize = size; setTextColor(color); typeface = SERIF
            setLineSpacing(0f, 1.15f)
        }
        addView(v, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = context.dp(6) })
        return v
    }

    fun LinearLayout.button(t: String, primary: Boolean = false, onClick: (View) -> Unit): MaterialButton {
        val b = MaterialButton(context).apply {
            text = t; isAllCaps = false; typeface = SERIF; textSize = if (primary) 17f else 15f
            minHeight = context.dp(if (primary) 56 else 48)
            cornerRadius = context.dp(28)
            if (primary) {
                backgroundTintList = ColorStateList.valueOf(GOLD); setTextColor(INK)
            } else {
                backgroundTintList = ColorStateList.valueOf(Color.TRANSPARENT); setTextColor(IVORY)
                strokeColor = ColorStateList.valueOf(LINE); strokeWidth = context.dp(1)
            }
            setOnClickListener(onClick)
        }
        addView(b, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = context.dp(10) })
        return b
    }

    fun LinearLayout.row(build: LinearLayout.() -> Unit): LinearLayout {
        val r = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        addView(r, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = context.dp(8) })
        r.build(); return r
    }

    fun LinearLayout.field(hint: String, value: String = "", number: Boolean = false): EditText {
        val e = EditText(context).apply {
            this.hint = hint; setText(value); setTextColor(IVORY); setHintTextColor(MUTED); typeface = SERIF
            textSize = 16f
            inputType = if (number) InputType.TYPE_CLASS_NUMBER else InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            backgroundTintList = ColorStateList.valueOf(GOLD)
        }
        addView(e, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = context.dp(4) })
        return e
    }

    fun LinearLayout.card(selected: Boolean = false, build: LinearLayout.() -> Unit): MaterialCardView {
        val inner = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL; setPadding(context.dp(16), context.dp(16), context.dp(16), context.dp(16)) }
        val c = MaterialCardView(context).apply {
            radius = context.dp(22).toFloat(); setCardBackgroundColor(SURFACE)
            strokeColor = if (selected) GOLD else LINE; strokeWidth = context.dp(if (selected) 2 else 1)
            cardElevation = 0f
            addView(inner)
        }
        addView(c, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = context.dp(12) })
        inner.build(); return c
    }
}
