package com.kcl.hitwtimer.client.gui

import com.kcl.hitwtimer.client.HITWtimerClient
import com.kcl.hitwtimer.client.config.HitwConfig
import com.kcl.hitwtimer.client.hud.HudRenderer
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component
import org.lwjgl.glfw.GLFW

/**
 * Overlay screen for HUD edit mode.
 *
 * Why a screen is required:
 * - In-game the mouse is grabbed; free-cursor drag cannot work without a Screen open.
 * - Mouse events arrive in GUI-scaled coordinates (matches HUD draw space).
 *
 * Controls:
 * - Left-drag: move HUD
 * - Scroll wheel: scale HUD
 * - Esc / K (edit keybind): save & exit
 */
class HudEditScreen : Screen(Component.literal("HITWtimer HUD Edit")) {

    private var dragging = false
    private var dragOffsetX = 0.0
    private var dragOffsetY = 0.0

    override fun isPauseScreen(): Boolean = false

    /** Use in-game UI style (no dirt menu background). */
    override fun isInGameUi(): Boolean = true

    override fun extractBackground(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, a: Float) {
        // Very light dim so the world stays visible while editing
        graphics.fillGradient(0, 0, width, height, 0x33000000, 0x33000000)
        minecraft?.gui?.extractDeferredSubtitles()
    }

    override fun extractRenderState(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, a: Float) {
        super.extractRenderState(graphics, mouseX, mouseY, a)

        val font = minecraft!!.font
        val lines = listOf(
            "§e§l[HITW HUD EDIT]",
            "§fLeft-drag §7to move   §fScroll §7to scale   §fEsc/K §7to save & exit",
            "§7Pos: (${HitwConfig.getHudX()}, ${HitwConfig.getHudY()})  Scale: ${"%.2f".format(HitwConfig.getHudScale())}x"
        )
        var y = 8
        for (line in lines) {
            graphics.centeredText(font, line, width / 2, y, 0xFFFFFF)
            y += font.lineHeight + 2
        }
    }

    override fun mouseClicked(event: MouseButtonEvent, doubleClick: Boolean): Boolean {
        if (event.button() == 0) {
            dragging = true
            // Offset from HUD top-left (GUI-scaled coords, same as draw space)
            dragOffsetX = event.x() - HitwConfig.getHudX()
            dragOffsetY = event.y() - HitwConfig.getHudY()
            return true
        }
        return super.mouseClicked(event, doubleClick)
    }

    override fun mouseReleased(event: MouseButtonEvent): Boolean {
        if (event.button() == 0 && dragging) {
            dragging = false
            return true
        }
        return super.mouseReleased(event)
    }

    override fun mouseDragged(event: MouseButtonEvent, dx: Double, dy: Double): Boolean {
        if (dragging) {
            val maxX = (width - 20).coerceAtLeast(0)
            val maxY = (height - 20).coerceAtLeast(0)
            val newX = (event.x() - dragOffsetX).toInt().coerceIn(0, maxX)
            val newY = (event.y() - dragOffsetY).toInt().coerceIn(0, maxY)
            HitwConfig.updateHud(newX, newY, HitwConfig.getHudScale())
            return true
        }
        return super.mouseDragged(event, dx, dy)
    }

    override fun mouseScrolled(x: Double, y: Double, scrollX: Double, scrollY: Double): Boolean {
        if (scrollY != 0.0) {
            HudRenderer.adjustScale(scrollY)
            return true
        }
        return super.mouseScrolled(x, y, scrollX, scrollY)
    }

    override fun keyPressed(event: KeyEvent): Boolean {
        // Esc handled by Screen; also exit on the edit keybind (default K)
        if (event.key() == GLFW.GLFW_KEY_ESCAPE) {
            closeEdit()
            return true
        }
        // Match current edit key binding
        if (HITWtimerClient.keyEditHud.matches(event)) {
            closeEdit()
            return true
        }
        return super.keyPressed(event)
    }

    override fun onClose() {
        closeEdit()
    }

    private fun closeEdit() {
        dragging = false
        HudRenderer.exitEdit(save = true)
        // Avoid re-entrant setScreen(null) loops
        if (minecraft?.screen === this) {
            minecraft?.setScreen(null)
        }
    }
}
