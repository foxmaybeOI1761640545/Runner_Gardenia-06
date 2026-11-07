# 主副指标位置交换合同

同步开发起点：远端 `design/p0-ui-lab` 的 `0de2481a85eeee192ea0f203429bf086865ba681`，不是旧的 df900b1。仅 UI Lab 的 JavaScript/CSS/HTML、验收、文档与预览更新；原生 Android 不变。

## 位置模型

`metricLayoutsByTheme[themeId] = {primary, secondary, notice}`。`secondary` 是有序的四位置数组，最后一位始终为 `gpsAccuracy`。九套主题初始为 `distance / [duration, paceSpeed, heartRate, gpsAccuracy]`；Neon 为 `heartRate / [distance, duration, paceSpeed, gpsAccuracy]`。这与同步后的源码设计一致。

`swapMetric(slot)` 只对 `primary` 与 `secondary[slot]` 做一次解构交换，拒绝非整数、越界和辅助信息位置；不 filter、不 sort、不重新构造副位置顺序。全部主题共享这一入口，各主题只提供自己的 hero 容器。四个核心指标永远恰好出现一次。

布局映射独立于模拟运动的 `state`，所以切换主题、进出历史/心率/设置、重置演示数据都保留每套主题的主副位置。刷新页面会恢复默认；不使用后端或账号，也不把这个会话选择写入云端。

## 数据与呈现

`motion()` 继续使用已有 GPS/BLE/运动状态投影；`metricData()` 是主、副共用的唯一指标格式入口。`primaryMetric()` 与 `metric()` 消费相同的 value/unit/label。每秒 `updateMotionMetrics()` 依据 `data-metric-id` 更新现有文本节点，不通过中文标签推断指标，不重建整页 DOM。

| 指标 | 示例与规则 |
|---|---|
| distance | 5.24 km / 3.26 mi；两位小数；未定位或丢失时占位 |
| duration | 28:35 / 01:25:30；沿用 mm:ss 与 HH:mm:ss；暂停冻结 |
| paceSpeed | 5′27″/km / 8′46″/mi，或 11.0 km/h / 6.8 mph；动态名称与单位 |
| heartRate | 新鲜模拟值 148 BPM；未连接、过期等非有效状态为 -- BPM |
| gpsAccuracy | 有效 ±5 m，否则 --；固定第四副位置，不参与交换 |

速度仍取当前已有模拟速率 `5.24 km / 1714 s`，按原有一位小数规则得到 11.0 km/h；没有为匹配示例数值重写运动、GPS 或 BLE 逻辑。

Neon 主标题跟随当前指标；波形保留，并独立标注“心率曲线 · BPM”。01 网格、02 留白、03 地形地图、04 霓虹曲线、05 编辑结构、06 刻度仪表、07 柔和圆环、08 渐变玻璃、09 竞速数字、10 几何硬边均继续保留。较长的时间/配速使用各主题自己的字号变量，单位保持整体，不截断数字。

## 可访问交互

三个可交换副位置用 `button type=button`，GPS 辅助信息为非交互 div。按钮有动态 aria-label、鼠标悬停/按压反馈、可见焦点，支持触屏、Tab、Enter、Space。交换后焦点回到同一个副位置（此时内容是原主指标），不跳到第一张卡。

可见结果提示在数据网格下方的正常文档流中；屏幕阅读器通过稳定的 `#metric-announcement` live region 接收公告，不用覆盖数据的 toast。原有记录、导航、导出按钮继续使用原事件委托，新增 `swap-metric` action 与它们分开。

## 验收

`tests/metric_swap_acceptance.py` 接入原 `browser_acceptance.py`，两次运行分别针对发布前本地站点和发布后的实际 Pages URL。逐一覆盖 10 主题 × 3 视口的连续位置交换、其他位置不动、无重复/丢失、格式、GPS/BLE、暂停、单位/配速偏好、焦点及 DOM 节点保持；另有主题/页面状态保留、真实 Chromium 触屏输入，以及通过真实模拟 ticker 推进到 01:25:30 后的全主题三视口检查。

原有全部回归断言保留。`metric-swap-*.png` 为交换和长时长的真实浏览器截图，随紧凑验收 Artifact 保存；`previews/theme-01.png` 至 `theme-10.png` 更新为新版本的默认布局截图。完整测试结果、来源提交与运行地址以验收 JSON、PROVENANCE 与 TEST_REPORT 为准。
