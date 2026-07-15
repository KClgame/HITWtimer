@file:Suppress("SpellCheckingInspection", "unused", "LocalVariableName", "FunctionName")

package com.kcl.hitwtimer.client.config

import com.kcl.hitwtimer.client.trap.TrapDefinition
import com.kcl.hitwtimer.client.trap.TrapEvent
import net.fabricmc.loader.api.FabricLoader
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import kotlin.io.path.*

@Suppress("SpellCheckingInspection", "unused", "LocalVariableName")
object ConfigLoader {
    private val configDir: Path = FabricLoader.getInstance().configDir.resolve("hitwtimer")
    private val overallConfigPath: Path = configDir.resolve("overallconfig.txt")

    fun getOverallConfigPath(): Path = overallConfigPath
    fun getTrapListPath(name: String): Path = configDir.resolve("$name.txt")

    /**
     * Ensure overallconfig.txt and at least traplist1.txt exist with clean templates.
     * Called on init and reload.
     * overallconfig.txt = mod-level (overall) settings
     * traplist*.txt = list header (traplistconfig) + traps (trapconfig)
     */
    fun ensureConfigExists() {
        if (!configDir.exists()) {
            configDir.createDirectories()
        }
        if (!overallConfigPath.exists()) {
            overallConfigPath.writeText(buildOverallConfigExample(), Charsets.UTF_8)
        }
        val trapList1 = getTrapListPath("traplist1")
        if (!trapList1.exists()) {
            trapList1.writeText(buildTrapListTemplate("traplist1"), Charsets.UTF_8)
        }
    }

    /**
     * Global only example (no traps inside). Contains the full user-provided bilingual template instructions.
     */
    fun buildOverallConfigExample(): String {
        val sb = StringBuilder()
        sb.appendLine("# HITWtimer Overall Config (overallconfig.txt)")
        sb.appendLine("# This is the TOP-LEVEL / MOD-LEVEL configuration.")
        sb.appendLine("# It controls the entire mod behavior, HUD defaults, detection, and initial lists.")
        sb.appendLine("# ")
        sb.appendLine("# Trap list specific defaults (traplistconfig) go at the top of each traplist*.txt")
        sb.appendLine("# Individual trap settings (trapconfig) go under name= sections in traplist*.txt")
        sb.appendLine("# You can control lists at runtime with: /hitwtimer enable <name> / disable <name> / reload")
        sb.appendLine()
        sb.appendLine("# --- Master ---")
        sb.appendLine("enabled=true")
        sb.appendLine()
        sb.appendLine("# --- Lists (which traplist*.txt to load) ---")
        sb.appendLine("enabled_lists=traplist1")
        sb.appendLine("# Example for multiple: enabled_lists=traplist1,pvp,custom")
        sb.appendLine()
        sb.appendLine("# --- Detection ---")
        sb.appendLine("detect_subtitle=true")
        sb.appendLine("detect_chat=true")
        sb.appendLine()
        sb.appendLine("# --- Preparation defaults (used if not overridden in list or trap) ---")
        sb.appendLine("subtitle_preparation=true")
        sb.appendLine("preparation_time=3.0")
        sb.appendLine("preparation_color=#55FF55")
        sb.appendLine("main_color=#FFFFFF")
        sb.appendLine()
        sb.appendLine("# Note: Preparation phase is auto-inserted (in timing + display) ONLY for subtitle detections.")
        sb.appendLine("# Chat detections never have preparation. Do not configure 'Preparation' as an event in traps.")
        sb.appendLine("# --- HUD appearance and position (editable in-game with edit mode) ---")
        sb.appendLine("hud_x=10")
        sb.appendLine("hud_y=10")
        sb.appendLine("hud_scale=1.0")
        sb.appendLine("render_background=true")
        sb.appendLine("hud_horizontal_padding=8")
        sb.appendLine("hud_vertical_padding=6")
        sb.appendLine()
        sb.appendLine("# --- Behavior ---")
        sb.appendLine("auto_reload_keywords=hitw,hole in the wall,游戏开始")
        sb.appendLine()
        sb.appendLine("# --- Misc ---")
        sb.appendLine("debug=false")
        sb.appendLine("#   true 时会输出额外调试信息：")
        sb.appendLine("#   - 每次匹配陷阱时打印到控制台 (println)")
        sb.appendLine("#   - 触发陷阱时在聊天显示详细启动信息（正常模式下静默）")
        sb.appendLine("#   适合测试为什么某个陷阱没触发或想看匹配细节时使用。")
        sb.appendLine("#   日常使用建议保持 false，避免聊天刷屏。")
        return sb.toString()
    }

