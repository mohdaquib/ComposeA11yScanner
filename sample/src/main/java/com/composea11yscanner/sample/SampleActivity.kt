package com.composea11yscanner.sample

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.platform.AbstractComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.semantics.SemanticsOwner
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composea11yscanner.core.model.A11yNode
import com.composea11yscanner.core.model.ScannerConfig
import com.composea11yscanner.rules.ScannerRules
import com.composea11yscanner.sample.ui.theme.ScannerTheme
import com.composea11yscanner.triggers.scanOnShake
import com.composea11yscanner.ui.A11yNodeExtractor
import com.composea11yscanner.ui.A11yScannerController
import com.composea11yscanner.ui.A11yScannerScaffold

class SampleActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ScannerTheme {
                BrokenAccessibilitySampleApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrokenAccessibilitySampleApp(modifier: Modifier = Modifier) {
    val activity = LocalContext.current as? ComponentActivity
    var selectedScreen by remember { mutableIntStateOf(0) }
    var viewingFixed by remember { mutableStateOf(false) }
    var scanScrollY by remember { mutableIntStateOf(0) }
    val scrollState = rememberScrollState()
    val screens = listOf("Login", "Feed", "Form")
    val screenSubtitles = listOf(
        "Credentials and compact actions",
        "Stories, captions, and repeated labels",
        "Form controls and focus order",
    )
    val issueOffsetY by remember {
        derivedStateOf { scanScrollY - scrollState.value }
    }
    val scannerController = remember(activity) {
        A11yScannerController(
            nodeProvider = {
                scanScrollY = scrollState.value
                activity?.extractBrokenSampleNodes().orEmpty()
            },
            screenDensity = activity?.resources?.displayMetrics?.density ?: 1f,
        )
    }
    val scannerConfig = remember(selectedScreen, viewingFixed) {
        ScannerConfig(
            enabledRules = ScannerRules.allRuleIds().toSet(),
            autoScan = false,
        )
    }
    fun startSampleScan() {
        scanScrollY = scrollState.value
        scannerController.startScan()
    }

    LaunchedEffect(selectedScreen, viewingFixed) {
        scanScrollY = scrollState.value
        scannerController.clearState()
    }

    scanOnShake(onScanRequested = { startSampleScan() })

    A11yScannerScaffold(
        scannerController = scannerController,
        config = scannerConfig,
        modifier = modifier.fillMaxSize(),
        issueOffsetY = issueOffsetY,
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = "Compose A11y Scanner",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                        )
                    },
                    actions = {
                        IconButton(onClick = { scannerController.clearState() }) {
                            Icon(
                                imageVector = Icons.Filled.Clear,
                                contentDescription = "Clear scan results",
                            )
                        }
                        IconButton(onClick = { startSampleScan() }) {
                            Icon(
                                imageVector = Icons.Filled.Search,
                                contentDescription = "Scan selected sample",
                            )
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                    ),
                )
            },
            bottomBar = {
                NavigationBar {
                    screens.forEachIndexed { index, label ->
                        NavigationBarItem(
                            selected = selectedScreen == index,
                            onClick = {
                                selectedScreen = index
                                viewingFixed = false
                            },
                            icon = {
                                Icon(
                                    imageVector = if (selectedScreen == index) {
                                        Icons.Filled.CheckCircle
                                    } else {
                                        Icons.Filled.Search
                                    },
                                    contentDescription = null,
                                )
                            },
                            label = { Text(label) },
                        )
                    }
                }
            },
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                    .verticalScroll(scrollState)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    ),
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(2.dp),
                            ) {
                                Text(
                                    text = screens[selectedScreen],
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Text(
                                    text = screenSubtitles[selectedScreen],
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            FilterChip(
                                selected = !viewingFixed,
                                onClick = { viewingFixed = false },
                                label = { Text("Broken") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Filled.Warning,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                    )
                                },
                            )
                            FilterChip(
                                selected = viewingFixed,
                                onClick = { viewingFixed = true },
                                label = { Text("Fixed") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Filled.CheckCircle,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                    )
                                },
                            )
                        }
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 1.dp,
                ) {
                    Box(
                        modifier = Modifier
                            .padding(8.dp)
                            .semantics { testTag = BrokenSampleContentTag },
                    )
                    {
                        if (viewingFixed) {
                            when (selectedScreen) {
                                0 -> FixedLoginScreen()
                                1 -> FixedFeedScreen()
                                else -> FixedFormScreen()
                            }
                        } else {
                            when (selectedScreen) {
                                0 -> BrokenLoginScreen()
                                1 -> BrokenFeedScreen()
                                else -> BrokenFormScreen()
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BrokenLoginScreen(onViewFixed: (() -> Unit)? = null) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF0D1117))
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            BankLogoMark()
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "SecureBank",
                    color = Color.White,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "Private client access",
                    color = Color(0xFF8B949E),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Welcome back",
                color = Color.White,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "Sign in to view balances, transfers, and card activity.",
                color = Color(0xFF8B949E),
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
                trailingIcon = {
                    Text(
                        text = if (passwordVisible) "Hide" else "Show",
                        color = Color(0xFF6C63FF),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .size(width = 44.dp, height = 30.dp)
                            .clickable { passwordVisible = !passwordVisible },
                    )
                },
                visualTransformation = if (passwordVisible) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(Color(0xFF6C63FF), Color(0xFF00D4AA)),
                    ),
                )
                .clickable { },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "Sign In",
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Forgot password?",
                color = Color(0xFF4DA6FF),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier
                    .size(width = 108.dp, height = 30.dp)
                    .clickable { },
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TinyClickableLabel("?", Color(0xFF1C2B3A))
                TinyClickableLabel("!", Color(0xFF2E2A1C))
                TinyClickableLabel("x", Color(0xFF281C2E))
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF121826))
                .padding(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "Device trust",
                        color = Color.White,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "Review this session",
                        color = Color(0xFF8B949E),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF00D4AA))
                        .clickable { },
                    contentAlignment = Alignment.Center,
                ) {
                    Text("Go", color = Color(0xFF0D1117), fontWeight = FontWeight.Bold)
                }
            }
        }

        onViewFixed?.let {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF1C1C28))
                    .clickable { it() },
                contentAlignment = Alignment.Center,
            ) {
                Text("View Fixed Version")
            }
        }
    }
}

