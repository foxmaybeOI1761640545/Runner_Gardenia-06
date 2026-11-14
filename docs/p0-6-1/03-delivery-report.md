# Runner Gardenia P0.6.1 中文交付报告

## A. Git、来源与历史保护

原始开发基线为 `c7112514565c6b10dac7131b97432328ec67ef1d`，独立分支为 `feature/p0-6-map-real-validation`。实际验收、编译与新Tag指向的源码为 `6cee1958a23bfd1a1fe3b6a9b547717c528cbc75`，`src/main` tree为 `4b0442e84948da0180bb5b5b17715747115401b7`。签名批准提交 `8958de3422e62b6e1ad69e26690754ad8c3d069e` 和后续发布批准只增加批准记录与文档，不能改变已验收的应用、测试、版本或构建代码。发布批准与最终文档HEAD见本目录的最终索引；不要把文档HEAD误认成APK源码。

原P0.6源码 `af6e90eabee20ada79ba942584ab7eb8c08660e3`、版本6778、Tag/Release/APK全部保留。正常提交、正常推送独立分支，无main修改、强推、历史改写或历史资产替换。CI逐项核对 `.github/p061/protected-refs.json` 中原分支与Tag快照。服务器分支保护查询返回403，不能声称已检查或修改服务器保护设置。新版本分配前核查全部远程refs、10个既有Release及其build-info、台账与properties，原最高6778；新6779独立预留并验证reservation、seriesHighWater、codeHighWater一致。旧 `p06_version.py` 的来源保护没有放宽。

完整变更提交和文件清单见 `development-commits.txt`、`changed-files.txt`；可复核对比基线到实际源码，再单独检查批准/文档提交。

## B. 真实地图、范围与覆盖证据

|项目|实测值|
|---|---|
|文件|RunnerGardenia_GD_PRD_Zhangzhou_Dongshan.map|
|字节数|105834033|
|SHA-256|5412067262dace98d9f95b02621446516e7d98e1debbb6a314d575cae022ee1e|
|构建运行 / Artifact|43470716313 / 14061852955|
|完整Artifact ZIP SHA-256|a00ee8a2f30667b27a33a97a25aea81a22e19cd2d031765c16c54956666209f7|
|Mapsforge Reader / Writer|0.25.0 / 0.25.0|
|文件外接范围|经度111.349484..118.488622；纬度21.025508..25.222025|

原Artifact未过期，已下载完整ZIP、实际计算摘要并读取manifest、acceptance-report、ATTRIBUTION与边界诊断；附件传输32MiB上限没有被误当完整100MB地图。原地图未重建、未扩围，100MB文件没有进入Git或正式APK；CI临时测试资产/adb独立传输。

为补齐原包未保存完整多边形的证据，用同一原始PBF `china-261112.osm.pbf`（SHA-256 `c2f4fb0e0239219c4b9f02a90355f65c0d2986d7c3c948324acc72000a6f8be3`）复现原行政关系与750米缓冲union。完整 `map-boundary/target-region.geojson` SHA-256为 `c1dcdecec318fadafd854117582f7b603bf4eaf1fa928edf849112ae8a58161e`，几何有效且为单个Polygon。没有用矩形或convex hull替代真实边界；行政海域仍可能位于真实关系内。

Reader与原生图像检查覆盖广州、深圳、珠海、佛山、东莞、中山、江门、惠州、肇庆、汕尾、揭阳、汕头、潮州、漳州城区、龙海、漳浦、东山岛，共17地点，每处z7/12/16。特别复核佛山、珠海及珠三角连接、粤东四市与福建沿线。长泰、云霄、诏安、南靖、平和、华安等归属由原行政几何/manifest验证；未将17个图像点说成每个区县每条道路都已逐一截图。

全部1727个与实际多边形相交的z12瓦片扫描：0不可读、98无要素。空瓦片不自动等于裁切缺失。梅州、阳江、茂名、湛江、泉州的排除检查点都在目标几何外；此为实际几何和点检查，不声称各非目标行政区每个点都没有缓冲交叠。原设计没有梅州路线走廊。

漳州至东山额外扫描33101个目标相交z16瓦片、55064条独特highway线。0.2米几何容差下，漳州、龙海、漳浦、东山附近道路属于同一主视觉分量；G357双桥与邻接trunk端点实际存在。该检查考虑编码精度与线段内部交点，属于渲染道路连续性诊断，不是导航可达性验证，不能据此声称每条路拓扑均正确。完整几何、manifest、覆盖CSV和方法限制保存在 `map-boundary/`。

署名继续为 © OpenStreetMap contributors，https://www.openstreetmap.org/copyright，遵循ODbL；Mapsforge与AndroidSVG许可证保留。仅选择性引入成功工具分支的workflow/build_map.py，并使今后Artifact保留实际GeoJSON与摘要；未合并失败地图实现或整个工具分支。

## C. 修改、原问题与修复后行为

