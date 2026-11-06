# Runner Gardenia P0 原生 UI 实现指南

适用基线：`test-p0-v0.1.0-p0.1` → `3116e5fa7a56ce6f299ea45d5a45745f1aed4466`。必须结合 [实际技术栈审查](ANDROID_TECH_STACK_AUDIT.md) 阅读。本文是后续 Android UI 开发规范；本设计分支不修改 Android。

## 实现原则

继续使用 **Java 17 + XML Views + ViewBinding + AppCompat / Material / Preference + AndroidViewModel / LiveData**。不引入 Compose 迁移，不使用 WebView 承载正式 UI，不把 HTML/JS 状态机搬进记录服务。HTML 只是展示与交互合同。

GPS、BLE、数据库、前台服务、运动记录和 GPX 继续由现有类负责。新增 UI 适配器可以投影状态，不得维护第二份真实运动数据。原型的模拟器、轮播、手机外壳、收藏方案、模拟接触开关均不属于正式应用功能。

## 与实际源码对应的组件映射

Java 路径前缀：`src/main/java/de/dennisguse/opentracks/`。

| HTML 原型组件 | 现有原生入口与未来实现方向 | 迁移边界 |
|---|---|---|
| `.app-scroller` / 页面标题 | `AbstractActivity`、`p0/app/P0Activity` + `P0HomeBinding` / `p0_home.xml`；现有 `P0HistoryActivity`、`P0SensorsActivity` | 保留 Activity/ViewBinding；可将内容提取 Fragment，但不要求为外观重建导航架构 |
| `.app-nav` 四栏 | Material 库已存在，可新建 `BottomNavigationView` 与 XML menu，路由到原生页面容器 | 基线没有 P0 四栏导航，属于未来 UI 工作。选中和返回状态用 SavedState/Bundle 保存；切换页不停止服务 |
| `.hero-number` / `.metric-grid` / `.card` | TextView、LinearLayout、ConstraintLayout / GridLayout、MaterialCardView 或 shape drawable | 数字 sp、间距 dp；使用 format 层，不把格式化字符串反解析为业务数据 |
| `.status-pill` / GPS 页 | `P0RecordingViewModel.Snapshot.gps/position/session`、`GpsStatusValue`、`MotionFormat` → 原生 TextView / 状态图标 | 搜星、坏信号、丢失、暂停分别显示；位置有效性与精度来自 Position，不由颜色或按钮推断 |
| 心率当前值与 freshness | `HeartRatePanel.describe()`、`HeartRateStatus`、`HeartRatePreview` | 沿用 ≤5000ms、CONNECTED、有效值、接触良好的规则；连接成功尚未收到新数据时仍是 `--` |
| BLE 设备列表 | `BluetoothLeSensorPreference` 现有扫描 Dialog + ListAdapter / Preference | 保留 BleScanSession 权限、蓝牙关闭、失败、代际保护；若新做列表，仅适配名称/地址/RSSI，不另开 GATT |
| 连接 / 手动重连 / 主动断开 | `P0SensorsFragment` → `P0RecordingViewModel.previewHeartRate(true)` / `disconnectHeartRate()`；`HeartRateCommands` | 手动重连必须一级可见，不能埋在溢出菜单；自动重连不承诺必然恢复数据 |
| `.history-row` | `P0HistoryActivity` 现有 ListView + ArrayAdapter、`P0HistoryViewModel` | 在现有 adapter 增加原生 row XML 即可；分页/加载/错误/保护逻辑不变，不强制引入 RecyclerView |
| 运动详情指标 | `P0HistoryViewModel.State.selected` / `WorkoutSummary` | 采用 repository 真实统计；无心率样本显示 `--`，不展示 0 BPM 作为有效测量 |
| SVG 心率曲线 | `chart/ChartFragment`、`ChartView`、`HeartRatePlot`、`HeartRateChartHistory` | 扩展现有 Canvas 绘图样式；P0 详情目前未嵌图，未来需 UI 适配接入 TrackDataHub；复用原始样本与断点处理 |
| SVG GPS 轨迹概览 | `WorkoutRepository.forEachPoint()` 在 worker 读真实点，未来小型 Canvas View 按包围盒归一化分段绘制 | 基线无内嵌地图 SDK；不引入在线地图，不把 SVG 示例当真地图。保留已有外部地图 Intent；新增概览需单独原生 UI 实现 |
| `.export-sheet` | MaterialAlertDialogBuilder / 原生 DialogFragment；`P0HistoryActivity` 的 CreateDocument；`P0HistoryViewModel.exportSelected()` / `P0GpxFiles` | 文件保存 SAF，分享 FileProvider / ACTION_SEND；复用准备/失败/完成事件；取消不宣称成功；活动/暂停记录受保护 |
| 设置 | `SettingsActivity` / `UserInterfaceSettingsFragment` / `PreferencesUtils`、`P0SensorsFragment` | 单位、配速/速度、自动重连绑定已有 preference key；整理入口不重写存储逻辑 |
| 环形仪表/等高线/硬边装饰 | 少量自定义 View 或 XML drawable / VectorDrawable | 仅显示层、无采集职责；无必要时不做每帧动画 |

