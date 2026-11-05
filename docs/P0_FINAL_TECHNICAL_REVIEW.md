# Runner Gardenia P0 测试发布及最终技术审查

结论：测试 Pre-release 已公开发布，原始 APK 未重建、未修改、未重新签名。阶段0–13的软件审查及发布检查已完成；真实设备项目仍为 NEEDS_DEVICE_VALIDATION。本轮没有发现需要修复生产功能并生成 p0.2 的缺陷。审查确认存在非功能性旧代码残留，详见 D；没有把残留宣称为已彻底删除。

## A. 测试 Release

| 项目 | 实际结果 |
| --- | --- |
| Tag | `test-p0-v0.1.0-p0.1`，annotated，注释包含版本、构建 SHA、Run ID、APK SHA |
| 标题 | `[TEST][P0] v0.1.0-p0.1` |
| Release | https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/releases/tag/test-p0-v0.1.0-p0.1 |
| 状态 | draft=false，prerelease=true；make_latest=false；发布后 Latest API404（仓库无稳定 Latest），本版本未成为 Latest |
| APK | https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/releases/download/test-p0-v0.1.0-p0.1/RunnerGardenia-P0-Release.apk |
| 大小 | 2,715,993 字节，约2.59 MiB |
| SHA-256 | `94d3322310e8b6050504eb4c8d8c72fa09f0190f8add35ae0100b9bc93cc6ab0` |
| 附件 | APK、SHA256SUMS.txt、P0_DELIVERY_REPORT.md、P0_DEVICE_CHECKLIST.md、build-info.json、runtime-smoke.json，共6个 |

原始 Artifact13391972069 在本轮重新下载并验证。附件在 Draft 上传后和公开发布后均通过 GitHub API 再次下载，与本地准备的每个附件逐一比较 SHA/大小及 GitHub digest。另通过公开 browser_download_url 下载 APK 并核对同一哈希。6个附件均 uploaded，未上传 Debug APK、JKS/私钥/密码或无关临时文件。

附件两份报告保留 e8973de 的原始验收快照，Release Notes 明确解释其“尚无 Tag/Release”的历史语境。本文件和更新的真机清单是后续技术审查补充，不改变原 APK 的来源和历史测试事实。

## B. 源码和构建信息

- 仓库：foxmaybeOI1761640545/Runner_Gardenia-06。
- 唯一修改分支：feature/p0-core-migration。
- main 原始基线：`9b5d7b06afc1eb8e3315ba73874ac088fa4d5169`。
- Tag 解析后的提交、实际 APK 构建提交均为 `3116e5fa7a56ce6f299ea45d5a45745f1aed4466`。
- 原始验收文档：`e8973de980398f9f64f72eae510a789b86338356`；其与 b4d8070 都是构建提交后的 docs-only 提交。
- 原始构建 Run41974364095 / Artifact13391972069：[完整运行](https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/actions/runs/41974364095)。
- 附件传递 Run41991238827：[成功运行](https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/actions/runs/41991238827)。此运行没有执行 Gradle、Debug/Release 构建或签名；它验证后传递原 Artifact，原产品测试门禁没有被替换。
- Application ID=com.fox.runner；Version Name=v0.1.0-p0.1；Version Code=6772；变体=reproducibleRelease；debuggable=false。
- 固定证书 SHA256=`E3E7D46BFCB869F5A521A7FBA6FD1BA5790D7DBCAF37F722DDC69D7BB1439A22`。使用官方 apksig9.5.0 独立验证原 Artifact APK 及旧版 APK，SDK26–37签名检查通过、v2有效、证书相同。此项签名验证不代表在所有这些 Android 版本上完成真机运行。

本轮生产源码、Gradle、Wrapper、依赖、版本及签名配置与3116e5f完全相同。新增内容限于附件传递的CI辅助流程/范围测试和审查文档。由于工作区 uploads.github.com 返回401，上传改由 GitHub Actions 原生短期 GITHUB_TOKEN 完成；没有要求用户提供 PAT，也没有读取签名 Secrets。首次附件传递因 Artifact 端点 Accept 类型错误415失败，随后正常新提交修复并验证成功，历史失败记录保留。

## C. 自动化验收证据复核

