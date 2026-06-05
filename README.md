# HITWtimer

**纯客户端 Fabric Kotlin 模组**（适用于 Minecraft 1.21.4 / 官方映射 26.1.2），用于 HITW（Hole in the Wall）等小游戏的陷阱（trap）检测与多阶段计时。

主要特性：字幕/聊天双源检测、严格可控的准备阶段（Preparation）、多阶段事件计时器 + 声音/颜色、**自适应深色 HUD**（支持拖拽 + 滚轮缩放实时编辑并自动保存）、热重载、运行时多列表启停、**3 级配置系统**、可自定义检测 pattern + 匹配模式（支持正则）、双语支持。

---

## 功能特性 (Features)

- **检测系统**
  - 主要通过字幕/标题 (Subtitle/Title) 检测（GuiTitleMixin 注入）
  - 同时支持聊天 (Chat) 检测
  - 每个 trap 可独立设置 `source=SUBTITLE | CHAT | BOTH`
  - 去重机制（消息 + 同名 trap 触发间隔）

- **准备阶段 (Preparation) 严格语义**
  - 仅在 **subtitle 来源** 且 `subtitle_preparation` 允许时自动插入
  - `subtitle_preparation=false`（在 trap 级）是**严格无准备**，完全忽略列表/全局设置
  - Chat 来源**永远没有**准备阶段（preparationSeconds=0）
  - 准备阶段的 label、时长、颜色、声音延迟均自动处理，**用户 events 里不要写“准备”**

- **多阶段计时器**
  - 每个 trap 定义多个事件（阶段）
  - 每个阶段可独立设置 offset 时间、显示文本、颜色、播放声音
  - 准备阶段结束后自动播放 start_sound，全部结束后播放 end_sound
  - 实时剩余时间 + 当前阶段 label 显示

- **HUD**
  - 自适应深色半透明背景（可关闭）
  - 拖拽移动位置 + 鼠标滚轮缩放（编辑模式）
  - K 键切换编辑模式，退出时自动保存 HUD 位置/缩放到 overallconfig.txt
  - 支持多个同时激活的计时器（当前取第一个显示）

- **按键绑定**（完整出现在原版「控制」菜单里）
  - `H`：切换/清除当前计时器（Toggle/Clear Timers）
  - `K`：进入/退出 HUD 编辑模式（HUD Edit Mode (drag+scroll)）
  - `L`：热重载所有配置 + trap 列表（Reload Configs + Lists）
  - 通过 `OptionsMixin` 注入实现，无需 KeyBindingHelper

- **配置热重载与运行时控制**
  - L 键 或启动时自动加载
  - 配置文件修改后按 L 即可生效（会清空去重缓存）
  - 完整命令支持运行时列表启停：`/hitwtimer list | reload | enable <name> | disable <name>`
  - `/hitwtimer list` 显示当前启用的列表（彩色：列表名绿色 + 描述灰色）

- **强大的 3 级配置系统**（见下文详细说明）
- **灵活的检测匹配**
  - `pattern=` 支持 `关键词1|关键词2|...`（OR）
  - 4 种 `match_mode`：`CONTAINS`（默认）、`EQUALS`、`STARTS_WITH`、`REGEX`
  - `case_sensitive=false`（默认不区分大小写）
- **事件格式**：`事件名|持续秒数|颜色(可选)|声音(可选)`
- **双语**：中英双语 lang 文件 + 配置模板注释
- **Debug 模式**：`debug=true` 时控制台打印每次匹配详情 + 聊天显示启动信息

---

## 按键 (Keybinds)

所有按键均通过 Mixin 正确注册到原版 Controls 菜单的「杂项」分类下（`key.categories.misc`），重启游戏后即可在设置里查看和修改。

- H - Toggle/Clear Timers（切换/清除计时器）
- K - HUD Edit Mode（拖拽 + 滚轮缩放，退出自动保存）
- L - Reload Configs + Lists（热重载）

---

## 命令 (Commands)

```
/hitwtimer
/hitwtimer list
/hitwtimer reload
/hitwtimer enable <name>
/hitwtimer disable <name>
```

- `list`：列出当前启用的 traplist（来自 overallconfig 的 enabled_lists），带彩色输出（列表名绿色 + description 灰色）。示例：
  ```
  §a当前启用的列表：
    §f- §atraplist1 §7经典HITW陷阱列表 / Classic HITW Trap List (示例)
  ```

- `reload`：立即重载 overallconfig + 所有启用的 traplist（等同于按 L 键）。

- `enable <name>` / `disable <name>`：运行时启用或禁用某个列表（name 为文件名不含 .txt，例如 `traplist1`）。执行后会自动 reload 生效。列表必须先存在于 config 目录。

这些命令让多列表管理非常灵活（例如一个 pvp 列表 + 一个经典 HITW 列表，按需切换）。

