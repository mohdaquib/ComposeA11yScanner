package com.composea11yscanner.rules

import com.composea11yscanner.core.model.A11yNode
import com.composea11yscanner.core.model.A11yRole
import com.composea11yscanner.core.model.Color
import com.composea11yscanner.core.model.Rect
import java.util.concurrent.atomic.AtomicInteger

private val nodeIdSeq = AtomicInteger(0)

fun createNode(
    composableName: String = "Box",
    bounds: Rect = Rect(0, 0, 100, 100),
    contentDescription: String? = null,
    isTouchTarget: Boolean = false,
    textColor: Color? = null,
    backgroundColors: List<Color> = emptyList(),
    isFocusable: Boolean = false,
    isMergedDescendant: Boolean = false,
    depth: Int = 0,
    role: A11yRole? = null,
    nodeId: String = "node-${nodeIdSeq.incrementAndGet()}",
): A11yNode = A11yNode(
    nodeId = nodeId,
    composableName = composableName,
    bounds = bounds,
    contentDescription = contentDescription,
    isTouchTarget = isTouchTarget,
    textColor = textColor,
    backgroundColors = backgroundColors,
    isFocusable = isFocusable,
    isMergedDescendant = isMergedDescendant,
    depth = depth,
    role = role,
)
