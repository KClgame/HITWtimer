package com.kcl.hitwtimer.client.detection

import com.kcl.hitwtimer.client.config.HitwConfig
import com.kcl.hitwtimer.client.timer.TimerManager
import com.kcl.hitwtimer.client.trap.TrapDefinition
import net.minecraft.network.chat.Component
import java.util.concurrent.atomic.AtomicLong

object SubtitleDetector {
    // Simple dedup across recent messages
    private val recent = mutableMapOf<String, Long>()
    private const val DEDUP_MS = 2000L
    private val lastReload = AtomicLong(0)

    fun handleSubtitle(component: Component) {
        val text = component.string
        if (text.isBlank()) return

        val now = System.currentTimeMillis()
        // dedup
        val prev = recent[text] ?: 0
        if (now - prev < DEDUP_MS) return
        recent[text] = now

        // periodic reload of traps? (or on K key / command)
        if (now - lastReload.get() > 30000) {
            HitwConfig.reload()
            lastReload.set(now)
        }

        if (!HitwConfig.shouldDetectSubtitle()) return

        val enabled = HitwConfig.getEnabledTraps()
        for (trap in enabled) {
            if (trap.source == TrapDefinition.Source.CHAT) continue
            if (trap.matches(text)) {
                if (HitwConfig.isDebug()) {
                    val pat = trap.pattern ?: trap.name
                    println("[HITW Debug] Matched SUBTITLE trap '${trap.name}' (pattern='${pat}', mode=${trap.matchMode}, case=${trap.caseSensitive}) from: \"$text\"")
                }
                // resolve preparation for this trap (strict override)
                val prep = resolvePreparation(trap)
                TimerManager.addTrap(trap, now, prep)
                return // one match per message for now
            }
        }
    }

    fun handleChat(component: Component) {
        val text = component.string
        if (text.isBlank()) return
        val now = System.currentTimeMillis()
        val prev = recent[text] ?: 0
        if (now - prev < DEDUP_MS) return
        recent[text] = now

        val enabled = HitwConfig.getEnabledTraps()
        for (trap in enabled) {
            if (trap.source == TrapDefinition.Source.SUBTITLE) continue
            if (trap.matches(text)) {
                if (HitwConfig.isDebug()) {
                    val pat = trap.pattern ?: trap.name
                    println("[HITW Debug] Matched CHAT trap '${trap.name}' (pattern='${pat}', mode=${trap.matchMode}) from: \"$text\"")
                }
                // Chat detection never has preparation phase, regardless of subtitle_preparation setting
                TimerManager.addTrap(trap, now, 0.0)
                return
            }
        }
    }

    /**
     * Resolve preparation time for a subtitle detection.
     * Strict rule: if trap.subtitlePreparation == false -> 0.0 (no prep, no fallback)
     * else if true -> use trap/list/global time
     * else (null) -> list/global
     *
     * Chat detections always pass 0.0 (see handleChat).
     * Preparation is never part of the user-configured trap.events; it's auto-handled in ActiveTimer.
     */
    private fun resolvePreparation(trap: TrapDefinition): Double {
        val listCfg = HitwConfig.getListConfig(trap.listName)
        val usePrep = when (trap.subtitlePreparation) {
            false -> false
            true -> true
            null -> listCfg?.defaultSubtitlePreparation ?: HitwConfig.isSubtitlePreparationEnabled()
        }
        if (!usePrep) return 0.0
        val globalTime = HitwConfig.getPreparationTime()
        return listCfg?.resolvePreparationTime(globalTime) ?: globalTime
    }

    fun clearDedup() {
        recent.clear()
    }
}
