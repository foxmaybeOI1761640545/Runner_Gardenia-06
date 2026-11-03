# Runner Gardenia 云端构建与自动预发布

只需提交并推送 `foxmaybeOI1761640545/Runner_Gardenia-06` 的 `main`。一个 Actions 运行顺序完成：

```text
prepare → checks → signed/APK 验证 → create-tag → publish
           Debug/Lint/Unit/API35                 Pre-release
```

不再 dispatch 第二个工作流，也不依赖 `GITHUB_TOKEN` 推送 Tag 触发事件。PR 和手动 CI 只执行 prepare/checks，不接触签名 Secrets、不创建 Tag、不发布。

## 版本与精确提交

- 正式包名 `com.fox.runner`；Debug 包名 `com.fox.runner.debug`。Java namespace 保留 `de.dennisguse.opentracks`。
- `version.properties` 的已提交内容定义版本系列与最低 Code；初始种子为 `v0.1.0-rc1.1` / `6766`。
- prepare 检查所有现有版本 Tag 和 `release-ledger.json` 历史分配记录：同系列取下一个预发布序号，Code 高于所有已分配版本（包括清理前已持久化的失败 Tag）。高水位、重复 Code 或存活 Tag 与历史冲突时失败退出。历史已分配版本不会重新使用；当前已发布 rc1.4 / 6769。
- 计算结果写入 `release-plan.json` Artifact；每个构建 Job 用同一计划生成本次 `version.properties`；`VERSION_NAME_FULL` 固定为计划版本和源码 SHA，避免创建 Tag 前后影响 About 信息或重跑产物。本地无该字段时保留 git-describe。Debug 和 Release 的 Version Name/Code 完全一致，Debug 只保留包名后缀，不再增加版本名称后缀。
- 不产生机器人版本提交，不改写 Git 历史。Tag 指向经过测试的准确源码 SHA；annotated Tag 内保存分配的版本、Code、SHA、run ID。Tag 内的版本计划及 Release 的 build-info.json 记录实际分发版本，不应把 Tag 源码树中的种子配置误认为本次分配结果。
- 仅在所有检查、报告门禁、Debug APK 上传、正式签名构建及 APK 身份/独立证书/SHA-256 验证、签名 Artifact 上传成功后创建 Tag。测试或签名失败均不会创建 Tag。Tag 注释另绑定已验证 APK 的 SHA-256。
- 同一次 run 重跑时，若已创建 Tag，恢复其版本计划，绝不移动、删除或覆盖 Tag。后续新提交自动计算下一个候选，不需要手动更新版本号。经授权删除无效 Tag 前，必须先扩充并提交历史 ledger，不能删除版本分配记录。

## 测试与时区

Ubuntu 24.04，Temurin 21，Java source/target 保持 17，Gradle Wrapper 9.8.2、AGP 9.5.0 不变。使用 SDK API 37、Build Tools 36.0.0，API 35 AOSP Gradle Managed Device。

Runner 使用 timedatectl 设置 Asia/Singapore；TZ 和 JAVA_TOOL_OPTIONS 同时明确设置，实际检查 JVM user.timezone。测试 Runner 通过临时 shell 权限设置模拟设备系统时区、关闭设备自动时区，测试检查 persist.sys.timezone 及进程默认时区均为 Asia/Singapore，2020 年测试时间偏移为 UTC+08:00。此设置仅在测试代码中，正式应用仍尊重设备系统时区。TimezoneRule 在 finally 中恢复默认值。

任务：

```text
clean assembleReproducibleDebug lintReproducibleDebug testReproducibleDebugUnitTest
mediumPhoneReproducibleDebugAndroidTest
clean assembleReproducibleRelease
```

仪器测试命令保留 `-Pandroid.testoptions.manageddevices.emulator.gpu=swiftshader_indirect`，并传入 `-Pandroid.testInstrumentationRunnerArguments.timezone=Asia/Singapore`。

报告分别统计 Tests、Failures、Errors、Skipped。缺失/无效 XML、声明数量与 testcase 不一致、重复测试、遗漏已配置测试类、必需回归测试被跳过、执行数量为零或存在失败/错误都会阻止发布。保留原有合理的 @Ignore；跳过不计为通过。新增 src/test 纯 Java 协议、状态机、扫描、图表与实际 JVM 时区测试；分别校验其 XML 和非零执行数，NO-SOURCE 或报告缺失不能通过。UI 门禁另校验 40 张真实模拟器截图（两种尺寸、亮/暗模式）。

## 固定签名与发布

signed Job 使用 `environment: android-release`，直接引用已有配置：

- Secrets：ANDROID_KEYSTORE_BASE64、ANDROID_KEYSTORE_PASSWORD、ANDROID_KEY_ALIAS、ANDROID_KEY_PASSWORD。
- Variable：ANDROID_SIGNING_CERT_SHA256。

不生成密钥、不打印密钥或密码。JKS 只恢复到 RUNNER_TEMP，权限 600，通过 EXIT trap 与 always 步骤清理；签名任务禁用 Gradle 缓存和 configuration cache。保留原有本地 -D 签名参数兼容。

验证脚本读取实际 output-metadata.json 定位唯一 APK，检查包名、版本、Code、debuggable=false、apksigner、独立证书 SHA-256 和快捷方式身份。快捷方式 XML 路径从 APK 资源表解析，并确认对应 ZIP 条目存在后才解码；不假设资源优化后仍保留源码中的文件名。

发布 Job 再次验证 APK 的校验和、签名和身份，创建 draft pre-release，上传 APK、SHA256SUMS、build-info.json，核对每项资产后再公开。固定 prerelease=true、make_latest=false。Release notes 包含准确提交、包名、版本、Code、指纹和 APK SHA-256。

网络中断可在 Actions 选择 Re-run failed jobs：

- 签名 Artifact 已存在：恢复同一 run 的原始字节，重新用 Environment 独立指纹及 apksigner/manifest 校验，不重建或覆盖。
- Tag 已存在：只接受同一 run、同一提交、同一版本计划及同一 APK SHA-256。原 Artifact 缺失/过期时明确失败，不静默替换。
- draft 已存在：校验身份后补齐缺失资产，已有不同内容的资产会失败退出，不覆盖。
- 已发布：下载并核对已有 APK、指纹、版本与校验文件，确认同一提交后直接成功，不替换 APK。
- 代码修复：正常提交到 main，自动分配新版本，不复用旧 Tag。

无需手动 Run workflow、Tag 或发布确认。Environment 当前无 Required reviewers/Wait timer；若后来添加审批，GitHub 会暂停签名任务。

## 下载和核对

Actions 运行的 Artifacts 包含版本计划、Debug APK、完整测试/Lint 报告、签名 APK 和发布回执。正式签名 APK 在 GitHub Releases 对应 Pre-release 页面下载。SHA256SUMS 是 APK 文件校验值；Artifact 压缩包摘要不是 APK 自身摘要。

后续安装保留固定包名/签名并提高 Code；不要卸载旧应用以保留数据。原始 JKS 应由维护者保留至少两份独立加密备份，GitHub Secrets 不是可恢复备份。

## 权限

默认 contents: read，仅 create-tag、publish 为 contents: write；signed 额外 actions: read 仅用于恢复本运行 Artifact。不使用 pull_request_target，不增加 PAT/GitHub App Token。Actions 固定完整 commit SHA。main 发布运行串行且不取消进行中的发布，PR 可取消过时运行。只缓存依赖，不缓存签名材料。工作流不改变固定签名、包名、依赖或正式应用设备时区行为。
