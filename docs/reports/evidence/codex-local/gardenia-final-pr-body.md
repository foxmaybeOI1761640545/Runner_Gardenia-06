## 实现结果

复用现有 BLE Driver、记录服务、SQLite 与 ChartView，补齐标准心率协议解析、可靠订阅/断连重连、原始样本保存、实时状态和历史曲线；没有另建 BLE 服务、数据库或云端上传模块。正式包名和固定签名保持不变。

- 0x2A37 支持 UINT8/UINT16 BPM、接触状态、Energy、精确 RR（1/1024 秒）和非法包检查；0x180D/0x2A38/可选电量沿用现有驱动。
- 扫描有权限/代次/15 秒超时和取消保护；记住手动选择，展示名称/RSSI，提供扫描、连接、断开、重连。GATT 等待 CCCD 确认、30 秒握手超时、1/3/5/10 秒重连间隔，主动断开取消重连，旧回调不能重启已关闭连接。
- 同一数据库新增 heart_rate_samples（schema 42→43），保存轨迹关联、时间、样本/连接身份、设备/来源及可选 RR。去重以通知身份为准；不改变 GPS 距离/10 秒 TrackPoint 筛选。暂停、停止和预览不写入运动样本；断连不结束运动。v42 历史及既有 v23 升级链已测试。
- 记录页及现有传感器设置显示 BPM、连接/接触/新鲜度和最近更新时间；现有时间/距离图表显示完整样本统计、限量绘制及断连间隔。保留导航、主题和其他图表；原始隐私数据仅本地保存。

## 工作流与历史清理

`prepare → checks → signed/APK verification + artifact → create-tag → publish`。签名失败不打 Tag。Tag 绑定准确测试提交、版本计划和 APK SHA-256；恢复原签名 Artifact 后再次独立校验证书和身份，拒绝改动字节。只有签名 Job 访问 android-release，只有 Tag/发布 Job 有 contents:write，未新增 PAT/密钥/权限。

已先提交并远程核对 release-ledger.json，再删除确认无签名 Artifact/Release 的 rc1.1/rc1.2/rc1.3；rc1.4 的原 Tag 对象、正式 APK 和 Release 均保留。高水位 4/6769 不随清理下降；下一候选 rc1.5/6770。详见 [清理审计](docs/RELEASE_TAG_AUDIT.md)。

## 分阶段验收

| 阶段 | 实际通过的 PR CI |
| --- | --- |
| 0 架构审查 | https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/actions/runs/41691689014 |
| 1 BLE/解析 | https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/actions/runs/41699651106 |
| 2 原始样本/兼容 | https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/actions/runs/41701825550 |
| 3 UI/图表/截图 | https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/actions/runs/41730815364 |
| 4 完整回归 | https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/actions/runs/41733224958 |
| 5 先签名后打 Tag | https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/actions/runs/41735349725 |
| 6 Ledger/孤立 Tag 清理 | https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/actions/runs/41737068350 |

最后一轮对应 feature SHA `121ea381fd11111427073fe291dfb5ddabf8b03e`：Debug/Lint、328 项 Android（327 执行、1 原有 Ignore）、52 项实际 JVM 测试均通过，Failures/Errors 全为 0；20 项 Python 门禁/分配/恢复测试通过。下载 XML 按测试标识比对：原有 294 项 Android 全保留，新增 34 项 Android +52 项 JVM；未把 NO-SOURCE 算作测试。

实际下载 Debug APK SHA-256：`48058638fde38fca14600d97d1656b496fea8c5ead4720dd3b11e712edb4c63a`，身份 com.fox.runner.debug / reproducibleDebug / v0.1.0-rc1.5 / 6770。40 张 API35 实际截图覆盖 411dp/360dp、亮/暗模式；[UI 检查与代表截图](docs/HEART_RATE_UI_REVIEW.md)。SGT 检查覆盖 Runner、Gradle JVM/测试进程及模拟器系统/仪器测试。

本地 actionlint、Python 语法/20 项脚本测试、Gradle 9.8.2 help、diff/签名文件检查均通过；Cloud 未安装 Android SDK/模拟器，未升级 Wrapper/AGP/JDK/核心依赖，仅增加测试用 JUnit。

## 边界及最终发布门禁

没有真实心率带：协议/状态测试使用可控替身，UI 使用明确测试数据。虚拟蓝牙离线页经过实际运行，设备行扫描展示为标明的 fixture。真实 RF、不同品牌兼容性、长时间功耗、物理安装覆盖升级仍需 [硬件清单](docs/HEART_RATE_BLE_TEST_CHECKLIST.md)，不能据此声称实机验收。

阶段 0–6 已通过，可正常合并。最终 main 必须再次完成完整测试、正式签名/身份/证书/摘要验证，随后自动打 Tag 并公开完整 Pre-release；这些结果将在 Actions 与最终交付报告中逐项核对，不以 PR 成功代替正式发布验收。