|修改与路径|原问题及修复行为|回归与影响|
|---|---|---|
|`P06MapActivity`、`SensorManager`、`GpsHandler`、`SensorHandler`、`GpsInternal`、`TrackRecordingService`|地图原来持久写入GPS=INTERNAL及间隔1；现在从不改写这两个用户偏好，由服务在空闲地图owner期间提供临时内部GPS/1秒读取配置，运动/准备使用原配置；同配置不重连，最后owner退出先断开不再需要的GPS再恢复临时模式|4个Activity偏好/所有权测试覆盖NONE/7、REMOTE/7且HR预览、INTERNAL/7运动driver保持、旋转与重复进入；owner无泄漏、运动继续、HR Handler保留。BLE算法不变|
|`OfflineMapRepository`|保留64KiB后台流式复制，补完整复制长度及提交前中断检查；损坏/中断保留旧选择并清理pending|真实100MB摘要、源副本删除、持久化、新进程、切换/删除、损坏/中断实际通过；没有整图byte[]|
|`MapRenderBudget`、`OfflineMapView`|Mapsforge默认方形离屏画布及1.2 overdraw增加工作集并与可见瓦片争用有限LRU；改用北向矩形画布/1.0 overdraw|缓存仍32MiB、最多64块；3个JVM预算测试及真实地图全可见瓦片完成/截图验证，没有扩大缓存或地图范围|
|`MapDiagnostics`、`OfflineMapView`、本地诊断菜单|新增中心/zoom/可见tile X/Y、MapFile bbox、Reader支持/ways/POI、真正renderer键缓存状态、画布尺寸/缓存容量/完成数；独立后台MapFile始终关闭|删除曾产生假阴性的128项job历史旁路，直接查询原缓存；缺行政多边形时明确UNKNOWN，bbox不冒充覆盖边界；诊断不自动外传位置|
|`P06MapActivity`|保存并恢复重建前地图视口；查看地图不需要定位授权，点击我的位置才按需申请精确权限|真实历史轨迹/分段、重建、无定位地图、普通GNSS模拟、关闭地图记录继续、大字/小屏原生操作验证|
|独立P061 CI/版本/发布脚本|避免把后续文档HEAD重新绑定旧P0.6；绑定真实源码、raw XML、截图、固定签名字节与独立批准|新6779；旧来源冲突保护保留；签名仅专用job读取android-release原Secrets，JKS只在RUNNER_TEMP并trap/always清理|

上述Java文件位于 `src/main/java/de/dennisguse/opentracks/` 相应map/sensors/services目录。`MapView.destroyAll`按Mapsforge0.25销毁layer、关闭MapFile并释放缓存；切换/删除先销毁使用者。无INTERNET、在线地图/瓦片/API Key/WebView/账号/同步；Application ID仍com.fox.runner、namespace不变、SQLite schema43不变。GPS记录主链路、暂停/恢复、P0.5.1过滤、BLE心率采样/原始心率、GPX1.1、历史、批量ZIP与四主题八明暗均保留回归。

旧版本已经覆盖且未保存的用户偏好无法凭空推回；本次保证地图不再污染持久偏好。

空白分类：A实际覆盖外；B该级别无要素/海域；C应有源数据缺失；D应用渲染/缓存问题。用户39.067秒实机录屏已逐秒查看39帧，存在灰矩形与缩放残留，也有细道路；没有中心/zoom/tile日志，不能唯一归因每处手机空白。CI曾重现Reader有大量ways但尚未缓存的灰块，拒绝早期截图后定位并修复上述D类问题。最终当前源码的目标视口及签名视口严格等待全部支持瓦片完成，再直接看真实原生画面；合法文件外灰色仍存在。没有据此宣称C绝不存在或整张地图每个点已穷尽验收。

## D. 实际自动化、原始报告与视觉证据

完整验收运行：[43609863706](https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/actions/runs/43609863706)，来源6cee1958，push，同一独立分支，attempt1，终态success。scope、checks、map-smoke、native-window-helper、map-geometry、review-transfer成功；签名/发布在此验证模式按设计未启动。

|真实XML范围|Tests|Executed|Failures|Errors|Skipped|
|---|---:|---:|---:|---:|---:|
|Python Helpers|141|141|0|0|0|
|全部JVM|167|167|0|0|0|
|完整Android API35|457|456|0|0|1|
|地图/GPS快速子集|69|69|0|0|0|

原始JVM XML位于Artifact中的 `build/test-results/testReproducibleDebugUnitTest/TEST-*.xml`，Android XML为 `build/outputs/androidTest-results/managedDevice/debug/flavors/reproducible/mediumPhone/TEST-mediumPhone.xml`，汇总与源绑定见 `build/reports/p061-validation/tests.json`。

唯一Skip是历史已有 `TrackRecordingServiceStateMachineTest.recording_stopGPS_noop`；没有新增Ignore/Skip，也没有删除失败测试或降低断言。快速子集与完整Android重叠，不相加；NO-SOURCE未计为执行。真实100MB独立write/read各实际1项，PID2118/2484；DB/指标/批量共享write/read探针PID1949/2265，三个领域不重复计数。