@Composable
private fun BankLogoMark() {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF6C63FF)),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(width = 24.dp, height = 10.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(Color.White.copy(alpha = 0.92f)),
        )
        Box(
            modifier = Modifier
                .size(width = 10.dp, height = 24.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(Color.White.copy(alpha = 0.72f)),
        )
    }
}

@Composable
private fun BrokenFeedScreen(onViewFixed: (() -> Unit)? = null) {
    BrokenScreenCard(
        title = "Market Pulse",
        subtitle = "Live financial news and price movers",
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MarketFilterChip("All", selected = true)
            MarketFilterChip("Stocks", selected = false)
            MarketFilterChip("Crypto", selected = false)
        }

        FinancialNewsCard(
            monogram = "A",
            logoColor = Color(0xFF6C63FF),
            headline = "Apex Bank rallies as mobile deposits hit quarterly record",
            priceChange = "+4.8%",
            isPositive = true,
            timestamp = "2m ago",
            chartStart = Color(0xFF26385F),
            chartEnd = Color(0xFF121826),
            description = "Open market story",
        )
        FinancialNewsCard(
            monogram = "N",
            logoColor = Color(0xFFFF4D6A),
            headline = "NovaPay slips after analysts flag rising card loss reserves",
            priceChange = "-2.1%",
            isPositive = false,
            timestamp = "11m ago",
            chartStart = Color(0xFF4B2332),
            chartEnd = Color(0xFF161923),
            description = "Open market story",
        )
        onViewFixed?.let {
            Button(onClick = it, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                Text("View Fixed Version")
            }
        }
    }
}

