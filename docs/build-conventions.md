# 构建规范

## 包名

```
luzzr.zou
```

所有代码文件以 `luzzr.zou.*` 为包根，目录结构与包名一致。

## 签名

使用项目内的 `app/keystore.jks`（别名 `betone`）统一签名。

- Release 构建 → 自动签名
- Debug 构建 → 也使用同一密钥签名（确保 dev→release 无缝升级）

## 构建命令

```bash
# Debug APK（arm64-v8a，未压缩，含调试符号）
./gradlew assembleDebug
# → app/build/outputs/apk/debug/app-arm64-v8a-debug.apk

# Release APK（arm64-v8a，R8 压缩 + 资源裁剪，已签名）
./gradlew assembleRelease
# → app/build/outputs/apk/release/app-arm64-v8a-release.apk
```

## 分离打包（APK Splits）

- ABI：仅 **arm64-v8a**（适配小米15 Pro 等主流设备）
- 不生成 universal APK（避免体积膨胀）
- 语言资源：仅保留 **zh（中文）+ en（英文）**

## 版本号

| 版本 | 代号 | 说明 |
|------|------|------|
| 0.1.x | 初始 | 旧 NoteFlow 基础版本 |
| 0.2.0 | zou | 首次更名为 zou，包名统一，构建规范化 |

更新版本：修改 `app/build.gradle.kts` 中的 `versionCode` 和 `versionName`。

## 注意

- `keystore.properties` 和 `*.jks` 已被 `.gitignore` 排除，不会提交
- 新环境部署需手动放置 `app/keystore.jks` + 创建 `keystore.properties`
