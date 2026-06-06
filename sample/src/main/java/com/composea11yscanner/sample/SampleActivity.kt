package com.composea11yscanner.sample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.composea11yscanner.ComposeA11yScanner
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SampleActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                BrokenAccessibilitySampleApp()
            }
        }
    }
}

@Composable
fun BrokenAccessibilitySampleApp(modifier: Modifier = Modifier) {
    var selectedScreen by remember { mutableIntStateOf(0) }
    val screens = listOf("Login", "Feed", "Form")

    Scaffold(modifier = modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = "Broken A11y Samples",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "Pick a screen, then run the scanner.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Button(onClick = { ComposeA11yScanner.triggerScan() }) {
                    Text("Run Scan")
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                screens.forEachIndexed { index, label ->
                    Button(
                        onClick = { selectedScreen = index },
                        enabled = selectedScreen != index,
                    ) {
                        Text(label)
                    }
                }
            }

            when (selectedScreen) {
                0 -> BrokenLoginScreen()
                1 -> BrokenFeedScreen()
                else -> BrokenFormScreen()
            }
        }
    }
}

@Composable
private fun BrokenLoginScreen() {
    BrokenScreenCard(
        title = "Screen 1: Broken Login",
        subtitle = "Missing content descriptions and small touch targets",
    ) {
        var email by remember { mutableStateOf("") }
        var password by remember { mutableStateOf("") }

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            modifier = Modifier.fillMaxWidth(),
        )

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            TinyClickableLabel("?", Color(0xFF006D77))
            TinyClickableLabel("!", Color(0xFFE29578))
            TinyClickableLabel("x", Color(0xFF8D99AE))
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF2A9D8F))
                    .clickable { },
                contentAlignment = Alignment.Center,
            ) {
                Text("Go", color = Color.White)
            }
            Text(
                text = "Forgot password",
                modifier = Modifier
                    .size(width = 92.dp, height = 30.dp)
                    .clickable { },
                color = Color(0xFF1565C0),
            )
        }
    }
}

@Composable
private fun BrokenFeedScreen() {
    BrokenScreenCard(
        title = "Screen 2: Broken Feed",
        subtitle = "Poor contrast text over images and duplicate descriptions",
    ) {
        FeedHero(
            imageColor = Color(0xFF9E9E9E),
            title = "Morning trail update",
            description = "Open story",
        )
        FeedHero(
            imageColor = Color(0xFF78909C),
            title = "City lights tonight",
            description = "Open story",
        )

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            DuplicateAction("Save", "Save item")
            DuplicateAction("Bookmark", "Save item")
        }
    }
}

@Composable
private fun BrokenFormScreen() {
    BrokenScreenCard(
        title = "Screen 3: Broken Form",
        subtitle = "Clickable elements lack roles and source order breaks focus flow",
    ) {
        Text("Preference form", style = MaterialTheme.typography.titleMedium)

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .background(Color(0xFFECEFF1), RoundedCornerShape(8.dp)),
        ) {
            FormAction(
                text = "Submit preferences",
                color = Color(0xFF00796B),
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp),
            )
            FormAction(
                text = "Choose plan",
                color = Color(0xFF5E35B1),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp),
            )
            FormAction(
                text = "Reset fields",
                color = Color(0xFFC62828),
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(16.dp),
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            FormAction("Monthly", Color(0xFF455A64), Modifier.weight(1f))
            FormAction("Yearly", Color(0xFF455A64), Modifier.weight(1f))
        }
    }
}

@Composable
private fun BrokenScreenCard(
    title: String,
    subtitle: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                Text(subtitle, style = MaterialTheme.typography.bodyMedium)
            }
            content()
        }
    }
}

@Composable
private fun TinyClickableLabel(text: String, color: Color) {
    Box(
        modifier = Modifier
            .size(28.dp)
            .background(color, RoundedCornerShape(6.dp))
            .clickable { },
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = Color.White, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun FeedHero(
    imageColor: Color,
    title: String,
    description: String,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp)
            .clip(RoundedCornerShape(8.dp))
            .semantics { contentDescription = description }
            .clickable { },
    ) {
        Image(
            painter = ColorPainter(imageColor),
            contentDescription = description,
            modifier = Modifier.fillMaxSize(),
        )
        Text(
            text = title,
            color = Color(0xFFBDBDBD),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(14.dp),
        )
    }
}

@Composable
private fun DuplicateAction(label: String, description: String) {
    Box(
        modifier = Modifier
            .size(width = 132.dp, height = 52.dp)
            .background(Color(0xFF283593), RoundedCornerShape(8.dp))
            .semantics { contentDescription = description }
            .clickable { },
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = Color.White)
    }
}

@Composable
private fun FormAction(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .height(56.dp)
            .background(color, RoundedCornerShape(8.dp))
            .clickable { }
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = Color.White, fontWeight = FontWeight.SemiBold)
    }
}

@Preview(showBackground = true)
@Composable
fun BrokenAccessibilitySampleAppPreview() {
    MaterialTheme {
        BrokenAccessibilitySampleApp()
    }
}
