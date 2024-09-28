package dev.imranr.wallstopper

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import dev.imranr.wallstopper.ui.theme.WallstopperTheme
import android.content.Intent
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.runtime.*
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.foundation.layout.*
import androidx.compose.ui.unit.dp
import android.graphics.Color
import android.util.Log
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import android.widget.Toast
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {
    private val noiseGenerationViewModel: NoiseGenerationViewModel by lazy {
        NoiseGenerationViewModel.getInstance() // Get the shared instance
    }

    @OptIn(ExperimentalStdlibApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val prefs = getSharedPreferences("wallpaper_prefs", MODE_PRIVATE)

        setContent {
            val lightTheme = !isSystemInDarkTheme()
            val barColor = MaterialTheme.colorScheme.background.toArgb()
            LaunchedEffect(lightTheme) {
                if (lightTheme) {
                    enableEdgeToEdge(
                        statusBarStyle = SystemBarStyle.light(
                            barColor, barColor,
                        ),
                        navigationBarStyle = SystemBarStyle.light(
                            barColor, barColor,
                        ),
                    )
                } else {
                    Log.e("AAA", "BBB")
                    Log.e("AAA", barColor.toHexString())
                    enableEdgeToEdge(
                        statusBarStyle = SystemBarStyle.dark(
                            barColor.inv(),
                        ),
                        navigationBarStyle = SystemBarStyle.dark(
                            barColor.inv(),
                        ),
                    )
                }
            }
            var frameGenerationProgress by remember { mutableStateOf<Float?>(null) }

            // Observe LiveData from SharedViewModel
            noiseGenerationViewModel.value.observe(this) { newMessage ->
                frameGenerationProgress = newMessage
            }

            WallstopperTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var colorInput by remember { mutableStateOf(TextFieldValue(prefs.getInt("wallpaper_color", initColour).toHexString(format = HexFormat.UpperCase).substring(2))) }
                    var fpsInput by remember { mutableStateOf(TextFieldValue(prefs.getInt("fps", initFPS).toString())) }
                    var loopSecondsInput by remember { mutableStateOf(TextFieldValue(prefs.getInt("loop_seconds", initLoopSeconds).toString())) }
                    var scaleFactorInput by remember { mutableStateOf(TextFieldValue(prefs.getInt("scale_factor", initScaleFactor).toString())) }
                    var tilingFactorInput by remember { mutableStateOf(TextFieldValue(prefs.getInt("tiling_factor", initTilingFactor).toString())) }
                    var maxNoiseBrightnessInput by remember { mutableStateOf(TextFieldValue(prefs.getInt("max_noise_brightness", initMaxNoiseBrightness).toString())) }
                    Column(
                        modifier = Modifier
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 32.dp, bottom = 32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Wallstopper", // Your title text
                                fontSize = 32.sp,      // Large font size
                                modifier = Modifier.padding(bottom = 16.dp) // Bottom padding
                            )
                        }
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp, bottom = 16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            TextField(
                                value = colorInput,
                                onValueChange = {
                                    if (it.text.isEmpty() || it.text.matches(Regex("^#?[0-9a-fA-F]{0,6}$"))) {
                                        colorInput = it
                                    }
                                },
                                label = { Text("Background Color (Hex)") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            TextField(
                                value = fpsInput,
                                onValueChange = {
                                    if (it.text.isEmpty() || it.text.matches(Regex("[0-9]+")) && it.text.toInt() >= 1 && it.text.toInt() <= 300) {
                                        fpsInput = it
                                    }
                                },
                                label = { Text("FPS (1-300)") },
                                modifier = Modifier.fillMaxWidth(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )
                            TextField(
                                value = loopSecondsInput,
                                onValueChange = {
                                    if (it.text.isEmpty() || it.text.matches(Regex("[0-9]+")) && it.text.toInt() >= 1 && it.text.toInt() <= 10) {
                                        loopSecondsInput = it
                                    }
                                },
                                label = { Text("Loop Seconds (1-10)") },
                                modifier = Modifier.fillMaxWidth(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )
                            TextField(
                                value = scaleFactorInput,
                                onValueChange = {
                                    if (it.text.isEmpty() || it.text.matches(Regex("[0-9]+")) && it.text.toInt() >= 1 && it.text.toInt() <= 8) {
                                        scaleFactorInput = it
                                    }
                                },
                                label = { Text("Scale Factor (1-8)") },
                                modifier = Modifier.fillMaxWidth(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )
                            TextField(
                                value = tilingFactorInput,
                                onValueChange = {
                                    if (it.text.isEmpty() || it.text.matches(Regex("[0-9]+")) && it.text.toInt() >= 1 && it.text.toInt() <= 8) {
                                        tilingFactorInput = it
                                    }
                                },
                                label = { Text("Tiling Factor (1-8)") },
                                modifier = Modifier.fillMaxWidth(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )
                            TextField(
                                value = maxNoiseBrightnessInput,
                                onValueChange = {
                                    if (it.text.isEmpty() || it.text.matches(Regex("[0-9]+")) && it.text.toInt() >= 1 && it.text.toInt() <= 256) {
                                        maxNoiseBrightnessInput = it
                                    }
                                },
                                label = { Text("Max Noise Brightness (1-256)") },
                                modifier = Modifier.fillMaxWidth(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp, bottom = 16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Button(
                                onClick = {
                                    try {
                                        // Validate and save inputs
                                        val colorHex = colorInput.text
                                        val fps =
                                            fpsInput.text.toIntOrNull()?.coerceIn(1, 300) ?: initFPS
                                        val loopSeconds =
                                            loopSecondsInput.text.toIntOrNull()?.coerceIn(1, 10)
                                                ?: initLoopSeconds
                                        val scaleFactor =
                                            scaleFactorInput.text.toIntOrNull()?.coerceIn(1, 8)
                                                ?: initScaleFactor
                                        val tilingFactor =
                                            tilingFactorInput.text.toIntOrNull()?.coerceIn(1, 8)
                                                ?: initTilingFactor
                                        val maxNoiseBrightness =
                                            maxNoiseBrightnessInput.text.toIntOrNull()
                                                ?.coerceIn(1, 256)
                                                ?: initMaxNoiseBrightness

                                        val color = if (colorHex.startsWith("#")) {
                                            Color.parseColor(colorHex)
                                        } else {
                                            Color.parseColor("#$colorHex")
                                        }

                                        with(prefs.edit()) {
                                            putInt("wallpaper_color", color)
                                            putInt("fps", fps)
                                            putInt("loop_seconds", loopSeconds)
                                            putInt("scale_factor", scaleFactor)
                                            putInt("tiling_factor", tilingFactor)
                                            putInt("max_noise_brightness", maxNoiseBrightness)
                                            apply()
                                        }
                                        Toast.makeText(
                                            this@MainActivity,
                                            "Settings updated.",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    } catch (e: Exception) {
                                        Toast.makeText(
                                            this@MainActivity,
                                            "Invalid input.",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        throw e
                                    }
                                },
                                modifier = Modifier.padding(top = 64.dp)
                            ) {
                                Text("Update Wallpaper Settings")
                            }
                            Button(
                                onClick = {
                                    val intent = Intent(Intent.ACTION_SET_WALLPAPER)
                                    startActivity(intent)
                                },
                                modifier = Modifier.padding(top = 16.dp, bottom = 16.dp)
                            ) {
                                Text("Set Live Wallpaper")
                            }
                            if (frameGenerationProgress != null) {
                                ProgressBar(
                                    progress = frameGenerationProgress!!,
                                    label = "Generating frames...\nWallpaper may be choppy or shorter than expected until this ends."
                                )
                            }
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 96.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Bottom
                            ) {
                                ClickableWebLink("https://github.com/ImranR98/Wallstopper", "Source Code")
                            }
                        }
                    }
                }
            }
        }
    }
}