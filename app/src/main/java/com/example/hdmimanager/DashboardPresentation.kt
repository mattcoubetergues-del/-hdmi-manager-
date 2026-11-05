package com.example.hdmimanager

import android.app.Presentation
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
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

// ────────────── Engrenage dessiné (dents réelles, rotation continue) ──────────────
class GearView(context: Context, private val size: Float, private val teeth: Int, color: Int) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color }
    private val hole = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = Color.argb(255, 24, 17, 10) }
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
        c.drawRect(cx - size * 0.03f, cy - r * 0.3f, cx + size * 0.03f, cy + r * 0.3f, paint)
        c.restore()
    }
}

// ────────────── Manomètre analogique (cadran + aiguille) ──────────────
class PressureGauge(context: Context) : View(context) {
    private var ratio = 0f
    private val brass = Color.rgb(183, 141, 65)
    private val body = Paint(Paint.ANTI_ALIAS_FLAG).apply { setStyle(Paint.Style.STROKE); strokeWidth = 8f; this.color = brass }
    private val tickP = Paint(Paint.ANTI_ALIAS_FLAG).apply { strokeWidth = 3f; this.color = brass }
    private val needle = Paint(Paint.ANTI_ALIAS_FLAG).apply { strokeWidth = 6f; this.color = Color.rgb(255, 120, 40) }
    private val label = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = brass; textSize = 30f; textAlign = Paint.Align.CENTER; typeface = Typeface.SERIF
    }
    fun setValue(r: Float) { ratio = r.coerceIn(0f, 1f); invalidate() }
    override fun onDraw(c: Canvas) {
        super.onDraw(c)
        val cx = width / 2f; val cy = height * 0.85f; val r = width * 0.42f
        c.drawCircle(cx, cy, r, body)
        for (i in 0..10) {
            val a = Math.PI + Math.PI * i / 10.0
            c.drawLine(cx + (r * 0.82 * Math.cos(a)).toFloat(), cy + (r * 0.82 * Math.sin(a)).toFloat(),
                       cx + (r * Math.cos(a)).toFloat(), cy + (r * Math.sin(a)).toFloat(), tickP)
        }
        c.drawText("PRESSION hPa", cx, cy - r * 0.35f, label)
        val a = Math.PI + Math.PI * ratio
        c.drawLine(cx, cy, cx + (r * 0.78 * Math.cos(a)).toFloat(),
                   cy + (r * 0.78 * Math.sin(a)).toFloat(), needle)
        c.drawCircle(cx, cy, 10f, needle)
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
    private lateinit var flame: TextView

    private val brass = Color.rgb(183, 141, 65)
    private val copper = Color.rgb(140, 84, 42)
    private val cream = Color.rgb(245, 230, 200)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val safeName = try { display.name ?: "Écran ${display.displayId}" }
                       catch (e: Exception) { "Écran ${display.displayId}" }

        val bg = GradientDrawable(GradientDrawable.Orientation.TL_BR,
            intArrayOf(Color.rgb(40, 29, 17), Color.rgb(18, 12, 7)))
        bg.setStroke(8, copper)

        fun card(): LinearLayout {
            val d = GradientDrawable().apply {
                cornerRadius = 30f; setStroke(4, copper)
                setColor(Color.argb(100, 30, 21, 12))
            }
            return LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                background = d
                setPadding(44, 26, 44, 26)
            }
        }

        fun gear(sz: Float, teeth: Int, color: Int): GearView {
            val g = GearView(context, sz, teeth, color)
            g.layoutParams = LinearLayout.LayoutParams(sz.toInt(), sz.toInt()).apply {
                setMargins(18, 0, 18, 0)
            }
            return g
        }

        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            background = bg
            setPadding(60, 40, 60, 40)
        }

        val header = LinearLayout(context).apply { gravity = Gravity.CENTER }
        flame = TextView(context).apply { text = "🔥"; textSize = 30f }
        val title = TextView(context).apply {
            text = "☁️ TABLEAU DE BORD VAPEUR ☁️"
            textSize = 32f; setTextColor(brass)
            typeface = Typeface.create("serif", Typeface.BOLD)
            setShadowLayer(10f, 0f, 0f, Color.argb(150, 255, 190, 80))
        }
        // Grand engrenages à gauche, petit au centre, grand à droite — sens alternés
        listOf(gear(96f, 12, brass), gear(60f, 9, copper), gear(96f, 12, brass))
            .forEach { header.addView(it) }
        pulsate(flame)

        clock = TextView(context).apply {
            textSize = 96f; setTextColor(cream); gravity = Gravity.CENTER
            typeface = Typeface.MONOSPACE
            setShadowLayer(18f, 0f, 0f, Color.argb(130, 255, 170, 40))
        }
        dateView = TextView(context).apply {
            textSize = 24f; setTextColor(brass); gravity = Gravity.CENTER
            typeface = Typeface.create("serif", Typeface.ITALIC)
        }

        val row1 = LinearLayout(context).apply { gravity = Gravity.CENTER }
        val p1 = card()
        weatherView = TextView(context).apply {
            text = "⏳ La chaudière monte en pression…"
            textSize = 20f; setTextColor(cream); gravity = Gravity.CENTER; typeface = Typeface.SERIF
        }
        forecastView = TextView(context).apply {
            textSize = 16f; setTextColor(brass); gravity = Gravity.CENTER; typeface = Typeface.SERIF
        }
        p1.addView(weatherView); p1.addView(forecastView)
        val p2 = card()
        gauge = PressureGauge(context)
        gauge.layoutParams = LinearLayout.LayoutParams(460, 300)
        p2.addView(gauge)
        listOf(p1, p2).forEach { row1.addView(it) }

        val p3 = card()
        btcView = TextView(context).apply {
            text = "⏳ Les mineurs réchauffent les machines…"
            textSize = 20f; setTextColor(cream); gravity = Gravity.CENTER; typeface = Typeface.SERIF
        }
        p3.addView(btcView)

        statusView = TextView(context).apply {
            text = "⚙ $safeName · machines v2.1"
            textSize = 14f; setTextColor(Color.GRAY); gravity = Gravity.CENTER
        }
        val closeBtn = Button(context).apply { text = "🕯️ Éteindre la machine" }
        closeBtn.setOnClickListener { dismiss() }

        listOf(header, clock, dateView, row1, p3, closeBtn, statusView).forEach {
            root.addView(it, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(0, 18, 0, 18) })
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
                    "🌡️ %.1f°C   💨 %.0f km/h   💧 %d%%",
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
                    "⛏️ BITCOIN  %s €  ·  %s $   %s %.1f%% (24 h)",
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
