# Lunary

Lunary（月忆）是一个离线优先的 Android 私人纪念日与人生事件管理器。界面使用简体中文和 Material 3，核心功能无需账号或网络，数据默认保存在本机。

> An offline-first Android anniversary and milestone manager built with Kotlin, Jetpack Compose, and Material 3.

- 隐私说明：[PRIVACY.md](PRIVACY.md)
- 发布、签名与已知限制：[RELEASE.md](RELEASE.md)

## 功能

### 日历与事件

- **月 / 周 / 日视图**：月历为 42 格，可配置一周起始日、左右翻月、回到今天；周视图与日视图支持前后导航和事件标记。
- **自适应双栏**：平板横屏等宽裕窗口下，左侧日历与右侧当天事项并排；紧凑窗口保持单列滚动。
- **事件列表**：搜索标题、备注和地点；支持全部 / 未来 / 过去筛选，以及按分类、标签、地点有无组合筛选。
- **新建与编辑**：标题（120 字素上限）、emoji、日期、仅月日模式、每年重复、分类、标签、颜色、备注、地点和附件。
- **详情页**：展示日期、间隔、分类、标签、备注、地点、提醒和附件；附件只读浏览，编辑与删除统一在编辑页完成。
- **分类与标签**：新建、编辑、软删除、拖拽排序；删除分类后相关事件自动变为未分类。

### 提醒与附件

- **多条提醒**：同一事件可设置当天 / 1 天 / 3 天 / 7 天提醒，每条可自定义时间；使用 `AlarmManager` 调度，精确闹钟不可用时自动降级。
- **可靠恢复**：设备重启、系统时间或时区变化后自动重排提醒；通知点击可直达事件详情。
- **附件**：支持图片、视频和音频，单个事件最多 20 个；文件复制到应用私有目录，并记录 SHA-256、大小和 MIME。
- **内置查看器**：全屏左右滑动切换附件；图片等比缩放，视频和音频原地播放并支持进度控制。编辑页负责添加和移除附件，详情页只读。

### 隐私与数据

- **应用锁**：6–12 位数字 PIN，支持生物识别解锁、自动锁定，以及锁定时隐藏通知中的事件标题。
- **本地加密备份**：导出 / 导入 `.lunary` 文件，包含事件、分类、标签、提醒和附件；使用 PBKDF2-HMAC-SHA256 与 AES-256-GCM，恢复前自动生成本机快照。
- **自动备份**：本地文件夹、每天 / 每周 / 每月计划，保留最近 14 份；备份密码由 Android Keystore 加密保存。
- **回收站**：删除的事件保留 30 天，可恢复或彻底删除。
- **离线优先**：没有账号系统，核心功能不发起网络请求；附件和数据库都存放在应用私有目录。

### 界面与无障碍

- Material 3 浅色 / 深色主题，Android 12+ 支持动态取色。
- 手机使用底部导航，较宽窗口切换为 `NavigationRail`；设置页在宽屏下为左侧分类导航 + 右侧内容。
- 支持系统字体缩放；交互目标不小于 48dp；日历日期格向 TalkBack 暴露“日期 + 事项数”语义。

## 技术架构

### 技术栈

- Kotlin + Jetpack Compose + Material 3
- Navigation Compose、Hilt、Room、DataStore、KSP
- AGP 9、Gradle 9、JDK 25
- `minSdk 26`、`targetSdk 37`、`applicationId com.lunary.app`

### 分层

```text
app/src/main/java/com/lunary/app/
├── data/        Room、DataStore、仓库实现、附件存储、备份与提醒调度
├── domain/      领域模型、日期逻辑、字素工具、仓库接口
├── di/          Hilt 模块
└── ui/          Compose 页面、ViewModel、导航与主题
```

- **UI 层**：Compose 声明式界面，ViewModel 持有状态并通过 `StateFlow` 暴露；导航和自适应外壳集中在 `LunaryApp.kt`。
- **领域层**：不依赖 Android 框架，集中处理闰日、重复规则、周年间隔、字素截断等纯逻辑。
- **数据层**：Room 单库保存事件、分类、标签、关联、提醒、附件和同步队列；DataStore 保存偏好；Repository 统一封装读写与软删除。
- **安全**：附件落在 `filesDir/attachments/`，通过 `FileProvider` 提供应用内 URI；备份使用分块 AES-GCM 容器；应用锁 PIN 与自动备份密码由 Android Keystore 保护的密钥加密。
- **测试**：JUnit 单元测试、Room 仪器测试和 Compose UI 测试；CI 校验 Gradle Wrapper，并执行 debug / release 构建、单元测试与 Lint。

## 构建

```bash
./gradlew :app:assembleDebug
./gradlew :app:testDebugUnitTest
./gradlew :app:lintDebug
```

Release 构建：

```bash
./gradlew :app:assembleRelease
```

检测到 `keystore.properties` 时产出已签名 APK，否则产出未签名包；签名文件与密码不进入仓库。详细发布检查见 [RELEASE.md](RELEASE.md)。

## 权限

- `POST_NOTIFICATIONS`（Android 13+）：提醒通知，可拒绝，不影响事件和提醒数据。
- `SCHEDULE_EXACT_ALARM`：优先使用精确闹钟；未授权时自动降级为非精确提醒。
- `RECEIVE_BOOT_COMPLETED`：重启后恢复提醒调度。

附件通过应用私有目录和 `FileProvider` 访问，不申请广泛存储权限。
