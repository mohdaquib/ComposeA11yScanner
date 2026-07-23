package com.composea11yscanner.rules

import com.composea11yscanner.core.model.ScannerConfig
import com.composea11yscanner.core.rule.A11yRule

/** Factory and metadata for the bundled scanner rule set. */
object ScannerRules {
    /** Version of the bundled rule set. */
    const val VERSION = "0.1.0"

    /** Returns every built-in rule id understood by [buildRules]. */
    fun allRuleIds(): List<String> = listOf(
        "touch-target-overlap",
        "missing-content-description",
        "duplicate-content-description",
        "focus-order",
        "text-scaling",
        "image-text-overlay",
        "clickable-role",
    )

    /**
     * Builds the list of rules enabled by [config], wiring density-dependent rules with [screenDensity].
     *
     * @param config Scanner configuration containing enabled rule ids and thresholds.
     * @param screenDensity Display density used by pixel/dp conversion rules. Must not be zero.
     * @return Rule instances that should run for the provided configuration.
     */
    fun buildRules(config: ScannerConfig, screenDensity: Float): List<A11yRule> = buildList {
        if ("touch-target-overlap" in config.enabledRules)
            add(TouchTargetOverlapRule())
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
