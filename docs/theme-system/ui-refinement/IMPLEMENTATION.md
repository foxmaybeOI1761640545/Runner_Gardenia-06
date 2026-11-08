# 原生 UI 深度还原：实现说明

本轮从已完成 P0.3 的 `cebf4682cec672fbde022c54bda3b442ff0663ab` 建立 `feature/theme-system-p0-3-ui-refinement`。旧 P0.3 及设计分支、所有既有 Tag/Release 保持冻结。新 APK 预留 `v0.1.0-p0.4 / 6775`；版本命名不代表重新开始另一轮产品任务。

## 一套业务状态，四种原生组织

`P0RecordingViewModel.Snapshot → DashboardState → DashboardRenderer → 四份 XML`。服务仍拥有计时、状态机、GPS、BLE、数据库和导出；UI 不另建业务状态机。`DashboardState` 校验选中 track 与统计一致、沿用已有 GPS 新鲜度/速度配速格式及心率有效性规则。`DashboardRenderer` 只绑定内容、状态提示、按钮与主题表现。

| 家族 | 独立信息架构 | 对照原型的具体实现 |
|---|---|---|
| Precision Pro | 左对齐距离主值 → 有分隔线 2×2 指标 → 操作 → 本地轨迹 → 紧凑心率细节 | 精密网格、紧凑数据层级、大数字小单位、明确边界 |
| Quiet Minimal | 居中距离主值 → 开放无卡框 2×2 指标 → 操作 → 轨迹与心率 | 左对齐简洁标题、中心排版、轻数字与自然间距；状态控件透明无框 |
| Outdoor Explorer | 主地图/真实轨迹 + 距离覆盖层 → 状态 → 三列指标 + 紧凑单行 GPS 精度 → 操作 → 心率 | 238dp 地图优先、本地矢量地形与真实轨迹分层、自然配色、较方正操作与状态标签 |
| Neon Pulse | 中心 BPM + 紧凑采样曲线与设备元信息 → 运动状态 → 2×2 次指标 → 操作 → 轨迹 | 青色心率视觉中心、紫色层次、弱光渐变、较圆操作；有效 GPS 状态避免重复说明，告警和准备说明完整保留 |

所有家族 Light/Dark 使用相同对应布局；八套既有 Token 字节未改动。`ThemeUtils`、原生 `values-night` 与已有 `MODE_NIGHT_FOLLOW_SYSTEM` 沿用。没有 HTML/WebView、在线地图依赖、生产夹具或新签名身份。

## 真实轨迹与真实心率

- `RoutePreviewModel` 是 ViewModel 中的只读异步增量查询：实际 trackpoints、真实微度坐标、手动/自动分段和时间断点，跨配置保留。切换/清空 track 先清除旧显示，避免展示另一段运动。GPS/BLE/录制层不改动。
- `RouteGeometry` 本地北向投影、经度换日线处理、合法边界与保留断点的缩减。它不计算/修改业务距离。
- `RouteMapView` Canvas 绘制真实已记录路径、起点和新鲜 GPS 点；地形明确标注仅为本地装饰、无地图瓦片，无 GPS 时显示设计后的空状态，读取失败显示错误。没有假地图瓦片或预制生产路线。
- `PulseChartView` 主页面 84dp、设备页面 100dp，实际采样曲线，断连区间不连线。无采样只显示网格和说明，不制造波形。仅新采样产生短暂反馈，遵守系统动画开关。Neon 有效 BPM 是大数字，缺失值为适当大小。
- 首页设备显示名称、状态和更新时间；原始 MAC 留在实际设备配置中。未连接与已连接但没有有效采样是不同状态。

## 导航与操作

`P0Navigation` 使用四个已有真实 Activity：运动、历史、心率、设置；原顶部历史/设置快捷图标移到底部，无假目的地。`REORDER_TO_FRONT` 复用原页面，Activity 恢复时沿用已有主题检查；录制与连接持续由原服务管理。选中导航具有文字、图标、选择语义和主题表现。

运动操作位于滚动内容中，导航位于独立 64dp 区域，两者不重叠；按钮至少 48dp 目标，开始/取消等待/暂停/继续/结束仍走原有命令。普通字号全部八种测试状态额外要求三种尺寸下当前可见主要操作首屏完整可见，长说明与大字体仍可自然滚动。

## 测试与证据边界

`src/debug/DashboardPreviewActivity` 仅承载相同生产布局、绑定器和 Canvas 组件，不连接服务。固定数据、路线、心率采样全在 `src/androidTest/DashboardFixture`。预览标明 UI fixture；Release manifest、production Java 不包含入口/固定数值。

矩阵使用真实 API35 原生窗口、实际 `wm size`/density 和系统夜间模式，3 尺寸 × 4 家族 × 2 模式 × 8 状态 = 192 PNG。对照语言只限定 Debug 预览资源上下文，生产语言跟随用户设置。独立真实导航测试使用现有服务、SQLite 路径和测试 SIG/GPS 输入，检查跨目的地、系统模式和家族切换后的 track、ViewModel、GPS 枚举与心率 connectionId。

保留全部既有单元与 Instrumentation 业务测试；额外本地投影 5 个 JVM 测试。完整门禁还要求 GPX 1.1 实际文件校验、两个真正不同进程的数据库恢复、Release offline manifest 审计、原 P0/P0.2/P0.3 截图/状态回归与独立原生窗口助手。

自动矩阵成功不等于视觉放行。`iterations/01`、`iterations/02`、`iterations/03` 保留已经拒绝的实际截图和原因。签名前需绑定完整成功测试来源与人工逐图检查；签名后还需检查正式 APK 32 张真实页面截图，再以独立出版操作复用原 APK，禁止重新签名或覆盖旧 Tag/资产。

API35 模拟器和合成输入不能证明实体 H303 射频、户外 GNSS、耗电或人工 TalkBack 语音体验；这些保留为真实设备待验证项。
