# Android 技术栈只读审查

审查日期：2026-11-07（Asia/Shanghai）。审查对象仅为验收参考 Tag，不代表 P0.2 分支当前状态。

## 基线证据与隔离

已执行 `git fetch origin --tags` 和 `git ls-remote`，远端 Tag 为 annotated tag：

- Tag：`test-p0-v0.1.0-p0.1`
- Tag 对象：`c3af195d4d99eb468dc09420bee43336b295bbd2`
- peeled commit：`3116e5fa7a56ce6f299ea45d5a45745f1aed4466`，与指定 SHA 一致。
- Tag 注释标明 `Hardware acceptance: NEEDS_DEVICE_VALIDATION`。本次沿用用户指定的源码参考，不将其解释为硬件问题已经消失。
- 远端无 `design/p0-ui-lab`，从上述提交建立独立 worktree 和长期设计分支；没有切换或修改原工作区的业务分支。
- 仓库 `has_pages=false`，Pages GET 返回 404；没有发现既有站点可被覆盖。

## 已核实的 UI 技术栈

| 项目 | 源码事实 / 证据 |
|---|---|
| 语言 | `src/main/java/` 下 265 个 Java 文件，无 Kotlin 文件；`build.gradle` 设置 Java 17 |
| UI | Android Views；Activity / Fragment；无 Compose 插件、依赖或调用 |
| XML | `src/main/res/layout/p0_home.xml`、`p0_history.xml`、`p0_sensors.xml` 等真实布局 |
| ViewBinding | `build.gradle` 的 `buildFeatures.viewBinding=true`；`P0Activity` 使用 `P0HomeBinding` |
| DataBinding | 未启用；未发现 `<layout>` DataBinding 根或 DataBinding 调用 |
| 组件 | AppCompat 1.8.0、Material 1.14.0、Preference 1.2.1、ConstraintLayout 2.2.2、AndroidX GridLayout 等，见 `build.gradle` |
| SDK | minSdk 26 / compileSdk 37 / targetSdk 37；本任务不变更这些配置 |
| 状态 | `AndroidViewModel` + `LiveData`；不可变 record 快照；后台 Executor 执行数据库工作 |
| 导航 | P0 通过显式 Intent 启动 Activity；历史 Activity 内列表/详情显隐切换和 OnBackPressedCallback。传统记录页使用 ViewPager2 / FragmentStateAdapter。未发现 P0 已有四栏 BottomNavigationView 或 Jetpack Navigation graph |
| 图表 | 自有 `chart/ChartView.java:60`，继承 View，用 Canvas / Paint / Path 绘制；无 MPAndroidChart、GraphView 等外部图表依赖 |
| WebView | 搜索实际 `src/` 与 Gradle：未发现 WebView / android.webkit 的声明或使用。帮助、关于页面也是 XML 原生页面；不能把外部地图 Intent 或帮助链接当作 WebView |

## 主要页面与职责路径

以下路径相对于仓库根目录；Java 公共前缀为 `src/main/java/de/dennisguse/opentracks/`。

| 页面 / 模块 | 实际源码 | 现有行为与边界 |
|---|---|---|
| P0 运动首页 | `p0/app/P0Activity.java`；`src/main/res/layout/p0_home.xml` | 绑定按钮、权限入口、运动指标、GPS、实时心率、最近历史；不直接操作数据库或 GATT |
| P0 页面状态 | `p0/recording/P0RecordingViewModel.java` | Snapshot 汇总 session/data/gps/position/preview/error/busy；attach/detach 不等于停止后台记录 |
| 运动生命周期 | `p0/recording/RecordingSession.java`；`p0/data/SessionStore.java` | READY / RECORDING / PAUSED / FINISHED；保留现有恢复逻辑 |
| 设备页 | `p0/app/P0SensorsActivity.java`、`P0SensorsFragment.java`；`src/main/res/xml/p0_sensors.xml` | PreferenceFragmentCompat、连接/主动断开/自动重连、错误、实时预览 |
| BLE 扫描 | `settings/bluetooth/BluetoothLeSensorPreference.java`；`sensors/driver/BleScanSession.java` | 真实原生扫描 Dialog；设备名、地址、RSSI（`getRssi()`）、扫描代际保护 |
| BLE 命令 | `p0/ble/HeartRateCommands.java`；`P0HeartRatePreference.java` | 通过应用内命令请求现有 transport；本身不持有 GATT |
| BLE transport | `sensors/driver/BluetoothDriver.java`、`BleConnectionSession.java`、`HeartRateBluetooth.java`、`HeartRateMeasurement.java` | 连接/发现/订阅/重连/解析通知；UI 不重写此层 |
| 心率显示 | `ui/HeartRatePanel.java`；`sensors/HeartRateStatus.java`、`HeartRatePreview.java` | fresh 需有效数据、CONNECTED、未失去接触且 age ≤ 5000 ms；过期显示占位符 |
| GPS 获取 | `sensors/driver/GpsInternal.java`；`sensors/GpsStatusValue.java` | LocationManager GPS_PROVIDER，经 AndroidX location 更新；GPS 状态与运动暂停是不同维度 |
| 记录服务 | `services/TrackRecordingService.java`；`services/handlers/TrackPointCreator.java` | 服务生命周期、传感器、实际轨迹、记录状态与预览；UI 不另建记录引擎 |
| 历史/详情 | `p0/app/P0HistoryActivity.java`；`P0HistoryViewModel.java`；`src/main/res/layout/p0_history.xml` | ListView + ArrayAdapter；详情当前主要为文本指标和 GPX 操作，并无原型那样的内嵌轨迹卡 |
| 数据查询 | `p0/data/WorkoutRepository.java`、`WorkoutSummary.java` | ContentResolver / CustomContentProvider，worker 线程；raw heart-rate 优先、legacy fallback；保护活动记录 |
| 存储 | `data/CustomContentProvider.java`、`data/HeartRateRepository.java`、`data/tables/HeartRateSamplesColumns.java` | 现有 SQLite / ContentProvider 链路，禁止另建浏览器式数据层替代 |
| 原生心率图表 | `chart/ChartFragment.java`、`ChartView.java`、`ChartValueSeries.java`、`HeartRateChartHistory.java`、`HeartRatePlot.java` | ChartFragment 接 TrackDataHub；原始心率、暂停活动时间轴、超过 5 秒/连接变化断线、极值保留降采样 |
| GPX | `p0/export/P0GpxExporter.java`、`P0GpxFiles.java`；`P0HistoryViewModel.exportSelected()` | CreateDocument 保存；FileProvider + ACTION_SEND / chooser 分享；禁止导出活动/暂停记录 |
| 设置 | `settings/SettingsActivity.java`、`DefaultsSettingsFragment.java`、`UserInterfaceSettingsFragment.java`、`PreferencesUtils.java`（单位/配速键见 `src/main/res/xml/settings_defaults.xml`）；P0 的 `P0SensorsFragment` | 现有单位/速度配速偏好与传感器偏好；原型统一入口是 UI 整理提案 |
| 既有地图方式 | `util/IntentUtils.java`、`introduction/OSMDashboardFragment.java`、`src/main/res/layout/osm_dashboard.xml` | 外部地图 / OSMDashboard 集成；没有发现内嵌地图 SDK。原型本地 SVG 不是现有原生地图能力 |

