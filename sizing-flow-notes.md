# ZType 尺寸与适配说明（PC + 移动端）

这份文档用于解释当前项目里“窗口大小 / 逻辑世界 / 渲染像素”三套尺寸是怎么协同工作的。

---

## 1. 三套尺寸概念

项目里有三套不同层面的“宽高”：

1. 逻辑世界尺寸（你定义）
   - `WORLD_WIDTH` / `WORLD_HEIGHT`
   - 位置：`core/src/main/java/com/ztype/game/ZTypeGame.java`
   - 作用：作为游戏内统一坐标系（敌人、HUD、按钮都按它计算）。

2. 窗口尺寸（平台层）
   - desktop 使用 `setWindowedMode(width, height)`
   - 位置：`desktop/src/main/java/com/ztype/game/DesktopLauncher.java`
   - 作用：决定系统窗口有多大。

3. BackBuffer 尺寸（框架运行时给出）
   - `Gdx.graphics.getBackBufferWidth()/getBackBufferHeight()`
   - 作用：真正 OpenGL 渲染的像素尺寸。
   - 注意：高 DPI/缩放下，常与窗口尺寸不相等。

---

## 2. PC 端链路（Desktop）

### 2.1 启动器决定窗口大小

文件：`desktop/src/main/java/com/ztype/game/DesktopLauncher.java`

当前逻辑：

- 读取显示器分辨率 `displayMode`
- 计算安全高度 `safeHeight = max(900, displayMode.height - 120)`
- 目标窗口高度 `windowHeight = min(1400, safeHeight)`
- 按竖屏比例 `900/1400` 计算 `windowWidth`
- `setWindowedMode(windowWidth, windowHeight)`
- `setWindowPosition(...)` 居中
- `setHdpiMode(HdpiMode.Pixels)` 避免高 DPI 首帧错位/裁切

### 2.2 core 用固定逻辑世界

文件：`core/src/main/java/com/ztype/game/ZTypeGame.java`

- PC 端使用 `FitViewport(WORLD_WIDTH, WORLD_HEIGHT, camera)`
- 这会把逻辑世界完整映射到当前窗口，按比例缩放，不裁切内容。

### 2.3 resize 用 backbuffer 更新

- `resize()` 中用 `getBackBufferWidth/Height` 调 `viewport.update(...)`
- 目的是在高 DPI 场景下用真实渲染像素更新视口，避免初始显示异常。

---

## 3. 移动端链路（Android）

### 3.1 Android 不设窗口大小

文件：`android/src/main/java/com/ztype/game/android/AndroidLauncher.java`

- `initialize(new ZTypeGame(), config)` 启动
- 不存在 `setWindowedMode`（手机全屏由系统管理）

### 3.2 core 判断触屏平台

文件：`core/src/main/java/com/ztype/game/ZTypeGame.java`

- `showTouchKeyboard = isTouchPlatform()`
- Android / iOS 返回 `true`

### 3.3 计算移动端逻辑高度 worldHeight

- 先取基准 `WORLD_HEIGHT`
- 触屏平台下：
  - `aspect = screenHeight / screenWidth`
  - `worldHeight = max(WORLD_HEIGHT, WORLD_WIDTH * aspect)`

作用：

- 保持横向基准宽度 900
- 对长屏设备增加逻辑高度，减少上下空区
- 同时避免 `FillViewport` 带来的左右裁切

### 3.4 移动端 viewport

- 使用 `FitViewport(WORLD_WIDTH, worldHeight, camera)`
- 保证内容不被左右裁掉（HUD、按键边缘可见）

### 3.5 键盘区与游戏区分离

- `KEYBOARD_AREA_HEIGHT` 定义底部触屏区高度
- `enemyMinY = DANGER_LINE_Y + KEYBOARD_AREA_HEIGHT`
- 敌人到该线以下会判失败，避免压住键盘区

### 3.6 触摸坐标转换

- `touchDown` 时用 `viewport.unproject(...)`
- 把屏幕像素触点转换为逻辑世界坐标
- 再做按钮命中判定（与游戏世界统一坐标）

---

## 4. 哪些是你提供，哪些是框架自动给

你提供：

- `WORLD_WIDTH/WORLD_HEIGHT`
- desktop 的窗口目标尺寸策略（`setWindowedMode`）
- 何时调用 `viewport.update`（通常在 `resize`）

框架提供/计算：

- 当前窗口尺寸（`Gdx.graphics.getWidth/Height`）
- 当前 backbuffer 像素尺寸（`getBackBufferWidth/Height`）
- `FitViewport` 内部缩放、留边、相机可视范围计算

---

## 5. 当前代码为什么看起来“复杂”

因为同时满足了这些目标：

1. PC + Android + Web 复用一套 core
2. 竖屏设计基准
3. 高 DPI / 多分辨率设备可用
4. 触屏键盘与游戏区不冲突

如果只做单平台，确实可以删掉不少适配层；但跨平台下这属于必要复杂度。
