# 任务清单：ZType 统一竖屏改造（PC 先行）

## 目标

在保持 `core` 一套逻辑的前提下，先将 PC 版从横屏改为大竖屏并验证可玩，再接入 Android 手机版，最终实现跨端统一竖屏布局与高复用输入逻辑。

---

## 执行顺序（已确认）

1. 先做 PC 版横屏 -> 竖屏改造（优先）。
2. PC 竖屏稳定后，再新增 Android 手机版。

理由：先在 desktop 快速迭代布局与交互，能更低成本发现问题，再迁移到 mobile，整体风险更低。

---

## Phase 1：PC 竖屏化（先做）

### Task 1.1 调整桌面窗口为大竖屏

- 文件：`desktop/src/main/java/com/ztype/game/DesktopLauncher.java`
- 目标：窗口改为竖屏比例，建议 `900x1400`。
- 验收：桌面启动后为竖屏窗口，帧率与现有启动行为正常。

### Task 1.2 核心逻辑世界改为竖屏比例

- 文件：`core/src/main/java/com/ztype/game/ZTypeGame.java`
- 目标：调整 `WORLD_WIDTH/WORLD_HEIGHT` 为竖屏逻辑尺寸。
- 验收：敌人生成、危险线、HUD 显示在竖屏下完整且不重叠。

### Task 1.3 适配竖屏下的游戏区与HUD

- 文件：`core/src/main/java/com/ztype/game/ZTypeGame.java`
- 目标：重排状态栏、目标词、Game Over 文案位置与字号。
- 验收：PC 竖屏下信息可读，无明显遮挡。

### Task 1.4 预留底部键盘区布局槽位

- 文件：`core/src/main/java/com/ztype/game/ZTypeGame.java`
- 目标：在逻辑布局上预留底部输入区高度（即使先不画完整键盘）。
- 验收：敌人活动区域与底部输入区域视觉分离。

### Task 1.5 回归 PC 键盘玩法

- 文件：`core/src/main/java/com/ztype/game/ZTypeGame.java`
- 目标：确保 `onLetterInput(char)` 路径不受竖屏改造影响。
- 验收：桌面键盘输入可锁定目标、逐字命中、计分与掉命逻辑正常。

---

## Phase 2：触屏字母输入接入（仍在 core）

### Task 2.1 定义触屏键位数据结构

- 文件：`core/src/main/java/com/ztype/game/ZTypeGame.java`
- 目标：建立 26 字母 3 行键位模型（QWERTY）。
- 验收：键位可被渲染并具备可点击边界。

### Task 2.2 实现触点命中与字符映射

- 文件：`core/src/main/java/com/ztype/game/ZTypeGame.java`
- 目标：点击键位后统一调用 `onLetterInput(char)`。
- 验收：鼠标点击（PC 模拟触屏）可触发等效字母输入。

### Task 2.3 增加功能键（Pause/Restart）

- 文件：`core/src/main/java/com/ztype/game/ZTypeGame.java`
- 目标：`Pause` 单键切换 `Pause/Resume`，`Restart` 重开。
- 验收：运行中可暂停继续；游戏结束后可重开。

---

## Phase 3：Android 模块接入

### Task 3.1 新增 Android 平台模块

- 文件：`settings.gradle`、`build.gradle`、`android/...`
- 目标：添加 Android 启动工程与依赖配置。
- 验收：Gradle 能识别 `android` 模块并成功构建。

### Task 3.2 Android 启动与竖屏配置

- 文件：`android/src/.../AndroidLauncher.*`、`AndroidManifest.xml`
- 目标：游戏可在手机竖屏启动，进入主界面。
- 验收：真机或模拟器可安装并启动。

### Task 3.3 移动端触屏体验微调

- 文件：`core/src/main/java/com/ztype/game/ZTypeGame.java`
- 目标：调整键高、间距、字体，减少误触。
- 验收：手机上 26 键可连续点击输入，体验可用。

---

## 风险与检查点

1. Android Gradle/SDK 兼容：若阻塞，先保证 Phase 1~2 可交付。
2. 竖屏宽度变窄：检查敌人单词可读性与碰撞密度。
3. 触控误触：优先保证键位尺寸与间距。

---

## Definition of Done（本轮完成标准）

1. PC 版已切换为 `900x1400` 大竖屏并可完整游玩。
2. `core` 内已具备底部 26 键触屏输入，调用统一 `onLetterInput(char)`。
3. `Pause/Resume` 与 `Restart` 在竖屏布局下可用。
4. Android 版可启动、可通过触屏键盘游玩基础回合。

---

## 本次已确认参数

- 首版仅支持竖屏。
- PC 窗口建议：`900x1400`。
- `Pause`：单键切换 `Pause/Resume`。
- 首版不加入 `Back` 回退键。