## 能力差距与可扩展性

1. 原生组件适合扩展：保留 Java + XML + ViewBinding、Activity / Fragment、MaterialButton、TextView、Preference、ListView、既有图表。新四栏导航可使用已依赖的 Material `BottomNavigationView`，但应作为未来 UI 改动单独评审，本次不实现。
2. P0 当前历史详情未直接嵌入 ChartFragment；可以在未来通过原生 UI 适配连接现有 TrackDataHub / 图表，不能假设加一个 XML 标签即可完成。
3. 原型要求“有效定位后才开始”。当前 `P0Activity.render()` 的 start enable 条件主要依据 session/busy，未直接要求 fix。原型可表达目标交互，但未来应由独立 UI 任务审查权限、服务启动搜星与按钮门控的协调，不能在本次暗改业务逻辑。
4. 原型完整四栏、SVG 轨迹示意、导出底部弹层都是设计提案；不代表当前 APK 已有相同呈现。
5. 自动重连仍可能失败；必须保留 `previewHeartRate(true)` / `HeartRateCommands.reconnect()` 的明确手动入口，不可将连接成功等同于收到了新 BPM。

## HTML/CSS 到原生的视觉迁移

| 视觉效果 | 可行性与实现方向 |
|---|---|
| 颜色、字号、间距、粗细、边框、圆角 | 直接映射 Android color/dimen/style、shape drawable、MaterialShapeDrawable |
| Grid / Flex 卡片与列表 | XML LinearLayout / ConstraintLayout / GridLayout；当前 ListView 可先保留，不为设计强制引入新框架 |
| 状态标签、导航、弹层、设置 | 原生 TextView / MaterialButton / BottomNavigationView / MaterialAlertDialogBuilder / Preference |
| SVG 图标 | 简单 Path 转 VectorDrawable，24dp；状态另带文字说明 |
| 心率线与断线 | 优先扩展既有 ChartView；保留 HeartRatePlot 的时间轴、极值与断点，不用装饰性平滑补点 |
| 环形仪表、刻度、等高线装饰 | 少量 Canvas / Path 自定义绘制；非信息装饰降低层次，避免增加实时重绘负担 |
| 轨迹卡 | 未来小型原生 Canvas 路线概览读取真实点/分段；没有底图时明确“轨迹概览”，不伪装精确地图，不加入在线瓦片 |
| Aurora 玻璃 | 用固定渐变、实色半透明 surface、描边模拟层次；不要依赖 API 31+ 模糊，否则 minSdk 26 兼容性差 |
| Neon 发光、阴影 | 克制描边/有限光晕；不建议大面积实时模糊和多层发光，保证户外可读性与功耗 |
| 手机外壳、轮播、自动播放、设计调试面板 | 仅实验室；全部不迁移到正式 Android UI |

结论：十套设计均可用现有原生技术栈实现主要视觉和交互。自定义仪表/轨迹概览有额外 UI 绘制成本；玻璃与发光应简化。不引入 WebView，不重写 GPS、BLE、数据库或运动记录逻辑。本阶段仅新增本审查报告，未修改 Android 源码、配置或现有工作流。