## 可直接采用的设计 Token

颜色取自 `styles.css` 的实际变量；`bg/surface/fg/muted/accent/onAccent` 分别映射 Android theme 的背景、表面、正文、次级正文、强调和强调色上的文字。状态颜色除色彩外必须有文字与图标。不要直接将 `accent` 当作所有主题的浅底正文色（例如粗野主义的荧光黄）。

| 方案 | bg | surface | fg | muted | accent | onAccent | radius |
|---|---|---|---|---|---|---|---|
| 01 Precision | #121614 | #1C221E | #F2F5EC | #A0AD9F | #CCF96B | #18240B | 12dp |
| 02 Quiet | #FAFBF9 | #FFFFFF | #24352E | #728178 | #3C6A54 | #FFFFFF | 16dp |
| 03 Explorer | #F0EEE2 | #E5E7D7 | #233E30 | #72816B | #345C3D | #FFF9E6 | 9dp |
| 04 Neon | #0C0E1B | #17162A | #F1ECFF | #AAA5C9 | #70F4ED | #09202B | 18dp |
| 05 Swiss | #F6F2E9 | #F6F2E9 | #171816 | #77766B | #D13627 | #FFFFFF | 0dp |
| 06 Industrial | #262824 | #30332C | #F5E9CA | #B1B19E | #EFBD5D | #292517 | 3dp |
| 07 Wellness | #F7F5EC | #FFFDF7 | #354C42 | #7E9084 | #4C8069 | #FFFFFF | 25dp |
| 08 Aurora | #151B39 | #2B3455 | #F2F5FF | #B7C4E1 | #BCCAFF | #1C274D | 22dp |
| 09 Racing | #F3F3EF | #FFFFFF | #161817 | #7C807C | #CC302C | #FFFFFF | 4dp |
| 10 Brutal | #FAFAF2 | #FAFAF2 | #11120C | #666959 | #E9F34B | #10120A | 0dp |

Aurora 的强调色规范值为 `#BCCAFF`。渐变背景上使用不低于原型 surface 不透明度的内容层；不用系统模糊作为最低版本依赖。

| 参数 | 原型值 | 原生规则 |
|---|---|---|
| 基准内容宽高 | 393 × 873 CSS px | 是设计坐标，不是固定设备像素。以 dp 自适应可用宽度，sp 随系统字体缩放 |
| 字体 | 系统 sans；Industrial/Brutal 为系统 monospace | Android 系统 sans-serif / monospace；无在线字体；中文允许系统 fallback |
| 品牌辅助标签 | 9px / 600 / 字距 2px | 原生建议 11–12sp，弱辅助信息也要可读，不照搬过小字体 |
| 页面标题 | 20–28px / 400–900 | 22–26sp；按主题调粗细，中文不做强制单行截断 |
| 核心距离 | 55–98px / 300–900 | 64–88sp 起，空间不足优先缩字与换行；数字采用 tabular/monospace feature |
| 次级指标 | 23–31px | 24–30sp，单位 12sp，正文 14–16sp |
| 常规说明 | 10–12px（手机模拟器设计坐标） | 原生 12–14sp，错误/操作提示优先 14sp；TalkBack 可读 |
| 空间体系 | 4 / 8 / 12 / 16 / 20 / 24 / 32 | 同数值 dp；屏幕内容水平边距 20–24dp；主卡片内边距 16–24dp |
| 按钮 | 48px 高，12–15px padding | 至少 48dp 触摸范围；长中文标签换行或增高，不能截断重连入口 |
| 导航 | 70px + 19px 底部区域 | BottomNavigationView + WindowInsetsCompat；不用写死系统导航栏高度 |
| 描边 | 普通 1px；硬朗主题 2–3px | 1dp / 2dp，状态不只依赖细线 |
| 阴影 | 普通低阴影；Brutal 3–5px 偏移 | 普通 elevation 0–4dp；硬边用分层 shape；霓虹光晕只静态、有限范围 |
| 图标 | SVG 24×24、1.7 stroke | VectorDrawable 24dp、保持可见笔画；触摸目标 48dp；有 contentDescription |

## 页面与状态呈现合同