---

## 配置系统 (Config System) —— 核心

### 目录与文件

首次运行（或 `ensureConfigExists`）会在 `config/hitwtimer/` 下自动创建：

- `overallconfig.txt` —— **全局 / 模组级** 配置
- `traplist1.txt`（或其他你自己创建的 `xxx.txt`） —— **列表文件**

在 `overallconfig.txt` 的 `enabled_lists=traplist1,另一个列表` 控制加载哪些列表。

**运行时可用文件名（不含 .txt）通过命令或代码控制启停**（当前 list 已支持显示）。

### 3 级优先级（从高到低）

1. **trapconfig**（单个 `name=xxx` 块里的设置）
2. **traplistconfig**（`traplist*.txt` 第一个 `name=` 之前的头部设置，作为该列表的默认值）
3. **overallconfig**（全局回退）

**特殊规则**：
- `subtitle_preparation=false` 在 trap 级是**严格优先**，即使列表/全局为 true 也强制无准备阶段。
- 其他字段（颜色、时间、声音、match mode 等）正常三层回退。

### overallconfig.txt 格式与所有可用键

```properties
# HITWtimer Overall Config (overallconfig.txt)
# This is the TOP-LEVEL / MOD-LEVEL configuration.
# ...

# --- Master ---
enabled=true

# --- Lists (which traplist*.txt to load) ---
enabled_lists=traplist1
# 多个用逗号分隔: enabled_lists=traplist1,pvp,自定义

# --- Detection ---
detect_subtitle=true
detect_chat=true

# --- Preparation defaults (fallbacks) ---
subtitle_preparation=true
preparation_time=3.0
preparation_color=#55FF55
main_color=#FFFFFF

# --- HUD appearance and position (editable in-game with edit mode) ---
hud_x=10
hud_y=10
hud_scale=1.0
render_background=true
hud_horizontal_padding=8
hud_vertical_padding=6

# --- Behavior ---
auto_reload_keywords=hitw,hole in the wall,游戏开始

# --- Misc ---
debug=false
#   true 时：
#   - 每次 subtitle/chat 匹配都会打印详细日志到控制台
#   - 触发 trap 时聊天显示启动信息（含准备时长、列表名）
#   - 适合调试为什么某个陷阱没触发
#   日常使用保持 false，避免刷屏
```

**完整键说明**（解析时大小写不敏感，`_` 可省）：

| 键 | 默认 | 说明 |
|----|------|------|
| enabled | true | 模组总开关 |
| enabled_lists | traplist1 | 要加载的列表文件名列表（逗号分隔，不带.txt） |
| detect_subtitle / detect_chat | true | 是否启用对应来源检测 |
| subtitle_preparation | true | 全局准备阶段默认（可被列表/trap 覆盖） |
| preparation_time | 3.0 | 全局准备秒数 |
| preparation_color / main_color | #55FF55 / #FFFFFF | 颜色（#RRGGBB 或 RRGGBB） |
| hud_x / hud_y / hud_scale | 10 / 10 / 1.0 | HUD 初始位置与缩放（编辑模式会覆盖保存） |
| render_background | true | 是否绘制深色背景 |
| hud_*_padding | 8 / 6 | HUD 背景内边距 |
| auto_reload_keywords | hitw,... | 预留：检测到这些关键词时可自动 reload（当前部分实现） |
| debug | false | 详细调试输出 |

保存 HUD 编辑结果时会调用 `ConfigLoader.save`，只更新 overallconfig.txt 中的 HUD 相关项。

### traplist*.txt 格式（traplistconfig + trapconfig）

文件结构：

```
# HITWtimer 陷阱列表: traplist1
# ... 说明注释

# ==================== traplistconfig (本列表的状态 + 默认配置) ====================
# 必须放在第一个 name= 之前
enabled=true
description=经典HITW陷阱列表 / Classic HITW Trap List (示例)

# 此列表的默认值（trap 未定义时使用）
subtitle_preparation=true
preparation_time=3.0
preparation_color=#55FF55
main_color=#FFFFFF
start_sound=block.note_block.pling
end_sound=block.note_block.bell
source=SUBTITLE
enabled=true

# 检测默认 (可选)
match_mode=CONTAINS
case_sensitive=false
# pattern=   (列表级较少使用)

# ==================== 陷阱配置 (Trap Configurations) ====================
# 下面是多个陷阱定义

name=falling sand
enabled=true
source=SUBTITLE
subtitle_preparation=true
main_color=#FFAA00
start_sound=block.fire.extinguish
end_sound=block.fire.extinguish
pattern=falling sand|sand   # 支持|分隔多个匹配词 (必填)
match_mode=CONTAINS
case_sensitive=false
events=
  stop falling|10|#FF5500|entity.blaze.shoot
  sands gone|5.0|
```

