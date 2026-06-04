# 开发与发布

## 工具链

- JDK 21
- Android SDK 37
- Android Gradle Plugin 9.2.1
- Gradle Wrapper 9.4.1
- Kotlin 2.3.21

最低支持 Android 10（API 29）。构建只保留中文和英文资源，并分别生成 `arm64-v8a` 与 `x86_64` APK。

## 常用命令

Linux 和 macOS：

```bash
# 本地质量门
./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:compileDebugAndroidTestKotlin --console=plain

# 连接设备后运行仪器测试
./gradlew :app:connectedDebugAndroidTest --console=plain

# Release APK
./gradlew :app:assembleRelease --console=plain
```

Windows PowerShell：

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:compileDebugAndroidTestKotlin --console=plain
.\gradlew.bat :app:connectedDebugAndroidTest --console=plain
.\gradlew.bat :app:assembleRelease --console=plain
```

APK 输出位置：

```text
app/build/outputs/apk/debug/app-arm64-v8a-debug.apk
app/build/outputs/apk/debug/app-x86_64-debug.apk
app/build/outputs/apk/release/app-arm64-v8a-release.apk
app/build/outputs/apk/release/app-x86_64-release.apk
```

## 签名

本地存在 `keystore.properties` 时，Debug 和 Release 构建会使用其中配置的密钥。密钥文件和 `keystore.properties` 已被 Git 忽略，不应提交到仓库。

没有发布密钥的贡献者可以正常构建和测试 Debug 版本。

## 测试

测试分为两类：

- `app/src/test`：JVM 单元测试。
- `app/src/androidTest`：数据库迁移、Repository、Compose 和端到端流程测试。

数据库 schema 变更需要：

1. 增加 Room migration。
2. 更新数据库版本。
3. 重新生成 `app/schemas`。
4. 增加迁移测试。

GitHub Actions 会在 `main` 和 Pull Request 上运行本地质量门、Release 构建，并上传测试报告与 APK。

## 性能检查

连接模拟器或真机后，可在 Windows PowerShell 运行：

```powershell
.\scripts\android-gfxinfo-smoke.ps1
```

脚本会安装 Debug APK，切换主要页面，并将 `gfxinfo`、`framestats`、logcat 和 UI tree 保存到：

```text
scratch/performance/<timestamp>/
```

性能数据用于前后版本对比。模拟器结果容易受宿主机负载影响，出现明显卡顿时应使用 Perfetto 或 Android Studio System Trace 进一步定位。

## 发布

发布新版本时：

1. 更新 `app/build.gradle.kts` 中的 `versionCode` 和 `versionName`。
2. 运行本地质量门与 Release 构建。
3. 验证 APK 内版本号、ABI、签名和 SHA-256。
4. 提交并推送版本改动，等待 GitHub Actions 通过。
5. 创建不可变版本标签和 GitHub Release，并上传各 ABI APK 与校验文件。

发布包和历史版本位于 [GitHub Releases](https://github.com/ninrry/zou/releases)。
