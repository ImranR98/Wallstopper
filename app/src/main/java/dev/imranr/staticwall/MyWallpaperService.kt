package dev.imranr.staticwall

import android.graphics.*
import android.os.Handler
import android.os.Looper
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder
import android.content.SharedPreferences
import android.util.Log
import kotlinx.coroutines.*
import kotlin.random.Random

val initColour = Color.parseColor("#000000")
const val initFPS = 24
const val initLoopSeconds = 5
const val initScaleFactor = 2
const val initMaxNoiseBrightness = 70

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
        private var wallpaperWidth: Int = 0
        private var wallpaperHeight: Int = 0
        private val coroutineScope = CoroutineScope(Dispatchers.Default + Job())
        private var currentFrameIndex = 0
        private var frameGenerationJob: Job? = null

        // Speed/quality params
        private var FPS = initFPS
        private var loopSeconds = initLoopSeconds
        private var scaleFactor = initScaleFactor
        private var maxNoiseBrightness = initMaxNoiseBrightness

        // Derived params
        private var frameDurationMillis = 1000L / FPS
        private var totalFrames = loopSeconds * FPS
        private var noiseFrames = arrayOfNulls<Bitmap>(totalFrames)

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
            Log.e("MyWallpaperService", "onSurfaceCreated")
            super.onSurfaceCreated(holder)
            wallpaperWidth = holder.surfaceFrame.width() / scaleFactor
            wallpaperHeight = holder.surfaceFrame.height() / scaleFactor
            generateNoiseFrames()
            draw()
            handler.post(animationRunnable) // Start animation loop
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            super.onSurfaceDestroyed(holder)
            handler.removeCallbacks(animationRunnable) // Stop the animation
            visible = false
        }

        override fun onDestroy() {
            Log.e("MyWallpaperService", "onDestroy")
            super.onDestroy()
            prefs.unregisterOnSharedPreferenceChangeListener(this)
            frameGenerationJob?.cancel() // Cancel frame generation job if active
            noiseFrames.forEach {
                it?.recycle()
            } // Clean up generated bitmaps
        }

        override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
            var restart = true
            if (sharedPreferences != null) {
                if (key == "wallpaper_color") {
                    backgroundColor = sharedPreferences.getInt("wallpaper_color", initColour)
                    restart = false
                } else if (key == "fps") {
                    FPS = sharedPreferences.getInt("fps", 24)
                } else if (key == "loop_seconds") {
                    loopSeconds = sharedPreferences.getInt("loop_seconds", 3)
                } else if (key == "scale_factor") {
                    scaleFactor = sharedPreferences.getInt("scale_factor", 1)
                } else if (key == "max_noise_brightness") {
                    maxNoiseBrightness = sharedPreferences.getInt("max_noise_brightness", 256)
                }
            }
            if (restart) {
                frameDurationMillis = 1000L / FPS
                totalFrames = loopSeconds * FPS
                for (i in 0 until totalFrames) {
                    noiseFrames[i] = null
                }
                generateNoiseFrames()
            }
            if (visible) {
                draw()
            }
        }

        // Animation runnable to update the current frame
        private val animationRunnable = object : Runnable {
            override fun run() {
                if (visible && noiseFrames.isNotEmpty()) {
                    draw() // Draw the current frame
                    val nextFrameIndex = (currentFrameIndex + 1) % noiseFrames.size
                    if (noiseFrames[nextFrameIndex] != null) {
                        currentFrameIndex = nextFrameIndex // Loop back to the start
                    } else {
                        currentFrameIndex = 0
                    }
                    handler.postDelayed(this, frameDurationMillis) // Schedule the next frame
                }
            }
        }

        // Parallel noise generation using Kotlin coroutines at a lower resolution
        private suspend fun generateNoiseBitmapParallel(width: Int, height: Int): Bitmap {
            // Only create a new bitmap if the size changes
            val noiseBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val pixels = IntArray(width * height)
            // Split the work into parallel coroutines to fill the pixels array
            val chunkSize = pixels.size / Runtime.getRuntime().availableProcessors()
            val jobs = mutableListOf<Job>()
            for (i in pixels.indices step chunkSize) {
                val end = minOf(i + chunkSize, pixels.size)
                jobs.add(coroutineScope.launch {
                    for (j in i until end) {
                        // Generate random grayscale noise and make it semi-transparent
                        val noise = Random.nextInt(0, maxNoiseBrightness)
                        pixels[j] = Color.argb(50, noise, noise, noise) // Alpha = 50 for transparency
                    }
                })
            }
            jobs.joinAll()
            noiseBitmap.setPixels(pixels, 0, width, 0, 0, width, height)
            return noiseBitmap
        }

        private fun generateNoiseFrames() {
            Log.e("MyWallpaperService", "Starting to generate frames.")
            frameGenerationJob?.cancel()
            frameGenerationJob = CoroutineScope(Dispatchers.Default).launch {
                for (i in 0 until totalFrames) {
                    noiseFrames[i] = generateNoiseBitmapParallel(wallpaperWidth, wallpaperHeight)
                }
                Log.e("MyWallpaperService", "Finished generating frames.")
            }
        }

        private fun draw() {
            val holder = surfaceHolder
            var canvas: Canvas? = null
            try {
                canvas = holder.lockCanvas()

                if (canvas != null) {
                    // Draw the background color
                    canvas.drawColor(backgroundColor)

                    // Overlay the current noise frame if it exists
                    if (currentFrameIndex < noiseFrames.size) {
                        noiseFrames[currentFrameIndex]?.let {
                            val matrix = Matrix().apply {
                                postScale(scaleFactor.toFloat(), scaleFactor.toFloat())
                            }
                            val scaledNoiseBitmap = Bitmap.createBitmap(it, 0, 0, it.width, it.height, matrix, false)
                            canvas.drawBitmap(scaledNoiseBitmap, 0f, 0f, noisePaint)
                        }
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

