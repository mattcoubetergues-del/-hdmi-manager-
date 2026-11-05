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
import android.widget.LinearLayout
import android.widget.TextView
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import org.json.JSONObject

// ══════════ Engrenage animé ══════════
class GearView(ctx: Context, sizePx: Float, private val teeth: Int, color: Int) : View(ctx) {
    private val size = sizePx
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        setShadowLayer(size * 0.10f, 0f, 0f, Color.argb(160, 255, 140, 40))
    }
    private val hole = Paint(Paint.ANTI_ALIAS_FLAG).also { it.color = Color.rgb(14, 10, 6) }
    private var angle = 0f
    private val main = Handler(Looper.getMainLooper())
    private val dir = if (teeth % 2 == 0) 1f else -1f
    private val spin = object : Runnable {
        override fun run() { angle += 0.6f * dir; invalidate(); main.postDelayed(this, 16) }
    }
    override fun onAttachedToWindow() { super.onAttachedToWindow(); main.post(spin) }
    override fun onDetachedFromWindow() { main.removeCallbacks(spin); super.onDetachedFromWindow() }
    override fun onDraw(c: Canvas) {
        super.onDraw(c)
        val cx = width / 2f; val cy = height / 2f; val r = size / 2f
        c.save(); c.rotate(angle, cx, cy)
        val step = (2.0 * Math.PI / teeth).toFloat()
        var i = 0
        while (i < teeth) {
            c.save(); c.rotate(Math.toDegrees(step.toDouble()).toFloat() * i, cx, cy)
            c.drawRect(cx - r * 0.09f, cy - r * 1.12f, cx + r * 0.09f, cy - r * 0.82f, paint)
            c.restore(); i++
        }
        c.drawCircle(cx, cy, r * 0.85f, paint)
        c.drawCircle(cx, cy, r * 0.32f, hole)
        c.restore()
    }
}

// ══════════ Panneau cuivré ══════════
class BrassPanel(ctx: Context, private val scale: Float) : LinearLayout(ctx) {
    private fun s(v: Float): Float = v * scale
    private val frame = Paint(Paint.ANTI_ALIAS_FLAG).apply { setStyle(Paint.Style.STROKE) }
    private val bg = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rivet = Paint(Paint.ANTI_ALIAS_FLAG)
    private val copper = Color.rgb(206, 120, 52)
    private val glow = Color.argb(150, 255, 150, 60)

    init {
        orientation = VERTICAL
        gravity = Gravity.CENTER
        setPadding(s(56f).toInt(), s(40f).toInt(), s(56f).toInt(), s(40f).toInt())
        setWillNotDraw(false)
    }

    override fun onDraw(c: Canvas) {
        super.onDraw(c)
        val w = width.toFloat(); val h = height.toFloat(); val r = s(30f)
        bg.shader = LinearGradient(0f, 0f, 0f, h, Color.rgb(38, 30, 22), Color.rgb(18, 13, 8), Shader.TileMode.CLAMP)
        c.drawRoundRect(0f, 0f, w, h, r, r, bg)
        frame.color = glow; frame.strokeWidth = s(10f)
        c.drawRoundRect(s(5f), s(5f), w - s(5f), h - s(5f), r, r, frame)
        frame.color = copper; frame.strokeWidth = s(5f)
        c.drawRoundRect(s(5f), s(5f), w - s(5f), h - s(5f), r, r, frame)
        frame.color = Color.argb(90, 250, 235, 200); frame.strokeWidth = s(2f)
        c.drawRoundRect(s(14f), s(14f), w - s(14f), h - s(14f), r - s(9f), r - s(9f), frame)
        val m = s(24f); val rr = s(10f)
        val xs = floatArrayOf(m, w - m, m, w - m)
        val ys = floatArrayOf(m, m, h - m, h - m)
        var i = 0
        while (i < 4) {
            rivet.shader = RadialGradient(xs[i] - rr * 0.3f, ys[i] - rr * 0.3f, rr * 1.8f,
                Color.rgb(255, 200, 120), Color.rgb(90, 50, 18), Shader.TileMode.CLAMP)
            c.drawCircle(xs[i], ys[i], rr, rivet)
            i++
        }
    }
}

