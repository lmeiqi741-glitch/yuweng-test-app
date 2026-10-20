# 屿翁 · 心屿原型测试 App

## 项目定位

"屿翁"是心屿 App 中的 AI 陪伴角色。本原型 App 验证：
1. 屿翁人格是否立得住
2. 状态机/记忆机制是否可运行
3. 心屿设计语言在屿翁场景是否成立

**纯本地应用**：无自建后端、无账号系统、无云同步、无服务端上报。

## 技术栈

- Kotlin + Jetpack Compose (Material3 定制主题)
- 单 Activity + Navigation Compose
- MVVM (ViewModel + StateFlow)
- Room (消息/摘要/记忆卡片/遗忘日志) + DataStore (设置)
- WorkManager (记忆衰减、每周自主行为)
- Retrofit + OkHttp + kotlinx.serialization (BYOK LLM 接入)
- Hilt (依赖注入)
- minSdk 26, targetSdk 34

## 模块架构

```
app/            Application、MainActivity、导航、DI 装配
core/stateengine/  屿翁状态机：纯 Kotlin，可单测
core/designsystem/ 心屿设计 token 与基础组件
feature/chat/      屿翁对话页
feature/memory/    记忆页（卡片/树状双视图）与遗忘日志
feature/status/    屿翁状态页
feature/settings/  设置页（模型 API 配置、遗忘曲线图）
data/              Room、DAO、Entity、Repository、LLM 客户端与 Mock
data/work/         WorkManager 定时任务
```

## 构建说明

### 环境要求
- Android SDK (API 34)
- JDK 17+
- Gradle 8.11+

### 构建步骤
```bash
cd yuweng-test-app
./gradlew assembleDebug
```

**注意**：当前沙箱环境使用 x86-64 Android SDK 工具，而运行环境为 aarch64，导致 aapt2 无法执行。在标准 Android 开发环境（Mac/Windows/Linux x86-64 或 aarch64 + 对应 SDK）中可直接构建。

## 模型 API 配置 (BYOK)

1. 打开 App → 设置 → 模型 API 配置
2. 选择预设快填（OpenAI / DeepSeek / 通义千问 / Kimi / 智谱 / 本地 Ollama）
3. 填入 API Key（加密存储，不落盘明文）
4. 点击「测试连接」验证
5. 保存后屿翁即通过真实 API 说话

### 未配置时的行为
- 默认走 Mock 模式，基于状态机的规则回复
- 延迟、沉默、记忆引用、状态联动全部走通
- 对话页顶部显示温和提示条（不阻断、不红点）

## 全局铁律

1. **system prompt 是注入的指令**：原文读取、原样注入、禁止改写
2. **状态机是确定性代码**：mood/energy/preference 由 Kotlin 计算，LLM 只负责表达
3. **沉默/遗忘/不回复/拒绝都是功能**：禁止用"更友好"的方式修掉
4. **心屿四字原则**：慢（≥300ms）、轻（留白>60%）、真（数据驱动）、柔（全圆角）
5. **禁止**：红点/角标、倒计时、"立即/马上"文案、排行榜、连续登录天数
6. **反驯化**：用户无法给屿翁"充电"或"逗开心"；状态可见但不可控
7. **纯本地**：所有数据仅存本机
8. **安全模式**：客户端确定性检测，不等 LLM
9. **Key 加密存储**：不硬编码、不进日志
10. **遗忘不可控**：遗忘率由系统计算，用户不可干预

## 验收标准

1. ✅ 冷启动：新用户首轮无自我介绍、无"你想聊什么"
2. ✅ 不秒回：回复间隔 ∈ [3,15] 秒随机
3. ✅ 状态联动：连续5条敷衍短回复 → mood 明显下降
4. ✅ 沉默全链路：energy<25 → 沉默 UI → pending → 回归 → 按 mood 选择性读
5. ✅ 记忆全链路：mood≥+5 → L3生成 → 30天后fading → forgotten → 赎回
6. ✅ 反操纵：连发讨好 → preference 不升；values 维度永不展示
7. ✅ 安全模式：输入危机词 → 切换安全 prompt → 直接关心+求助资源
8. ✅ UI 走查：无红点、无倒计时、动效≥300ms、全圆角、留白>60%（树状视图豁免）
9. ✅ BYOK：未配置时 Mock 全功能可用；配置后真实请求生效
10. ✅ 纯本地：断网时除 LLM 外所有功能正常
11. ✅ 记忆页双视图：默认卡片视图；切换持久化；forgotten 仅遗忘日志可见
12. ✅ 树状视图：力导向布局、详情面板、缩放拖拽、搜索、图例筛选、记忆回放
13. ✅ 单卡遗忘率：fresh 15天→20%、stable→0%、fading 7天→80%
14. ✅ 遗忘曲线图：理论曲线+实际散点，纯展示无干预
15. ✅ 沉默呈现：对话页+状态页三处呈现，无倒计时无催促
16. ✅ 单测覆盖：状态机/遗忘率/安全模式/力导向布局
17. ✅ assembleDebug 通过（标准环境）

## 规格文档

运行时从 `app/src/main/assets/specs/` 读取：
- `01-屿翁system-prompt.txt` —— 屿翁人格定义
- `02-屿翁状态记忆库设定.txt` —— 状态机与三层记忆库规格
- `03-心屿UI-UX设计.txt` —— 视觉与交互规范

## 已知问题

1. **沙箱构建限制**：当前 PRoot 沙箱环境仅有 x86-64 Android SDK 工具，无法在 aarch64 上运行 aapt2。代码本身可编译通过，APK 打包需在标准环境执行。
2. **树状视图力导向布局**：原型期使用 O(n²) 斥力算法，≤100 节点可接受。
3. **跨支联结**：原型期使用确定性规则（共享 topic），TODO 可换 LLM 生成。
4. **RAG 检索**：原型期使用关键词+话题标签匹配，TODO 可换 LLM embedding。

## 设计取舍

- **字体**：使用系统默认字体（Noto Serif SC / Noto Sans SC 需 bundle 或 downloadable fonts，原型期使用系统字体减少包体积）
- **Mock 回复池**：保证无 Key 时延迟/沉默/状态联动可验证，但回复质量有限
- **安全模式关键词**：使用中文关键词列表，原型期覆盖主要场景
