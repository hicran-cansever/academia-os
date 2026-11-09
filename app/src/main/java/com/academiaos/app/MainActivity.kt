package com.academiaos.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.academiaos.app.Ui.button
import com.academiaos.app.Ui.cap
import com.academiaos.app.Ui.card
import com.academiaos.app.Ui.dp
import com.academiaos.app.Ui.page
import com.academiaos.app.Ui.text
import com.academiaos.app.Ui.title
import java.util.concurrent.Executors

/** Tema Merkezi: seç, tek tuşla uygula, kurulumu tamamla. */
class MainActivity : AppCompatActivity() {
    private val io = Executors.newSingleThreadExecutor()
    private var selected: AcademiaTheme = Themes.ALL[0]
    private lateinit var status: TextView
    private lateinit var themeBox: LinearLayout
    private var chainRunning = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        selected = Store.theme(this)
        build()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
    }

    override fun onResume() {
        super.onResume()
        // Widget ekleme penceresi kapanınca sıradakini aç.
        if (chainRunning) chainRunning = PinHelper.requestNext(this)
        refreshStatus()
    }

    private fun build() {
        page {
            title("Academia OS", 36f)
            text("Kuşlu, çiçekli kişisel çalışma temam. Bir tema seç ve tek tuşla uygula.")

            cap("Tema seç")
            themeBox = LinearLayout(this@MainActivity).apply { orientation = LinearLayout.VERTICAL }
            addView(themeBox)
            drawThemes()

            button("Bu temayı uygula", primary = true) { applyTheme() }
            status = text("", 14f)
            refreshStatus()

            cap("Kurulumu tamamla")
            text("1 · Widget'lar: ekleme pencereleri sırayla açılır, her birinde “Ekle”ye dokun.", 14f)
            button("Widget'ları ana ekrana ekle") {
                chainRunning = PinHelper.startChain(this@MainActivity)
                if (!chainRunning) {
                    val left = PinHelper.missing(this@MainActivity).size
                    Toast.makeText(this@MainActivity,
                        if (left == 0) "Tüm widget'lar zaten ekranda." else "Ana ekran bu isteği desteklemiyor; widget'ları ana ekranda uzun basıp ekleyebilirsin.",
                        Toast.LENGTH_LONG).show()
                }
            }
            text("2 · İkonlar (bir kere): Samsung, uygulamaların ikon değiştirmesine izin vermiyor. " +
                "İkonların Galeri › AcademiaOS › Ikonlar klasörüne kaydedildi. Theme Park › İkon › Oluştur'da " +
                "her uygulamaya kendi kuşlu ikonunu seç ve kaydet.", 14f)
            button(if (SystemLinks.themeParkInstalled(this@MainActivity)) "Theme Park'ı aç" else "Theme Park'ı yükle") {
                if (!SystemLinks.openThemePark(this@MainActivity))
                    Toast.makeText(this@MainActivity, "Galaxy Store'da “Theme Park” ara.", Toast.LENGTH_LONG).show()
            }
            text("3 · Koyu mod: sistem ayarından açılır; tema koyu modla en güzel görünür.", 14f)
            button("Ekran ayarlarını aç") { SystemLinks.openDisplaySettings(this@MainActivity) }
            text("4 · Takvim widget'ında etkinlik noktaları için (isteğe bağlı):", 14f)
            button("Takvime erişim ver") { requestPermissions(arrayOf(Manifest.permission.READ_CALENDAR), 2) }

            cap("Verilerim")
            button("Görevler") { startActivity(Intent(this@MainActivity, TasksActivity::class.java)) }
            button("Okuduğum kitap") { startActivity(Intent(this@MainActivity, ReadingActivity::class.java)) }
            button("Dersler ve odak süresi") { startActivity(Intent(this@MainActivity, SubjectsActivity::class.java)) }

            cap("Diğer")
            button("İkon eşleştirme listesi (diğer ana ekranlar için)") { showComponents() }
            button("Varsayılan duvar kâğıdına dön") {
                io.execute {
                    runCatching { ThemeApplier.restoreDefaultWallpaper(this@MainActivity) }
                    runOnUiThread { Toast.makeText(this@MainActivity, "Varsayılan duvar kâğıdı geri yüklendi.", Toast.LENGTH_SHORT).show() }
                }
            }
        }
    }

    private fun drawThemes() {
        themeBox.removeAllViews()
        for (t in Themes.ALL) {
            val isSel = t.id == selected.id
            val cardView = themeBox.card(isSel) {
                val img = ImageView(context).apply {
                    setImageResource(t.preview); adjustViewBounds = true
                    contentDescription = "${t.name} önizleme"
                }
                addView(img, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))
                title((if (isSel) "● " else "") + t.name, 20f).also { (it.layoutParams as LinearLayout.LayoutParams).topMargin = dp(10) }
                text(t.tagline, 13f)
            }
            cardView.setOnClickListener { selected = t; drawThemes() }
        }
    }

    private fun applyTheme() {
        status.text = "Uygulanıyor…"
        val t = selected
        io.execute {
            val r = ThemeApplier.apply(this, t)
            runOnUiThread {
                drawThemes()
                val lines = buildString {
                    append(if (r.wallpaper) "✓ Ana ekran ve kilit ekranı duvar kâğıdı: ${t.name}\n" else "✗ Duvar kâğıdı ayarlanamadı: ${r.error}\n")
                    append("✓ Widget'lar ${t.name} görünümüne geçti\n")
                    append("✓ ${r.iconsSaved} ikon Galeri'ye kaydedildi\n")
                }
                status.text = lines + statusTail()
                // Eksik widget varsa ekleme zincirini başlat.
                if (PinHelper.missing(this).isNotEmpty()) chainRunning = PinHelper.startChain(this)
            }
        }
    }

    private fun statusTail(): String {
        val left = PinHelper.missing(this).size
        return if (left == 0) "✓ Tüm widget'lar ana ekranda" else "• Ana ekranda olmayan widget: $left"
    }

    private fun refreshStatus() {
        if (::status.isInitialized && status.text.isNullOrEmpty()) {
            status.text = "Şu anki tema: ${Store.theme(this).name}\n" + statusTail()
        } else if (::status.isInitialized) {
            status.text = status.text.toString().substringBeforeLast("\n").let { "$it\n${statusTail()}" }
        }
    }

    /** Diğer ana ekranlarda ikon paketi eşleşmezse doğru bileşen adlarını görmek için. */
    private fun showComponents() {
        val wanted = listOf("Notes", "Not", "Takvim", "Calendar", "Hatırlatıcı", "Reminder", "Dosyalar", "Files",
            "Galeri", "Gallery", "Chrome", "YouTube", "Moodreader", "Flexcil", "Health", "Ayarlar", "Settings",
            "Hesap", "Calculator", "Kamera", "Camera", "Play Store", "Saat", "Clock", "PENUP")
        val main = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val list = packageManager.queryIntentActivities(main, 0)
            .map { it.loadLabel(packageManager).toString() to "${it.activityInfo.packageName}/${it.activityInfo.name}" }
            .filter { (label, _) -> wanted.any { label.contains(it, ignoreCase = true) } }
            .sortedBy { it.first }
            .joinToString("\n\n") { "${it.first}\n${it.second}" }
        AlertDialog.Builder(this)
            .setTitle("Uygulama bileşenleri")
            .setMessage(list.ifEmpty { "Eşleşen uygulama bulunamadı." })
            .setPositiveButton("Kapat", null)
            .show()
    }
}
