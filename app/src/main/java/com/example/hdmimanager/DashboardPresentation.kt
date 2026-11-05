package com.example.hdmimanager

import android.app.Presentation
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.graphics.Typeface
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import android.view.Display
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import org.json.JSONObject

// ── Palette "machine de nuit" ──

class GearView(context: Context, sizePx: Float, private val teeth: Int, color: Int) : View(context) {
    private val size = sizePx
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        setShadowLayer(size * 0.10f, 0f, 0f, Color.argb(160, 255, 140, 40))
    }
    private val hole = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = Color.rgb(14, 10, 6) }
    private val shine = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = Color.argb(110, 255, 235, 180)
        setStyle(Paint.Style.STROKE); strokeWidth = size * 0.035f
    }
    private var angle = 0f
    private val main = Handler(Looper.getMainLooper())
    private val dir = if (teeth % 2 == 0) 1f else -1f

    private val spin = object : Runnable {
        override fun run() {
            angle += 0.6f * dir
            invalidate()
            main.postDelayed(this, 16)
        }
    }
    override fun onAttachedToWindow() { super.onAttachedToWindow(); main.post(spin) }
    override fun onDetachedFromWindow() { main.removeCallbacks(spin); super.onDetachedFromWindow() }
    override fun onDraw(c: Canvas) {
        super.onDraw(c)
        val cx = width / 2f; val cy = height / 2f; val r = size / 2f
        c.save(); c.rotate(angle, cx, cy)
        val tooth = 2f * Math.PI.toFloat() / teeth
        for (i in 0 until teeth) {
            c.save(); c.rotate(i * tooth * 57.2958f, cx, cy)
            c.drawRect(cx - size * 0.055f, cy - r, cx + size * 0.055f, cy - r * 0.72f, paint)
            c.restore()
        }
        c.drawCircle(cx, cy, r * 0.74f, paint)
        c.drawCircle(cx, cy, r * 0.30f, hole)
        c.drawCircle(cx, cy, r * 0.52f, shine)
        c.drawRect(cx - size * 0.03f, cy - r * 0.3f, cx + size * 0.03f, cy + r * 0.3f, paint)
        c.restore()
    }
}

// ── Panneau charbon, bordure cuivre, rivets — toutes dimensions proportionnelles ──
class BrassPanel(context: Context, scale: Float) : LinearLayout(context) {
    private val s: (Float) -> Float = { v: Float -> v * scale }
    private val rivet = Paint(Paint.ANTI_ALIAS_FLAG)
    private val frame = Paint(Paint.ANTI_ALIAS_FLAG).apply { setStyle(Paint.Style.STROKE) }
    private val copper = Color.rgb(206, 120, 52)
    private val copperGlow = Color.argb(150, 255, 150, 60)

    init {
        orientation = VERTICAL
        gravity = Gravity.CENTER
        setPadding(s(56).toInt(), s(40).toInt(), s(56).toInt(), s(40).toInt())
        setWillNotDraw(false)
    }

    override fun onDraw(c: Canvas) {
        super.onDraw(c)
        val w = width.toFloat(); val h = height.toFloat(); val radius = s(30)
        val bg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(0f, 0f, 0f, h,
                Color.rgb(38, 30, 22), Color.rgb(18, 13, 8), Shader.TileMode.CLAMP)
        }
        c.drawRoundRect(0f, 0f, w, h, radius, radius, bg)
        frame.color = copperGlow; frame.strokeWidth = s(10)
        c.drawRoundRect(s(5), s(5), w - s(5), h - s(5), radius, radius, frame)
        frame.color = copper; frame.strokeWidth = s(5)
        c.drawRoundRect(s(5), s(5), w - s(5), h - s(5), radius, radius, frame)
        frame.color = Color.argb(90, 250, 235, 200); frame.strokeWidth = s(2)
        c.drawRoundRect(s(14), s(14), w - s(14), h - s(14), radius - s(9), radius - s(9), frame)
        val m = s(24); val rr = s(10)
        val corners = listOf(m to m, w - m to m, m to h - m, w - m to h - m)
        for (corner in corners) {
            val x = corner.first; val y = corner.second
            rivet.shader = RadialGradient(x - rr * 0.3f, y - rr * 0.3f, rr * 1.8f,
                Color.rgb(255, 200, 120), Color.rgb(90, 50, 18), Shader.TileMode.CLAMP)
            c.drawCircle(x, y, rr, rivet)
        }
    }
}

