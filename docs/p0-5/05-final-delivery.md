# P0.5 最终交付

状态：PASSED，P0.5 已完成开发、逐阶段验收、固定签名构建、P0.4 覆盖升级与公开 Pre-release 交付。最终核验日期：2026-11-10（Asia/Shanghai）。

[下载 APK](https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/releases/download/test-p0-v0.1.0-p0.5/RunnerGardenia-P0-Release.apk) · [完整 Release 与全部交付资产](https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/releases/tag/test-p0-v0.1.0-p0.5)

| 最终身份 | 已核验结果 |
|---|---|
| applicationId / variant | com.fox.runner / reproducibleRelease，非 debuggable |
| versionName / versionCode | v0.1.0-p0.5 / 6776 |
| APK 源码 / Tag 解引用 | a8891f61122712b81bee357f40d688151a948e57 |
| Tag 对象 | 84ca084c781aa4596dcfa09945d54c17fc89fc1f |
| APK 大小 | 2,853,444 bytes |
| APK SHA-256 | 7b0d0f48e2de5088f73ecd9272747beea4d79281d9dd8bb630cad821b1767598 |
| 固定证书 SHA-256 | E3E7D46BFCB869F5A521A7FBA6FD1BA5790D7DBCAF37F722DDC69D7BB1439A22 |
| 全量测试 / 签名运行 / 发布 Actions | 42758735766 / 42761772173 / 42764487155，均 SUCCESS |
| Release | [TEST][P0.5] v0.1.0-p0.5，ID439993653，Pre-release / 非 Latest |

| 原始报告统计 | Tests | Executed | Failures | Errors | Skipped |
|---|---:|---:|---:|---:|---:|
| API35 全量 Android | 425 | 424 | 0 | 0 | 1 |
| JVM 全量 | 124 | 124 | 0 | 0 | 0 |
| 发布/收据辅助 | 104 | 104 | 0 | 0 | 0 |

Android 唯一 Skipped 是原有 TrackRecordingServiceStateMachineTest.recording_stopGPS_noop；不计为通过，没有新增 Ignore/跳过或删减失败断言。NO-SOURCE 不作为执行证据。

功能 A/B/C/D 全部完成：四主题标题区域删除并收缩、运动/GPS/心率紧凑布局；主副指标精确原位交换与分主题持久化；六分类设置及原单位/速率/BLE/历史入口；历史多选、后台逐记录 GPX/ZIP、标准多文件分享和二次确认批量删除。Phase0–4报告全部 PASSED。最终精确源码296张原生矩阵截图与实际签名80张页面截图均人工审查通过；固定XSD的16份GPX、跨UID正负权限收据、数据库/真实新PID恢复、离线Manifest、受保护记录与分享不改源数据均通过。

实际使用原P0.4公版APK覆盖安装升级，历史GPS/原始心率/单位/速率/主题/设备配置保留，新增指标布局和历史批量功能可用。正式交付11个公开资产逐个重新下载并与不可变正式Artifact逐字节一致；APK再次独立完整验证v2密码学签名、内容摘要和二进制Manifest。原有全部远端分支/Tag及P0.4发布资产身份/哈希/大小/源码保持不变。

真实微信客户端、实体GNSS/BLE RF/耗电、TalkBack语音保持 NEEDS_DEVICE_VALIDATION；模拟器、合成GPS/心率数据和独立模拟接收器不代替这些验证。

下文按时间保留每轮失败、修复与验收记录；“尚未发布”等表述描述当轮状态。最终交付状态以本节及文末公开核验收据为准。

前置：Phase0至Phase4均有精确源码、Actions、实际原生截图/原始JUnit/失败保留与最终验收。最新Phase4源码50bc63c、Actions42657847351成功；Android78/78/0/0/0、JVM124/124/0/0/0、辅助94/94/0/0/0；验收提交5f700ba已推送核对。

版本分配前重新读取全部远端refs、公开Release build-info与本地台账，最高均6775，P0.5Tag不存在。预留v0.1.0-p0.5 / 6776，分配依据为5f700ba；不可把分配依据误写成未来APK源码。[分配收据](evidence/version-allocation/remote-version-check.json)。原历史台账逐条保留，仅附加新分配；正式APK的Tag将绑定最终全测/审图源码。

