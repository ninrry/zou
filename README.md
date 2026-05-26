# Zou — 个人多功能记事本

> **本地优先 · 自建设计规范 · 动画驱动**  
> 用 Android 原生 Kotlin + Jetpack Compose 打造的个人效率工具，聚合任务、习惯、笔记与今日概览。

[![Kotlin](https://img.shields.io/badge/Kotlin-2.1+-7F52FF?logo=kotlin)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/Compose-1.7+-4285F4?logo=jetpackcompose)](https://developer.android.com/jetpack/compose)
[![API](https://img.shields.io/badge/minSdk-29-3DDC84?logo=android)](https://developer.android.com/studio)
[![License](https://img.shields.io/badge/license-MIT-green)](LICENSE)

---

## ✨ 特性

### 🗂 四大模块

| 模块 | 功能 |
|------|------|
| **今日概览** | 聚合当天待办和习惯，快速创建 FAB，Pill 卡片展示 |
| **待办任务** | 列表 / 详情 / 三步创建向导 / 子任务 / 到期提醒 / 优先级别 |
| **习惯追踪** | 列表 / 详情 / 三步创建向导 / 自定义步骤 / 重复提醒 |
| **笔记** | Markdown 编辑与渲染 / 图片内嵌引用 / 本地路径映射 |

### 🎨 动画系统

经过严苛的 6 条动画路径审核与迭代优化，每条动画均为满分水准：

- **Pill 弹簧入场** — 物理弹簧阻尼 0.30，缩放 0.80→1.00 弹性显现
- **RadialExpansion** — 径向圆展开过渡，`key()` 强制重置确保无起始跳跃
- **StaggeredReveal** — 列表交错显现，支持正向 / 反向 / 同时三种模式
- **Tab 切换** — 视差缩放 + 透明度渐变 + 水平滑动惯性
- **复选框弹跳** — 旧圆缩小 30%，新圆弹簧弹出
- **SwipeToDismiss** — 列表滑动删除带阻尼回弹
- **向导步骤过渡** — HorizontalPager 原生滑动 + 指示器递进
- **返回退出动画** — 自定义 `popExitTransition`（Scale + FadeOut + Clip）

### 🎯 设计系统

- **自建设计规范** — 精心定制的全局主题与交互组件库
- **温暖色调** — 默认底色盘 `#FFFBF7`，深暖灰文本
- **圆角语言** — 大一统的 CornerRadius 体系（4dp / 8dp / 12dp / 16dp）
- **间距系统** — 基于 4dp 倍数的层级间距（4 / 8 / 12 / 16 / 24 / 32 / 48dp）
- **排版** — 字重 + 行高 + 字距统一 token
- **Easing 曲线** — 自定义加速 / 减速 / 弹性曲线

### 🔧 实用功能

- **重复提醒** — 支持一次性、每日、每周、每月、自定义间隔
- **免打扰时段** — 自定义静音窗口
- **回收站** — 软删除，30 天内可恢复
- **备份与恢复** — 本地 JSON 格式导入导出
- **数据持久化** — Room 本地数据库 + DataStore 设置

---

## 🏗 技术栈

| 层 | 技术 |
|----|------|
| **语言** | Kotlin 2.1+ |
| **UI** | Jetpack Compose（Material 3）+ 自定义动画 |
| **架构** | MVVM + UseCase + Repository 模式 |
| **DI** | Hilt |
| **导航** | Navigation Compose（类型安全路由） |
| **数据库** | Room（KAPT） |
| **异步** | Kotlin Coroutines + Flow |
| **序列化** | Kotlin Serialization |
| **后台** | WorkManager / AlarmManager |
| **构建** | Gradle KTS + Version Catalog |

---

## 📁 项目结构

```
app/src/main/java/luzzr/zou/
├── app/                    # 应用入口、Hilt 模块、导航（ZouNavHost）
├── core/
│   ├── designsystem/       # 主题与基础设计系统
│   ├── hyperos/            # 澎湃OS 系统兼容层
│   ├── markdown/           # Markdown 渲染核心
│   ├── reminder/           # 提醒调度（AlarmScheduler）
│   ├── time/               # 时间处理工具
│   └── ui/                 # UI 通用控件与组件库
├── data/                   # Room 数据库、DataStore 设置、Repository 实现与备份逻辑
├── domain/                 # 领域模型、仓储接口、UseCase
└── feature/
    ├── backup/             # 备份与恢复
    ├── habits/             # 习惯追踪模块
    ├── notes/              # 笔记模块
    ├── settings/           # 设置
    ├── tasks/              # 待办任务模块
    ├── today/              # 今日概览模块
    └── trash/              # 回收站
```

---

## 🚀 快速开始

### 环境要求

- **Android Studio** Ladybug 或更新版本
- **JDK** 17+
- **Android SDK** 36
- **最低支持** Android 10（API 29）
- **推荐设备** Android 13+（API 33+）以获得最佳交互体验

### 构建

```bash
# Debug 构建
./gradlew assembleDebug

# Release 构建（需要签名密钥）
./gradlew assembleRelease

# 安装 Debug 到设备
./gradlew installDebug
```

### 产物

| 类型 | 路径 |
|------|------|
| Debug APK | `app/build/outputs/apk/debug/app-arm64-v8a-debug.apk` |
| Release APK | `app/build/outputs/apk/release/app-arm64-v8a-release.apk` |

---

## 📖 文档

| 文档 | 说明 |
|------|------|
| `docs/prd.md` | 产品需求文档 |
| `docs/tech-spec.md` | 技术方案设计 |
| `docs/db-schema.md` | 数据库模型 |
| `docs/information-architecture.md` | 信息架构 |
| `docs/ui-style-guide.md` | UI 风格指南 |
| `docs/ui-design-spec.md` | UI 设计规格（页面清单、色彩系统、动画规格） |
| `docs/ui-upgrade-plan.md` | UI 升级计划 v1（动画打磨） |
| `docs/ui-upgrade-plan-v2.md` | UI 升级计划 v2（全面视觉审计） |
| `docs/animation-polish-plan.md` | 动画打磨计划 |
| `docs/hyperos-notification-plan.md` | 澎湃OS 通知优化方案 |
| `docs/notification-optimization-plan.md` | 通知优化方案 |
| `docs/build-conventions.md` | 构建约定（签名、版本、产物） |
| `docs/agents.md` | AI 开发规范 |

---

## 🧩 设计原则

1. **本地优先** — 不依赖云服务，数据 100% 在设备上
2. **离线可用** — 全功能离线，无需网络
3. **动画驱动** — 微交互是体验的核心，每条动画路径独立审核
4. **独立视觉语言** — 抛弃原生刻板印象，打造专属设计规范
5. **渐进式构建** — 功能按里程碑交付，保持可编译可运行

---

## 📦 版本历史

| 版本 | 代号 | 说明 |
|------|------|------|
| v0.1.0 | NoteFlow | 初始版本，任务 + 习惯 + 笔记基础模块 |
| v0.2.0 | Zou | 重构命名，引入动画系统，UI 设计系统 |
| v0.3.0 | Polished | 动画路径满分打磨，RadialExpansion 修复，Spring 增强 |
| v0.3.1 | UI 打磨 R1-R3 | 底部栏优化、进度条视觉增强、空状态图标规范化 |
| v0.3.2 | UI 打磨 R4-R6 | 底部栏平衡方案、进度条 10dp/轨道透明度修正、AutoMirrored 修复 |
| v0.3.3 | HyperOS | 澎湃OS 通知优化：USE_EXACT_ALARM、4 频道、XiaomiPowerKeeper 检测 |
| v0.3.4 | DateRange | 任务提醒日期范围控制：支持指定提醒的起止日期，仅在范围内发送提醒 |

---

## ⚠️ 已知限制

- **提醒功能** 依赖 Android 后台权限，不同厂商 ROM 行为可能存在差异
- **云同步** 暂不支持，计划未来加入
- **Widget** 暂不支持

---

## 🤝 贡献

本项目为个人效率工具，欢迎 Fork 和 Issue 反馈。

---

## 📄 许可证

[MIT License](LICENSE)

Copyright © 2026 季札

---

<p align="center">Made with ❤️ by <a href="https://github.com/ninrry">ninrry</a></p>
