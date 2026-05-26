# Zou UI 升级记录 v1.0

> 2026-05-08 → 2026-05-09
> 状态：**已完成** ✅

---

## 概述

基于 Material Design 3 官方规范研究和全代码审计，对 Zou App 的 UI 进行了系统性打磨。以下为完成记录。

---

## 已完成项

### ✅ ① Easing 曲线修正（P0）

**问题**：`EasingEmphasized` 和 `EasingStandard` 值完全相同（`0.25,1,0.5,1`），违背 M3 规范。

**修复**：`MotionTokens.kt` 修正为 M3 标准值：
- `EasingEmphasized` → `(0.05f, 0.7f, 0.1f, 1.0f)`
- `EasingStandard` → `(0.2f, 0.0f, 0.0f, 1.0f)`

**影响**：7 个文件、27 处引用自动受益。
**状态**：✅ 已上线

---

### ✅ ② 笔记卡片模块色标识（P1）

**问题**：笔记卡片缺少模块色（ZouNoteAccent）指示器，与任务/习惯卡片格式不统一。

**修复**：`NotesScreen.kt` 所有笔记卡片使用 `accentColor = ZouNoteAccent`。

**状态**：✅ 已上线

---

### ✅ ③ 无障碍 contentDescription（P2）

**问题**：`TopModuleTabBar` 的 Tab 图标 `contentDescription = null`，TalkBack 无法识别。

**修复**：改为 `contentDescription = destination.label`（"今日"、"待办"、"习惯"、"笔记"）。

**状态**：✅ 已上线

---

## 待完成项（低优先级）

### ⏳ ④ 水平 padding 不一致（P1）

**问题**：TodayScreen 使用 `16.dp`，其余页面和 LayoutTokens 使用 `20.dp`。

**修复计划**：`.padding(horizontal = 16.dp) → horizontal = LayoutTokens.ScreenHorizontalPadding`

**优先级**：低 — 不影响功能

---

### ⏳ ⑤ 列表垂直间距不一致（P1）

**问题**：TodayScreen 使用 `spacedBy(10.dp)`，其余页面使用 `14.dp`。

**修复计划**：`spacedBy(10.dp) → spacedBy(LayoutTokens.Space12)`

**优先级**：低 — 不影响功能

---

## 不修复项

| # | 问题 | 理由 |
|:-|:----|:-----|
| Tab bar 高度 74dp | 视觉正常，不易量化改进 |
| HeroCard 内边距硬编码 | 16dp 视觉合理，LayoutTokens 无对应值 |
| GlassSurface 层级区分 | 需更多设计决策，后续迭代 |
| Settings 输入框样式 | 需自定义 TextField，工作量较大 |
| 双列布局间隙 | 双列需更紧密布局，有其合理性 |

---

## 相关文档

- `docs/ui-upgrade-plan-v2.md` — 验证版计划记录
- `docs/animation-polish-plan.md` — 动画打磨完成报告
- `docs/ui-design-spec.md` — UI 设计规格
