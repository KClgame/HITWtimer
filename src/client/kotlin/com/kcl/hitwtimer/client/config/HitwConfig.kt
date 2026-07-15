@file:Suppress("SpellCheckingInspection")

package com.kcl.hitwtimer.client.config

import com.kcl.hitwtimer.client.trap.TrapDefinition
import net.fabricmc.loader.api.FabricLoader
import java.nio.file.Path

/**
 * Facade for runtime config access. Updated on reload.
 */
object HitwConfig {
    @Volatile
    private var global: GlobalConfig = GlobalConfig()

    @Volatile
    private var loadedTraps: List<TrapDefinition> = emptyList()

    @Volatile
    private var enabledTrapLists: Set<String> = setOf("traplist1")

    // per-list configs (from traplistconfig header)
    private val loadedListConfigs = mutableMapOf<String, TrapListConfig>()

    private val configDir: Path = FabricLoader.getInstance().configDir.resolve("hitwtimer")

    fun getConfigDir(): Path = configDir

    // For compatibility during transition, overall is the global mod config
    @Deprecated("Use getOverallConfig or similar")
    fun getDefaultConfigPath(): Path = configDir.resolve("overallconfig.txt")

    fun reload() {
        val loaded = ConfigLoader.load()
        global = loaded.first
        loadedTraps = loaded.second
        enabledTrapLists = global.enabledLists.toSet()

        // load per-list traplistconfig
        loadedListConfigs.clear()
        for (name in global.enabledLists) {
            ConfigLoader.loadListConfig(name)?.let { loadedListConfigs[name] = it }
        }
    }

    fun getEnabledTraps(): List<TrapDefinition> = loadedTraps.filter { it.enabled }

    fun shouldDetectSubtitle(): Boolean = global.detectSubtitle

    fun getPreparationTime(): Double = global.preparationTime

    fun getPreparationColor(): Int = global.preparationColor

    fun getMainColor(): Int = global.mainColor

    fun isSubtitlePreparationEnabled(): Boolean = global.subtitlePreparationEnabled

    // Additional overall settings
    fun isEnabled(): Boolean = global.enabled
    fun shouldDetectChat(): Boolean = global.detectChat
    fun getRenderBackground(): Boolean = global.renderBackground
    fun getHudHorizontalPadding(): Int = global.hudHorizontalPadding
    fun getHudVerticalPadding(): Int = global.hudVerticalPadding
    fun getAutoReloadKeywords(): List<String> = global.autoReloadKeywords
    fun isDebug(): Boolean = global.debug

    fun getEnabledListNames(): List<String> = global.enabledLists

    fun loadListConfig(listName: String): TrapListConfig? = ConfigLoader.loadListConfig(listName)

    fun getListConfig(listName: String): TrapListConfig? = loadedListConfigs[listName]

    fun setEnabledLists(lists: List<String>) {
        global = global.copy(enabledLists = lists)
        enabledTrapLists = lists.toSet()
        // persist? caller should save if wanted, but commands may call save
    }

    fun toggleSubtitlePreparation() {
        global = global.copy(subtitlePreparationEnabled = !global.subtitlePreparationEnabled)
    }

    // For HUD persistence (simple, real impl may save via ConfigLoader)
    fun updateHud(x: Int, y: Int, scale: Float) {
        global = global.copy(hudX = x, hudY = y, hudScale = scale)
    }

    fun getHudX(): Int = global.hudX
    fun getHudY(): Int = global.hudY
    fun getHudScale(): Float = global.hudScale

    fun save() {
        ConfigLoader.save(global)
    }

    fun getCurrentGlobalConfig(): GlobalConfig = global

    fun updateGlobal(newGlobal: GlobalConfig) {
        global = newGlobal
    }

    fun getTrapsForList(listName: String): List<TrapDefinition> {
        return loadedTraps.filter { it.listName == listName }
    }

    fun getAllListNames(): List<String> = ConfigLoader.loadAllListNames()

    fun createNewList(name: String) {
        ConfigLoader.createNewList(name)
        reload()
    }

    fun deleteList(listName: String): Boolean {
        val ok = ConfigLoader.deleteList(listName)
        if (ok) reload()
        return ok
    }

    fun saveTrapList(listName: String, traps: List<TrapDefinition>, listConfig: TrapListConfig? = null) {
        ConfigLoader.saveTrapList(listName, traps, listConfig)
        reload()
    }

    @Volatile
    private var hudVisible: Boolean = true

    fun isHudVisible(): Boolean = hudVisible
    fun setHudVisible(visible: Boolean) { hudVisible = visible }
}