    /**
     * Clean trap-only template for a named list (e.g. traplist1.txt).
     * Contains the exact CN template header the user requested + simplified EN below.
     * NO concrete example traps.
     */
    fun buildTrapListTemplate(listName: String): String {
        val sb = StringBuilder()
        sb.appendLine("# HITWtimer 陷阱列表: $listName")
        sb.appendLine("# 此文件只放陷阱定义。可在 overallconfig.txt 的 enabled_lists 中引用本文件名(不带.txt)")
        sb.appendLine("# 运行时用 /hitwtimer enable $listName 启用，disable 禁用。支持多个列表同时激活。")
        sb.appendLine()
        sb.appendLine("# ==================== 陷阱配置模板 (Trap Configuration Template) ====================")
        sb.appendLine("#")
        sb.appendLine("# 说明 / Instructions:")
        sb.appendLine("# 1. 每个陷阱必须以 name= 开头 (pattern= 也必填) / Every trap must start with name= (pattern= is also required)")
        sb.appendLine("# 2. 所有参数都可以按需填写，不需要的可以删除 / All fields are optional except name and events")
        sb.appendLine("# 3. events 阶段从上到下依次执行 / Events are executed from top to bottom in order")
        sb.appendLine("#")
        sb.appendLine("# 层级: overallconfig (mod) > traplistconfig (list defaults in traplist*.txt header) > trapconfig (per trap)")
        sb.appendLine("#")
        sb.appendLine("# 【参数详解 / Parameter Reference】")
        sb.appendLine("# 必须 (Required):")
        sb.appendLine("#   name=xxx                  陷阱唯一标识 (必填)")
        sb.appendLine("#   pattern=...               检测匹配的字幕/聊天关键词 (必填，支持 | 分隔多个备选)")
        sb.appendLine("#   events (下面的阶段行)     至少一个阶段 (必填)")
        sb.appendLine("#")
        sb.appendLine("# 可选 (Optional, 不填则使用本列表traplistconfig的默认, 再回退overall):")
        sb.appendLine("#   enabled=true/false")
        sb.appendLine("#   source=SUBTITLE/CHAT/BOTH")
        sb.appendLine("#   subtitle_preparation=true/false   (false=严格无准备)")
        sb.appendLine("#   start_sound=...")
        sb.appendLine("#   end_sound=...")
        sb.appendLine("#   main_color=#RRGGBB      陷阱默认事件颜色 (event没指定color时用)")
        sb.appendLine("#   preparation_time=3.0    本陷阱准备时长 (覆盖list)")
        sb.appendLine("#   preparation_color=#RRGGBB")
        sb.appendLine("#")
        sb.appendLine("# 检测相关 (Detection pattern & match mode):")
        sb.appendLine("#   pattern=要匹配的字幕/聊天原文 (必填)")
        sb.appendLine("#     支持 | 分隔多个关键词(OR): pattern=falling sand|sand")
        sb.appendLine("#     REGEX 模式下 | 按正则本身含义")
        sb.appendLine("#   match_mode=CONTAINS (默认,包含匹配) / EQUALS(完全相等) / STARTS_WITH / REGEX")
        sb.appendLine("#   case_sensitive=false (默认, 不区分大小写; true 则严格大小写)")
        sb.appendLine("#")
        sb.appendLine("# 事件格式 (Event format):")
        sb.appendLine("#   eventname|time|color(可选)|sound(可选)")
        sb.appendLine("#   color不填 -> 用本trap的main_color")
        sb.appendLine("#   示例:")
        sb.appendLine("#     stop falling|10|#FF5500|entity.blaze.shoot")
        sb.appendLine("#     sands gone|5.0|")
        sb.appendLine("#")
        sb.appendLine("# 重要说明 / Important Note:")
        sb.appendLine("# Preparation（准备阶段） **不需要** 在 trap 的 events 中配置！")
        sb.appendLine("# 它完全由检测来源自动决定：")
        sb.appendLine("# - 来源是 subtitle 且检测到该 trap，并且 subtitle_preparation 允许（trap/list/global 三级，trap 的 false 严格优先）")
        sb.appendLine("#   → 自动作为 trap 阶段列表第一行插入（显示英文 Preparation、使用 preparation_time、延迟 start_sound）")
        sb.appendLine("# - 来源是 chat（非 subtitle）→ 永不插入准备阶段（preparationSeconds=0）")
        sb.appendLine("# trap 的 events 只列出检测到后实际执行的阶段。")
        sb.appendLine("#")
        sb.appendLine("# 英文版说明 (English):")
        sb.appendLine("# Required: name=, pattern=, events")
        sb.appendLine("# Optional: fall back to traplistconfig defaults then overall.")
        sb.appendLine("# Event: name|duration|color(optional)|sound(optional)")
        sb.appendLine("#   color falls back to this trap's main_color")
        sb.appendLine("# IMPORTANT: Do NOT put a 'Preparation' stage in your trap's events list.")
        sb.appendLine("# Preparation is auto-inserted as the FIRST stage row in the HUD (label: Preparation).")
        sb.appendLine("# Only for subtitle detections (based on subtitle_preparation). Chat never has prep.")
        sb.appendLine()
        sb.appendLine("# --- traplistconfig: 本列表的状态和默认配置 (在第一个 name= 之前) ---")
        sb.appendLine("# list status")
        sb.appendLine("# enabled=true")
        sb.appendLine("# description=这个列表的描述 / 分类名字 (用于显示或分类)")
        sb.appendLine("#")
        sb.appendLine("# defaults for traps in this list (if trap doesn't define, use these)")
        sb.appendLine("# subtitle_preparation=true")
        sb.appendLine("# preparation_time=3.0")
        sb.appendLine("# preparation_color=#55FF55")
        sb.appendLine("# main_color=#FFFFFF")
        sb.appendLine("# start_sound=block.note_block.pling")
        sb.appendLine("# end_sound=block.note_block.bell")
        sb.appendLine("# source=SUBTITLE")
        sb.appendLine("# enabled=true")
        sb.appendLine("#")
        sb.appendLine("# 检测默认 (可选):")
        sb.appendLine("# match_mode=CONTAINS")
        sb.appendLine("# case_sensitive=false")
        sb.appendLine("# pattern=   (列表级默认较少用, 通常每个trap单独指定必填的pattern)")
        sb.appendLine()
        sb.appendLine("# 在下面添加你的陷阱定义 (name= 开始一个新陷阱)")
        sb.appendLine("# Add your trap definitions below (start each with name=)")
        sb.appendLine()
        sb.appendLine("# ========== 示例陷阱 (Concrete Examples) ==========")
        sb.appendLine("# 这些是完整可用的示例。你可以复制修改。")
        sb.appendLine("# 注意: pattern 是每个 trap 的必填变量，用于检测匹配。")
        sb.appendLine()
        sb.appendLine("name=falling sand")
        sb.appendLine("enabled=true")
        sb.appendLine("source=SUBTITLE")
        sb.appendLine("subtitle_preparation=true")
        sb.appendLine("main_color=#FFAA00")
        sb.appendLine("start_sound=block.fire.extinguish")
        sb.appendLine("end_sound=block.fire.extinguish")
        sb.appendLine("pattern=falling sand|sand   # 支持|分隔多个匹配词 (必填)")
        sb.appendLine("match_mode=CONTAINS")
        sb.appendLine("case_sensitive=false")
        sb.appendLine("events=")
        sb.appendLine("  stop falling|10|#FF5500|entity.blaze.shoot")
        sb.appendLine("  sands gone|5.0|")
        sb.appendLine()
        return sb.toString()
    }

