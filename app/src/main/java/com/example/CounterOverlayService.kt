package com.example

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CounterOverlayService : Service() {

    private var windowManager: WindowManager? = null
    private var composeView: ComposeView? = null
    private var lifecycleOwner: ComposeLifecycleOwner? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        lifecycleOwner = ComposeLifecycleOwner()
        lifecycleOwner?.onCreate()
    }

    private var sessionNote: String = ""

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val sizeStr = intent?.getStringExtra("SIZE") ?: "Medium"
        val templateStr = intent?.getStringExtra("TEMPLATE") ?: "Standard"
        val enableTimer = intent?.getBooleanExtra("ENABLE_TIMER", false) ?: false
        sessionNote = intent?.getStringExtra("NOTE") ?: ""

        if (composeView == null) {
            showOverlay(sizeStr, templateStr, enableTimer)
        }
        return START_NOT_STICKY
    }

    private fun showOverlay(sizeStr: String, templateStr: String, enableTimer: Boolean) {
        val prefs = getSharedPreferences("countr_settings", Context.MODE_PRIVATE)
        val alpha = prefs.getFloat("bubble_alpha", 0.95f)

        val params = WindowManager.LayoutParams(
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
        params.gravity = Gravity.TOP or Gravity.START
        params.x = 100
        params.y = 200

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
                        params.x += dx.toInt()
                        params.y += dy.toInt()
                        windowManager?.updateViewLayout(this, params)
                    }
                )
            }
        }

        windowManager?.addView(composeView, params)
        lifecycleOwner?.onStart()
        lifecycleOwner?.onResume()
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