从原始成功运行重新下载了完整测试和 Release/R8 报告，独立解析实际 XML，而非仅使用 BUILD SUCCESSFUL 或 Job Summary。

| 原始构建测试 | Tests | Executed | Failures | Errors | Skipped |
| --- | ---: | ---: | ---: | ---: | ---: |
| Android API35 | 379 | 378 | 0 | 0 | 1 |
| JVM JUnit | 88 | 88 | 0 | 0 | 0 |
| 原始工作流辅助测试 | 39 | 39 | 0 | 0 | 0 |

本轮新增5项发布范围测试在 Actions 实际执行通过；本地现有辅助测试共44项通过。没有重新构建/执行全部 Android 测试来冒充新 APK；379/88/39属于字节一致且生产源码不变的原始构建。

- Debug、Lint及全部原始强制门禁通过。Kotlin等NO-SOURCE任务未计为测试。
- 两个实际文件数据库 instrumentation 进程 write/read 各1项通过，PID1943/2306不同；下载的原始命令输出重新解析一致。这两次执行不重复算作新增独立测试方法。
- 9份实际 Android 导出的 GPX 文件已重新核对文件哈希，并再次用官方 GPX1.1 XSD 离线验证；没有从网络加载 schema/DTD。
- 实际固定签名 APK 在 API35 AOSP 模拟器覆盖旧版6771，保留合成旧记录的1个 GPS 点、2条原始 HR、schema43；历史详情和原始HR统计可读取。
- 实际新运动记录46个 GPS 点、2个手动段、距离20.272055m，活动57781ms、墙钟69490ms；蓝牙/通知权限拒绝时 GPS 正常。暂停后关闭进程并重新打开、恢复及结束通过。此为模拟器验证，不是户外测量精度结论。
- 正式 APK 实际导出旧记录 GPX含1个 GPS 点/2条原始HR，已重新验证 XSD。APK、运行记录、构建信息、校验文件均为同一 SHA。

唯一跳过项为 TrackRecordingServiceStateMachineTest.recording_stopGPS_noop，原注释为“TODO Bug: GPS can be stopped although the current track is recording”。这是上游内部 hard-stop 接口契约的既有 TODO；stopSensors 当前为 package-private，P0 UI不直接调用。P0使用有活动记录保护的 stopIdleSensors，暂停/结束先结束记录，销毁时 hard-stop 用于释放资源。保留该 Ignore 和全部原断言，未宣称这项 TODO 已修复。正常 P0 路径的GPS/服务/权限/生命周期回归及实际记录通过。

## D. 技术审查

### 时区和时间语义

GPS/心率接收采用同一 MonotonicClock：以启动时的 epoch 时间加 elapsedRealtime 增量获得 Instant，避免记录期间设备墙钟回拨；不是把 Singapore 偏移加到数据库数值。轨迹时间及HR索引列按 epoch毫秒保存、Instant.ofEpochMilli读取，HR JSON元数据及GPX保留 ISO时间精度。历史 time_offset 单独存秒，不重复加到绝对时间。

P0Activity/P0HistoryActivity中的 ZoneId.systemDefault()用于界面本地显示，合理且保留。生产代码没有 TimeZone.setDefault 或固定Asia/Singapore；Instant/GPX序列化不依赖显示时区。CI Runner/JVM/单测进程/API35系统及instrumentation则明确设Asia/Singapore，已有实际系统/进程时区断言和失败后恢复默认时区的测试。

使用9份实际导出GPX中的40个时间值，在Singapore/UTC/Berlin/New_York做160次epoch与ISO往返校验；02:02Z转换为10:02+08:00，DST回拨的两个1:30保持相差一小时，外部+01:15显式偏移语义保留。没有修改任何数据库时间或外部测试数据。此为主机数据语义审查，真实Android时区切换列入真机清单，不冒充设备执行。

### 实际 APK 精简结果

比较的两个 APK 都为同仓库 reproducibleRelease、com.fox.runner、同一固定证书和相同主要工具链（JDK21/source17、AGP9.5.0、Wrapper9.8.2、API37、BuildTools36）。旧版6771的实际APK哈希与其发布build-info一致、独立签名有效。旧版 minifyEnabled=false，资源压缩未开启；P0开启两项压缩并裁掉非P0入口。因此以下是最终应用整体差异，不能全部归因于单独R8，也不是严格单变量实验。

