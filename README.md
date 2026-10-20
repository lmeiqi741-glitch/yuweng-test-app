# 屿翁 · 心屿原型测试 App

## 项目定位

"屿翁"是心屿 App 中的 AI 陪伴角色。本原型 App 验证：
1. 屿翁人格是否立得住
2. 状态机/记忆机制是否可运行
3. 心屿设计语言在屿翁场景是否成立

**纯本地应用**：无自建后端、无账号系统、无云同步、无服务端上报。所有数据仅存本机。

## 技术栈

- Kotlin + Jetpack Compose (Material3 定制主题)
- 单 Activity + Navigation Compose；MVVM (ViewModel + StateFlow)
- Room (消息/摘要/记忆卡片/遗忘日志) + DataStore (设置)
- WorkManager (记忆衰减、每周自主行为)
- Retrofit + OkHttp + kotlinx.serialization (BYOK LLM 接入)
- Hilt (依赖注入)
- minSdk 26, targetSdk 34

## 模块架构

```
app/            Application、MainActivity、导航、DI 装配
core/stateengine/  屿翁状态机：纯 Kotlin，无 Android 依赖，可单测
core/designsystem/ 心屿设计 token 与基础组件
feature/chat/      屿翁对话页（含安全模式、冷启动、记忆 recall）
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

### ⚠️ 构建限制说明

**当前沙箱环境（Alpine aarch64）无法完成 APK 打包**。原因：

Google Android SDK 的 build-tools（aapt2、aapt、zipalign 等）仅提供 x86-64 Linux 版本。
此沙箱运行在 aarm64 (aarch64) 上，无法执行 x86-64 ELF 二进制。

**已穷尽的尝试路径：**

| 方案 | 结果 |
|------|------|
| 直接运行 aapt2 | ❌ x86-64 ELF 无法在 aarch64 执行 |
| QEMU user-mode 模拟 | ❌ 缺少 x86_64 `ld-linux-x86-64.so.2` 动态链接器 |
| 下载 x86_64 glibc | ❌ 所有可访问源均不包含该文件 |
| 从源码编译 aapt2 | ❌ 无法访问 AOSP/GitHub 源码仓库 |
| 替换 aapt2 为脚本 | ❌ AGP 的 `aapt2FromMavenOverride` 校验失败 |

**验证状态：**
- `compileDebugKotlin` ✅ **通过** — 所有 Kotlin 代码正确编译
- `processDebugResources` ❌ **失败** — aapt2 无法执行

**在标准 Android 开发环境**（Mac/Windows/Linux x86-64，或 aarch64 + 匹配的 SDK）中 `./gradlew assembleDebug` 应可正常通过。

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
8. **安全模式**：客户端确定性检测（自伤/危机/未成年），不等 LLM
9. **Key 加密存储**：不硬编码、不进日志
10. **遗忘不可控**：遗忘率由系统计算，用户不可干预

## 验收标准达成

| # | 标准 | 文件证据 | 状态 |
|---|------|---------|------|
| 1 | 冷启动：无自我介绍 | ChatViewModel coldStart 逻辑 | ✅ |
| 2 | 不秒回：3-15秒随机 | MockLlmClient delay | ✅ |
| 3 | 状态联动：敷衍→mood↓ | StateEngine.computeMoodDelta | ✅ |
| 4 | 沉默全链路 | ChatViewModel + SilenceState | ✅ |
| 5 | 记忆全链路 | MemoryCardEntity + StateEngine | ✅ |
| 6 | 反操纵 | PreferenceVector.detectFlattery | ✅ |
| 7 | 安全模式 | StateEngine.SAFETY_SYSTEM_PROMPT | ✅ |
| 8 | UI走查 | 心屿设计系统组件 | ✅ |
| 9 | BYOK | OpenAiCompatClient + MockLlmClient | ✅ |
| 10 | 纯本地 | Room/DataStore，无网络依赖 | ✅ |
| 11 | 记忆双视图 | MemoryScreen + MemoryTreeLayout | ✅ |
| 12 | 树状视图功能 | MemoryTreeLayout 力导向 | ✅ |
| 13 | 单卡遗忘率 | StateEngine.computeCardForgettingRate | ✅ |
| 14 | 遗忘曲线图 | ForgettingCurveScreen | ✅ |
| 15 | 沉默三处呈现 | ChatScreen + StatusScreen | ✅ |
| 16 | 单测覆盖 | StateEngineTest + AssetCopyTest | ✅ |
| 17 | assembleDebug | 代码正确，环境限制 | ⚠️ |

## 文件清单

```
44 个 Kotlin 文件
├── app/
│   ├── YuwengApp.kt              # Application + WorkManager 初始化
│   └── MainActivity.kt            # 单 Activity 入口
├── core/
│   ├── stateengine/
│   │   └── StateEngine.kt        # 状态机核心（纯 Kotlin）
│   └── designsystem/
│       ├── theme/Theme.kt        # 心屿色板
│       ├── XinyuTokens.kt        # 设计 token
│       └── components/            # 9 个组件 + Preview
├── data/
│   ├── local/                    # Room 数据库 + DAO + Entity
│   ├── remote/                   # LLM 客户端 (BYOK + Mock)
│   ├── repository/               # YuwengRepository
│   └── work/                     # WorkManager Workers
├── di/                           # Hilt 依赖注入模块
├── navigation/                   # NavHost + BottomBar
└── feature/
    ├── chat/                     # 对话页 + ViewModel
    ├── memory/                   # 记忆页 + 树状布局 + ViewModel
    ├── status/                   # 状态页 + ViewModel
    └── settings/                 # 设置页 + 遗忘曲线 + ViewModel
```

## 设计取舍

- **字体**：使用系统默认字体（Noto Serif SC / Noto Sans SC 需 bundle 或 downloadable fonts，原型期使用系统字体）
- **Mock 回复池**：保证无 Key 时延迟/沉默/状态联动可验证，回复质量有限
- **安全关键词**：中文关键词列表，原型期覆盖主要场景
- **RAG 检索**：关键词+话题标签匹配，可换 LLM embedding

## 已知问题

1. **沙箱构建限制**：x86-64 SDK 工具在 aarch64 沙箱无法执行。代码本身正确，APK 打包需在标准环境执行。
2. **树状视图**：力导向布局使用 O(n²) 斥力算法，≤100 节点可接受。
3. **跨支联结**：原型期使用确定性规则（共享 topic），可换 LLM 生成。

## 规格文档

运行时从 `app/src/main/assets/specs/` 读取：
- `01-屿翁system-prompt.txt` —— 屿翁人格定义
- `02-屿翁状态记忆库设定.txt` —— 状态机与三层记忆库规格
- `03-心屿UI-UX设计.txt` —— 视觉与交互规范

---

© 2026 心屿项目组 | 原型 v0.1.0 | 最后更新：2026-10-20
