package com.example

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.ui.platform.ComposeView
import androidx.core.app.NotificationCompat
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CounterOverlayService : Service() {

    private var windowManager: WindowManager? = null
    private var composeView: ComposeView? = null
    private var lifecycleOwner: ComposeLifecycleOwner? = null
    private var layoutParams: WindowManager.LayoutParams? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        lifecycleOwner = ComposeLifecycleOwner()
        lifecycleOwner?.onCreate()
        createNotificationChannel()
    }

    private var sessionNote: String = ""

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val sizeStr = intent?.getStringExtra("SIZE") ?: "Medium"
        val templateStr = intent?.getStringExtra("TEMPLATE") ?: "Standard"
        val enableTimer = intent?.getBooleanExtra("ENABLE_TIMER", false) ?: false
        sessionNote = intent?.getStringExtra("NOTE") ?: ""

        // Start as Foreground Service so it doesn't die when the screen sleeps
        val notification = NotificationCompat.Builder(this, "countr_channel")
            .setContentTitle("Countr is running")
            .setContentText("Your floating timer is active")
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
        startForeground(1, notification)

        if (composeView == null) {
            showOverlay(sizeStr, templateStr, enableTimer)
        }
        return START_NOT_STICKY
    }

    private fun showOverlay(sizeStr: String, templateStr: String, enableTimer: Boolean) {
        val prefs = getSharedPreferences("countr_settings", Context.MODE_PRIVATE)
        val alpha = prefs.getFloat("bubble_alpha", 0.95f)

        layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        )
        layoutParams?.gravity = Gravity.TOP or Gravity.START
        layoutParams?.x = 100
        layoutParams?.y = 200

        composeView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(lifecycleOwner)
            setViewTreeSavedStateRegistryOwner(lifecycleOwner)
            setViewTreeViewModelStoreOwner(lifecycleOwner)
            setContent {
                BubbleOverlay(
                    sizeStr = sizeStr,
                    templateStr = templateStr,
                    enableTimer = enableTimer,
                    overlayAlpha = alpha,
                    onClose = { check, cross, checkTime, crossTime ->
                        saveSession(check, cross, checkTime, crossTime)
                        stopSelf()
                    },
                    onDrag = { dx, dy ->
                        val params = layoutParams ?: return@BubbleOverlay
                        params.x += dx.toInt()
                        params.y += dy.toInt()
                        
                        // Clamp during drag so it doesn't fly off screen
                        val metrics = resources.displayMetrics
                        params.x = params.x.coerceIn(0, metrics.widthPixels - 100)
                        params.y = params.y.coerceIn(0, metrics.heightPixels - 100)
                        
                        windowManager?.updateViewLayout(this, params)
                    }
                )
            }
        }

        windowManager?.addView(composeView, layoutParams)
        lifecycleOwner?.onStart()
        lifecycleOwner?.onResume()
    }

    // Clamp when rotating
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        val params = layoutParams ?: return
        val view = composeView ?: return
        
        val metrics = resources.displayMetrics
        val viewWidth = if (view.width > 0) view.width else 100
        val viewHeight = if (view.height > 0) view.height else 100

        params.x = params.x.coerceIn(0, metrics.widthPixels - viewWidth)
        params.y = params.y.coerceIn(0, metrics.heightPixels - viewHeight)
        
        windowManager?.updateViewLayout(view, params)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "countr_channel",
                "Countr Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun saveSession(check: Int, cross: Int, checkTime: Long, crossTime: Long) {
        val app = application as MainApplication
        val session = com.example.data.CounterSession(
            checkCount = check,
            crossCount = cross,
            totalCheckTimeMs = checkTime,
            totalCrossTimeMs = crossTime,
            note = sessionNote,
            timestamp = System.currentTimeMillis()
        )
        CoroutineScope(Dispatchers.IO).launch {
            app.repository.insertSession(session)
        }
    }

    override fun onDestroy() {
        lifecycleOwner?.onPause()
        lifecycleOwner?.onStop()
        lifecycleOwner?.onDestroy()
        if (composeView != null) {
            windowManager?.removeView(composeView)
            composeView = null
        }
        super.onDestroy()
    }
}
