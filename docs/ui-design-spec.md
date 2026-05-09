# zou (NoteFlow) UI 设计规格

> 生成日期：2026-05-09
> 来源：全代码库调研

---

## 一、导航架构

### 顶层结构
- **Tab 容器**：`TopLevelCanvasRoute` — HorizontalPager + 自定义 Tab Bar
- **4 个 Tab**：今日 / 待办 / 习惯 / 笔记
- **子页面（全屏路由）**：详情页、编辑页、设置页、回收站、备份恢复

### 完整页面清单（共 17 个界面）

| # | 页面 | 类型 | 路径文件 |
|---|------|------|---------|
| 1 | **今日页 (Today)** | 主页 Tab | `feature/today/TodayScreen.kt` + `TodayComponents.kt` |
| 2 | **待办列表 (Tasks)** | 主页 Tab | `feature/tasks/TasksScreen.kt` |
| 3 | **任务详情** | 子路由 | `feature/tasks/TaskDetailScreen.kt` |
| 4 | **任务编辑（3步向导）** | 子路由 | `feature/tasks/TaskEditorScreen.kt` |
| 5 | **习惯列表 (Habits)** | 主页 Tab | `feature/habits/HabitsScreen.kt` |
| 6 | **习惯详情** | 子路由 | `feature/habits/HabitDetailScreen.kt` |
| 7 | **习惯编辑（3步向导）** | 子路由 | `feature/habits/HabitEditorScreen.kt` |
| 8 | **笔记列表 (Notes)** | 主页 Tab | `feature/notes/NotesScreen.kt` |
| 9 | **笔记详情** | 子路由 | `feature/notes/NoteDetailScreen.kt` |
| 10 | **笔记编辑** | 子路由 | `feature/notes/NoteEditorScreen.kt` |
| 11 | **设置页** | 子路由 | `feature/settings/SettingsScreen.kt` |
| 12 | **回收站** | 子路由 | `feature/trash/TrashScreen.kt` |
| 13 | **备份与恢复** | 子路由 | `feature/backup/BackupRestoreScreen.kt` |
| 14 | **今日快速创建（FAB 展开）** | 覆盖层 | `feature/today/TodayComponents.kt` → `TodayQuickCreateFab` |
| 15 | **日期时间选择器 BottomSheet** | 弹窗 | `core/ui/ZouDateTimeSheet.kt` |
| 16 | **径向展开动画覆盖层** | 动画 | `core/ui/RadialExpansionOverlay.kt` |
| 17 | **Tab Bar** | 固定顶栏 | `core/ui/TopModuleTabBar.kt` |

---

## 二、设计系统

### 2.1 色彩体系

**底色盘（温暖系）：**
```
背景: #FFFBF7 (奶油白)
表面: #FFFCF8
表面强调: #F4EDE2
文本主: #27221B (深暖灰)
文本次: #584F44
文本第三: #85796D
```

**模块强调色（4 色体系）：**
| 模块 | Accent | Soft | Glow |
|------|--------|------|------|
| 今日 (Today) | #6F8FB3 (灰蓝) | #E3EBF5 | #C8D5E5 |
| 任务 (Task) | #8C7DD0 (紫) | #EAE6FB | #D1C8F0 |
| 习惯 (Habit) | #739E8A (绿) | #E5F0EA | #C8DCCF |
| 笔记 (Note) | #C0A562 (金) | #F5ECCE | #E6D8A8 |

**Dark 主题：** 独立暗色配色，调性匹配

### 2.2 排版
- **Headline/Title**: FontFamily.Serif, SemiBold
- **Body/Label**: 默认系统字体
- 字号：headlineLarge=32sp → labelMedium=12sp

### 2.3 形状
```
extraSmall=14dp, small=18dp, medium=24dp, large=30dp, extraLarge=36dp
```
所有形状统一使用 `RoundedCornerShape`

### 2.4 布局 Token
```
Space8=8dp, Space12=12dp, Space16=16dp, Space20=20dp, Space24=24dp, Space28=28dp
ScreenHorizontalPadding=20dp, ScreenVerticalPadding=16dp
RadiusInput=16dp, RadiusCard=24dp, RadiusFab=20dp, RadiusPill=32dp
```

### 2.5 运动 Token
- Spring: SpringBouncy, SpringSmooth
- Easing: EasingEmphasized(0.05,0.7,0.1,1.0), EasingAccelerate, EasingStandard
- Duration: Short=200, Medium=340, Long=500, NavEnter=340, NavExit=90

---

## 三、核心组件

### 3.1 GlassSurface（玻璃拟态卡片）
- 全项目统一使用的卡片容器
- 支持 GlassLevel.Normal / Weak / Strong
- 支持自定义 shape、accentColor
- 带有光泽、边框、内发光、阴影等效果

