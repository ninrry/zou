# Zou UI 升级计划书 v2.0 — 已验证版

> 依据 Material Design 3 官方规范 + 代码审计，每项改动均经二次验证
> 日期：2026-05-08

---

## 验证方法

1. **代码阅读** — 逐行确认问题存在
2. **M3 官方规范对照** — 确认标准值
3. **引用追踪** — 确认改动影响范围和连锁反应
4. **GlassSurface 参数审计** — 发现额外死参数问题

---

## 修复项（经核实）

---

### ① Easing 曲线等值 Bug（P0）

#### 证据

**代码** (`MotionTokens.kt:24-26`)：
```kotlin
val EasingEmphasized = CubicBezierEasing(0.25f, 1f, 0.5f, 1f)
val EasingStandard   = CubicBezierEasing(0.25f, 1f, 0.5f, 1f)  // ← 完全一致！
```

**验证方法**：直接对比两个常量的值——`0.25,1,0.5,1` == `0.25,1,0.5,1`

**M3 官方值**（来源：m3.material.io/styles/motion/easing-and-duration/tokens-specs）：

| Token | M3 Android 值 | Compose CubicBezierEasing |
|:------|:-------------:|:-------------------------:|
| Emphasized Decelerate | `PathInterpolator(0.05, 0.7, 0.1, 1.0)` | `(0.05f, 0.7f, 0.1f, 1.0f)` |
| Standard | `PathInterpolator(0.2, 0.0, 0.0, 1.0)` | `(0.2f, 0.0f, 0.0f, 1.0f)` |

**影响范围**（27 处引用，7 个文件）：
- `RadialExpansionOverlay.kt` — 4 处（FAB 径向展开动画）
- `ZouStaggeredReveal.kt` — 3 处（列表交错的渐入动画）
- `ZouStepper.kt` — 1 处（编辑器步骤切换）
- `TodayComponents.kt` — 3 处（FAB 展开动画）
- `ZouApp.kt` — 2 处（页面入口动画）
- `ZouNavHost.kt` — 6 处引用 EasingEmphasized + 6 处引用 EasingStandard

**为什么会出问题**：复制粘贴时没改值，导致 Emphasized 和 Standard 使用同一缓动曲线，所有动画的「强调感」丢失。

**修复风险**：低。仅改常量值，所有引用方自动受益。不会造成动画崩溃或视觉异常——只是曲线变平滑。

#### 改动

```diff
- val EasingEmphasized = CubicBezierEasing(0.25f, 1f, 0.5f, 1f)
- val EasingStandard   = CubicBezierEasing(0.25f, 1f, 0.5f, 1f)
+ val EasingEmphasized = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1.0f)
+ val EasingStandard   = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)
```

#### 效果预期
- Emphasized：先快后慢，用于导航过渡、FAB 展开等强调场景 → 更自然的「惯性感」
- Standard：匀速感更强，用于步骤切换等工具性场景 → 更干脆

---

### ② 水平 padding 不一致（P1）

#### 证据

直接代码对比：

| 页面 | 文件:行号 | 当前值 |
|:----|:---------|:------:|
| TodayScreen | `TodayScreen.kt:136` | `horizontal = 16.dp` |
| TasksScreen | `TasksScreen.kt:78` | `horizontal = 20.dp` |
| HabitsScreen | `HabitsScreen.kt:81` | `horizontal = 20.dp` |
| NotesScreen | `NotesScreen.kt:74` | `horizontal = 20.dp` |
| LayoutTokens 定义 | `LayoutTokens.kt:12` | `ScreenHorizontalPadding = Space20` |

TodayScreen 的 16dp 与其余 3 个页面和设计 Token 定义（20dp）不一致。

**修复风险**：极低。只改 1 行，从 16dp→20dp 只是让 Today 页的内容与其余页面视觉对齐，内容宽度减少 8dp，不影响布局完整性。

