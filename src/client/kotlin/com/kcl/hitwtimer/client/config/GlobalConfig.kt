@file:Suppress("SpellCheckingInspection")

package com.kcl.hitwtimer.client.config

/**
 * Overall (mod-level) settings loaded from overallconfig.txt .
 * These are the top level settings for the entire HITWtimer mod.
 * Per-traplist and per-trap settings will be in their respective config sections/files.
 */
data class GlobalConfig(
    // === Overall / Mod level ===
    val enabled: Boolean = true,                    // master switch for the whole mod
    val enabledLists: List<String> = listOf("traplist1"),

    // Detection
    val detectSubtitle: Boolean = true,
    val detectChat: Boolean = true,                 // if we support chat detection separately

    // Global fallbacks for preparation (can be overridden by traplistconfig or trapconfig)
    val subtitlePreparationEnabled: Boolean = true,
    val preparationTime: Double = 3.0,
    val preparationColor: Int = 0x55FF55,
    val mainColor: Int = 0xFFFFFF,

    // HUD global
    val hudX: Int = 10,
    val hudY: Int = 10,
    val hudScale: Float = 1.0f,
    val renderBackground: Boolean = true,
    val hudHorizontalPadding: Int = 8,
    val hudVerticalPadding: Int = 6,

    // Behavior
    val autoReloadKeywords: List<String> = listOf("hitw", "hole in the wall", "游戏开始"),

    // Misc
    /**
     * debug mode:
     * - true: print detailed match logs to console, and show "trap started" messages in chat with more info.
     * - false (default): minimal output, keeps chat clean.
     * Useful for troubleshooting trap detection.
     */
    val debug: Boolean = false
) {
    fun toSaveString(): String {
        val sb = StringBuilder()
        sb.appendLine("# HITWtimer Overall Config (overallconfig.txt) - Mod level settings")
        sb.appendLine("# This is the top-level config for the entire mod.")
        sb.appendLine("# List-specific defaults go in the header of each traplist*.txt")
        sb.appendLine("# Per-trap settings go under each name= in traplist*.txt")
        sb.appendLine("# Runtime list control via /hitwtimer enable/disable <list>")
        sb.appendLine()
        sb.appendLine("# --- Master ---")
        sb.appendLine("enabled=$enabled")
        sb.appendLine()
        sb.appendLine("# --- Lists ---")
        sb.appendLine("enabled_lists=${enabledLists.joinToString(",")}")
        sb.appendLine()
        sb.appendLine("# --- Detection ---")
        sb.appendLine("detect_subtitle=$detectSubtitle")
        sb.appendLine("detect_chat=$detectChat")
        sb.appendLine()
        sb.appendLine("# --- Preparation defaults (fallbacks) ---")
        sb.appendLine("subtitle_preparation=$subtitlePreparationEnabled")
        sb.appendLine("preparation_time=$preparationTime")
        sb.appendLine("preparation_color=#${preparationColor.toUInt().toString(16).padStart(6,'0').uppercase()}")
        sb.appendLine("main_color=#${mainColor.toUInt().toString(16).padStart(6,'0').uppercase()}")
        sb.appendLine()
        sb.appendLine("# --- HUD ---")
        sb.appendLine("hud_x=$hudX")
        sb.appendLine("hud_y=$hudY")
        sb.appendLine("hud_scale=$hudScale")
        sb.appendLine("render_background=$renderBackground")
        sb.appendLine("hud_horizontal_padding=$hudHorizontalPadding")
        sb.appendLine("hud_vertical_padding=$hudVerticalPadding")
        sb.appendLine()
        sb.appendLine("# --- Behavior ---")
        sb.appendLine("auto_reload_keywords=${autoReloadKeywords.joinToString(",")}")
        sb.appendLine()
        sb.appendLine("# --- Misc ---")
        sb.appendLine("debug=$debug")
        return sb.toString()
    }
}