### 3.2 空状态 (ZouEmptyStateCard)
- 统一空状态组件，支持 icon、title、description、action
- 各列表页使用：TasksScreen / HabitsScreen / NotesScreen / TrashScreen

### 3.3 编辑器组件
- **ZouStepBar**：顶部分步指示器
- **ZouStepBottomBar**：底部导航栏（上一步/下一步/取消/保存）
- **ZouEditorSection**：编辑区段容器
- **ZouPageHeader**：页面标题+副标题

### 3.4 列表卡片
- 统一使用 GlassSurface + noteFlowPressScale
- 支持 combinedClickable（点击详情/长按编辑）
- 各列表统一 spacing=14dp，horizontalPadding=20dp，verticalPadding=12dp

---

## 四、已知状态

以下为代码审计发现的待改进事项，按修复状态分类。

### ✅ 已修复

| # | 问题 | 修复 |
|:-|:----|:-----|
| ① | TopLevelDestination 图标未用 AutoMirrored | 已确认 Back 导航图标统一使用 `Icons.AutoMirrored.Outlined.ArrowBack` |
| ② | Tab Bar 选中态 flicker | 现已使用 `selectedDestination` 而非 `pagerState.currentPage`，快滑不闪烁 |
| ③ | Tab Bar 图标 contentDescription | 已改为 `destination.label`，TalkBack 可识别 |
| ④ | Easing 曲线等值 Bug | EasingEmphasized 与 EasingStandard 已分别修正为 M3 标准值 |
| ⑤ | 笔记缺模块色 | NotesScreen 卡片已使用 `accentColor = ZouNoteAccent` |
| ⑥ | animateItem 缺失 | 6 个 LazyColumn 均已添加 `Modifier.animateItem()` |
| ⑦ | PullToRefresh 缺失 | 4 个主页面全部接入 PullToRefreshBox |
| ⑧ | SwipeToDismiss 缺失 | TasksScreen 已实现 SwipeToDismissBox |
| ⑨ | 触觉反馈缺失 | TasksScreen 滑动触发时已添加 HapticFeedback |
| ⑩ | indication = null 全部消除 | 默认 M3 ripple 恢复（删除 indication=null 参数） |
| ⑪ | HABITS 图标语义 | Refresh → Loop（循环箭头更贴切习惯概念） |
| ⑫ | 删除无淡出动画 | 已添加 AnimatedVisibility + fadeOut(250ms) + shrinkVertically |
| ⑬ | TodayScreen padding/spacing 不一致 | 已统一使用 LayoutTokens（20dp/Space12） |

### ⏳ 待改进

| # | 问题 | 优先级 | 说明 |
|:-|:----|:------:|:-----|
| ① | 硬编码 padding 遍布各 Screen 文件 | P3 | 尚有若干页面未统一迁移至 LayoutTokens |
| ② | Card padding 不统一（18dp vs 16dp） | P3 | TaskCard/HabitCard/NoteCard 与 TodayQuickCard 不一致 |
| ③ | Checkbox contentDescription = null | P2 | TaskCard 中 Checkbox，但已使用 Filled/Outlined Circle 图标，视觉可识别 |
| ④ | ZouShimmer 未接入加载态 | P2 | 骨架屏组件已创建，待接入各 Screen 加载状态 |
| ⑤ | Settings / Backup 页面布局 | P3 | 区隔感和视觉主次可优化 |
| ⑥ | Snackbar 可能被 FAB 遮挡 | P3 | 需要布局调整 |
| ⑦ | animateContentSize 覆盖不足 | P3 | 仅在 TodayComponents 有 1 处 |

---

## 五、打磨迭代记录

### Round 1-3：UI 打磨（2026-05-08）
- 底部栏取消按钮 + 48dp 高度
- 进度条 6dp→10dp，alpha 1.0
- 空状态图标更新（TaskAlt / EditNote / AutoAwesome）
- 进度条轨道 Contrast 提升（accent alpha=14%）
- 习惯图标 AutoAwesome → Repeat

### Round 4-6：动画打磨（2026-05-09）
- 6 条动画路径逐条录屏审核
- Pill 弹簧增强（damping 0.50→0.30）
- RadialExpansion 起始跳跃修复（key()）
- 复选框旧圆缩放 0.7→0.3
- 返回退出自定义 popExitTransition
- 全部动画路径满分通过

### Round 7：最终收尾（2026-05-09）
- indication 默认 M3 ripple 恢复
- HABITS 图标 Refresh → Loop
- TodayScreen padding/spacing 统一 LayoutTokens
- 删除淡出动画（AnimatedVisibility 250ms）
- GitHub 仓库清理（移除旧 package schema 文件）
- Release 构建 v0.3.2

### 下一步
- 低优先级待改进项（见上表）可按需逐步实施