完整Debug、Lint、JVM/API35、GPX1.1、离线合并Release Manifest及打包Debug权限、schema43、数据持久化、新进程、历史回归均已通过。全部必需378张图与额外173张历史原生图，共551张已直接查看；其中真实17×3地点图、历史轨迹与重建共53张，另有独立飞行模式新进程图。截图具有原生窗口XML、focus与逐文件SHA，不以瓦片计数替代实际内容。

实际100MB文件完成原生zoom按钮17→16、触摸拖动中心变化、SAF契约导入、私有摘要、源Provider副本删除、损坏/中断保留、切换/删除、飞行模式、新进程恢复。完整回归采样导入至稳定首视口6766ms，Java heap峰103793936字节、PSS313053KiB、heaplimit201326592字节；独立快速进程8132ms/76754624字节/210748KiB。这是模拟器过程采样，不是手机长期性能基准；32MiB仅tile cache预算，不是整个应用内存上限。实际framebuffer1080×1528与viewport一致，tile704、cache16、overdraw1.0。

|证据Artifact|ID|实际下载ZIP SHA-256|
|---|---|---|
|完整原XML/报告|14120153708|14df2c2d820473a09a210ebfb3f7f5d5943220635a681de17b4ced5c036db456|
|完整原生图集|14120203743|0299b91995cf2d3d75d250005b255b0ab83553fbaffdb75eacf4e08da26f57cd|
|快速真实地图/GPS|14119833108|813e3c8e4ff2f638ee523050ec3c8b05ebb35377940f8e49c5b0c5749f6db7ba|
|完整边界/覆盖|14120306229|c29ec01ede070c34cd361a7059d1e1e835643b1f8d400e03e76c30c454da6193|

历史失败和取消记录仍保留；根因、修复见01-ci.md，包括类型编译、旧GPS夹具、ActivityScenario生命周期、真实瓦片未完成和诊断历史假阴性。它们未被当作当前成功。签名运行43613996030已终态success；scope、signed、signed-review-transfer成功，其他模式Job按设计跳过。实际覆盖升级、99张签名图和正式Artifact均已复核，细节见04-signed-publication-index.md。发布终态与匿名下载结果须以该批准提交对应的发布运行和最终交付消息为准，批准记录本身不冒充公开下载回执。

## E. APK与发布

固定密钥v0.1.0-p0.6.1 /6779，Application ID com.fox.runner，APK3234719字节，SHA-256 `b6df2f1ad0431c5a8ccd805209d7452171eb5fdbd611bc2b8f3f4d9bcf4d8e11`，证书SHA-256 `E3E7D46BFCB869F5A521A7FBA6FD1BA5790D7DBCAF37F722DDC69D7BB1439A22`。R8、资源优化、zipalign与apksigner实际执行；最终发布索引须同时附签名/覆盖升级和匿名下载回执，单纯构建成功不等于交付通过。

新版本固定Release地址（发布终态以回执为准）：https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/releases/tag/test-p0-v0.1.0-p0.6.1

新版本固定APK地址（发布终态以回执为准）：https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/releases/download/test-p0-v0.1.0-p0.6.1/RunnerGardenia-P0-Release.apk

只允许新唯一Tag、测试Pre-release、非Latest；发布批准搬运已验收同字节APK，不重编译、不重签，不替换旧版本附件。

## F. 状态边界及用户最后验收

|类别|状态与限制|
|---|---|
|完整自动化、原功能回归|已通过当前源码，原XML计数如上|
|真实105834033字节地图文件|Reader、导入/摘要/持久化、17地点多zoom、边界/排除/连续性诊断通过所述范围；不宣称每条道路穷尽|
|API35原生图像|当前全量551张及签名99张已直接查看；升级证据见最终索引|
|用户旧实机录屏|39帧已看，确认现象；缺坐标无法逐空白唯一分类，不算新版手机通过|
|Xiaomi CIVI 5 Pro、真实GNSS、Magene H303 BLE、长期后台/耗电|NEEDS_DEVICE_VALIDATION|
|微信真实接收、TalkBack实际语音|NEEDS_DEVICE_VALIDATION；模拟接收/原生UI不替代实体客户端/读屏|

手机步骤（尚未执行的项目不要标通过）：

1. 从新Release下载APK，直接覆盖P0.6；不卸载、不清理数据。先确认历史、原始心率、GPS来源/间隔和主题保留。
2. 用系统文件管理器导入上述100MB地图，打开飞行模式；确认重开无需网络或原下载文件。
3. 查看广州、佛山、珠海、深圳、珠三角其他城市、汕尾/揭阳/汕头/潮州、漳州/龙海/漳浦/东山岛；分别缩放、拖动并查看道路、水系和地名。
4. 遇到空白，打开“地图诊断（本地）”，记录中心、zoom、tile X/Y、Reader要素与renderer状态；可同时录屏。文件bbox本身不代表实际行政覆盖。
5. 点击我的位置再按需授权，户外检查位置；打开历史与实时轨迹、全程适配，关闭地图确认运动持续、GPS设置保持、Magene H303心率继续。
6. 强制结束/重开应用确认本地地图保留；观察长时间缩放、拖动、后台、内存/耗电与小米省电策略。将真机结果单独记录，尤其GNSS/BLE和长时表现。