最终门禁清单：全部JVM/API35测试及原有回归；原始JUnit区分Tests/Executed/Failures/Errors/Skipped，原有唯一Ignore单独披露且不增加跳过；完整UI/八配色、离线/Manifest、所有GPX1.1 schema、数据库/真实新PID/缓存恢复、跨UID文件/ZIP完整性；固定签名assembleReproducibleRelease，先保留不可变候选；实际P0.4公版APK覆盖升级且历史/GPS/心率/设置保留；签名APK原生UI/指标持久化/历史批量行为；人工审查实际签名截图；发布专用激活只复用已验收Artifact，不重建/重签。Pre-release且非Latest，新Tag test-p0-v0.1.0-p0.5、标题[TEST][P0.5] v0.1.0-p0.5，所有公开资产与构建Artifact逐字节一致，匿名重新下载APK核对SHA/package/version/code/cert/size/tag源码。

本文件将在实际完成后补充所有构建/测试/发布ID、APK/校验和/构建信息/测试摘要/截图包/关键收据/Release Notes/完整提交记录。微信真实客户端、实体GNSS/BLE RF/耗电与TalkBack语音保持NEEDS_DEVICE_VALIDATION，不能把模拟接收器或模拟器当真实设备验收。

## 首轮全量回归：45ff582 / 42664349449

2026-11-10（Asia/Shanghai），[Actions 42664349449](https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/actions/runs/42664349449)整体SUCCESS，包括独立native-window-helper。已实际下载报告Artifact13698185521，ZIP SHA256=97d8ca1aaf6237d2e3a461c5bc9b2696f28ed8ee3abe1b952e5fa31eeb1633da，并核对GitHub不可变digest。独立重读原始JUnit：Android Tests425 / Executed424 / Failures0 / Errors0 / Skipped1；JVM124 / 124 / 0 / 0 / 0；辅助101 / 101 / 0 / 0 / 0。唯一skip是原有TrackRecordingServiceStateMachineTest.recording_stopGPS_noop，不算作通过，没有新增Ignore。

16份实际GPX通过固定官方1.1 XSD；最终Release/Debug Manifest离线检查通过；真实write PID1718→force-stop→read PID2224，各1/1/0/0/0，原GPS/心率/数据库、四主题指标及缓存恢复检查通过。该轮原始收据保存于[evidence/phase5-first-full-42664349449](evidence/phase5-first-full-42664349449)。

随后修正签名验收脚本对“恢复默认布局”的理解：现有PreferencesUtils通过移除当前主题override恢复默认，脚本同时核对移除与原生主指标恢复，并检查其他主题不变。修正提交7916056f3e92511de8d02337d3d592690d83e3f9未改变产品代码/数据路径；精确源码全量run42665221967进行中。第一轮通过不冒充后续源码已通过；尚未激活签名/发布，也尚未审查最终签名APK的80张截图。

## 最终全量门禁：c5093ed / 42670260321 attempt2

GitHub attempt1在创建Job前因内部服务器错误停止（0 Job/未执行测试），失败证据保留；同一源码attempt2整体SUCCESS。原始报告Artifact13732289318与截图Artifact13732513894均实际下载，ZIP摘要分别0440bd043534575a288960f4312b20b0baa2de217b9b24faa2b6582fe888bf54、5c2eae8579ec56aeeb106000f376f0d08eff3135f1f4a53b309fdfd8ea418cca，与GitHub不可变digest一致。

独立重计JUnit：Android425/424/0/0/1（原有唯一Ignore）；JVM124/124/0/0/0；辅助102/102/0/0/0。真实PID1838→2295，数据库/GPS/心率、四主题指标及中断缓存恢复通过；16实际GPX1.1 schema、离线Manifest、六份跨UID分享收据通过。全量原有UI回归与独立native-window-helper通过。

已逐组查看37张联系表对应296张真实原生截图：48顶部、192状态、16设置、8长时长、32历史。主布局、状态、指标及历史确认无裁字重叠；一张Quiet浅色设置Hub截到了OS换色过渡帧的标题/状态栏，最终签名截图关闭系统动画，发布前必须复查稳定页。截图均为模拟器/合成输入，不证明硬件或微信通过。[原始收据、矩阵摘要与人工审查联系表](evidence/phase5-full-42670260321)。

本次激活仅开放签名与签名包验收，尚未开放发布。签名APK须继续经过原P0.4覆盖升级、实际运行及80张原生截图审查。

## 首次签名包运行验收失败与原字节恢复

签名run42754884300成功构建、R8/资源压缩、对齐并核验原证书；实际候选APK2853388 bytes，SHA256=9603a836bdc51b15fb3e5390709e30a46c3496e6d4560e594426554ffe8af042。下载后独立Python密码学v2签名/内容摘要与二进制Manifest验证com.fox.runner、v0.1.0-p0.5/6776及原固定证书均通过。

