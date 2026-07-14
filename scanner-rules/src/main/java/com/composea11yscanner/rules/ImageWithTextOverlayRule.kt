package com.composea11yscanner.rules

import com.composea11yscanner.core.model.A11yIssue
import com.composea11yscanner.core.model.A11yNode
import com.composea11yscanner.core.model.A11ySeverity
import com.composea11yscanner.core.rule.BaseScanRule

/**
 * Flags text nodes that significantly overlap image nodes.
 *
 * @param overlapThreshold Fraction of the text node's area that must intersect an image
 *   node before the pair is flagged (default 0.5 = 50%).
 */
class ImageWithTextOverlayRule(
    private val overlapThreshold: Float = 0.5f,
) : BaseScanRule() {

    /** Stable id for the image text overlay rule. */
    override val ruleId = "image-text-overlay"

    /** Human-readable rule name. */
    override val ruleName = "Image With Text Overlay"

    /** Severity assigned to possible image/text contrast risk. */
    override val severity = A11ySeverity.Warning

    /** WCAG criterion associated with contrast. */
    override val wcagReference = "WCAG 1.4.3 Contrast Minimum (Level AA)"

    /** Evaluates all text and image nodes together to find overlaps. */
    override fun evaluateAll(nodes: List<A11yNode>): List<A11yIssue> {
        val textNodes = nodes.filter { it.composableName.contains("Text", ignoreCase = true) }
        val imageNodes = nodes.filter { it.composableName.contains("Image", ignoreCase = true) }

        return textNodes.mapNotNull { textNode ->
            val overlaps = imageNodes.any { imageNode ->
                overlapRatio(textNode.bounds, imageNode.bounds) > overlapThreshold
            }
            if (!overlaps) return@mapNotNull null

            issue(
                node = textNode,
                message = "Text rendered over image may fail contrast requirements on different images.",
                howToFix = "Add a semi-transparent scrim or solid background behind the text " +
                    "(e.g. Modifier.background(Color.Black.copy(alpha = 0.5f))), or verify " +
                    "the image always provides sufficient contrast (4.5:1 normal, 3:1 large text).",
            )
        }
    }

    private fun overlapRatio(text: Rect, image: Rect): Float {
        val intLeft = maxOf(text.left, image.left)
        val intTop = maxOf(text.top, image.top)
        val intRight = minOf(text.right, image.right)
        val intBottom = minOf(text.bottom, image.bottom)

        if (intRight <= intLeft || intBottom <= intTop) return 0f

        val textArea = text.width * text.height
        if (textArea == 0) return 0f

        val intersectionArea = (intRight - intLeft) * (intBottom - intTop)
        return intersectionArea.toFloat() / textArea
    }
}

private typealias Rect = com.composea11yscanner.core.model.Rect
