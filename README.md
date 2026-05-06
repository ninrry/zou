# zou

一个使用 **Android 原生 Kotlin + Jetpack Compose** 开发的本地优先个人多功能记事应用，聚合了任务、习惯、笔记与今日概览能力。

## 当前能力

- 任务模块：列表、详情、编辑、子任务、提醒
- 习惯模块：列表、详情、编辑、步骤、提醒
- 笔记模块：列表、编辑、Markdown 预览、图片引用
- 今日页：聚合任务与习惯视图
- 设置：提醒相关偏好
- 回收站：软删除内容管理
- 备份恢复：本地导出与导入

## 技术栈

- Kotlin / Jetpack Compose
- Room / Hilt / Navigation Compose
- WorkManager / DataStore
- Kotlin Serialization

## 项目结构

```text
app/src/main/java/luzzr/zou
├─ app/                 # 应用入口、导航
├─ core/                # UI、时间、Markdown、提醒等通用能力
├─ data/                # Room、DataStore、Repository 实现
├─ domain/              # 模型、仓储接口、UseCase
└─ feature/             # tasks / habits / notes / today / trash / backup / settings
```

## 环境要求

- Android SDK 36
- 最低支持 Android 10（API 29）

## 构建

```bash
# Debug
./gradlew assembleDebug

# Release（已签名）
./gradlew assembleRelease
```

## 产物

- Debug APK：`app/build/outputs/apk/debug/app-debug.apk`
- Release APK：`app/build/outputs/apk/release/app-release.apk`

## 设计与文档

- `docs/prd.md` — 产品需求
- `docs/tech-spec.md` — 技术方案
- `docs/db-schema.md` — 数据模型
- `docs/information-architecture.md` — 信息架构
- `docs/ui-style-guide.md` — UI 风格指南
- `docs/agents.md` — AI 开发规范

## 注意

- 本地优先，暂不包含云同步
- 提醒功能依赖 Android 后台权限，真机行为需持续验证
