# P0.7.1 独立维护交付

一级页面返回历史已修复，本版独立固定签名 Pre-release 已发布，并通过全量源码测试、API35 实际 APK 运行与覆盖升级、原生截图审查、独立公开下载验证。实体手机 GNSS、BLE 射频、OEM 预测性手势和后台策略仍待验证。

## 版本身份

| 项目 | 值 |
|---|---|
| 分支 | `fix/p0-7-1-navigation-backstack` |
| Draft PR | [#6](https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/pull/6)，保持未合并 |
| 产品源码 / APK 构建源码 | `857c744321825a2656d77f45d57c6e43b4334962` |
| Tag 解引用 Commit | `857c744321825a2656d77f45d57c6e43b4334962` |
| Tag | `test-p0-v0.1.0-p0.7.1` |
| 源码测试 Run | [45086993985](https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/actions/runs/45086993985) |
| 签名构建 Run | [45092892094](https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/actions/runs/45092892094) |
| 签名工作流与审批 Commit | `348b225c68e90f095fc4f9207ba872c9205bed66`，不是 APK 产品源码 |
| 发布 Run | [45096590087](https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/actions/runs/45096590087) |
| Application ID | `com.fox.runner` |
| versionName / code | `v0.1.0-p0.7.1 / 6781` |
| APK bytes | 3238755 |
| APK SHA-256 | `1c68922da5052c8b9dc224194e719116ccfafd616905f00d6909b837ebd82759` |
| 固定证书 SHA-256 | `E3E7D46BFCB869F5A521A7FBA6FD1BA5790D7DBCAF37F722DDC69D7BB1439A22` |
| Release | [https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/releases/tag/test-p0-v0.1.0-p0.7.1](https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/releases/tag/test-p0-v0.1.0-p0.7.1) |
| APK | [下载 APK](https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/releases/download/test-p0-v0.1.0-p0.7.1/RunnerGardenia-P0-Release.apk) |
| 发布属性 | 独立 Pre-release、非 Latest；既有资产不覆盖 |

交付文档在产品源码之后独立提交。此报告最后一次提交为交付记录提交，不代表重新构建；最终交付 HEAD 的准确 SHA 记录在 PR #6 及连续任务的 P0.8 基线/总报告，使用前必须确认它与产品源码的差异仅为文档和审批记录。不能把该 HEAD 当作 APK 源码。

## 修复范围与根因

原四个顶层 Activity 已通过 REORDER_TO_FRONT 保留实例，但默认 Back 仍遍历访问历史；设置/历史根返回还永久关闭了内部 Back 回调。现保留原 Activity、ViewModel 与内部 Fragment 结构，通过 AndroidX 根返回将任务置于后台，内部页面、选择/忙碌状态和弹窗优先处理原返回。重复点击与切换中重复启动被拒绝，首页/设置快捷入口也复用同一根实例。

复用 Heart Activity 暴露 stale ViewModel 在 Home 已暂停后误判会话结束、移除 journal 的回归。重新观察服务时恢复既有未结束会话，状态回调读取服务同步状态；原始失败和最小修复见04。生产仅修改六个文件，完整清单见 final-release/changed-files.json；GPS/BLE driver、TrackRecordingService、RecordingSession、SQLite schema43、地图和主题生产代码保持原样。

## 自动化与导航结果

| Suite | Tests | Executed | Passed | Failures | Errors | Skipped |
|---|---:|---:|---:|---:|---:|---:|
| Python | 160 | 160 | 160 | 0 | 0 | 0 |
| JVM | 167 | 167 | 167 | 0 | 0 | 0 |
| API35 full | 489 | 488 | 488 | 0 | 0 | 1 |
| Navigation focused | 18 | 18 | 18 | 0 | 0 | 0 |
| GPS/business focused | 47 | 47 | 47 | 0 | 0 | 0 |
| Home map focused | 2 | 2 | 2 | 0 | 0 | 0 |
| 独立权限进程 | 3 | 3 | 3 | 0 | 0 | 0 |

Focused/full 重复用例不累加。唯一历史 Skip `TrackRecordingServiceStateMachineTest.recording_stopGPS_noop` 不计通过；158 个原有 Java 测试不变，没有新增 Ignore/Skip。Release、Lint、GPX1.1、离线属性、持久化和实际新进程恢复均通过。T01–T18 明细与原始方法名见06及独立 JSON：一级循环/快速/重选/实例唯一、全部根系统Back、API35实际边缘手势、设置内部返回、历史详情、设备与删除弹窗、重建、真实旋转、后台恢复、运动切换、GPS/BLE合成数据连续性和四主题八配色全部通过。

源码构建517张原生PNG已实际审查，另审查3张原有状态导航原图。固定签名APK的99张原生PNG/XML已独立校验并实际查看（native88、合成地图4、真实地图7），另有 34 次实际根Back后台/显式回Home检查。截图收据绑定实际APK摘要，fixture不冒充真实地图或硬件。

## 固定签名、升级和独立公开验证

固定原JKS在受保护Environment签名；实际R8和资源优化、apksigner、aapt、ZIP alignment、Manifest/无INTERNET属性通过。本地使用公开证书独立验证APK v2密码学签名、完整内容分块摘要、ZIP CRC与166个存储条目4-byte对齐，篡改副本均被拒绝；无本地SDK和私钥。

API35安装已发布P0.7.0正式APK后使用install -r覆盖，不卸载、不清数据。历史fixture保留1个GPS点、2条原始心率样本及schema43；主题、单位/速率、设备/重连配置、GPS来源与7秒采样设置、原100MB地图文件SHA-256及索引选择保留。实际记录、暂停/恢复、强停后恢复、GPX导出和schema验证通过，空闲/准备定位不创建运动。合成GNSS/BLE输入不等于实体硬件验收。

发布复用已验收不可变APK，不重新构建。完整资产清单和每个文件摘要见 publication.json / ARTIFACT-MANIFEST.json。匿名公开资产API下载HTTP200，摘要、大小、签名正确；另在没有签名密钥的全新runner匿名下载独立复核。浏览器下载链接见上表，实际匿名验证通道明确为public asset API，不冒充本网络未执行的浏览器下载。

## 保留记录与后续

00–06及evidence保留全部失败、取消和修复，不以最后成功抹去历史。旧P0.7.0及更早Tag/Release/资产和Draft PR#5保留；本PR不自动合并。真实手机/H303、卫星、BLE射频和OEM手势继续NEEDS_DEVICE_VALIDATION。此APK不包含路线库或路线跟随。

本版独立发布、公开验证和交付报告全部完成后，连续任务自动从正确交付HEAD进入P0.8.0；按最新用户范围实现GPX独立路线库、Mapsforge叠加、GPS跟随、进度与偏离距离，不引入BRouter/自动路由/重新规划/语音导航。不得将后续计划写作本版已实现。
