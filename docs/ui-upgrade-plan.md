# Zou UI 打磨升级计划书 v1.0

> 基于设计审计 + Material Design 3 标准研究
> 日期：2026-05-08
> 状态：**待审查** 🔍

---

## 概述

基于对全部 UI 代码（45+ Compose 文件）的通读和 Material Design 3 官方规范研究，共发现 **10 处设计问题**。经筛选评估，计划在内修复 **5 项**（P0~P1），其余留待后续。

---

## 调研摘要

### 已查阅资料
- Material Design 3 Easing & Duration 官方 Token 规范 ✅
- Android Accessibility 触控目标指南 ✅
- 项目内全部 UI 代码 45+ 文件 ✅

### MotionTokens 关键发现（Bug）

**当前代码** (`MotionTokens.kt:24-26`)：
```kotlin
val EasingEmphasized = CubicBezierEasing(0.25f, 1f, 0.5f, 1f)
val EasingStandard   = CubicBezierEasing(0.25f, 1f, 0.5f, 1f)  // 完全一样！
```

**M3 官方值**：
| Token | Compose 值 |
|:------|:-----------|
| Emphasized Decelerate | `CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)` |
| Standard | `CubicBezierEasing(0.2f, 0f, 0f, 1f)` |

两条曲线完全一致说明是复制粘贴错误，所有动画的缓动效果都是错的。

---

## 修复项明细

---

### ① P0 — easing 曲线修正

**问题**：`EasingEmphasized` 和 `EasingStandard` 值完全相同，不符合 M3 运动规范

**影响范围**：
- `MotionTokens.kt` — 修改常量
- `ZouNavHost.kt` — 导航过渡动画引用这些常量
- `RadialExpansionOverlay.kt` — 径向展开动画引用
- 所有使用 `MotionTokens.EasingEmphasized` / `EasingStandard` 的地方

**改动**：
| 常量 | 当前值 | 目标值 |
|:-----|:------:|:------:|
| `EasingEmphasized` | `(0.25, 1, 0.5, 1)` | `(0.05, 0.7, 0.1, 1.0)` — M3 Emphasized Decelerate |
| `EasingStandard` | `(0.25, 1, 0.5, 1)` | `(0.2, 0.0, 0.0, 1.0)` — M3 Standard |

**效果**：所有页面过渡动画、FAB 径向展开动画的节奏感提升，符合 M3 运动语言。

---

### ② P1 — 水平内边距统一（修复不一致）

**问题**：各页面水平边距不统一

| 页面 | 当前水平 padding | 来源 |
|:----|:---------------:|:----|
| TodayScreen | **16dp** | `horizontal = 16.dp` 硬编码 |
| TasksScreen | 20dp | `horizontal = 20.dp` |
| HabitsScreen | 20dp | `horizontal = 20.dp` |
| NotesScreen | 20dp | `horizontal = 20.dp` |
| LayoutTokens 定义 | 20dp | `ScreenHorizontalPadding = Space20` |

TodayScreen 的 `16dp` 与其余页面和 LayoutTokens 定义不一致。

**改动**：
```kotlin
// TodayScreen.kt:136
.padding(horizontal = 16.dp, vertical = 12.dp)
// → 改为
.padding(horizontal = LayoutTokens.ScreenHorizontalPadding, vertical = LayoutTokens.ScreenVerticalPadding)
```

**影响范围**：仅 `TodayScreen.kt` 一行

---

### ③ P1 — 列表垂直间距统一

**问题**：列表项间距不统一

| 页面 | 当前 verticalArrangement |
|:----|:----------------------:|
| TodayScreen 双列 | `spacedBy(10.dp)` |
| TasksScreen | `spacedBy(14.dp)` |
| HabitsScreen | `spacedBy(14.dp)` |
| NotesScreen | `spacedBy(14.dp)` |

TodayScreen 的 `10dp` 间隙较小，与其余页面不一致。

