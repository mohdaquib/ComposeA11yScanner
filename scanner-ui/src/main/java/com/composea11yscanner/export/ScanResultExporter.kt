package com.composea11yscanner.export

import com.composea11yscanner.core.model.A11yNode
import com.composea11yscanner.core.model.A11ySeverity
import com.composea11yscanner.core.model.Color
import com.composea11yscanner.core.model.Rect
import com.composea11yscanner.core.model.ScanResult
import java.util.Locale

/** Exports scan results to text formats suitable for sharing or CI artifacts. */
object ScanResultExporter {

    /**
     * Converts [result] to a JSON string.
     *
     * @param result Result to export.
     * @return JSON representation of the result.
     */
    fun exportToJson(result: ScanResult): String = buildString {
        appendLine("{")
        appendJsonField("scanId", result.scanId, indent = 2, trailingComma = true)
        appendJsonField("timestamp", result.timestamp, indent = 2, trailingComma = true)
        appendJsonField("totalNodes", result.totalNodes, indent = 2, trailingComma = true)
        appendJsonField("passedRules", result.passedRules, indent = 2, trailingComma = true)
        appendJsonField("failedRules", result.failedRules, indent = 2, trailingComma = true)
        appendJsonField("errorCount", result.errorCount, indent = 2, trailingComma = true)
        appendJsonField("warningCount", result.warningCount, indent = 2, trailingComma = true)
        appendJsonField("infoCount", result.infoCount, indent = 2, trailingComma = true)
        appendJsonField("overallScore", result.overallScore, indent = 2, trailingComma = true)
        appendLine("  \"issues\": [")
        result.issues.forEachIndexed { index, issue ->
            appendLine("    {")
            appendJsonField("issueId", issue.issueId, indent = 6, trailingComma = true)
            appendJsonField("severity", issue.severity.label(), indent = 6, trailingComma = true)
            appendJsonField("ruleId", issue.ruleId, indent = 6, trailingComma = true)
            appendJsonField("ruleName", issue.ruleName, indent = 6, trailingComma = true)
            appendJsonField("message", issue.message, indent = 6, trailingComma = true)
            appendJsonField("howToFix", issue.howToFix, indent = 6, trailingComma = true)
            appendJsonNullableField("wcagReference", issue.wcagReference, indent = 6, trailingComma = true)
            appendLine("      \"affectedNode\": ${issue.affectedNode.toJson()}")
            append("    }")
            if (index != result.issues.lastIndex) append(",")
            appendLine()
        }
        appendLine("  ]")
        append("}")
    }

    /**
     * Converts [result] to a Markdown report.
     *
     * @param result Result to export.
     * @return Markdown table and summary text.
     */
    fun exportToMarkdown(result: ScanResult): String = buildString {
        appendLine("# Compose Accessibility Scan Report")
        appendLine()
        appendLine("- Scan ID: `${result.scanId}`")
        appendLine("- Timestamp: `${result.timestamp}`")
        appendLine("- Score: ${result.overallScore.formatPercent()}")
        appendLine("- Nodes scanned: ${result.totalNodes}")
        appendLine("- Rules passed: ${result.passedRules}")
        appendLine("- Rules failed: ${result.failedRules}")
        appendLine("- Issues: ${result.issues.size} (${result.errorCount} errors, ${result.warningCount} warnings, ${result.infoCount} info)")
        appendLine()

        if (result.issues.isEmpty()) {
            appendLine("No issues found.")
            return@buildString
        }

        appendLine("| Severity | Rule | Node | Message | How to fix | WCAG |")
        appendLine("| --- | --- | --- | --- | --- | --- |")
        result.issues.forEach { issue ->
            appendLine(
                listOf(
                    issue.severity.label(),
                    issue.ruleName,
                    issue.affectedNode.composableName,
                    issue.message,
                    issue.howToFix,
                    issue.wcagReference ?: "",
                ).joinToString(prefix = "| ", separator = " | ", postfix = " |") {
                    it.escapeMarkdownTableCell()
                },
            )
        }
    }

