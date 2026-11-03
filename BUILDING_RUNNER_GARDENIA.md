# Runner Gardenia 云端构建与自动预发布

仓库：`foxmaybeOI1761640545/Runner_Gardenia-06`。所有 Android 构建和测试均在 GitHub Actions 完成；Windows 和 Codex Cloud 无需安装 Android SDK。

## 应用身份与版本

正式变体为 `reproducibleRelease`，包名 `com.fox.runner`。保留 Java namespace `de.dennisguse.opentracks`，它是源码类名空间，不是安装包名。Debug 包为 `com.fox.runner.debug`；Nightly 包为 `com.fox.runner.nightly`。Provider、快捷方式及自有 Intent action 随应用包名绑定。

唯一发布版本来源为 `version.properties`。当前首个候选版本为 `v0.1.0-rc1.1`、Version Code `6766`。每一个已发布版本的 Version Code 必须严格递增，不能依赖 Git 提交数量或 Actions 运行次数。

建议版本递进：
- 首个测试候选：`v0.1.0-rc1.1` / Code `6766`。
- 同一 RC 系列的下一次测试：`v0.1.0-rc1.2` / Code `6767`，之后依次递增。
- 新功能阶段可进入下一个版本系列，例如 `v0.2.0-rc1.1`；Version Code 仍在上一已发布版本基础上递增。
- 所有 GitHub Release 均标记为 **Pre-release**，并显式设置 `latest=false`；不自动设为 Latest。

## 自动工作流

### 1. Pull Request 和 main 推送：Debug CI

- PR 和 `main` 推送自动运行 Debug APK 构建、Lint、单元测试任务及 API 35 仪器测试。
- 强制检查仪器测试真实执行、必需回归测试存在且没有失败。
- 验证 Debug APK 的包名、版本、Variant、Debuggable 标记和 APK 完整性。
- 成功后上传 Debug APK Artifact 和测试/Lint 报告。
- PR 不注入正式签名 Secrets，也不创建 Tag 或 Release。

### 2. main CI 成功：自动创建版本 Tag

`create-tag` job 仅在规范仓库的 `main` Push 且 `checks` 全部成功后运行：

1. 读取 `version.properties`。
2. 检查版本格式、当前提交确实属于 main 历史，以及 Version Code 高于此前沿袭的所有版本 Tag。
3. 若当前版本 Tag 不存在，则在通过 CI 的精确提交上创建 annotated Tag，Tag 名称必须与 `VERSION_NAME` 完全一致。
4. 使用本 job 的 `GITHUB_TOKEN` 显式 dispatch 同一工作流，并传入内部 `release=true`，启动 Tag 对应的签名发布流程。
5. 若相同 Tag 已存在于较早提交且版本元数据一致，则跳过，不重复创建版本；若同名 Tag 的版本元数据不一致，则失败退出，禁止覆盖或移动 Tag。

因此，普通 main 提交不会重复发布已经存在的版本。准备新版本时必须更新 `VERSION_NAME` 和递增 `VERSION_CODE`。如果 CI 在创建 Tag 前失败，修复后继续推送同一个候选版本即可；不要仅为重试而增加 Version Code。

GitHub 的 `GITHUB_TOKEN` 推送 Tag 不会自动触发新的 push 工作流，所以创建 Tag 后会显式调用 `workflow_dispatch`。GitHub 官方文档明确允许使用 `GITHUB_TOKEN` 触发 `workflow_dispatch`，无需额外 PAT。手动运行工作流时 `release` 默认是 false，只执行 CI；自动发布任务才会传入 true。

### 3. Tag 对应的签名构建与 Pre-release

自动 dispatch 后，工作流会在该 Tag 对应的精确提交上重新运行完整检查，然后：

- 进入受保护的 `android-release` Environment。
- 恢复固定 JKS 到 `$RUNNER_TEMP`，使用 JDK 21 构建 `reproducibleRelease`。
- 验证 APK 签名、证书 SHA-256、包名 `com.fox.runner`、Version Name、Version Code、不可调试标记、快捷方式包名和唯一 APK 产物。
- 生成 APK SHA-256、`SHA256SUMS`、`build-info.json` 和 Release notes。
- 验证传递校验和后，自动创建 GitHub Pre-release 并上传 APK、校验和及构建信息。
- 发布命令固定使用 `--prerelease --latest=false`。同名 Release 已存在时失败退出，不覆盖已分发资产。

