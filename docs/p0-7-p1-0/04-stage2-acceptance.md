# 阶段2：应用前台全局定位（等待完整回归）

**PENDING：不能据此开始阶段3或发布P0.7。**

阶段1已经独立通过，验收提交c69ac06。阶段2当前修复候选为 `3e389c183a5d828906a4281dc50408f6510adb77`，[Run44943379419](https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/actions/runs/44943379419)，定向38项。a9ee020/Run44942552700的定向38项中1项因系统已配对蓝牙集合为空而崩溃，完整回归未执行；已修复扫描空集合处理并保留原测试。上一候选 `1047df6` / [Run44937943787](https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/actions/runs/44937943787) 的完整API35已返回失败：464项中实际执行463项，1失败、0错误、1历史Skip。仅选设备意外触发心率连接，生产连接门禁修复正在重新验证，不能进入阶段3。

## 实际实现与文件

- `Startup.java` 注册 `p0/gps/ForegroundGpsController.java`，应用级管理已启动Activity集合及唯一前台需求，跨内部页面保留同一服务/驱动，普通后台立即释放，配置重建只有500ms有界过渡。晚到的服务绑定再次检查权限与前台状态；崩溃报告独立进程不采集位置。
- `services/TrackRecordingService.java` 将应用前台请求、明确地图请求、GPS准备和正式记录统一裁决；全局请求不是第二个mapGpsOwners客户端。空闲INTERNAL/1Hz沿用现有临时读取方式，不修改持久GPS来源和间隔。显式心率预览意图独立保存；无实际GPS需求后丢弃排队位置回调。
- `settings/bluetooth/BluetoothLeSensorPreference.java` 对系统返回空的已配对设备集合做空值处理，保留记住设备及扫描流程，避免对话框在蓝牙状态竞态中崩溃。
- `sensors/SensorManager.java`、`sensors/HeartRateHandler.java` 增加独立运行时心率请求门禁，仅显式连接、运动准备或正式记录请求连接；保留持久连接偏好及原GATT/解析/采样代码。
- `sensors/GpsHandler.java` 复用既有GpsMotionFilter的独立预览状态，处理跳点/重定位，不污染正式记录滤波状态。已断开或不再属于当前传感器生命周期的Handler不接收旧回调。有效性、精度、单调时间及新鲜度沿用既有数据模型与服务路径。
- `P07ForegroundGpsTest.java`、`P07PermissionStateTest.java` 添加真实Application/Activity/Service生命周期、跨页重建、同驱动、迟到回调、弱/过期/跳点、默认与显式心率、实际权限状态、零空闲历史测试。原P061GpsPreferencesTest与P06GpsOwnershipTest逐字保留。原取消UI断言保留，先验证前台GPS与零历史，再移至后台验证原停传感器断言。
- `P07HomeMapTest.java` 保留实际105MB地图、边界、缩放、手势、重建、替换、删除和缓存断言；截图必须同时等待服务确认的新鲜Fix、实际原生Ready文本和真实可见瓦片。
- `.github/p07/phase.json`、`p07_stage.py`、`p07_permission_smoke.py` 及clean验证workflow，要求前阶段已验收、受保护测试源码原样、当前源码的非空定向/全量报告、唯一历史Skip、不同真实进程的denied/granted权限烟测。

## 上一候选1047df6的检查点（完整回归失败，不能用于修复候选验收）

| 独立检查点 | Tests | Executed | Failures | Errors | Skipped |
|---|---:|---:|---:|---:|---:|
| GPS定向（含原owner/偏好/准备/原生运动UI） | 29 | 29 | 0 | 0 | 0 |
| 首页真实地图定向 | 2 | 2 | 0 | 0 | 0 |
| 系统权限拒绝，独立进程 | 1 | 1 | 0 | 0 | 0 |
| 系统权限授予，另一进程 | 1 | 1 | 0 | 0 | 0 |

这些是重复执行的不同检查点，不累加冒充全量数量。Debug/Lint/JVM编译及Python步骤已成功，但完整最终数量仍待原始报告核实。

源码绑定的35张首页截图（四主题×昼夜×360/390dp×1.0/1.4字体，加3种空白状态）及2张实际权限截图已经逐张查看。真实道路/地名、当前位置实心标记、精度、分离的定位与缩放控件、可滚动访问的主操作正常；拒绝权限时明确提示，无虚假Fix或运动统计。图像SHA256、原始定向XML、权限文本及逐图结论位于 `evidence/run44937943787-focused/`。完整回归仍未验收。

## 失败、限制及下一门禁

所有失败及修复见 `03-stage2-validation.md`，包括嵌套独立测试任务销毁首页，以及截图未等待连续定位确认。原断言未删除、未改owner预期、未新增Ignore。被新源码取代的取消运行不算成功。

此阶段不改变SQLite Schema43、GPX导出、运动历史写入或BLE驱动/解析/采样实现；修复仅增加独立的运行时心率连接请求门禁。不分配版本、不访问签名Secrets、不创建Tag/Release。正式运动的临时采样配置、交接和恢复是下一阶段，尚未开始。

阶段3启动条件：当前源码完整Python/JVM/API35报告成功、唯一历史Skip核实、实际原生视觉审查、阶段报告提交推送及远程核验全部完成。实体GNSS、功耗、Xiaomi CIVI5Pro、Redmi Note12Pro+及Magene H303仍为NEEDS_DEVICE_VALIDATION。

## 当前候选3e389c1的先行检查点

Run44943379419已实际完成GPS定向38/38/0/0/0、真实地图2/2/0/0/0、独立权限denied与granted各1/1/0/0/0；两次权限进程PID1926/2400不同。三个检查点Artifact均独立下载核对SHA256，原始XML/权限stdout及37张实际查看的截图摘要见 `evidence/run44943379419-focused/`。已查看四主题昼夜360/390dp与1.0/1.4字体32图、3种空白地图状态及2张实际权限截图。完整API35尚在运行，以上不构成阶段通过，不得开始阶段3。
