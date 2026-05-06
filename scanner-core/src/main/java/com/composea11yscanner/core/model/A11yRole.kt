package com.composea11yscanner.core.model

/**
 * Mirrors [androidx.compose.ui.semantics.Role] without a Compose dependency.
 * Conversion in :scanner-ui: A11yRole.from(semanticsNode.config[SemanticsProperties.Role])
 */
enum class A11yRole {
    Button,
    Checkbox,
    DropdownList,
    Image,
    RadioButton,
    Switch,
    Tab,
}
