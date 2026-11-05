package com.example.hdmimanager

import android.app.Presentation
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Shader
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
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        setShadowLayer(8f, 3f, 3f, Color.argb(140, 0, 0, 0))
    }
    private val hole = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = Color.argb(255, 24, 17, 10) }
    private val shine = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = Color.argb(90, 255, 235, 180)
        setStyle(Paint.Style.STROKE); strokeWidth = 3f
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
        c.drawCircle(cx, cy, r * 0.52f, shine) // reflet métallique
        c.drawRect(cx - size * 0.03f, cy - r * 0.3f, cx + size * 0.03f, cy + r * 0.3f, paint)
        c.restore()
    }
}

// ────────────── Cadre "plaque de laiton rivetée" ──────────────
class BrassPanel(context: Context, light: Int, dark: Int) : LinearLayout(context) {
    private val lightC = light; private val darkC = dark
    private val rivet = Paint(Paint.ANTI_ALIAS_FLAG)
    private val frame = Paint(Paint.ANTI_ALIAS_FLAG).apply { setStyle(Paint.Style.STROKE); strokeWidth = 6f }
    private val grad = LinearGradient(0f, 0f, 0f, height.toFloat(),
        lightC, darkC, Shader.TileMode.CLAMP)

    init {
        orientation = VERTICAL
        gravity = Gravity.CENTER
        setPadding(52, 34, 52, 34)
        setWillNotDraw(false)
    }

    override fun onDraw(c: Canvas) {
        super.onDraw(c)
        val w = width.toFloat(); val h = height.toFloat()
        val radius = 34f
        // Fond laiton avec dégradé
        val bg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(0f, 0f, 0f, h, lighten(lightC, 1.15f), darken(darkC, 0.7f), Shader.TileMode.CLAMP)
        }
        c.drawRoundRect(0f, 0f, w, h, radius, radius, bg)
        // Bordure cuivre foncé + liseré clair
        frame.color = darken(darkC, 0.55f); frame.strokeWidth = 8f
        c.drawRoundRect(3f, 3f, w - 3, h - 3, radius, radius, frame)
        frame.color = Color.argb(120, 255, 230, 170); frame.strokeWidth = 2f
        c.drawRoundRect(10f, 10f, w - 10, h - 10, radius - 7, radius - 7, frame)
        // 4 rivets d'angle
        rivet.shader = RadialGradient(0.35f, 0.35f, 12f,
            Color.rgb(250, 225, 160), Color.rgb(120, 85, 35), Shader.TileMode.CLAMP)
        val rr = 9f
        listOf(Pair(22f, 22f), Pair(w - 22f, 22f), Pair(22f, h - 22f), Pair(w - 22f, h - 22f))
            .forEach { (x, y) ->
                rivet.shader = RadialGradient(x - 3, y - 3, rr * 1.6f,
                    Color.rgb(255, 240, 190), Color.rgb(110, 78, 30), Shader.TileMode.CLAMP)
                c.drawCircle(x, y, rr, rivet)
            }
    }

    private fun lighten(c: Int, f: Float) = Color.rgb(
        (Color.red(c) * f).toInt().coerceAtMost(255),
        (Color.green(c) * f).toInt().coerceAtMost(255),
        (Color.blue(c) * f).toInt().coerceAtMost(255))
    private fun darken(c: Int, f: Float) = Color.rgb(
        (Color.red(c) * f).toInt(), (Color.green(c) * f).toInt(), (Color.blue(c) * f).toInt())
}

