# Zou App 交互与动画打磨计划书

> 基于 M3 Motion 标准（m3.material.io/styles/motion）和 Jetpack Compose 最新 API
> 环境: composeBom=2026.02.01, compileSdk=36, targetSdk=36
> 验证: 所有问题均通过实际代码行号确认

---

## 总览

| 优先级 | 项目数 | 涉及文件 | 新增代码 | 风险 |
|:-----:|:------:|:--------:|:--------:|:----:|
| 🔴 P0 | 5 | ~12 | ~210行 | 中 |
| 🟡 P1 | 5 | ~7 | ~120行 | 低 |
| 🟢 P2 | 3 | ~6 | ~150行 | 低 |
| **合计** | **13** | **~20** | **~480行** | — |

---

## 🔴 P0 — 核心交互缺失

### P0-1: animateItem() → 列表增删动画

**证据**：6 个 LazyColumn 均无 `animateItem()` modifier
- `TasksScreen.kt:75` — `LazyColumn(...)`
- `HabitsScreen.kt:78` — `LazyColumn(...)`
- `NotesScreen.kt:71` — `LazyColumn(...)`
- `TodayScreen.kt:133` — `LazyColumn(...)`
- `TrashScreen.kt:106` — `LazyColumn(...)`
- `ZouDateTimeSheet.kt:651` — `LazyColumn(...)`

**M3 标准**：Compose Foundation 推荐每个 LazyColumn 的 items 使用 `Modifier.animateItem()`，使增删重排有平滑动画。

**改动 before/after**：

```kotlin
// Before
LazyColumn(...) {
    items(items = list, key = { it.id }) { item ->
        ItemCard(...)
    }
}

// After
LazyColumn(...) {
    items(items = list, key = { it.id }) { item ->
        ItemCard(
            modifier = Modifier.animateItem(
                fadeInSpec = MotionTokens.SpringSmooth,
                placementSpec = MotionTokens.SpringSmooth,
            )
        )
    }
}
```

**影响**：6 个文件，每处加 `Modifier.animateItem()`。需在所有 `item{}` 块使用 `key` 参数（已有 key）。低风险纯增量改动。

---

### P0-2: AnimatedVisibility 包裹列表项 → 删除/完成淡出

**证据**：整个项目只有 2 处 `AnimatedVisibility`（ZouStaggeredReveal 和 TodayComponents 的快速创建），没有用于列表项的移除动画。完成任务/删除笔记时瞬间消失。

**M3 标准**：移除动画让用户感知状态变化，符合 M3 Motion 的 "meaningful transitions" 原则。

**方案**：在 ViewModel 中为每个列表项维护 `removingIds: Set<String>`，删除时先加入 set → 触发 exit animation → 动画结束后真正删除。

```kotlin
// ViewModel 中
private val _removingIds = MutableStateFlow<Set<String>>(emptySet())
val removingIds: StateFlow<Set<String>> = _removingIds.asStateFlow()

fun deleteTask(taskId: String) {
    _removingIds.update { it + taskId }
    viewModelScope.launch {
        delay(300) // 匹配退出动画时长
        deleteTaskUseCase(taskId)
        _removingIds.update { it - taskId }
    }
}
```

```kotlin
// Composable 中
LazyColumn {
    items(items = tasks, key = { it.id }) { task ->
        AnimatedVisibility(
            visible = task.id !in viewModel.removingIds.collectAsState().value,
            exit = fadeOut(animationSpec = tween(250)) +
                    shrinkVertically(animationSpec = tween(250)) { it / 2 },
        ) {
            TaskCard(modifier = Modifier.animateItem(...))
        }
    }
}
```

**影响**：TasksScreen/TodayComponents + 对应 ViewModel。中等风险（需测试动画时序），但体验增益大。

---

### P0-3: SwipeToDismissBox → 滑动操作

**证据**：0 处 `SwipeToDismiss` 或 `SwipeToDismissBox`。

**M3 标准**（developer.android.com/develop/ui/compose/touch-input/user-interactions/swipe-to-dismiss）：
`SwipeToDismissBox` 提供标准滑动手势交互。Key API:
- `rememberSwipeToDismissBoxState(confirmValueChange = {...})`
- `SwipeToDismissBox(state, backgroundContent = {...}) { content }`
- `swipeToDismissBoxState.progress` 用于动画背景色
- `SwipeToDismissBoxValue.StartToEnd` / `EndToStart` / `Settled`

