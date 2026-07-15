package com.kcl.hitwtimer.client.command

import com.kcl.hitwtimer.client.config.HitwConfig
import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import net.minecraft.network.chat.Component

object HitwCommands {
    fun register() {
        ClientCommandRegistrationCallback.EVENT.register { dispatcher, _ ->
            registerCommands(dispatcher)
        }
    }

    private fun registerCommands(dispatcher: CommandDispatcher<FabricClientCommandSource>) {
        dispatcher.register(
            LiteralArgumentBuilder.literal<FabricClientCommandSource>("hitwtimer")
                .then(
                    LiteralArgumentBuilder.literal<FabricClientCommandSource>("list")
                        .executes { context ->
                            val enabled = HitwConfig.getEnabledListNames()
                            if (enabled.isEmpty()) {
                                context.source.sendFeedback(
                                    Component.literal("§7当前没有启用的列表")
                                )
                            } else {
                                context.source.sendFeedback(
                                    Component.literal("§a当前启用的列表：")
                                )
                                enabled.forEach { name ->
                                    val cfg = HitwConfig.getListConfig(name)
                                    val desc = cfg?.description?.takeIf { it.isNotBlank() } ?: ""
                                    val line = if (desc.isNotEmpty()) {
                                        "  §f- §a$name §7$desc"
                                    } else {
                                        "  §f- §a$name"
                                    }
                                    context.source.sendFeedback(Component.literal(line))
                                }
                            }
                            1
                        }
                )
                .then(
                    LiteralArgumentBuilder.literal<FabricClientCommandSource>("reload")
                        .executes { context ->
                            HitwConfig.reload()
                            context.source.sendFeedback(
                                Component.literal("§a[HITW] 配置和陷阱列表已重载")
                            )
                            1
                        }
                )
                .then(
                    LiteralArgumentBuilder.literal<FabricClientCommandSource>("enable")
                        .then(
                            com.mojang.brigadier.builder.RequiredArgumentBuilder.argument<FabricClientCommandSource, String>(
                                "name",
                                com.mojang.brigadier.arguments.StringArgumentType.word()
                            ).executes { context ->
                                val name = com.mojang.brigadier.arguments.StringArgumentType.getString(context, "name")
                                val current = HitwConfig.getEnabledListNames().toMutableList()
                                if (!current.contains(name)) current.add(name)
                                HitwConfig.setEnabledLists(current)
                                HitwConfig.reload()
                                context.source.sendFeedback(
                                    Component.literal("§a[HITW] 已启用列表: $name （重载生效）")
                                )
                                1
                            }
                        )
                )
                .then(
                    LiteralArgumentBuilder.literal<FabricClientCommandSource>("disable")
                        .then(
                            com.mojang.brigadier.builder.RequiredArgumentBuilder.argument<FabricClientCommandSource, String>(
                                "name",
                                com.mojang.brigadier.arguments.StringArgumentType.word()
                            ).executes { context ->
                                val name = com.mojang.brigadier.arguments.StringArgumentType.getString(context, "name")
                                val current = HitwConfig.getEnabledListNames().filter { it != name }
                                HitwConfig.setEnabledLists(current)
                                HitwConfig.reload()
                                context.source.sendFeedback(
                                    Component.literal("§e[HITW] 已禁用列表: $name （重载生效）")
                                )
                                1
                            }
                        )
                )
                .then(
                    LiteralArgumentBuilder.literal<FabricClientCommandSource>("settings")
                        .executes { context ->
                            context.source.client.setScreen(
                                com.kcl.hitwtimer.client.gui.SettingsScreen()
                            )
                            1
                        }
                )
                .executes { context ->
                    context.source.sendFeedback(
                        Component.literal("§e[HitwTimer] 可用子命令: list | reload | enable <name> | disable <name> | settings")
                    )
                    1
                }
        )
    }
}
