# Lunary

Lunary（月忆）是一个面向 Android 平板、离线优先的私人纪念日与人生事件管理器。界面使用简体中文与 Material 3（Material You），数据全部保存在本机，无需联网即可完成全部核心操作。

当前进度：**阶段 0–6 已完成硬化；阶段 5 的本地加密备份/恢复与定时自动备份已完成并验证，Google Drive 云备份仍待 OAuth 客户端凭据接入。**

- 隐私说明：[PRIVACY.md](PRIVACY.md)
- 发布检查与已知阻塞项：[RELEASE.md](RELEASE.md)

## 技术栈

- Kotlin 2.4.10（AGP 9 内置 Kotlin 编译）
- Jetpack Compose + Material 3，Compose BOM 2026.09.00
- Navigation Compose、Hilt、Room、DataStore、KSP
- AGP 9.5.1 / Gradle 9.8.2 / JDK 工具链 25
- minSdk 26、targetSdk 37、applicationId `com.lunary.app`

## 已实现（阶段 0–5）

- **月历主页**：42 格月历、周起始可配置、左右翻月、回到今天、选中日期事项列表、事件色点（同日最多 3 个）。
- **月 / 周 / 日视图**：顶部分段按钮切换；周视图显示一周七天与事件标记，日视图突出当天与事项；前一天/后一天、上一周/下一周、上一月/下一月导航；“今天”一键回到当前日期。
- **自适应双栏**：宽度 ≥760dp 且高度 ≥560dp（如平板横屏）时，左侧日历、右侧当天事项并排显示；手机横屏等紧凑高度下保持单列滚动，避免月份被裁切。
- **事项列表**：搜索标题/备注/地点，全部/未来/过去快捷筛选；高级筛选支持分类、标签（多选 OR）、地点有无，组合生效；按最近未来优先排序，显示分类与间隔天数。
- **事项详情**：完整日期、间隔、分类、标签、备注、地点，支持编辑与删除确认。
- **提醒**：可为同一事项添加多个提前提醒（当天 / 1 天 / 3 天 / 7 天），每条提醒可自定义时间（默认 09:00），支持点按编辑、启用开关与删除；使用 AlarmManager 调度，优先精确闹钟、未授权自动降级，设备重启/改时间/改时区后自动重排；通知点击可直达事项详情。
- **附件**：从系统照片选择器批量导入图片/视频（单次最多 20 个），或从文档选择器批量导入音频；文件复制到应用私有目录并记录 SHA-256、大小、MIME；详情页显示缩略图/图标；点击附件在内置全屏查看器中显示，可左右滑动切换全部附件，图片按屏幕等比放大，视频/音频内置播放并支持暂停、进度拖动，删除事项时同步清理文件。
- **通知权限**：Android 13+ 首次添加提醒时显示用途说明并请求通知权限；拒绝通知不影响提醒数据与事项保存。
- **新建/编辑事项**：标题（120 字素上限与计数）、日期选择、仅月日模式、每年重复、分类（自动套用分类颜色）、标签多选、颜色色板、备注、地点。
- **分类与标签管理**：新建、编辑（名称/颜色/emoji）、软删除；支持**拖拽排序**并持久化顺序；删除分类时事项自动变为未分类，并给出确认提示。
- **本地加密备份**：导出/导入 `.lunary` 文件，包含事项、分类、标签、提醒和附件；PBKDF2-HMAC-SHA256（210,000 次）+ AES-256-GCM，文件带版本头与 SHA-256 完整性清单；备份使用 v2 流式加密容器（`LUNARYBK2`，分块读取/写入，不再受 60/40 MB 上限约束）；恢复前自动生成本机快照（`auto_backups`，保留 5 份）；密码错误或文件损坏时不修改本机数据。
- **回收站**：删除事项先进入回收站，保留 30 天后自动清理；支持恢复、彻底删除、清空回收站；启动时自动清理过期条目。
- **主题**：完整 Material 3 浅/深色 `ColorScheme`、`Typography` 与 `Shapes`；Android 12+ 动态取色，关闭时回退 Lunary 紫色静态主题。
- **设置**：按“外观 / 日历 / 提醒 / 隐私与安全 / 数据与备份 / 分类与标签 / 帮助与关于”分组，说明文字与控件分层；手机单栏、宽屏（≥840dp）左侧分类导航 + 右侧内容（最大 720dp）；支持深色、1.5× 字体和窄窗口；精确提醒、应用锁、自动备份等原有行为不变；帮助与隐私说明以对话框呈现。
- **应用锁**：可选 6–12 位数字 PIN，PBKDF2-HMAC-SHA256（210,000 次）加随机盐，密文由 Android Keystore 密钥加密；支持修改/关闭 PIN、立即/1/5/15 分钟自动锁定；可用时提供生物识别解锁，并回退设备凭证。
- **隐私**：锁定期间可隐藏通知中的事项标题，仅显示“Lunary 提醒”。
- **无障碍**：日历与周视图日期格提供“日期 + 事项数”语义与选中状态，点击区域不小于 48dp；支持系统字体缩放，锁定页与表单在大字号下不截断。
- **数据层**：Room 单库 `lunary.db`（事件、分类、标签、多对多关联、提醒、附件、同步队列 7 张表，schema 导出于 `app/schemas/`），DataStore 偏好，Hilt 注入，Repository 封装，软删除。
- **自适应框架**：窄屏底部导航，≥720dp 使用 NavigationRail。
- **测试**：`DayMathTest`（闰日策略、重复、间隔、周年）、`CalendarBoundaryTest`（周起始、跨年、42 格月历、闰日、生日边界）、`TextLimitsTest`（字素截断/计数）。