| 实际测量 | rc1.6 /6771 | P0 /6772 |
| --- | ---: | ---: |
| APK字节 | 11,007,368 | 2,715,993 |
| DEX文件 | 5 | 2 |
| DEX类定义 | 14,838 | 1,519 |
| DEX已定义方法 | 119,317 | 8,472 |
| DEX解压字节 | 18,872,496 | 1,471,200 |
| 资源ID | 8,280 | 5,050 |
| layout资源ID | 198 | 62 |
| 资源ZIP条目（含arsc） | 1,519 | 690 |

减少8,291,375字节，约75.326%。原始完整签名日志实际执行 minifyReproducibleReleaseWithR8 和 optimizeReproducibleReleaseResources；下载的非空mapping/usage/resources/configuration文件与build-info映射哈希一致。类定义来自真实DEX解析，类名用R8映射对照，不是源码文件数量或仅看Gradle开关。

### 明确保留的非功能性残留

不能声称APK已经只包含P0代码。实际DEX仍有42个Health Connect相关类映射（基线1582个），以及Health设置/Worker、部分旧传感器驱动、CSV/KML导出与旧ExportActivity类。旧偏好默认值XML的intent引用、私有ShareContentProvider及共享handler/datamodel根使这些类仍被保留；R8默认规则/库consumer规则也会保留反射根。Gradle依赖声明仍含原有Health Connect/Work等版本，未擅自升级或删除。

按映射对应的实际DEX类去重，库类残留计数为：androidx.health.38；androidx.work.29；androidx.mediarouter.0；androidx.constraintlayout.0；androidx.gridlayout.0；kotlinx.coroutines.98。这些库类计数不等于它们独占的压缩字节数，也不证明存在对应用户功能。

这些是已确认的体积残留，不是已修复事项。实际Manifest没有Health权限/Activity/WorkManager启动器或Job服务；Health导出调用来自被移除入口的旧TrackStopped/Health Activity，P0结束记录直接调用核心Service。P0只启动GPS/HR驱动，没有踏频/功率设备设置入口；CSV/KML共享provider为非exported，P0只通过私有FileProvider分享显式生成的GPX。旧首页、记录Activity、图表Fragment/View已从实际DEX剔除。该残留没有启用P0范围外功能，故不以重写成熟架构/重建已验收APK的方式作额外优化。

### Manifest、权限和数据安全

从公开下载APK直接解码binary AndroidManifest.xml，并与原始Gradle实际合并Manifest比较：

- 无INTERNET、ACCESS_NETWORK_STATE、Health、BOOT权限；未引入广告/账号/云同步。依赖清单和映射后DEX检查未见被审查的常见广告/统计SDK；P0源码中的HTTP字符串是GPX命名空间，不是网络请求。
- 仅4个Activity：兼容launcher跳转及P0首页/心率设置/本地历史；仅launcher exported。数据/FileProvider均private，服务为核心记录器及disabled的AppLocales元数据服务，无receiver。
- FGS声明mask0x18（location|connectedDevice），运行时按已授权能力和是否记录选择子集。GPS按钮只请求定位；BLE另行请求Scan/Connect，通知可选。仅GPS不要求蓝牙；HR预览gpsRecording=false，不启动GPS。
- 通知为低重要性持续前台服务通知，点击PendingIntent到P0Activity，未引用已移除的旧页面。后台GPS依赖正确FGS；长期锁屏/OEM行为仍需设备验收。
- 实际编译的allowBackup=false；legacy备份及cloud-backup/device-transfer XML对9个域全部exclude。FileProvider实际编译路径仅cache/p0_exports/。旧SQLite schema43和数据保留不依靠系统云备份。
- BLE30秒连接/发现/订阅超时，1/3/5/10/30/60秒封顶退避；手动断开保存禁用状态，重连策略可关闭。回调检查当前Gatt/measurement/session，close前清空旧Gatt引用；取消重试和可选读超时、注销receiver，避免旧连接污染新连接。原始单元/Native生命周期回归通过；这不证明真实RF。
- Service释放前禁用并注销SharedPreferences回调，迟到回调不能访问已释放成员。P0 UI通过ViewModel/Repository，不直接持有Gatt或操作DB。
- 原正式签名流程JKS只在RUNNER_TEMP，环境变量传密码、trap+always清理，不缓存/上传密钥。新增附件传递Job无Environment/签名Secrets，只有必要contents:write/actions:read；默认仍contents:read，没有PAT、没有新密钥。

