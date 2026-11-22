# P0.7阶段性交付报告：阶段1～3

按用户“先把阶段一二三完成之后，可以先暂停”的最新范围，完成首页真实离线地图、前台全局定位及GPS预览与正式运动交接，交付P0.7测试Pre-release后暂停。P0.8、P0.9、P1.0及步态技术设计未启动，不列为完成。

## A. Git状态

- 正式验收文档基线ae68319361c1277e97e4fe9bfc9ae52de88146e5；P0.6.1应用/测试源码6cee1958a23bfd1a1fe3b6a9b547717c528cbc75。开始前重新fetch核对，审计见00-baseline-and-architecture及baseline-version-audit.json。
- 新分支feature/p0-7-to-p1-0-offline-navigation-clean，从正式文档HEAD创建；未修改main。
- 阶段1测试源码e08887d，验收c69ac06；阶段2源码3e389c1，验收aebc672；均先独立验收提交推送，再开始下一阶段。
- 阶段3主要提交00fb224（临时GPS模式/交接/恢复）、289ce60（完整交付门禁及6780独立预留）、f794dce（通知角色修复）；首轮完整验收f15b054；467f208修正热GPS条件下签名地图验收脚本，生产树不变；重新完整验收8f20457。当前签名审查激活87c87b7；签名/发布实际产品源码均为`467f2086c0f712e04430382203da2bde0c870acb`。完整开发提交列表随Release的DEVELOPMENT_COMMITS.txt保存。
- [Draft PR #5](https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/pull/5)目标feature/p0-6-map-real-validation，未自动合并。
- 实验分支e7f5ee7b475ea02961489f540bd262f9e916fc3d及PR4保持REFERENCE ONLY — DO NOT MERGE，不在clean祖先中，未合并/整体cherry-pick/构建正式包/修改历史。失败Run43696204580保留失败。最终审计核对36个历史受保护refs及11个旧Release和全部旧资产均未变更，详见evidence/p07-publication/history-audit.json。

## B. 版本交付

唯一成功新发布版本：[v0.1.0-p0.7.0](https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/releases/tag/test-p0-v0.1.0-p0.7.0)，Version Code6780，非Latest测试Pre-release。Tag为test-p0-v0.1.0-p0.7.0，指向上述实际验收源码，注释绑定APK/证书摘要。6780来自全部refs/11个Release build-info/远程台账/Tag属性审计后的独立预留；没有复制实验分支的未发布版本配置，没有覆盖P0.6.1包。

## C. 功能与阶段

| 阶段 | 实现/自动测试/交付状态 | 边界 |
|---|---|---|
| 1 首页地图 | 已实现、已验收、P0.7已发布 | 真机性能仍待验 |
| 2 全局前台定位 | 已实现、已验收、P0.7已发布 | 实际GNSS及OEM策略仍待验 |
| 3 GPS运动交接 | 已实现、已验收、P0.7已发布 | 两款手机及H303组合仍待验 |
| 4 GPX路线库/导入 | 未启动 | 按用户范围暂停 |
| 5 跟随/路书/离线规划 | 未启动 | 不宣称P0.7支持 |
| 6 导航辅助/重规划 | 未启动 | 不宣称P0.7支持 |
| 7 步态技术设计 | 未启动 | 未新增传感器生产功能或设计交付 |

首页复用OfflineMapRepository和Mapsforge0.25.0，加载用户选定真实.map，绘制当前位置及水平精度；默认跟随，手动拖动/缩放暂停跟随，定位按钮恢复。没有地图或当前位置当前缩放无可用瓦片要素时显示中性空白和定位状态。要素检查不只依据外接矩形，也不把有要素瓦片等同完整行政/道路覆盖。资源随视图销毁/主题重建/地图替换删除释放，缓存仍不超过32MiB/64瓦片。

Application生命周期统一管理前台预览，页面共享最新Fix，内部切页不重复获取GPS；无运动退后台释放预览。复用TrackRecordingService、SensorManager、GpsHandler的单一路径，正式运动由现有Foreground Service管理，前台恢复不重复启动驱动。过期、弱精度、无时间戳、跳点及无权限状态分别处理。

P0运动采用临时内部1Hz运行时模式，保留用户GPS来源、采样间隔及存点距离；正式确认后才计时写点，预览GPS/HR不混入运动，暂停恢复不跨段连接，结束后前台恢复预览且不生成第二条历史。原SQLite Schema43、BLE采集、GPX1.1导出、历史/ZIP分享、四主题及FOLLOW_SYSTEM、包名/签名保持升级兼容。异常恢复保留既有运动；GPS权限失效时失败关闭，不声称绕过系统强制停止。

## D. 路由引擎与数据

本轮范围截至阶段3，没有集成BRouter或其他路由引擎，没有.rd5、路由下载/删除、跑步/骑行Profile、自动离线规划、长距离规划或联网备用。路由引擎增加体积不适用；整个正式APK较P0.6.1增加1,876字节。地图渲染数据不能替代路由数据，未把这些后续能力标为已交付。

## E. 测试与证据

所有Android构建、测试、签名、安装运行及发布在GitHub Actions执行；用户本机不需SDK，开发工作区未安装Android构建环境。采用原Wrapper/AGP、JDK21、API37/Build Tools36.0.0和API35 Managed Device，未为本任务升级工具链。

| 完整同源码回归 | Tests | Executed | Failures | Errors | Skipped |
|---|---:|---:|---:|---:|---:|
| Python Helpers | 146 | 146 | 0 | 0 | 0 |
| JVM | 167 | 167 | 0 | 0 | 0 |
| Android API35 | 471 | 470 | 0 | 0 | 1 |

唯一历史Skip为TrackRecordingServiceStateMachineTest.recording_stopGPS_noop，不计通过。原owner/偏好/心率测试源码与正式基线逐字相同、原断言保留，无新增Ignore。额外GPS专项45/45、首页真实地图2/2、三组独立权限进程及六个持久化write/read阶段均0失败/错误/Skip，单独列出，不叠加进471项去重总数。

[完整验证44987372985](https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/actions/runs/44987372985)包含Debug、Lint、全部JVM/Python/API35、19份实际GPX官方1.1 XSD验证、六组跨UID分享、数据库/指标/批量缓存新进程持久化、真实105,834,033-byte地图与中断导入在飞行模式下重启及原生窗口工具验证。已下载重算Artifact摘要、重新解析原始JUnit，完整回执和XML见evidence/run44987372985-full。失败轮次44979967128及原始XML保留，两个新通知断言修复后执行首轮全量回归。首次签名运行44984776201在真实地图热GPS视口定位场景失败，未发布；证据保留，修正脚本仍保留全部原断言并增加中心核验，467f208重新执行本次全量回归，随后签名运行45008081843完整成功。

实际查看本轮492张Debug原生PNG及签名APK全部99张原生PNG。覆盖四主题日夜、小/普通/大屏、大字体、地图有无数据、记录/GPS/HR/历史/设置；签名88张原生窗口焦点/页面XML与截图对应，另4张离线地图、7张真实地图。320dp大字体下Import .map换行较生硬，但控件完整可用，保留为外观改进项。布局/合成地图夹具仅作原回归，实际地图验证有独立真实文件证据；不使用HTML模拟截图。逐图文件/SHA/审查说明见两份visual-review.json。启动淡入、刻意滚动取景与旧多轴图表较密等边界已记录，未据淡入图判断稳定主题配色。

真实地图测试导入5,373ms，运行期间采样Java堆峰81,879,776 bytes、PSS峰299,642KiB，Java堆上限201,326,592 bytes；32MiB/64瓦片预算及多次切换/重建断言通过。这些为模拟器采样，不替代真机持续内存/启动/电池验证。

[签名与实际升级运行45008081843](https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/actions/runs/45008081843)在android-release使用原固定密钥，先重新独立核验完整证据，执行实际R8/资源优化构建，验证正式APK，再在API35上用原P0.6.1签名包覆盖升级、保留历史/地图/配置、实际GPS输入/暂停恢复/结束、强制停止后的数据与显式恢复、GPX导出及主题/地图原生界面。99图实际审查及验收提交07ca3ac后，由df0769c另行启动发布；发布Job复用不可变APK，不重新构建或签名。

[发布与独立公开下载45011169008](https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/actions/runs/45011169008)最终attempt3成功。前两次匿名下载分别遇GitHub共享IP限流403，失败日志与当时Run JSON保留；只重跑失败Job，15个公开资产ID及摘要始终一致，没有重签或重建。发布前下载核对全部Draft资产，随后发布非Latest Pre-release；另一全新Runner无签名环境/密钥、匿名下载公开APK，重新运行包名/版本/证书/摘要验证。

## F. 正式APK

| 项目 | 实际值 |
|---|---|
| Version Name | v0.1.0-p0.7.0 |
| Version Code | 6780 |
| Application ID | com.fox.runner |
| 大小 | 3,236,595 bytes |
| APK SHA-256 | 3c6ad819e740f82b24063d3be6da9e259f69b959bb9a54c1145b6974b085beb7 |
| 证书SHA-256 | E3E7D46BFCB869F5A521A7FBA6FD1BA5790D7DBCAF37F722DDC69D7BB1439A22 |
| Release | https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/releases/tag/test-p0-v0.1.0-p0.7.0 |
| 独立公开下载 | PASS，匿名HTTP200，包名/版本/证书/SHA一致 |

正式APK为优化的reproducibleRelease、debuggable=false。固定JKS只恢复到GitHub Runner临时目录并在所有结果下清理；未新建或提交密钥、未打印Secret、未将密钥放入Artifact。内部Debug com.fox.runner.debug只是测试产物，不替代正式升级包。原P0.6.1 APK及全部历史资产保留。

## G. 步态分析设计

按用户将本轮范围限定为阶段1～3的最新指令，本轮不启动阶段7，不提交尚未开展的技术可行性结论或设计交付，不增加手机传感器生产采集。

## H. 真实设备验证

Xiaomi CIVI5Pro、Redmi Note12Pro+和Magene H303均NEEDS_DEVICE_VALIDATION。实际卫星定位、OEM锁屏/省电策略、真实BLE射频/断连重连、长时间GPS/地图/心率耗电、温升和真实分享目标尚未在实体设备执行。自动化使用模拟器注入位置和合成HR，不能填作真机通过。检查与回执要求见07-real-device-checklist.md。

## I. 最终状态

阶段1～3已实现、自动验收、固定签名并发布P0.7测试版本，公开APK独立验证通过。当前无已知未解决的本轮P0/P1自动回归或交付门禁失败；仍有上述真实设备验收未完成。此结论不是全设备稳定性保证。

按用户要求暂停，不启动P0.8及后续。下一轮产品开发应基于该已发布源码和验收证据，并先纳入真机反馈；本Draft PR保持待审查且不自动合并。