阶段 1 验收结果：杀进程重开后事件仍在；飞行模式下可完成创建、查看、编辑、删除。

阶段 2 验收结果：月/周/日可切换，日期前后导航正确；平板宽度显示双栏，手机横竖屏均可滚动浏览；`CalendarBoundaryTest` 覆盖周起始、跨年、42 格月历与闰日边界；`assembleDebug`、`lintDebug`、`testDebugUnitTest` 均通过。

阶段 3 验收结果：提醒可按事件日期与提前量计算下次触发（`dumpsys alarm` 验证）；通知可展示并点击跳转事项详情；附件复制到私有目录后在杀进程重启后仍可访问，删除附件/清空回收站会同步删除文件；`assembleDebug`、`assembleRelease`、`lintDebug`、`testDebugUnitTest` 均通过。

阶段 4 验收结果：浅/深色与动态取色可用；应用锁 PIN 启用、错误 PIN 拒绝、正确 PIN 解锁、立即锁定均验证；锁定并开启隐藏通知后，提醒通知只显示“Lunary 提醒”；日历日期格 TalkBack 语义与 1.5× 字体缩放验证；模拟器无已录入生物识别，生物识别解锁未能实测。`assembleDebug`、`assembleRelease`、`lintDebug`、`testDebugUnitTest` 均通过。

阶段 5 验收结果（本地部分）：导出包含附件与提醒的备份（约 90 KB，含 86 KB 图片）；导入预览显示事项/分类/标签/提醒/附件数量；恢复后附件文件与事项关联完整；错误密码被拒绝且原有数据未变；恢复前自动快照写入 `files/auto_backups`。v2 备份取消应用层大小上限，实际仅受目标文件系统/剩余空间限制；新建的 `lunary-auto-*.lunary` 已验证包含附件。

Google Drive 云备份尚未接入：需要 Google Cloud 项目中创建 Android OAuth 客户端（包名 `com.lunary.app` + 本机签名 SHA-1），并实现 Drive API v3 `drive.appdata` 上传/列出/下载。未配置时界面会明确显示“尚未接入”，不会伪装成可用。

### 权限说明

- `POST_NOTIFICATIONS`（Android 13+）：用于提醒通知，可拒绝。
- `SCHEDULE_EXACT_ALARM`：优先使用精确闹钟；未授权时自动降级为非精确闹钟（可能延迟，不会丢失）。
- `RECEIVE_BOOT_COMPLETED`：重启后恢复提醒调度。
- 附件使用应用私有 `filesDir/attachments/`，通过 `FileProvider` 生成内部可访问 URI，供内置查看器播放；不支持预览时仍可临时授权外部应用打开。
- 设置页提供「精确提醒权限」入口，可跳转系统授权页；未授权时自动使用非精确闹钟。

