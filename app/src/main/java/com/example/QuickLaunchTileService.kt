package com.example

import android.content.Context
import android.content.Intent
import android.service.quicksettings.TileService

class QuickLaunchTileService : TileService() {
    override fun onClick() {
        super.onClick()
        val prefs = getSharedPreferences("countr_settings", Context.MODE_PRIVATE)
        val size = prefs.getString("tile_size", "Medium") ?: "Medium"
        val template = prefs.getString("tile_template", "Standard") ?: "Standard"
        val timer = prefs.getBoolean("tile_timer", false)
        val autoClose = prefs.getBoolean("auto_close", false)

        val serviceIntent = Intent(this, CounterOverlayService::class.java).apply {
            putExtra("SIZE", size)
            putExtra("TEMPLATE", template)
            putExtra("ENABLE_TIMER", timer)
        }
        startService(serviceIntent)
        
        // Broadcast intent to close MainActivity if open
        if (autoClose) {
            val closeIntent = Intent("com.example.CLOSE_DASHBOARD")
            sendBroadcast(closeIntent)
        }
    }
}
