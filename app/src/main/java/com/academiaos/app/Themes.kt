package com.academiaos.app

import android.graphics.Color

/**
 * Bir Academia OS teması: duvar kâğıdı + widget kart görünümü.
 * Yeni tema eklemek için: res/raw'a kare (2304x2304) duvar kâğıdı, res/drawable'a kart arka planı
 * ve önizleme görseli koyup bu listeye bir satır ekle.
 */
data class AcademiaTheme(
    val id: String,
    val name: String,
    val tagline: String,
    val wallpaper: Int,
    val preview: Int,
    val card: Int,
    val bar: Int,
    val accent: Int,
)

object Themes {
    val ALL = listOf(
        AcademiaTheme(
            id = "forest",
            name = "Orman Gecesi",
            tagline = "Siyah-yeşil zemin, kuşlar ve çiçek ormanı",
            wallpaper = R.raw.wallpaper_forest,
            preview = R.drawable.preview_forest,
            card = R.drawable.card_forest,
            bar = R.drawable.bar_forest,
            accent = Color.parseColor("#C9A86A"),
        ),
        AcademiaTheme(
            id = "navy",
            name = "Gece Mavisi",
            tagline = "Lacivert zemin, aynı orman, lavanta vurgular",
            wallpaper = R.raw.wallpaper_navy,
            preview = R.drawable.preview_navy,
            card = R.drawable.card_navy,
            bar = R.drawable.bar_navy,
            accent = Color.parseColor("#C9A86A"),
        ),
    )

    fun byId(id: String?): AcademiaTheme = ALL.firstOrNull { it.id == id } ?: ALL[0]
}
