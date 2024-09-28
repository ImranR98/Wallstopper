package dev.imranr.staticwall

import android.graphics.*
import android.os.Handler
import android.os.Looper
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder
import android.content.SharedPreferences
import kotlinx.coroutines.*
import kotlin.random.Random

val initColour = android.graphics.Color.parseColor("#FF0000")

class MyWallpaperService : WallpaperService() {

    override fun onCreateEngine(): Engine {
        return MyWallpaperEngine()
    }

    inner class MyWallpaperEngine : Engine(), SharedPreferences.OnSharedPreferenceChangeListener {
        private val noisePaint = Paint()
        private val handler = Handler(Looper.getMainLooper())
        private var visible = true
        private val prefs = getSharedPreferences("wallpaper_prefs", MODE_PRIVATE)
        private var backgroundColor = initColour
        private var noiseBitmap: Bitmap? = null

        // Lower resolution scale factor (e.g., quarter the resolution)
        private val scaleFactor = 6

        // Coroutine scope for parallel noise generation
        private val coroutineScope = CoroutineScope(Dispatchers.Default + Job())

        init {
            prefs.registerOnSharedPreferenceChangeListener(this)
        }

        override fun onVisibilityChanged(visible: Boolean) {
            this.visible = visible
            if (visible) {
                draw()
                handler.post(animationRunnable) // Start animation when visible
            } else {
                handler.removeCallbacks(animationRunnable) // Stop animation when invisible
            }
        }

        override fun onSurfaceCreated(holder: SurfaceHolder) {
            super.onSurfaceCreated(holder)
            draw()
            handler.post(animationRunnable) // Start animation loop
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            super.onSurfaceDestroyed(holder)
            handler.removeCallbacks(animationRunnable) // Stop the animation
            visible = false
        }

        override fun onDestroy() {
            super.onDestroy()
            prefs.unregisterOnSharedPreferenceChangeListener(this)
            coroutineScope.cancel() // Cancel any running coroutines
        }

        override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
            if (key == "wallpaper_color" && sharedPreferences != null) {
                val newColor = sharedPreferences.getInt("wallpaper_color", initColour)
                updateColor(newColor)
            }
        }

        private fun updateColor(newColor: Int) {
            backgroundColor = newColor
            if (visible) {
                draw()
            }
        }

        // Animation runnable to update noise every 1/24th of a second
        private val animationRunnable = object : Runnable {
            override fun run() {
                if (visible) {
                    coroutineScope.launch {
                        generateNoiseBitmapParallel() // Generate new noise in parallel
                        withContext(Dispatchers.Main) {
                            draw() // Redraw with the new noise on the main thread
                        }
                    }
                    handler.postDelayed(this, 100) // 24 FPS
                }
            }
        }

        // Parallel noise generation using Kotlin coroutines at a lower resolution
        private suspend fun generateNoiseBitmapParallel() {
            val width = surfaceHolder.surfaceFrame.width() / scaleFactor
            val height = surfaceHolder.surfaceFrame.height() / scaleFactor

            // Only create a new bitmap if the size changes
            if (noiseBitmap == null || noiseBitmap?.width != width || noiseBitmap?.height != height) {
                noiseBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            }

            val pixels = IntArray(width * height)

            // Split the work into parallel coroutines to fill the pixels array
            val chunkSize = pixels.size / Runtime.getRuntime().availableProcessors()
            val jobs = mutableListOf<Job>()

            for (i in pixels.indices step chunkSize) {
                val end = minOf(i + chunkSize, pixels.size)
                jobs.add(coroutineScope.launch {
                    for (j in i until end) {
                        // Generate random grayscale noise and make it semi-transparent
                        val noise = Random.nextInt(0, 256)
                        pixels[j] = Color.argb(50, noise, noise, noise) // Alpha = 50 for transparency
                    }
                })
            }

            // Wait for all coroutines to finish
            jobs.joinAll()

            noiseBitmap?.setPixels(pixels, 0, width, 0, 0, width, height)
        }

        private fun draw() {
            val holder = surfaceHolder
            var canvas: Canvas? = null
            try {
                canvas = holder.lockCanvas()

                if (canvas != null) {
                    // Draw the background color
                    canvas.drawColor(backgroundColor)

                    // Scale up and overlay the noise
                    noiseBitmap?.let {
                        val matrix = Matrix().apply {
                            postScale(scaleFactor.toFloat(), scaleFactor.toFloat())
                        }
                        val scaledNoiseBitmap = Bitmap.createBitmap(it, 0, 0, it.width, it.height, matrix, false)
                        canvas.drawBitmap(scaledNoiseBitmap, 0f, 0f, noisePaint)
                    }
                }

            } finally {
                if (canvas != null) {
                    holder.unlockCanvasAndPost(canvas)
                }
            }
        }
    }
}
