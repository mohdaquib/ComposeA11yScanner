package com.composea11yscanner.rules

import com.composea11yscanner.core.model.ScannerConfig
import com.composea11yscanner.core.rule.A11yRule

object ScannerRules {
    const val VERSION = "0.1.0"

    fun allRuleIds(): List<String> = listOf(
        "touch-target-size",
        "missing-content-description",
        "duplicate-content-description",
        "focus-order",
        "text-scaling",
        "image-text-overlay",
        "clickable-role",
    )

    /**
     * Builds the list of rules enabled by [config], wiring density-dependent rules with
     * [screenDensity] (from DisplayMetrics.density — must not be zero).
     */
    fun buildRules(config: ScannerConfig, screenDensity: Float): List<A11yRule> = buildList {
        if ("touch-target-size" in config.enabledRules)
            add(TouchTargetRule(config.minTouchTargetDp))
        if ("missing-content-description" in config.enabledRules)
            add(MissingContentDescriptionRule())
        if ("duplicate-content-description" in config.enabledRules)
            add(DuplicateContentDescriptionRule())
        if ("focus-order" in config.enabledRules)
            add(FocusOrderRule(screenDensity))
        if ("text-scaling" in config.enabledRules)
            add(TextScalingRule(screenDensity))
        if ("image-text-overlay" in config.enabledRules)
            add(ImageWithTextOverlayRule())
        if ("clickable-role" in config.enabledRules)
            add(ClickableRoleRule())
    }
}
