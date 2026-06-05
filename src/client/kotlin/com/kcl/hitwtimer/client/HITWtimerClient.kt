package com.kcl.hitwtimer.client

import com.kcl.hitwtimer.client.command.HitwCommands
import com.kcl.hitwtimer.client.config.ConfigLoader
import com.kcl.hitwtimer.client.config.HitwConfig
import com.kcl.hitwtimer.client.detection.SubtitleDetector
import com.kcl.hitwtimer.client.hud.HudRenderer
import com.kcl.hitwtimer.client.timer.TimerManager
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
// KeyBindingHelper import removed (may not resolve in this 26 env); keys appended via OptionsMixin instead
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents
import net.minecraft.client.KeyMapping
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import org.lwjgl.glfw.GLFW
import org.slf4j.LoggerFactory

object HITWtimerClient : ClientModInitializer {
    private val logger = LoggerFactory.getLogger("hitwtimer")
    // Keys - appended via OptionsMixin so they show in Controls. Use vanilla Category to satisfy ctor in this mapping version.
    @JvmField val keyToggleHud = KeyMapping("key.hitwtimer.toggle", GLFW.GLFW_KEY_H, KeyMapping.Category.MISC)
    @JvmField val keyEditHud = KeyMapping("key.hitwtimer.edit", GLFW.GLFW_KEY_K, KeyMapping.Category.MISC)
    @JvmField val keyReload = KeyMapping("key.hitwtimer.reload", GLFW.GLFW_KEY_L, KeyMapping.Category.MISC)

    override fun onInitializeClient() {
        logger.info("HITWtimer client starting...")

        // ensure configs
        ConfigLoader.ensureConfigExists()
        HitwConfig.reload()

        // keys are appended into vanilla options via OptionsMixin (no KeyBindingHelper call needed here)

        // commands
        HitwCommands.register()

        // chat / subtitle receive (for detection)
        ClientReceiveMessageEvents.CHAT.register { message, _, _, _, _ ->
            SubtitleDetector.handleChat(message)
        }
        // Note: subtitles are harder; we use GuiTitleMixin (or SubtitleOverlay) to call handleSubtitle

        // tick
        ClientTickEvents.END_CLIENT_TICK.register { client ->
            val now = System.currentTimeMillis()
            TimerManager.tick(now)

            // key handling
            while (keyReload.consumeClick()) {
                HitwConfig.reload()
                SubtitleDetector.clearDedup()
                client.player?.sendSystemMessage(Component.literal("§a[HITW] Reloaded configs + trap lists"))
            }
            while (keyEditHud.consumeClick()) {
                HudRenderer.toggleEdit()
                val on = HudRenderer.isEditing()
                client.player?.sendSystemMessage(Component.literal("§e[HITW] HUD edit mode: $on (drag with mouse, scroll to scale, K again to save+exit)"))
            }
            while (keyToggleHud.consumeClick()) {
                // simple toggle visibility? for demo we just clear or log
                if (TimerManager.hasAny()) {
                    TimerManager.clear()
                    client.player?.sendSystemMessage(Component.literal("§7[HITW] Cleared active timers"))
                } else {
                    client.player?.sendSystemMessage(Component.literal("§7[HITW] HUD visible (no active)"))
                }
            }
        }

        // HUD rendered via GuiMixin (see client mixin)
        logger.info("HITWtimer client initialized. Use /hitwtimer , keys L/K/H . Configs in config/hitwtimer/")
    }

    // Called from mixin when subtitle/title text arrives
    fun onSubtitle(component: Component) {
        SubtitleDetector.handleSubtitle(component)
    }
}
