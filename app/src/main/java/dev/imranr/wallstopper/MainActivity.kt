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
import android.graphics.PorterDuff
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import android.widget.Toast
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {
    private val noiseGenerationViewModel: NoiseGenerationViewModel by lazy {
        NoiseGenerationViewModel.getInstance()
    }

    @OptIn(ExperimentalStdlibApi::class, ExperimentalMaterial3Api::class)
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

            noiseGenerationViewModel.value.observe(this) { newMessage ->
                frameGenerationProgress = newMessage
            }

            WallstopperTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var colorInput by remember { mutableStateOf(TextFieldValue(prefs.getInt("wallpaper_color", initColour).toHexString(format = HexFormat.UpperCase).substring(2))) }
                    var colorInput2 by remember { mutableStateOf(TextFieldValue(prefs.getInt("wallpaper_color_2", initSecondaryColour).toHexString(format = HexFormat.UpperCase).substring(2))) }
                    var startXPctInput by remember { mutableStateOf(TextFieldValue(prefs.getInt("start_x_pct", initStartX).toString())) }
                    var startYPctInput by remember { mutableStateOf(TextFieldValue(prefs.getInt("start_y_pct", initStartY).toString())) }
                    var endXPctInput by remember { mutableStateOf(TextFieldValue(prefs.getInt("end_x_pct", initEndX).toString())) }
                    var endYPctInput by remember { mutableStateOf(TextFieldValue(prefs.getInt("end_y_pct", initEndY).toString())) }
                    var fpsInput by remember { mutableStateOf(TextFieldValue(prefs.getInt("fps", initFPS).toString())) }
                    var loopSecondsInput by remember { mutableStateOf(TextFieldValue(prefs.getInt("loop_seconds", initLoopSeconds).toString())) }
                    var scaleFactorInput by remember { mutableStateOf(TextFieldValue(prefs.getInt("scale_factor", initScaleFactor).toString())) }
                    var tilingFactorInput by remember { mutableStateOf(TextFieldValue(prefs.getInt("tiling_factor", initTilingFactor).toString())) }
                    var minNoiseBrightnessInput by remember { mutableStateOf(TextFieldValue(prefs.getInt("min_noise_brightness", initMinNoiseBrightness).toString())) }
                    var maxNoiseBrightnessInput by remember { mutableStateOf(TextFieldValue(prefs.getInt("max_noise_brightness", initMaxNoiseBrightness).toString())) }
                    var blendModeInput by remember { mutableStateOf(prefs.getString("blend_mode", initBlendMode)) }
                    Column(
                        modifier = Modifier
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceAround
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            StatusBarSpacer()
                            Text(
                                text = "Wallstopper",
                                fontSize = 32.sp,
                            )
                        }
                        Column(
                            modifier = Modifier
                                .fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row (
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                TextField(
                                    value = colorInput,
                                    onValueChange = {
                                        if (it.text.isEmpty() || it.text.matches(Regex("^#?[0-9a-fA-F]{0,6}$"))) {
                                            colorInput = it
                                        }
                                    },
                                    label = { Text("Background Color (Hex)") },
                                    modifier = Modifier.weight(1f).fillMaxWidth()
                                )
                                TextField(
                                    value = colorInput2,
                                    onValueChange = {
                                        if (it.text.isEmpty() || it.text.matches(Regex("^#?[0-9a-fA-F]{0,6}$"))) {
                                            colorInput2 = it
                                        }
                                    },
                                    label = { Text("Background Color 2 (Hex)") },
                                    modifier = Modifier.weight(1f).fillMaxWidth()
                                )
                            }
                            Row (
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                TextField(
                                    value = startXPctInput,
                                    onValueChange = {
                                        if (it.text.isEmpty() || it.text.matches(Regex("[0-9]+")) && it.text.toInt() >= 0 && it.text.toInt() <= 100) {
                                            startXPctInput = it
                                        }
                                    },
                                    label = { Text("Start X %") },
                                    modifier = Modifier.weight(1f).fillMaxWidth(),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                )
                                TextField(
                                    value = startYPctInput,
                                    onValueChange = {
                                        if (it.text.isEmpty() || it.text.matches(Regex("[0-9]+")) && it.text.toInt() >= 0 && it.text.toInt() <= 100) {
                                            startYPctInput = it
                                        }
                                    },
                                    label = { Text("Start Y %") },
                                    modifier = Modifier.weight(1f).fillMaxWidth(),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                )
                                TextField(
                                    value = endXPctInput,
                                    onValueChange = {
                                        if (it.text.isEmpty() || it.text.matches(Regex("[0-9]+")) && it.text.toInt() >= 0 && it.text.toInt() <= 100) {
                                            endXPctInput = it
                                        }
                                    },
                                    label = { Text("End X %") },
                                    modifier = Modifier.weight(1f).fillMaxWidth(),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                )
                                TextField(
                                    value = endYPctInput,
                                    onValueChange = {
                                        if (it.text.isEmpty() || it.text.matches(Regex("[0-9]+")) && it.text.toInt() >= 0 && it.text.toInt() <= 100) {
                                            endYPctInput = it
                                        }
                                    },
                                    label = { Text("End Y %") },
                                    modifier = Modifier.weight(1f).fillMaxWidth(),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                )
                            }
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
                            Row (
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                TextField(
                                    value = minNoiseBrightnessInput,
                                    onValueChange = {
                                        if (it.text.isEmpty() || it.text.matches(Regex("[0-9]+")) && it.text.toInt() >= 1 && it.text.toInt() <= 256) {
                                            minNoiseBrightnessInput = it
                                        }
                                    },
                                    label = { Text("Min Noise Brightness (1-256)") },
                                    modifier = Modifier.weight(1f).fillMaxWidth(),
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
                                    modifier = Modifier.weight(1f).fillMaxWidth(),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                )
                            }
                            val blendModes = PorterDuff.Mode.entries
                            var blendModeDropdownExpanded by remember { mutableStateOf(false) }
                            Button(
                                onClick = { blendModeDropdownExpanded = !blendModeDropdownExpanded },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = androidx.compose.ui.graphics.Color.Transparent,
                                    contentColor = MaterialTheme.colorScheme.primary
                                ),
                            ) {
                                Text(text = "Change Blend Mode (${blendModeInput})")
                            }
                            DropdownMenu(
                                expanded = blendModeDropdownExpanded,
                                onDismissRequest = { blendModeDropdownExpanded = false },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                blendModes.forEach { mode ->
                                    DropdownMenuItem(
                                        text = { Text(mode.name) },
                                        onClick = {
                                            blendModeInput = mode.name
                                            blendModeDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth(),
                            ) {
                                Button(
                                    onClick = {
                                        try {
                                            val colorHex = colorInput.text
                                            val colorHex2 = colorInput2.text
                                            val startXPct =
                                                startXPctInput.text.toIntOrNull()?.coerceIn(0, 100) ?: initStartX
                                            val startYPct =
                                                startYPctInput.text.toIntOrNull()?.coerceIn(0, 100) ?: initStartY
                                            val endXPct =
                                                endXPctInput.text.toIntOrNull()?.coerceIn(0, 100) ?: initEndX
                                            val endYPct =
                                                endYPctInput.text.toIntOrNull()?.coerceIn(0, 100) ?: initEndY
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
                                            val minNoiseBrightness =
                                                minNoiseBrightnessInput.text.toIntOrNull()
                                                    ?.coerceIn(1, 256)
                                                    ?: initMinNoiseBrightness
                                            if (minNoiseBrightness > maxNoiseBrightness) {
                                                throw Exception("Min noise brightness must be <= max.")
                                            }
                                            val color = if (colorHex.startsWith("#")) {
                                                Color.parseColor(colorHex)
                                            } else {
                                                Color.parseColor("#$colorHex")
                                            }
                                            val color2 = if (colorHex2.startsWith("#")) {
                                                Color.parseColor(colorHex2)
                                            } else {
                                                Color.parseColor("#$colorHex2")
                                            }

                                            with(prefs.edit()) {
                                                putInt("wallpaper_color", color)
                                                putInt("wallpaper_color_2", color2)
                                                putInt("start_x_pct", startXPct)
                                                putInt("start_y_pct", startYPct)
                                                putInt("end_x_pct", endXPct)
                                                putInt("end_y_pct", endYPct)
                                                putInt("fps", fps)
                                                putInt("loop_seconds", loopSeconds)
                                                putInt("scale_factor", scaleFactor)
                                                putInt("tiling_factor", tilingFactor)
                                                putInt("min_noise_brightness", minNoiseBrightness)
                                                putInt("max_noise_brightness", maxNoiseBrightness)
                                                putString("blend_mode", blendModeInput)
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
                                            print(e.stackTraceToString())
                                        }
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Update Wallpaper Settings")
                                }
                                Spacer(modifier = Modifier.padding(start = 8.dp))
                                Button(
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.secondary
                                    ),
                                    onClick = {
                                        val intent = Intent(Intent.ACTION_SET_WALLPAPER)
                                        startActivity(intent)
                                    },
                                ) {
                                    Text("Set Wallpaper")
                                }
                            }
                        }

                        if (frameGenerationProgress != null) {
                            ProgressBar(
                                progress = frameGenerationProgress!!,
                                label = "Generating frames...\nWallpaper may be choppy or shorter than expected until this ends."
                            )
                        } else {
                            Spacer(modifier = Modifier.padding(bottom = 92.dp))
                        }
                        Column(
                            modifier = Modifier
                                .fillMaxWidth(),
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

    override fun onResume() {
        super.onResume()
        noiseGenerationViewModel.updateValue(null)
    }
}

@Composable
fun StatusBarSpacer() {
    Spacer(modifier = Modifier.windowInsetsTopHeight(WindowInsets.statusBars))
}