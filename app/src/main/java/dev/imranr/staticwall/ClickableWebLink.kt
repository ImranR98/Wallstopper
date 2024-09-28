package dev.imranr.staticwall

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.sp

@Composable
fun ClickableWebLink(url: String, linkText: String) {
    val context = LocalContext.current

    // Build the annotated string to hold the clickable text
    val annotatedString = buildAnnotatedString {
        append(linkText) // Text to display
        addStyle(
            style = SpanStyle(
                color = MaterialTheme.colorScheme.primary, // Set the link color
                textDecoration = TextDecoration.Underline // Underline to indicate it's a link
            ),
            start = 0,
            end = linkText.length
        )
        // Add annotation with the URL
        addStringAnnotation(
            tag = "URL",
            annotation = url,
            start = 0,
            end = linkText.length
        )
    }

    // Clickable text composable
    ClickableText(
        text = annotatedString,
        onClick = { offset ->
            // Get the URL annotation when the text is clicked
            annotatedString.getStringAnnotations("URL", start = offset, end = offset)
                .firstOrNull()?.let { stringAnnotation ->
                    // Open the URL in the default browser
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(stringAnnotation.item))
                    context.startActivity(intent)
                }
        }
    )
}