Tag 触发的检查会在该 Tag 上再运行一次。这是有意的二次门禁，确保签名与发布基于 Tag 实际指向的提交。

## Codex 的阶段交付规则

Codex 完成一个明确的开发阶段时，先完成代码、回归测试和验收；只有阶段达到可发布状态时，才在最终提交中更新 `version.properties` 并推送 `main`。GitHub Actions 会在 CI 通过后自动创建 Tag、启动签名构建并发布 Pre-release。

Codex 不应手动创建 Tag、手动执行签名发布或手动上传 APK。它应等待并核对 main CI、自动 Tag、签名构建和 Pre-release 的实际结果，再向你报告 Release 链接和可下载 APK。中间开发提交不要提前更新版本号，否则只要当前版本 Tag 尚不存在，成功的 main CI 就会触发版本晋升。

若签名构建或发布因代码问题失败，不得强制移动或复用已经创建的 Tag。修复后使用新的版本名和递增后的 Version Code 创建下一候选版本。纯基础设施故障可重跑原 Tag 工作流，不要重写 Tag。

## 工具链与任务

Ubuntu 24.04、Temurin 21；Java 源码/目标兼容版本保持 17；Gradle Wrapper 9.8.2、AGP 9.5.0 不变。

安装官方 SDK `platforms;android-37.0`、Build Tools 36.0.0；测试额外安装 emulator 和 `system-images;android-35;default;x86_64`，开启 KVM。所有测试和构建统一使用 `Asia/Singapore`（UTC+08:00）。

主要任务：

```text
clean lintReproducibleDebug testReproducibleDebugUnitTest assembleReproducibleDebug
mediumPhoneReproducibleDebugAndroidTest
clean assembleReproducibleRelease
```

APK 目录为 `build/outputs/apk/reproducible/{debug,release}/`，不是 `app/build/`。签名验证读取 `output-metadata.json`，核对唯一产物、证书、实际 APK 包名、版本、不可调试标记和身份绑定，导出文件到 `dist/`。

## 固定签名和密钥安全

GitHub Environment `android-release` 必须配置：

- Secrets：`ANDROID_KEYSTORE_BASE64`、`ANDROID_KEYSTORE_PASSWORD`、`ANDROID_KEY_ALIAS`、`ANDROID_KEY_PASSWORD`。
- Variable：`ANDROID_SIGNING_CERT_SHA256`，填写可信的 SHA-256 证书指纹，允许冒号分隔。

工作流不生成密钥。Base64 只是编码，不是加密；不要在聊天、日志或仓库保存密钥内容。运行时只向签名步骤注入 Secrets。JKS 仅恢复到 `$RUNNER_TEMP/runner-gardenia-signing/release.jks`，权限 600，并通过 EXIT trap 和 always 步骤双重清理。签名任务禁用缓存和 configuration cache。

证书指纹必须独立配置，不能从本次 APK 自动学习。使用固定证书、固定包名和递增 Code 支持覆盖更新；实际设备安装及数据保留仍需用两个版本进行验收。

请将原始 JKS、Alias、证书指纹和恢复说明做至少两份独立加密备份，并在安全位置保存密码、定期试验恢复。GitHub Secrets 不是可恢复备份。密钥丢失会阻断正常更新，泄露会使签名身份受损。不要把上游 README 的公开证书指纹当作本项目证书。

## 权限和安全

工作流默认 `contents: read`。自动 Tag job 单独获得 `contents: write` 和 `actions: write`；发布 job 单独获得 `contents: write`。PR 不获得正式 Secrets，不使用 `pull_request_target`，签名步骤不输出敏感环境变量。无需额外 PAT。

请在 `android-release` Environment 的 Deployment branches and tags 中限制可信版本 Tag，并按需要配置审批；审批会暂停签名 job，若希望完全无人值守发布，应确认该 Environment 没有要求人工审批。保护 `main` 和 `v*` Tag，限制可修改工作流及发布的人。

上游说明、源码类路径、资源文字及导入导出格式仍保留 OpenTracks 引用；它们不是安装身份。此阶段不做 UI 品牌替换、不改业务逻辑。
