package com.kcl.hitwtimer.client.timer

import com.kcl.hitwtimer.client.config.HitwConfig
import com.kcl.hitwtimer.client.trap.TrapDefinition
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import java.util.concurrent.CopyOnWriteArrayList

object TimerManager {
    private val active = CopyOnWriteArrayList<ActiveTimer>()

    // For dedup: last trigger time per trap name
    private val lastTrigger = mutableMapOf<String, Long>()

    fun clear() {
        active.clear()
    }

    fun getActiveTimers(): List<ActiveTimer> = active.toList()

    /**
     * Add a trap timer. preparationSeconds: resolved by caller.
     * - subtitle detection + subtitle_preparation allowed (considering strict trap=false): >0 → prep phase active
     * - chat detection: always 0.0 (no prep ever)
     * Preparation is NOT a user event in trap.events; it's auto-handled in ActiveTimer (label, timing, sound delay).
     */
    fun addTrap(trap: TrapDefinition, nowMillis: Long, preparationSeconds: Double, listName: String? = null) {
        val dedupWindow = 1500L // ms
        val last = lastTrigger[trap.name] ?: 0
        if (nowMillis - last < dedupWindow) {
            return // dedup
        }
        lastTrigger[trap.name] = nowMillis

        // remove any previous same name
        active.removeIf { it.trap.name == trap.name }

        val timer = ActiveTimer(trap, nowMillis, preparationSeconds.coerceAtLeast(0.0), listName)
        active.add(timer)

        // Debug-only feedback to avoid chat spam for normal users.
        // When debug=true in overallconfig.txt, we log trap activation details.
        if (HitwConfig.isDebug()) {
            val mc = Minecraft.getInstance()
            if (mc.player != null) {
                mc.player?.sendSystemMessage(
                    Component.literal("§a[HITW Debug] ${trap.name} timer started (prep=${preparationSeconds}s, list=${listName ?: "default"})")
                )
            }
        }
    }

    /**
     * Tick all active, remove finished ones. Play sounds via the ActiveTimer.
     */
    fun tick(nowMillis: Long) {
        // For CopyOnWriteArrayList, use removeAll to avoid COWIterator.remove() UnsupportedOperationException
        active.forEach { it.tick(nowMillis) }
        active.removeAll { it.isFinished() }
        // cleanup old dedup entries
        val cutoff = nowMillis - 10000
        lastTrigger.entries.removeIf { it.value < cutoff }
    }

    fun hasAny(): Boolean = active.isNotEmpty()
}