但实际运行验收失败，不能宣称升级/运行通过：偏好对话框点击后连续两次Back与关闭动画竞争，最后截图/原生XML确认为Settings Hub，随后找不到运动状态控件。原报告Artifact13735122739摘要6e16c814863b1fc3725685e2bb0dce0f2e79cd23c56391e8fca33a54e9b23315已核验，失败截图、窗口、日志与签名优化证明归档[evidence/phase5-signed-failed-42754884300](evidence/phase5-signed-failed-42754884300)。

修复仅在新运行验收脚本中逐页等待实际原生目的页：Preferred units→Back→Theme and appearance→Back→Recording；原业务/原测试/版本/签名/发布代码不变，原c509全量门禁仍有效。专用恢复工作流没有Gradle构建、签名Secrets或密钥步骤；严格校验原失败job仅运行步骤失败、原签名构建成功、固定candidate Artifact身份/摘要、原APK完整签名/哈希、原全测报告及冻结源码，并重新执行完整实际运行/升级/80图/分享门禁。恢复只能更改新运行脚本/工作流/宿主辅助测试及报告，任何产品/原测试/版本/旧门禁改动均拒绝。新增宿主测试加原回归104/104/0/0/0通过；与原APK全测辅助102项分别记录。尚未发布。

## 签名包人工审图拒绝：42755978037

恢复工作流42755978037整体SUCCESS，同一9603…APK逐字节与原签名候选一致，独立v2密码学签名/内容摘要/Manifest复核通过。实际P0.4覆盖升级保留设备/单位/速率/主题、原GPS1点及原始心率2样本；新增GPS34点/22.48m、暂停恢复2段、force-stop恢复/GPX1.1通过。80个实际原生页面、8组指标新进程持久化/当前主题恢复默认、4组ZIP及直接多GPX分享且数据库签名不变通过。正式Artifact13736925719 SHA=c0a7bc4334ea0ca55695086caeceb9847332c10a1b3041df793fd1bdfa191a14已下载核验。

但人工查看前6类48张签名截图时发现应用信息仍显示OpenTracks（原R.string.app_name），与Manifest实际Runner Gardenia P0标签不同，因此人工UI门禁拒绝，不能发布。剩余历史等4类32图未继续审查，不宣称80图均人工通过。Quiet浅色设置Hub在关闭动画的稳定截图中标题/状态栏正确，原过渡帧问题已澄清。

修复设置页使用ApplicationInfo.loadLabel实际应用标签，增强原Android测试对实际标签/Runner Gardenia品牌断言，并在正式签名截图检查应用标签。同步把已验证的逐页Back等待集成回正式运行验收脚本，保留所有原GPS/恢复/数据断言。产品源码发生这一实际修复，必须新的精确源码全量测试和原固定密钥构建/升级/截图审查；不能把旧候选作为最终发布。旧候选、成功运行收据与人工拒绝截图归档[evidence/phase5-signed-ui-rejected-42755978037](evidence/phase5-signed-ui-rejected-42755978037)。P0.5Tag/Release尚未创建，版本6776保留同一未发布分配；所有旧发布不变。

## 品牌修复后完整门禁：a8891f6 / 42758735766

精确源码a8891f61122712b81bee357f40d688151a948e57的[完整Actions](https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/actions/runs/42758735766)整体SUCCESS（checks及native-window-helper）。两份不可变Artifact实际下载/摘要核验：reports13737173196=214ae192442b3ec4f2a7f049f8bf7eba4a32051c60c6a2594b80b685d6102d63；visual13737387227=f1b1130e5030c2a26f918811e151fcb79e9ecccc5431d1c41490fd30dc4bf227。独立原始JUnit重计Android425/424/0/0/1（原有Ignore）、JVM124/124/0/0/0、辅助104/104/0/0/0。实际进程PID1828→2208、16份GPX1.1 schema、六份跨UID分享/权限、离线/Manifest和所有原有回归通过。新增实际应用标签断言通过。

已实际查看全部37张联系表、296张原生截图（48顶部/192状态/16设置/8长时长/32历史），四主题八配色无横向溢出、文字重叠或标题换色过渡。原始XML/收据、矩阵摘要和联系表归档[evidence/phase5-brand-full-42758735766](evidence/phase5-brand-full-42758735766)。模拟输入仍不证明实体硬件或微信兼容性。

本次仅激活新源码的固定密钥签名及实际覆盖升级/80张原生截图门禁，尚未开放发布。旧9603…候选因应用名称错误被拒绝，不作为最终APK。

## 新签名包验收通过：42761772173