// ── Manomètre — tout est calculé depuis la taille de la vue, donc suit le scale ──
class PressureGauge(context: Context) : View(context) {
    private var ratio = 0f
    private val copper = Color.rgb(206, 120, 52)
    private val body = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        setStyle(Paint.Style.STROKE); this.color = copper
        setShadowLayer(12f, 0f, 0f, Color.argb(140, 255, 140, 40))
    }
    private val face = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = Color.rgb(30, 24, 16) }
    private val tickP = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = Color.rgb(245, 230, 200) }
    private val red = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = Color.rgb(255, 70, 50) }
    private val needle = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = Color.rgb(255, 80, 55) }
    private val label = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = Color.rgb(245, 225, 180); textAlign = Paint.Align.CENTER; typeface = Typeface.SERIF
    }
    private val glass = Paint(Paint.ANTI_ALIAS_FLAG)
    fun setValue(r: Float) { ratio = r.coerceIn(0f, 1f); invalidate() }
    override fun onDraw(c: Canvas) {
        super.onDraw(c)
        val cx = width / 2f; val cy = height * 0.85f; val r = width * 0.42f
        body.strokeWidth = r * 0.045f
        tickP.strokeWidth = r * 0.015f
        red.strokeWidth = r * 0.015f
        needle.strokeWidth = r * 0.03f
        label.textSize = r * 0.14f
        c.drawCircle(cx, cy, r + body.strokeWidth * 0.6f, body)
        c.drawCircle(cx, cy, r, face)
        for (i in 0..10) {
            val a = Math.PI + Math.PI * i / 10.0
            val p = if (i >= 8) red else tickP
            c.drawLine(cx + (r * 0.82 * Math.cos(a)).toFloat(), cy + (r * 0.82 * Math.sin(a)).toFloat(),
                       cx + (r * Math.cos(a)).toFloat(), cy + (r * Math.sin(a)).toFloat(), p)
        }
        c.drawText("PRESSION", cx, cy - r * 0.40f, label)
        c.drawText("hPa", cx, cy - r * 0.20f, label)
        val a = Math.PI + Math.PI * ratio
        c.drawLine(cx, cy, cx + (r * 0.78 * Math.cos(a)).toFloat(),
                   cy + (r * 0.78 * Math.sin(a)).toFloat(), needle)
        c.drawCircle(cx, cy, r * 0.045f, needle)
        glass.shader = LinearGradient(cx - r, cy - r, cx, cy,
            Color.argb(55, 255, 255, 255), Color.argb(0, 255, 255, 255), Shader.TileMode.CLAMP)
        c.drawCircle(cx, cy, r, glass)
    }
}

class DashboardPresentation(context: Context, display: Display) : Presentation(context, display) {

    private val main = Handler(Looper.getMainLooper())
    private val fmt = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    private val fmtDate = SimpleDateFormat("EEEE d MMMM yyyy", Locale.FRENCH)
    private lateinit var clock: TextView
    private lateinit var dateView: TextView
    private lateinit var weatherView: TextView
    private lateinit var forecastView: TextView
    private lateinit var btcView: TextView
    private lateinit var statusView: TextView
    private lateinit var gauge: PressureGauge

    private val copper = Color.rgb(206, 120, 52)
    private val cream = Color.rgb(248, 236, 210)
    private val soft = Color.rgb(210, 190, 160)

    // Facteur d'échelle calculé depuis la VRAIE résolution de l'écran cible
    private lateinit var s: (Float) -> Float

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Résolution réelle de l'écran HDMI (pas celle du téléphone)
        val metrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        display.getRealMetrics(metrics)
        // Base de conception : 1920×1080. On prend le facteur minimal
        // pour garantir que tout reste visible, puis on garde 1 décimale de marge.
        val scale = minOf(metrics.widthPixels / 1920f, metrics.heightPixels / 1080f) * 0.97f
        s = { v: Float -> v * scale }

