package dev.imranr.staticwall

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import dev.imranr.staticwall.ui.theme.StaticWallTheme
import android.content.Intent
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.input.TextFieldValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.unit.dp
import android.content.SharedPreferences
import android.graphics.Color
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.unit.dp
import android.widget.Toast
import android.provider.Settings
import android.util.Log
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalStdlibApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = getSharedPreferences("wallpaper_prefs", MODE_PRIVATE)
        setContent {
            StaticWallTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var colorInput by remember { mutableStateOf(TextFieldValue(prefs.getInt("wallpaper_color", initColour).toHexString(format = HexFormat.UpperCase).substring(2))) }
                    var fpsInput by remember { mutableStateOf(TextFieldValue(prefs.getInt("fps", initFPS).toString())) }
                    var loopSecondsInput by remember { mutableStateOf(TextFieldValue(prefs.getInt("loop_seconds", initLoopSeconds).toString())) }
                    var scaleFactorInput by remember { mutableStateOf(TextFieldValue(prefs.getInt("scale_factor", initScaleFactor).toString())) }
                    var maxNoiseBrightnessInput by remember { mutableStateOf(TextFieldValue(prefs.getInt("max_noise_brightness", initMaxNoiseBrightness).toString())) }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        TextField(
                            value = colorInput,
                            onValueChange = {
                                if (it.text.isEmpty() || it.text.matches(Regex("^#?[0-9a-fA-F]{0,6}$"))) {
                                    colorInput = it
                                }
                            },
                            label = { Text("Background Color (Hex, without #)") },
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
                                    val maxNoiseBrightness =
                                        maxNoiseBrightnessInput.text.toIntOrNull()?.coerceIn(1, 256)
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
                                        putInt("max_noise_brightness", maxNoiseBrightness)
                                        apply()
                                    }
                                    Toast.makeText(this@MainActivity, "Settings updated.", Toast.LENGTH_SHORT).show()
                                } catch (e: Exception) {
                                    Log.wtf("MainActivity", e.toString())
                                    Toast.makeText(this@MainActivity, "Invalid input.", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.padding(top = 16.dp)
                        ) {
                            Text("Update Wallpaper Settings")
                        }

                        Button(
                            onClick = {
                                val intent = Intent(Intent.ACTION_SET_WALLPAPER)
                                startActivity(intent)
                            },
                            modifier = Modifier.padding(top = 16.dp)
                        ) {
                            Text("Set Live Wallpaper")
                        }
                    }
                }
            }
        }
    }
}