// ══════════ Manomètre ══════════
class GaugeView(ctx: Context, private val sizePx: Float, private val label: String) : View(ctx) {
    private fun dp(v: Float): Float = v * sizePx / 300f
    private val dial = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(24, 18, 12) }
    private val tick = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(245, 235, 210); strokeWidth = dp(3f); setStyle(Paint.Style.STROKE) }
    private val redzone = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(200, 200, 40, 40); setStyle(Paint.Style.STROKE); strokeWidth = dp(12f) }
    private val needleP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(255, 170, 60); strokeWidth = dp(5f) }
    private val txt = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(245, 235, 210); textSize = dp(22f); textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD }
    private var value = 0f // 0..1

    fun setValue(v: Float) { value = v.coerceIn(0f, 1f); invalidate() }

    override fun onDraw(c: Canvas) {
        super.onDraw(c)
        val cx = width / 2f; val cy = height * 0.58f; val r = sizePx / 2f * 0.9f
        c.drawCircle(cx, cy, r, dial)
        val start = 150f; val sweep = 240f
        c.drawArc(cx - r, cy - r, cx + r, cy + r, start + sweep * 0.75f, sweep * 0.25f, false, redzone)
        var i = 0
        while (i <= 12) {
            val a = Math.toRadians((start + sweep * i / 12f).toDouble())
            val long = i % 3 == 0
            val r1 = r * (if (long) 0.72f else 0.80f)
            c.drawLine(cx + r1 * Math.cos(a).toFloat(), cy + r1 * Math.sin(a).toFloat(),
                       cx + r * 0.90f * Math.cos(a).toFloat(), cy + r * 0.90f * Math.sin(a).toFloat(), tick)
            i++
        }
        val a = Math.toRadians((start + sweep * value).toDouble())
        c.drawLine(cx, cy, cx + r * 0.70f * Math.cos(a).toFloat(), cy + r * 0.70f * Math.sin(a).toFloat(), needleP)
        c.drawCircle(cx, cy, dp(8f), needleP)
        c.drawText(label, cx, cy + r * 0.55f, txt)
    }
}

