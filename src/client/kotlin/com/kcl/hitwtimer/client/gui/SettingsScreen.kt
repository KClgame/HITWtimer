package com.kcl.hitwtimer.client.gui

import com.kcl.hitwtimer.client.config.HitwConfig
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.components.events.GuiEventListener
import net.minecraft.client.gui.narration.NarratableEntry
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component

/**
 * Main settings screen with three tabs.
 * Accepts optional parent for ModMenu integration.
 *
 * Uses reflection to clear private Screen widget lists on tab switch,
 * since Minecraft 26.x Screen has no public clearWidgets() method.
 */
class SettingsScreen(
    val parent: Screen? = null
) : Screen(Component.literal("HITWtimer 设置")) {

    @Suppress("unused")
    companion object {
        // Reflectively cached field handles for widget list clearing
        private val CHILDREN_FIELD by lazy {
            Screen::class.java.getDeclaredField("children").also { it.isAccessible = true }
        }
        private val RENDERABLES_FIELD by lazy {
            Screen::class.java.getDeclaredField("renderables").also { it.isAccessible = true }
        }
        private val NARRATABLES_FIELD by lazy {
            Screen::class.java.getDeclaredField("narratables").also { it.isAccessible = true }
        }
    }

    private var selectedTab: Int = 0
    private val tabLabels = listOf("全局设置", "陷阱列表", "关于")

    override fun init() {
        super.init()
        rebuildAll()
    }

    @Suppress("UNCHECKED_CAST")
    private fun rebuildAll() {
        // Clear via reflection (Screen fields are private)
        (CHILDREN_FIELD.get(this) as MutableList<GuiEventListener>).clear()
        (RENDERABLES_FIELD.get(this) as MutableList<Any>).clear()
        (NARRATABLES_FIELD.get(this) as MutableList<NarratableEntry>).clear()

        val tabW = (width / 3).coerceAtMost(150)
        val startX = width / 2 - (tabW * 3) / 2
        val tabY = 30

        for ((i, label) in tabLabels.withIndex()) {
            val btn = Button.builder(Component.literal(label)) { _ ->
                selectedTab = i
                rebuildAll()
            }
                .bounds(startX + i * tabW, tabY, tabW - 4, 20)
                .build()
            addRenderableWidget(btn)
        }

        when (selectedTab) {
            0 -> renderOverallTab()
            1 -> renderListsTab()
            2 -> renderAboutTab()
        }
    }

    private fun renderOverallTab() {
        val cfg = HitwConfig.getCurrentGlobalConfig()
        val lines = listOf(
            "HUD: (${cfg.hudX}, ${cfg.hudY})  Scale: ${cfg.hudScale}x",
            "Subtitle: ${cfg.detectSubtitle}  Chat: ${cfg.detectChat}  Debug: ${cfg.debug}",
            "Prep: ${cfg.preparationTime}s  PrepColor: #${String.format("%06X", cfg.preparationColor and 0xFFFFFF)}",
            "MainColor: #${String.format("%06X", cfg.mainColor and 0xFFFFFF)}",
            "Lists: ${cfg.enabledLists.joinToString(", ")}"
        )
        val label = Button.builder(Component.literal("§7§lGlobal Config (read-only preview)")) { }.bounds(width / 2 - 130, 60, 260, 14).build()
        label.active = false
        addRenderableWidget(label)

        var y = 78
        for (line in lines) {
            val lbl = Button.builder(Component.literal("§7$line")) { }.bounds(width / 2 - 140, y, 280, 14).build()
            lbl.active = false
            addRenderableWidget(lbl)
            y += 16
        }

        val note = Button.builder(Component.literal("§8GUI editing widgets coming in Phase 4")) { }.bounds(width / 2 - 120, y + 8, 240, 14).build()
        note.active = false
        addRenderableWidget(note)
    }

    private fun renderListsTab() {
        val names = HitwConfig.getAllListNames()
        val header = Button.builder(Component.literal("§7§lTrap Lists (${names.size} found)")) { }.bounds(width / 2 - 130, 60, 260, 14).build()
        header.active = false
        addRenderableWidget(header)

        var y = 78
        if (names.isEmpty()) {
            val lbl = Button.builder(Component.literal("§7  (No list files created yet)")) { }.bounds(width / 2 - 130, y, 260, 14).build()
            lbl.active = false
            addRenderableWidget(lbl)
            y += 16
        } else {
            for (n in names) {
                val enabled = n in HitwConfig.getEnabledListNames()
                val prefix = if (enabled) "§a[ON]" else "§7[OFF]"
                val trapCount = HitwConfig.getTrapsForList(n).size
                val line = "$prefix §f$n §7($trapCount traps)"
                val lbl = Button.builder(Component.literal(line)) { }
                    .bounds(width / 2 - 140, y, 280, 14).build()
                lbl.active = false
                addRenderableWidget(lbl)
                y += 16
            }
        }

        val note = Button.builder(Component.literal("§8List management UI coming in Phase 5")) { }.bounds(width / 2 - 120, y + 8, 240, 14).build()
        note.active = false
        addRenderableWidget(note)
    }

    private fun renderAboutTab() {
        val version = try {
            net.fabricmc.loader.api.FabricLoader.getInstance()
                .getModContainer("hitwtimer")
                .map { it.metadata.version.friendlyString }
                .orElse("?")
        } catch (_: Exception) { "?" }

        val lines = listOf(
            "§eHITWtimer §f v$version",
            "§7Hole in the Wall trap timer",
            "",
            "§fKeys:",
            "§7  L §8- reload config",
            "§7  K §8- HUD edit mode",
            "§7  H §8- clear timers",
            "",
            "§fCmd: §7/hitwtimer <list|reload|enable|disable|settings>",
            "",
            "§8By KCl"
        )
        var y = 60
        for (line in lines) {
            val lbl = Button.builder(Component.literal(line)) { }
                .bounds(width / 2 - 130, y, 260, 14).build()
            lbl.active = false
            addRenderableWidget(lbl)
            y += 15
        }
    }

    override fun onClose() {
        if (parent != null) {
            minecraft.setScreen(parent)
        } else {
            super.onClose()
        }
    }
}
