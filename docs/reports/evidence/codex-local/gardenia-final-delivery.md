

## 阶段 7 最终验收：PASS

正常合并提交：`270cfa49a1c94d658d16dfc1e84563765aee832f`。最终 Actions：https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/actions/runs/41738430305 。所有 Job 成功；首次 Push 自动完成整条链路。随后只重跑 signed 及后续 Job，第二次运行也成功：实际恢复原 Artifact、独立校验证书/身份、复用原 Tag，未重新构建或上传签名 Artifact，未重新分配版本。

| 验收 | 实际结果 |
| --- | --- |
| Debug / Lint | PASS；Debug Artifact 已下载并核对身份、摘要 |
| API35 Android 测试 | 328 项 / 实际执行 327 / Failures 0 / Errors 0 / Skipped 1（原有 Ignore） |
| JVM 测试 | 52 项 / 全部实际执行 / Failures 0 / Errors 0 / Skipped 0 |
| Python CI 辅助测试 | 20 项 PASS（实际 prepare 日志） |
| 新旧应用测试对比 | 原有 294 项全部保留；新增 34 Android +52 JVM＝86；应用测试共 380 项，实际执行 379 |
| UI | 40 张真实 API35 截图；最终 main 的首页/记录/过期/重连/历史/图表/设置/扫描代表图已目视检查；两种尺寸及亮/暗模式 |
| 测试时区 | Runner、Gradle/JVM worker、Android 系统及仪器进程 Asia/Singapore 实际检查通过 |
| 正式 APK | `com.fox.runner` / `reproducibleRelease` / debuggable=false；apksigner、独立指纹及快捷方式身份检查通过 |
| 版本 | `v0.1.0-rc1.5` / Code `6770`，高于 rc1.4/6769 |
| Tag | 指向同一 main 测试提交；签名 Job 23:32:11 UTC 完成，Tag Job 23:32:14 才开始 |
| 发布 | Pre-release=true、draft=false；Latest stable API 返回 404，新版未设 Latest；三个公开资产已实际重新下载 |
| 恢复幂等性 | Tag 对象 `ee40e121ceb03a9bfacea7bdb8a58fa077a553f3` 不变；Release ID、三个资产 ID/摘要/字节、原签名 Artifact ID 13279344011 均未变 |
| 历史版本保护 | rc1.4 原 Tag 对象与资产保留；rc1.1/2/3 清理后高水位仍受 ledger 保护；下一候选 rc1.6 / 6771 |
| 工作区 | main 与 origin/main 一致且干净，未改写历史、未强推、未提交密钥 |

正式证书 SHA-256（与 rc1.4 一致）：

```text
E3E7D46BFCB869F5A521A7FBA6FD1BA5790D7DBCAF37F722DDC69D7BB1439A22
```

正式 APK SHA-256（签名 Artifact、Tag 注释、公开下载、SHA256SUMS、build-info 和 GitHub asset digest 一致）：

```text
6453f09ce8f538a42abe7a7eb4be52117877bede88f5215c81776157a54f236e
```

- Pre-release：https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/releases/tag/v0.1.0-rc1.5
- 正式 APK：https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/releases/download/v0.1.0-rc1.5/runner-gardenia-v0.1.0-rc1.5-6770.apk
- SHA256SUMS：https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/releases/download/v0.1.0-rc1.5/SHA256SUMS
- build-info：https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/releases/download/v0.1.0-rc1.5/build-info.json
- Debug Artifact：https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/actions/runs/41738430305/artifacts/13280872041
- 完整测试报告/40 张 UI 截图：https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/actions/runs/41738430305/artifacts/13279853488

阶段 7 软件/云端发布验收通过。真实心率带 RF、品牌兼容、功耗及物理设备覆盖升级尚未测试；数据库 v42/v23 兼容测试通过，不能据此把实机升级描述为已执行。硬件清单继续保留这些待测项。