// ══════════ Dashboard ══════════
class DashboardPresentation(ctx: Context, private val display: Display) : Presentation(ctx, display) {
    private fun s(v: Float): Float = v * scale
    private var scale: Float = 1f
    private lateinit var clock: TextView
    private lateinit var dateView: TextView
    private lateinit var btcView: TextView
    private lateinit var gauge: GaugeView
    private lateinit var statusView: TextView
    private val main = Handler(Looper.getMainLooper())
    private val fmt = SimpleDateFormat("HH:mm:ss", Locale.FRANCE)
    private val fmtDate = SimpleDateFormat("EEEE d MMMM yyyy", Locale.FRANCE)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val m = DisplayMetrics()
        @Suppress("DEPRECATION")
        display.getRealMetrics(m)
        scale = Math.min(m.widthPixels / 1920f, m.heightPixels / 1080f)

        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.rgb(14, 10, 6))
            setPadding(s(40f).toInt(), s(30f).toInt(), s(40f).toInt(), s(30f).toInt())
        }

        // Titre + engrenages
        val titleRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER
            addView(GearView(context, s(90f), 12, Color.rgb(206, 120, 52)),
                LinearLayout.LayoutParams(s(100f).toInt(), s(100f).toInt()))
            addView(TextView(context).apply {
                text = "⌈ CONSOLE PRINCIPALE ⌋"
                textSize = s(46f)
                setTextColor(Color.rgb(245, 235, 210))
                typeface = Typeface.DEFAULT_BOLD
                gravity = Gravity.CENTER
            }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            addView(GearView(context, s(90f), 9, Color.rgb(206, 120, 52)),
                LinearLayout.LayoutParams(s(100f).toInt(), s(100f).toInt()))
        }

        // Horloge
        clock = TextView(context).apply {
            textSize = s(120f); setTextColor(Color.rgb(255, 200, 110))
            typeface = Typeface.MONOSPACE; gravity = Gravity.CENTER
        }
        dateView = TextView(context).apply {
            textSize = s(28f); setTextColor(Color.rgb(200, 180, 150))
            gravity = Gravity.CENTER
        }

        // Panneau météo + jauge
        val weatherText = TextView(context).apply {
            textSize = s(26f); setTextColor(Color.rgb(245, 235, 210)); gravity = Gravity.CENTER
            setPadding(0, s(20f).toInt(), 0, s(20f).toInt())
        }
        gauge = GaugeView(context, s(260f), "CHARGE")
        gauge.setValue(0.62f)
        val midRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER
            addView(BrassPanel(context, scale).apply {
                addView(weatherText)
            }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f).apply {
                setMargins(s(12f).toInt(), s(12f).toInt(), s(12f).toInt(), s(12f).toInt())
            })
            addView(BrassPanel(context, scale).apply {
                addView(gauge)
            }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f).apply {
                setMargins(s(12f).toInt(), s(12f).toInt(), s(12f).toInt(), s(12f).toInt())
            })
        }

        // Bitcoin
        btcView = TextView(context).apply {
            textSize = s(26f); setTextColor(Color.rgb(255, 200, 110)); gravity = Gravity.CENTER
        }
        val btcPanel = BrassPanel(context, scale).apply { addView(btcView) }

        // Barre d'état : résolution détectée
        statusView = TextView(context).apply {
            textSize = s(18f); setTextColor(Color.argb(180, 200, 180, 150)); gravity = Gravity.CENTER
            text = "Écran : ${m.widthPixels}×${m.heightPixels} · échelle ×%.2f".format(scale)
        }

        root.addView(titleRow)
        root.addView(clock)
        root.addView(dateView)
        root.addView(midRow, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f).apply {
            setMargins(0, s(12f).toInt(), 0, s(12f).toInt()) })
        root.addView(btcPanel, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
            setMargins(s(12f).toInt(), 0, s(12f).toInt(), s(12f).toInt()) })
        root.addView(statusView)
        setContentView(root)

        tick.run()
        main.post { refreshWeather(weatherText) }
        main.post { refreshBtc() }
    }

    private fun refreshWeather(tv: TextView) = Thread {
        try {
            val con = URL("https://api.open-meteo.com/v1/forecast?latitude=48.85&longitude=2.35&current=temperature_2m,wind_speed_10m&timezone=auto")
                .openConnection() as HttpURLConnection
            con.connectTimeout = 8000; con.readTimeout = 8000
            val json = JSONObject(con.inputStream.bufferedReader().readText())
            val cur = json.getJSONObject("current")
            val t = cur.getDouble("temperature_2m"); val w = cur.getDouble("wind_speed_10m")
            main.post { tv.text = String.format(Locale.FRANCE,
                "🌤 MÉTÉO PARIS   %.1f °C   ·   vent %.0f km/h", t, w) }
            con.disconnect()
        } catch (e: Exception) {
            main.post { tv.text = "🌤 Station météo hors ligne : ${e.message}" }
        }
    }.start()

    private fun refreshBtc() = Thread {
        try {
            val con = URL("https://api.coinbase.com/v2/exchange-rates?currency=BTC")
                .openConnection() as HttpURLConnection
            con.connectTimeout = 8000; con.readTimeout = 8000
            val rates = JSONObject(con.inputStream.bufferedReader().readText())
                .getJSONObject("data").getJSONObject("rates")
            val eur = rates.getDouble("EUR")
            main.post { btcView.text = String.format(Locale.FRANCE,
                "⛏️ BITCOIN   %s €", java.text.NumberFormat.getIntegerInstance(Locale.FRANCE).format(eur)) }
            con.disconnect()
        } catch (e: Exception) {
            main.post { btcView.text = "⛏️ Machine hors ligne : ${e.message}" }
        }
    }.start()

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
