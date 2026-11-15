# 阶段1：首页真实离线地图（等待完整回归及截图审查）

**当前状态：PENDING。不能据此启动阶段2或发布 P0.7。**

目标是让首页直接显示用户选择的真实 Mapsforge 离线地图，使用已有位置和轨迹状态，支持精度圈、拖动缩放、跟随恢复和真实无数据状态。

## 实际实现

| 文件 | 实际修改 |
|---|---|
| `p0/app/RouteMapView.java` | 保留四套首页的 XML/ViewBinding 契约，内部嵌入 OfflineMapView；后台读取已选地图和当前瓦片要素；跟随/相机状态恢复；onStop/detach 释放资源 |
| `p0/map/OfflineMapView.java` | 无文件的中性底图；以米为单位的 GPS 精度圈；拖动/多指操作暂停跟随；操作期间禁止自动重定位；保留既有渲染预算 |
| `p0/app/P0Activity.java` | 首页地图启动/停止及相机恢复，绑定既有 recording snapshot，取消妨碍地图交互的整块点击跳转 |
| `p0/app/DashboardState.java` | 有效空闲位置可用于地图、GPS 状态和精度显示；运动速度、距离、时长继续由原有运动状态控制 |
| `values/p07_map.xml`、`values-zh-rCN/p07_map.xml` | 英文/简体中文真实地图、无地图、当前缩放无要素状态 |
| `P07HomeMapTest.java` | 真正 105,834,033 字节地图、包围盒内无要素瓦片、范围外位置、预览与历史隔离、拖动/跟随/重建、替换删除、缓存预算、精度圈、32 张主题/昼夜/屏宽/字体截图及3张空白状态截图 |
| `.github/workflows/p07-clean-navigation.yml`、`.github/scripts/p07_stage.py` | clean 分支专用无密钥验证：原始 Python/JVM/API35 测试；拒绝缺失、空白、失败或新增 Skip 的报告 |
| `.github/workflows/android-build.yml` | 只增加 clean 分支的旧版 PR 版本分配排除条件；main 发布链不变 |

路径 `p0/` 位于 `src/main/java/de/dennisguse/opentracks/`；测试位于 `src/androidTest/java/de/dennisguse/opentracks/p0/map/`。完整变更以本分支 Git diff 为准。

## 设计与边界

复用现有私有地图仓库、Mapsforge 0.25.0 和现有 service snapshot。本阶段没有增加或修改 GPS owner、GpsHandler、SensorManager、TrackRecordingService、BLE、SQLite Schema 43、GPX 导出或历史写入路径。地图只是已有数据的消费者；用于仪器测试的位置输入明确为合成 GPS 测量，不代表真实手机 GNSS 验收。

首页默认跟随；拖动或多指操作退出跟随，重新定位恢复。文件包围盒不能证明覆盖；状态依据实际读取的当前瓦片/缩放要素，不声称行政区或完整道路覆盖。空地图仍可显示位置。首页紧凑布局把定位与缩放控件放在相反角落，全屏地图仍有比例尺。Tile Cache 不超过64块且不超过32MiB，overdraw保持1.0；不将地图完整读入内存。

全局前台定位所有权属于阶段2；开始/结束正式运动交接属于阶段3。此阶段的独立测试通过，不等于 P0.7 整体完成。

## 测试与失败修复

已完成先行截图审查的候选：`9b3641fd579c5c881c6317e3cfac1e60ead11080`，其中一帧位置标记缺失，视觉验收拒绝；覆盖层原子更新修复仍待新一轮验证。

Run：<https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/actions/runs/43741989911>。

上一轮 `dc965a5` / Run 43737955499 的完整测试真实成功：Python 141/141/0/0/0，JVM 167/167/0/0/0，API35 459/458/0/0/1（依次为 Tests / Executed / Failures / Errors / Skipped）。唯一历史 Skip 身份已核实，原始 XML 已提交。然而实际查看 Explorer 原生截图发现距离卡片遮挡控件，以及模拟器外部位置输入干扰有图场景，因此视觉验收明确拒绝，阶段1没有通过。

新候选将 Explorer 距离卡片移出地图、保持原有页面高度预算；测试通过现有受控 GPS fixture 隔离模拟器输入，并逐张要求目标位置、真实要素和可见瓦片完整渲染。新候选的测试和截图仍待取得；不沿用旧源码的测试成功作为本轮验收结论。最终还必须逐张检查全部35张首页地图截图。

已保留两次失败：API26不支持Handler.hasCallbacks（已改为兼容调度）；原生定位按钮覆盖缩放控件（布局断言未删除，已调整角落）。真实截图还发现并修复空闲定位状态与地图标记不一致的问题。失败和取消的运行都不算完整通过；见 `01-stage1-validation.md` 及 `evidence/`。

## Git、APK及下一阶段条件

基线 `ae68319361c1277e97e4fe9bfc9ae52de88146e5`，正式应用基线 `6cee1958a23bfd1a1fe3b6a9b547717c528cbc75`。clean 分支独立提交从 `7ed7fb0` 开始，修复逐次提交，未改写历史。Draft PR：<https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/pull/5>。

本阶段不分配正式版本、不签名、不创建Tag或Release。Debug 构建仅为阶段验证，P0.7固定密钥正式测试交付必须等待阶段1～3全部通过。

阶段2启动条件：当前候选完整报告成功、唯一历史Skip核实、实际原生截图审查通过、验收报告和原始报告索引提交并推送、远程提交核验完成。目前这些条件尚未全部成立。

真实手机 GPS/功耗、Xiaomi CIVI 5 Pro、Redmi Note 12 Pro+ 和 Magene H303：NEEDS_DEVICE_VALIDATION。
