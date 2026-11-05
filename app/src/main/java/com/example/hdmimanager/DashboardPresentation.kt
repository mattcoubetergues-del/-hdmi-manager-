package com.example.hdmimanager

import android.app.Presentation
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.Display
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DashboardPresentation(context: Context, display: Display) : Presentation(context, display) {

    private val colors = intArrayOf(
        Color.rgb(20, 24, 38),
        Color.rgb(10, 60, 40),
        Color.rgb(70, 20, 30),
        Color.rgb(40, 30, 70)
    )
    private var index = 0
    private lateinit var clock: TextView
    private val fmt = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    private val handler = android.os.Handler(android.os.Looper.getMainLooper())
    private val tick = object : Runnable {
        override fun run() {
            if (::clock.isInitialized) clock.text = fmt.format(Date())
            handler.postDelayed(this, 1000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val safeName = try { display.name ?: "Écran ${display.displayId}" } catch (e: Exception) { "Écran ${display.displayId}" }
        val mode = try { display.mode } catch (e: Exception) { null }
        val res = if (mode != null) "${mode.physicalWidth}x${mode.physicalHeight}" else "?"

        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(colors[index])
        }
        val title = TextView(context).apply {
            text = "🖥️ DASHBOARD — $safeName"
            textSize = 36f; gravity = Gravity.CENTER; setTextColor(Color.WHITE)
        }
        clock = TextView(context).apply {
            textSize = 90f; gravity = Gravity.CENTER; setTextColor(Color.WHITE)
        }
        val info = TextView(context).apply {
            text = "$safeName · $res"
            textSize = 22f; gravity = Gravity.CENTER; setTextColor(Color.LTGRAY)
        }
        val themeBtn = Button(context).apply { text = "🎨 Changer le thème" }
        val closeBtn = Button(context).apply { text = "❌ Fermer le dashboard" }
        themeBtn.setOnClickListener {
            index = (index + 1) % colors.size
            root.setBackgroundColor(colors[index])
        }
        closeBtn.setOnClickListener { dismiss() }

        listOf(title, clock, info, themeBtn, closeBtn).forEach {
            root.addView(it, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                setMargins(40, 30, 40, 30)
            })
        }
        setContentView(root)
        handler.post(tick)
    }

    override fun onStop() {
        handler.removeCallbacks(tick)
        super.onStop()
    }
}
