@file:Suppress("SpellCheckingInspection")

package com.kcl.hitwtimer.client.config

import com.kcl.hitwtimer.client.timer.TimerManager
import com.kcl.hitwtimer.client.trap.TrapDefinition
import net.fabricmc.loader.api.FabricLoader
import java.nio.file.Path
import kotlin.math.roundToInt

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

    private val loadedListConfigs = mutableMapOf<String, TrapListConfig>()

    private val configDir: Path = FabricLoader.getInstance().configDir.resolve("hitwtimer")

    fun getConfigDir(): Path = configDir

    @Deprecated("Use getOverallConfig or similar")
    fun getDefaultConfigPath(): Path = configDir.resolve("overallconfig.txt")

    fun reload() {
        val loaded = ConfigLoader.load()
        global = loaded.first
        loadedTraps = loaded.second
        enabledTrapLists = global.enabledLists.toSet()

        loadedListConfigs.clear()
        for (name in global.enabledLists) {
            ConfigLoader.loadListConfig(name)?.let { loadedListConfigs[name] = it }
        }

        // DISABLED: drop any running timers so no sound / stage continues
        if (!isModActive()) {
            TimerManager.clear()
        }
    }

    fun getEnabledTraps(): List<TrapDefinition> = loadedTraps.filter { it.enabled }

    fun shouldDetectSubtitle(): Boolean = isModActive() && global.detectSubtitle

    fun shouldDetectChat(): Boolean = isModActive() && global.detectChat

    fun getPreparationTime(): Double = global.preparationTime

    fun getPreparationColor(): Int = global.preparationColor

    fun getMainColor(): Int = global.mainColor

    fun isSubtitlePreparationEnabled(): Boolean = global.subtitlePreparationEnabled

    /** File-level master switch from overallconfig `enabled=`. */
    fun isEnabled(): Boolean = global.enabled

    /**
     * Runtime activity: overall enabled AND not HUD DISABLED.
     * When false: no detection, no new timers/sounds, no normal HUD.
     */
    fun isModActive(): Boolean =
        global.enabled && global.hudPresence != HudPresence.DISABLED

    fun getHudPresence(): HudPresence = global.hudPresence

    fun setHudPresence(presence: HudPresence) {
        global = global.copy(hudPresence = presence)
        if (presence == HudPresence.DISABLED) {
            TimerManager.clear()
        }
    }

    /** Cycle ALWAYS → ON_TRAP → DISABLED → ALWAYS. Returns new mode. */
    fun cycleHudPresence(): HudPresence {
        val next = global.hudPresence.next()
        setHudPresence(next)
        return next
    }

    fun getRenderBackground(): Boolean = global.renderBackground

    fun setRenderBackground(on: Boolean) {
        global = global.copy(renderBackground = on)
    }

    /** 0.0 .. 1.0 */
    fun getHudBgOpacity(): Float = global.hudBgOpacity.coerceIn(0f, 1f)

    fun setHudBgOpacity(opacity: Float) {
        global = global.copy(hudBgOpacity = opacity.coerceIn(0f, 1f))
    }

    fun adjustHudBgOpacity(delta: Float) {
        setHudBgOpacity(getHudBgOpacity() + delta)
    }

    /**
     * ARGB for HUD panel background. Alpha from opacity; RGB black.
     * Returns 0 if background disabled or fully transparent.
     */
    fun getHudBackgroundArgb(): Int {
        if (!global.renderBackground) return 0
        val a = (getHudBgOpacity() * 255f).roundToInt().coerceIn(0, 255)
        if (a <= 0) return 0
        return (a shl 24) // black RGB
    }

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
    }

    fun toggleSubtitlePreparation() {
        global = global.copy(subtitlePreparationEnabled = !global.subtitlePreparationEnabled)
    }

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
        if (newGlobal.hudPresence == HudPresence.DISABLED || !newGlobal.enabled) {
            TimerManager.clear()
        }
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

    /**
     * Whether the HUD should be drawn this frame (excluding edit-mode override).
     * Edit mode always shows the panel for positioning.
     */
    fun shouldDrawHud(hasActiveTrap: Boolean, editing: Boolean): Boolean {
        if (editing) return true
        if (!isModActive()) return false
        return when (global.hudPresence) {
            HudPresence.ALWAYS -> true
            HudPresence.ON_TRAP -> hasActiveTrap
            HudPresence.DISABLED -> false
        }
    }
}