    /**
     * Load overall (mod) config from overallconfig.txt + all enabled trap lists.
     * Returns (Overall/GlobalConfig, merged enabled TrapDefinitions)
     */
    fun load(): Pair<GlobalConfig, List<TrapDefinition>> {
        ensureConfigExists()

        val global = parseOverall(overallConfigPath)
        val traps = mutableListOf<TrapDefinition>()
        val seen = mutableSetOf<String>() // dedup by name across lists? last wins or merge?

        for (listName in global.enabledLists) {
            val listPath = getTrapListPath(listName)
            if (listPath.exists()) {
                val (listCfg, listTraps) = parseTrapList(listPath, listName)
                // TODO: store listCfg somewhere (e.g. in HitwConfig) for runtime list defaults
                for (t in listTraps) {
                    if (seen.add(t.name)) {
                        traps.add(t)
                    }
                }
            } else {
                // create on the fly?
                listPath.writeText(buildTrapListTemplate(listName), Charsets.UTF_8)
            }
        }
        return global to traps
    }

    private fun parseOverall(path: Path): GlobalConfig {
        if (!path.exists()) return GlobalConfig()
        val lines = Files.readAllLines(path, Charsets.UTF_8)
        var enabled = true
        var enabledLists = listOf("traplist1")
        var detectSubtitle = true
        var detectChat = true
        var subtitlePrep = true
        var prepTime = 3.0
        var prepColor = 0x55FF55
        var mainColor = 0xFFFFFF
        var hx = 10
        var hy = 10
        var hs = 1.0f
        var renderBg = true
        var hPad = 8
        var vPad = 6
        var autoKeywords = listOf("hitw", "hole in the wall", "游戏开始")
        var debug = false

        for (raw in lines) {
            val line = raw.trim()
            if (line.isEmpty() || line.startsWith("#")) continue
            val eq = line.indexOf('=')
            if (eq < 0) continue
            val key = line.substring(0, eq).trim().lowercase()
            val value = line.substring(eq + 1).trim()
            when (key) {
                "enabled" -> enabled = value.equals("true", true)
                "enabled_lists", "enabledlists" -> {
                    enabledLists = value.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                }
                "detect_subtitle", "detectsubtitle", "detect_title", "detecttitle" -> {
                    detectSubtitle = value.equals("true", true)
                }
                "detect_chat", "detectchat" -> {
                    detectChat = value.equals("true", true)
                }
                "subtitle_preparation", "subtitlepreparation" -> {
                    subtitlePrep = value.equals("true", true)
                }
                "preparation_time", "preparationtime", "prep_time" -> {
                    prepTime = value.toDoubleOrNull() ?: 3.0
                }
                "preparation_color", "preparationcolor", "prep_color" -> {
                    prepColor = parseColor(value)
                }
                "main_color", "maincolor" -> {
                    mainColor = parseColor(value)
                }
                "hud_x" -> hx = value.toIntOrNull() ?: 10
                "hud_y" -> hy = value.toIntOrNull() ?: 10
                "hud_scale" -> hs = value.toFloatOrNull() ?: 1.0f
                "render_background", "renderbackground" -> {
                    renderBg = value.equals("true", true)
                }
                "hud_horizontal_padding", "hudhorizontalpadding" -> hPad = value.toIntOrNull() ?: 8
                "hud_vertical_padding", "hudverticalpadding" -> vPad = value.toIntOrNull() ?: 6
                "auto_reload_keywords", "autoreloadkeywords" -> {
                    autoKeywords = value.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                }
                "debug" -> debug = value.equals("true", true)
            }
        }
        return GlobalConfig(
            enabled = enabled,
            enabledLists = enabledLists,
            detectSubtitle = detectSubtitle,
            detectChat = detectChat,
            subtitlePreparationEnabled = subtitlePrep,
            preparationTime = prepTime,
            preparationColor = prepColor,
            mainColor = mainColor,
            hudX = hx,
            hudY = hy,
            hudScale = hs,
            renderBackground = renderBg,
            hudHorizontalPadding = hPad,
            hudVerticalPadding = vPad,
            autoReloadKeywords = autoKeywords,
            debug = debug
        )
    }

