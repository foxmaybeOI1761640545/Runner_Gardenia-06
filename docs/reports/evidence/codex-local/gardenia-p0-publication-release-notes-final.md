## Runner Gardenia P0 测试版本

本版本为测试 Pre-release，仍需真实手机和 Magene H303 验收。复用已完整验收的原始 APK，没有重新构建、修改或重新签名。

### 版本信息

- Version Name：`v0.1.0-p0.1`
- Version Code：`6772`
- Application ID：`com.fox.runner`
- Build Variant：`reproducibleRelease`，`debuggable=false`
- APK：`RunnerGardenia-P0-Release.apk`
- 大小：2,715,993 字节，约 2.59 MiB
- APK SHA-256：`94d3322310e8b6050504eb4c8d8c72fa09f0190f8add35ae0100b9bc93cc6ab0`
- 固定证书 SHA-256：`E3E7D46BFCB869F5A521A7FBA6FD1BA5790D7DBCAF37F722DDC69D7BB1439A22`
- Tag：`test-p0-v0.1.0-p0.1`
- 实际构建和 Tag 提交：`3116e5fa7a56ce6f299ea45d5a45745f1aed4466`
- 文档快照：`e8973de980398f9f64f72eae510a789b86338356`
- 原始 Actions： https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/actions/runs/41974364095
- 原始 Artifact：`13391972069`

### 自动化验收

| 测试 | 总数 | 执行 | 失败 | 错误 | 跳过 |
| --- | ---: | ---: | ---: | ---: | ---: |
| Android API 35 | 379 | 378 | 0 | 0 | 1 |
| JVM | 88 | 88 | 0 | 0 | 0 |
| 工作流辅助测试 | 39 | 39 | 0 | 0 | 0 |

- Debug 构建、Lint 通过。
- 两进程文件数据库写入/强制停止/重新读取恢复验证通过。
- 9 份实际 Android GPX 文件通过官方 GPX 1.1 Schema 验证。
- R8、资源压缩、固定签名及独立证书比对、APK 对齐检查通过。
- 实际 API35 模拟器覆盖安装、合成旧 GPS/原始心率数据保留、schema43、GPS 记录、暂停恢复和正式 APK 的 GPX 导出通过。
- 实际 APK 离线 Manifest/权限及私有 FileProvider 审计通过。
- 发布前重新下载原始 Artifact，核对大小/哈希/包名/版本/不可调试属性，并使用 Android apksig 9.5.0 独立验证签名与固定证书。

### 真实设备验证：NEEDS_DEVICE_VALIDATION

以下项目尚未完成真实设备验收：

- Magene H303 实际连接与真实 BPM 数据。
- 真实射频断连和重连。
- 户外 GPS 精度和实际采样频率。
- 熄屏/锁屏后台运行和长时间稳定性。
- 耗电情况。
- 小米及其他机型、不同 Android 版本的实际权限表现。

模拟器、测试替身和合成旧数据不能代替真实设备验收。请按照附件 P0_DEVICE_CHECKLIST.md 执行。

### 安装与附件

请下载 `RunnerGardenia-P0-Release.apk`，根据 `SHA256SUMS.txt` 核对哈希，再覆盖安装；不要卸载或清除原有应用数据。正式私钥和密码不在附件中。

附件 `P0_DELIVERY_REPORT.md` 和 `P0_DEVICE_CHECKLIST.md` 是 e8973de 的原始验收快照；报告中“未创建 Tag/Release”描述的是此前 Artifact 交付阶段，本页面是其后的独立测试发布。后续技术审查结果单独记录，不能改变这份 APK 的构建来源或测试事实。


### 最终技术审查补充

本次发布后完成了真实报告、时区语义、实际DEX/资源、升级、binary Manifest/备份、权限/生命周期和P0 UI/功能边界审查。生产源码与原始构建提交一致，没有重建APK或更换签名。

实际与同变体旧版6771相比：APK11,007,368 ->2,715,993字节，减少75.326%；DEX类14,838 ->1,519。注意：仍有42个Health相关类及部分旧导出器/驱动代码残留；没有相应P0入口/权限或设备激活路径，不能宣称所有非P0代码已经彻底删除。

唯一原有Ignore保留并在审查报告中说明其内部hard-stop接口TODO背景；未删测试、降断言或把模拟器结果当成真机结果。真实H303/RF、户外GNSS、锁屏后台、耗电、小米/其他机型仍为NEEDS_DEVICE_VALIDATION。

- [最终技术审查报告（提交 60daeb2）](https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/blob/60daeb21aa50bdbceec6e6ad354a431b8c1be434/docs/P0_FINAL_TECHNICAL_REVIEW.md)
- [更新后的真机验收清单](https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/blob/60daeb21aa50bdbceec6e6ad354a431b8c1be434/docs/P0_DEVICE_CHECKLIST.md)
- [机器可读审查证据](https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/blob/60daeb21aa50bdbceec6e6ad354a431b8c1be434/docs/p0-evidence/p0-final-review.json)

原有6个附件作为原始构建验收快照保持不变；上述报告及清单补充此次收尾审查。
