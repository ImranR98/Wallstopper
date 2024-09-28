package dev.imranr.wallstopper

import android.graphics.*
import android.os.Handler
import android.os.Looper
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder
import android.content.SharedPreferences
import kotlinx.coroutines.*
import kotlin.random.Random
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import kotlin.math.max

val initColour = Color.parseColor("#000000")
const val initFPS = 60
const val initLoopSeconds = 4
const val initScaleFactor = 2
const val initMaxNoiseBrightness = 100
const val initTilingFactor = 6
const val initRotationSupport = false

class NoiseGenerationViewModel : ViewModel() {
    private val _value = MutableLiveData<Float?>()
    val value: LiveData<Float?> get() = _value
    fun updateValue(value: Float?) {
        _value.postValue(value)
    }
    companion object {
        private var instance: NoiseGenerationViewModel? = null
        fun getInstance(): NoiseGenerationViewModel {
            if (instance == null) {
                instance = NoiseGenerationViewModel()
            }
            return instance!!
        }
    }
}

class MyWallpaperService : WallpaperService() {
    override fun onCreateEngine(): Engine {
        return MyWallpaperEngine()
    }

    inner class MyWallpaperEngine : Engine(), SharedPreferences.OnSharedPreferenceChangeListener {
        private val noisePaint = Paint()
        private val handler = Handler(Looper.getMainLooper())
        private var visible = true
        private val prefs = getSharedPreferences("wallpaper_prefs", MODE_PRIVATE)
        private var wallpaperWidth: Int = 0
        private var wallpaperHeight: Int = 0
        private val coroutineScope = CoroutineScope(Dispatchers.Default + Job())
        private var currentFrameIndex = 0
        private var frameGenerationJob: Job? = null
        private var backgroundColor = prefs.getInt("wallpaper_color", initColour)
        private var fps = prefs.getInt("fps", initFPS)
        private var loopSeconds = prefs.getInt("loop_seconds", initLoopSeconds)
        private var scaleFactor = prefs.getInt("scale_factor", initScaleFactor)
        private var tilingFactor = prefs.getInt("tiling_factor", initTilingFactor)
        private var rotationSupport = prefs.getBoolean("rotation_support", initRotationSupport)
        private var maxNoiseBrightness = prefs.getInt("max_noise_brightness", initMaxNoiseBrightness)
        private var noiseFrames = arrayOfNulls<Bitmap>(loopSeconds * fps)

        init {
            prefs.registerOnSharedPreferenceChangeListener(this)
        }

        private val noiseGenerationViewModel: NoiseGenerationViewModel by lazy {
            NoiseGenerationViewModel.getInstance() // Get the shared instance
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
            wallpaperWidth = holder.surfaceFrame.width()
            wallpaperHeight = holder.surfaceFrame.height()
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
                when (key) {
                    "wallpaper_color" -> {
                        backgroundColor = sharedPreferences.getInt("wallpaper_color", initColour)
                        restart = false
                    }
                    "fps" -> {
                        fps = sharedPreferences.getInt("fps", 24)
                    }
                    "loop_seconds" -> {
                        loopSeconds = sharedPreferences.getInt("loop_seconds", 3)
                    }
                    "scale_factor" -> {
                        scaleFactor = sharedPreferences.getInt("scale_factor", 1)
                    }
                    "tiling_factor" -> {
                        tilingFactor = sharedPreferences.getInt("tiling_factor", 1)
                    }
                    "max_noise_brightness" -> {
                        maxNoiseBrightness = sharedPreferences.getInt("max_noise_brightness", 256)
                    }
                    "rotation_support" -> {
                        rotationSupport = sharedPreferences.getBoolean("rotation_support", false)
                    }
                }
            }
            if (restart) {
                noiseFrames = arrayOfNulls(loopSeconds * fps)
                for (i in noiseFrames.indices) {
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
                    currentFrameIndex = if (noiseFrames[nextFrameIndex] != null) {
                        nextFrameIndex // Loop back to the start
                    } else {
                        0
                    }
                    handler.postDelayed(this, 1000L / fps) // Schedule the next frame
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
            var wallpaperTileWidth = wallpaperWidth / (tilingFactor * scaleFactor)
            var wallpaperTileHeight = wallpaperHeight / (tilingFactor * scaleFactor)
            if (rotationSupport) {
                wallpaperTileWidth =  max(wallpaperTileWidth, wallpaperTileHeight)
                wallpaperTileHeight = max(wallpaperTileWidth, wallpaperTileHeight)
            }
            frameGenerationJob?.cancel()
            frameGenerationJob = CoroutineScope(Dispatchers.Default).launch {
                for (i in noiseFrames.indices) {
                    noiseFrames[i] = generateNoiseBitmapParallel(wallpaperTileWidth, wallpaperTileHeight)
                    noiseGenerationViewModel.updateValue((i.toFloat() / noiseFrames.size))
                }
                noiseGenerationViewModel.updateValue(null)
            }
        }

        private fun tileBitmap(originalBitmap: Bitmap, multiplier: Int): Bitmap? {
            // Calculate the size of the new bitmap
            var newWidth = originalBitmap.width * multiplier
            val newHeight = originalBitmap.height * multiplier

            // Create a new bitmap with the calculated size
            val tiledBitmap =
                originalBitmap.config?.let { Bitmap.createBitmap(newWidth, newHeight, it) }
            val paint = Paint()

            // Draw the original bitmap in a grid based on the scale factor
            for (x in 0 until multiplier) {
                for (y in 0 until multiplier) {
                    // Calculate the position for each tile
                    val left = x * originalBitmap.width
                    val top = y * originalBitmap.height
                    tiledBitmap?.let { Canvas(it) }
                        ?.drawBitmap(originalBitmap, left.toFloat(), top.toFloat(), paint)
                }
            }

            return tiledBitmap
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
                            val noiseBitmap = tileBitmap(it, tilingFactor)
                            if (noiseBitmap != null) {
                                val matrix = Matrix().apply {
                                    postScale(scaleFactor.toFloat(), scaleFactor.toFloat())
                                }
                                val scaledNoiseBitmap = Bitmap.createBitmap(noiseBitmap, 0, 0, noiseBitmap.width, noiseBitmap.height, matrix, false)
                                canvas.drawBitmap(scaledNoiseBitmap, 0f, 0f, noisePaint)
                            }
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

