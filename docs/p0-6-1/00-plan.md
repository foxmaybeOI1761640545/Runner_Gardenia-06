# P0.6.1 真实地图复核

基线 c7112514565c6b10dac7131b97432328ec67ef1d；独立分支 feature/p0-6-map-real-validation。用户当前请求优先于 AGENTS.md 中历史阶段分支要求。原 P0.6 af6e90e、6778、Tag、Release 及全部历史 refs 只读保留。

已独立读取全部10个远程 Release/build-info；最高6778。分配独立 P0.6.1/6779 reservation，原 p06_version.py 不改变，新增 p061_version.py 和历史校验测试。

地图原 Artifact14061852955/archive SHA256 a00ee8a2f30667b27a33a97a25aea81a22e19cd2d031765c16c54956666209f7；实际文件105834033字节，SHA256 5412067262dace98d9f95b02621446516e7d98e1debbb6a314d575cae022ee1e。已下载完整文件、manifest、acceptance-report、boundary diagnostic、署名并逐项读取。Mapsforge Reader/Writer0.25.0。文件不进入 Git 或正式 APK；CI 临时复制至 androidTest assets 供真实大文件测试使用。

修复策略：地图不写 GPS 来源/采样间隔偏好；服务仅在空闲且有地图 owner 时给现有 GpsHandler 临时 INTERNAL/1秒配置。记录/准备仍使用原配置。owner释放与进程退出无需恢复持久偏好。保留 GPS/BLE/数据库43/GPX主路径。

增加地图本机诊断：中心/zoom/可见tile X/Y、文件矩形、reader支持/要素数/缓存状态；明确矩形不是行政区范围，无要素和未缓存不能单独判故障。诊断读取使用独立后台 MapFile 并关闭，不自动导出真实位置。旋转恢复视口。继续使用32MiB/64瓦片上限，MapView.destroyAll按上游实现关闭renderer和MapFile。

新增独立真实文件JVM摘要/城市要素检查、Activity偏好/owner/记录不受关闭影响测试、真实SAF契约导入/飞行/17地点3zoom原生截图/轨迹叠加/重建/失败导入保留/进程重启检查。原完整及专项回归不删改断言、不增加Ignore。SAF契约测试使用测试Provider；签名运行仍须系统picker验证。

边界缺口：成功构建工具只选择性引入原两文件；将实际target-region.geojson与其摘要加入未来Artifact。现有地图不重建，诊断Job使用相同SHA的china-261112.osm.pbf重现边界并比较relation选择；检查排除城市点，后续需要区域采样和道路连续性分析。不能将点验收报成全区域通过。

2026-11-14用户录屏39.067秒、432×960；已查看每4秒关键帧联系表。可识别汕尾/陆丰及周边地名、矩形灰块/缩放残留和细道路。录屏无中心坐标/zoom/瓦片/读取日志，不能逐帧确定C/D。后续按当前源码原生截图与读取证据诊断，未得到证据的手机GNSS/H303/耗电保持NEEDS_DEVICE_VALIDATION。

所有自动化/视觉/签名/升级/公开下载门禁通过后才由新独立工作流发布非Latest测试Pre-release；没有批准记录时不访问签名Secrets。不得把历史P0.6证据作为本轮通过。
