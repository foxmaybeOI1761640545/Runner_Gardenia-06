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
