# 阶段3验收：GPS预览与运动交接

PASS（代码、完整回归及原生截图）。P0.7公开测试版本尚未发布；固定签名、优化APK实际升级/运行、99张签名截图审查与公开下载核验仍为发布前置条件。按用户最新范围，阶段4及后续不启动。

后续交付状态：首次签名实际运行Run44984776201失败，见[发布验证过程](08-release-validation.md)。本验收记录仍只对应下述f794dce与Debug回归；修正签名验收脚本后必须重新验证新候选，不能用本记录冒充正式APK完整交付。

## 来源与顺序

正式基线ae68319361c1277e97e4fe9bfc9ae52de88146e5，clean分支。阶段1验收c69ac06、阶段2验收aebc672均先提交推送并核对远程，再开始下一阶段。

本阶段实际测试源码：`f794dce94d843bc4d890c05436ab1c82bd64e6e2`；生产树`1b3c03950ef0ba319bb3d79abb02841c35028f0c`。

[完整Run44980804037](https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/actions/runs/44980804037)成功。Artifact14738987959：`Clean-navigation-stage-44980804037-1`，199,958,855 bytes，ZIP SHA-256 `25bd18a37eba40cce1b18925db4a860fa16f4a4dd4986084b10099a3a6663fc4`。已独立下载、核对摘要、重新解析原始JUnit及交付回执，未将空报告或NO-SOURCE视为通过。

## 实现与实际修改

主要提交00fb224（运行时GPS模式、记录边界、恢复日志与7项新增仪器测试）、289ce60（阶段3完整交付/发布门禁及审计后6780预留）、f794dce（实际通知角色交接失败修复）。完整文件清单可由`git diff --name-only aebc672 f794dce`审查。

生产文件包括SensorManager、GpsHandler、TrackRecordingService、TrackRecordingManager、TrackIdStorage、TrackRecordingServiceNotificationManager、P0RecordingViewModel及中英文地图/权限文案。新增P07GpsHandoffTest、P07RecoveryPermissionTest；CI新增p07_delivery、版本审计、签名准入、签名运行、不可变发布及独立公开下载脚本和两个clean专用工作流。

复用现有服务、GPS驱动及传感器数据路径；PREVIEW与P0_SESSION以相同内部1Hz配置交接，不重建相同Handler/driver。P0会话使用临时运行时配置，移除准备/恢复时对用户GPS来源、采样间隔、存点距离的持久写入；旧服务直接调用仍按用户配置。GPS与HR的记录段边界在正式确认后重置，暂停异地恢复不连接距离，结束后回到前台预览且不写入历史。既有私有恢复日志原子保存active ID和P0模式，不改Schema43；GPS权限失效时，即使蓝牙权限仍在也拒绝静默恢复，并保留运动。

通知先准备当前角色的单一startForeground包，再交由系统发布。第一轮两个新通知断言失败，见05-stage3-validation及evidence/run44979967128-failed；修复后重跑所有门禁。旧失败Run没有改为通过。

## 实际测试数量

| 检查 | Tests | Executed | Failures | Errors | Skipped |
|---|---:|---:|---:|---:|---:|
| Python Helpers | 146 | 146 | 0 | 0 | 0 |
| JVM | 167 | 167 | 0 | 0 | 0 |
| 完整API35 | 471 | 470 | 0 | 0 | 1 |
| GPS/原owner/偏好/心率专项 | 45 | 45 | 0 | 0 | 0 |
| 首页真实地图专项 | 2 | 2 | 0 | 0 | 0 |
| 三组实际独立权限进程（合计） | 3 | 3 | 0 | 0 | 0 |
| 三组持久化write/read新进程（合计） | 6 | 6 | 0 | 0 | 0 |

专项及额外进程检查不加进完整471项去重总数。唯一Skip仍为`TrackRecordingServiceStateMachineTest.recording_stopGPS_noop`，不计通过。P061GpsPreferencesTest、P06GpsOwnershipTest、P0SensorsTest源码与正式基线逐字相同；原断言全部保留，无新增Ignore。

Debug构建、Lint、JVM、API35完整回归均实际成功。19份实际导出的GPX通过官方GPX1.1 XSD；六组跨UID分享回执成功。数据库、指标及批量缓存write/read PID1997→2250，真实地图1991→2499，中断导入2011→2271；均实际force-stop后新进程，不是同进程重建替代。两个地图持久化设备实际开启飞行模式。API35原生窗口辅助工具在两次新进程校验成功，不使用正式签名密钥。

## 原生视觉与地图性能

本轮492张真实APK原生PNG均以62张有编号的对照图通过view_image实际查看，逐图文件名/SHA见evidence/run44980804037-full/visual-review.json。涵盖四套主题、日夜、小/普通/大屏、大字体、运动各状态、GPS速度/配速、心率连接/过期/重连、历史/分享/删除、设置、地图及真实首页。

旧布局与合成地图夹具只用于布局和原回归；真实地图验收另使用105,834,033-byte文件，SHA-256 `5412067262dace98d9f95b02621446516e7d98e1debbb6a314d575cae022ee1e`，53张真实地图及35张首页图。真实道路与地名、当前点、无地图、外接矩形外和矩形内无要素状态分别检查。地图跟随/拖动/缩放/恢复定位/替换/删除/重建及32MiB、64瓦片预算断言通过。

真实地图导入5,842ms；测试期间采样Java堆峰77,331,744 bytes、PSS峰299,563KiB、Java堆上限201,326,592 bytes。这些是本轮模拟器采样值，不是所有设备性能保证。未完整加载地图文件到内存。持久化测试结束后的meminfo为“No process found”，不能用该文件冒充运行中内存数据；上述数值来自运行中的真实地图测试回执。

视觉边界已记录：旧多轴图表刻度较密；小屏有意滚动到地图，顶部可滚动访问；拒绝权限图带启动淡入，仅验状态/布局，主题色以稳定矩阵为准；进程重启图有启动过渡取景，地图持久化可见，完整布局以稳定矩阵审查。未发现阻断本次交付的新视觉问题。

内部Debug APK为com.fox.runner.debug / v0.1.0-p0.7.0 / 6780，14,385,360 bytes，SHA-256 `d7f1317a39d815f8671d41db9bf072b6691b8d19f251a2bb69361c44ac96eef5`；不作为正式固定签名升级包。

## 遗留与下一条件

没有已知未解决的阶段3 P0/P1代码或自动回归失败。P0.7仍须通过受保护android-release固定密钥签名、实际优化APK覆盖P0.6.1升级/运行与99张签名原生图审查，再创建新不可变Tag和非Latest Pre-release，另一Runner公开下载复验。签名未通过不得发布，发布完成后按用户要求暂停。

Xiaomi CIVI5Pro、Redmi Note12Pro+、Magene H303、真实GNSS/电池/OEM后台策略仍需真实设备验证；模拟器注入位置及模拟HR不替代设备验收。服务生命周期恢复测试与用户显式恢复数据，不声称能绕过强制停止、权限撤销或系统限制。
