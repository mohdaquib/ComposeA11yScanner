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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.composea11yscanner.core.model.A11yNode
import com.composea11yscanner.core.model.ScannerConfig
import com.composea11yscanner.rules.ScannerRules
import com.composea11yscanner.triggers.scanOnShake
import com.composea11yscanner.ui.A11yNodeExtractor
import com.composea11yscanner.ui.A11yScannerController
import com.composea11yscanner.ui.A11yScannerScaffold

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
        onViewFixed?.let {
            Button(onClick = it, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                Text("View Fixed Version")
            }
        }
    }
}

@Composable
private fun BrokenFeedScreen(onViewFixed: (() -> Unit)? = null) {
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
        onViewFixed?.let {
            Button(onClick = it, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                Text("View Fixed Version")
            }
        }
    }
}

@Composable
private fun BrokenFormScreen(onViewFixed: (() -> Unit)? = null) {
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
        onViewFixed?.let {
            Button(onClick = it, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                Text("View Fixed Version")
            }
        }
    }
}

@Composable
fun FixedLoginScreen(onViewBroken: (() -> Unit)? = null) {
    BrokenScreenCard(
        title = "Fixed Login",
        subtitle = "Described controls with touch targets at least 48dp",
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

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            FixedIconAction("?", "Open login help", Color(0xFF006D77))
            FixedIconAction("!", "Show login requirements", Color(0xFFE29578))
            FixedIconAction("x", "Dismiss login form", Color(0xFF8D99AE))
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(width = 56.dp, height = 48.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF2A9D8F))
                    .semantics { contentDescription = "Sign in" }
                    .clickable(
                        role = Role.Button,
                        onClick = {},
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text("Go", color = Color.White)
            }
            Box(
                modifier = Modifier
                    .size(width = 144.dp, height = 48.dp)
                    .semantics { contentDescription = "Reset password" }
                    .clickable(
                        role = Role.Button,
                        onClick = {},
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text("Forgot password", color = Color(0xFF1565C0))
            }
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
fun FixedFeedScreen(onViewBroken: (() -> Unit)? = null) {
    BrokenScreenCard(
        title = "Fixed Feed",
        subtitle = "Readable captions and unique descriptions",
    ) {
        FixedFeedItem(
            imageColor = Color(0xFF607D8B),
            title = "Morning trail update",
            description = "Open morning trail story",
        )
        FixedFeedItem(
            imageColor = Color(0xFF546E7A),
            title = "City lights tonight",
            description = "Open city lights story",
        )

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            FixedLabeledAction("Save", "Save feed item")
            FixedLabeledAction("Bookmark", "Bookmark feed item")
        }
        onViewBroken?.let {
            Button(onClick = it, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                Text("View Broken Version")
            }
        }
    }
}

@Composable
fun FixedFormScreen(onViewBroken: (() -> Unit)? = null) {
    BrokenScreenCard(
        title = "Fixed Form",
        subtitle = "Button roles and top-to-bottom focus order",
    ) {
        Text("Preference form", style = MaterialTheme.typography.titleMedium)
        Button(onClick = {}, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
            Text("Choose plan")
        }
        Button(onClick = {}, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
            Text("Reset fields")
        }
        Button(onClick = {}, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
            Text("Submit preferences")
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = {}, modifier = Modifier.weight(1f).heightIn(min = 48.dp)) {
                Text("Monthly")
            }
            Button(onClick = {}, modifier = Modifier.weight(1f).heightIn(min = 48.dp)) {
                Text("Yearly")
            }
        }
        onViewBroken?.let {
            Button(onClick = it, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
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
    MaterialTheme {
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
