package com.composea11yscanner.sample

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.composea11yscanner.ComposeA11yScanner
import com.composea11yscanner.triggers.scanOnLongPress
import com.composea11yscanner.triggers.scanOnShake
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SampleActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                ScanTriggerDemo()
            }
        }
    }
}

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun ScanTriggerDemo(modifier: Modifier = Modifier) {
    var shakeEnabled by remember { mutableStateOf(true) }
    scanOnShake(enabled = shakeEnabled)

    Scaffold(modifier = modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = "Scan Trigger Demo",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
            )

            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val useWideLayout = maxWidth >= 720.dp
                if (useWideLayout) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        TriggerOptionCard(
                            title = "Manual",
                            body = "Button tap",
                            modifier = Modifier.weight(1f),
                        ) {
                            Button(
                                onClick = { ComposeA11yScanner.triggerScan() },
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                            ) {
                                Text("Run Scan")
                            }
                        }
                        TriggerOptionCard(
                            title = "Long Press",
                            body = "Hold this panel",
                            modifier = Modifier
                                .weight(1f)
                                .scanOnLongPress(),
                        ) {
                            TriggerTarget("Hold")
                        }
                        ShakeTriggerCard(
                            enabled = shakeEnabled,
                            onEnabledChange = { shakeEnabled = it },
                            modifier = Modifier.weight(1f),
                        )
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        TriggerOptionCard(title = "Manual", body = "Button tap") {
                            Button(onClick = { ComposeA11yScanner.triggerScan() }) {
                                Text("Run Scan")
                            }
                        }
                        TriggerOptionCard(
                            title = "Long Press",
                            body = "Hold this panel",
                            modifier = Modifier.scanOnLongPress(),
                        ) {
                            TriggerTarget("Hold")
                        }
                        ShakeTriggerCard(
                            enabled = shakeEnabled,
                            onEnabledChange = { shakeEnabled = it },
                        )
                    }
                }
            }

            DemoScanContent()
        }
    }
}

@Composable
private fun TriggerOptionCard(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = modifier.height(164.dp),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(body, style = MaterialTheme.typography.bodyMedium)
            }
            content()
        }
    }
}

@Composable
private fun ShakeTriggerCard(
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    TriggerOptionCard(
        title = "Shake",
        body = if (enabled) "Sensor armed" else "Sensor off",
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("Enabled", style = MaterialTheme.typography.labelLarge)
            Switch(checked = enabled, onCheckedChange = onEnabledChange)
        }
    }
}

@Composable
private fun TriggerTarget(label: String) {
    Box(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
            .padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun DemoScanContent() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Inspectable Content", style = MaterialTheme.typography.titleMedium)
            Text("These controls intentionally include a few accessibility issues for the scanner.")
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(Color(0xFF1976D2), RoundedCornerShape(4.dp))
                        .clickable { },
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text("Small clickable swatch")
            }
            Text(
                text = "Low contrast text sample",
                color = Color(0xFFBDBDBD),
                modifier = Modifier.background(Color.White),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ScanTriggerDemoPreview() {
    MaterialTheme {
        ScanTriggerDemo()
    }
}
