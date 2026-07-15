package com.kcl.hitwtimer.client.gui

import com.kcl.hitwtimer.client.HITWtimerClient
import com.kcl.hitwtimer.client.config.HitwConfig
import com.kcl.hitwtimer.client.config.HudPresence
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
 * Controls:
 * - Left-drag: move HUD
 * - Scroll: scale
 * - Shift+Scroll: background opacity (0–100%)
 * - M: cycle HUD mode ALWAYS / ON_TRAP / DISABLED
 * - B: toggle background on/off
 * - Esc / K: save & exit
 */
class HudEditScreen : Screen(Component.literal("HITWtimer HUD Edit")) {

    private var dragging = false
    private var dragOffsetX = 0.0
    private var dragOffsetY = 0.0

    override fun isPauseScreen(): Boolean = false

    override fun isInGameUi(): Boolean = true

    override fun extractBackground(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, a: Float) {
        graphics.fillGradient(0, 0, width, height, 0x33000000, 0x33000000)
        minecraft?.gui?.extractDeferredSubtitles()
    }

    override fun extractRenderState(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, a: Float) {
        super.extractRenderState(graphics, mouseX, mouseY, a)

        val font = minecraft!!.font
        val mode = HitwConfig.getHudPresence()
        val opacityPct = (HitwConfig.getHudBgOpacity() * 100f)
        val lines = listOf(
            "§e§l[HITW HUD EDIT]",
            "§fLeft-drag §7move  §fScroll §7scale  §fShift+Scroll §7opacity",
            "§fM §7cycle mode  §fB §7toggle background  §fEsc/K §7save & exit",
            "§7Pos: (${HitwConfig.getHudX()}, ${HitwConfig.getHudY()})  Scale: ${"%.2f".format(HitwConfig.getHudScale())}x",
            "§7Opacity: ${"%.0f".format(opacityPct)}%  BG: ${if (HitwConfig.getRenderBackground()) "on" else "off"}",
            "§7Mode: §f${mode.displayName()} §8— ${modeHint(mode)}"
        )
        var y = 8
        for (line in lines) {
            graphics.centeredText(font, line, width / 2, y, 0xFFFFFF)
            y += font.lineHeight + 2
        }
    }

    private fun modeHint(mode: HudPresence): String = when (mode) {
        HudPresence.ALWAYS -> "HUD always shown"
        HudPresence.ON_TRAP -> "HUD only when trap active"
        HudPresence.DISABLED -> "no HUD / no detect / no sound"
    }

    override fun mouseClicked(event: MouseButtonEvent, doubleClick: Boolean): Boolean {
        if (event.button() == 0) {
            dragging = true
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
        if (scrollY == 0.0) return super.mouseScrolled(x, y, scrollX, scrollY)

        val shift = hasShiftDown()
        if (shift) {
            HudRenderer.adjustOpacity(scrollY)
        } else {
            HudRenderer.adjustScale(scrollY)
        }
        return true
    }

    override fun keyPressed(event: KeyEvent): Boolean {
        if (event.key() == GLFW.GLFW_KEY_ESCAPE) {
            closeEdit()
            return true
        }
        if (HITWtimerClient.keyEditHud.matches(event)) {
            closeEdit()
            return true
        }
        // M: cycle presence mode
        if (event.key() == GLFW.GLFW_KEY_M) {
            val next = HitwConfig.cycleHudPresence()
            // Stay in edit even if DISABLED so user can switch back
            return true
        }
        // B: toggle background
        if (event.key() == GLFW.GLFW_KEY_B) {
            HitwConfig.setRenderBackground(!HitwConfig.getRenderBackground())
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
        if (minecraft?.screen === this) {
            minecraft?.setScreen(null)
        }
    }

    companion object {
        private fun hasShiftDown(): Boolean {
            val mc = net.minecraft.client.Minecraft.getInstance()
            val win = mc.window.handle()
            return org.lwjgl.glfw.GLFW.glfwGetKey(win, GLFW.GLFW_KEY_LEFT_SHIFT) == GLFW.GLFW_PRESS ||
                org.lwjgl.glfw.GLFW.glfwGetKey(win, GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS
        }
    }
}
