# 阶段3：预览与正式运动交接（开发与验证中）

PENDING。阶段2已由验收提交aebc67245bf3db90b928d781b88bbcd8fbc5d90d提交推送并核对远程后，才开始本阶段生产修改。阶段4及后续未启动；按用户最新范围，P0.7交付后暂停。

## 目标与实现

- SensorManager采用原采集路径的CONFIGURED/PREVIEW/P0_SESSION运行时模式，一次原子比较有效来源与间隔；内部预览1Hz与P0运动1Hz切换不重新连接相同驱动。旧服务直接startNewTrack/resumeTrack继续使用用户配置。
- P0RecordingViewModel不再写GPS来源、采样间隔或存点距离偏好；显式P0恢复通过resumeP0Track。TrackRecordingManager仅在服务内P0会话启用零距离存点间隔，保留配置值。
- TrackIdStorage在同一个既有私有恢复日志编辑中原子提交active ID与P0模式，结束/暂停清理。新服务恢复同一运动时先恢复运行时模式；若GPS权限无效，即使蓝牙权限仍在也拒绝静默无GPS恢复，保留既有运动供用户后续恢复。
- 交接时重置GPS/HR记录段聚合边界，预览点不写入正式轨迹；结束转回预览时重置旧预览跳点锚，保留相同驱动。通知沿用现有安静常驻通道，恢复预览后清除旧运动总量，正式恢复时清除预览说明。
- 无地图及权限提示改用中性措辞，不在权限拒绝状态声称定位可用。

## 新增验证（尚未执行，不作为通过数量）

P07GpsHandoffTest共6项：真实P0命令与相同Handler/driver、开始时间及GPS/HR隔离、结束恢复预览且历史不再增长、暂停异地恢复无距离桥、旋转/切后台/锁屏、新服务实例从日志恢复同一运动。P07RecoveryPermissionTest共1项，普通回归执行实际当前权限分支，独立AVD另以实际GPS拒绝、Bluetooth Connect授予的新进程验证失败关闭。原owner/偏好/心率三份测试逐字保护；GPS定向预期45项，完整API35预期至少471项，唯一历史Skip保留且不计通过。

原P0.6.1台账测试的全部断言保持原样，测试夹具固定到其正式验收快照；新增5项发布准入测试检查当前6780分配、错误分支/PR、过期或错误Artifact、缺失/Skip工作流，以及审批提交不能修改产品/测试/版本/工作流。所有实际执行数量待Actions原始报告核实。

## P0.7版本与发布门禁

重新fetch全部heads/tags后审计全部11个Release/build-info、Tag属性和远程分支台账，最高有效值6779；实验分支属性6780未发布且未分配。当前在clean分支独立预留v0.1.0-p0.7.0 / 6780，allocationBasisCommit为已存在的阶段2验收提交，commit仍为null，不编造未来源码。审计见evidence/p07-version-allocation-audit.json。

新clean工作流执行Debug/Lint/JVM/Python/45项定向/真实首页地图/独立权限/完整API35，再收集原生矩阵与实际GPX1.1官方XSD校验，运行数据库、指标、批量分享、105MB真实地图及中断导入的新进程与飞行模式验证，以及NativeWindowDump真实API35自测。缺失/空报告/额外Skip均失败。

新p07-release.yml仅响应clean分支的独立审查记录。签名Job依赖approved-tests重新解析原始JUnit及摘要、核对实际逐图审查；仅android-release原有Secrets恢复固定密钥到RUNNER_TEMP，全结果清理。签名候选先保存不可变Artifact，重试复用原字节。P0.6.1固定证书覆盖升级、实际GNSS输入、进程恢复、8种原生主题方案、真实地图验收后仍需实际查看99张签名APK截图，才允许发布新不可变Tag和非Latest Pre-release；发布后另一Runner匿名下载校验身份、证书和SHA256。

旧P0.6.1烟测仅增加默认行为不变的参数，所有原断言保留。新P0.7适配器使用实际设置→隐私和离线→离线地图及正确返回栈；不恢复妨碍拖动的整块地图点击，不向生产增加测试入口。

## 当前结果与边界

尚未运行阶段3Android测试、未访问签名密钥、未生成正式APK、未创建Tag/Release。当前只完成静态语法及文件差异检查，不能作为阶段验收。

所有Android构建、测试、签名与发布均由Actions完成；本地不安装SDK。新服务生命周期测试不等于绕过强制停止或系统权限限制；实体GNSS、电池、Xiaomi CIVI5Pro、Redmi Note12Pro+和Magene H303仍需真机验证。

## 第一轮真实失败与修复

源码289ce602f5a5c3af59155053dc43abd4e2a1ddb3 / Run44979967128：Python146/146/0/0/0、JVM167/167/0/0/0，GPS定向45/45/2/0/0；完整API35、地图及额外交付烟测未执行。两项新增通知断言观察到旧角色内容（恢复仍显示预览、结束仍显示旧距离），其余43项通过，原owner/偏好/心率断言无失败。原始XML、gate/helpers和摘要见evidence/run44979967128-failed/。

修复在准备startForeground的单一权威通知包之前设置正确角色内容，同角色正式运动保留已有实时总量，停止后重置角色。通知服务发布本身异步，新增测试有界等待实际NotificationManagerService出现目标文本，原精确文本与常驻断言不变；不新增Ignore、不改原owner/偏好/心率源码。当前仍PENDING，修复源码必须重新执行全部门禁。
