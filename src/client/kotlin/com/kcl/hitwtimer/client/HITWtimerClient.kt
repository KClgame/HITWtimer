package com.kcl.hitwtimer.client

import com.kcl.hitwtimer.client.command.HitwCommands
import com.kcl.hitwtimer.client.config.ConfigLoader
import com.kcl.hitwtimer.client.config.HitwConfig
import com.kcl.hitwtimer.client.detection.SubtitleDetector
import com.kcl.hitwtimer.client.gui.HudEditScreen
import com.kcl.hitwtimer.client.hud.HudRenderer
import com.kcl.hitwtimer.client.timer.TimerManager
import com.mojang.blaze3d.platform.InputConstants
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents
import net.minecraft.client.KeyMapping
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier
import org.lwjgl.glfw.GLFW
import org.slf4j.LoggerFactory

object HITWtimerClient : ClientModInitializer {
    private val logger = LoggerFactory.getLogger("hitwtimer")

    /** Dedicated Controls category: translation key = key.category.hitwtimer.main */
    @JvmField
    val KEY_CATEGORY: KeyMapping.Category = KeyMapping.Category.register(
        Identifier.fromNamespaceAndPath("hitwtimer", "main")
    )

    @JvmField
    val keyToggleHud: KeyMapping = KeyMapping(
        "key.hitwtimer.toggle",
        InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_H,
        KEY_CATEGORY
    )

    @JvmField
    val keyEditHud: KeyMapping = KeyMapping(
        "key.hitwtimer.edit",
        InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_K,
        KEY_CATEGORY
    )

    @JvmField
    val keyReload: KeyMapping = KeyMapping(
        "key.hitwtimer.reload",
        InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_L,
        KEY_CATEGORY
    )

    /** All mod keybinds (used by OptionsMixin to ensure Controls list includes them). */
    @JvmStatic
    fun allKeyMappings(): Array<KeyMapping> = arrayOf(keyToggleHud, keyEditHud, keyReload)

    override fun onInitializeClient() {
        logger.info("HITWtimer client starting...")

        ConfigLoader.ensureConfigExists()
        HitwConfig.reload()

        // Fabric registers into Options.keyMappings on Options.load().
        // If Options already exists (late init), inject manually as well.
        registerKeyMappings()

        HitwCommands.register()

        ClientReceiveMessageEvents.CHAT.register { message, _, _, _, _ ->
            SubtitleDetector.handleChat(message)
        }

        ClientTickEvents.END_CLIENT_TICK.register { client ->
            val now = System.currentTimeMillis()
            TimerManager.tick(now)

            while (keyReload.consumeClick()) {
                HitwConfig.reload()
                SubtitleDetector.clearDedup()
                client.player?.sendSystemMessage(Component.literal("§a[HITW] Reloaded configs + trap lists"))
            }
            while (keyEditHud.consumeClick()) {
                // Toggle via screen: free mouse cursor is required for drag/scale/opacity
                if (client.screen is HudEditScreen) {
                    client.screen?.onClose()
                } else if (client.screen == null) {
                    HudRenderer.enterEdit()
                    client.setScreen(HudEditScreen())
                    client.player?.sendSystemMessage(
                        Component.literal(
                            "§e[HITW] HUD edit: drag=move, scroll=scale, Shift+scroll=opacity, M=mode, Esc/K=save"
                        )
                    )
                }
            }
            while (keyToggleHud.consumeClick()) {
                // Cycle: ALWAYS → ON_TRAP → DISABLED → ALWAYS (persisted on save via edit, or save now)
                val mode = HitwConfig.cycleHudPresence()
                HitwConfig.save()
                val color = when (mode) {
                    com.kcl.hitwtimer.client.config.HudPresence.ALWAYS -> "§a"
                    com.kcl.hitwtimer.client.config.HudPresence.ON_TRAP -> "§e"
                    com.kcl.hitwtimer.client.config.HudPresence.DISABLED -> "§c"
                }
                client.player?.sendSystemMessage(
                    Component.literal("$color[HITW] Mode: ${mode.chatLabel()}")
                )
            }
        }

        logger.info("HITWtimer client initialized. Use /hitwtimer , keys L/K/H . Configs in config/hitwtimer/")
    }

    private fun registerKeyMappings() {
        for (key in allKeyMappings()) {
            try {
                KeyMappingHelper.registerKeyMapping(key)
            } catch (e: IllegalArgumentException) {
                // Already registered (e.g. class reloaded in dev) — safe to ignore
                logger.debug("Key mapping already registered: {}", key.name)
            } catch (e: IllegalStateException) {
                // Options already initialised — Fabric list was too late; OptionsMixin / force inject handle UI
                logger.warn("KeyMappingHelper late: {} — will force-inject into Options", e.message)
            }
        }
        // Belt-and-suspenders: ensure Controls screen can see them even if Fabric load() already ran
        forceInjectIntoOptions()
    }

    /**
     * If Options already exists and our keys are missing from keyMappings, append them.
     * keyMappings is a final field; reassignment is done via reflection (same as mixin @Mutable).
     */
    @JvmStatic
    fun forceInjectIntoOptions() {
        val options = Minecraft.getInstance()?.options ?: return
        val current = options.keyMappings ?: return
        val ours = allKeyMappings()
        val missing = ours.filter { k -> current.none { it === k } }
        if (missing.isEmpty()) return

        val merged = (
            current.filter { existing ->
                ours.none { it === existing || it.name == existing.name }
            } + ours.toList()
        ).toTypedArray()

        try {
            val field = options.javaClass.getDeclaredField("keyMappings")
            field.isAccessible = true
            field.set(options, merged)
            logger.info("Injected {} HITWtimer keybind(s) into Options for Controls UI", missing.size)
        } catch (e: Exception) {
            // OptionsMixin still appends on load/construct; log for diagnostics
            logger.warn("Could not force-inject keybinds into Options: {}", e.message)
        }
    }

    fun onSubtitle(component: Component) {
        SubtitleDetector.handleSubtitle(component)
    }
}
