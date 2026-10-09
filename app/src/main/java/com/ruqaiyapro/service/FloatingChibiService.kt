package com.ruqaiyapro.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.PixelFormat
import android.os.Build
import android.os.Environment
import android.os.IBinder
import android.os.Vibrator
import android.view.*
import android.widget.ImageView
import android.widget.Toast
import com.ruqaiyapro.R
import java.io.File

class FloatingChibiService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var chibiView: View
    private lateinit var chibiImage: ImageView
    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f
    private var scaleFactor = 1.0f

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        setupFloatingChibi()
    }

    private fun setupFloatingChibi() {
        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            220, 220,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 100
            y = 200
        }

        val inflater = getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        chibiView = inflater.inflate(R.layout.layout_floating_chibi, null)
        chibiImage = chibiView.findViewById(R.id.imgChibi)

        // Load prioritized Chibi bitmap (custom, Termux, internal)
        loadChibiBitmap()

        // Setup Pinch & Drag Gesture
        val scaleDetector = ScaleGestureDetector(this, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                scaleFactor *= detector.scaleFactor
                // min 40dp (approx 100px), max 300dp (approx 750px)
                scaleFactor = scaleFactor.coerceIn(0.5f, 3.0f)
                params.width = (220 * scaleFactor).toInt()
                params.height = (220 * scaleFactor).toInt()
                windowManager.updateViewLayout(chibiView, params)
                return true
            }
        })

        chibiView.setOnTouchListener { v, event ->
            scaleDetector.onTouchEvent(event)
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    params.x = initialX + (event.rawX - initialTouchX).toInt()
                    params.y = initialY + (event.rawY - initialTouchY).toInt()
                    windowManager.updateViewLayout(chibiView, params)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    val diffX = Math.abs(event.rawX - initialTouchX)
                    val diffY = Math.abs(event.rawY - initialTouchY)
                    if (diffX < 10 && diffY < 10) {
                        onChibiClicked()
                    }
                    true
                }
                else -> false
            }
        }

        windowManager.addView(chibiView, params)
    }

    /**
     * TOP PRIORITY: fun loadChibiBitmap()
     * Checks filesDir, externalFilesDir, /sdcard/Download/chibi_custom.png
     */
    fun loadChibiBitmap(): Bitmap? {
        val paths = listOf(
            File(filesDir, "chibi_custom.png"),
            File(getExternalFilesDir(null), "chibi_custom.png"),
            File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "chibi_custom.png"),
            File("/sdcard/Download/chibi_custom.png")
        )

        for (file in paths) {
            if (file.exists() && file.canRead()) {
                val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                if (bitmap != null) {
                    chibiImage.setImageBitmap(bitmap)
                    return bitmap
                }
            }
        }

        // Fallback default avatar
        chibiImage.setImageResource(R.drawable.ruqaiya_chibi_default)
        return null
    }

    private fun onChibiClicked() {
        val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        @Suppress("DEPRECATION")
        vibrator.vibrate(50)

        // Bounce Animation
        chibiImage.animate()
            .scaleX(1.35f)
            .scaleY(1.35f)
            .setDuration(120)
            .withEndAction {
                chibiImage.animate().scaleX(1.0f).scaleY(1.0f).setDuration(120).start()
            }.start()

        Toast.makeText(this, "Ruqaiya: Boss Rubel, ami apnar kachei achi! ❤️", Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::chibiView.isInitialized) {
            windowManager.removeView(chibiView)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}