package com.composea11yscanner.core.model

/**
 * Mirrors [androidx.compose.ui.semantics.Role] without a Compose dependency.
 * Conversion happens in :scanner-ui from Compose semantics roles.
 */
enum class A11yRole {
    /** Activates an action. */
    Button,

    /** Two-state checkable control. */
    Checkbox,

    /** List-style selection control. */
    DropdownList,

    /** Image or image-like content. */
    Image,

    /** Mutually exclusive selectable option. */
    RadioButton,

    /** Two-state switch control. */
    Switch,

    /** Tab item in a tab set. */
    Tab,

    /** Editable text field. */
    TextField,
}