    /**
     * Parse a trap list file (traplist*.txt).
     * First section (before any name=) is traplistconfig: list status + defaults for traps in this list.
     * Traps after: trapconfig, inherit list defaults if not defined (strict for subtitle_preparation=false).
     * Returns the list config + its traps (with listName set).
     */
    private fun parseTrapList(path: Path, listName: String): Pair<TrapListConfig, List<TrapDefinition>> {
        if (!path.exists()) {
            return TrapListConfig(listName) to emptyList()
        }
        val lines = Files.readAllLines(path, Charsets.UTF_8)
        val result = mutableListOf<TrapDefinition>()

        // === Parse traplistconfig header (before first name=) ===
        val listSettings = mutableMapOf<String, String>()
        val trapLines = mutableListOf<String>()
        var inListHeader = true

        for (raw in lines) {
            val line = raw.trim()
            if (line.isEmpty() || line.startsWith("#")) continue
            if (line.lowercase().startsWith("name=")) {
                inListHeader = false
            }
            if (inListHeader && line.contains("=")) {
                val (k, v) = line.split("=", limit = 2)
                listSettings[k.trim().lowercase()] = v.trim()
            } else if (!inListHeader) {
                trapLines.add(raw)
            }
        }

        val listConfig = TrapListConfig(
            listName = listName,
            enabled = listSettings["enabled"]?.toBoolean() ?: true,
            description = listSettings["description"] ?: listSettings["desc"] ?: "",
            defaultSource = try {
                TrapDefinition.Source.valueOf( (listSettings["source"] ?: "subtitle").uppercase() )
            } catch (_: Exception) { TrapDefinition.Source.SUBTITLE },
            defaultEnabled = listSettings["enabled"]?.toBoolean() ?: true,
            defaultSubtitlePreparation = listSettings["subtitle_preparation"]?.toBoolean(),
            defaultPreparationTime = listSettings["preparation_time"]?.toDoubleOrNull(),
            defaultPreparationColor = listSettings["preparation_color"]?.let { parseColor(it) },
            defaultMainColor = listSettings["main_color"]?.let { parseColor(it) },
            defaultStartSound = listSettings["start_sound"]?.ifBlank { null },
            defaultEndSound = listSettings["end_sound"]?.ifBlank { null },
            defaultPattern = listSettings["pattern"]?.ifBlank { null },
            defaultMatchMode = try {
                TrapDefinition.MatchMode.valueOf( (listSettings["match_mode"] ?: listSettings["matchmode"] ?: "contains").uppercase() )
            } catch (_: Exception) { TrapDefinition.MatchMode.CONTAINS },
            defaultCaseSensitive = listSettings["case_sensitive"]?.equals("true", true) ?: false
        )

        // === Now parse traps from trapLines, applying list defaults if not defined ===
        var currentName: String? = null
        var currentEnabled: Boolean? = null
        var currentSource: TrapDefinition.Source? = null
        var currentSubtitlePrep: Boolean? = null
        var currentStartSound: String? = null
        var currentEndSound: String? = null
        var currentMainColor: Int? = null
        var currentPrepTime: Double? = null
        var currentPrepColor: Int? = null
        var currentPattern: String? = null
        var currentMatchMode: TrapDefinition.MatchMode? = null
        var currentCaseSensitive: Boolean? = null
        val currentEvents = mutableListOf<TrapEvent>()

        fun flush() {
            if (currentName != null) {
                val effectiveEnabled = currentEnabled ?: listConfig.defaultEnabled
                val effectiveSource = currentSource ?: listConfig.defaultSource
                val effectivePrep = currentSubtitlePrep ?: listConfig.defaultSubtitlePreparation
                val effectiveStart = currentStartSound ?: listConfig.defaultStartSound
                val effectiveEnd = currentEndSound ?: listConfig.defaultEndSound

                result.add(
                    TrapDefinition(
                        name = currentName!!,
                        enabled = effectiveEnabled,
                        source = effectiveSource,
                        subtitlePreparation = effectivePrep,
                        startSound = effectiveStart,
                        endSound = effectiveEnd,
                        events = currentEvents.toList(),
                        listName = listName,
                        mainColor = currentMainColor ?: listConfig.defaultMainColor,
                        preparationTime = currentPrepTime ?: listConfig.defaultPreparationTime,
                        preparationColor = currentPrepColor ?: listConfig.defaultPreparationColor,
                        pattern = currentPattern ?: listConfig.defaultPattern ?: currentName!!,  // pattern is required; fallback to name if omitted (but docs/examples always specify it)
                        matchMode = currentMatchMode ?: listConfig.defaultMatchMode,
                        caseSensitive = currentCaseSensitive ?: listConfig.defaultCaseSensitive
                    )
                )
            }
            currentName = null
            currentEnabled = null
            currentSource = null
            currentSubtitlePrep = null
            currentStartSound = null
            currentEndSound = null
            currentMainColor = null
            currentPrepTime = null
            currentPrepColor = null
            currentPattern = null
            currentMatchMode = null
            currentCaseSensitive = null
            currentEvents.clear()
        }

        for (raw in trapLines) {
            val line = raw.trim()
            if (line.isEmpty() || line.startsWith("#")) continue
            val eqIdx = line.indexOf('=')
            // Lines without '=' are trap event continuations (e.g. "  stop falling|10|#FF5500|")
            if (eqIdx < 0) {
                if (currentName != null && (line.contains('|') || line.isNotBlank())) {
                    parseEventsValue(line)?.let { currentEvents.add(it) }
                }
                continue
            }
            val key = line.substring(0, eqIdx).trim().lowercase()
            val value = line.substring(eqIdx + 1).trim()

            when (key) {
                "subtitle_preparation", "subtitlepreparation" -> {
                    currentSubtitlePrep = value.equals("true", true)
                }
                "name" -> {
                    flush()
                    currentName = value
                }
                "enabled" -> currentEnabled = value.equals("true", true)
                "source" -> {
                    currentSource = try {
                        TrapDefinition.Source.valueOf(value.uppercase())
                    } catch (_: Exception) {
                        listConfig.defaultSource
                    }
                }
                "start_sound", "startsound" -> currentStartSound = value.ifBlank { null }
                "end_sound", "endsound" -> currentEndSound = value.ifBlank { null }
                "main_color", "maincolor" -> currentMainColor = parseColor(value)
                "preparation_time", "preparationtime", "prep_time" -> currentPrepTime = value.toDoubleOrNull()
                "preparation_color", "preparationcolor", "prep_color" -> currentPrepColor = parseColor(value)
                "pattern", "patterns" -> currentPattern = value.ifBlank { null }
                "match_mode", "matchmode", "mode" -> {
                    currentMatchMode = try {
                        TrapDefinition.MatchMode.valueOf(value.uppercase())
                    } catch (_: Exception) {
                        listConfig.defaultMatchMode
                    }
                }
                "case_sensitive", "casesensitive", "case" -> currentCaseSensitive = value.equals("true", true)
                "events" -> {
                    parseEventsValue(value)?.let { currentEvents.add(it) }
                }
                else -> {
                    if (currentName != null) {
                        val candidate = line.trimStart()
                        // event continuation: looks like "label|time..." or starts with whitespace or contains |
                        if (candidate.contains('|') || candidate.isNotBlank()) {
                            parseEventsValue(candidate)?.let { currentEvents.add(it) }
                        }
                    }
                }
            }
        }
        flush()
        return listConfig to result
    }

