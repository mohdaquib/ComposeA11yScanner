package com.composea11yscanner.sample

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun BrokenFeedScreen(onViewFixed: (() -> Unit)? = null) {
    BrokenScreenCard(
        title = "Market Pulse",
        subtitle = "Live financial news and price movers",
    ) {
        MarketFilters()

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
fun FixedFeedScreen(onViewBroken: (() -> Unit)? = null) {
    BrokenScreenCard(
        title = "Market Pulse",
        subtitle = "Accessible financial news and price movers",
    ) {
        MarketFilters()

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
private fun MarketFilters() {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        MarketFilterChip("All", selected = true)
        MarketFilterChip("Stocks", selected = false)
        MarketFilterChip("Crypto", selected = false)
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
        ChartOverlay(chartStart = chartStart, chartEnd = chartEnd, isFixed = false)
        FinancialNewsContent(
            monogram = monogram,
            logoColor = logoColor,
            headline = headline,
            priceChange = priceChange,
            isPositive = isPositive,
            timestamp = timestamp,
            useScrim = false,
        )
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
            .drawBehind { drawChart(chartStart, chartEnd, isFixed = true) }
            .semantics { contentDescription = description }
            .clickable(
                role = Role.Button,
                onClick = {},
            ),
    ) {
        FinancialNewsContent(
            monogram = monogram,
            logoColor = logoColor,
            headline = headline,
            priceChange = priceChange,
            isPositive = isPositive,
            timestamp = timestamp,
            useScrim = true,
        )
    }
}

@Composable
private fun ChartOverlay(chartStart: Color, chartEnd: Color, isFixed: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .drawBehind { drawChart(chartStart, chartEnd, isFixed) },
    )
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawChart(
    chartStart: Color,
    chartEnd: Color,
    isFixed: Boolean,
) {
    drawRect(
        brush = Brush.linearGradient(
            colors = listOf(chartStart.copy(alpha = if (isFixed) 0.45f else 0.35f), chartEnd),
            start = Offset.Zero,
            end = Offset(size.width, size.height),
        ),
    )
    repeat(5) { index ->
        val y = size.height * (0.24f + index * 0.13f)
        drawLine(
            color = Color.White.copy(alpha = if (isFixed) 0.08f else 0.09f),
            start = Offset(0f, y + index * 12f),
            end = Offset(size.width, y - 48f),
            strokeWidth = 2.5f,
        )
    }
    drawLine(
        color = Color(0xFF00D4AA).copy(alpha = if (isFixed) 0.28f else 0.34f),
        start = Offset(0f, size.height * 0.78f),
        end = Offset(size.width, size.height * 0.34f),
        strokeWidth = 5f,
    )
}

@Composable
private fun FinancialNewsContent(
    monogram: String,
    logoColor: Color,
    headline: String,
    priceChange: String,
    isPositive: Boolean,
    timestamp: String,
    useScrim: Boolean,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (useScrim) Modifier.background(Color.Black.copy(alpha = 0.54f)) else Modifier)
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
                    color = if (useScrim) Color.White else Color(0xFF8B949E),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = timestamp,
                    color = if (useScrim) Color(0xFFD0D7DE) else Color(0xFF6E7681),
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