**方案**：
- TasksScreen: 右滑(StartToEnd)→完成，左滑(EndToStart)→删除
- HabitsScreen: 右滑→完成
- NotesScreen: 左滑→删除

```kotlin
// Before
GlassSurface(modifier = Modifier.noteFlowPressScale(...)) {
    TaskItemContent(...)
}

// After
val dismissState = rememberSwipeToDismissBoxState(
    confirmValueChange = {
        when (it) {
            StartToEnd -> { onToggleDone(task.id); false /* 复位 */ }
            EndToStart -> { onRemove(task.id); true }
            Settled -> false
        }
    }
)

SwipeToDismissBox(
    state = dismissState,
    backgroundContent = {
        when (dismissState.dismissDirection) {
            StartToEnd -> SwipeBgIcon(
                icon = Icons.Default.Check,
                color = Color(0xFF4CAF50),
                align = Alignment.CenterStart,
                progress = dismissState.progress,
            )
            EndToStart -> SwipeBgIcon(
                icon = Icons.Default.Delete,
                color = Color(0xFFE53935),
                align = Alignment.CenterEnd,
                progress = dismissState.progress,
            )
            Settled -> {}
        }
    },
) {
    GlassSurface(modifier = Modifier.noteFlowPressScale(...)) {
        TaskItemContent(...)
    }
}
```

**影响**：TasksScreen, HabitsScreen, NotesScreen。需要测试手势与现有点击交互的兼容性。

---

### P0-4: PullToRefreshBox → 下拉刷新

**证据**：0 处 `pullRefresh` / `PullToRefreshBox` / `isRefreshing`。

**M3 标准**（developer.android.com/reference/kotlin/androidx/compose/material3/pulltorefresh）：
`PullToRefreshBox` 是 M3 1.3+ 的标准下拉刷新组件：
```kotlin
PullToRefreshBox(
    isRefreshing = isRefreshing,
    onRefresh = { viewModel.refresh() },
) {
    LazyColumn { ... }
}
```

**方案**：在 Today/Tasks/Habits/Notes 的 Scaffold content 外层包裹 `PullToRefreshBox`。

```kotlin
// Before
Scaffold(...) { innerPadding ->
    LazyColumn(modifier = Modifier.padding(innerPadding)) { ... }
}

// After
Scaffold(...) { innerPadding ->
    PullToRefreshBox(
        isRefreshing = uiState.isRefreshing,
        onRefresh = onRefresh,
        modifier = Modifier.padding(innerPadding),
    ) {
        LazyColumn { ... }
    }
}
```

**前提**：需要在 ViewModel 中添加 `isRefreshing` 状态和 `refresh()` 方法（或使用已有刷新逻辑）。

```kotlin
// ViewModel 添加
private val _isRefreshing = MutableStateFlow(false)
val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

fun refresh() {
    viewModelScope.launch {
        _isRefreshing.value = true
        loadItems()
        _isRefreshing.value = false
    }
}
```

**影响**：4 个 Screen + 4 个 ViewModel。需要 BOM 2026.02.01 确保 `PullToRefreshBox` 可用（确认可用）。

---

### P0-5: 恢复水波纹反馈 (indication)

**证据**：9 处 `indication = null`，禁用了 Material ripple 触摸反馈：

| 文件 | 行号 | 说明 |
|:----|:----|:----|
| `TasksScreen.kt` | 125 | Task card combinedClickable |
| `HabitsScreen.kt` | 152 | Habit card combinedClickable |
| `NotesScreen.kt` | 99 | Note card clickable |
| `TodayComponents.kt` | 233, 272, 846 | Today hero/section clickable |
| `TopModuleTabBar.kt` | 115 | Tab bar items |
| `ZouDateTimeSheet.kt` | 468, 664 | Date/time picker items |

**M3 标准**：Material Design 中的所有 interactive element 应有 ripple indication 作为视觉反馈。移除 `indication = null` 让系统使用默认 `rememberRipple()`。

**方案**：因为 `noteFlowPressScale` 已提供自定义 `scale` 反馈，可以保留两个并存：ripple + scale。
做法是移除 `indication = null` 或将 `combinedClickable(..., indication = null)` 改为 `combinedClickable(indication = rememberRipple())`。