## 构建与运行

1. 用 Android Studio 打开项目根目录（首次需等待 Gradle Sync）。
2. 选择 `app` 配置，连接 Android 8.0（API 26）以上设备或模拟器运行。
3. 命令行：

```bash
./gradlew :app:assembleDebug
./gradlew :app:testDebugUnitTest
./gradlew :app:lintDebug
```

产物：`app/build/outputs/apk/debug/app-debug.apk`（调试）与 `app/build/outputs/apk/release/app-release.apk`（本机签名 release；仓库不提交 keystore）。

> 若 Android Studio 的 Make Project 报找不到 `:app:unitTestClasses`，执行一次
> **File → Sync Project with Gradle Files**；本项目在 `app/build.gradle.kts` 末尾为
> AGP 9 注册了 IDE 兼容别名任务。

## 目录结构

```
app/src/main/java/com/lunary/app/
├── LunaryApplication.kt / MainActivity.kt
├── data/
│   ├── local/（Room 实体、DAO、数据库与种子数据）
│   ├── repository/（映射与实现）
│   └── settings/（DataStore）
├── di/（Hilt 模块）
├── domain/（模型、日期逻辑、字素工具、仓库接口）
└── ui/
    ├── LunaryApp.kt（导航与自适应外壳）
    ├── calendar/ events/ detail/ editor/ settings/ taxonomy/
    ├── components/ 与 UiFormat.kt
    └── theme/（动态色 + Lunary 紫色静态回退）
```

## 阶段 6 硬化结果

- 新增 Room 仪器测试 `LunaryDatabaseTest`：软删除/恢复、永久删除级联清理标签/提醒/附件、清除分类后事件变为未分类。
- 新增 Compose UI 测试 `MonthGridTest`：验证日期格向 TalkBack 暴露“日期 + 事项数”语义。
- 新增可选 release 签名配置：检测到 `keystore.properties` 时自动签名，否则产出未签名包，不使用调试密钥。
- 新增 `PRIVACY.md`（数据、权限、加密、删除说明）与 `RELEASE.md`（签名、Play 清单、阻塞项）。
- CI 增加 Gradle Wrapper 校验、release 构建、单元测试、Lint 与报告上传。
- 版本提升到 0.6.7（versionCode 14）。

## 0.6.7 设置页重整

- 信息结构固定为 7 组：外观、日历、提醒、隐私与安全、数据与备份、分类与标签、帮助与关于；组标题使用 `titleSmall`，设置名称、辅助说明、控件/状态按固定顺序排列。
- 复用 `SettingsSection`、`SettingsChoiceRow<T>`、`SettingsSwitchRow`、`SettingsNavigationRow`、`SettingsDivider`；开关整行可点击并提供 Switch 语义，导航行提供 Button 语义和 56dp 最小高度。
- 手机/窄窗口单栏最大 680dp；扩展窗口使用 232dp 分类导航 + 最大 720dp 内容；横屏与分屏窗口保持可滚动，不横向溢出。
- 精确提醒改为“名称 + 状态 + 单行说明”的导航行；应用锁开启后才显示修改 PIN、自动锁定、生物识别和隐藏通知内容；数据/分类/标签入口补充简短说明。
- 帮助与关于新增“使用帮助”“隐私说明”对话框、版本与版权信息。
- 自动备份子页统一为最大 720dp 居中布局：启用、备份计划、备份密码三块卡片均满宽；频率、执行时间、备份文件夹用分隔线组织，按钮和说明不再出现半宽模块。
- 验证：`assembleDebug`、`lintDebug`、`testDebugUnitTest`（26 个）、`assembleRelease`、`connectedDebugAndroidTest`（6 个）均通过；在 411dp 手机、1000dp 平板双栏、横屏、267dp 窄窗口、深色主题和 1.5× 字体下检查，未发现横向溢出或控件重叠。release APK 证书未变（SHA-1 `8f817bd306d8acc2547f038d3e3617b8925ea035`）。


