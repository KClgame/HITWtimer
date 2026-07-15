package com.kcl.hitwtimer.client.config

/**
 * How / whether the HUD (and whole mod activity) is active.
 *
 * - [ALWAYS]: HUD stays on screen (placeholder when no trap).
 * - [ON_TRAP]: HUD only while a trap timer is running (or while editing).
 * - [DISABLED]: no HUD, no subtitle/chat detection, no sounds / timers.
 */
enum class HudPresence {
    ALWAYS,
    ON_TRAP,
    DISABLED;

    fun next(): HudPresence = when (this) {
        ALWAYS -> ON_TRAP
        ON_TRAP -> DISABLED
        DISABLED -> ALWAYS
    }

    fun displayName(): String = when (this) {
        ALWAYS -> "ALWAYS"
        ON_TRAP -> "ON_TRAP"
        DISABLED -> "DISABLED"
    }

    fun chatLabel(): String = when (this) {
        ALWAYS -> "ALWAYS (HUD always on)"
        ON_TRAP -> "ON_TRAP (only when trap active)"
        DISABLED -> "DISABLED (no detect / no sound / no HUD)"
    }

    companion object {
        fun parse(raw: String?): HudPresence {
            if (raw.isNullOrBlank()) return ON_TRAP
            return when (raw.trim().lowercase().replace('-', '_')) {
                "always", "always_on", "permanent", "on" -> ALWAYS
                "on_trap", "ontrap", "trap", "detect", "auto" -> ON_TRAP
                "disabled", "disable", "off", "never", "none", "hidden" -> DISABLED
                else -> ON_TRAP
            }
        }
    }
}
