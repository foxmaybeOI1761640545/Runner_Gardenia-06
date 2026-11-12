# Runner Gardenia P0.5.1 与 P0.6 最终交付报告

P0.5.1 GPS 质量修复和 P0.6 完全离线地图均已完成自动化/模拟器验收、原固定密钥签名、保留数据升级和公开同字节 APK 发布。两阶段保持独立分支/PR、源码、版本和发布记录，main 未修改，未强推重写历史，原版本和签名资产未替换。以下每个阶段均区分实际测试源码与之后仅增加报告/批准的分支提交。最终分支 HEAD 请以 PR 与最后报告提交为准；发布 Tag 始终固定到实际验收源码。

## P0.5.1 GPS 质量修复

P0.5.1 最终分支提交为 `ba00559af1af3d48bc3ecfde811d3e8d062e2f68`（最终报告提交）；实际自动化验收及 APK 源码为 `a4b2c7a7521a19a60c1d444449ff82c0560e4d19`；审查与签名激活提交为 `4ffeb2a0b3971e782b0461ccfc759ad9ea8d9844`，只增加审查记录和文档。分支为 `feature/p0-5-1-gps-motion-quality`，[PR #2](https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/pull/2)，base 保持 `feature/p0-5-dashboard-settings-history` / `de225b664a3f1b16e455068f1d426204f638745d`。

原有三个提交 `89d8a8b`、`17d917e`、`18320e1` 均保留；未强推、重写历史或修改 main。

### CI 范围与工作流修复

建立独立 `.github/p051/` 阶段配置及 `check_p051_source.py`，以实际 PR base 与 HEAD 的差异审核允许的 GPS 算法、集成、测试和发布文件；其他业务实现继续冻结。`P0GpxFiles.java` 以当前 P0.5 base 重查，没有沿用 P0.4 旧基准误报。旧 P0.5 配置和验收流程保留，P0.5.1 push/PR 由自己的完整流水线执行，通用 Android workflow 避免重复。缺失 XML、零执行、缺失截图或未成功启动 Android 环境均使门禁失败；Python 辅助测试不会替代 Android 验收。最终 Java21、API37 编译、Build Tools36、API35 模拟器、完整 Gradle/JVM 与原有回归均实际运行通过。

### GPS 算法和集成

继续修正现有 `GpsMotionFilter`。静止时保留约 1Hz 时间点并固定坐标锚点，速度为 0，配速为 `—`，运动总计时继续；没有自动暂停，也没有删除静止记录点。

停止阈值为 0.4m/s，低速启动阈值为 0.45m/s，并结合精度限制下 4–10m 的锚点位移；普通启动阈值为 0.9m/s。启动／停止分别使用 2／4 点滞回，显示噪声下限为 0.3m/s，因而持续 0.5、0.7m/s 不会反复停止。停止确认期也固定坐标；真实恢复移动时恢复从锚点开始的位移。

孤立跳点被拒绝，三个相互一致的重定位点或超过 15 秒的 GPS 缺口建立新段。被拒绝点持续到达时，不会因上一个有效点变旧而自动放行任意跳跃。GPS 断开重连保留待断段状态；BLE-only 事件不消耗标记，最终标记落在成功存储的 GPS 点上。

跨断点距离使用窄范围修复：没有真实传感器距离时，自动段首携带零跨段距离，并强制保存段首。共享统计器、数据库结构、GPX 导出器和 BLE 采集实现保持与 PR base 一致。有效定位会清除之前的弱 GPS 状态；没有伪造缺失的单调时间戳。

### 实际测试结果

[完整 push 运行 43066910884](https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/actions/runs/43066910884) 与 [PR 运行 43066926326 的第二次尝试](https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/actions/runs/43066926326/attempts/2) 均成功。以下数量来自 XML／真实测试回执，并已独立复核。

| 测试范围 | tests | executed | failures | errors | skipped |
|---|---:|---:|---:|---:|---:|
| Python 流水线辅助测试 |120|120|0|0|0|
| 完整 JVM Unit Tests |154|154|0|0|0|
| API 35 Instrumentation |435|434|0|0|1|
| 独立进程写入探针 |1|1|0|0|0|
| 新进程读取探针 |1|1|0|0|0|

唯一跳过项为此前已存在的 `TrackRecordingServiceStateMachineTest.recording_stopGPS_noop`，没有作为通过统计。最后一对独立进程探针覆盖数据库、指标偏好与批量缓存三个领域，未按领域重复计数。编译成功、Python 测试和截图张数均未当作 Android 测试数量。

GPS Android 专项为准备 13 项、真实记录链路 13 项、UI 状态测试 1 项，均执行通过。60 秒合成移动分别以 0.5、0.7、3、10m/s 验证；数据库累计距离相对目标 30、42、180、600m 的 ±0.15m 断言全部通过。完整 JVM 中也包含对应的持续慢速、静止噪声、滞回、跳点、重定位、长缺口、缺失速度／精度、非单调时间、重置和配速边界测试。

真实集成链路覆盖 GPS Handler → SensorManager → AggregatorGPS → TrackPointCreator → TrackRecordingService → SQLite → GPX。

| GPX 合成场景 | GPS 点数 | 轨迹段数 | 核验结果 |
|---|---:|---:|---|
| 静止噪声 |255|1|一个固定坐标；255 个原始心率样本；时间戳、精度完整；速度及距离为 0|
| 断开重连 |4|2|无跨断点连线距离；心率保留|
| 重定位和长缺口 |6|3|跳点不计距离，断点形成独立轨迹段|

Debug、Lint、Instrumentation 编译、API 35 实际运行、GPX 1.1 Schema、离线权限／打包审计、跨 UID 分享、进程恢复与 P0–P0.5 必要回归全部通过。

### UI 实际检查

检查了同一源码的 54 张联系表、420 张原生截图：GPS 12、P0.5 布局 48、原有状态 192、指标 8、设置 16、历史 32、P0 20、P0.2 36、P0.3 56。对联系表上不够清晰的超长运动时间，另行检查了完整分辨率原图。未发现阻碍发布的新裁切、重叠或主题切换问题。

静止及再次停止时，实际速度 TextView 为 0.0，配速为 `—`；真实 0.5m/s 移动时为 1.8km/h、33:20/km，英制为 1.1mph、53:39min/mi。GPS 状态、运动操作、心率页面、历史批量操作和不同主题均检查了真实内容。

原始报告 Artifact `13878632370`，SHA-256 `2e62ebb3bb066e4bfbc78d6331e49a7a76a7c9a8b2cc4524e249d89366680ad0`；原始视觉 Artifact `13878622397`，SHA-256 `a033d8bc0f2ccf41ff57b6526af9e603b689aa1d41b4ac3369cff1bf630598df`。原 Artifacts 均保留，失败尝试也没有删除。

### 签名与发布

已完成固定密钥签名、API35 安装、从 P0.5／P0.4 保留数据覆盖升级，以及公开下载校验。签名构建 [43140735933](https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/actions/runs/43140735933) 和发布 [43144185795](https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/actions/runs/43144185795) 均成功。

- 版本：`v0.1.0-p0.5.1 / 6777`；Application ID：`com.fox.runner`。
- [公开 Pre-release](https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/releases/tag/test-p0-v0.1.0-p0.5.1)，非 Latest。
- [APK 下载](https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/releases/download/test-p0-v0.1.0-p0.5.1/RunnerGardenia-P0-Release.apk)。
- APK：2,856,108 字节，SHA-256 `a99a489ffdc63c746346d9a6521884b4fc21a6698552f10c4963711b2232816d`。
- 固定证书 SHA-256：`E3E7D46BFCB869F5A521A7FBA6FD1BA5790D7DBCAF37F722DDC69D7BB1439A22`。
- 正式 Artifact `13909864105`，SHA-256 `fb01b0de04ac06496a944af5f8fe1928514dc2288e98d25cc1c4c9147bccd890`。

全部80张签名 APK 原生截图已实际查看，XML／图片摘要与窗口焦点已独立复核；8套应用信息页均显示正确版本和应用名称。两条升级路径均保留原有历史运动、GPS、原始心率样本、主题、单位／配速和设备选择，数据库结构未变。新录制、暂停恢复、强制进程恢复和 GPX／ZIP 验证通过。发布使用经测试的同一份签名字节，未重新签名或重建。

Actions 和工作空间分别匿名下载了公开 APK，均为 HTTP200、相同完整 SHA-256 与固定证书；公开 build-info／运行回执与已审查正式证据逐字节一致，Tag 指向真实测试源码。

### 尚需真实手机验证

实体 GNSS 接收、不同环境下静止与低速步行、真实 BLE 心率带、耗电、TalkBack 语音和实际微信接收端未由模拟器证明。公开测试数据均为合成数据；用户上传的 GPX、私人轨迹坐标没有提交到仓库。

### P0.5.1 修改文件清单（40 个）

- `.github/p051/protected-refs.json`
- `.github/p051/publication-approval.json`
- `.github/p051/stage.json`
- `.github/p051/visual-approval.json`
- `.github/scripts/check_p051_source.py`
- `.github/scripts/p051_pipeline.py`
- `.github/scripts/p051_publication.py`
- `.github/scripts/p051_publish.py`
- `.github/scripts/p051_release.py`
- `.github/scripts/p051_release_smoke.py`
- `.github/scripts/p051_release_theme_ui.py`
- `.github/scripts/p051_review_transfer.py`
- `.github/scripts/p051_signing.py`
- `.github/scripts/p051_version.py`
- `.github/scripts/p05_version.py`
- `.github/scripts/release_policy.py`
- `.github/tests/test_p051_final_gate.py`
- `.github/tests/test_p051_review_transfer.py`
- `.github/tests/test_p051_scope.py`
- `.github/workflows/android-build.yml`
- `.github/workflows/p051-gps-motion-quality.yml`
- `docs/p0-5-1/00-gps-motion-quality-plan.md`
- `docs/p0-5-1/01-ci-and-motion-review.md`
- `docs/p0-5-1/02-signed-acceptance.md`
- `docs/p0-5-1/03-publication.md`
- `release-ledger.json`
- `src/androidTest/java/de/dennisguse/opentracks/p0/P02HomeUiTest.java`
- `src/androidTest/java/de/dennisguse/opentracks/p0/P051GpsUiTest.java`
- `src/androidTest/java/de/dennisguse/opentracks/p0/P0GpsRecordingTest.java`
- `src/main/java/de/dennisguse/opentracks/p0/app/DashboardState.java`
- `src/main/java/de/dennisguse/opentracks/p0/gps/GpsMotionFilter.java`
- `src/main/java/de/dennisguse/opentracks/p0/gps/GpsReadiness.java`
- `src/main/java/de/dennisguse/opentracks/p0/gps/MotionFormat.java`
- `src/main/java/de/dennisguse/opentracks/sensors/GpsHandler.java`
- `src/main/java/de/dennisguse/opentracks/services/TrackRecordingManager.java`
- `src/main/java/de/dennisguse/opentracks/services/handlers/TrackPointCreator.java`
- `src/test/java/de/dennisguse/opentracks/p0/GpsMotionFilterTest.java`
- `src/test/java/de/dennisguse/opentracks/p0/GpsReadinessTest.java`
- `src/test/java/de/dennisguse/opentracks/p0/MotionFormatTest.java`
- `version.properties`

## P0.6 完全离线地图

分支：`feature/p0-6-offline-map`；[PR #3](https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/pull/3)，base 为已发布 P0.5.1 的最终报告提交 `ba00559af1af3d48bc3ecfde811d3e8d062e2f68`。本阶段独立开发，没有修改 main、重写历史或混入 P0.5.1 APK。

选择 Mapsforge 0.25.0：原生 Java/Canvas 与现有项目匹配，直接读取本地 `.map`，不需要 OpenGL 后端、在线瓦片或 API Key；没有同时引入 VTM。引擎、主题和 AndroidSVG 的许可证及上游链接豁免在应用许可证菜单和仓库文档中保留。OSM 来源数据的署名显示在地图页面。

已实现的功能包括：

- SAF 文件导入，在后台流式复制至私有 `files/offline-maps/`，校验文件头、版本、长度、边界和读取能力后原子更新选择。
- 损坏、不支持、截断、中断的导入不替换当前地图；进程中断的未完成副本在重开时清理。
- 地图选择和删除私有副本，原始文件与运动历史保留；完成导入后不依赖原 URI 或 Provider。
- 真实本地矢量地图、可见的原生缩放按钮、触摸拖动、当前位置、轨迹范围适配。
- 从首页轨迹预览、历史详情菜单和设置的“隐私与离线”进入地图；绘制正在记录及历史轨迹。
- 轨迹读取包含自动段首坐标，BLE-only 数据不切断绘图；按断点分别创建覆盖层，不跨缺口或经度接缝画长直线。
- 地图预览复用原 GPS 服务；不创建运动，关闭地图不停止运动。暂停时保持地图 GPS，恢复时重建手动段的运动过滤状态并清除旧 GPS 聚合，避免 BLE 更新复用暂停前定位。

瓦片缓存有内存上限，文件和轨迹都留在本机；保留既有 GPS/BLE/SQLite/GPX 主路径。Release Manifest 无 INTERNET、网络状态或外部存储管理权限；阶段审计只允许新增两项固定版本地图运行依赖，并拒绝下载层与网络客户端路径。测试地图是自行编写、CC0 授权的751字节合成矢量数据，没有提交用户真实轨迹或大型地图文件。

实际验收源码：`af6e90eabee20ada79ba942584ab7eb8c08660e3`；签名激活提交：`60f9fa1700dc8aac42ec636563216da711fa3ee9`（只改批准和文档）。应用主树 `3b7b266985667a8b057c5353535af56929b79a9d`。完整 push43235456414 / PR43235469449 均成功；独立核对原始 XML：

| 测试范围 | tests | executed | failures | errors | skipped |
|---|---:|---:|---:|---:|---:|
| Python 辅助测试 |138|138|0|0|0|
| 完整 JVM |163|163|0|0|0|
| API35 完整 Android |452|451|0|0|1|
| 快速地图/GPS/历史/传感器子集 |64|64|0|0|0|

64项与完整452项重叠，不能相加；唯一 inherited Ignore 与 P0.5.1 相同，没有新增跳过。新增 Android17项、JVM9项；旧阶段必要回归完整运行。

当前源码437张 PNG 内容已核验：19张联系表中的148张 PNG，加独立进程地图原图1张，直接查看149张；另288张应用主体画面与已完整直接审查的 b8e59ac 基线逐像素一致，只排除明确的系统状态栏 inset，全部应用主体像素参与比较。所有变化和地图16/GPS12均直接查看；应用、Android测试、JVM树完全相同。逐图摘要和实际审查回执保存在 visual-content-comparison.json。签名后的88+4张另行全部直接查看，没有将437张都声称为再次直接查看。未发现新的阻断布局问题；八套应用配色共用内置标准浅色地图渲染主题。

地图进程探针write1/read1，PIDs1848/2212；原有数据库/指标/批量缓存三个领域共用另一对write1/read1（PIDs1742/2231），没有按领域重复计数。GPX静止255点/一个坐标/255心率/零速度距离、重连两段与重定位缺口三段再次通过。原始报告13953878314/digest 676851c6a38d339ea49efd64ca89d729d5719ed5288c5d0064bb6be0cb25f087，视觉13953823411/digest 7a9025e036fd4946d669b1224fe2c758d5f4f56cc5ca285e1e4d62d76a3bc894。

### 固定签名、安装升级与公开发布

固定密钥 [构建43241685805](https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/actions/runs/43241685805) 和 [发布43271052832](https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/actions/runs/43271052832) 均成功。签名后原生88张与地图运行4张，共92张图片/12张联系表已全部直接查看；每张原生窗口 XML、焦点和图片摘要均独立复核，8张信息页显示正确版本。真实系统 SAF 导入、飞行模式、删除外部原文件后的进程重启、从真实历史列表和运动 ID 进入地图、实际多点轨迹、普通 GNSS 驱动实时位置/轨迹、损坏导入保留旧图均通过。历史轨迹另以独立 Pillow 检查实际地图区域中的青色轨迹像素，不能仅由底图或单个定位点满足。

- Application ID：`com.fox.runner`；版本：`v0.1.0-p0.6 / 6778`。
- 实际测试/发布源码：`af6e90eabee20ada79ba942584ab7eb8c08660e3`；只改批准/文档的签名激活：`60f9fa1700dc8aac42ec636563216da711fa3ee9`；发布激活：`f5719d0fffb59ac6aa48117e240264ebd7c39fc6`。
- APK：3,231,211字节，SHA-256 `6a605a9c4f6a1e5fc73b5be3598225fd3bf88fda7eea15436d9ce133ab7a8c44`。
- 固定证书 SHA-256：`E3E7D46BFCB869F5A521A7FBA6FD1BA5790D7DBCAF37F722DDC69D7BB1439A22`；沿用原密钥与现有 Secrets/Variables，没有生成替代密钥。
- 正式 Artifact `13957072567`，archive SHA-256 `a9480768cb94e27a0315c34513d2f1948ee9d922540191a016587967cbe20bbb`。
- [公开 Pre-release](https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/releases/tag/test-p0-v0.1.0-p0.6)；Tag `test-p0-v0.1.0-p0.6` 指向上述真实测试源码，非 Latest、非 Draft。
- [APK 下载](https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/releases/download/test-p0-v0.1.0-p0.6/RunnerGardenia-P0-Release.apk)；匿名下载资产 API：`https://api.github.com/repos/foxmaybeOI1761640545/Runner_Gardenia-06/releases/assets/740431109`（Accept: application/octet-stream，无 Authorization）。

API35实际安装、从当前已发布 P0.5.1/code6777及历史 P0.4/code6775覆盖升级均通过：原运动/GPS/原始心率/设备选择/主题/单位和速率偏好保留，SQLite schema43未改变；新录制、准备不创建运动、暂停恢复、进程强停恢复、GPX1.1与批量分享/ZIP均通过。Release实际执行R8和资源优化，并通过zipalign、apksigner和离线权限审计。

发布直接转移已经验收的正式 Artifact 原 APK，没有重新构建或签名。Actions 与工作空间分别重新匿名下载公开资产；工作空间检查时间 `2026-11-13T01:42:20.105150+08:00 (Asia/Shanghai)`，HTTP200、完整 APK 摘要/长度/公开v2证书指纹一致，公开 build-info、GPS/升级/地图运行回执与正式证据逐字节相同，并核验 annotated Tag 与非 Latest 状态。Actions另做实际密码学 apksigner 验证；本地没有安装 Android SDK。

公开证据下载：[签名原生88张截图及窗口XML](https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/releases/download/test-p0-v0.1.0-p0.6/theme-screenshots.zip)、[地图运行4张截图及窗口XML](https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/releases/download/test-p0-v0.1.0-p0.6/offline-map-screenshots.zip)、[关键测试XML/JSON/GPX回执](https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/releases/download/test-p0-v0.1.0-p0.6/key-test-receipts.zip)。完整 Debug 图集与原始构建报告在上文实际测试运行的 Artifacts 中。

发布运行的第一次尝试在所有 Draft 资产核对并公开后，最后一次匿名 API 下载遭遇 GitHub HTTP403 rate limit exceeded，因此未作为发布验收通过。失败日志与证明 Artifact13967419262保留；工作空间随后匿名HTTP200完整核验成功，再对同一运行、同一源码和同一正式 APK 进行一次有界重试，第二次尝试完成公开下载及 apksigner 验证。既有 Tag 和资产未替换。

先前失败签名43195155793及43229996107、因历史轨迹证据不足取消的43219379925均未发布；候选和失败诊断仍保留。af6补强的首页视口、原生历史入口和真实轨迹验证经过新的完整 CI、当前截图和本次签名验收。


使用方式：打开离线地图并导入合法来源的 `.map` 文件。缩放按钮及拖动均在地图内操作；“我的位置”按需申请精确位置权限，不会开始运动。“显示全程”适配当前轨迹。历史详情的更多菜单可打开对应运动地图。

限制与实机验证：单文件导入上限2GiB，完成导入的副本持久保存；导入过程中销毁页面会取消未完成复制。地图轨迹渲染采用有界点列，数据库和 GPX 保留完整记录。大型真实地图、长时间渲染、实体 GNSS、真实 BLE 设备及功耗仍需要手机验证；模拟器结果不等于这些硬件项目通过。


### P0.6 修改文件清单（74 个，含批准与最终报告）

- `.github/p06/protected-refs.json`
- `.github/p06/publication-approval.json`
- `.github/p06/stage.json`
- `.github/p06/visual-approval.json`
- `.github/scripts/check_p06_source.py`
- `.github/scripts/p051_version.py`
- `.github/scripts/p06_map_durability.py`
- `.github/scripts/p06_map_evidence.py`
- `.github/scripts/p06_map_smoke.py`
- `.github/scripts/p06_offline.py`
- `.github/scripts/p06_pipeline.py`
- `.github/scripts/p06_publication.py`
- `.github/scripts/p06_publish.py`
- `.github/scripts/p06_release.py`
- `.github/scripts/p06_release_map_ui.py`
- `.github/scripts/p06_release_smoke.py`
- `.github/scripts/p06_release_theme_ui.py`
- `.github/scripts/p06_retained_diagnostic.py`
- `.github/scripts/p06_review_transfer.py`
- `.github/scripts/p06_signing.py`
- `.github/scripts/p06_version.py`
- `.github/scripts/release_policy.py`
- `.github/tests/test_p06_release_gate.py`
- `.github/tests/test_p06_scope.py`
- `.github/workflows/android-build.yml`
- `.github/workflows/p06-offline-map.yml`
- `build.gradle`
- `docs/p0-6/00-offline-map-decision.md`
- `docs/p0-6/01-development-checks.md`
- `docs/p0-6/02-exact-source-acceptance.md`
- `docs/p0-6/03-signed-acceptance.md`
- `docs/p0-6/04-final-delivery-report.md`
- `docs/p0-6/05-publication.md`
- `docs/p0-6/retained-signing-failure-review.json`
- `docs/p0-6/visual-content-comparison.json`
- `release-ledger.json`
- `src/androidTest/AndroidManifest.xml`
- `src/androidTest/assets/p06/gardenia-synthetic.map`
- `src/androidTest/java/de/dennisguse/opentracks/p0/P05HistoryBatchTest.java`
- `src/androidTest/java/de/dennisguse/opentracks/p0/map/P06FixtureProvider.java`
- `src/androidTest/java/de/dennisguse/opentracks/p0/map/P06GpsOwnershipTest.java`
- `src/androidTest/java/de/dennisguse/opentracks/p0/map/P06MapPersistenceTest.java`
- `src/androidTest/java/de/dennisguse/opentracks/p0/map/P06MapRepositoryTest.java`
- `src/androidTest/java/de/dennisguse/opentracks/p0/map/P06OfflineMapTest.java`
- `src/androidTest/java/de/dennisguse/opentracks/p0/map/P06RouteReadTest.java`
- `src/main/AndroidManifest.xml`
- `src/main/assets/p06-licenses/AndroidSVG-Apache-2.0.txt`
- `src/main/assets/p06-licenses/AndroidSVG-NOTICE.txt`
- `src/main/assets/p06-licenses/Mapsforge-0.25.0-README.md`
- `src/main/assets/p06-licenses/Mapsforge-GPL-3.0.txt`
- `src/main/assets/p06-licenses/Mapsforge-LGPL-3.0.txt`
- `src/main/assets/p06-licenses/OSM-data-NOTICE.txt`
- `src/main/java/de/dennisguse/opentracks/p0/app/P05SettingsSectionFragment.java`
- `src/main/java/de/dennisguse/opentracks/p0/app/P0Activity.java`
- `src/main/java/de/dennisguse/opentracks/p0/app/P0HistoryActivity.java`
- `src/main/java/de/dennisguse/opentracks/p0/app/RoutePreviewModel.java`
- `src/main/java/de/dennisguse/opentracks/p0/map/MapRouteSegments.java`
- `src/main/java/de/dennisguse/opentracks/p0/map/MapViewport.java`
- `src/main/java/de/dennisguse/opentracks/p0/map/OfflineMapRepository.java`
- `src/main/java/de/dennisguse/opentracks/p0/map/OfflineMapView.java`
- `src/main/java/de/dennisguse/opentracks/p0/map/P06MapActivity.java`
- `src/main/java/de/dennisguse/opentracks/sensors/GpsHandler.java`
- `src/main/java/de/dennisguse/opentracks/services/TrackRecordingService.java`
- `src/main/java/de/dennisguse/opentracks/services/handlers/TrackPointCreator.java`
- `src/main/res/layout/p06_offline_map.xml`
- `src/main/res/values-zh/p06_map_strings.xml`
- `src/main/res/values/p06_map_strings.xml`
- `src/release/AndroidManifest.xml`
- `src/test/java/de/dennisguse/opentracks/p0/MapViewportTest.java`
- `src/test/java/de/dennisguse/opentracks/p0/OfflineMapFixtureTest.java`
- `tools/p06-map-fixture/GenerateMap.java`
- `tools/p06-map-fixture/README.md`
- `tools/p06-map-fixture/gardenia-synthetic.osm`
- `version.properties`

## 验收证据入口

- P0.5.1：[CI/GPS审查](../p0-5-1/01-ci-and-motion-review.md)、[签名验收](../p0-5-1/02-signed-acceptance.md)、[公开发布](../p0-5-1/03-publication.md)。
- P0.6：[地图技术决策](00-offline-map-decision.md)、[失败记录与修复](01-development-checks.md)、[精确源码与UI验收](02-exact-source-acceptance.md)、[逐图内容核对](visual-content-comparison.json)、[签名验收](03-signed-acceptance.md)、[公开发布](05-publication.md)。
- 原始 XML、原生截图和窗口 XML、合成 GPX、进程探针与签名运行回执均在对应 Actions Artifacts/Release 资产保留；文中明确列出运行、source、artifact ID和摘要。

## 完成状态与实机边界

P0.5.1 静止锚点/零速度距离/完整时间点、真实低速运动、配速与英制公制、跳点/重定位/断开分段、BLE交错、GPX与旧阶段回归通过。P0.6 导入/校验/私有持久地图、缩放拖动、当前位置、实时和分段历史轨迹、适配、管理删除、飞行模式与进程恢复通过。两个 APK 都已公开且匿名下载验证为经过实际测试的同一份固定签名字节。没有尚未解决的自动化发布阻塞。

真实手机仍需验证：室内/城市遮挡/开阔环境 GNSS、长时间静止和低速步行、实体 BLE 心率带与重连、后台/省电策略和耗电、大型合法来源地图、长时间渲染、TalkBack 语音和实际微信接收。模拟器使用合成坐标、假 BLE 与真实原生交互，不能代替这些硬件结论；用户上传的私人运动数据没有公开提交。
