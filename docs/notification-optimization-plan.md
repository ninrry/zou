# Zou 通知系统优化：澎湃OS 3 × 小米15 Pro 适配

## 完成状态

| Round | 改动 | 状态 |
|:----:|:-----|:----:|
| **R1** | `SCHEDULE_EXACT_ALARM` → `USE_EXACT_ALARM` | ✅ |
| **R1** | 通知频道拆分 1→4 | ✅ |
| **R2** | `XiaomiPowerKeeper` 工具类（已存在） | ✅ |
| **R2** | 设置页 HyperOS 引导卡片 | ✅ |
| **R3** | 回退保障（原代码兼容） | ✅ |

## 改动的文件

| 文件 | 变更 |
|:----|:-----|
| `AndroidManifest.xml` | `USE_EXACT_ALARM` 替代 `SCHEDULE_EXACT_ALARM`，安装即授权 |
| `ReminderConstants.kt` | 4 个频道常量 + `channelConfigs` 列表 + `ChannelConfig` data class |
| `ReminderNotificationManager.kt` | `ensureChannel()` → `ensureChannels()`，按 reason 选频道 |
| `ZouApplication.kt` | 同步方法名 `ensureChannel()` → `ensureChannels()` |
| `SettingsScreen.kt` | 新增 `HyperOsOptimizationCard` 引导卡片 + 3 个设置跳转 |
| `SettingsViewModel.kt` | 已有 `isHyperOS` 检测（无需修改） |
| `SettingsUiState.kt` | 已有 `isHyperOS` 字段（无需修改） |
| `XiaomiPowerKeeper.kt` | 已有完整实现（无需修改） |

## HyperOS 引导卡片

在设置页顶部显示（仅小米系设备），引导用户完成 3 步：

```
┌──────────────────────────────────────┐
│ ℹ️ 澎湃OS 提醒优化                     │
│                                       │
│ 检测到小米澎湃OS系统。为确保提醒准时    │
│ 弹出，建议完成以下设置：               │
│                                       │
│ 🔋 省电策略 → 无限制     [设置] ➡️    │
│    防止系统自动限制后台提醒             │
│                                       │
│ ▶️ 允许自启动            [设置] ➡️    │
│    重启后仍可接收提醒广播              │
│                                       │
│ 🔒 锁屏通知→显示所有内容  [设置] ➡️    │
│    锁屏时也能看到提醒详情              │
└──────────────────────────────────────┘
```

## 编译验证

```
compileDebugKotlin:  BUILD SUCCESSFUL ✅
testDebugUnitTest:   BUILD SUCCESSFUL ✅ (35 tasks, 21s)
```

## 测试说明

通知系统优化涉及 HyperOS 行为，需在 **实体机上验证**（模拟器无法模拟）：
- 精确闹钟是否能准时触发
- 全屏提醒是否能弹出（核心项，HyperOS 默认阻止后台弹窗）
- 通知频道是否正确显示 4 个分类