    private fun parseEventsValue(v: String): TrapEvent? {
        // Supported formats:
        //   eventname|time|color(optional)|sound(optional)   <-- documented & used in examples
        //   time|eventname|color|sound                       <-- legacy compat
        // color not provided -> use trap's mainColor (resolved at load or runtime)
        if (v.isBlank()) return null
        val parts = v.split("|").map { it.trim() }
        if (parts.isEmpty()) return null

        val p0 = parts.getOrNull(0) ?: ""
        val p1 = parts.getOrNull(1) ?: ""
        val p2 = parts.getOrNull(2)
        val p3 = parts.getOrNull(3)

        val off: Double
        val label: String
        val colorStr: String?
        val snd: String?

        val p0AsTime = p0.toDoubleOrNull()
        if (p0AsTime != null && p1.isNotEmpty() && p1.toDoubleOrNull() == null) {
            // legacy: time|label|color|sound
            off = p0AsTime
            label = p1
            colorStr = p2
            snd = p3?.ifBlank { null }
        } else {
            // standard: label|time|color|sound
            label = p0
            off = p1.toDoubleOrNull() ?: 0.0
            colorStr = p2
            snd = p3?.ifBlank { null }
        }

        if (label.isBlank() && off == 0.0 && parts.size < 2) return null
        val color = if (colorStr.isNullOrBlank()) null else parseColor(colorStr)
        return TrapEvent(offsetSeconds = off, label = label, color = color, sound = snd)
    }

