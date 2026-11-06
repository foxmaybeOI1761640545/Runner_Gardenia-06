# BLE 自动重连只读审查

阶段 2：源码与现有状态机检查 PASSED；H303 真实故障根因 **尚未证实**。只做一轮完整审查和一次现有自动化测试，没有修改 BLE 生产代码、重试机制或测试断言。

审查源码基线：`60daeb21aa50bdbceec6e6ad354a431b8c1be434`；阶段 1 [云端检查](https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/actions/runs/42180056686)已通过。所有下述源码路径均相对于仓库根目录。

## 自动连接链

1. `p0/recording/P0RecordingViewModel.attach()` 绑定现有 `TrackRecordingService` 并订阅状态；**仅绑定不等于启动心率预览**。
2. 正式记录的 `TrackRecordingService.startSensors()`，或显式预览的 `tryStartSensors()` → `SensorManager.start()` / `onSharedPreferenceChanged()` → `HeartRateHandler.connect()`。选择地址来自 `settings_sensor_bluetooth_heart_rate_key`；`p0_hr_connection_enabled_key` 决定是否连接，自动重连开关独立。
3. `SensorHandler.connect()` 读取已选地址，创建通用 `BluetoothDriver<HeartRate>` 和 `HeartRateBluetooth`，传入现有服务 Handler；不存在 H303 名称硬编码。
4. `BluetoothDriver.connect()` → `BleConnectionSession.start()/attempt()` → `connectGatt(autoConnect=false, TRANSPORT_LE, PHY_LE_1M_MASK, Handler)`。
5. `onConnectionStateChange()` → `connected()` → `discoverServices()`；`onServicesDiscovered()` 查标准 `180D/2A37`（兼容广告别名不改变测量 UUID）。
6. `servicesDiscovered()` → SUBSCRIBING → `setCharacteristicNotification(true)` + CCCD `2902` 写入；API33+ 使用带值的新 API，旧版本使用 descriptor.setValue/writeDescriptor。
7. **仅**目标 CCCD 的 `onDescriptorWrite(GATT_SUCCESS)` → `subscribed()` → CONNECTED → `listener.ready()` → `HeartRateHandler.onConnected()` 新建连接 UUID、清除上次更新时间并重建聚合器。可选电量/佩戴位置读取排在订阅确认之后。
8. 通知回调（新旧 API 均有）→ `BluetoothDriver.receive()` 校验当前 GATT/特征对象/CONNECTED → 复制原始字节 → `HeartRateBluetooth.parsePayload()` → 严格 SIG 解析 → `HeartRateHandler.onDataReceived()` 单调时钟接收时间、接触状态、连接 ID → `AggregatorHeartRate` → 服务预览快照 → ViewModel 每秒 emit → `HeartRatePanel.describe()`。
9. 真实 transport 断开/握手失败 → `failed()` 关闭 GATT、失效旧值，按 1/3/5/10/30/60 秒重连；蓝牙关闭等待开启；权限拒绝/无效地址阻断；用户 stop 取消 desired 与全部定时任务。

涉及完整源码包前缀：`src/main/java/de/dennisguse/opentracks/`。主要文件：`sensors/driver/BluetoothDriver.java`、`BleConnectionSession.java`、`HeartRateBluetooth.java`、`HeartRateMeasurement.java`，`sensors/HeartRateHandler.java`、`SensorHandler.java`、`sensorData/AggregatorHeartRate.java`、`SensorManager.java`，`services/TrackRecordingService.java`，`p0/ble/HeartRateCommands.java`，`p0/recording/P0RecordingViewModel.java`，`p0/app/P0SensorsFragment.java`，`ui/HeartRatePanel.java`。

## 手动“重新连接所选心率带”链及差异

`P0SensorsFragment.p0_connect`（或连接权限成功回调）→ `model.previewHeartRate(true)` → 前台执行现有服务命令 → 保存 connectionEnabled=true → `tryStartSensors()`。如果传感器已启动且之前 enabled=true，再发送仅限应用/所选地址/2A37 的 reconnect 广播 → `BluetoothDriver.reconnectReceiver` → `BleConnectionSession.reconnect()`。

