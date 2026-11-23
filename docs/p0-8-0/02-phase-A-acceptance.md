# 阶段A验收：通过

产品候选`b00bd2538a710368b93daaf2f9f96f99fc0b36a7`的独立[Actions45116960695](https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/actions/runs/45116960695)成功，Debug、Lint、完整JVM/原生测试编译及Python回归完成，API35实际执行解析专项。下载Artifact14792470939，验证510053 bytes及SHA-256`6d567bf876b297d3f6550ced3a7b8c3a603a547e274c461a392f181b0dbee34e`，安全解包后独立解析原始JUnit XML并与门禁收据逐项比对。

| Suite | Tests | Executed | Passed | Failures | Errors | Skipped |
|---|---:|---:|---:|---:|---:|---:|
| Python全部 | 165 | 165 | 165 | 0 | 0 | 0 |
| JVM全部（原167+新43） | 210 | 210 | 210 | 0 | 0 | 0 |
| API35解析focused | 8 | 8 | 8 | 0 | 0 | 0 |

43项新JVM覆盖rte/trk、多路线/多段、不连接断点、标准元数据/文件SHA、WPT隔离、单点/重复点、无效坐标/海拔、空/格式/编码/读取/中断、命名空间、DTD/实体、schemaLocation不下载、实际各默认限制上下界、Unicode显示名及不可变/重命名模型。原有Java测试字节保持，Python预留兼容与独立门禁测试通过，无新增Ignore/Skip。

API35实际Android SAX通过前缀命名空间、多rte/trkseg、内部/外部/参数DTD拒绝、UTF16中文及literal时间、部分无效候选与全文件格式失败、取消读取、WPT/单点处理，并执行16MiB与20,000点实际边界。20,000点测试含越界拒绝用时354ms，之后Java堆使用18,842,624 bytes；16MiB测试含越界拒绝用时1833ms，之后Java堆使用137,176,064 bytes；设备max Java heap201,326,592 bytes（192MiB）。这是包含测试在内存构造输入的时间/当前堆快照，不是采样的进程或解析峰值；后续真实SAF流式导入还需单独验证内存与取消体验。预算在此作为已验证的阶段A默认值，不提高以掩盖失败。

原始XML/JSON、平台测量、源码计划与独立复核在evidence/phase-A。当前没有UI生产变更，视觉审查不适用；没有伪造截图通过。01与本地attempt记录继续保留早期编码分类失败及修正。本阶段不代表原有全部API35/UI、Route Repository、路线库、地图/跟随或签名升级/发布已通过。

阶段A数据模型与GPX解析验收通过。此报告提交并推送、核实远程HEAD后才进入阶段B独立Route Repository，之后仍按A～F逐项执行，不触碰P071既有发布资产。