## 0.6.8 纪念日详情页重整

- 详情页统一为最大 720dp 居中单栏，顶部为事件色 Hero、日期与时间说明，分类/标签用只读标签展示。
- 复用 `DetailSection`、`DetailTag` 与统一的分隔线、卡片和 20dp 组间距；提醒、附件各自成组，删除与添加动作位置固定。
- 提醒/附件行改为最小 64dp 的整行结构，附件使用缩略图、两行文件名和独立删除按钮；删除入口使用 error 色。
- 窄窗口标题自动缩写为“详情”，宽屏保留“纪念日详情”；保留编辑、删除、提醒权限、附件选择/打开/删除和地图预览等全部行为。

## 0.6.9 新建/编辑页重整

- 编辑器统一为最大 720dp 居中单栏；“标题”作为主输入，日期与重复、分类与标签、颜色、备注与地点、附件各自成组。
- 复用 `EditorSection`、`EditorSwitchRow`、`EditorColorSwatch`、`EditorSubLabel`；日期行和开关整行可点击，开关使用 Switch 语义，颜色选项使用 48dp 触控目标与 RadioButton 选中语义。
- 颜色面板改为可换行布局，窄屏不再横向溢出；附件按钮改为可换行 tonal 按钮，附件行提供两行文件名、大小和独立删除按钮。
- 顶栏在窄窗口缩写为“新建/编辑”，保存禁用态、标题 120 字限制、日期选择、每年重复、分类/标签/颜色、备注/地点/地图、附件最多 20 个、放弃修改确认与保存流程保持不变。
- 验证：`lintDebug`、`testDebugUnitTest`（26 个）、`connectedDebugAndroidTest`（6 个）、`assembleRelease` 均通过；在 411dp 手机、1000dp 平板、267dp 窄窗口、深色主题和 1.5× 字体下检查，无横向溢出或控件重叠。
- 已签名 `release/Lunary-0.6.9.apk`，SHA-256 `8e26bbc0c33a0ad62ac392c111d8d409e2edcf3d26c3f509e73ca3ca7a83c516`；证书 SHA-1 仍为 `8f817bd306d8acc2547f038d3e3617b8925ea035`。
- 截图：`release/editor-phone-0.6.9.png`、`release/editor-tablet-0.6.9.png`、`release/editor-dark-0.6.9.png`、`release/editor-narrow-0.6.9.png`。

## 0.7.0 详情层级与附件画廊

- 新建/编辑纪念日：移除“分类与标签”卡片内多余的“分类”子标题，分类选项卡直接作为卡片内容；标签子标题仅在存在标签时显示。
- 纪念日详情：以“详情”为一级标题，下面以“备注 / 提醒 / 附件”三个二级分区组织内容；备注分区内含地点与地图，提醒分区保留添加入口。
- 附件改为横向画廊：固定宽度卡片、缩略图/播放图标、文件名、大小与删除按钮，可左右滑动查看。
- 详情页、编辑页继续沿用 720dp 最大内容宽度、Material 3 组件、48dp 触控目标和浅色/深色/大字体适配。
- 已签名 `release/Lunary-0.7.0.apk`，SHA-256 `ce3599f63a514ba21a9d832d99ba513435188729ed1e56f74228989a12cd5e51`；证书 SHA-1 仍为 `8f817bd306d8acc2547f038d3e3617b8925ea035`。
- 截图：`release/detail-hierarchy-0.7.0.png`、`release/detail-gallery-0.7.0.png`、`release/detail-hierarchy-tablet-0.7.0.png`、`release/detail-hierarchy-dark-0.7.0.png`、`release/detail-hierarchy-narrow-0.7.0.png`、`release/editor-category-0.7.0.png`。


## 0.8.0 提醒面板重整

