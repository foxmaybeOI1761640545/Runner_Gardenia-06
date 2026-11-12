# P0.6 — 完全离线地图技术决策与验收

## 已发布基线

本阶段独立分支 `feature/p0-6-offline-map`，从 P0.5.1 最终记录提交 `ba00559af1af3d48bc3ecfde811d3e8d062e2f68` 建立。实际已验收／发布 APK 源码为 `a4b2c7a7521a19a60c1d444449ff82c0560e4d19`，Tag 为 `test-p0-v0.1.0-p0.5.1`，APK SHA-256 为 `a99a489ffdc63c746346d9a6521884b4fc21a6698552f10c4963711b2232816d`。原 Tag、分支、发布资产、证书和 GPS 修复保持独立。

## 决定：Mapsforge 0.25.0

已读取 Mapsforge 与 VTM 的上游 README、许可证说明和 Maven 实际 POM／源码包。固定选择 Maven Central 已发布的 Mapsforge 0.25.0，不使用动态版本，也不同时引入第二个地图引擎。

| 事项 | Mapsforge | VTM |
|---|---|---|
| 渲染 | Java 原生 Android／Canvas 位图瓦片渲染 | OpenGL 矢量渲染及 Android 平台后端 |
| 本地格式 | 直接读取 Mapsforge `.map` | 支持 `.map` 及更多瓦片源 |
| 当前需求匹配 | 本地地图、二维缩放拖动、位置与轨迹覆盖层已足够 | 更适合需要额外图形后端能力的场景 |
| 项目集成 | 贴合现有 Java/XML/ViewBinding/Canvas，减少平台和生命周期集成面 | 首阶段增加 OpenGL、图形后端与平台兼容验证成本 |
| 许可证 | LGPLv3，另有上游 §4(d)/(e) 豁免说明 | 同类 LGPLv3 豁免，另有多项第三方代码说明 |

应用只依赖 `mapsforge-map-android`、`mapsforge-themes` 0.25.0 及其必要的 core/map/reader、AndroidSVG 1.4 传递依赖。地图 writer 仅作为生成测试数据的开发工具，不能进入 APK。不使用下载层、在线瓦片源、HTTP 扩展、API Key 或在线主题。

## 文件、存储和渲染

通过系统 Storage Access Framework 选择地图文件，在后台流式复制到应用私有 `files/offline-maps/`。验证 `.map` 文件头、版本、边界和读取能力后才提交本地索引；损坏／不支持／中断的导入不替换当前地图。持久副本不依赖原 URI、外部文件是否移动或 Provider 是否联网。地图列表支持选择与删除，重启仍加载已选择的本地文件。文件、索引和缓存只保存在本机。

使用 MapFile → TileRendererLayer → 内置打包 XML 主题 → 原生 MapView。瓦片缓存仅使用有容量上限的本机内存；缩放、拖动及范围适配使用地图引擎原生能力。真实地图内容来自导入文件，不以现有示意等高线画布代替。

轨迹从现有 WorkoutRepository／SQLite 读取；按手动和自动轨迹段分组创建 Polyline，断点不跨线。正在记录的轨迹和当前位置复用现有服务／GPS Handler 的经过验证数据，不建立第二套定位、BLE、计时或数据库链路。单独查看当前位置必须不创建运动；离开地图不能停止正在记录的运动。

## 分阶段实现与验收

第一阶段：导入、有效性检查、持久副本、本地加载、地图渲染、缩放拖动、当前位置。第二阶段：实时轨迹、历史入口、按轨迹适配、地图管理删除、进程重启与异常恢复。

最终必须有：真实地图渲染截图；API35 实际导入／缩放／拖动／当前位置／实时与历史轨迹／断段／重启／飞行模式／损坏和不支持格式测试；原 P0–P0.5.1 GPS/BLE/GPX 回归；完整 XML 的 tests/executed/failures/errors/skipped；Debug、Lint、JVM、Instrumentation 编译及实际执行；权限和打包离线审计。缺少测试 XML 或实际截图不能通过。

测试地图使用自行编写、明确 CC0 授权的小型合成 OSM 输入生成，保留生成方法与摘要，不含用户真实坐标或运动记录。依赖与许可证文本一起记录；不引入 INTERNET 权限或轨迹上传。

已读取完整 ledger 与全部9个远程 Release 的 build-info：最高 Version Code 为6777，因此通过 ledger 分配 v0.1.0-p0.6／6778，保留此前全部条目。固定签名继续使用 P0.5.1 证书。完整测试／实际截图／签名安装升级／公开同字节下载分别验收，不用编译成功替代测试，不混入 P0.5.1。

## 上游依据

- https://github.com/mapsforge/mapsforge
- https://github.com/mapsforge/vtm
- https://repo.maven.apache.org/maven2/org/mapsforge/mapsforge-map-android/0.25.0/mapsforge-map-android-0.25.0.pom
- https://repo.maven.apache.org/maven2/org/mapsforge/mapsforge-themes/0.25.0/mapsforge-themes-0.25.0.pom
