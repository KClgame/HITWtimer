package com.kcl.hitwtimer.client.trap

/**
 * A single stage/event in a trap sequence.
 * offsetSeconds is relative to trap trigger (after optional preparation).
 */
data class TrapEvent(
    val offsetSeconds: Double,
    val label: String = "",
    val color: Int? = null,  // optional; falls back to trap's mainColor, then list/global main
    val sound: String? = null
)
