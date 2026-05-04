package com.composea11yscanner.core.model

data class A11yNode(
    val nodeId: String,
    val composableName: String,
    val bounds: Rect,
    val contentDescription: String?,
    val isTouchTarget: Boolean,
    val touchTargetSize: DpSize,
    val textColor: Color?,
    val backgroundColors: List<Color>,
    val isFocusable: Boolean,
    val isMergedDescendant: Boolean,
    val depth: Int,
)
