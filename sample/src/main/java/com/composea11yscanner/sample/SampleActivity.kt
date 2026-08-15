package com.composea11yscanner.sample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.composea11yscanner.core.model.ScannerConfig
import com.composea11yscanner.rules.ScannerRules
import com.composea11yscanner.sample.ui.theme.ScannerTheme
import com.composea11yscanner.triggers.scanOnShake
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
    val screens = remember {
        listOf(
            SampleScreen("Login", "Credentials and compact actions", Icons.Filled.Person),
            SampleScreen("Feed", "Stories, captions, and repeated labels", Icons.AutoMirrored.Filled.List),
            SampleScreen("Form", "Form controls and focus order", Icons.Filled.Edit),
        )
    }
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
        summaryBarTopOffset = 64.dp,
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                SampleTopBar(
                    onClear = { scannerController.clearState() },
                    onScan = { startSampleScan() },
                )
            },
            bottomBar = {
                SampleBottomBar(
                    screens = screens,
                    selectedScreen = selectedScreen,
                    onScreenSelected = {
                        selectedScreen = it
                        viewingFixed = false
                    },
                )
            },
        ) { innerPadding ->
            SampleContent(
                modifier = Modifier
                    .padding(innerPadding)
                    // Keep this space stable so showing the scan summary never moves content
                    // after the semantics bounds used by issue overlays have been captured.
                    .padding(top = 56.dp)
                    .fillMaxSize(),
                screens = screens,
                selectedScreen = selectedScreen,
                viewingFixed = viewingFixed,
                onViewingFixedChange = { viewingFixed = it },
                scrollModifier = Modifier.verticalScroll(scrollState),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SampleTopBar(
    onClear: () -> Unit,
    onScan: () -> Unit,
) {
    CenterAlignedTopAppBar(
        title = {
            Text(
                text = "Compose A11y Scanner",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
        },
        actions = {
            IconButton(onClick = onClear) {
                Icon(
                    imageVector = Icons.Filled.Clear,
                    contentDescription = "Clear scan results",
                )
            }
            IconButton(onClick = onScan) {
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
}

@Composable
private fun SampleBottomBar(
    screens: List<SampleScreen>,
    selectedScreen: Int,
    onScreenSelected: (Int) -> Unit,
) {
    NavigationBar {
        screens.forEachIndexed { index, screen ->
            NavigationBarItem(
                selected = selectedScreen == index,
                onClick = { onScreenSelected(index) },
                icon = {
                    Icon(
                        imageVector = screen.icon,
                        contentDescription = null,
                    )
                },
                label = { Text(screen.label) },
            )
        }
    }
}

@Composable
private fun SampleContent(
    modifier: Modifier = Modifier,
    screens: List<SampleScreen>,
    selectedScreen: Int,
    viewingFixed: Boolean,
    onViewingFixedChange: (Boolean) -> Unit,
    scrollModifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
            .then(scrollModifier)
            .semantics { testTag = SampleViewportTag }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SampleModeCard(
            screen = screens[selectedScreen],
            viewingFixed = viewingFixed,
            onViewingFixedChange = onViewingFixedChange,
        )
        SampleScreenHost(
            selectedScreen = selectedScreen,
            viewingFixed = viewingFixed,
        )
    }
}

@Composable
private fun SampleModeCard(
    screen: SampleScreen,
    viewingFixed: Boolean,
    onViewingFixedChange: (Boolean) -> Unit,
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
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = screen.label,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = screen.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FilterChip(
                    selected = !viewingFixed,
                    onClick = { onViewingFixedChange(false) },
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
                    onClick = { onViewingFixedChange(true) },
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
}

@Composable
private fun SampleScreenHost(
    selectedScreen: Int,
    viewingFixed: Boolean,
) {
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
        ) {
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

@Preview(showBackground = true)
@Composable
fun BrokenAccessibilitySampleAppPreview() {
    ScannerTheme {
        BrokenAccessibilitySampleApp()
    }
}

private data class SampleScreen(
    val label: String,
    val subtitle: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
)
