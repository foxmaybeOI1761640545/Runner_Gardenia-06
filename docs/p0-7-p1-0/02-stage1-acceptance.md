# 阶段1：首页真实离线地图（通过）

**当前状态：PASS（阶段1）。阶段2可以在此报告推送及远程核验后启动；P0.7发布仍需阶段2、3通过。**

目标是让首页直接显示用户选择的真实 Mapsforge 离线地图，使用已有位置和轨迹状态，支持精度圈、拖动缩放、跟随恢复和真实无数据状态。

## 实际实现

| 文件 | 实际修改 |
|---|---|
| `p0/app/RouteMapView.java` | 保留四套首页的 XML/ViewBinding 契约，内部嵌入 OfflineMapView；后台读取已选地图和当前瓦片要素；跟随/相机状态恢复；onStop/detach 释放资源 |
| `p0/map/OfflineMapView.java` | 无文件的中性底图；以米为单位的 GPS 精度圈；拖动/多指操作暂停跟随；操作期间禁止自动重定位；保留既有渲染预算 |
| `p0/app/P0Activity.java` | 首页地图启动/停止及相机恢复，绑定既有 recording snapshot，取消妨碍地图交互的整块点击跳转 |
| `p0/app/DashboardState.java` | 有效空闲位置可用于地图、GPS 状态和精度显示；运动速度、距离、时长继续由原有运动状态控制 |
| `layout/gardenia_home_explorer.xml` | 将距离卡片移至地图下方，保留原页面高度预算，消除对缩放/定位控件的遮挡 |
| `values/p07_map.xml`、`values-zh-rCN/p07_map.xml` | 英文/简体中文真实地图、无地图、当前缩放无要素状态 |
| `P07HomeMapTest.java` | 真正 105,834,033 字节地图、包围盒内无要素瓦片、范围外位置、预览与历史隔离、拖动/跟随/重建、替换删除、缓存预算、精度圈、32 张主题/昼夜/屏宽/字体截图及3张空白状态截图 |
| `.github/workflows/p07-clean-navigation.yml`、`.github/scripts/p07_stage.py` | clean 分支专用无密钥验证：原始 Python/JVM/API35 测试；拒绝缺失、空白、失败或新增 Skip 的报告 |
| `.github/workflows/android-build.yml` | 只增加 clean 分支的旧版 PR 版本分配排除条件；main 发布链不变 |

路径 `p0/` 位于 `src/main/java/de/dennisguse/opentracks/`；测试位于 `src/androidTest/java/de/dennisguse/opentracks/p0/map/`。完整变更以本分支 Git diff 为准。

## 设计与边界

复用现有私有地图仓库、Mapsforge 0.25.0 和现有 service snapshot。本阶段没有增加或修改 GPS owner、GpsHandler、SensorManager、TrackRecordingService、BLE、SQLite Schema 43、GPX 导出或历史写入路径。地图只是已有数据的消费者；用于仪器测试的位置输入明确为合成 GPS 测量，不代表真实手机 GNSS 验收。

首页默认跟随；拖动或多指操作退出跟随，重新定位恢复。文件包围盒不能证明覆盖；状态依据实际读取的当前瓦片/缩放要素，不声称行政区或完整道路覆盖。空地图仍可显示位置。首页紧凑布局把定位与缩放控件放在相反角落，全屏地图仍有比例尺。Tile Cache 不超过64块且不超过32MiB，overdraw保持1.0；不将地图完整读入内存。

全局前台定位所有权属于阶段2；开始/结束正式运动交接属于阶段3。此阶段的独立测试通过，不等于 P0.7 整体完成。

## 当前源码的原生视觉复核

`e08887d479719699e474e8407d4ac90f98fc128a` / [Run 43743383681](https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/actions/runs/43743383681) 的定向测试为 Tests 2 / Executed 2 / Failures 0 / Errors 0 / Skipped 0。Artifact `14173223799` 保留35张实际 API35 原生截图。逐张查看全部32张真实地图矩阵和3张空白状态截图后，当前位置实心标记、真实道路/地名、分离的定位/缩放控件、可滚动访问的开始按钮均正常。覆盖四主题、昼夜、360/390dp、1.0/1.4字体。该源码的视觉检查通过；原始定向 XML、每张图的 SHA256 及逐图结论见 `evidence/run43743383681-focused/`。