    private fun parseColor(s: String): Int {
        var t = s.trim().removePrefix("#").removePrefix("0x").removePrefix("0X")
        if (t.length == 3) t = t.map { "$it$it" }.joinToString("")
        return try {
            t.toInt(16) or 0xFF000000.toInt()
        } catch (_: Exception) {
            0xFFFFFFFF.toInt()
        }
    }

    /**
     * Save the global config back (used after HUD edit or command changes).
     */
    fun save(global: GlobalConfig) {
        ensureConfigExists()
        try {
            overallConfigPath.writeText(global.toSaveString(), Charsets.UTF_8, StandardOpenOption.TRUNCATE_EXISTING)
        } catch (_: Exception) {
            // ignore for now, or log
        }
    }

    /**
     * Save a specific trap list (future use for in-game trap editing).
     */
    fun saveTrapList(listName: String, traps: List<TrapDefinition>, listConfig: TrapListConfig? = null) {
        val p = getTrapListPath(listName)
        if (!p.parent.exists()) p.parent.createDirectories()

        val sb = StringBuilder()
        sb.appendLine("# HITWtimer 陷阱列表: $listName")
        sb.appendLine("# 由可视化配置界面保存 - 手动编辑请保持格式一致")
        sb.appendLine()

        // Serialize traplistconfig header
        val cfg = listConfig ?: loadListConfig(listName) ?: TrapListConfig(listName)
        sb.appendLine("# --- traplistconfig: 列表状态和陷阱默认值 ---")
        sb.appendLine("enabled=${cfg.enabled}")
        if (cfg.description.isNotBlank()) {
            sb.appendLine("description=${cfg.description}")
        }
        sb.appendLine("source=${cfg.defaultSource.name}")
        sb.appendLine("match_mode=${cfg.defaultMatchMode.name}")
        sb.appendLine("case_sensitive=${cfg.defaultCaseSensitive}")
        cfg.defaultPattern?.let { sb.appendLine("pattern=$it") }
        cfg.defaultSubtitlePreparation?.let { sb.appendLine("subtitle_preparation=$it") }
        cfg.defaultPreparationTime?.let { sb.appendLine("preparation_time=$it") }
        cfg.defaultPreparationColor?.let { sb.appendLine("preparation_color=#${colorToHex(it)}") }
        cfg.defaultMainColor?.let { sb.appendLine("main_color=#${colorToHex(it)}") }
        cfg.defaultStartSound?.let { sb.appendLine("start_sound=$it") }
        cfg.defaultEndSound?.let { sb.appendLine("end_sound=$it") }
        sb.appendLine()

        for ((i, trap) in traps.withIndex()) {
            if (i > 0) sb.appendLine()
            sb.appendLine("name=${trap.name}")
            sb.appendLine("enabled=${trap.enabled}")
            sb.appendLine("source=${trap.source.name}")
            sb.appendLine("pattern=${trap.pattern}")
            sb.appendLine("match_mode=${trap.matchMode.name}")
            sb.appendLine("case_sensitive=${trap.caseSensitive}")
            trap.subtitlePreparation?.let { sb.appendLine("subtitle_preparation=$it") }
            trap.startSound?.let { sb.appendLine("start_sound=$it") }
            trap.endSound?.let { sb.appendLine("end_sound=$it") }
            trap.mainColor?.let { sb.appendLine("main_color=#${colorToHex(it)}") }
            trap.preparationTime?.let { sb.appendLine("preparation_time=$it") }
            trap.preparationColor?.let { sb.appendLine("preparation_color=#${colorToHex(it)}") }
            sb.appendLine("events=")
            for (event in trap.events) {
                val c = event.color?.let { "#${colorToHex(it)}" } ?: ""
                val s = event.sound ?: ""
                sb.appendLine("  ${event.label}|${formatTime(event.offsetSeconds)}|$c|$s")
            }
        }
        sb.appendLine()
        p.writeText(sb.toString(), Charsets.UTF_8)
    }

