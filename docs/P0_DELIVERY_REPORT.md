# Runner Gardenia P0 最终交付报告

自动化开发及 APK 交付：PASSED。原始 APK 现已发布为 [P0 测试 Pre-release](https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/releases/tag/test-p0-v0.1.0-p0.1)，没有重新构建或重新签名。真实心率带、户外 GPS 和长期后台运行：NEEDS_DEVICE_VALIDATION。

## 1. Git 分支情况

- 原仓库：https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06
- 原 main 基线：`9b5d7b06afc1eb8e3315ba73874ac088fa4d5169`。
- 新开发分支：`feature/p0-core-migration`。
- 本次实际构建源代码：`3116e5fa7a56ce6f299ea45d5a45745f1aed4466`。其后补充验收文档及仅传递既有附件的发布辅助流程；没有重新构建 APK 或改变业务代码。
- main 和原有 `feature/heart-rate-monitoring` 分支保持初始 SHA；未合并 main。最初 Artifact 交付时未创建 Tag/Release；本轮按新指令发布了独立测试 Tag `test-p0-v0.1.0-p0.1`，仍指向原始构建提交。
- 分阶段提交及验收：[迁移计划](P0_MIGRATION_PLAN.md)、[完整验收记录](P0_ACCEPTANCE.md)、[冻结基线](P0_BASELINE.json)。

## 2. P0 功能完成情况

| 范围 | 实现及自动验收 |
| --- | --- |
| GPS | 复用原定位驱动、记录器和统计；1Hz 无批处理请求，跑步/骑行、开始/暂停/恢复/结束、定位状态/坐标/精度/原始测量时间、距离/速度/配速。暂停分段与距离/时间门禁通过；实际硬件采样频率仍需真机测量。 |
| BLE | 复用标准 0x180D/0x2A37 驱动；扫描、名称/MAC/RSSI、手动选择、记忆设备、连接/订阅/断开/权限和生命周期。未默认连接所有设备。 |
| 心率 | 8/16-bit BPM、接触标志、可选 Energy/RR、边界/坏包过滤；实时 BPM、设备状态及更新时间。相同 BPM 的独立有效通知保留不同时间和采样标识。 |
| 重连 | 连接超时、断连检测、有上限的退避、用户主动断开/关闭重试、Gatt 释放和旧连接回调隔离。自动测试通过，未声称真实 H303 RF 验收。 |
| 本地数据 | 继续使用 database.db/schema43、Track/TrackPoint/HeartRateRepository；原始 heart_rate_samples 保留运动、设备、时间、来源、连接会话、接触、Energy、RR。事务/读取/删除关联、历史兼容和两个实际进程恢复验证通过。 |
| GPX | 系统本地保存/分享，真实 GPS 点和分段；标准扩展按时间保守匹配心率，同时保留所有原始通知及未匹配样本，绝不伪造 GPS 点。见 [GPX 格式与匹配规则](P0_GPX_FORMAT.md)。 |
| 离线 | 实际 Release APK 无 INTERNET 权限，无账号/上传/云端/在线地图入口，禁用系统数据备份；保留必要 GPS/BLE/前台服务权限。见 [离线审计](P0_OFFLINE_AUDIT.md)。 |
| 基础 UI | 精简 P0 入口、运行/心率设置/本地历史与统计/GPX；首次介绍直接跳过。亮/暗、小/大窗口自动截图门禁，以及正式压缩 APK 的实际页面检查通过。 |

最终补齐的 GPS 展示使用记录器已经持有的最近一次真实不可变测量，保留其时间戳和精度；不会因 SensorDataSet 被存储流程消费而清空坐标/速度。GPS 关闭/丢失时清除显示。没有修改成熟 GPS 距离/过滤算法，也没有新增数据库或云服务。

## 3. 测试结果

本轮完整运行：`41974364095`；时区：Asia/Singapore。数量来自下载后的实际 XML 独立重计，NO-SOURCE 不计为测试。

| 测试 | Tests | Executed | Failures | Errors | Skipped |
| --- | ---: | ---: | ---: | ---: | ---: |
| Android API35 Instrumentation | 379 | 378 | 0 | 0 | 1 |
| JVM JUnit | 88 | 88 | 0 | 0 | 0 |
| Python 工作流/校验脚本 | 39 | 39 | 0 | 0 | 0 |

原有合理 Ignore 保留。另有同一文件数据库测试的 write/read 两次独立 instrumentation 调用各1项通过，进程 PID1943/2306；这些是额外执行证据，不重复算作新增独立测试方法。

