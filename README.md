# Zou

[![Android Quality](https://github.com/ninrry/zou/actions/workflows/android-quality.yml/badge.svg)](https://github.com/ninrry/zou/actions/workflows/android-quality.yml)
[![Latest Release](https://img.shields.io/github/v/release/ninrry/zou)](https://github.com/ninrry/zou/releases/latest)
[![Android 10+](https://img.shields.io/badge/Android-10%2B-3DDC84)](https://github.com/ninrry/zou/releases/latest)

Zou 是一个本地优先的 Android 个人效率工具，用于管理待办、习惯和 Markdown 笔记。应用提供今日概览、提醒、回收站和本地备份，数据默认保存在设备上。

项目使用 Kotlin 与 Jetpack Compose 构建，最低支持 Android 10。

## 功能

- **今日概览**：集中查看当天待办、习惯进度和最近笔记。
- **待办任务**：支持优先级、紧急标记、子任务、截止时间和重复提醒。
- **习惯追踪**：支持步骤型与时长型打卡、执行频率、提醒窗口和连续记录。
- **Markdown 笔记**：支持本地图片、置顶、批量操作和 Markdown 导出。
- **提醒系统**：支持精确提醒、重复提醒，并在重启、时间或时区变化后恢复调度。
- **数据管理**：支持软删除、回收站、ZIP 备份与恢复。
- **界面与动效**：使用定制色彩和统一动效系统串联主要页面与状态变化。

Zou 当前不提供云同步、团队协作、Web 或 iOS 客户端。

## 下载与安装

最新版本：**[v0.3.6](https://github.com/ninrry/zou/releases/latest)**

| 安装包 | 适用设备 |
| --- | --- |
| [arm64-v8a APK](https://github.com/ninrry/zou/releases/download/v0.3.6/zou-v0.3.6-arm64-v8a.apk) | 大多数现代 Android 手机和平板 |
| [x86_64 APK](https://github.com/ninrry/zou/releases/download/v0.3.6/zou-v0.3.6-x86_64.apk) | x86_64 模拟器和部分 ChromeOS 设备 |
| [SHA-256 校验文件](https://github.com/ninrry/zou/releases/download/v0.3.6/SHA256SUMS.txt) | 校验下载文件完整性 |

安装提醒：

- Android 13 及以上版本需要授予通知权限，提醒功能才能正常显示通知。
- 部分 Android 厂商系统会限制后台任务。若提醒不稳定，请允许精确闹钟、自启动或关闭相关省电限制。
- Release APK 使用项目维护者的签名证书签名，可直接覆盖安装同签名的旧版本。

## 技术栈

- Kotlin
- Jetpack Compose 与 Material 3
- MVVM、UseCase、Repository
- Hilt
- Navigation Compose
- Room 与 DataStore
- Kotlin Coroutines 与 Flow
- WorkManager 与 AlarmManager
- kotlinx.serialization
- Coil

应用采用单模块、分层组织的结构。UI 状态由 ViewModel 暴露，业务操作通过 UseCase 和 Repository 访问本地数据与提醒调度。

## 开发环境

- JDK 21
- Android SDK 37
- Android Studio 或兼容的 Gradle 开发环境
- Windows、macOS 或 Linux

克隆仓库：

```bash
git clone https://github.com/ninrry/zou.git
cd zou
```

常用命令：

```bash
# Debug APK
./gradlew :app:assembleDebug

# 单元测试、Lint、Debug 构建和 Android 测试源码编译
./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:compileDebugAndroidTestKotlin --console=plain

# 连接模拟器或真机后运行仪器测试
./gradlew :app:connectedDebugAndroidTest --console=plain

# Release APK
./gradlew :app:assembleRelease
```

Windows PowerShell 中将 `./gradlew` 替换为 `.\gradlew.bat`。

APK 按 ABI 分包生成：

```text
app/build/outputs/apk/debug/app-arm64-v8a-debug.apk
app/build/outputs/apk/debug/app-x86_64-debug.apk
app/build/outputs/apk/release/app-arm64-v8a-release.apk
app/build/outputs/apk/release/app-x86_64-release.apk
```

本地存在 `keystore.properties` 时，Release 构建会使用配置的密钥签名。贡献者通常使用 Debug 构建即可。

## 项目结构

```text
app/src/main/java/luzzr/zou/
├── app/        # 应用入口与导航
├── core/       # 通用 UI、主题、提醒、Markdown、权限和平台适配
├── data/       # Room、DataStore、备份、媒体与 Repository 实现
├── domain/     # 领域模型、Repository 接口和 UseCase
└── feature/    # today、tasks、habits、notes、settings、backup、trash
```

## 数据与权限

Zou 的任务、习惯、笔记、设置和媒体文件默认保存在设备本地。应用不会自动上传数据。

主要权限用于：

- 显示通知
- 调度精确提醒
- 在设备重启或系统时间变化后恢复提醒
- 提醒时振动、唤醒设备或显示提醒页面

备份文件包含数据库数据、设置和相关媒体。请自行妥善保存导出的备份文件。

## 质量与性能

GitHub Actions 会在 `main` 和 Pull Request 上运行：

- 单元测试
- Android Lint
- Debug 构建
- Android 测试源码编译
- Release 构建

连接 Android 设备后，可运行性能冒烟脚本：

```powershell
.\scripts\android-gfxinfo-smoke.ps1
```

结果会写入 `scratch/performance/`。更多说明见 [性能验收手册](docs/performance-playbook.md)。

## 文档

- [产品需求](docs/prd.md)
- [技术方案](docs/tech-spec.md)
- [信息架构](docs/information-architecture.md)
- [UI 设计规范](docs/ui-design-spec.md)
- [构建规范](docs/build-conventions.md)
- [性能验收手册](docs/performance-playbook.md)

## 参与贡献

欢迎通过 [Issues](https://github.com/ninrry/zou/issues) 报告问题或提出建议，也欢迎提交 Pull Request。

提交代码前建议至少运行：

```bash
./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug --console=plain
```

涉及导航、数据库、提醒或用户操作流程的改动，建议同时运行相关仪器测试。

## 版本发布

版本记录和安装包位于 [GitHub Releases](https://github.com/ninrry/zou/releases)。当前稳定版本为 `v0.3.6`。

## 许可证

当前仓库尚未包含许可证文件。在许可证明确前，请不要假定本项目代码可以用于再分发或商业用途。