| 维度 | 状态 | 展示 / 允许操作 |
|---|---|---|
| GPS | 搜星 / 未有效定位 | 无坐标；距离、速度、配速 `--`；精度未确定；开始按钮不可用。注意基线当前门控差距需在正式 UI 任务审查，不在此改服务 |
| GPS | 有效定位 | 显示真实精度和采样时间；允许开始；零距离 `0.00` 与未知 `--` 区分 |
| GPS | 正在记录且丢失信号 | 告知 GPS 丢失；瞬时速度/配速 `--`；已积累距离不伪增；轨迹缺口不插值；恢复后只接纳新有效点 |
| 运动 | 主动暂停 | 显示“用户主动暂停”，不写成 GPS 丢失；保留已有指标，恢复与结束可见 |
| 运动 | 恢复 / 结束 | 防止 busy 时连点；调用现有 ViewModel 命令；结束成功后可进入详情；UI 切换不等于暂停服务 |
| BLE | 未连接 / 扫描 / 附近设备 | 名称无值时友好占位；名称、ID、RSSI、扫描进度、权限或蓝牙关闭提示 |
| BLE | 连接 / 订阅中 | BPM `--`；避免把 GATT 连上当作心率数据已就绪；可显示现有 transport 具体状态 |
| BLE | 已连接且新鲜 | BPM、设备、更新时间、接触状态；freshness 必须沿用现有 `HeartRatePanel` 判断 |
| BLE | 重连 / 过期 / 接触不良 | BPM `--`；更新时间保留但明确陈旧；手动重连一直可见；主动断开不能被自动重连策略覆盖 |
| 历史 | 加载 / 空 / 多条 / 错误 | 原生 state.loading/error/more 必须继续呈现。原型模拟空/多条不表示可以丢掉加载错误状态 |
| 详情 | 无样本 / 正常 / 活动记录保护 | 无数据占位；真实 min/avg/max；保护中的删除/导出保持禁用，遵从现有 ViewModel |
| 导出 | 准备 / 保存选择 / 分享 / 成功 / 失败 / 取消 | CreateDocument/chooser 的实际结果驱动反馈，失败可重试，取消不报成功，临时文件生命周期继续由 P0GpxFiles 管理 |

## 图表与轨迹规范

- 心率纵轴明确 BPM，横轴明确活动时间或距离。原型用 SVG 示意，正式数据来自现有 HeartRateRepository / TrackDataHub，不能读取原型内硬编码曲线。
- 采样间隔超过 5 秒、连接 ID 变化、接触丢失等无效段断开；保留 `HeartRateChartHistory` 与 `HeartRatePlot` 既有断点和极值保留策略。
- Canvas 心率线约 2dp，网格低对比 1dp；数据不使用模糊。给 TalkBack 提供时间范围、min/avg/max、数据缺口摘要。
- 轨迹以有效点的包围盒归一化，保留纵横比例；分段绘制，GPS 丢失与暂停间隔不跨段补线。无底图标“轨迹概览”。用 worker 准备绘制数据，UI 线程只绘制。
- 环形图仅作数值的视觉容器，不暗示尚未实现的运动目标、训练评分或健康诊断。

## 交互、兼容性与后续验收

1. 四个主导航在录制、暂停、断连时仍能使用；返回链：历史 → 详情 → 心率详情，返回恢复父页；系统 Back 与工具栏返回一致。
2. 旋转/进程恢复保留选中页和已选记录，用原生 SavedState/Bundle；重建 Activity 不重新启动一场运动，也不自动重新连接一次设备。
3. 错误有明确文字、可重试动作和 TalkBack announce；状态不只用红绿区分；不依赖闪烁动画。
4. minSdk 26：WindowInsetsCompat 处理刘海/状态栏/手势区。不要复刻网页固定手机状态栏，也不使用 API 31 blur 作为基本功能。
5. 设置系统大字体 1.0/1.3/2.0、深浅显示、窄屏横屏、TalkBack、48dp 触摸目标、户外高亮环境分别验收。原型在桌面缩放后的小字号不能作为原生可读性验收依据。
6. 原生重连不稳定是已知真实问题；此原型中的定时成功仅为交互模拟，不证明 BLE 修复。正式验收需真实心率带与 GPS 硬件，不用原型结果代替。
7. 首选验证 01/02/03 的基础布局，复杂的 06 自绘刻度、08 玻璃层次和 10 硬边阴影按选定方案单独实现，不需要一次将十套主题装入 APK。

迁移可行性：十套主要页面均可用现有技术栈实现。新增四栏导航、详情图表接入与轨迹概览是后续 UI 适配工作；服务/数据层保持不变。正式 Android UI 是否采用某方案由用户最终选择。