@Composable
private fun MarketFilterChip(label: String, selected: Boolean) {
    Box(
        modifier = Modifier
            .height(48.dp)
            .background(
                color = if (selected) Color(0xFF6C63FF) else Color(0xFF1C1C28),
                shape = RoundedCornerShape(24.dp),
            )
            .clickable(
                role = Role.Button,
                onClick = {},
            )
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = if (selected) Color.White else Color(0xFF8B949E),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun FinancialNewsCard(
    monogram: String,
    logoColor: Color,
    headline: String,
    priceChange: String,
    isPositive: Boolean,
    timestamp: String,
    chartStart: Color,
    chartEnd: Color,
    description: String,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(146.dp)
            .clip(RoundedCornerShape(8.dp))
            .semantics { contentDescription = description }
            .clickable { },
    ) {
        Image(
            painter = ColorPainter(chartStart),
            contentDescription = description,
            modifier = Modifier.fillMaxSize(),
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawBehind {
                    drawRect(
                        brush = Brush.linearGradient(
                            colors = listOf(chartStart.copy(alpha = 0.35f), chartEnd),
                            start = Offset.Zero,
                            end = Offset(size.width, size.height),
                        ),
                    )
                    repeat(5) { index ->
                        val y = size.height * (0.24f + index * 0.13f)
                        drawLine(
                            color = Color.White.copy(alpha = 0.09f),
                            start = Offset(0f, y + index * 12f),
                            end = Offset(size.width, y - 48f),
                            strokeWidth = 2.5f,
                        )
                    }
                    drawLine(
                        color = Color(0xFF00D4AA).copy(alpha = 0.34f),
                        start = Offset(0f, size.height * 0.78f),
                        end = Offset(size.width, size.height * 0.34f),
                        strokeWidth = 5f,
                    )
                },
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .background(logoColor, RoundedCornerShape(21.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = monogram,
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = headline,
                        color = Color(0xFF8B949E),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = timestamp,
                        color = Color(0xFF6E7681),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
            Box(
                modifier = Modifier
                    .background(
                        color = if (isPositive) Color(0xFF164E3F) else Color(0xFF5A1D2A),
                        shape = RoundedCornerShape(14.dp),
                    )
                    .padding(horizontal = 9.dp, vertical = 5.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = priceChange,
                    color = if (isPositive) Color(0xFF00D4AA) else Color(0xFFFF4D6A),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun BrokenFormScreen(onViewFixed: (() -> Unit)? = null) {
    BrokenScreenCard(
        title = "Payment form",
        subtitle = "Transfer details with intentionally broken focus order",
    ) {
        var amount by remember { mutableStateOf("") }
        var recipient by remember { mutableStateOf("") }
        var reference by remember { mutableStateOf("") }
        var paymentDate by remember { mutableStateOf("") }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(460.dp)
                .background(Color(0xFF0D1117), RoundedCornerShape(8.dp))
                .padding(16.dp),
        ) {
            PaymentSubmitAction(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth(),
            )

            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it },
                label = { Text("Amount") },
                prefix = {
                    Text(
                        text = "$",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                    )
                },
                textStyle = MaterialTheme.typography.headlineMedium.copy(
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                ),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .fillMaxWidth(),
            )

            Row(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(top = 116.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(Color(0xFF6C63FF), RoundedCornerShape(28.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "MJ",
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
                OutlinedTextField(
                    value = recipient,
                    onValueChange = { recipient = it },
                    label = { Text("Recipient Account") },
                    modifier = Modifier.weight(1f),
                )
            }

            OutlinedTextField(
                value = paymentDate,
                onValueChange = { paymentDate = it },
                label = { Text("Payment Date") },
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(top = 210.dp)
                    .fillMaxWidth(),
            )

            OutlinedTextField(
                value = reference,
                onValueChange = { reference = it },
                label = { Text("Reference Note") },
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(top = 294.dp)
                    .fillMaxWidth(),
            )
        }

        onViewFixed?.let {
            Button(onClick = it, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                Text("View Fixed Version")
            }
        }
    }
}

@Composable
private fun PaymentSubmitAction(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .height(56.dp)
            .background(
                brush = Brush.horizontalGradient(
                    colors = listOf(Color(0xFF6C63FF), Color(0xFF00D4AA)),
                ),
                shape = RoundedCornerShape(16.dp),
            )
            .clickable(
                role = Role.Button,
                onClick = {},
            ),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Filled.Lock,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = "Send Payment",
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
fun FixedLoginScreen(onViewBroken: (() -> Unit)? = null) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF0D1117))
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            BankLogoMark()
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "SecureBank",
                    color = Color.White,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "Private client access",
                    color = Color(0xFF8B949E),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Welcome back",
                color = Color.White,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "Sign in to view balances, transfers, and card activity.",
                color = Color(0xFF8B949E),
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
                trailingIcon = {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .semantics {
                                contentDescription = if (passwordVisible) {
                                    "Hide password"
                                } else {
                                    "Show password"
                                }
                            }
                            .clickable(
                                role = Role.Button,
                                onClick = { passwordVisible = !passwordVisible },
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = if (passwordVisible) "Hide" else "Show",
                            color = Color(0xFF6C63FF),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                },
                visualTransformation = if (passwordVisible) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(Color(0xFF6C63FF), Color(0xFF00D4AA)),
                    ),
                )
                .semantics { contentDescription = "Sign in" }
                .clickable(
                    role = Role.Button,
                    onClick = {},
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "Sign In",
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .semantics { contentDescription = "Reset password" }
                    .clickable(
                        role = Role.Button,
                        onClick = {},
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "Forgot password?",
                    color = Color(0xFF4DA6FF),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FixedIconAction("?", "Open login help", Color(0xFF1C2B3A))
                FixedIconAction("!", "Show login requirements", Color(0xFF2E2A1C))
                FixedIconAction("x", "Dismiss login form", Color(0xFF281C2E))
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF121826))
                .padding(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "Device trust",
                        color = Color.White,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "Review this session",
                        color = Color(0xFF8B949E),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF00D4AA))
                        .semantics { contentDescription = "Review trusted device session" }
                        .clickable(
                            role = Role.Button,
                            onClick = {},
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("Go", color = Color(0xFF0D1117), fontWeight = FontWeight.Bold)
                }
            }
        }

        onViewBroken?.let {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF1C1C28))
                    .semantics { contentDescription = "View broken version" }
                    .clickable(
                        role = Role.Button,
                        onClick = { it() },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text("View Broken Version")
            }
        }
    }
}