```kotlin
// Before
.combinedClickable(
    interactionSource = interactionSource,
    indication = null,
    onClick = onClick,
)

// After
.combinedClickable(
    interactionSource = interactionSource,
    indication = rememberRipple(bounded = true), // 恢复水波纹
    onClick = onClick,
)
```

**影响**：9 个位置。低风险，测试触摸反馈视觉效果即可。注意 `noteFlowPressScale` 的 `scale` 和 ripple 同时存在不会冲突（ripple 是颜色层，scale 是缩放层）。

---

## 🟡 P1 — 动画细节润色

### P1-6: 硬编码动画值 → MotionTokens

**证据**：`TodayComponents.kt:778-791`：
```kotlin
// Line 778
durationMillis = 220, // → 应使用 MotionTokens.DurationShort (200)
// Line 784
durationMillis = 300, // → 应使用 MotionTokens.DurationMedium (340)
// Line 791
durationMillis = 280, // → 应使用 MotionTokens.DurationMedium (340)
```

**改动**：
```kotlin
// Line 778
durationMillis = MotionTokens.DurationShort,
// Line 784
durationMillis = MotionTokens.DurationMedium,
// Line 791
durationMillis = MotionTokens.DurationMedium,
// 注意: hardcoded exit 值 (120, 160, 140) 也需替换
// Line 797
fadeOut(animationSpec = tween(MotionTokens.DurationShort))
// Line 799
slideOutVertically(animationSpec = tween(MotionTokens.DurationShort))
// Line 803
scaleOut(animationSpec = tween(MotionTokens.DurationShort))
```

**影响**：1 个文件。低风险，纯符号替换。

---

### P1-7: 进度条动画过渡

**证据**：`TodayComponents.kt:639` → `clampedProgress` 直接用于 `fillMaxWidth()`，值变化时宽度跳变。

```kotlin
// Before
val clampedProgress = progress.coerceIn(0f, 1f)
Box(modifier = Modifier.fillMaxWidth(clampedProgress.coerceAtLeast(0.06f)))
```

**改动**：
```kotlin
// After
val clampedProgress = progress.coerceIn(0f, 1f)
val animatedProgress by animateFloatAsState(
    targetValue = clampedProgress.coerceAtLeast(0.06f),
    animationSpec = MotionTokens.SpringSmooth,
    label = "today_progress_bar",
)
Box(modifier = Modifier.fillMaxWidth(animatedProgress))
```

**影响**：1 个文件，加一个 `animateFloatAsState`。低风险。

---

### P1-8: 复选框自定义动画

**证据**：`TasksScreen.kt:160` — 使用标准 `Checkbox`，选中/取消瞬间切换，无品牌动画。

**M3 标准**：`Checkbox` 默认有简短的选中/取消动画，但可以进一步定制。在现有代码中，我们可以在 item 的 `isCompleted` 变化时对卡片整体添加淡化/缩放效果。

**方案**：对已完成任务添加行修饰效果：

```kotlin
// After — 在 TaskCard 中
val cardAlpha by animateFloatAsState(
    targetValue = if (item.isCompleted) 0.7f else 1f,
    animationSpec = MotionTokens.SpringSmooth,
    label = "task_card_alpha",
)

// 应用到卡片修饰符
graphicsLayer { alpha = cardAlpha }
```

**影响**：TasksScreen.kt，低风险。

---

### P1-9: FAB 微交互动画

**证据**：`ModuleFab.kt:39-43` — `iconRotation` 的 targetValue 硬编码为 `0f`，永不旋转。

```kotlin
// Before
val iconRotation by animateFloatAsState(
    targetValue = 0f,
    animationSpec = MotionTokens.SpringBouncy,
    label = "module_fab_icon_rotation",
)
```

**改进方案**：为 FAB 添加点击时的旋转反馈。需要在 `ModuleFab` 中添加旋转状态：

```kotlin
// After — 方案1: FAB 点击时旋转 45° 再弹回
var isSpinning by remember { mutableStateOf(false) }
val iconRotation by animateFloatAsState(
    targetValue = if (isSpinning) 45f else 0f,
    animationSpec = MotionTokens.SpringBouncy,
    label = "module_fab_icon_rotation",
)

// onClick 中
onClick = {
    isSpinning = true
    // 延迟复位让动画可见
    coroutineScope.launch {
        delay(300)
        isSpinning = false
    }
    // 原有逻辑
    ...
}
```

**影响**：ModuleFab.kt，低风险。

---

### P1-10: 触觉反馈 (HapticFeedback)