        val safeName = try { display.name ?: "Écran ${display.displayId}" }
                       catch (e: Exception) { "Écran ${display.displayId}" }
        val density = metrics.density

        // dp = px / densité : on convertit nos px scalés en dp pour les tailles de texte
        fun sp(v: Float) = s(v) / density

        val bg = android.graphics.drawable.GradientDrawable(
            android.graphics.drawable.GradientDrawable.Orientation.TL_BR,
            intArrayOf(Color.rgb(24, 18, 11), Color.rgb(8, 6, 3)))
        bg.setStroke(s(6).toInt(), copper)

        fun gear(szDp: Float, teeth: Int, color: Int): GearView {
            val px = s(szDp)
            val g = GearView(context, px, teeth, color)
            g.layoutParams = LinearLayout.LayoutParams(px.toInt(), px.toInt()).apply {
                setMargins(s(16).toInt(), 0, s(16).toInt(), 0)
            }
            return g
        }

        fun label(txt: String, sizeDp: Float, color: Int, bold: Boolean = false): TextView =
            TextView(context).apply {
                text = txt; setTextColor(color); gravity = Gravity.CENTER
                textSize = sp(sizeDp)
                typeface = Typeface.create("sans-serif", if (bold) Typeface.BOLD else Typeface.NORMAL)
                setShadowLayer(sp(4), 0f, sp(2), Color.argb(160, 0, 0, 0))
            }