| 环节 | 自动路径 | 手动路径 |
|---|---|---|
| 服务未启动 | 只有 attach 不主动预览；记录/预览入口启动 | 显式启动预览服务 |
| 服务已启动、当前链路 CONNECTED | start/connect 对同地址既有会话可直接返回；autoReconnect 开关不重启健康链路 | 主动 cancelTasks → closeGatt → lost → attempt，强制新 GATT/代次 |
| 断连后重建 | failed/retry/attempt | reconnect/attempt |
| 重建后的发现/CCCD订阅 | 相同 transport/discover/subscribe/callback | 完全相同 |
| 宣告 CONNECTED | CCCD 成功回调 | CCCD 成功回调 |
| 等待首条有效 BPM | 没有独立首通知看门狗 | 重连后同样没有，但强制重订阅可能恢复设备通知 |

所以两条路径共享连接与订阅实现，但**入口和强制重置行为并不完全相同**。开启自动重连不是“保证有 BPM”：它主要处理 transport 失败。

## 排查结论与置信度

- **高置信度排除**“正常路径在 CCCD 完成前宣告 CONNECTED”：状态机明确要求目标 descriptor 的成功回调；现有测试覆盖此顺序。Android/设备误报成功不在纯源码可排除范围。
- **高置信度确认机制缺口**：30 秒握手超时在 subscribed 成功时取消；CONNECTED 后既无首条有效测量超时，也无通知停止触发 transport 重建。`HeartRatePanel` 在超过 5 秒、无值、无接触时显示无效/过期，但 UI stale 不触发重连。此时“连接成功但无 BPM，手动强制重订阅后恢复”与源码行为吻合。
- **中等置信度候选原因**：设备已确认 CCCD 但没有开始发送，或发送的包因接触丢失/非法字段被过滤；自动重连不处理这种仍 CONNECTED 的无有效通知情形。没有 H303 原始通知、descriptor 状态及带时间的状态日志，不能在这些原因中确定唯一根因。
- **中等置信度另一入口差异**：仅返回首页/重建 Activity 的 attach 不保证自动启动预览；必须区分“记住设备/自动重连”与“用户已明确启动的连接会话”。不要默认连接周围设备。
- **未发现明显旧会话混入**：回调校验 GATT 对象身份；generation 拒绝旧超时/重试；closeGatt 先置空对象/measurement/service，再 disconnect/close；手动重连重新建立 UUID并清除有效值。不能据此宣称已通过真实 RF 生命周期验收。
- 原始采样保留每条独立通知时间与身份；相同 BPM 不等于重复通知。合法接触丢失会使 BPM 无效，不能靠保留旧 BPM 掩盖问题。

## 本次实际检查

一次执行原有 `BleConnectionSessionTest`：**23 项，Failures 0，Errors 0，Skipped 0**；[完整原始 JUnit 输出](ble-audit-junit.txt)。使用现有 JDK 编译模块、JUnit 4.13.2，source/target17，`Asia/Singapore`；未安装 Android SDK。覆盖 CCCD 确认、失败/超时、旧代次、重连退避、权限/蓝牙、主动断开和策略变化。它不模拟真实 Android GATT、H303 或射频。生产代码和已有测试未改，故不宣称本阶段执行了新的 Android 硬件测试。

## 后续建议（本轮不实施）

后续独立修复任务先采集可脱敏诊断：每代连接时间、discover 状态、CCCD请求/回调、首通知到达时间、原始 flags/长度及解析拒绝原因（不要记录个人运动数据或设备地址到公开日志）。明确区分“已订阅/等待有效心率/收到有效心率”。在实机证据支持后，再设计有上限、用户可关、区分无接触与传输失活的恢复策略并加测试；不得本轮凭猜测添加重试。

## H303 必须确认：NEEDS_DEVICE_VALIDATION

实际首通知是否在 CCCD 确认前到达；成功回调后是否持续发送；接触 flags 与解析有效性；手动 reconnect 是否产生新 GATT/新订阅/新通知；蓝牙开关与远离/返回；自动重连开关关闭与主动断开；后台/锁屏。先抓可重复的真实设备时序，再确认根因。本轮 Release Notes 必须说明“BLE 自动连接只完成源码审查，未修复”。
