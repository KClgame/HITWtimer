@file:Suppress("SpellCheckingInspection")

package com.kcl.hitwtimer.client.config

/**
 * Overall (mod-level) settings loaded from overallconfig.txt .
 *
 * HUD background default opacity is 50% (0.5).
 */
data class GlobalConfig(
    // === Overall / Mod level ===
    val enabled: Boolean = true,                    // master switch (file-level); DISABLED presence also gates runtime
    val enabledLists: List<String> = listOf("traplist1"),

    // Detection
    val detectSubtitle: Boolean = true,
    val detectChat: Boolean = true,

    // Global fallbacks for preparation
    val subtitlePreparationEnabled: Boolean = true,
    val preparationTime: Double = 3.0,
    val preparationColor: Int = 0xFF55FF55.toInt(),
    val mainColor: Int = 0xFFFFFFFF.toInt(),

    // HUD global
    val hudX: Int = 10,
    val hudY: Int = 10,
    val hudScale: Float = 1.0f,
    /** ALWAYS | ON_TRAP | DISABLED */
    val hudPresence: HudPresence = HudPresence.ON_TRAP,
    /** Draw semi-transparent panel background */
    val renderBackground: Boolean = true,
    /**
     * Background opacity 0.0 (invisible) .. 1.0 (opaque).
     * Default 0.5 (50%).
     */
    val hudBgOpacity: Float = DEFAULT_HUD_BG_OPACITY,
    val hudHorizontalPadding: Int = 8,
    val hudVerticalPadding: Int = 6,

    // Behavior
    val autoReloadKeywords: List<String> = listOf("hitw", "hole in the wall", "游戏开始"),

    val debug: Boolean = false
) {
    companion object {
        /** Default panel opacity: 50%. */
        const val DEFAULT_HUD_BG_OPACITY: Float = 0.5f
    }

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
        sb.appendLine("preparation_color=#${preparationColor.toUInt().toString(16).padStart(6, '0').uppercase()}")
        sb.appendLine("main_color=#${mainColor.toUInt().toString(16).padStart(6, '0').uppercase()}")
        sb.appendLine()
        sb.appendLine("# --- HUD ---")
        sb.appendLine("hud_x=$hudX")
        sb.appendLine("hud_y=$hudY")
        sb.appendLine("hud_scale=$hudScale")
        sb.appendLine("# ALWAYS = always show | ON_TRAP = only when trap active | DISABLED = no HUD/detect/sound")
        sb.appendLine("hud_mode=${hudPresence.name}")
        sb.appendLine("render_background=$renderBackground")
        sb.appendLine("# 0.0..1.0  default 0.5 (50%)")
        sb.appendLine("hud_bg_opacity=$hudBgOpacity")
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