- 纪念日详情右上角新增闹钟按钮（编辑左侧），有生效提醒时显示主色圆底和数量角标；详情页不再占用版面显示提醒列表。
- 提醒设置改为完整面板：新提醒默认时间、50/100/一年纪念日提醒、提前当天/1/3/7 天提醒，可同时启用多个。
- 支持未来与已过日期的纪念日；负偏移里程碑从纪念日起算，后续可继续扩展。
- 已签名 `release/Lunary-0.8.0.apk`，SHA-256 `9e98e703eeeae39995b09dd26a9925ad592aa9f6b8b5430883cbabe53c312ffc`；证书 SHA-1 仍为 `8f817bd306d8acc2547f038d3e3617b8925ea035`；面板在手机、平板、窄屏、深色和大字体下验证。
- 截图：`release/reminder-panel-0.8.0.png`、`release/reminder-panel-rows-0.8.0.png`、`release/reminder-panel-tablet-0.8.0.png`、`release/reminder-panel-dark-0.8.0.png`、`release/reminder-panel-narrow-0.8.0.png`、`release/reminder-panel-font15-0.8.0.png`、`release/reminder-active-badge-0.8.0.png`。


## 0.8.1 列表提醒指示

- 列表卡片在事项存在未来可排程的有效提醒时，显示一个 20dp 的闹钟图标，颜色使用主题主色，位置在箭头左侧。
- 指示依据实际调度结果，而不是提醒开关数量；提醒停用或已过期后图标自动消失。
- 闹钟图标带 TalkBack 描述“有生效的提醒”。
- 已签名 `release/Lunary-0.8.1.apk`，SHA-256 `21cdded35ea45bd0b0c6e3f8f0993f1c38869c22a28ba05f3d83e85a514f8c2e`；证书 SHA-1 仍为 `8f817bd306d8acc2547f038d3e3617b8925ea035`。
- 截图：`release/list-icon-0.8.1.png`、`release/list-icon-dark-0.8.1.png`、`release/list-icon-tablet-0.8.1.png`、`release/list-icon-narrow-0.8.1.png`。

## 0.8.4 内置附件查看器
- 详情页附件卡片点击后不再跳出应用，改为全屏 `HorizontalPager` 查看器；从点击的附件开始，左右滑动可切换同一事项的全部附件。
- 图片使用受控采样解码（最长边 2048px）并按 `ContentScale.Fit` 放大显示，避免超大原图导致内存峰值；加载失败显示明确提示。
- 视频使用系统 `VideoView` 内置播放，提供居中的播放/暂停、可拖动进度条和时间标签；音频使用 `MediaPlayer`，提供音符占位、播放/暂停、进度条和时间标签；离开页面或关闭查看器时释放播放器。
- 顶部提供关闭按钮、文件名和“当前/总数”计数，支持系统返回键关闭；查看器固定深色背景和白色控制，适配浅色/深色主题，不额外引入 Media3/ExoPlayer/Coil 依赖。
- 非图片/视频/音频附件提供“暂不支持预览”的说明，并保留“用其他应用打开”的兜底入口。
- `lintDebug`、`testDebugUnitTest`、`connectedDebugAndroidTest`（6 项）、`assembleRelease` 通过。
- 已签名 `release/Lunary-0.8.4.apk`，SHA-256 `c7c6221f537643b1b457ef80d5625041c236b62367b599b6f47437b4d4b098ee`；证书 SHA-1 `8f817bd306d8acc2547f038d3e3617b8925ea035`。
- 截图：`release/attachment-viewer-image-0.8.4.png`、`release/attachment-viewer-video-0.8.4.png`、`release/attachment-viewer-audio-0.8.4.png`、`release/attachment-viewer-font15-0.8.4.png`、`release/attachment-viewer-tablet-0.8.4.png`、`release/attachment-viewer-landscape-0.8.4.png`、`release/attachment-viewer-narrow-0.8.4.png`。

