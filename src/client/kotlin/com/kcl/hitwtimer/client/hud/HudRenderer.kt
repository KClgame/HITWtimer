package com.kcl.hitwtimer.client.hud

import com.kcl.hitwtimer.client.config.HitwConfig
import com.kcl.hitwtimer.client.config.HudPresence
import com.kcl.hitwtimer.client.timer.TimerManager

/**
 * Pure state + logic for HUD. Actual drawing is done from GuiHudRenderMixin.java.
 *
 * Coordinates are **GUI-scaled**. [hudScale] scales content from the stored top-left.
 * Background opacity defaults to 50% (see [HitwConfig.getHudBgOpacity]).
 */
object HudRenderer {
    @Volatile
    var editing = false
        private set

    private const val PREP_LABEL = "Preparation"
    private const val DIM_COLOR = 0xFF888888.toInt()

    fun toggleEdit() {
        if (editing) exitEdit(save = true) else enterEdit()
    }

    fun enterEdit() {
        editing = true
    }

    fun exitEdit(save: Boolean) {
        if (!editing) return
        editing = false
        if (save) {
            HitwConfig.save()
        }
    }

    fun isEditing(): Boolean = editing

    /** Scroll wheel without modifier: scale. */
    fun adjustScale(delta: Double) {
        if (!editing) return
        val cur = HitwConfig.getHudScale()
        val neu = (cur + (delta * 0.08).toFloat()).coerceIn(0.4f, 4.0f)
        HitwConfig.updateHud(HitwConfig.getHudX(), HitwConfig.getHudY(), neu)
    }

    /** Shift+scroll in edit mode: background opacity 0..1. */
    fun adjustOpacity(delta: Double) {
        if (!editing) return
        // ~4% per notch
        HitwConfig.adjustHudBgOpacity((delta * 0.04).toFloat())
        if (!HitwConfig.getRenderBackground() && HitwConfig.getHudBgOpacity() > 0f) {
            HitwConfig.setRenderBackground(true)
        }
    }

    data class HudSnapshot(
        val x: Int,
        val y: Int,
        val scale: Float,
        val editing: Boolean,
        val visible: Boolean,
        val lines: List<Pair<String, Int>>,
        val bgColor: Int,
        val hPadding: Int = HitwConfig.getHudHorizontalPadding(),
        val vPadding: Int = HitwConfig.getHudVerticalPadding()
    )

    fun getSnapshot(): HudSnapshot {
        val timers = TimerManager.getActiveTimers()
        val hasTrap = timers.isNotEmpty()
        val x = HitwConfig.getHudX()
        val y = HitwConfig.getHudY()
        val sc = HitwConfig.getHudScale()
        val bg = HitwConfig.getHudBackgroundArgb()

        val visible = HitwConfig.shouldDrawHud(hasTrap, editing)
        if (!visible) {
            return HudSnapshot(x, y, sc, editing, false, emptyList(), 0)
        }

        if (!hasTrap) {
            val mode = HitwConfig.getHudPresence()
            val placeholder = if (editing) {
                listOf(
                    "HITWtimer HUD" to 0xFFFFFF55.toInt(),
                    "  mode: ${mode.displayName()}" to 0xFFAAAAAA.toInt(),
                    "  opacity: ${"%.0f".format(HitwConfig.getHudBgOpacity() * 100)}%" to 0xFF888888.toInt(),
                    "  (no active trap)" to 0xFF888888.toInt()
                )
            } else {
                listOf("No current trap" to 0xFF888888.toInt())
            }
            return HudSnapshot(
                x, y, sc, editing, true, placeholder, bg,
                HitwConfig.getHudHorizontalPadding(), HitwConfig.getHudVerticalPadding()
            )
        }

        val now = System.currentTimeMillis()
        val lines = mutableListOf<Pair<String, Int>>()

        for ((ti, t) in timers.withIndex()) {
            if (ti > 0) {
                lines.add("" to 0x00000000)
            }

            val elapsed = t.getElapsed(now)
            val prepMax = if (t.preparationSeconds > 0) t.preparationSeconds else 0.0
            val events = t.trap.events.sortedBy { it.offsetSeconds }
            val totalDuration = prepMax + (events.maxOfOrNull { it.offsetSeconds } ?: 0.0)
            val totalRem = (totalDuration - elapsed).coerceAtLeast(0.0)
            val mainColor = t.trap.mainColor ?: 0xFFFFFFFF.toInt()
            val prepColor = t.trap.preparationColor ?: 0xFF55FF55.toInt()
            val inPrep = prepMax > 0 && elapsed < prepMax

            lines.add("${t.trap.name}  ${"%.1f".format(totalRem)}s" to mainColor)

            if (prepMax > 0) {
                if (inPrep) {
                    val prepRem = (prepMax - elapsed).coerceAtLeast(0.0)
                    lines.add("  $PREP_LABEL  ${"%.1f".format(prepRem)}s" to prepColor)
                    for (event in events) {
                        lines.add("  ${event.label}" to DIM_COLOR)
                    }
                } else {
                    lines.add("  $PREP_LABEL" to DIM_COLOR)
                    appendEventLines(lines, events, elapsed - prepMax, mainColor)
                }
            } else {
                appendEventLines(lines, events, elapsed, mainColor)
            }
        }

        return HudSnapshot(
            x, y, sc, editing, true, lines, bg,
            HitwConfig.getHudHorizontalPadding(), HitwConfig.getHudVerticalPadding()
        )
    }

    private fun appendEventLines(
        lines: MutableList<Pair<String, Int>>,
        events: List<com.kcl.hitwtimer.client.trap.TrapEvent>,
        eventElapsed: Double,
        mainColor: Int
    ) {
        for (event in events) {
            if (event.offsetSeconds > eventElapsed) {
                val eventRem = (event.offsetSeconds - eventElapsed).coerceAtLeast(0.0)
                val eventColor = event.color ?: mainColor
                lines.add("  ${event.label}  ${"%.1f".format(eventRem)}s" to eventColor)
                val remaining = events.dropWhile { it != event }.drop(1)
                for (next in remaining) {
                    lines.add("  ${next.label}" to DIM_COLOR)
                }
                return
            } else {
                lines.add("  ${event.label}" to DIM_COLOR)
            }
        }
    }
}