        fun wideParams(): LinearLayout.LayoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { setMargins(0, s(20).toInt(), 0, s(20).toInt()) }

        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            background = bg
            setPadding(s(70).toInt(), s(50).toInt(), s(70).toInt(), s(50).toInt())
        }

        // ── En-tête symétrique ──
        val header = LinearLayout(context).apply { gravity = Gravity.CENTER }
        val flame = TextView(context).apply { text = "🔥"; textSize = sp(28) }
        header.addView(gear(88f, 12, copper))
        header.addView(gear(56f, 9, Color.rgb(150, 100, 50)))
        header.addView(label("◈ TABLEAU DE BORD VAPEUR ◈", 34f, cream, true))
        header.addView(gear(56f, 9, Color.rgb(150, 100, 50)))
        header.addView(gear(88f, 12, copper))
        header.addView(flame)
        pulsate(flame)

        // ── Horloge géante ──
        clock = TextView(context).apply {
            textSize = sp(120); setTextColor(cream); gravity = Gravity.CENTER
            typeface = Typeface.MONOSPACE
            setShadowLayer(sp(24), 0f, 0f, Color.argb(150, 255, 150, 40))
        }
        dateView = label("", 26f, copper, true)

        // ── Rangée météo / manomètre ──
        val row = LinearLayout(context).apply { gravity = Gravity.CENTER }
        val p1 = BrassPanel(context, scale)
        p1.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            .apply { setMargins(0, 0, s(24).toInt(), 0) }
        weatherView = label("⏳ La chaudière monte en pression…", 24f, cream, true)
        forecastView = label("", 19f, soft)
        p1.addView(weatherView); p1.addView(forecastView)
        val p2 = BrassPanel(context, scale)
        p2.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            .apply { setMargins(s(24).toInt(), 0, 0, 0) }
        gauge = PressureGauge(context)
        gauge.layoutParams = LinearLayout.LayoutParams(s(500).toInt(), s(320).toInt())
        p2.addView(gauge)
        row.addView(p1); row.addView(p2)

        // ── Bitcoin ──
        val p3 = BrassPanel(context, scale)
        btcView = label("⏳ Les mineurs réchauffent les machines…", 26f, cream, true)
        p3.addView(btcView)

        statusView = TextView(context).apply {
            text = "⚙ $safeName · ${metrics.widthPixels}×${metrics.heightPixels} · v2.4"
            textSize = sp(15); setTextColor(Color.rgb(150, 130, 100)); gravity = Gravity.CENTER
        }
        val closeBtn = Button(context).apply { text = "🕯️ Éteindre la machine"; textSize = sp(18) }
        closeBtn.setOnClickListener { dismiss() }

        listOf(header, clock, dateView, row, p3, closeBtn, statusView).forEach {
            root.addView(it, wideParams())
        }
        setContentView(root)

        main.post(tick)
        refreshApis()
    }

    private fun pulsate(v: TextView) {
        v.animate().scaleX(1.3f).scaleY(1.3f).setDuration(500).withEndAction {
            v.animate().scaleX(1f).scaleY(1f).setDuration(500).withEndAction { pulsate(v) }.start()
        }.start()
    }

    private fun refreshApis() {
        Thread { fetchWeather() }.start()
        Thread { fetchBitcoin() }.start()
        main.postDelayed({ refreshApis() }, 600_000L)
    }

    private fun get(url: String): String =
        (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 8000; readTimeout = 8000
            setRequestProperty("Accept", "application/json")
        }.inputStream.bufferedReader().readText()

    private fun fetchWeather() {
        try {
            val json = JSONObject(get("https://api.open-meteo.com/v1/forecast?latitude=48.85&longitude=2.35&current=temperature_2m,wind_speed_10m,relative_humidity_2m,surface_pressure&hourly=temperature_2m&forecast_days=1"))
            val c = json.getJSONObject("current")
            val h = json.getJSONObject("hourly")
            val times = h.getJSONArray("time")
            val temps = h.getJSONArray("temperature_2m")
            val nowHour = SimpleDateFormat("HH", Locale.US).format(Date()).toInt()
            val sb = StringBuilder("PRÉVISIONS : ")
            var count = 0
            for (i in 0 until times.length()) {
                if (count >= 3) break
                val hr = times.getString(i).substring(11, 13).toInt()
                if (hr > nowHour && hr % 3 == 0) {
                    sb.append(String.format(Locale.FRANCE, "%02dh %.0f°C   ", hr, temps.getDouble(i)))
                    count++
                }
            }
            val pressure = c.getDouble("surface_pressure")
            main.post {
                weatherView.text = String.format(Locale.FRANCE,
                    "🌡️ %.1f°C    💨 %.0f km/h    💧 %d%%",
                    c.getDouble("temperature_2m"), c.getDouble("wind_speed_10m"),
                    c.getInt("relative_humidity_2m"))
                forecastView.text = sb.toString()
                gauge.setValue((pressure / 1100.0).toFloat())
            }
        } catch (e: Exception) {
            main.post { weatherView.text = "🌡️ Vapeurs indisponibles : ${e.message}" }
        }
    }

    private fun fetchBitcoin() {
        try {
            val json = JSONObject(get("https://api.coingecko.com/api/v3/simple/price?ids=bitcoin&vs_currencies=eur,usd&include_24hr_change=true"))
            val btc = json.getJSONObject("bitcoin")
            val chg = btc.getDouble("eur_24h_change")
            val arrow = if (chg >= 0) "📈" else "📉"
            main.post {
                btcView.text = String.format(Locale.FRANCE,
                    "⛏️ BITCOIN   %s €   ·   %s $    %s %.1f%% (24 h)",
                    java.text.NumberFormat.getIntegerInstance(Locale.FRANCE).format(btc.getLong("eur")),
                    java.text.NumberFormat.getIntegerInstance(Locale.US).format(btc.getLong("usd")),
                    arrow, chg)
            }
        } catch (e: Exception) {
            main.post { btcView.text = "⛏️ Machine hors ligne : ${e.message}" }
        }
    }

    private val tick = object : Runnable {
        override fun run() {
            if (::clock.isInitialized) {
                clock.text = fmt.format(Date())
                dateView.text = fmtDate.format(Date()).replaceFirstChar { it.uppercase() }
            }
            main.postDelayed(this, 1000)
        }
    }

    override fun onStop() {
        main.removeCallbacksAndMessages(null)
        super.onStop()
    }
}
