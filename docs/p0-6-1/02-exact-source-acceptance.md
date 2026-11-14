# P0.6.1 精确源码验收

应用与测试源码：`6cee1958a23bfd1a1fe3b6a9b547717c528cbc75`；main tree：`4b0442e84948da0180bb5b5b17715747115401b7`。

[完整运行43609863706](https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/actions/runs/43609863706)，push/feature/p0-6-map-real-validation/attempt1，终态success。scope、checks、map-smoke、native-window-helper、map-geometry、review-transfer均成功；此验证模式的signed/publish正确未启动。

原XML独立提取：

|范围|Tests|Executed|Failures|Errors|Skipped|
|---|---:|---:|---:|---:|---:|
|Helpers|141|141|0|0|0|
|JVM|167|167|0|0|0|
|完整API35|457|456|0|0|1|
|快速地图/GPS子集|69|69|0|0|0|

唯一历史Ignore为TrackRecordingServiceStateMachineTest.recording_stopGPS_noop，未计通过。子集与全量重叠，不相加。真实100MB独立write/read各执行1项，PID2118/2484；DB/指标/批量探针共享write/read，PID1949/2265，不将三个领域重复加总。GPX1.1、离线Manifest、schema43和原回归均实际通过。

实际文件105834033字节，SHA256 `5412067262dace98d9f95b02621446516e7d98e1debbb6a314d575cae022ee1e`。17地点×z7/12/16的51张、历史轨迹与重建2张共53张通过严格可见瓦片完成和直接原生视觉复核；真实文件原生zoom按钮17/16与触摸拖动中心变化已断言。源副本删除、私有摘要、损坏/中断保留、切换删除、飞行模式、实际新进程重开均通过。全部必需378张及额外172张历史UI、1张原合成地图进程图已查看，共551张原生PNG；没有以瓦片计数代替画面审查。

完整回归进程的导入至稳定首视口6766ms；期间采样Java heap峰103793936字节、PSS313053KiB，heaplimit201326592字节。快速独立进程对应8132ms/76754624字节/210748KiB。它们是本轮API35测量，非手机性能基准；32MiB仅瓦片缓存上限。复制保持64KiB流式，没有100MB byte[]。实际画布1080×1528与视口一致，tile704、缓存16块、overdraw1.0。

报告Artifact14120153708/archive SHA256 `14df2c2d820473a09a210ebfb3f7f5d5943220635a681de17b4ced5c036db456`；视觉14120203743/SHA `0299b91995cf2d3d75d250005b255b0ab83553fbaffdb75eacf4e08da26f57cd`；快速14119833108/SHA `813e3c8e4ff2f638ee523050ec3c8b05ebb35377940f8e49c5b0c5749f6db7ba`。全部实际下载计算并核对。

边界Artifact14120306229/archive SHA `c29ec01ede070c34cd361a7059d1e1e835643b1f8d400e03e76c30c454da6193`。完整几何及来源、覆盖审计已原样保存在map-boundary目录，实际GeoJSON SHA `c1dcdecec318fadafd854117582f7b603bf4eaf1fa928edf849112ae8a58161e`。原地图不重建、不扩围；全部1727目标相交z12瓦片0不可读、98无要素；5排除城市中心在外。单一行政Polygon含海域，不等于所有路网连通；高zoom福建视觉道路几何相交诊断支持漳州/龙海/漳浦/东山同一主视觉分量，但不是导航可达性证明。不存在矩形/convex hull替代。

录屏39秒逐秒查看，有灰矩形、缩放残留及细道路；缺少中心/zoom/tile日志，不能逐帧唯一分类。新诊断使用真实renderer Job键直接查询缓存，报告中心/zoom/tiles/要素/bbox/缓存/画布。未发现本轮目标视口内未完成瓦片灰块；合法文件外灰色、海域与无要素仍保留。未声称每条道路、每个空点或手机长期表现全部通过。

Xiaomi CIVI5Pro、真实GNSS、Magene H303、长期耗电/后台省电、真实微信与TalkBack语音仍为NEEDS_DEVICE_VALIDATION。本批准只授权以这些已验收字节源码使用原固定密钥构建；不等于新APK的签名、覆盖升级或公开发布已通过。