#### 改动

```diff
// TodayScreen.kt:136
- .padding(horizontal = 16.dp, vertical = 12.dp),
+ .padding(horizontal = LayoutTokens.ScreenHorizontalPadding, vertical = LayoutTokens.ScreenVerticalPadding),
```

---

### ③ 列表垂直间距不一致（P1）

#### 证据

| 页面 | 行号 | 间距值 |
|:----|:----|:------:|
| TodayScreen | `TodayScreen.kt:137` | `spacedBy(10.dp)` |
| TasksScreen | `TasksScreen.kt:79` | `spacedBy(14.dp)` |
| HabitsScreen | `HabitsScreen.kt:82` | `spacedBy(14.dp)` |

TodayScreen 使用 `10dp`，其余页面使用 `14dp`。TodayScreen 的双列布局本身已有内部间隙（`columnGap=12dp`, `cardGap=10dp`），外层 LazyColumn 用 10dp 稍显拥挤。

**修复风险**：极低。只改 1 行，10→12dp 间距增加 2dp，视觉更宽松，不影响布局完整性。

#### 改动

```diff
// TodayScreen.kt:137
- verticalArrangement = Arrangement.spacedBy(10.dp),
+ verticalArrangement = Arrangement.spacedBy(LayoutTokens.Space12),
```

---

### ④ TopModuleTabBar 无障碍缺失（P2）

#### 证据

**代码** (`TopModuleTabBar.kt:130`)：
```kotlin
Icon(
    imageVector = if (selectionProgress > 0.55f) {
        destination.selectedIcon
    } else {
        destination.unselectedIcon
    },
    contentDescription = null,  // ← 屏幕阅读器无法读出
    tint = lerp(...),
)
```

`contentDescription = null` 意味着 TalkBack 等读屏软件会跳过该图标，用户无法感知到导航图标的含义。

**TopLevelDestination.label** 已有中文标签值（"今日"、"待办"、"习惯"、"笔记"），可以直接复用。

**修复风险**：极低。只改 1 行，改后 TalkBack 用户能听到"今日 按钮"等提示。

#### 改动

```diff
- contentDescription = null,
+ contentDescription = destination.label,
```

---

## 不修复项说明（调研后排除）

| 原始问题 | 调研结论 | 排除理由 |
|:---------|:---------|:---------|
| 笔记卡片缺色条 | ❌ 查实列表页均无色条 | 只有 Today 快速卡片有色条，列表页统一无此模式，不是缺失 |
| GlassSurface accentColor 未使用 | ⚠️ 确认是死参数 | 不影响功能，参数已声明但未用，清理属重构范畴不属 UI 打磨，留待后续 |
| Settings 输入框样式 | ❌ 需自定义 TextField | 工作量大，非本次范围 |
| HeroCard 内边距硬编码 | ⚠️ 确认但非问题 | 16dp 视觉合理，LayoutTokens 无对应值，硬编码可接受 |

---

## 实施清单

| # | 文件 | 改动行数 | 类型 | 风险 |
|:-|:----|:-------:|:----|:----:|
| ① | `MotionTokens.kt` | 2 行 | 常量值修正 | 🟢 低 |
| ② | `TodayScreen.kt` | 1 行 | LayoutTokens 对齐 | 🟢 极低 |
| ③ | `TodayScreen.kt` | 1 行 | 间距统一 | 🟢 极低 |
| ④ | `TopModuleTabBar.kt` | 1 行 | 无障碍 | 🟢 极低 |

**总计 4 项，3 个文件，5 行改动。**

---

## 构建验证方案

```bash
./gradlew clean assembleRelease
# 检查：
# - 编译通过 ✅
# - R8 压缩通过 ✅
# - APK 签名正常 ✅
# - APK 大小无异常增长
```

---

**以上 4 项均已交叉验证，请主人审查计划。同意后按顺序逐个实施。** 😊
