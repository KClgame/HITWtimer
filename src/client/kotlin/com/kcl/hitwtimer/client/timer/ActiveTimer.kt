package com.kcl.hitwtimer.client.timer

import com.kcl.hitwtimer.client.trap.TrapDefinition
import com.kcl.hitwtimer.client.trap.TrapEvent
import net.minecraft.client.Minecraft
import kotlin.math.max

/**
 * Runtime active timer instance for one matched trap.
 * Handles preparation stage (if not explicitly disabled at trap level) + the events.
 */
class ActiveTimer(
    val trap: TrapDefinition,
    val triggerTimeMillis: Long,
    val preparationSeconds: Double, // resolved at creation time from config/trap
    val listName: String? = null
) {
    private val events: List<TrapEvent> = trap.events.sortedBy { it.offsetSeconds }
    private var currentEventIndex = 0
    private var preparationDone = false
    private var finished = false
    private var lastPlayedStart = false

    val totalDuration: Double
        get() = (if (preparationSeconds > 0) preparationSeconds else 0.0) + (events.lastOrNull()?.offsetSeconds ?: 0.0)

    fun isFinished(): Boolean = finished

    fun getDisplayLabel(): String {
        if (!preparationDone && preparationSeconds > 0) {
            return "准备: ${trap.name}"
        }
        val ev = if (currentEventIndex < events.size) events[currentEventIndex] else events.lastOrNull()
        return ev?.label ?: trap.name
    }

    fun getColor(): Int {
        if (!preparationDone && preparationSeconds > 0) {
            return trap.preparationColor ?: 0x55FF55 // use trap prep color or default green
        }
        val ev = if (currentEventIndex < events.size) events[currentEventIndex] else null
        return ev?.color
            ?: trap.mainColor
            ?: 0xFFFFFF
    }

    fun getRemainingSeconds(nowMillis: Long): Double {
        val elapsed = getElapsed(nowMillis)
        if (!preparationDone && preparationSeconds > 0) {
            return max(0.0, preparationSeconds - elapsed)
        }
        val base = if (preparationSeconds > 0) preparationSeconds else 0.0
        val target = base + (events.getOrNull(currentEventIndex)?.offsetSeconds ?: 0.0)
        return max(0.0, target - elapsed)
    }

    fun getElapsed(nowMillis: Long): Double = (nowMillis - triggerTimeMillis) / 1000.0

    /**
     * Advance stages, play sounds. Returns sound to play this tick or null.
     */
    fun tick(nowMillis: Long): String? {
        if (finished) return null
        val elapsed = getElapsed(nowMillis)

        if (!preparationDone) {
            if (preparationSeconds <= 0 || elapsed >= preparationSeconds) {
                preparationDone = true
                // play start sound on leaving prep / entering main
                if (!lastPlayedStart && trap.startSound != null) {
                    lastPlayedStart = true
                    playSound(trap.startSound)
                    return trap.startSound
                }
            } else {
                return null
            }
        }

        // advance events
        while (currentEventIndex < events.size) {
            val base = if (preparationSeconds > 0) preparationSeconds else 0.0
            val target = base + events[currentEventIndex].offsetSeconds
            if (elapsed >= target) {
                val ev = events[currentEventIndex]
                currentEventIndex++
                if (ev.sound != null) {
                    playSound(ev.sound)
                    return ev.sound
                }
                // continue to check next immediately if 0 duration gap
            } else break
        }

        if (currentEventIndex >= events.size && !finished) {
            finished = true
            if (trap.endSound != null) {
                playSound(trap.endSound)
                return trap.endSound
            }
        }
        return null
    }

    private fun playSound(id: String) {
        // Sound playback disabled for compile (ResourceLocation / client classes visibility in this env).
        // In real run the sounds would play here using Minecraft sound manager.
        val mc = Minecraft.getInstance()
        // mc.soundManager.play(... ) would go here
    }

    fun checkPreparationEndAndGetSound(nowMillis: Long): String? {
        if (preparationDone) return null
        val elapsed = getElapsed(nowMillis)
        if (preparationSeconds > 0 && elapsed >= preparationSeconds) {
            preparationDone = true
            if (trap.startSound != null && !lastPlayedStart) {
                lastPlayedStart = true
                playSound(trap.startSound)
                return trap.startSound
            }
        }
        return null
    }
}