## 0.8.3 Emoji 标签
- Emoji 从标题文本中拆出，持久化为独立 `emoji` 字段；标题、字数、校验、搜索和排序只使用标题文本。
- 列表标题前显示独立 emoji 标签，标签与标题间距 8dp；详情标题前为独立标签，间距 10dp；编辑器标题栏按钮只设置标签，不再改写标题。
- Room `version = 3`，`MIGRATION_2_3` 新增 `emoji TEXT` 并把 0.8.2 及以上版本标题开头的目录 emoji 自动拆出；旧备份缺少 `emoji` 时同样回退拆分，新备份写入可选字段。
- 1.5× 字体缩放下月历单元格高度随 fontScale 增长，农历小字不再被裁切。
- `lintDebug`、`testDebugUnitTest`（32 项）、`connectedDebugAndroidTest`（6 项）、`assembleRelease` 通过。
- 已签名 `release/Lunary-0.8.3.apk`，SHA-256 `d9e2239e88c373f904a8222e38071a8c5bd683125929df5a05b3be34bc9cb9dd`；证书 SHA-1 `8f817bd306d8acc2547f038d3e3617b8925ea035`。
- 截图：`release/emoji-label-0.8.3.png`、`release/emoji-label-editor-0.8.3.png`、`release/emoji-label-dark-0.8.3.png`、`release/emoji-label-narrow-0.8.3.png`、`release/emoji-label-tablet-0.8.3.png`、`release/emoji-label-font15-0.8.3.png`、`release/calendar-font15-0.8.3.png`。

## 0.8.2 标题 Emoji

- 添加/编辑纪念日时，标题栏开头提供可选 emoji 按钮；从 48 个常用 emoji 中单选一个，保存后作为标题的第一个字符显示在列表与详情中。
- 同一标题最多保留一个通过选择器添加的 emoji；再次选择会替换，选择“移除 emoji”会只移除开头 emoji，不改变其余标题文字。
- 标题仍限制 120 个 grapheme；emoji 单独存在时不能保存，必须补充文字。
- 新增 `EmojiCatalog` 纯逻辑单元测试，覆盖开头 emoji 提取、移除与单 grapheme 约束。
- 已签名 `release/Lunary-0.8.2.apk`，SHA-256 `d08f07bb1507de936b3c085ce276c8c0d01b741d97eb0ed28f922c781b8f141e`；证书 SHA-1 仍为 `8f817bd306d8acc2547f038d3e3617b8925ea035`；验收覆盖手机、窄屏、深色和宽屏。
- 截图：`release/emoji-title-0.8.2.png`、`release/emoji-picker-0.8.2.png`、`release/emoji-picker-dark-0.8.2.png`、`release/emoji-picker-narrow-0.8.2.png`、`release/emoji-picker-tablet-0.8.2.png`、`release/emoji-editor-narrow-0.8.2.png`。

## 已知限制

- 精确闹钟未授权时为非精确提醒，可能延迟最多约 1 小时。
- 生物识别解锁已在模拟器录入指纹后验证通过；仍未在真实设备验证。
- Google Drive 云备份未接入：需要 Android OAuth 客户端 ID 与 Drive API 实现；备份页会明确显示未配置，不会伪装可用。
- 恢复支持“合并恢复（保留本机数据，备份内容作为“恢复副本”导入）”与“替换本机数据”；v2 备份流式处理，不再有 60/40 MB 上限，但暂不支持断点续传。
- 本地备份密码遗忘后无法恢复。
- **自动备份**：本地文件夹、每天/每周/每月、指定时间；WorkManager 近似执行；保留最近 14 份；自动备份密码使用 Android Keystore 加密保存，换机恢复仍需手动输入同一密码。
- 自动备份目标目录需选择一个子文件夹（Android DocumentsUI 不允许直接选择存储根目录/Download 根目录）。
- Compose UI 测试覆盖月历无障碍语义、底部导航与编辑器标题校验；锁屏 UI 仍待自动化。

## 测试与质量

- 单元测试：`./gradlew :app:testDebugUnitTest`（26 个）
- 仪器测试：`ANDROID_SERIAL=emulator-5554 ./gradlew :app:connectedDebugAndroidTest`（6 个：Room DAO 3 + Compose UI 3）
- Lint：`./gradlew :app:lintDebug`
- Release：`./gradlew :app:assembleRelease`（本机已配置 `keystore.properties`，产出已签名 `app-release.apk`）
- CI：`.github/workflows/android.yml` 校验 wrapper、构建 debug/release、跑单元测试与 Lint，并上传报告。
- 模拟器性能基线：冷启动 2.40s（首次）、1.57s、1.42s；前台 PSS ≈ 85 MB（API 37，中等手机分辨率）。