@Composable
fun FixedFeedScreen(onViewBroken: (() -> Unit)? = null) {
    BrokenScreenCard(
        title = "Market Pulse",
        subtitle = "Accessible financial news and price movers",
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MarketFilterChip("All", selected = true)
            MarketFilterChip("Stocks", selected = false)
            MarketFilterChip("Crypto", selected = false)
        }

        FixedFinancialNewsCard(
            monogram = "A",
            logoColor = Color(0xFF6C63FF),
            headline = "Apex Bank rallies as mobile deposits hit quarterly record",
            priceChange = "+4.8%",
            isPositive = true,
            timestamp = "2m ago",
            chartStart = Color(0xFF26385F),
            chartEnd = Color(0xFF121826),
            description = "Open Apex Bank market story",
        )
        FixedFinancialNewsCard(
            monogram = "N",
            logoColor = Color(0xFFFF4D6A),
            headline = "NovaPay slips after analysts flag rising card loss reserves",
            priceChange = "-2.1%",
            isPositive = false,
            timestamp = "11m ago",
            chartStart = Color(0xFF4B2332),
            chartEnd = Color(0xFF161923),
            description = "Open NovaPay market story",
        )

        onViewBroken?.let {
            Button(onClick = it, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                Text("View Broken Version")
            }
        }
    }
}

