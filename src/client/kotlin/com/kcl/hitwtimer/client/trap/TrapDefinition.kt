package com.kcl.hitwtimer.client.trap

/**
 * Definition of one trap loaded from a traplist txt.
 *
 * Required:
 * - name (unique identifier)
 * - pattern (detection keyword(s); supports "a|b|c" for alternatives)
 * - at least one event
 *
 * Optional (if not provided, fall back to traplistconfig defaults for this list, then overall/global):
 * - enabled
 * - source
 * - subtitlePreparation (false is strict no-prep)
 * - startSound, endSound
 * - mainColor (default color for events without color)
 * - preparationTime, preparationColor (per-trap prep override)
 * - matchMode (CONTAINS/EQUALS/STARTS_WITH/REGEX)
 * - caseSensitive (default false = case-insensitive match)
 *
 * listName: the traplist this belongs to, for resolving list defaults.
 *
 * NOTE: Do NOT include a "Preparation" stage in the events list.
 * Preparation phase (timing, "准备" label, start sound delay) is automatically
 * prepended ONLY for subtitle detections (when subtitlePreparation setting resolves to true).
 * Chat detections always have preparationSeconds=0.
 */
data class TrapDefinition(
    val name: String,
    val enabled: Boolean = true,
    val source: Source = Source.SUBTITLE,
    val subtitlePreparation: Boolean? = null,
    val startSound: String? = "block.note_block.pling",
    val endSound: String? = "block.note_block.bell",
    val events: List<TrapEvent> = emptyList(),
    /** The traplist this trap belongs to. Used for list-level defaults fallback. */
    val listName: String = "default",
    // Trap-level optional configs (if null, fall back to list defaults from traplistconfig)
    val mainColor: Int? = null,
    val preparationTime: Double? = null,
    val preparationColor: Int? = null,

    // === Detection pattern & match mode (NEW) ===
    /** The string(s) to look for in subtitle or chat text. Required for every trap (use name as fallback only if absolutely necessary, but always specify in config). */
    val pattern: String,
    val matchMode: MatchMode = MatchMode.CONTAINS,
    val caseSensitive: Boolean = false
) {
    enum class Source {
        SUBTITLE, CHAT, BOTH
    }

    enum class MatchMode {
        /** Contains substring (most common, default). Supports pattern="火墙|fire wall" for OR. */
        CONTAINS,
        /** Exact full string match. */
        EQUALS,
        /** Starts with prefix. */
        STARTS_WITH,
        /** Regular expression (use pattern="火.*墙|arrow.*rain", case flag applies). */
        REGEX
    }

    /**
     * Returns true if the given game text (subtitle or chat .string) triggers this trap.
     * Uses `pattern` (or name as fallback). Honors matchMode and caseSensitive.
     * | in pattern acts as OR for non-REGEX modes.
     */
    fun matches(text: String): Boolean {
        if (text.isBlank()) return false
        val rawPat = (pattern?.takeIf { it.isNotBlank() } ?: name).trim()
        if (rawPat.isEmpty()) return false

        val isRegex = matchMode == MatchMode.REGEX
        val pats: List<String> = if (!isRegex && rawPat.contains('|')) {
            rawPat.split('|').map { it.trim() }.filter { it.isNotEmpty() }
        } else {
            listOf(rawPat)
        }

        for (p0 in pats) {
            val matched = when (matchMode) {
                MatchMode.CONTAINS -> {
                    val t = if (caseSensitive) text else text.lowercase()
                    val p = if (caseSensitive) p0 else p0.lowercase()
                    t.contains(p)
                }
                MatchMode.EQUALS -> {
                    val t = if (caseSensitive) text else text.lowercase()
                    val p = if (caseSensitive) p0 else p0.lowercase()
                    t == p
                }
                MatchMode.STARTS_WITH -> {
                    val t = if (caseSensitive) text else text.lowercase()
                    val p = if (caseSensitive) p0 else p0.lowercase()
                    t.startsWith(p)
                }
                MatchMode.REGEX -> {
                    try {
                        val re = if (caseSensitive) p0.toRegex() else p0.toRegex(setOf(RegexOption.IGNORE_CASE))
                        re.containsMatchIn(text)
                    } catch (_: Exception) {
                        false
                    }
                }
            }
            if (matched) return true
        }
        return false
    }
}