    private fun A11yNode.toJson(): String = buildString {
        append("{")
        appendJsonPair("nodeId", nodeId)
        append(", ")
        appendJsonPair("parentNodeId", parentNodeId)
        append(", ")
        appendJsonPair("composableName", composableName)
        append(", ")
        appendJsonPair("bounds", bounds)
        append(", ")
        appendJsonPair("contentDescription", contentDescription)
        append(", ")
        appendJsonPair("isTouchTarget", isTouchTarget)
        append(", ")
        appendJsonNullablePair("effectiveTouchBounds", effectiveTouchBounds)
        append(", ")
        appendJsonPair("textColor", textColor)
        append(", ")
        append("\"backgroundColors\": [")
        append(backgroundColors.joinToString { it.toJson() })
        append("], ")
        appendJsonPair("isFocusable", isFocusable)
        append(", ")
        appendJsonPair("isMergedDescendant", isMergedDescendant)
        append(", ")
        appendJsonPair("depth", depth)
        append(", ")
        appendJsonPair("role", role?.name)
        append("}")
    }

    private fun StringBuilder.appendJsonField(
        name: String,
        value: String,
        indent: Int,
        trailingComma: Boolean,
    ) {
        append(" ".repeat(indent))
        appendJsonPair(name, value)
        if (trailingComma) append(",")
        appendLine()
    }

    private fun StringBuilder.appendJsonNullableField(
        name: String,
        value: String?,
        indent: Int,
        trailingComma: Boolean,
    ) {
        append(" ".repeat(indent))
        appendJsonPair(name, value)
        if (trailingComma) append(",")
        appendLine()
    }

    private fun StringBuilder.appendJsonField(
        name: String,
        value: Number,
        indent: Int,
        trailingComma: Boolean,
    ) {
        append(" ".repeat(indent))
        appendJsonPair(name, value)
        if (trailingComma) append(",")
        appendLine()
    }

    private fun StringBuilder.appendJsonPair(name: String, value: String?) {
        append("\"")
        append(name.escapeJson())
        append("\": ")
        append(value?.let { "\"${it.escapeJson()}\"" } ?: "null")
    }

    private fun StringBuilder.appendJsonPair(name: String, value: Number) {
        append("\"")
        append(name.escapeJson())
        append("\": ")
        append(value)
    }

    private fun StringBuilder.appendJsonPair(name: String, value: Boolean) {
        append("\"")
        append(name.escapeJson())
        append("\": ")
        append(value)
    }

    private fun StringBuilder.appendJsonPair(name: String, value: Rect) {
        append("\"")
        append(name.escapeJson())
        append("\": ")
        append(value.toJson())
    }

    private fun StringBuilder.appendJsonNullablePair(name: String, value: Rect?) {
        append("\"")
        append(name.escapeJson())
        append("\": ")
        append(value?.toJson() ?: "null")
    }

    private fun StringBuilder.appendJsonPair(name: String, value: Color?) {
        append("\"")
        append(name.escapeJson())
        append("\": ")
        append(value?.toJson() ?: "null")
    }

    private fun Rect.toJson(): String =
        """{"left": $left, "top": $top, "right": $right, "bottom": $bottom, "width": $width, "height": $height}"""

    private fun Color.toJson(): String =
        """{"value": $value}"""

    private fun Float.formatPercent(): String =
        String.format(Locale.US, "%.1f%%", this)

    private fun A11ySeverity.label(): String = when (this) {
        A11ySeverity.Error -> "Error"
        A11ySeverity.Warning -> "Warning"
        A11ySeverity.Info -> "Info"
    }

    private fun String.escapeJson(): String = buildString {
        this@escapeJson.forEach { char ->
            when (char) {
                '\\' -> append("\\\\")
                '"' -> append("\\\"")
                '\b' -> append("\\b")
                '\u000C' -> append("\\f")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> {
                    if (char.code < 0x20) {
                        append("\\u")
                        append(char.code.toString(16).padStart(4, '0'))
                    } else {
                        append(char)
                    }
                }
            }
        }
    }

    private fun String.escapeMarkdownTableCell(): String =
        replace("\\", "\\\\")
            .replace("|", "\\|")
            .replace("\r", " ")
            .replace("\n", " ")
            .trim()
}
