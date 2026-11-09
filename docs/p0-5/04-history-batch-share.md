# Phase 4：历史多选与批量分享

状态：IN_PROGRESS。Phase3源码f0d3080/Actions42601932032通过，72张真实原生截图人工审查及收据验收提交7ad403a已推送并核对远端。

范围：原P0HistoryActivity/ListView增加稳定long ID多选、可见勾选、独立五操作栏、已选数量/全选当前加载/取消/返回；原详情、曲线和单文件入口保留。选择与SAF快照随Activity保存，确认/分享选项用DialogFragment固定ID列表跨重建。设置的历史分类明确进入管理模式。P0HistoryViewModel沿用单一后台worker，查询generation与操作token独立，旋转/刷新不丢导出结果；错误信息保留在页面，不仅显示短暂Toast。

P0GpxFiles扩展批量暂存：先检查每条存在且不受保护，复用原P0GpxExporter逐记录写GPX，唯一目录/稳定ID文件名，按需ZIP，全部完成并再验证后才公开内容URI。任何失败清理不完整暂存，不会静默少一条。Share用标准SEND/SEND_MULTIPLE、ClipData和READ grant；多选默认ZIP且保留直接GPX选项。文件生成/ZIP/批量读取删除均后台；批量文件API拒绝主线程。临时pending目录进程中断后清理，已分享文件保留7天供接收方读取；同进程活跃暂存不会误清理。删除整组选中项预检，再逐条复检与验证结果；非事务性失败显示已删除/所选总数及失败记录，刷新后保留未删选择，不自动继续删除。

实际门禁待执行：原生长按/切换/55条分页加载全选/重建/普通详情；单GPX、多GPX及ZIP实际Sharesheet Intent和取消；独立测试APK UID接收全部文件/原始心率/ZIP内容与权限负向对照；无效/缺失/受保护记录与文件中断、孤儿缓存及TTL；SAF快照不随随后选择变化、失败保留源记录；删除数量/二次确认取消/跨重建/删除级联；八配色360dp/1.3字体列表、多选、分享、删除32张截图与人工审查；原历史/数据库/GPX回归及所有之前选定回归；所有实际GPX经固定GPX1.1 XSD验证；真实write→force-stop→read新PID清理不完整缓存且保留已完成分享文件。未执行前不计作通过。

模拟接收应用在测试APK的独立进程与UID，使用Binder调用身份核验，不把目标应用自行读取URI冒充跨应用权限。负向对照使用测试专用action避免Android自动把SEND的EXTRA_STREAM迁移为读权限；正向仍发送真实产品Intent。无微信AppID、不引入SDK/硬编码Activity。微信客户端兼容性始终NEEDS_DEVICE_VALIDATION。

## 首轮编译失败：5a7b99f / 42652881294

Android测试APK编译失败，未执行原生验收，未产生可用截图；不能计作测试通过。11条Java错误均在新增P05HistoryBatchTest：Intent重载配合var推断为ActivityScenario<Activity>，以及通配lifecycle/atomic导入令AtomicReference歧义。修复为显式ActivityScenario<P0HistoryActivity>及明确lifecycle导入；保留全部测试及断言。原始编译错误存于evidence/phase4-failed-42652881294，修复后重新执行完整本阶段门禁。

## 第二轮Lint失败：c85ef46 / 42653242697

Java编译已通过，Lint发现新增错误提示中的记录ID以Long传给%s格式（中英文两处StringFormatMatches错误）。修复为显式转换String，保留可显示“—”的未知ID提示，不关闭Lint或降低检查。原生测试尚未执行，截图/GPX缺失门禁按预期失败，未计作通过。失败日志存于evidence/phase4-failed-42653242697，重新执行全部检查。

## 首轮实际原生结果：7d127b7 / 42653679266

Android Tests78 / Executed78 / Failures2 / Errors0 / Skipped0；JVM124 / 124 / 0 / 0 / 0；辅助测试87 / 87 / 0 / 0 / 0。两处失败均为新增测试中Files.writeString在API35不存在（NoSuchMethodError），发生在跨UID接收收据保存和孤儿缓存测试。多选/分页/删除确认/保护/分享Intent/SAF快照/刷新重建/八配色视觉及原历史回归已执行通过；独立进程缓存probe因原生失败未执行，不计作通过。完整跨UID正向三格式尚未通过，不宣称文件分享验收完成。

修正测试写入/读取为兼容的字节接口；另经Android15实际DOM工厂源码核对，移除测试接收器不支持的Apache配置选项，保持命名空间/版本/ID/原始心率/逐文件SHA和固定GPX1.1 XSD断言。新增收据在断言前保存原始内容、独立归档原始GPX，收据门禁核对每份GPX与原记录完整ID/SHA并加入7项篡改负向测试（本地94项通过）。独立收集各矩阵并保持任何失败为非零，避免某份收据缺失阻止其他成功截图归档。改正ZIP Sharesheet标题为通用“分享”；预检数据库异常也携带实际记录ID。

实际32张历史列表/多选/分享选项/删除确认截图已逐组人工审查，四主题八配色360dp/1.3字体无裁字或横向溢出；原图/4张联系表/原始JUnit/失败详情与Artifact摘要归档于evidence/phase4-failed-42653679266。本候选整体失败，不能进入Phase5；全部修正须精确新源码重新执行。
