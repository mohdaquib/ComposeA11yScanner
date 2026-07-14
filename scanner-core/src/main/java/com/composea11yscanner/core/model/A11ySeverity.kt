package com.composea11yscanner.core.model

/** Priority level for an accessibility issue. */
sealed interface A11ySeverity : Comparable<A11ySeverity> {
    /** Numeric order used for sorting, where lower values are more severe. */
    val sortOrder: Int

    /** Sorts severities by [sortOrder]. */
    override fun compareTo(other: A11ySeverity): Int = sortOrder.compareTo(other.sortOrder)

    /** Must fix — blocks accessibility. */
    data object Error : A11ySeverity { override val sortOrder = 0 }

    /** Should fix — degrades experience. */
    data object Warning : A11ySeverity { override val sortOrder = 1 }

    /** Consider fixing — best practice violation. */
    data object Info : A11ySeverity { override val sortOrder = 2 }
}
