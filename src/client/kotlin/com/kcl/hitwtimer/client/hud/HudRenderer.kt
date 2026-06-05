package com.kcl.hitwtimer.client.hud

import com.kcl.hitwtimer.client.config.HitwConfig
import com.kcl.hitwtimer.client.timer.TimerManager

/**
 * Pure state + logic for HUD. Actual drawing is done from GuiMixin.java to avoid
 * client-only GuiGraphics resolution issues in this setup.
 */
object HudRenderer {
    @Volatile
    var editing = false
    private var dragOffsetX = 0
    private var dragOffsetY = 0

    fun toggleEdit() {
        editing = !editing
        if (!editing) {
            HitwConfig.save()
        }
    }

    fun isEditing(): Boolean = editing

    fun startDrag(mouseX: Double, mouseY: Double) {
        if (!editing) return
        val x = HitwConfig.getHudX()
        val y = HitwConfig.getHudY()
        dragOffsetX = (mouseX - x).toInt()
        dragOffsetY = (mouseY - y).toInt()
    }

    fun updateDrag(mouseX: Double, mouseY: Double) {
        if (!editing) return
        val newX = (mouseX - dragOffsetX).toInt().coerceIn(0, 2000)
        val newY = (mouseY - dragOffsetY).toInt().coerceIn(0, 1200)
        HitwConfig.updateHud(newX, newY, HitwConfig.getHudScale())
    }

    fun adjustScale(delta: Double) {
        if (!editing) return
        val cur = HitwConfig.getHudScale()
        val neu = (cur + (delta * 0.08f).toFloat()).coerceIn(0.4f, 4.0f)
        HitwConfig.updateHud(HitwConfig.getHudX(), HitwConfig.getHudY(), neu)
    }

    // Data snapshot for the java renderer
    data class HudSnapshot(
        val x: Int,
        val y: Int,
        val scale: Float,
        val editing: Boolean,
        val lines: List<Pair<String, Int>>, // text + color
        val bgColor: Int,
        val hPadding: Int = HitwConfig.getHudHorizontalPadding(),
        val vPadding: Int = HitwConfig.getHudVerticalPadding()
    )

    fun getSnapshot(): HudSnapshot {
        val timers = TimerManager.getActiveTimers()
        val x = HitwConfig.getHudX()
        val y = HitwConfig.getHudY()
        val sc = HitwConfig.getHudScale()
        val bg = if (HitwConfig.getRenderBackground()) 0xCC000000.toInt() else 0x00000000

        if (timers.isEmpty()) {
            return HudSnapshot(x, y, sc, editing, listOf("No current trap" to 0xAAAAAA), bg, HitwConfig.getHudHorizontalPadding(), HitwConfig.getHudVerticalPadding())
        }
        val t = timers.first()
        val remStr = String.format("%.1f", t.getRemainingSeconds(System.currentTimeMillis()))
        val line1 = "${t.trap.name}  ${remStr}s"
        val line2 = t.getDisplayLabel()
        val c1 = HitwConfig.getMainColor()
        val c2 = t.getColor()  // ActiveTimer.getColor() already handles prep phase + fallback to trap mainColor
        return HudSnapshot(x, y, sc, editing, listOf(line1 to c1, line2 to c2), bg, HitwConfig.getHudHorizontalPadding(), HitwConfig.getHudVerticalPadding())
    }
}
