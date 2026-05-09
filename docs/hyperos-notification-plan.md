# HyperOS 3 通知优化计划

## 目标
针对小米15 Pro + 澎湃OS 3，提高 zou 通知提醒的准时性和准确性。

---

## Phase 1：P0 — 准时分发保障

### 1.1 权限声明变更

**文件：** `AndroidManifest.xml`
**变更：** `SCHEDULE_EXACT_ALARM` → `USE_EXACT_ALARM`

| Before | After |
|:-------|:------|
| `<uses-permission android:name="android.permission.SCHEDULE_EXACT_ALARM" />` | `<uses-permission android:name="android.permission.USE_EXACT_ALARM" />` |

**理由：** `USE_EXACT_ALARM` 是专门为日历/闹钟类应用设计的普通权限，安装时自动授予，无需用户跳转设置。zou 是任务提醒应用，符合 Google Play 政策要求。

### 1.2 新增 HyperOS 工具类

**文件：** `app/src/main/java/luzzr/zou/core/hyperos/XiaomiPowerKeeper.kt`（新建）

提供 3 个功能：
- `isHyperOS()` — 检测是否运行在 HyperOS/MIUI
- `openBatteryOptimizationSettings()` — 打开应用省电策略页 → 引导用户设为"无限制"
- `openAutoStartSettings()` — 打开自启动管理页 → 引导用户开启

### 1.3 设置页新增 HyperOS 优化区

**文件：** `SettingsScreen.kt` / `SettingsUiState.kt` / `SettingsViewModel.kt`

在"提醒偏好"卡片下新增 HyperOS 专属设置区，仅 HyperOS 设备可见：

```
┌─ HyperOS 通知优化 ─────────────────────┐
│  检测到小米澎湃OS，以下优化可提高       │
│  提醒准时性。                           │
│                                         │
│  [🔋 省电策略 → 设为无限制]            │
│  [🚀 自启动管理 → 设为允许]            │
│  [🔔 系统通知设置]                      │
└─────────────────────────────────────────┘
```

| 变更 | Before | After |
|:----|:-------|:------|
| SettingsUiState | 无 HyperOS 相关字段 | 新增 `isHyperOS: Boolean` |
| SettingsViewModel | 无 | 新增检测逻辑，注入 `XiaomiPowerKeeper` |
| SettingsScreen | 通知权限只有一个按钮 | 新增 HyperOS 专属引导区，3 个按钮 |

---

## Phase 2：P1 — 通知系统细化

### 2.1 通知频道拆分

**文件：** `ReminderConstants.kt` / `ReminderNotificationManager.kt`

| Before | After |
|:-------|:------|
| 1 个频道 `task_reminders` | 4 个频道 |
| 所有提醒共用 | 按类型分配 |

| 频道 ID | 名称 | 用途 | Importance | 震动 |
|:--------|:-----|:-----|:----------|:----:|
| `task_start` | 任务开始提醒 | 任务到了开始时间 | HIGH | ✅ |
| `task_due` | 任务截止提醒 | 任务到了截止时间 | HIGH | ✅ |
| `habit_reminder` | 习惯提醒 | 习惯打卡时间到 | HIGH | ✅ |
| `reminder_repeat` | 重复提醒 | 逾期重复催促 | DEFAULT | ❌ |

`ensureChannel()` → `ensureChannels()`，批量创建所有频道。

### 2.2 ReminderNotificationManager 适配多频道

`showTaskReminder()` / `showHabitReminder()` 根据 `reason` 参数选择频道：
- `START` → `task_start`
- `EXACT` → `task_due`（任务）或 `habit_reminder`（习惯）
- `REPEAT` → `reminder_repeat`

### 2.3 锁屏/悬浮通知引导

在 HyperOS 设置区增加锁屏通知检测与引导（HyperOS 对 `IMPORTANCE_HIGH` 额外加了锁屏开关）。

---

## 实施顺序

1. Phase 1.1 — manifest 改一行
2. Phase 1.2 — 新建 XiaomiPowerKeeper.kt
3. Phase 1.3 — 更新 SettingsScreen + SettingsUiState + SettingsViewModel
4. Phase 2.1 — 更新 ReminderConstants
5. Phase 2.2 — 更新 ReminderNotificationManager

---

## 验证

- Build 通过 ✅
- 仅 HyperOS 设备显示优化引导区
- 点击按钮跳转到正确系统设置页
- 各提醒类型使用正确的通知频道
