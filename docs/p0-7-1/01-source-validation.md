# P0.7.1 源码验收（签名、升级和发布尚待执行）

本次验收绑定产品源码 `a189bca91b92fd3258d3ace35c3523bf1cba4688`，`src/main` tree `7d6f1b6751ec8d01e278c2e14db21dbf6f66a0cc`。分支 `fix/p0-7-1-navigation-backstack`；Draft PR #6。版本 `v0.1.0-p0.7.1 / 6781` 已审计台账、全部远程分支、Tag、Release 和公开 build-info 后唯一分配。原发布资产与 PR #5 保留。

Actions [45053438403](https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/actions/runs/45053438403) 完整阶段门禁成功；下载不可变 Artifact 后独立检查原始 JUnit，重新运行交付证据校验。完整原始 XML/JSON 见 `evidence/run45053438403-full/`。

| Suite | Tests | Executed | Passed | Failures | Errors | Skipped |
|---|---:|---:|---:|---:|---:|---:|
| Python | 156 | 156 | 156 | 0 | 0 | 0 |
| JVM | 167 | 167 | 167 | 0 | 0 | 0 |
| API 35 full | 489 | 488 | 488 | 0 | 0 | 1 |
| T01～T18 focused | 18 | 18 | 18 | 0 | 0 | 0 |
| GPS focused | 45 | 45 | 45 | 0 | 0 | 0 |
| Home-map focused | 2 | 2 | 2 | 0 | 0 | 0 |

Focused 和 full 有重复用例，不能累加为不同测试。API35 唯一原有 Skip：`TrackRecordingServiceStateMachineTest.recording_stopGPS_noop`，未计为通过。没有修改原有 Java 测试、增加 Ignore 或降低断言。Release assemble、Lint、原有 GPX1.1/离线检查、地图及数据持久化/新进程恢复、权限拒绝/授予/拒绝恢复的三个独立进程检查均通过。

T01～T18 全部通过，原始方法名和结果位于独立复核 JSON 及 XML。T08 实际启用 API35 gestural overlay，等待 SystemUI mode=2 后发送边缘 swipe，断言任务根离开前台；它是模拟器真实系统手势路径，不是实体手机或 OEM 预测性动画验收。T12 覆盖项目实际使用的删除 DialogFragment；本应用该路径没有独立 BottomSheet。T16/T17 在一级切换、根返回、暂停/恢复场景验证运动状态、服务及合成 GPS/BLE 样本持久化；不冒充实体卫星/蓝牙射频测试。

最终交付清单内 517 张原生 PNG 已实际查看（40 张联系表覆盖 480 张；37 张已审查且 SHA 完全一致的 focused PNG 复用视觉收据）。四主题八配色、不同窗口/字号、历史批量选择/删除/分享、设置分类、离线地图与真实地图缩放、重建、导航详情/弹窗、运动和心率状态未发现本轮导航修复引入的视觉异常。布局夹具的中性地图占位不作为真实地图证据。真实地图的覆盖边界空白保留；已有窄屏滚动和旧图表密度未在维护版本扩改。

失败和取消记录仍保留：首次导航 T08 未等待 SystemUI 异步模式切换而失败，修正等待后实测通过；旧通用 PR 工作流不能识别维护系列，已将维护 PR 指向完整独立门禁；中间候选在运行导航前被取消，不能计为通过。具体 Run 及原始失败记录见 evidence。

目前仅源码/自动化/API35/原生视觉门禁通过；固定签名、P0.7.0 覆盖升级、正式 APK 运行截图、独立 Pre-release、匿名公开下载及最终交付均尚未完成。P0.8.0 未建分支、未修改生产代码。固定签名与升级必须继续通过，才允许发布；全部发布及报告完成后才进入 P0.8.0。
