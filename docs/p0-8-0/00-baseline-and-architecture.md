# P0.8.0 第四阶段：基线、架构与实施顺序

采用2026-11-23最新连续任务范围：P0.7.1完整独立交付后，自动开发P0.8.0独立GPX规划路线库、导入/管理、Mapsforge叠加、前台GPS路线跟随、进度/剩余/偏离距离。此前静态预览范围被该最新指令明确扩展；不把P0.7.1维护修复计为阶段4。不包含BRouter/离线路由数据包/自动道路规划/重新规划/转向语音/后台导航系统，偏离容差配置和路书继续留在后续阶段。

## 已验收基线

P0.7.1产品与APK构建源码、Tag解引用均为`857c744321825a2656d77f45d57c6e43b4334962`；注释Tag对象`8f7e34ef9c3c71691fcbb2c18a23629fdfef8ae5`；最终交付HEAD`f2d57b7440dca0350813313ad9e7ef4a12e55fa6`。产品源码是交付HEAD祖先，后续334个差异文件仅文档及两份P071审批，src/main tree均`3abf6c6f16e0f7893158d56fb10f128b02460e48`。

P071版本`v0.1.0-p0.7.1 / 6781`，APK3238755 bytes，SHA-256`1c68922da5052c8b9dc224194e719116ccfafd616905f00d6909b837ebd82759`；固定证书`E3E7D46BFCB869F5A521A7FBA6FD1BA5790D7DBCAF37F722DDC69D7BB1439A22`。源码45086993985、签名/升级45092892094、发布及全新runner匿名校验45096590087全部成功；源码517图、签名99图已实际查看；原有12个Release/105资产及受保护引用不变，PR#5仍Draft未合并，P071 PR#6仍Draft。完整独立报告见docs/p0-7-1/08-final-delivery.md。

新分支`feature/p0-8-0-gpx-route-follow`在P071最终报告推送并验证后，从f2d57b7创建，独立工作树/workspace/work/p080；维护工作树保留。P08重新读取16个远程分支、13个Tag、13个Release及全部build-info二进制并核对摘要；最高分配6781，因此唯一预留`v0.1.0-p0.8.0 / 6782`。证据见evidence/baseline/version-audit.json。Application ID、签名、JDK21/Java17、Wrapper9.8.2、AGP9.5.0、compile/target37、BuildTools36、API35保持。Android构建/模拟器/签名均在Actions，不在本地安装SDK。

## 数据隔离与GPX

现有GPXTrackImporter/TrackImporter写历史Track/TrackPoint；RoutePreviewModel是历史/活动运动的增量只读绘制模型，不能作为路线持久化模型。新增纯Java`p0.routes`独立模型Route/RouteSegment/RoutePoint、流式GpxRouteImporter及后续RouteRepository；它们不引用Android、WorkoutRepository、TrackRecordingService或历史数据库。

每个顶层rte或trk产生独立候选，trkseg明确保留空段和断点，不跨段计距离或连接线。一点有效路线允许预览，长度不可用；两个重复坐标点长度为已知0，重复点连同元数据保留并提示。WPT不作为路线，明确记录忽略数量。全文件读完且XML结构完整后才返回预览；部分无效候选列出原因，坐标无效使整个候选无效，不能丢点后连线。全部无效的文件拒绝，但保留完整读取后的无效项目清单；格式/编码/读取中断不暴露部分成功候选。

namespace-aware SAX处理GPX1.1，兼容无命名空间1.1但给出提示；不把扩展子树误认为路线。每次解析禁用外部实体/外部DTD等可用安全选项，必须安装LexicalHandler并拒绝任何DTD，EntityResolver对全部外部实体抛错。解析器不加载远程XSD或链接，无全局XML属性副作用。Android SAX兼容性必须在API35验证。经纬度严格十进制、有限且范围有效，NaN/Infinity/十六进制/越界拒绝。

保留route/point标准元数据、原名/描述/类型、海拔、literal时间（不为无时区时间虚构UTC）、名字/符号/精度字段及链接文本/类型/href。UUID稳定，本地显示名与GPX原名分开；初始过长显示名截至256字符，原元数据仍完整保留。原文件名缺失显示未知，不伪造。SHA-256覆盖整份原始字节；文件哈希+候选序号用于重复提示，允许显式创建额外UUID副本，不覆盖原路线。

集中预算：16MiB源文件、20,000点（无效点和WPT也计数）、1024段（含空段）、128候选（含无效项）、XML深度32、单字段2048字符、元数据key/value及分隔字符总预算1,048,576。重复元数据仅保留首值并提示，警告列表最多32项；空link也消耗结构预算。采用流式读取与前向哈希，不保留整份原GPX副本，限制多份大源文件重复存储。依据是有界对象/字符串开销和普通路线规模；JVM128MiB堆边界测试已执行，Android实际边界/堆与时间收据仍需验证，不能以JVM冒充Android内存证明。

