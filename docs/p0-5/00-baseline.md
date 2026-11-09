# P0.5 基线与实际验收计划

日期：2026-11-09（Asia/Shanghai）。Phase 0 状态：候选，待本提交的远端检查收据；本阶段未修改产品代码。

## 仓库与不可变发布

- 开发基线：`f994d1ecd0e19a840800b2f946df3656a6b312c6`，与远端 `feature/theme-system-p0-3-ui-refinement` 一致。
- 新分支：`feature/p0-5-dashboard-settings-history`，直接从此基线建立。初始工作区干净；保留原 `work`/`main` 本地分支。不会向 main 或旧开发分支提交。
- 已获取全部远端分支及 Tag；精确快照见 [baseline.json](baseline.json)。设计分支当前为 `45ce47d9eec81dd2f0cdfcf2bfd7055e23378e02`。
- 本地另有历史孤立 Tag `v0.1.0-rc1.1`，远端不存在；单列 localOnlyTags，不能误列为远端有效发布，亦不删除本地引用。
- P0.4 annotated Tag 对象：`f3900d5dadd09d3b8132bd2951d674692b2a6011`；解引用源码：`ccc59b546b5087a2038d32087cc675a3096852c3`。开发 HEAD 的后三个提交是验收/交付记录，不能作为旧 APK 的实际源码。
- [P0.4 Release](https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/releases/tag/test-p0-v0.1.0-p0.4)：保持 Pre-release。资产不覆盖、不移动、不重建、不重新签名。
- 已实际下载公开 APK：`com.fox.runner`、`v0.1.0-p0.4` / `6775`、`reproducibleRelease`，大小 `2803864` bytes；独立计算 SHA-256 为 `aa5ebc21c76cbca492edc66453b91d7fdecaf82bd5a69fcab208c740b4224ebc`。
- 独立解析下载 APK 的 v2 签名块，证书 SHA-256 为 `E3E7D46BFCB869F5A521A7FBA6FD1BA5790D7DBCAF37F722DDC69D7BB1439A22`。此项是证书提取核对；完整密码学签名校验另由 Actions 的 apksigner 执行，不能混称。
- 已读取公开 build-info，与 Tag、版本、哈希、大小、证书一致。P0.4 原测试收据：Android 407 / executed 406 / failures 0 / errors 0 / skipped 1；JVM 118 / executed 118 / failures 0 / errors 0 / skipped 0。这是旧版本证据，不是 P0.5 测试结果。
- 已检查所有 Releases：P0.1–P0.4、rc1.4–rc1.6 均保留；本轮目标 Tag 尚不存在。

## 版本与工具链

基线 version.properties 为 `v0.1.0-p0.4` / `6775`。release-ledger.json 的 codeHighWater=6775，P0.4 条目仍为 reserved，实际发布身份以不可变 Tag 和公开 build-info 为准。本阶段保留台账原文；Phase 5 重新核对远端分配，若最高仍为 6775，才分配 P0.5=6776，否则选择更高空闲值。不得提前复用旧版本签名构建。

保留 Wrapper 9.8.2、AGP 9.5.0、JDK 21 / Java 17、compile/targetSdk 37、minSdk 26、Build Tools 36.0.0、API35 Managed Device、CI Asia/Singapore。云端不安装 Android SDK；Android 构建、模拟器和签名通过 Actions。新增 P0.5 分支专用工作流，原分支工作流及发布资产保持原样；Phase 0 只执行静态准备检查，不构建旧版本。

## 已阅读与实际架构

已阅读当前 AGENTS.md、P0_MIGRATION_PLAN/P0_ACCEPTANCE、阶段交付资料，以及设计分支的 METRIC_SWAP_SPEC、DESIGN_SPEC、PAGE_STRUCTURE、NATIVE_UI_IMPLEMENTATION_GUIDE、ANDROID_TECH_STACK_AUDIT、TEST_REPORT。主题预览网站实际 HTTPS 200，明确为模拟设计参考。旧 AGENTS 的 P0.3 分支限制由本轮用户明确指令覆盖。

- Java/XML/ViewBinding 原生 Activity；P0HomeBinding 绑定四套不同 XML 层级；DashboardState 从现有 P0RecordingViewModel Snapshot 投影，DashboardRenderer 绑定现有 View。GPS/BLE/记录服务/数据库算法保留。
- 当前标题主题名称在四 XML 和 renderer 中；删除 View 同时删除绑定，收缩标题容器。p0_home.xml 是共同 binding 合同，也要同步处理。
- 现有顶部状态行和 RadioGroup 分开；新行需短标签、完整无障碍描述、48dp 触摸目标和大字体适配。保留选中状态和真实状态入口。
- 当前首页格式仍硬编码公制并同时显示配速/速度；Phase 2 必须接入现有 PreferencesUtils/格式器。四指标有序位置模型与真实数据隔离，GPS 精度固定；仅交换主槽与点击槽，持久化走既有默认 SharedPreferences。
- 设置目前直接展示 GardeniaThemeSettingsFragment。新主页按六类进入区域；主题页保留原 fragment，单位/速率使用原键，心率继续 P0SensorsActivity/Fragment；不新增 BLE 管理器。
- 历史是 ListView/ArrayAdapter + 详情区域，P0HistoryViewModel 已有单线程 worker。WorkoutRepository.isProtected 结合 SessionStore 与实际活动服务，覆盖暂停及恢复记录；P0GpxExporter 再次校验存在和保护。扩展这一路径完成批量操作。
- 旧 ShareUtils 可能静默跳过不存在记录且缺显式 ClipData，不能整体复用。新批量导出必须逐条校验、清晰失败、原数据不变；文件与 ZIP 在 worker，FileProvider/ClipData 授权交给系统 Sharesheet。

