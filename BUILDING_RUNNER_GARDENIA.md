# Runner Gardenia 云端构建与预发布

仓库：`foxmaybeOI1761640545/Runner_Gardenia-06`。所有 Android 构建和测试在 GitHub Actions 完成，Windows 和 Codex Cloud 无需安装 SDK。

## 应用身份与版本

正式变体为 `reproducibleRelease`，包名 `com.fox.runner`。保留 Java namespace `de.dennisguse.opentracks`，它是类名空间，不是安装包名。Debug 包为 `com.fox.runner.debug`；Nightly 包为 `com.fox.runner.nightly`。Provider、快捷方式及自有 Intent action 随应用包名绑定。公共 marker action 现为 `com.fox.runner.MarkerDetails` 和 `com.fox.runner.CreateMarker`，外部调用者须明确指向本应用。

唯一发布版本来源为 `version.properties`，初始版本 `v0.1.0-rc1.1`、Code `6766`。新包名拥有独立版本序列，当前仓库没有既往发布 Tag；无法从源码证明设备是否曾装过同包名自签包。

每个后续发布必须在普通代码提交中显式提高 Code，并修改 Name。Tag 必须与 Name 完全相等且指向 main 历史中的提交。CI 不创建 Tag、不修改版本、不执行上游 RELEASE.sh。普通 CI 的 Git 描述允许退回短 SHA；签名构建必须有精确版本 Tag。完整 checkout 只包含本仓库历史，不导入上游历史。

## 工作流与操作

- PR 和 main 推送：Lint、单元测试任务、Debug APK、现有 API 35 AOSP Gradle Managed Device 仪器测试。正式 Secrets 不注入。当前没有 `src/test` 测试，单元任务可能为 NO-SOURCE；仪器测试必须实际执行且通过，零执行不能通过验收。
- Actions → Android build and pre-release → Run workflow：选择 main 时只做基础 CI 并上传 Debug APK。需要手动构建正式签名 APK 时必须选择已存在且符合版本配置的 `v*` Tag；只上传 Artifact，不创建 Release。不能从 main 手动绕过 Tag 检查。
- 推送 `v*` Tag：完成同样的检查，再进入 `android-release`，构建并验证正式 APK，然后创建 GitHub Pre-release。始终 `--prerelease --latest=false`。已有同名 Release 时失败退出，避免悄悄替换已分发的 APK；如需修订，请提高版本并创建新 Tag。
- 点击 Actions 运行查看每步日志，Artifacts 下载 Debug APK、测试报告或已验证的签名 APK。Tag 发布产物还可在 Releases 下载，包括 APK、SHA256SUMS 和 build-info.json。

本次实现不创建首版 Tag。请先检查 main CI；通过后由维护者决定何时创建 `v0.1.0-rc1.1` Tag。受保护 Environment 如要求审批，需在 Actions 页面批准签名任务。

## 工具链与任务

Ubuntu 24.04、Temurin 21，Java 源码/目标保持 17，Wrapper 9.8.2、AGP 9.5.0 保持不变。

安装官方 SDK `platforms;android-37.0`、Build Tools 36.0.0；测试额外安装 emulator 和 `system-images;android-35;default;x86_64`，开启 KVM。

任务：

```text
clean lintReproducibleDebug testReproducibleDebugUnitTest assembleReproducibleDebug
mediumPhoneReproducibleDebugAndroidTest
clean assembleReproducibleRelease
```

APK 目录为 `build/outputs/apk/reproducible/{debug,release}/`，不是 `app/build/`。签名验证读取 `output-metadata.json`，核对唯一产物、证书、实际 APK 包名、版本、不可调试标记和身份绑定，导出文件到 `dist/`。

SDK 37 平台识别、实际 Gradle 任务、依赖解析及设备测试仍须由真实 Actions 运行确认。不会在此环境安装 Android SDK 规避这些检查。

## 固定签名和密钥安全

GitHub Environment `android-release` 必须配置：

- Secrets：`ANDROID_KEYSTORE_BASE64`、`ANDROID_KEYSTORE_PASSWORD`、`ANDROID_KEY_ALIAS`、`ANDROID_KEY_PASSWORD`。
- Variable：`ANDROID_SIGNING_CERT_SHA256`，填写可信的 SHA-256 证书指纹，允许冒号分隔。

工作流不生成密钥。Base64 只是编码；不应在聊天、日志或仓库保存它。运行时只向签名步骤注入 Secrets，用环境变量传给 Gradle。JKS 仅恢复到 `$RUNNER_TEMP/runner-gardenia-signing/release.jks`，权限 600，EXIT trap 和 always 步骤双重清理。强制终止时仍依赖 GitHub 托管临时 Runner 的销毁，不应改用常驻共享 Runner。签名任务禁用缓存和 configuration cache；Artifact 使用明确的公开产物清单，不包含 JKS 或 Gradle 状态。

Gradle 新增 `ANDROID_KEYSTORE_FILE` 环境变量作为路径；保留原 `-Drelease_store_*` 本地方式兼容，但不建议把密码写在命令行。

证书指纹是公开的验证值，必须独立配置，不能从本次 APK 自动学习。使用固定证书加固定包名和递增 Code 支持覆盖更新；实际设备安装及数据保留还需后续用两个版本验收，不能仅凭签名通过宣称完成。

请将原始 JKS、Alias、证书指纹、恢复说明做至少两份独立加密备份，并在安全位置保存密码、定期试验恢复。GitHub Secrets 不是可恢复备份。密钥丢失会阻断正常更新，泄露会使签名身份受损。不要把上游 README 的公开证书指纹当作本项目证书。

## 权限和仓库设置

工作流默认 `contents: read`，仅发布 job 为 `contents: write`。不使用 pull_request_target，PR 无正式 Secrets，签名步骤不使用 Shell tracing。Actions 固定完整 commit SHA，Wrapper 校验由 setup-gradle 执行。

请在 `android-release` 的 Deployment branches and tags 中仅允许可信版本 Tag，按需要配置审批人；保护 main 和 `v*` Tag，限制可修改工作流及发布的人。源码中的身份检查不能替代 GitHub 仓库权限控制。无需创建额外 PAT，Release 使用该 job 的 GITHUB_TOKEN。

上游说明、类路径、资源文字及导入导出格式仍保留 OpenTracks 引用；它们不是安装身份。此阶段不做 UI 品牌替换、不改业务逻辑。