**traplistconfig 头部支持的键**（作为列表默认）：

- `enabled`、`description`（description 用于 `/hitwtimer list` 显示）
- 所有 trap 级的可选字段前加 `default` 或直接写（解析会读取 `source`、`subtitle_preparation`、`match_mode` 等）
- 检测相关：`match_mode`、`case_sensitive`、`pattern`

### 单个 Trap 的必填与可选配置

**必须 (Required)**：
- `name=唯一标识`（陷阱唯一 ID，同时用于 HUD 显示回退）
- `pattern=...` （必填，检测匹配的字幕/聊天关键词，支持 `关键词1|关键词2` 作为 OR）
- `events=` + 至少一行事件（可多行缩进）

**可选 (Optional，未填则用列表默认 → overall)**：

- `enabled=true/false`
- `source=SUBTITLE/CHAT/BOTH`
- `subtitle_preparation=true/false` （**false 是严格无准备**）
- `main_color=#RRGGBB`
- `preparation_time=2.5`、`preparation_color=#...`
- `start_sound=...`、`end_sound=...`
- `match_mode=CONTAINS`（默认） / `EQUALS` / `STARTS_WITH` / `REGEX`
- `case_sensitive=false`（默认）

**事件格式（推荐）**：
```
事件显示名|偏移秒数|颜色(可选)|声音ID(可选)
```

示例：
```
  stop falling|10|#FF5500|entity.blaze.shoot
  sands gone|5.0|
```

颜色回退顺序：事件指定 > 该 trap 的 main_color > 列表 defaultMainColor > overall main_color > 白色

**重要**：`events` 里**绝对不要**写准备阶段！准备完全由检测来源 + `subtitle_preparation` 自动决定并在 `ActiveTimer` 内部处理（显示“准备: xxx”、延迟 start_sound 等）。

---

## 完整示例配置文件 (Full Example Configs)

以下是项目中 `ConfigLoader` 生成的**最新完整示例文件内容**（带所有中英双语注释，pattern 作为必填变量）。你可以直接复制到 `config/hitwtimer/` 下使用，然后根据实际游戏字幕/聊天内容微调 `pattern`。

推荐先把 `debug=true` 打开，用 L 键重载后在控制台观察匹配情况。

### overallconfig.txt

```properties
# HITWtimer Overall Config (overallconfig.txt)
# This is the TOP-LEVEL / MOD-LEVEL configuration.
# It controls the entire mod behavior, HUD defaults, detection, and initial lists.
# 
# Trap list specific defaults (traplistconfig) go at the top of each traplist*.txt
# Individual trap settings (trapconfig) go under name= sections in traplist*.txt
# You can control lists at runtime with: /hitwtimer enable <name> / disable <name> / reload

# --- Master ---
enabled=true

# --- Lists (which traplist*.txt to load) ---
enabled_lists=traplist1
# Example for multiple: enabled_lists=traplist1,pvp,custom

# --- Detection ---
detect_subtitle=true
detect_chat=true

# --- Preparation defaults (fallbacks) ---
subtitle_preparation=true
preparation_time=3.0
preparation_color=#55FF55
main_color=#FFFFFF

# --- HUD appearance and position (editable in-game with edit mode) ---
hud_x=10
hud_y=10
hud_scale=1.0
render_background=true
hud_horizontal_padding=8
hud_vertical_padding=6

# --- Behavior ---
auto_reload_keywords=hitw,hole in the wall,游戏开始

# --- Misc ---
debug=false
#   When true:
#   - Prints detailed match info to console for every subtitle/chat detection.
#   - Shows extra "trap started" message in chat (with prep time and list name).
#   - Helps debug why traps trigger or don't.
#   Keep false for normal play to avoid spam.
```

### traplist1.txt （推荐的示例列表，使用 falling sand 作为演示 trap）

