package com.ruqaiyapro.debug.floating

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.Gravity
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import com.ruqaiyapro.debug.R
import java.io.File

class FloatingChibiService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var chibiView: ImageView
    private lateinit var params: WindowManager.LayoutParams
    private lateinit var scaleDetector: ScaleGestureDetector
    private var currentSize = 90f // default 90dp

    companion object {
        @Volatile var running = false
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        running = true
        initChibiOverlay()
    }

    private fun initChibiOverlay() {
        val prefs = getSharedPreferences("ruqaiya_chibi_coords", Context.MODE_PRIVATE)
        val initialX = prefs.getInt("chibi_x", 100)
        val initialY = prefs.getInt("chibi_y", 300)
        currentSize = prefs.getFloat("chibi_size", 90f)

        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        params = WindowManager.LayoutParams(
            dpToPx(currentSize),
            dpToPx(currentSize),
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = initialX
            y = initialY
        }

        chibiView = ImageView(this).apply {
            val bitmap = loadChibiBitmap()
            if (bitmap != null) {
                setImageBitmap(bitmap)
                android.widget.Toast.makeText(context, "Custom Chibi loaded \uD83D\uDE0D", android.widget.Toast.LENGTH_SHORT).show()
            } else {
                setImageResource(R.mipmap.ic_launcher)
            }
        }

        setupGestures()
        try {
            windowManager.addView(chibiView, params)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun loadChibiBitmap(): Bitmap? {
        val paths = listOf(
            File(filesDir, "chibi_custom.png").absolutePath,
            File(getExternalFilesDir(null), "chibi_custom.png").absolutePath,
            File(filesDir, "chibi.png").absolutePath,
            File(getExternalFilesDir(null), "chibi.png").absolutePath,
            "/sdcard/Download/ruqaiya_chibi.png",
            "/sdcard/Download/chibi_custom.png",
            "/storage/emulated/0/Download/chibi_custom.png",
            "/sdcard/Download/chibi.png"
        )

        for (path in paths) {
            val file = File(path)
            if (file.exists() && file.canRead()) {
                try {
                    val bitmap = decodeScaled(file.absolutePath)
                    if (bitmap != null) return bitmap
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        return null
    }

    private fun decodeScaled(path: String): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, bounds)
        var sample = 1
        while (bounds.outWidth / sample > 600 || bounds.outHeight / sample > 600) sample *= 2
        return BitmapFactory.decodeFile(path, BitmapFactory.Options().apply { inSampleSize = sample })
    }

    private fun setupGestures() {
        scaleDetector = ScaleGestureDetector(this, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                currentSize *= detector.scaleFactor
                currentSize = currentSize.coerceIn(40f, 300f) // min 40dp max 300dp
                params.width = dpToPx(currentSize)
                params.height = dpToPx(currentSize)
                windowManager.updateViewLayout(chibiView, params)
                getSharedPreferences("ruqaiya_chibi_coords", Context.MODE_PRIVATE)
                    .edit().putFloat("chibi_size", currentSize).apply()
                return true
            }
        })

        var initialTouchX = 0f
        var initialTouchY = 0f
        var initialX = 0
        var initialY = 0
        var downTime = 0L
        val longHandler = android.os.Handler(android.os.Looper.getMainLooper())
        val longRunnable = Runnable {
            vibrate(50)
            android.widget.Toast.makeText(this, "Chibi: drag = move, pinch = resize, tap = bounce", android.widget.Toast.LENGTH_SHORT).show()
        }

        chibiView.setOnTouchListener { _, event ->
            scaleDetector.onTouchEvent(event)
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    downTime = System.currentTimeMillis()
                    longHandler.postDelayed(longRunnable, 500)
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    if (Math.abs(event.rawX - initialTouchX) > 20 || Math.abs(event.rawY - initialTouchY) > 20) longHandler.removeCallbacks(longRunnable)
                    if (event.pointerCount == 1) {
                        params.x = initialX + (event.rawX - initialTouchX).toInt()
                        params.y = initialY + (event.rawY - initialTouchY).toInt()
                        windowManager.updateViewLayout(chibiView, params)
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    longHandler.removeCallbacks(longRunnable)
                    // Persist XY
                    getSharedPreferences("ruqaiya_chibi_coords", Context.MODE_PRIVATE).edit()
                        .putInt("chibi_x", params.x)
                        .putInt("chibi_y", params.y)
                        .apply()

                    val duration = System.currentTimeMillis() - downTime
                    if (duration < 300 && Math.abs(event.rawX - initialTouchX) < 10) {
                        // Click Bounce 80dp -> 200dp animation & 50ms vibrate
                        vibrate(50)
                        triggerBounceAnimation()
                    }
                    true
                }
                else -> false
            }
        }
    }

    private fun triggerBounceAnimation() {
        chibiView.animate()
            .scaleX(1.4f)
            .scaleY(1.4f)
            .setDuration(120)
            .withEndAction {
                chibiView.animate().scaleX(1.0f).scaleY(1.0f).setDuration(120).start()
            }
            .start()
    }

    private fun vibrate(ms: Long) {
        val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(ms)
        }
    }

    private fun dpToPx(dp: Float): Int = (dp * resources.displayMetrics.density).toInt()

    override fun onDestroy() {
        super.onDestroy()
        running = false
        if (::chibiView.isInitialized) {
            try { windowManager.removeView(chibiView) } catch (e: Exception) { e.printStackTrace() }
        }
    }
}