[固定签名构建与实际运行](https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/actions/runs/42761772173)整体SUCCESS。实际APK源码a8891f61122712b81bee357f40d688151a948e57，构建流程提交cf1c8bf3795ffd8220e90fdb85facef818d39942，正式Artifact13738852270，ZIP SHA256=b763f1134e55184ef8285668a0ae942ea69dffa0f02640942298a67276a07746。全部11项正式资产摘要核验通过；APK与不可变候选逐字节一致，独立完整v2密码学签名/内容摘要/二进制Manifest通过：com.fox.runner、v0.1.0-p0.5/6776、2853444 bytes、SHA256=7b0d0f48e2de5088f73ecd9272747beea4d79281d9dd8bb630cad821b1767598，固定证书E3E7D46BFCB869F5A521A7FBA6FD1BA5790D7DBCAF37F722DDC69D7BB1439A22。

实际安装原P0.4正式APK后覆盖升级，设备/单位/速率/主题保留，原GPS1点及原始心率2样本保留；新记录33个模拟GPS输入点/22.48m，暂停恢复2段、force-stop进程恢复、原生速率切换不改变记录、GPX1.1全部通过。指标新进程持久化/当前主题恢复默认、8组ZIP/直接多GPX分享且数据库记录签名不变均通过。

已逐组查看全部10张联系表对应80张实际签名页面（首页、Hub、外观、运动显示、设备、应用信息、历史、选择、分享、删除）；四主题八配色360dp/1.3字体无裁字重叠，Quiet浅色Hub标题/状态栏稳定，应用信息正确为Runner Gardenia P0、v0.1.0-p0.5、6776。80份原生窗口XML/焦点Activity/PNG与摘要一致。[构建身份、运行收据与人工审图证据](evidence/phase5-signed-accepted-42761772173)。模拟器/合成记录不能证明真实微信或实体GNSS/BLE/TalkBack语音通过。

本次发布激活仅转移已验收的正式Artifact原字节，禁止重建/重签；P0.5整体仍待公开Release与全部公开资产独立下载核验后才能宣布完成。

## 最终公开交付核验：42764487155

发布激活提交584665f6f7aa393432a837b29fe9c0265aff7f00仅增加发布审批与验收报告，没有产品/测试/版本/构建/签名代码变化。[发布Actions](https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/actions/runs/42764487155) SUCCESS，下载并核验publication Artifact13738937338，ZIP SHA256=72b92f56d3fdade62709f64f43613191623a3e952d6cb8721432064ae290316d。流程先核对全部Draft资产，发布后无Authorization公开下载APK并再次验签，HTTP200。

随后独立从公开Release下载全部11个资产，与正式Artifact13738852270每个文件逐字节一致，APK完整v2密码学签名/内容摘要/Manifest再次验证通过。注释Tag对象84ca084c781aa4596dcfa09945d54c17fc89fc1f解引用a8891f61122712b81bee357f40d688151a948e57；不是签名流程提交cf1c8bf或发布审批提交584665f。Release ID439993653，draft=false、prerelease=true；仓库稳定Latest接口404（当前无稳定Latest），P0.5未设为Latest。独立脚本首轮因未处理此404停止，保留记录后按“无Latest”修正宿主脚本并重新完整执行；未改变产品、发布代码或已发布资产。

全部原始远端refs在公开交付后再次逐项比对基线，保持不变；P0.4 APK资产ID724746708、2803864 bytes、aa5ebc21…b4224ebc与原注释Tag/实际源码不变。公开APK资产ID730286622，2853444 bytes，SHA7b0d0f48…b1767598。最终证据见[evidence/phase5-public-42764487155](evidence/phase5-public-42764487155/independent-public-verification.json)。

交付资产均在上述Release：RunnerGardenia-P0-Release.apk、SHA256SUMS、build-info.json、test-summary.json、theme-screenshots.zip（80份实际签名截图及原生窗口）、key-test-receipts.zip、RELEASE_NOTES.md、DEVELOPMENT_COMMITS.txt、runtime-smoke.json、native-theme-smoke.json、ARTIFACT-MANIFEST.json。本地公开下载副本位于/workspace/output/RunnerGardenia-P05，并附independent-public-verification.json。完整阶段报告00–05、所有失败收据与审图联系表在新开发分支保留。所有开发仅推送feature/p0-5-dashboard-settings-history，未向main或旧开发分支提交。

P0.5 最终门禁全部完成；真实设备/微信待验证事项如上，未虚报通过。文档归档提交不重建、不重签、不更改已验收Tag或任何Release资产。
