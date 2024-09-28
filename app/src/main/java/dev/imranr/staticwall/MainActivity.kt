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
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalStdlibApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            StaticWallTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var colorText by remember { mutableStateOf(TextFieldValue((initColour.toHexString(format = HexFormat.UpperCase)).substring(2))) }
                    var isValidColor by remember { mutableStateOf(true) }
                    var currentColor by remember { mutableStateOf(Color(initColour)) }

                    // Function to validate the color input
                    fun isValidHexColor(input: String): Boolean {
                        return input.length == 6 && input.all { it.isDigit() || it in 'A'..'F' || it in 'a'..'f' }
                    }

                    // Validate the input
                    isValidColor = isValidHexColor(colorText.text)

                    // UI with Jetpack Compose
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        // Preview box
                        Box(
                            modifier = Modifier
                                .size(100.dp)
                                .background(currentColor)
                                .padding(16.dp)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Text input for color
                        BasicTextField(
                            value = colorText,
                            onValueChange = { colorText = it },
                            modifier = Modifier
                                .width(150.dp)
                                .align(Alignment.CenterHorizontally)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Set color button
                        Button(
                            onClick = {
                                val colorString = "#" + colorText.text // Prepend `#` to hex code
                                val color = try {
                                    android.graphics.Color.parseColor(colorString)
                                } catch (e: IllegalArgumentException) {
                                    print(e);
                                    initColour // Fallback color
                                }

                                // Save the selected color in SharedPreferences
                                val sharedPrefs = getSharedPreferences("wallpaper_prefs", MODE_PRIVATE)
                                sharedPrefs.edit().putInt("wallpaper_color", color).apply()

                                // Update preview
                                currentColor = Color(color)

                            },
                            enabled = isValidColor
                        ) {
                            Text("Set Wallpaper Color")
                        }

                        // Show validation message if input is invalid
                        if (!isValidColor) {
                            Text(text = "Invalid color! Please enter a valid 6-character hex code.", color = Color.Red)
                        }
                    }
                }
            }
        }
    }
}