同一源码的完整回归现已成功完成，并已独立下载 Artifact `14174890299`，ZIP SHA256 `3f1a7876a034a2f4b80193e5f85cc6b37f23f5726bc5990959f8e61b55daaaae` 与 GitHub 摘要一致。重新解析原始 XML 确认报告非空、所有原有测试类齐全：

| 套件 | Tests | Executed | Failures | Errors | Skipped |
|---|---:|---:|---:|---:|---:|
| Python Helpers | 141 | 141 | 0 | 0 | 0 |
| JVM | 167 | 167 | 0 | 0 | 0 |
| Android API35 | 459 | 458 | 0 | 0 | 1 |

唯一 Skip 仍为 `TrackRecordingServiceStateMachineTest.recording_stopGPS_noop`，未算作通过。原始 Android XML、20份 JVM XML、gate/helpers JSON、实际 Actions Job/Step 状态和摘要见 `evidence/run43743383681-full/`。Debug构建、Lint、Android测试编译均成功。原始 GPS ownership/preferences 测试源文件与正式基线逐字一致。完整回归保留原有 GPS/BLE/GPX/数据库/升级测试；正式发布专用的外部新进程、签名升级等额外烟测仍需在 P0.7 交付门禁执行。

此轮35张视觉复核取自同 Run/同源码的先行原生检查点；全量回归另有35张截图及哈希索引，不将它们宣称为第二次逐张视觉检查。两次仪器执行之间没有产品源码修改。

## 测试与失败修复

已完成先行截图审查的候选：`9b3641fd579c5c881c6317e3cfac1e60ead11080`，其中一帧位置标记缺失，该轮视觉验收拒绝；后续覆盖层原子更新已由本报告的e08887d同源码完整验证通过。

Run：<https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/actions/runs/43741989911>。

上一轮 `dc965a5` / Run 43737955499 的完整测试真实成功：Python 141/141/0/0/0，JVM 167/167/0/0/0，API35 459/458/0/0/1（依次为 Tests / Executed / Failures / Errors / Skipped）。唯一历史 Skip 身份已核实，原始 XML 已提交。然而实际查看 Explorer 原生截图发现距离卡片遮挡控件，以及模拟器外部位置输入干扰有图场景，因此视觉验收明确拒绝，阶段1没有通过。

新候选将 Explorer 距离卡片移出地图、保持原有页面高度预算；测试通过现有受控 GPS fixture 隔离模拟器输入，并逐张要求目标位置、真实要素和可见瓦片完整渲染。新候选进一步以 `e08887d` 的覆盖层原子更新修复缺失标记，新增实际 PNG 中可见标记断言。该源码已经取得上述全量成功及35张实际视觉复核，不沿用旧源码的成功结论。

已保留两次失败：API26不支持Handler.hasCallbacks（已改为兼容调度）；原生定位按钮覆盖缩放控件（布局断言未删除，已调整角落）。真实截图还发现并修复空闲定位状态与地图标记不一致的问题。失败和取消的运行都不算完整通过；见 `01-stage1-validation.md` 及 `evidence/`。

## Git、APK及下一阶段条件

基线 `ae68319361c1277e97e4fe9bfc9ae52de88146e5`，正式应用基线 `6cee1958a23bfd1a1fe3b6a9b547717c528cbc75`。clean 分支独立提交从 `7ed7fb0` 开始，修复逐次提交，未改写历史。Draft PR：<https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/pull/5>。

本阶段不分配正式版本、不签名、不创建Tag或Release。Debug 构建仅为阶段验证，P0.7固定密钥正式测试交付必须等待阶段1～3全部通过。实际 Debug APK：14,381,516字节，SHA256 `af5929dad26bfdab51dbd6f31c6c0217e233ee07fc3ea27847d3750f49480b95`。其版本属性沿用6779以供内部测试，包名仍为原有 Debug 变体 `com.fox.runner.debug`；不是正式 `com.fox.runner` 固定密钥发布包，不替换 P0.6.1。

阶段2启动条件：当前候选完整报告成功、唯一历史Skip核实、实际原生截图审查通过、验收报告和原始报告索引提交并推送、远程提交核验完成。当前源码完整报告、唯一历史Skip和原生视觉检查已通过；本报告及原始证据提交后执行远程核验，再启动阶段2。

真实手机 GPS/功耗、Xiaomi CIVI 5 Pro、Redmi Note 12 Pro+ 和 Magene H303：NEEDS_DEVICE_VALIDATION。
