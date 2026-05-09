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

## 四、已知问题（初步发现）

### P0 - 视觉 Bug
1. **活动栏按钮组图标未使用 AutoMirrored** — TopLevelDestination 中使用了 Icons.Filled.Home / Description / Refresh / CheckCircle，部分应为 AutoMirrored 版本
2. **Tab Bar 图标 Refresh 不直观** — 习惯 tab 使用 Refresh（刷新图标），更通用的习惯图标可能是 Loop（循环）

### P1 - 不一致
1. **TodayScreen 硬编码 padding** — `TodayHeroCard` 内部使用 `16.dp` 硬编码而不是 `LayoutTokens`（L142-143），与其他卡片不一致
2. **TasksScreen padding 硬编码** — L79 使用 `20.dp` 和 `12.dp` 而非 `LayoutTokens.ScreenHorizontalPadding` / `ScreenVerticalPadding`
3. **HabitsScreen** — 同样问题，L82 硬编码值
4. **NotesScreen** — L75 同样问题
5. **TaskDetail / HabitDetail / NoteDetail / Settings / Trash / Backup** — 全都有类似硬编码 padding
6. **Card padding 不统一** — TaskCard / HabitCard / NoteCard 使用 18.dp，而 TodayQuickCard 使用 layoutSpec.cardPadding（16dp/12dp）
7. **模组 Tab Bar 选中态** — 当前使用 pagerState.currentPage 而非 selectedDestination，会导致快速滑动时选中态闪烁

### P2 - 无障碍
1. **Checkbox contentDescription=null** — TaskCard 中 Checkbox 无 contentDescription
2. **IconButton contentDescription** — TopLevelSettingsButton 存在但其他图标按钮需确认

### P3 - 视觉打磨
1. **Editor bottom bar** — 返回按钮为 TextButton 而非带图标的 IconButton，视觉权重偏轻
2. **Settings 页面布局** — 开关 / 输入框密集排列，区隔感不足
3. **Backup 页面** — 两个 Button 完全一样的样式（filled），缺少视觉主次
4. **回收站空状态** — 打开回收站页面前需要先显示返回按钮，UX 流程可优化
5. **Detail 页面间距** — TaskDetail / HabitDetail / NoteDetail 的 SectionCard 间距 18dp 略松
6. **Today empty state** — 缺少图标层，视觉略空
7. **Snackbar 位置** — 在今日页中 FAB 可能遮挡 Snackbar

---

## 五、打磨优先级建议

### 第一轮：基础一致性（P1 修复）
- 所有页面统一使用 LayoutTokens
- 消除硬编码 padding
- 统一卡片内边距规范

### 第二轮：空状态与引导（P0 + P3）
- 检查和修复 AutoMirrored
- 优化 Tab Bar 图标语义
- 统一空状态视觉语言

### 第三轮：细节打磨（P3）
- Editor 导航改进
- Settings 区隔感
- 页面间距与视觉密度优化

### 第四轮：无障碍 + 收尾（P2）
- contentDescription 全覆盖
- 触摸目标检查