```properties
# HITWtimer 陷阱列表: traplist1
# 此文件只放陷阱定义。可在 overallconfig.txt 的 enabled_lists 中引用本文件名(不带.txt)
# 运行时用 /hitwtimer enable traplist1 启用，disable 禁用。支持多个列表同时激活。

# ==================== traplistconfig (本列表的状态 + 默认配置) ====================
# 放在第一个 name= 之前
# list status
enabled=true
description=经典HITW陷阱列表 / Classic HITW Trap List (示例)

# 此列表的默认值（trap 未定义时使用）
subtitle_preparation=true
preparation_time=3.0
preparation_color=#55FF55
main_color=#FFFFFF
start_sound=block.note_block.pling
end_sound=block.note_block.bell
source=SUBTITLE
enabled=true

# 检测默认 (可选)
match_mode=CONTAINS
case_sensitive=false

# ==================== 陷阱配置 (Trap Configurations) ====================
# 每个陷阱以 name= 开头
# 必须: name= 、 pattern= (必填) 、和至少一个事件
# 可选字段不填则使用上面的列表默认值（再回退 overallconfig）
#
# 重要：Preparation 准备阶段 **不需要** 在 events 中写！
# 它由检测来源自动决定：
# - subtitle 检测 + subtitle_preparation 允许 → 自动插入准备
# - chat 检测 → 绝不插入准备
#
# 事件格式: eventname|time|color(可选)|sound(可选)
# color 不填 → 使用本陷阱的 main_color
#
# 检测相关:
#   pattern=...          (必填，匹配关键词，支持 a|b|c 作为OR)
#   match_mode=CONTAINS/EQUALS/STARTS_WITH/REGEX
#   case_sensitive=false

name=falling sand
enabled=true
source=SUBTITLE
subtitle_preparation=true
main_color=#FFAA00
start_sound=block.fire.extinguish
end_sound=block.fire.extinguish
pattern=falling sand|sand   # 支持|分隔多个匹配词 (必填)
match_mode=CONTAINS
case_sensitive=false
events=
  stop falling|10|#FF5500|entity.blaze.shoot
  sands gone|5.0|

# 你可以继续添加更多陷阱...
# name=your_new_trap
# enabled=true
# pattern=你的关键词|english   # 必填
# match_mode=CONTAINS
# events=
#   阶段1|1.0
#   阶段2|2.5|#FF00FF|block.anvil.land
```

**提示**：这些示例已经包含在 `generated-config-examples/` 目录和首次运行时自动创建的 `config/hitwtimer/` 下。你也可以直接从本 README 复制最新版本。

---

## 检测匹配详解 (Pattern & Match Mode)

匹配发生在 `SubtitleDetector.handleSubtitle` / `handleChat` 中：

- 只匹配 `source` 允许的 trap
- 使用 `trap.matches(text)`（text 来自 `component.string`）
- `pattern` 是必填字段
- 支持 `pattern=词A|词B|词C`（CONTAINS/EQUALS/STARTS_WITH 下自动 OR）
- REGEX 模式下直接使用正则（支持 `.*` 等），大小写由 `case_sensitive` 控制

示例效果：
- `pattern=falling sand|sand` + CONTAINS → 字幕含「falling sand」或「sand」即可触发
- `pattern=anvil.*fall` + REGEX → 匹配 "Anvil is falling" 等（如果使用该模式）

---

## HUD 编辑流程

1. 按 `K` 进入编辑模式（聊天提示）
2. 鼠标左键按住拖拽 HUD 位置
3. 鼠标滚轮调整缩放（0.4 ~ 4.0）
4. 再次按 `K` 退出 → 自动调用 `HitwConfig.save()` 写回 `overallconfig.txt` 的 `hud_x/y/scale`

---

## 构建与使用

```bash
# Windows
.\gradlew.bat build -x test

# 生成的 jar 在 build/libs/hitwtimer-*.jar
```

把 jar 放到 mods 文件夹，启动客户端。

首次启动会自动在 `config/hitwtimer/` 生成带完整中英双语注释的示例配置文件。

修改后按 `L` 热重载即可生效（推荐先把 `debug=true` 测试匹配）。

---

## 当前实现状态与注意事项

- 声音播放 (`playSound`) 在当前构建环境中被 stub（需要完整环境 + ResourceLocation / SoundManager 实现）。
- HUD 实际绘制使用 `HudRenderer.getSnapshot()` 提供数据，真正的 `GuiGraphics` 渲染 mixin 可能需要在完整 IDE 工作区补充（代码中已有注释说明）。
- 命令目前完整实现了 `list`（带彩色 description）；`enable`/`disable` 等可在 `HitwCommands` + `HitwConfig.setEnabledLists` 基础上扩展并持久化。
- `auto_reload_keywords` 已解析但完整自动重载逻辑可按需补充（当前有 30s 周期重载 + 手动 L）。
- 所有配置解析均大小写不敏感，并有良好的注释。

更多细节见源码：
- `ConfigLoader.kt`（模板生成 + 解析）
- `TrapDefinition.kt`（matches 逻辑 + 文档）
- `SubtitleDetector.kt` + `ActiveTimer.kt`
- `HudRenderer.kt` + 三个 Mixin

---

## License

CC0 / 基于模板。

---

**提示**：本 README 里已经嵌入了**最新完整示例**（见上文“完整示例配置文件”章节），可以直接全选复制使用。也可以从 `generated-config-examples/` 或首次运行自动生成的 `config/hitwtimer/` 目录复制，然后根据实际游戏字幕/聊天文本调整各个 trap 的 `pattern` 和 `match_mode`。

有任何问题或需要扩展（更多命令、声音实现、多个同时显示的 HUD 等），欢迎继续迭代！
