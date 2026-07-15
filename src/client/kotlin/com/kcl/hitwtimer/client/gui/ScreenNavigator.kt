package com.kcl.hitwtimer.client.gui

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.Screen

/**
 * Navigation stack for the settings GUI.
 * Tracks the back stack so sub-screens can pop back to the previous one.
 */
object ScreenNavigator {
    private val backStack = ArrayDeque<Screen>()

    fun push(screen: Screen) {
        backStack.addLast(screen)
        Minecraft.getInstance().setScreen(screen)
    }

    fun pop() {
        backStack.removeLastOrNull()
        val prev = backStack.lastOrNull()
        Minecraft.getInstance().setScreen(prev)
    }

    fun popToRoot() {
        backStack.clear()
        Minecraft.getInstance().setScreen(null)
    }

    fun notifyClosed(screen: Screen) {
        if (backStack.isNotEmpty() && backStack.last() === screen) {
            backStack.removeLast()
        }
    }
}