@Composable
private fun FixedFinancialNewsCard(
    monogram: String,
    logoColor: Color,
    headline: String,
    priceChange: String,
    isPositive: Boolean,
    timestamp: String,
    chartStart: Color,
    chartEnd: Color,
    description: String,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(146.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF121826))
            .drawBehind {
                drawRect(
                    brush = Brush.linearGradient(
                        colors = listOf(chartStart.copy(alpha = 0.45f), chartEnd),
                        start = Offset.Zero,
                        end = Offset(size.width, size.height),
                    ),
                )
                repeat(5) { index ->
                    val y = size.height * (0.24f + index * 0.13f)
                    drawLine(
                        color = Color.White.copy(alpha = 0.08f),
                        start = Offset(0f, y + index * 12f),
                        end = Offset(size.width, y - 48f),
                        strokeWidth = 2.5f,
                    )
                }
                drawLine(
                    color = Color(0xFF00D4AA).copy(alpha = 0.28f),
                    start = Offset(0f, size.height * 0.78f),
                    end = Offset(size.width, size.height * 0.34f),
                    strokeWidth = 5f,
                )
            }
            .semantics { contentDescription = description }
            .clickable(
                role = Role.Button,
                onClick = {},
            ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.54f))
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .background(logoColor, RoundedCornerShape(21.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = monogram,
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = headline,
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = timestamp,
                        color = Color(0xFFD0D7DE),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
            Box(
                modifier = Modifier
                    .background(
                        color = if (isPositive) Color(0xFF164E3F) else Color(0xFF5A1D2A),
                        shape = RoundedCornerShape(14.dp),
                    )
                    .padding(horizontal = 9.dp, vertical = 5.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = priceChange,
                    color = if (isPositive) Color(0xFF00D4AA) else Color(0xFFFF4D6A),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
fun FixedFormScreen(onViewBroken: (() -> Unit)? = null) {
    BrokenScreenCard(
        title = "Payment form",
        subtitle = "Accessible transfer details with top-to-bottom focus order",
    ) {
        var amount by remember { mutableStateOf("") }
        var recipient by remember { mutableStateOf("") }
        var reference by remember { mutableStateOf("") }
        var paymentDate by remember { mutableStateOf("") }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF0D1117), RoundedCornerShape(8.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it },
                label = { Text("Amount") },
                prefix = {
                    Text(
                        text = "$",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                    )
                },
                textStyle = MaterialTheme.typography.headlineMedium.copy(
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                ),
                modifier = Modifier.fillMaxWidth(),
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(Color(0xFF6C63FF), RoundedCornerShape(28.dp))
                        .semantics { contentDescription = "Recipient avatar for Maya Johnson" },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "MJ",
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
                OutlinedTextField(
                    value = recipient,
                    onValueChange = { recipient = it },
                    label = { Text("Recipient Account") },
                    modifier = Modifier.weight(1f),
                )
            }

            OutlinedTextField(
                value = paymentDate,
                onValueChange = { paymentDate = it },
                label = { Text("Payment Date") },
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = reference,
                onValueChange = { reference = it },
                label = { Text("Reference Note") },
                modifier = Modifier.fillMaxWidth(),
            )

            PaymentSubmitAction(modifier = Modifier.fillMaxWidth())
        }

        onViewBroken?.let {
            Button(
                onClick = it,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
            ) {
                Text("View Broken Version")
            }
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

@Composable
private fun FixedIconAction(
    label: String,
    description: String,
    color: Color,
) {
    Box(
        modifier = Modifier
            .size(56.dp)
            .background(color, RoundedCornerShape(8.dp))
            .semantics { contentDescription = description }
            .clickable(
                role = Role.Button,
                onClick = {},
        ),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = Color.White, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun FixedFeedItem(
    imageColor: Color,
    title: String,
    description: String,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = description }
            .clickable(
                role = Role.Button,
                onClick = {},
            ),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Image(
            painter = ColorPainter(imageColor),
            contentDescription = null,
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .clip(RoundedCornerShape(8.dp)),
        )
        Text(title, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun FixedLabeledAction(label: String, description: String) {
    Box(
        modifier = Modifier
            .size(width = 132.dp, height = 56.dp)
            .background(Color(0xFF283593), RoundedCornerShape(8.dp))
            .semantics { contentDescription = description }
            .clickable(
                role = Role.Button,
                onClick = {},
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = Color.White)
    }
}

@Preview(showBackground = true)
@Composable
fun BrokenAccessibilitySampleAppPreview() {
    ScannerTheme {
        BrokenAccessibilitySampleApp()
    }
}

private fun ComponentActivity.extractBrokenSampleNodes(): List<A11yNode> =
    runCatching {
        val hostView = (window.decorView as? ViewGroup)
            ?.findFirstAbstractComposeView()
            ?: return emptyList()
        val semanticsOwner = hostView.findSemanticsOwner() ?: return emptyList()
        val sampleRoot = semanticsOwner.unmergedRootSemanticsNode
            .findNodeByTestTag(BrokenSampleContentTag)
            ?: return emptyList()
        A11yNodeExtractor(Density(this)).extract(sampleRoot)
    }.getOrDefault(emptyList())

private const val BrokenSampleContentTag = "broken-sample-content"

private fun SemanticsNode.findNodeByTestTag(tag: String): SemanticsNode? {
    if (config.getOrNull(SemanticsProperties.TestTag) == tag) return this
    children.forEach { child ->
        child.findNodeByTestTag(tag)?.let { return it }
    }
    return null
}

private fun ViewGroup.findFirstAbstractComposeView(): AbstractComposeView? {
    for (i in 0 until childCount) {
        val child = getChildAt(i)
        if (child is AbstractComposeView) return child
        if (child is ViewGroup) {
            child.findFirstAbstractComposeView()?.let { return it }
        }
    }
    return null
}

private fun AbstractComposeView.findSemanticsOwner(): SemanticsOwner? {
    val composeOwnerView: View = getChildAt(0) ?: return null
    return runCatching {
        composeOwnerView.javaClass
            .getMethod("getSemanticsOwner")
            .invoke(composeOwnerView) as? SemanticsOwner
    }.getOrNull()
}
