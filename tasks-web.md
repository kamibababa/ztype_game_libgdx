# 任务清单：ZType Web 版接入

## Phase 1：工程接入

1. 在 `settings.gradle` 增加 `gwt` 模块。
2. 在根 `build.gradle` 增加 `gwt` 依赖与任务。
3. 新建 `gwt` 模块目录结构（`src/main/java`、`webapp` 等）。

验收：Gradle 能识别 `gwt` 模块。

---

## Phase 2：Web 启动壳

1. 新增 `HtmlLauncher`（GWT EntryPoint）。
2. 新增 GWT module xml 与 `index.html`。
3. 连接 `new ZTypeGame()` 启动流程。

验收：本地可打开页面，游戏加载成功。

---

## Phase 3：运行与验证

1. 增加/确认开发运行任务（superDev 或等价方式）。
2. 编译 Web 产物并本地访问。
3. 验证输入、HUD、暂停/重开、Game Over。

验收：Web 首版达到最小可玩。

---

## Phase 4：回归检查

1. `:desktop:classes` 通过。
2. `:android:assembleDebug` 通过。
3. `:gwt` 相关任务通过。

验收：三端构建互不影响。
