# P0.5 最终交付

状态：IN_PROGRESS，不能宣布P0.5完成。

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