**证据**：全 app 无 `LocalHapticFeedback` 使用。

**M3 标准**：关键操作用触觉反馈增强交互感知。

```kotlin
// Compose API
val hapticFeedback = LocalHapticFeedback.current

// 在点击回调中
onClick = {
    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
    actualAction()
}
```

**方案**：在以下位置添加触觉反馈：
1. SwipeToDismissBox 触发完成/删除时
2. FAB 点击时
3. 复选框点击时

**影响**：低风险，新增 ~5 行。

---

## 🟢 P2 — 高级体验

### P2-11: 空状态入场动画

**证据**：`ZouEmptyStateCard.kt` — 纯静态 Box，无任何入场过渡。

```kotlin
// Before: ZouEmptyStateCard 直接显示内容
Column(...) { icon, title, description, button }

// After: 外层包装 AnimatedVisibility
AnimatedVisibility(
    visible = true,
    enter = fadeIn(animationSpec = tween(MotionTokens.DurationMedium)) +
            scaleIn(initialScale = 0.92f, animationSpec = tween(MotionTokens.DurationMedium)),
) {
    Column(...) { icon, title, description, button }
}
```

**影响**：ZouEmptyStateCard.kt，1 个文件。

---

### P2-12: animateContentSize → 展开动画

**证据**：0 处 `animateContentSize` 使用。卡片展开/收起时尺寸突变。

**M3 标准**：内容区域变化时应用平滑过渡。

**方案**：在可展开的卡片上添加：
```kotlin
Modifier.animateContentSize(animationSpec = MotionTokens.SpringSmooth)
```

**适用位置**：
1. Task detail 中的子任务展开区域
2. TodayComponents 中的展开卡片

**影响**：2-3 个文件，低风险。

---

### P2-13: Loading 骨架屏

**证据**：加载状态仅使用 `CircularProgressIndicator`，内容区域空白。`TaskDetailScreen.kt:50-57`、`TaskEditorScreen.kt:231-238`。

**方案**：创建 `ZouShimmer` 组件替代纯加载圈：

```kotlin
@Composable
fun ZouShimmer(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition()
    val alpha by transition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse,
        ),
    )
    Box(
        modifier = modifier
            .background(Color.LightGray.copy(alpha = alpha))
            .clip(RoundedCornerShape(12.dp)),
    )
}
```

**适用位置**：在加载态时显示 3-4 个 `ZouShimmer` 卡片占位，而不是白屏 + 转圈。

**影响**：新组件 + 4 个 Screen 文件。

---

## 风险与注意事项

1. **SwipeToDismissBox 与现有点击冲突** — `SwipeToDismissBox` 处理水平滑动手势，`clickable`/`combinedClickable` 处理垂直点击，两者可共存。但需测试边界情况。
2. **animateItem() 性能** — 大量数据时动画帧率可能下降。`SpringSmooth`（damping=1, stiffness=300）应足够轻量。
3. **PullToRefreshBox 嵌套滚动** — 需要确保 `LazyColumn` 的垂直滚动和 `PullToRefreshBox` 的下拉手势协调。
4. **indication 恢复** — 某些自定義交互可能故意禁用了 ripple，恢复前需确认（当前 9 处 `indication=null` 全部在 `combinedClickable`/`clickable` 中，结合 `noteFlowPressScale` 使用，恢复 ripple 可增强反馈无副作用）。

---

## 实施顺序建议

```
Round 1 (P0) — 最影响体验的交互缺失
  ├── P0-1: animateItem() → 6 个 LazyColumn
  ├── P0-2: AnimatedVisibility 删除淡出 → TasksScreen + ViewModel
  ├── P0-3: SwipeToDismissBox → TasksScreen
  └── P0-5: Restore ripple → 9 个位置

Round 2 (P0 + P1) — 剩余核心 + 细节润色
  ├── P0-4: PullToRefreshBox → 4 个 Screens
  ├── P1-6: 硬编码值 → MotionTokens
  ├── P1-7: 进度条动画
  ├── P1-9: FAB 旋转
  └── P1-10: 触觉反馈

Round 3 (P2) — 高级体验
  ├── P2-11: 空状态入场
  ├── P2-12: animateContentSize
  └── P2-13: Loading 骨架屏
```

---

## 编译验证

每次修改后运行：
```bash
./gradlew assembleDebug --no-daemon -q 2>&1 | tail -20
```

确保编译通过后安装截图验证效果。
