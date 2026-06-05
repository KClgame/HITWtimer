package com.kcl.hitwtimer.client.config

import com.kcl.hitwtimer.client.trap.TrapDefinition

/**
 * Configuration for a trap list (from header of traplist*.txt before first name=).
 * Contains list status and default values for traps in this list.
 * If a trap doesn't define a value, it falls back to the list's default (if set), then global/overall.
 * description: optional description or category name for this list (for display/classification). The list name for commands (/hitwtimer enable <name>) and internal keys is the filename-derived listName (e.g. "traplist1" from traplist1.txt).
 *
 * Detection defaults (matchMode / caseSensitive) apply to traps that don't specify their own.
 * pattern default is rarely set at list level (each trap usually has unique trigger text).
 */
data class TrapListConfig(
    val listName: String,

    // === List status ===
    val enabled: Boolean = true,           // whether this list is initially enabled (can be toggled at runtime via commands)
    val description: String = "",          // optional description / category name for the list (for display or classification; the list identifier used in /hitwtimer enable <name> is the filename-based listName)

    // === Defaults for traps in this list (if trap doesn't specify) ===
    // These are used when the individual trapconfig doesn't define them.
    val defaultSource: TrapDefinition.Source = TrapDefinition.Source.SUBTITLE,
    val defaultEnabled: Boolean = true,

    // subtitle_preparation default for traps in list.
    // If trap sets it (even to false), trap wins strictly.
    // If null here, falls back to overall.
    // NOTE: This ONLY affects subtitle detections. Chat detections never insert preparation.
    val defaultSubtitlePreparation: Boolean? = null,

    // Preparation related defaults for this list's traps.
    // Used when creating prep phase or for timing.
    val defaultPreparationTime: Double? = null,
    val defaultPreparationColor: Int? = null,
    val defaultMainColor: Int? = null,

    // Sound defaults
    val defaultStartSound: String? = "block.note_block.pling",
    val defaultEndSound: String? = "block.note_block.bell",

    // === Detection defaults (for match pattern / mode) ===
    val defaultPattern: String? = null,
    val defaultMatchMode: TrapDefinition.MatchMode = TrapDefinition.MatchMode.CONTAINS,
    val defaultCaseSensitive: Boolean = false
) {
    /**
     * Resolve effective subtitlePreparation for a trap in this list.
     * Trap's value (if not null) wins strictly.
     * This only decides prep for subtitle detections; chat detections always get 0 prep time.
     */
    fun resolveSubtitlePreparation(trapSubtitlePreparation: Boolean?): Boolean? {
        return trapSubtitlePreparation ?: defaultSubtitlePreparation
    }

    /**
     * Resolve effective prep time: trap would override but since not in trap yet, use list or global.
     * For now, caller can pass.
     */
    fun resolvePreparationTime(globalTime: Double): Double {
        return defaultPreparationTime ?: globalTime
    }

    fun resolvePreparationColor(globalColor: Int): Int {
        return defaultPreparationColor ?: globalColor
    }

    fun resolveMainColor(globalColor: Int): Int {
        return defaultMainColor ?: globalColor
    }

    fun resolveStartSound(): String? = defaultStartSound
    fun resolveEndSound(): String? = defaultEndSound
    fun resolveSource(): TrapDefinition.Source = defaultSource
    fun resolveEnabled(): Boolean = defaultEnabled

    fun resolveMatchMode(): TrapDefinition.MatchMode = defaultMatchMode
    fun resolveCaseSensitive(): Boolean = defaultCaseSensitive
    fun resolvePattern(): String? = defaultPattern?.ifBlank { null }
}
