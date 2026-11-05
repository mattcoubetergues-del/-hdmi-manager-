package com.example.hdmimanager

import android.app.Presentation
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Display
import android.view.Gravity
import android.view.View
import android.view.animation.LinearInterpolator
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import org.json.JSONObject

class DashboardPresentation(context: Context, display: Display) : Presentation(context, display) {

    private val main = Handler(Looper.getMainLooper())
    private val fmt = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    private val fmtDate = SimpleDateFormat("EEEE d MMMM yyyy", Locale.FRENCH)
    private lateinit var clock: TextView
    private lateinit var dateView: TextView
    private lateinit var weatherView: TextView
    private lateinit var gauge: LinearLayout
    private lateinit var btcView: TextView

    private val brass = Color.rgb(183, 141, 65)
    private val copper = Color.rgb(140, 84, 42)
    private val cream = Color.rgb(245, 230, 200)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val safeName = try {
            display.name ?: "Écran ${display.displayId}"
        } catch (e: Exception) { "Écran ${display.displayId}" }

        val bg = GradientDrawable(GradientDrawable.Orientation.TL_BR,
            intArrayOf(Color.rgb(38, 28, 16), Color.rgb(20, 14, 8)))
        bg.setStroke(6, copper)

        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            background = bg
            setPadding(60, 40, 60, 40)
        }

        // En-tête : engrenages animés + titre
        val header = LinearLayout(context).apply { gravity = Gravity.CENTER }
        val gearL = gearView()
        val gearR = gearView()
        spin(gearL, 12000L)
        spin(gearR, 12000L)
        val title = TextView(context).apply {
            text = "TABLEAU DE BORD"
            textSize = 34f
            setTextColor(brass)
            typeface = Typeface.create("serif", Typeface.BOLD)
            setShadowLayer(8f, 0f, 0f, Color.argb(160, 255, 200, 90))
        }
        listOf(gearL, title, gearR).forEach { header.addView(it) }

        // Horloge
        clock = TextView(context).apply {
            textSize = 100f
            setTextColor(cream)
            gravity = Gravity.CENTER
            typeface = Typeface.MONOSPACE
            setShadowLayer(15f, 0f, 0f, Color.argb(120, 255, 170, 40))
        }
        dateView = TextView(context).apply {
            textSize = 24f
            setTextColor(brass)
            gravity = Gravity.CENTER
            typeface = Typeface.create("serif", Typeface.ITALIC)
        }

        // Panneau météo + jauge de pression
        val panel1 = panelCard()
        weatherView = TextView(context).apply {
            text = "⏳ Baromètre en chauffe…"
            textSize = 20f
            setTextColor(cream)
            gravity = Gravity.CENTER
            typeface = Typeface.SERIF
        }
        gauge = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
        }
        panel1.addView(weatherView)
        panel1.addView(gauge)

        // Panneau Bitcoin
        val panel2 = panelCard()
        btcView = TextView(context).apply {
            text = "⏳ Mineur au travail…"
            textSize = 20f
            setTextColor(cream)
            gravity = Gravity.CENTER
            typeface = Typeface.SERIF
        }
        panel2.addView(btcView)

        val nameLine = TextView(context).apply {
            text = "⚙ $safeName"
            textSize = 15f
            setTextColor(Color.GRAY)
            gravity = Gravity.CENTER
        }
        val closeBtn = Button(context).apply { text = "🕯️ Éteindre la machine" }
        closeBtn.setOnClickListener { dismiss() }

        listOf(header, clock, dateView, panel1, panel2, closeBtn, nameLine).forEach {
            root.addView(it, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(0, 20, 0, 20) })
        }
        setContentView(root)

        main.post(tick)
        Thread { fetchWeather() }.start()
        Thread { fetchBitcoin() }.start()
    }

    private fun gearView(): TextView = TextView(context).apply {
        text = "⚙"
        textSize = 34f
        setTextColor(brass)
    }

    private fun spin(v: View, duration: Long) {
        v.animate().rotationBy(360f).setDuration(duration)
            .setInterpolator(LinearInterpolator())
            .withEndAction { v.rotation = 0f; spin(v, duration) }
            .start()
    }

    private fun panelCard(): LinearLayout {
        val card = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 26f
            setStroke(4, copper)
            setColor(Color.argb(90, 28, 20, 12))
        }
        return LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            background = card
            setPadding(40, 26, 40, 26)
        }
    }

    // ── API gratuite Open-Meteo (météo, sans clé) ──
    private fun fetchWeather() {
        try {
            val url = URL("https://api.open-meteo.com/v1/forecast?latitude=48.85&longitude=2.35&current=temperature_2m,wind_speed_10m,surface_pressure")
            val json = JSONObject((url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 8000
                readTimeout = 8000
            }.inputStream.bufferedReader().readText())
            val c = json.getJSONObject("current")
            val pressure = c.getDouble("surface_pressure")
            main.post {
                weatherView.text = "🌡️ ${c.getDouble("temperature_2m")}°C   💨 ${c.getDouble("wind_speed_10m")} km/h"
                buildGauge(pressure)
            }
        } catch (e: Exception) {
            main.post { weatherView.text = "🌡️ Vapeurs indisponibles : ${e.message}" }
        }
    }

    private fun buildGauge(hpa: Double) {
        gauge.removeAllViews()
        val ratio = (hpa / 1100.0).coerceIn(0.0, 1.0)
        val label = TextView(context).apply {
            text = String.format(Locale.FRANCE, "⏲️ Pression : %.0f hPa", hpa)
            textSize = 18f
            setTextColor(brass)
            typeface = Typeface.SERIF
        }
        val track = FrameLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(700, 34)
            background = GradientDrawable().apply {
                cornerRadius = 12f
                setColor(Color.argb(70, 0, 0, 0))
                setStroke(2, copper)
            }
        }
        val fill = View(context).apply {
            background = GradientDrawable().apply {
                cornerRadius = 12f
                setColor(Color.rgb(255, 170, 40))
            }
        }
        track.addView(fill, FrameLayout.LayoutParams(
            (700 * ratio).toInt(), FrameLayout.LayoutParams.MATCH_PARENT))
        gauge.addView(label)
        gauge.addView(track)
    }

    // ── API gratuite CoinGecko (Bitcoin, sans clé) ──
    private fun fetchBitcoin() {
        try {
            val url = URL("https://api.coingecko.com/api/v3/simple/price?ids=bitcoin&vs_currencies=eur,usd")
            val json = JSONObject((url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 8000
                readTimeout = 8000
                setRequestProperty("Accept", "application/json")
            }.inputStream.bufferedReader().readText())
            val btc = json.getJSONObject("bitcoin")
            main.post {
                btcView.text = String.format(
                    "⛏️ BITCOIN — %s € · %s $",
                    java.text.NumberFormat.getIntegerInstance(Locale.FRANCE).format(btc.getLong("eur")),
                    java.text.NumberFormat.getIntegerInstance(Locale.US).format(btc.getLong("usd")))
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
        main.removeCallbacks(tick)
        super.onStop()
    }
}
