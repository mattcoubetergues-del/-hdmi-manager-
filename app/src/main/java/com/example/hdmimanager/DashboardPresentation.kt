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
        Color.rgb(20, 24, 38),   // Nuit
        Color.rgb(10, 60, 40),   // Vert
        Color.rgb(70, 20, 30),   // Rouge sombre
        Color.rgb(40, 30, 70)    // Violet
    )
    private var index = 0
    private lateinit var clock: TextView
    private lateinit var status: TextView
    private val fmt = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(colors[index])
        }

        val title = TextView(context).apply {
            text = "🖥️ DASHBOARD HDMI"
            textSize = 40f
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
        }
        clock = TextView(context).apply {
            textSize = 90f
            gravity = Gravity.CENTER
            setTextColor(Color.rgb(0, 220, 255))
        }
        status = TextView(context).apply {
            textSize = 24f
            gravity = Gravity.CENTER
            setTextColor(Color.LTGRAY)
            text = "Écran externe actif • ${display.mode.physicalWidth}x${display.mode.physicalHeight}"
        }

        // Boutons interactifs
        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        fun addButton(label: String, action: () -> Unit) {
            row.addView(Button(context).apply {
                text = label
                textSize = 18f
                setOnClickListener { action() }
            })
        }
        addButton("🎨 Thème") {
            index = (index + 1) % colors.size
            root.setBackgroundColor(colors[index])
        }
        addButton("✨ Bonus") {
            status.text = listOf(
                "Ça marche ! 🚀", "Bonjour depuis l'écran HDMI ! 👋",
                "Battery : ${android.os.BatteryManager.BATTERY_STATUS_CHARGING}", ""
            ).let { "Tap ! 🎉" }
        }

        root.addView(title)
        root.addView(clock)
        root.addView(status)
        root.addView(row)
        setContentView(root)

        // Horloge en direct
        val handler = android.os.Handler(android.os.Looper.getMainLooper())
        val tick = object : Runnable {
            override fun run() {
                clock.text = fmt.format(Date())
                handler.postDelayed(this, 1000)
            }
        }
        handler.post(tick)
    }
}
