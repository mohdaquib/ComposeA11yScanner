package com.composea11yscanner.core.model

/**
 * Packed ARGB color stored as a 64-bit value.
 *
 * The bit layout matches Compose's Color.value (each channel 16 bits: alpha, red, green, blue).
 * Conversion from a Compose Color in :scanner-ui is zero-cost:
 *   Color(composeColor.value.toLong())
 */
@JvmInline
value class Color(
    /** Packed color value compatible with Compose color storage. */
    val value: Long,
) {
    /** Returns the packed 8-bit Android ARGB representation for sampled sRGB colors. */
    fun toArgb(): Int = (value ushr 32).toInt()

    companion object {
        /** Sentinel value used when a color could not be sampled. */
        val Unspecified = Color(0L)

        /** Creates a sampled sRGB color from an Android ARGB integer. */
        fun fromArgb(argb: Int): Color = Color((argb.toLong() and 0xFFFFFFFFL) shl 32)
    }
}