## 持久化设计（阶段B待实现）

独立files/route-library，routes/<UUID>.route、index.bin、index.mirror、pending/<transaction UUID>/；不使用cacheDir，不与offline-maps混放，不修改schema43。计划采用有界版本化二进制，理由是几何紧凑、编码/长度严格校验、无需新增JSON依赖即可做纯JVM文件系统故障测试；不是复用历史数据库。文件头/版本/长度/UTF8/内容SHA和索引的文件SHA交叉验证。报告与审计导出JSON方便核查；元数据和原始文件名/hash保留，原GPX不重复存储。具体格式与实际故障测试结果一起在阶段B冻结。

两阶段导入：全部解析和选择后，在pending写入并验证选中路线；before/after索引与prepared事务记录同步落盘后，才提升新文件，原子替换正式索引为提交点，最后验证一致性和清理。提交前失败回滚本次UUID，不修改已有路线；提交后仅剩清理失败时按已提交事务恢复。恢复必须先解决pending再允许索引重建，避免将中断导入孤儿文件当成正式路线。删除先将目标文件移入pending退休区，再原子提交索引；提交前恢复文件，提交后清理，防止重建时复活已删除路线。重命名只修改索引别名，路线文件字节不变。镜像/重建修复明确报告异常，不把损坏或未来格式当成功。

## 界面、地图及跟随设计（阶段C～E待实现）

四个一级页面保留，Home共享RouteMapView和设置分类提供最小二级入口，新增路线库/详情/地图二级Activity，遵循实际调用者正常Back，不改P071共享一级导航。SAF OpenDocument使用通用MIME和ContentResolver流，无真实路径/长期URI依赖；后台解析、进度、有效/无效/重复候选预览、多选后事务导入；取消不留正式数据。列表/详情支持空、加载、错误、重命名、确认删除和重新打开，适配现有四主题明暗。

复用OfflineMapView/Renderer/MapFile/cache，以及RouteGeometry/MapRouteSegments/MapViewport几何绘制能力；规划、已记录轨迹和真实GPS各自独立图层。静态路线详情预览仅绑定规划几何，不创建GPS owner或运动会话，缺地图/部分/无覆盖明确提示。现有全局前台GPS策略在任意前台页面可能已工作；静态页面增加零定位请求，不能将原全局owner冒充成静态页面的新定位，也不能谎称整个前台应用无GPS。多段不连接，端点/命名途经点有界绘制，拖动、缩放、视口保存和资源销毁沿用一个Mapsforge实例。

跟随控制器只读观察既有TrackRecordingService真实fix和状态，不注册LocationListener、不start服务、不申请GPS owner、不发运动开始/暂停/结束命令。跨四个一级页面保持同一前台跟随状态，全部退后台暂停UI观察；正式运动的后台记录继续独立运行。状态NONE/SELECTED/FOLLOWING/PAUSED/ENDED，进程恢复将先前FOLLOWING明确恢复为PAUSED，不悄悄创建后台导航。结束跟随不结束运动，结束运动不删除路线。

路线距离为段内great-circle边长之和；匹配使用点到球面线段的投影而非最近顶点。全局最近距离用于真实偏离，连续候选窗口/时间与精度/滞回限制用于稳定进度，处理交叉、环线、跳点、过期/低精度/丢失定位；无法可靠判定时保留稳定进度并提示，不虚构点、不吸附改写原GPS。不加入路由、语音或重新规划。

## 文件计划和门禁

A：新增p0/routes纯模型/解析/距离，43项JVM与8项API35平台测试；B：新增独立Repository/codec/事务/恢复与故障测试；C：SAF导入与候选预览；D：列表/详情/重命名/删除及二级返回；E：共享地图叠加、静态预览、只读跟随控制器/线段匹配/进度与偏离；F：原有完整回归、数据库/偏好/地图快照隔离、实际进程中断恢复、原生截图审查、P071覆盖升级、原固定JKS、唯一Tag/Pre-release及匿名校验。同一阶段通过对应源码证据/报告后才开始下一阶段。

.github/p08、p08-stage.yml和p08_*脚本独立，不更改p07或p071的阶段/审批/历史收据；保留全部旧Java测试字节。P071版本reader仅改为读取自己那条历史预留行并验证完整台账，使追加P08分配后仍精确返回6781，原160项Python断言全部保留且通过。旧rc工作流按精确P08 PR分支交给独立门禁，避免错误版本分配；不跳过独立完整测试。GPS/BLE/运动服务/数据库/既有GPX和P0Navigation是受保护生产路径，需要改动时必须先报告最小替代。

当前仅阶段A模型/解析及测试代码已实现；Repository、界面、地图叠加、GPS跟随、完整回归与P08发布均未完成。阶段A Actions尚待执行，初始所有发布审批关闭。实体手机验证独立列为NEEDS_DEVICE_VALIDATION。
