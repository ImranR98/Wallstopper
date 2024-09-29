package dev.imranr.wallstopper

import android.graphics.*
import android.os.Handler
import android.os.Looper
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder
import android.content.SharedPreferences
import android.util.Log
import kotlinx.coroutines.*
import kotlin.random.Random
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import kotlin.math.max

val initColour = Color.parseColor("#1D0130")
val initSecondaryColour = Color.parseColor("#FC056C")
const val initStartX = 50
const val initStartY = 60
const val initEndX = 100
const val initEndY = 100
const val initFPS = 60
const val initLoopSeconds = 1
const val initScaleFactor = 2
const val initTilingFactor = 2
const val initMinNoiseBrightness = 1
const val initMaxNoiseBrightness = 23
var initBlendMode = PorterDuff.Mode.SCREEN.name

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

    @OptIn(ExperimentalStdlibApi::class)
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
        private var backgroundSecondaryColor = prefs.getInt("wallpaper_color_2", initSecondaryColour)
        private var startXPct = prefs.getInt("start_x_pct", initStartX)
        private var startYPct = prefs.getInt("start_y_pct", initStartY)
        private var endXPct = prefs.getInt("end_x_pct", initEndX)
        private var endYPct = prefs.getInt("end_y_pct", initEndY)
        private var fps = prefs.getInt("fps", initFPS)
        private var loopSeconds = prefs.getInt("loop_seconds", initLoopSeconds)
        private var scaleFactor = prefs.getInt("scale_factor", initScaleFactor)
        private var tilingFactor = prefs.getInt("tiling_factor", initTilingFactor)
        private var minNoiseBrightness = prefs.getInt("min_noise_brightness", initMinNoiseBrightness)
        private var maxNoiseBrightness = prefs.getInt("max_noise_brightness", initMaxNoiseBrightness)
        private var gradientPaint = Paint()
        private var blendMode = prefs.getString("blend_mode", initBlendMode)
        private var noiseFrames = arrayOfNulls<Bitmap>(loopSeconds * fps)

        init {
            prefs.registerOnSharedPreferenceChangeListener(this)
            noisePaint.setXfermode(PorterDuffXfermode(PorterDuff.Mode.entries.find { it.name == blendMode }))
        }

        private val noiseGenerationViewModel: NoiseGenerationViewModel by lazy {
            NoiseGenerationViewModel.getInstance()
        }

        override fun onVisibilityChanged(visible: Boolean) {
            this.visible = visible
            if (visible) {
                draw()
                handler.post(animationRunnable)
            } else {
                handler.removeCallbacks(animationRunnable)
            }
        }

        override fun onSurfaceCreated(holder: SurfaceHolder) {
            super.onSurfaceCreated(holder)
            handler.post(animationRunnable)
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            super.onSurfaceDestroyed(holder)
            handler.removeCallbacks(animationRunnable)
            visible = false
        }

        override fun onDestroy() {
            super.onDestroy()
            prefs.unregisterOnSharedPreferenceChangeListener(this)
            frameGenerationJob?.cancel()
            noiseFrames.forEach {
                it?.recycle()
            }
        }

        override fun onSurfaceChanged(
            holder: SurfaceHolder,
            format: Int,
            width: Int,
            height: Int
        ) {
            super.onSurfaceChanged(holder, format, width, height)
            if (width != wallpaperWidth || height != wallpaperHeight) {
                wallpaperWidth = width
                wallpaperHeight = height
                generateGradient()
                generateNoiseFrames()
            }
        }

        @OptIn(ExperimentalStdlibApi::class)
        override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
            noiseGenerationViewModel.updateValue(null)
            var regenerateNoise = false
            var regenerateGradient = false
            if (sharedPreferences != null) {
                when (key) {
                    "wallpaper_color" -> {
                        backgroundColor = sharedPreferences.getInt("wallpaper_color", initColour)
                        regenerateGradient = true
                    }
                    "wallpaper_color_2" -> {
                        backgroundSecondaryColor = sharedPreferences.getInt("wallpaper_color_2", initSecondaryColour)
                        regenerateGradient = true
                    }
                    "start_x_pct" -> {
                        startXPct = sharedPreferences.getInt("start_x_pct", initStartX)
                        regenerateGradient = true
                    }
                    "start_y_pct" -> {
                        startYPct = sharedPreferences.getInt("start_y_pct", initStartY)
                        regenerateGradient = true
                    }
                    "end_x_pct" -> {
                        endXPct = sharedPreferences.getInt("end_x_pct", initEndX)
                        regenerateGradient = true
                    }
                    "end_y_pct" -> {
                        endYPct = sharedPreferences.getInt("end_y_pct", initEndY)
                        regenerateGradient = true
                    }
                    "fps" -> {
                        fps = sharedPreferences.getInt("fps", initFPS)
                        regenerateNoise = true
                    }
                    "loop_seconds" -> {
                        loopSeconds = sharedPreferences.getInt("loop_seconds", initLoopSeconds)
                        regenerateNoise = true
                    }
                    "scale_factor" -> {
                        scaleFactor = sharedPreferences.getInt("scale_factor", initScaleFactor)
                        regenerateNoise = true
                    }
                    "tiling_factor" -> {
                        tilingFactor = sharedPreferences.getInt("tiling_factor", initTilingFactor)
                        regenerateNoise = true
                    }
                    "min_noise_brightness" -> {
                        minNoiseBrightness = sharedPreferences.getInt("min_noise_brightness", initMinNoiseBrightness)
                        regenerateNoise = true
                    }
                    "max_noise_brightness" -> {
                        maxNoiseBrightness = sharedPreferences.getInt("max_noise_brightness", initMaxNoiseBrightness)
                        regenerateNoise = true
                    }
                    "blend_mode" -> {
                        blendMode = sharedPreferences.getString("blend_mode", initBlendMode)
                        noisePaint.setXfermode(PorterDuffXfermode(PorterDuff.Mode.entries.find { it.name == blendMode }))
                    }
                }
            }
            if (regenerateGradient) {
                generateGradient()
            }
            if (regenerateNoise) {
                noiseFrames = arrayOfNulls(loopSeconds * fps)
                generateNoiseFrames()
            }
        }

        private val animationRunnable = object : Runnable {
            override fun run() {
                if (visible && noiseFrames.isNotEmpty()) {
                    draw()
                    val nextFrameIndex = (currentFrameIndex + 1) % noiseFrames.size
                    currentFrameIndex = if (noiseFrames[nextFrameIndex] != null)  nextFrameIndex else 0
                    handler.postDelayed(this, 1000L / fps)
                }
            }
        }

        private suspend fun generateNoiseBitmap(width: Int, height: Int): Bitmap {
            val noiseBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val pixels = IntArray(width * height)
            val chunkSize = pixels.size / Runtime.getRuntime().availableProcessors()
            val jobs = mutableListOf<Job>()
            for (i in pixels.indices step chunkSize) {
                val end = minOf(i + chunkSize, pixels.size)
                jobs.add(coroutineScope.launch {
                    for (j in i until end) {
                        val noise = Random.nextInt(minNoiseBrightness, maxNoiseBrightness)
                        pixels[j] = Color.argb(255, noise, noise, noise)
                    }
                })
            }
            jobs.joinAll()
            noiseBitmap.setPixels(pixels, 0, width, 0, 0, width, height)
            return noiseBitmap
        }

        private fun tileBitmap(originalBitmap: Bitmap, multiplier: Int): Bitmap {
            val newWidth = originalBitmap.width * multiplier
            val newHeight = originalBitmap.height * multiplier
            val tiledBitmap =
                originalBitmap.config.let { Bitmap.createBitmap(newWidth, newHeight, it!!) }
            val paint = Paint()
            for (x in 0 until multiplier) {
                for (y in 0 until multiplier) {
                    val left = x * originalBitmap.width
                    val top = y * originalBitmap.height
                    Canvas(tiledBitmap)
                        .drawBitmap(originalBitmap, left.toFloat(), top.toFloat(), paint)
                }
            }
            return tiledBitmap
        }

        private fun generateGradient() {
            val startX = (startXPct / 100F) * wallpaperWidth
            val startY = (startYPct / 100F) * wallpaperHeight
            val endX = (endXPct / 100F) * wallpaperWidth
            val endY = (endYPct / 100F) * wallpaperHeight
            val gradient = LinearGradient(
                startX, startY, endX, endY,
                backgroundColor, backgroundSecondaryColor,
                Shader.TileMode.CLAMP
            )
            gradientPaint.shader = gradient
        }

        @OptIn(ExperimentalStdlibApi::class)
        private fun generateNoiseFrames() {
            var wallpaperTileWidth = wallpaperWidth / (tilingFactor * scaleFactor)
            var wallpaperTileHeight = wallpaperHeight / (tilingFactor * scaleFactor)
            frameGenerationJob?.cancel()
            frameGenerationJob = CoroutineScope(Dispatchers.Default).launch {
                for (i in noiseFrames.indices) {
                    var noiseFrame = generateNoiseBitmap(wallpaperTileWidth, wallpaperTileHeight)
                    if (tilingFactor > 1) {
                        noiseFrame = tileBitmap(noiseFrame, tilingFactor)
                    }
                    if (scaleFactor > 1) {
                        val scalingMatrix = Matrix().apply {
                            postScale(scaleFactor.toFloat(), scaleFactor.toFloat())
                        }
                        noiseFrame = Bitmap.createBitmap(
                            noiseFrame,
                            0,
                            0,
                            noiseFrame.width,
                            noiseFrame.height,
                            scalingMatrix,
                            false
                        )
                    }
                    noiseFrames[i] = noiseFrame
                    noiseGenerationViewModel.updateValue((i.toFloat() / noiseFrames.size))
                }
                noiseGenerationViewModel.updateValue(null)
            }
        }

        private fun draw() {
            val holder = surfaceHolder
            val canvas: Canvas? = holder.lockCanvas()
            if (canvas != null) {
                try {
                    noiseFrames[currentFrameIndex]?.let {
                        canvas.drawRect(
                            0f,
                            0f,
                            wallpaperWidth.toFloat(),
                            wallpaperHeight.toFloat(),
                            gradientPaint
                        )
                        canvas.drawBitmap(it, 0f, 0f, noisePaint)
                    }
                } finally {
                    holder.unlockCanvasAndPost(canvas)
                }
            }
        }
    }
}