## 分阶段验收清单

每阶段先读代码/定义范围，候选提交仅推送新分支，检查精确 SHA 的 Actions，下载原始 XML/截图，人工审图，修复重测并归档报告后才进入下一阶段。失败收据保留。报告提交不重复签名旧资产。

| 阶段 | 修改与验收门禁 | 证据 |
|---|---|---|
| 0 | 干净工作区、开发 HEAD、全部分支/Tag/Release、版本台账、公开 APK 哈希/证书、设计与业务/测试审查、独立分支与 CI | 本报告、baseline.json、Phase 0 远端收据 |
| 1 | 四主题删除名称 View/间距；跑步/骑行/GPS/心率紧凑布局；真实状态及点击；48dp；不影响 session/service | API35 原生 4×2×3×2（360/393/412dp，字体1.0/1.3）矩阵；无裁字/重叠/溢出断言与人工截图审查；相关记录回归；01-dashboard-layout.md |
| 2 | DISTANCE/DURATION/PACE_SPEED/HEART_RATE 槽模型；每主题默认值与独立持久化；精准交换、GPS 不交换；无效心率 --；统一单位/速率/长时长；键盘/TalkBack语义和公告/焦点 | JVM 模型测试、API35 交换与偏好/重建/页面/主题/进程恢复测试、记录/暂停/GPS/BLE隔离回归、八配色原生截图；02-metric-swap.md |
| 3 | 六分类入口/返回；四主题 FOLLOW_SYSTEM；原单位/速率键与恢复默认布局；原设备控制；历史入口；离线说明；BuildConfig 正式版本 | API35 导航/偏好重建/窄屏大字体测试、八配色截图；03-settings-hub.md |
| 4 | 长按/点击多选、数量/全选已加载/取消/退出；单击详情不变；逐记录 GPX、ZIP 默认/多 GPX 可选；worker、明确错误、二次确认删除、活动/暂停保护 | 单/多文件/ZIP解包、独立模拟接收应用读取每份URI/内容/授权；不存在/失败/中断/取消/生命周期清理；详情/曲线/单导出回归；04-history-batch-share.md |
| 5 | 全 JVM/API35、原有回归、离线/Manifest、GPX1.1 schema、数据库/真实进程恢复、P0.4固定签名覆盖升级与保留数据、文件分享/ZIP | 原始 JUnit XML 分列 Tests/Executed/Failures/Errors/Skipped（非零执行；原有 skip 单独披露）；全矩阵审图；签名与 Release 独立下载核验；05-final-delivery.md |

最终发布在所有门禁后使用原密钥 `assembleReproducibleRelease`，目标 `test-p0-v0.1.0-p0.5` / `[TEST][P0.5] v0.1.0-p0.5`，Pre-release 且非 Latest。记录源码 SHA、run/Artifact、APK/SHA256SUMS/build-info/test-summary、截图包、收据与提交历史；公开下载逐项比对资产与构建 Artifact。签名材料仅在既有受保护 android-release job 的 RUNNER_TEMP，绝不进入日志/Git/Artifact。

## 已知风险与诚实边界

1. 当前 CI 只路由旧分支且 P0.4 源码冻结检查禁止 export 修改，必须新增独立 P0.5 路径，不能盲用旧任务或降低结果完整性断言。
2. 原截图矩阵是 360/390/412dp、1.0 字体，本轮明确要求 393dp 和 1.3 字体；历史成功截图不能代替新矩阵。
3. Outdoor 主指标叠加轨迹，长时长/配速必须约束宽度；Neon 主标题需随指标变更，BPM 图表保留独立语义。
4. 原设置导航测试假设直接进入主题页，重构后调整测试导航步骤并保留原业务断言。
5. 文件跨应用权限必须由独立接收进程验证，不能只检查 Intent 对象。分享失败、取消或导出失败绝不能删除原数据。
6. 微信真实客户端、户外 GNSS、实体心率带 RF/耗电及人工 TalkBack 语音仍为 `NEEDS_DEVICE_VALIDATION`。模拟接收应用/API35 模拟器不证明这些事项；不引入未经授权 AppID/SDK。

Phase 0 后仅在真实远端基线检查通过后进入 Phase 1。其余阶段当前均未实施、未验收，P0.5 尚未完成。