**改动**：
```kotlin
// TodayScreen.kt:137
verticalArrangement = Arrangement.spacedBy(10.dp),
// → 改为
verticalArrangement = Arrangement.spacedBy(LayoutTokens.Space12),
```

但注意 Today 双列布局内有自己的 `cardGap`（8dp/10dp），外层用 12dp 是比较合理的折中。

**影响范围**：仅 `TodayScreen.kt` 一行

---

### ④ P1 — 笔记卡片缺少模块色标识

**问题**：任务卡和习惯卡左侧有彩色指示器条（`TodayCardLeadingAccent`），笔记卡片没有

**Before**（`NotesScreen.kt:88-100`）：
```kotlin
GlassSurface(
    modifier = Modifier.fillMaxWidth()...
        .combinedClickable(...)
) {
    Column {
        Text(title, ...)
        Text(preview, ...)
        Row { date, tags }
    }
}
```

**After**：添加与模块色（笔记金 `ZouNoteAccent`）匹配的左侧指示器，与任务/习惯卡片格式统一

**改动**：`NotesScreen.kt` 的笔记卡片内部添加 `TodayCardLeadingAccent` 组件

**影响范围**：仅 `NotesScreen.kt`

---

### ⑤ P2 — 无障碍 contentDescription 补充

**问题**：`TopModuleTabBar` 中图标 `contentDescription = null`，屏幕阅读器无法识别

**Before**：
```kotlin
Icon(
    imageVector = ...,
    contentDescription = null,  // ❌ 无障碍盲区
    ...
)
```

**After**：
```kotlin
Icon(
    imageVector = ...,
    contentDescription = destination.label,  // ✅ "今日" / "待办" / "习惯" / "笔记"
    ...
)
```

**影响范围**：`TopModuleTabBar.kt:130` 一行

---

## 不修复项说明

| # | 问题 | 理由 |
|:-|:----|:-----|
| ⑥ Tab bar 高度硬编码 74dp | 视觉正常，不易量化改进 | 暂不修 |
| ⑦ HeroCard 内边距硬编码 | 视觉正常，LayoutTokens 暂无对应值 | 暂不修 |
| ⑧ GlassSurface 层级区分 | 需要更多的设计决策 | 后续迭代 |
| ⑨ Settings 页输入框玻璃风格 | 需要自定义 TextField 样式，工作量大 | 后续迭代 |
| ⑩ 双列布局间隙 Today 特殊处理 | 双列需要更紧密布局，有其合理性 | 保留 |

---

## 实施文件清单

| # | 文件 | 改动类型 | 涉及行数 |
|:-|:----|:--------|:--------:|
| ① | `MotionTokens.kt` | 修改 2 个常量的值 | 2 行 |
| ② | `TodayScreen.kt` | 修改 padding | 1 行 |
| ③ | `TodayScreen.kt` | 修改 spacing | 1 行 |
| ④ | `NotesScreen.kt` | 添加左侧色条组件 | 3~5 行 |
| ⑤ | `TopModuleTabBar.kt` | 修改 contentDescription | 1 行 |

**共计 5 个文件**，均只改几行，不影响业务逻辑，无重构风险。

---

## 测试验证

1. `./gradlew assembleDebug` — 编译通过 ✅
2. `./gradlew assembleRelease` — R8 压缩通过 ✅
3. 视觉检查每个页面边距一致性
4. 检查 Tab 切换动画流畅度变化
5. 检查笔记卡片视觉样式

---

## 计划总结

```
MotionTokens 缓动曲线修复 (①)
    ↓
TodayScreen 内边距对齐 LayoutTokens (②)
    ↓
TodayScreen 列表间距统一 (③)
    ↓
NotesScreen 添加模块色指示器 (④)
    ↓
TopModuleTabBar 无障碍优化 (⑤)
    ↓
Release 构建验证
```

**预计代码修改量**：约 **10 行**，涉及 **5 个文件**，零风险。

---

**请主人审查此计划**，确认后我按顺序逐个实施！😊
