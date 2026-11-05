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
import android.view.animation.LinearInterpolator
import android.widget.Button
import android.widget.ImageView
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
    private lateinit var statusLine: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val safeName = try { display.name ?: "Écran ${display.displayId}" } catch (e: Exception) { "Écran ${display.displayId}" }
        val brass = Color.rgb(183, 141, 65)
        val copper = Color.rgb(140, 84, 42)
        val dark = Color.rgb(28, 20, 12)
        val cream = Color.rgb(245, 230, 200)

        val bg = GradientDrawable(GradientDrawable.Orientation.TL_BR,
            intArrayOf(Color.rgb(38, 28, 16), Color.rgb(20, 14, 8)))
        bg.setStroke(6, copper)

        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            background = bg
            setPadding(60, 40, 60, 40)
        }

        // En-tête avec engrenages animés
        val header = LinearLayout(context).apply { gravity = Gravity.CENTER }
        val gearL = gear(brass)
        val gearR = gear(brass)
        animateGear(gearL, 12000f)
        animateGear(gearR, 12000f)
        val title = TextView(context).apply {
            text = "⚙️ TABLEAU DE BORD ⚙️"
            textSize = 34f; setTextColor(brass)
            typeface = Typeface.create("serif", Typeface.BOLD)
            setShadowLayer(8f, 0f, 0f, Color.argb(160, 255, 200, 90))
        }
        listOf(gearL, title, gearR).forEach { header.addView(it) }

        // Horloge style cadran
        clock = TextView(context).apply {
            textSize = 110f; setTextColor(cream); gravity = Gravity.CENTER
            typeface = Typeface.MONOSPACE
            setShadowLayer(15f, 0f, 0f, Color.argb(120, 255, 170, 40))
        }
        dateView = TextView(context).apply {
            textSize = 24f; setTextColor(brass); gravity = Gravity.CENTER
            typeface = Typeface.create("serif", Typeface.ITALIC)
        }

        // Panneau vapeur : météo + jauge de pression
        val panel = panelCard(copper, dark)
        weatherView = TextView(context).apply {
            text = "titude Barométrique… ⏳"
            textSize = 20f; setTextColor(cream); gravity = Gravity.CENTER
            typeface = Typeface.SERIF
        }
        gauge = LinearLayout(context).apply { gravity = Gravity.CENTER; setPadding(0, 20, 0, 0) }
        listOf(weatherView, gauge).forEach { panel.addView(it) }

        // Panneau_machine : cours Bitcoin
        val panel2 = panelCard(copper, dark)
        btcView = TextView(context).apply {
            text = "Mine d'or numérique… ⛏️"
            textSize = 20f; setTextColor(cream); gravity = Gravity.CENTER
            typeface = Typeface.SERIF
        }
        panel2.addView(btcView)

        statusLine = TextView(context).apply {
            text = "⚙ $safeName"
            textSize = 16f; setTextColor(Color.GRAY); gravity = Gravity.CENTER
        }

        val closeBtn = Button(context).apply { text = "🕯️ Éteindre la machine" }

        listOf(header, clock, dateView, panel, panel2, closeBtn, statusLine).forEach {
            root.addView(it, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                setMargins(0, 24, 0, 24)
            })
        }
        setContentView(root)
        closeBtn.setOnClickListener { dismiss() }

        main.post(tick)
        Thread { fetchWeather() }.start()
        Thread { fetchBitcoin() }.start()
    }

    private fun gear(color: Int): ImageView {
        val iv = ImageView(context)
        val d = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setStroke(8, color)
            setColor(Color.TRANSPARENT)
        }
        iv.background = d
        iv.setPadding(18, 18, 18, 18)
        iv.setImageDrawable(null)
        iv.text?.let {} // no-op
        iv.minimumWidth = 40; iv.minimumHeight = 40
        // Roue dentée stylisée : cercle + croix
        val ll = LinearLayout(context) // fallback: emoji gear used in title; this is decorative ring
        iv.contentDescription = "gear"
        return iv
    }

    private fun animateGear(iv: ImageView, duration: Long) {
        iv.animate().rotationBy(360f).setDuration(duration)
            .setInterpolator(LinearInterpolator()).withEndAction {
                iv.rotation = 0f
                animateGear(iv, duration)
            }.start()
    }

    private fun panelCard(border: Int, fill: Int): LinearLayout {
        val card = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 26f
            setStroke(4, border)
            setColor(Color.argb(90, fill.red(), fill.green(), fill.blue()))
        }
        return LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            background = card
            setPadding(40, 26, 40, 26)
        }
    }

    private fun Int.red() = (this shr 16) and 0xFF
    private fun Int.green() = (this shr 8) and 0xFF
    private fun Int.blue() = this and 0xFF

    // ── API gratuite Open-Meteo (météo, sans clé) ──
    private fun fetchWeather() {
        try {
            val url = URL("https://api.open-meteo.com/v1/forecast?latitude=48.85&longitude=2.35&current=temperature_2m,wind_speed_10m,surface_pressure")
            val json = JSONObject((url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 8000; readTimeout = 8000
            }.inputStream.bufferedReader().readText())
            val c = json.getJSONObject("current")
            val pressure = c.getDouble("surface_pressure") / 100.0 // en bar
            main.post {
                weatherView.text = "🌡️ ${c.getDouble("temperature_2m")}°C   💨 ${c.getDouble("wind_speed_10m")} km/h"
                buildGauge(pressure)
            }
        } catch (e: Exception) {
            main.post { weatherView.text = "🌡️ Vapeurs indisponibles : ${e.message}" }
        }
    }

    private fun buildGauge(barValue: Double) {
        gauge.removeAllViews()
        val max = 1100.0; val ratio = (barValue / max).coerceIn(0.0, 1.0)
        val gaugeLabel = TextView(context).apply {
            text = String.format("⏲️ Pression : %.0f hPa  ", barValue)
            textSize = 18f; setTextColor(Color.rgb(183, 141, 65))
            typeface = Typeface.SERIF
        }
        val track = android.view.View(context).apply {
            background = GradientDrawable().apply {
                cornerRadius = 12f
                setColor(Color.argb(70, 0, 0, 0))
                setStroke(2, Color.rgb(140, 84, 42))
            }
        }
        val fillView = android.view.View(context).apply {
            background = GradientDrawable().apply {
                cornerRadius = 12f
                setColor(Color.rgb(255, 170, 40))
            }
        }
        val trackRow = android.widget.FrameLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(700, 34)
            addView(track, android.widget.FrameLayout.LayoutParams(
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT))
            addView(fillView, android.widget.FrameLayout.LayoutParams(
                (700 * ratio).toInt(), android.widget.FrameLayout.LayoutParams.MATCH_PARENT))
        }
        gauge.addView(gaugeLabel)
        gauge.addView(trackRow)
    }

    // ── API gratuite CoinGecko (cours Bitcoin, sans clé) ──
    private fun fetchBitcoin() {
        try {
            val url = URL("https://api.coingecko.com/api/v3/simple/price?ids=bitcoin&vs_currencies=eur,usd")
            val json = JSONObject((url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 8000; readTimeout = 8000
                setRequestProperty("Accept", "application/json")
            }.inputStream.bufferedReader().readText())
            val btc = json.getJSONObject("bitcoin")
            main.post {
                btcView.text = String.format(
                    "⛏️ BITCOIN  —  %s €  ·  %s $",
                    java.text.NumberFormat.getInstance(Locale.FRANCE).format(btc.getInt("eur")),
                    java.text.NumberFormat.getInstance(Locale.US).format(btc.getInt("usd")))
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