    /**
     * Load only the traplistconfig for a specific list (header before traps).
     */
    fun loadListConfig(listName: String): TrapListConfig? {
        val listPath = getTrapListPath(listName)
        if (!listPath.exists()) return null
        val (cfg, _) = parseTrapList(listPath, listName)
        return cfg
    }

    /**
     * Scan config/hitwtimer/ for all *.txt files excluding overallconfig.txt.
     */
    fun loadAllListNames(): List<String> {
        if (!configDir.exists()) return emptyList()
        return configDir.listDirectoryEntries("*.txt")
            .map { it.nameWithoutExtension }
            .filter { it != "overallconfig" }
            .sorted()
    }

    /**
     * Create a new traplist file with the given name, writing a template.
     */
    fun createNewList(name: String): Path {
        val p = getTrapListPath(name)
        if (!p.parent.exists()) p.parent.createDirectories()
        p.writeText(buildTrapListTemplate(name), Charsets.UTF_8)
        return p
    }

    /**
     * Delete a traplist file.
     */
    fun deleteList(listName: String): Boolean {
        val p = getTrapListPath(listName)
        return if (p.exists()) {
            p.deleteExisting()
            true
        } else false
    }

    /**
     * Load only the trap definitions from a specific list.
     */
    fun loadTrapsForList(listName: String): List<TrapDefinition> {
        val listPath = getTrapListPath(listName)
        if (!listPath.exists()) return emptyList()
        val (_, traps) = parseTrapList(listPath, listName)
        return traps
    }

    private fun colorToHex(color: Int): String {
        return String.format("%06X", color and 0xFFFFFF)
    }

    private fun formatTime(seconds: Double): String {
        return if (seconds == seconds.toLong().toDouble()) {
            String.format("%.1f", seconds)
        } else seconds.toString()
    }
}
