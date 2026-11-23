# P0.7.1 导航维护：实施与验收计划

本任务采用 2026-11-23 最新用户指令：先独立完成 P0.7.1 的代码、测试、固定签名、P0.7.0 覆盖升级、独立 Pre-release、匿名下载及交付；全部通过后自动开始 P0.8.0。P0.8.0 的最新范围包含独立路线库、GPX 路线导入、地图叠加、GPS 路线跟随、进度及偏离距离。本维护版本不含这些功能。

## 基线身份

- P0.7.0 APK 源码 / Tag 解引用：`467f2086c0f712e04430382203da2bde0c870acb`。
- P0.7.0 clean 交付 HEAD：`cf679d7d7c5870ace7aec0a68b356fc303997273`。
- clean 是产品源码后续 6 个提交，差异仅 docs 和 `.github/p07` 的两份审批 JSON；`src`、构建工具链、版本配置、台账相同。
- 维护分支：`fix/p0-7-1-navigation-backstack`，从 clean 交付 HEAD 派生；PR #5 保持原 Draft。
- 重新审计全部远程分支、Tag、12 个 Release 的真实 build-info：最高 6780；独立预留 6781。审计见 evidence/baseline-version-audit.json。

## 根因与最小方案

四个顶层 Activity 原来用 REORDER_TO_FRONT 保留实例，但默认 Back 仍逐个暴露之前访问的 Activity。设置与历史的内部返回回调在到达根节点时还永久关闭了自身，重新进入内部页面后不能保证正常返回。

保留现有四个 Activity 及其 ViewModel、Fragment 和界面状态。一级根节点通过 AndroidX Back dispatcher 将整项任务置于后台，符合 Android launcher 根页面的返回行为；不 finish、不停止服务。设置分类、历史详情/选择/忙碌保护和弹窗继续先处理内部返回。回调不再永久禁用。顶层切换加 SINGLE_TOP，并在切换启动到下次 onResume 之间拒绝重复请求。页面数量保持最多每类一个，返回不会遍历访问过的顶层页面。

生产修改涉及 P0RecordingViewModel（仅重新观察服务时同步未完成会话，原因见04暂停日志回归报告）、P0Navigation、P0SettingsActivity、P0HistoryActivity，以及首页和设置分类中通往已有一级页面的快捷入口（P0Activity、P05SettingsSectionFragment）。快捷入口也使用相同的 REORDER_TO_FRONT/SINGLE_TOP，避免从首页或设置重复创建心率/历史根；历史管理请求通过 onNewIntent 进入原管理模式，正在执行的批量工作仍受保护。GPS/BLE/TrackRecordingService、数据库 schema 43、地图及主题业务代码保持原样。没有重写导航架构。

## 独立门禁

`.github/p071/`、p071-navigation.yml 和 p071-release.yml 独立维护；不改变 `.github/p07/phase.json` 或旧验收收据。完整复用原 Python/JVM/API35、GPS 专项 45 项、地图 2 项、权限、GPX1.1、持久化、原生截图门禁。全部旧 Java 测试逐字保护，新增 18 项 API35 测试。只允许原历史 Skip，NO-SOURCE 和缺失结果均拒绝。

T01 运动→历史；T02 历史→设置；T03 设置→心率；T04 连续循环与实例唯一；T05 重复点击；T06 快速连点；T07 每个根页面真实系统 Back 与恢复；T08 API35 实际边缘手势；T09 设置分类及根返回后再进入分类；T10 历史详情/列表/根；T11 心率设备管理弹窗；T12 删除确认弹窗先返回且不删除；T13 Activity 重建保留内部栈；T14 实际旋转；T15 Home 后恢复内部页；T16 运动中切页及根返回；T17 切页期间真实服务持续接收合成 GPS/BLE；T18 四主题×明暗导航。

当前实现的设备管理与删除确认采用 DialogFragment，没有独立 BottomSheet 页面；T11/T12 验证实际存在的弹窗。T08 验证 API35 模拟器系统手势，不替代真机预测性动画、取消手势和 OEM 手势验收。

## 真机验收待办

Xiaomi CIVI5Pro、Redmi Note12Pro+ 分别执行 T01～T18，尤其左右边缘手势、预测性返回启动/取消/提交、旋转、系统回收恢复和锁屏后台；搭配 Magene H303 检查真实连接/断连/连续采样及 GNSS 运动不中断。云端模拟器和合成输入不能标作实体设备通过。

所有状态在实际执行后写入独立报告。当前文档是计划与实现说明，不宣称尚未执行的 Android、签名、升级或发布已经通过。

版本台账新增维护预留时，需要让 release_policy 识别 reserved-p071-test，并让旧 p07_version 精确读取自身的历史预留行。保留原台账前缀、编号连续性及全部原测试断言，未修改旧 phase/审批/测试收据。初次本地 Python 151 项中 6 项因上述兼容问题报错，修复后新增准入测试共 156 项全部通过；后续 Actions 实际结果及失败修正见 01、02、03 文档，不能以本计划末尾的早期状态代替最终报告。