// ────────────── Manomètre analogique (cadran + aiguille + verre) ──────────────
class PressureGauge(context: Context) : View(context) {
    private var ratio = 0f
    private val brass = Color.rgb(183, 141, 65)
    private val body = Paint(Paint.ANTI_ALIAS_FLAG).apply { setStyle(Paint.Style.STROKE); strokeWidth = 10f; this.color = brass }
    private val face = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = Color.rgb(252, 246, 228) }
    private val tickP = Paint(Paint.ANTI_ALIAS_FLAG).apply { strokeWidth = 3f; this.color = Color.rgb(90, 60, 20) }
    private val red = Paint(Paint.ANTI_ALIAS_FLAG).apply { strokeWidth = 3f; this.color = Color.rgb(180, 30, 20) }
    private val needle = Paint(Paint.ANTI_ALIAS_FLAG).apply { strokeWidth = 7f; this.color = Color.rgb(200, 40, 20) }
    private val label = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = Color.rgb(70, 45, 15); textSize = 26f; textAlign = Paint.Align.CENTER; typeface = Typeface.SERIF
    }
    private val glass = Paint(Paint.ANTI_ALIAS_FLAG)
    fun setValue(r: Float) { ratio = r.coerceIn(0f, 1f); invalidate() }
    override fun onDraw(c: Canvas) {
        super.onDraw(c)
        val cx = width / 2f; val cy = height * 0.85f; val r = width * 0.42f
        c.drawCircle(cx, cy, r + 10f, body)
        c.drawCircle(cx, cy, r, face)
        for (i in 0..10) {
            val a = Math.PI + Math.PI * i / 10.0
            val p = if (i >= 8) red else tickP
            c.drawLine(cx + (r * 0.82 * Math.cos(a)).toFloat(), cy + (r * 0.82 * Math.sin(a)).toFloat(),
                       cx + (r * Math.cos(a)).toFloat(), cy + (r * Math.sin(a)).toFloat(), p)
        }
        c.drawText("PRESSION", cx, cy - r * 0.42f, label)
        c.drawText("hPa", cx, cy - r * 0.22f, label)
        val a = Math.PI + Math.PI * ratio
        c.drawLine(cx, cy, cx + (r * 0.78 * Math.cos(a)).toFloat(),
                   cy + (r * 0.78 * Math.sin(a)).toFloat(), needle)
        c.drawCircle(cx, cy, 11f, needle)
        // Reflet de verre
        glass.shader = LinearGradient(cx - r, cy - r, cx, cy,
            Color.argb(60, 255, 255, 255), Color.argb(0, 255, 255, 255), Shader.TileMode.CLAMP)
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
    private lateinit var flame: TextView

    private val brass = Color.rgb(183, 141, 65)
    private val copper = Color.rgb(140, 84, 42)
    private val cream = Color.rgb(245, 230, 200)
    private val ink = Color.rgb(48, 30, 12)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val safeName = try { display.name ?: "Écran ${display.displayId}" }
                       catch (e: Exception) { "Écran ${display.displayId}" }

        val bg = GradientDrawable(GradientDrawable.Orientation.TL_BR,
            intArrayOf(Color.rgb(45, 32, 18), Color.rgb(16, 11, 6)))
        bg.setStroke(8, copper)

        fun panel(): BrassPanel {
            val p = BrassPanel(context, Color.rgb(60, 43, 24), Color.rgb(28, 19, 10))
            return p
        }

        fun gear(sz: Float, teeth: Int, color: Int): GearView {
            val g = GearView(context, sz, teeth, color)
            g.layoutParams = LinearLayout.LayoutParams(sz.toInt(), sz.toInt()).apply {
                setMargins(18, 0, 18, 0)
            }
            return g
        }

        fun label(txt: String, size: Float, color: Int, bold: Boolean = false): TextView =
            TextView(context).apply {
                text = txt; textSize = size; setTextColor(color); gravity = Gravity.CENTER
                typeface = Typeface.create("serif", if (bold) Typeface.BOLD else Typeface.NORMAL)
                setShadowLayer(6f, 1f, 1f, Color.argb(120, 0, 0, 0))
            }

        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            background = bg
            setPadding(60, 40, 60, 40)
        }

        val header = LinearLayout(context).apply { gravity = Gravity.CENTER }
        flame = TextView(context).apply { text = "🔥"; textSize = 30f }
        val title = label("☁️ TABLEAU DE BORD VAPEUR ☁️", 32f, brass, true)
        listOf(gear(96f, 12, brass), gear(60f, 9, copper), gear(96f, 12, brass))
            .forEach { header.addView(it) }
        header.addView(flame)
        pulsate(flame)

        clock = TextView(context).apply {
            textSize = 96f; setTextColor(cream); gravity = Gravity.CENTER
            typeface = Typeface.MONOSPACE
            setShadowLayer(18f, 0f, 0f, Color.argb(130, 255, 170, 40))
        }
        dateView = label("", 24f, brass)

        val row1 = LinearLayout(context).apply { gravity = Gravity.CENTER }
        val p1 = panel()
        weatherView = label("⏳ La chaudière monte en pression…", 20f, ink)
        forecastView = label("", 15f, Color.rgb(90, 60, 25))
        p1.addView(weatherView); p1.addView(forecastView)
        val p2 = panel()
        gauge = PressureGauge(context)
        gauge.layoutParams = LinearLayout.LayoutParams(460, 300)
        p2.addView(gauge)
        listOf(p1, p2).forEach { row1.addView(it) }

        val p3 = panel()
        btcView = label("⏳ Les mineurs réchauffent les machines…", 20f, ink)
        p3.addView(btcView)

        statusView = TextView(context).apply {
            text = "⚙ $safeName · machines v2.2 · plaques de laiton rivetées"
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
