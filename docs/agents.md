# AGENTS.md - 给 Codex 的执行规范

## 项目目标
开发一个 **Android 原生 Kotlin + Jetpack Compose** 的个人多功能记事软件 zou，包含：
- 任务
- 习惯
- 笔记

并实现：
- 子任务 / 步骤
- 重复提醒
- 特别提醒
- 免打扰时间
- Markdown + 图片
- 回收站
- 备份恢复

## 绝对约束
1. 必须使用 Kotlin。
2. 必须使用 Jetpack Compose。
3. 必须使用 Room、Hilt、Navigation Compose、WorkManager。
4. 包名前缀：`luzzr.*`
5. 所有构建必须使用项目签名密钥（`app/keystore.jks`）。
6. 首版必须本地优先，不实现云同步。
7. 删除默认做软删除。
8. 任何阶段都必须保持项目可编译、可运行。
9. 不要一次做完所有功能，必须按里程碑逐步实现。

## 开发方法
每次只处理一个里程碑或一个明确任务。  
改动时遵守：
- 先列计划
- 再修改
- 最后自检

## 输出格式要求
每次完成任务后，输出以下结构：
### 1. Plan
简要说明本轮要做什么。

### 2. Files Changed
列出修改/新增的文件。

### 3. What Was Implemented
说明完成了哪些功能。

### 4. Build / Test Result
写明：
- 是否编译通过
- 是否运行测试
- 哪些未验证

### 5. Risks / TODO
列出风险、待完善项、下一步建议。

## 代码风格
- 优先可读性
- 避免过度抽象
- 命名清晰
- 注释仅写必要原因，不写废话
- 不要生成无用占位代码

## 架构要求
- UI 层：Compose + ViewModel
- Domain 层：UseCase / Model / Repository Interface
- Data 层：Room + Repository 实现
- ReminderScheduler 统一处理提醒逻辑
- Settings 使用 DataStore

## 功能分批顺序
1. 项目初始化
2. 任务模块 MVP
3. 任务提醒
4. 习惯模块
5. 笔记模块
6. 今日页聚合
7. 免打扰
8. 回收站
9. 备份恢复
10. 测试与打磨

## 特别注意
- 任务与习惯的提醒逻辑不要分散写在页面中。
- Markdown 图片引用必须使用可恢复的本地路径映射方案。
- 对 Android 后台限制相关问题，要给出清晰 TODO。
- 不要默认引入复杂第三方编辑器；首版选择稳妥方案。