- Debug 构建、Lint、完整报告/类/方法完整性、身份检查通过。
- 9份 Android 实际 GPX 导出文件通过官方 GPX1.1 XSD、计数/时间/哈希检查；下载后再次独立验证。
- 原有40张界面截图、20张 P0 亮暗/小大窗口矩阵、9张 P0 诊断截图均实际生成；另外检查正式 APK 的5个实际页面截图。
- `assembleReproducibleRelease` R8/资源压缩、唯一 output-metadata 产物、apksigner/独立证书/非 Debug/包名版本/zipalign/实际离线 Manifest/私有 FileProvider 均通过。
- 实际签名 APK 在 API35 AOSP 设备从旧版6771覆盖安装到6772，保留合成旧记录的1个 GPS 点和2条原始心率样本，schema43不变。
- 蓝牙和通知权限未授予时实际 GPS 记录/暂停/进程关闭后恢复/结束通过：保存46个 GPS 点、2个手动段，总距离20.272055m，活动57781ms、墙钟69490ms。定位信息、精度、速度和配速非空且数值合法。
- 正式 APK 实际导出旧记录 GPX：1个 GPS 点、2条原始 HR 样本；下载后独立校验 XSD 通过。
- 早期失败及根因修复保留在验收记录中，没有跳过失败门禁或冒用历史成功结果。

## 4. APK 信息

| 项目 | 结果 |
| --- | --- |
| APK | RunnerGardenia-P0-Release.apk |
| 大小 | 2,715,993 字节，约2.59 MiB |
| Version Name | v0.1.0-p0.1 |
| Version Code | 6772 |
| Application ID | com.fox.runner |
| 变体 | reproducibleRelease；debuggable=false |
| APK SHA-256 | `94d3322310e8b6050504eb4c8d8c72fa09f0190f8add35ae0100b9bc93cc6ab0` |
| 固定证书 SHA-256 | `E3E7D46BFCB869F5A521A7FBA6FD1BA5790D7DBCAF37F722DDC69D7BB1439A22` |

[最终 APK Artifact](https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/actions/runs/41974364095/artifacts/13391972069) 包含 APK、SHA256SUMS、build-info.json、runtime-smoke.json。已实际下载，APK/校验文件/构建与运行信息的 SHA 一致，并通过本地独立 aapt2 身份检查。平台常规 Artifact 下载为归档，本构建没有给 APK 再套一层 ZIP；保留90天。

签名使用现有 android-release Environment，不生成/更换密钥。密码只作为环境变量，JKS 只在 RUNNER_TEMP 恢复，EXIT trap 和 always 清理；签名 Job 禁用 Gradle 缓存。正式和诊断 Artifact 不包含 JKS/密码。所有 Job contents:read，没有 Tag/Release 权限。

[安装与构建使用说明](../BUILDING_P0.md)。请覆盖安装，不要卸载/清除数据；保存 APK、校验文件及原有独立加密签名备份。

## 5. 真机待验证项目

状态统一为 NEEDS_DEVICE_VALIDATION，见 [逐项真机清单](P0_DEVICE_CHECKLIST.md)：

- 迈金 H303/其他标准心率带的真实发现、佩戴/接触、通知、可选 RR/Energy。
- 超出范围、关机、蓝牙关闭再开启、自动重连、主动断开及关闭重试的实际 RF 行为。
- 户外跑步/骑行 GPS 精度、真实1Hz交付、遮挡/恢复、暂停移动与分段、速度和移动时间。
- 熄屏/后台至少30分钟、电池策略/耗电、进程或设备重启。
- Android8–11及12+真实权限差异，真实个人历史数据覆盖升级和系统文件保存/分享。

模拟器/测试替身验证不能代替上述硬件验收；没有真实 H303 或户外设备连接到本次环境。

## 6. GitHub Actions

- [实际成功运行及日志](https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/actions/runs/41974364095)
- [正式 APK 下载](https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/actions/runs/41974364095/artifacts/13391972069)
- [R8/正式运行记录和5张截图](https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/actions/runs/41974364095/artifacts/13391757704)
- [完整测试报告与截图](https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/actions/runs/41974364095/artifacts/13391716581)

原始构建流程：精确分支/提交准备 → Debug/Lint/JVM/API35 → 报告/GPX/文件恢复/UI/离线身份门禁 → 同轮报告验证 → R8正式签名 → APK安全/身份/证书验证 → 实际安装/升级/记录/GPX → Artifact。PR 和其他分支不能进入这个签名 Job。原始构建运行没有 Tag 或 GitHub Release 发布操作；后续单独附件传递 run41991238827 复用同一 APK，完成测试发布，没有执行构建或签名。最终技术审查见 [P0_FINAL_TECHNICAL_REVIEW.md](P0_FINAL_TECHNICAL_REVIEW.md)。