### P0 功能和 UI 边界

已按源码、实际组件/资源、实际报告与截图核对GPS跑步/骑行、BLE扫描/选择/连接、BPM、接触/Energy/RR、断联/重连、原始样本、轨迹关联、暂停恢复结束、本地历史和HR最小/平均/最大、GPX及离线能力。没有新增地图、踏频/功率、分段训练、云同步、账号或广告入口。

重新核对20张P0亮暗/小大屏矩阵的实际PNG尺寸/主题，抽查小屏记录/详情和大屏设置，保留原有40张UI回归证据；正式APK的5个实际页面截图也与运行记录一致。小屏内容需要正常滚动，相关操作在Native/实际APK中可达；未以截图合成或随机BPM声称真实连接。

## E. 真实设备边界

以下全部为 NEEDS_DEVICE_VALIDATION：Magene H303实际连接/真实BPM、RF断连重连、户外GNSS精度/真实交付频率、锁屏后台、长时间运动、耗电、小米/MIUI/HyperOS和其他Android机型/版本权限表现、真实个人历史数据及跨时区显示。

[更新后的可操作清单](P0_DEVICE_CHECKLIST.md)包含佩戴与选择、离线GPS/HR记录、暂停移动、断连/重连、历史/GPX、重启、默认/允许后台电池策略分开测试、30分钟/两小时记录、实际时区切换及恢复。没有硬件连接到本次环境，没有编造任何PASSED设备项。至此停止扩展P0，等待真实手机与H303验收。

## F. Git、安全及阶段收口

main仍为9b5d7b06afc1eb8e3315ba73874ac088fa4d5169，原feature/heart-rate-monitoring仍为121ea381fd11111427073fe291dfb5ddabf8b03e；未合并main、未创建仓库、未覆盖旧Tag或Release、未更换签名密钥。本轮只增加一个指向3116e5f的新测试Tag和其Pre-release；分支代码修改仅在P0分支且限发布辅助脚本/CI/范围测试。最终文档提交不触发新APK构建，最终工作区清洁/远程分支一致核对记录在交付结果中。

| 阶段 | 状态 | 验证依据 |
| --- | --- | --- |
| 0 | PASSED | 原始Artifact新下载、SHA/大小/aapt2/apksig、提交关系/冻结分支 |
| 1 | PASSED | annotated Tag远程解析到真实构建SHA，不覆盖原Tag |
| 2 | PASSED | Draft434907895，正确标题/Tag/目标、prerelease及non-Latest |
| 3 | PASSED | 6个原始附件，原文档快照，Actions传递，无构建/密钥 |
| 4 | PASSED | Draft及发布后逐个重下载/哈希、公开browser URL、非Latest |
| 5 | PASSED | 实际XML379/378/1、JVM88、helper39、9GPX、两进程；原Ignore保留 |
| 6 | PASSED | 绝对存储/本地UI/ISO GPX分离；4时区160次数据语义检查 |
| 7 | PASSED，有记录的体积残留 | 实际DEX/资源/R8报告、旧版同变体对比；不声称零残留 |
| 8 | PASSED | 同签名、不可调试、6771->6772真实模拟器升级/schema43/历史GPX |
| 9 | PASSED | 实际binary/合并Manifest、备份XML、独立权限/FGS/生命周期 |
| 10 | PASSED | 仅P0可达功能，非P0保留代码不具入口/权限/驱动路径；UI矩阵 |
| 11 | PASSED（边界审查） | 真机项目全部NEEDS_DEVICE_VALIDATION，清单完整可操作 |
| 12 | PASSED | 仅修复发布CI请求和文档，不改生产代码，不需新APK/version；未降断言 |
| 13 | PASSED | 本报告、机器证据和最终Git/公开资产复核 |

机器证据：[p0-final-review.json](p0-evidence/p0-final-review.json)。历史开发验收：[P0_ACCEPTANCE.md](P0_ACCEPTANCE.md)。本轮发布过程：[P0_PUBLICATION_AND_REVIEW.md](P0_PUBLICATION_AND_REVIEW.md)。
