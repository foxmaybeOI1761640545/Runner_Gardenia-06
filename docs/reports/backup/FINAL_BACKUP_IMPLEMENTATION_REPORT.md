# 项目备份与跨分支治理实施最终报告

## 目标与范围

已在两个获准的现有分支建立 Git 长期归档、来源/SHA-256清单、标准库验证器、可重复恢复流程、轻量CI、五分支目录和Workspace收尾检查。没有创建仓库/分支/Tag/Release，没有合并、强推、Android构建/签名或业务修改。**这不是独立异地灾难恢复备份。**

## 阶段记录

| 阶段 | 实际提交SHA | Actions Run | 已核实结果 |
|---|---|---|---|
| 0–1 | `31bdff76d90722cf59142fff95fd7349f3235897` | 42359725754 | 原始证据恢复、旧归档门禁 |
| 2工具 | `502b37d08c210f1f23ce0fe66bb8e7219a79654e` | 无：有意跳过push CI，后续仅文档提交启动门禁 | skip CI：避免触发 Android；首次 dispatch404已记录 |
| 2云端 | `c787de60025a5cf435131babc062c1c17602e9db` | 42360049929 /42360049863 | 29新测试、17项验证；旧归档通过 |
| 3归档 | `a20f0afe764ca71f9bf269ed25789081bf1c9f70` | 42360254448 | 部署前/部署/线上浏览器全部成功，各1303组 |
| 3来源措辞 | `335da1bbccb5216715e99d15175f34e5aa1dbfec` | 无：有意跳过push CI，后续仅文档提交启动门禁 | 仅澄清33张完全重复图片的永久保存方式；静态/哈希核验，无产品变化 |
| 4工具 | `836eea9522c5803c8ed619ffbb0d50b71cacbaab` | 无：有意跳过push CI，后续仅文档提交启动门禁 | 11项收尾检查测试，完整本地111项通过 |
| 4云端 | `dd65eeb387b018d27f9cf9b4f705d6e30cad98c2` | 42361833567 /42361833544 | 29+11工具测试、18项清单、历史门禁通过 |
| 5边界修复 | `9529021d7c0794d63546f3b57fa2d4911d1804c7` | 无：有意跳过push CI，后续仅文档提交启动门禁 | 补凭据目录拒读/设计Token误判回归；本地113项 |
| 5完整验收 | `1fda133adcf17d3409cd2f0a306f140a26c943db` | 42362105036 /42362105039 | 29+13工具测试、19项、旧归档通过 |

每个云端结果均已读取实际日志/产物，而非仅凭摘要。阶段5源码检查点为9529021，验收检查点为1fda133；本最终报告在这些成功检查之后提交。报告不能包含自己的未来SHA；克隆后执行以下命令获得真实报告提交，并以最终交付消息中的完整SHA/新运行链接核对：

```sh
git log -1 --format=%H -- docs/reports/backup/FINAL_BACKUP_IMPLEMENTATION_REPORT.md
```

## 归档与恢复数量

- 成功恢复**12份原始资料**：2份P0.1合并Manifest、4份历史本地测试回执、5份不同提交快照JSON回执、1份UI Lab原始验收JSON。不可恢复：**0**。
- 本轮另外生成并保存2份真实本地测试原始回执（111项阶段4、113项最终回归），不是历史恢复，不是云端测试输出。
- P0.2增量清单**19项**＝11份恢复原件＋6份不可变来源/基线说明＋2份本轮回执。UI Lab原始JSON和其来源/校验记录留在设计分支，未复制到P0.2。
- 原有66份Artifact manifest、1759个保留成员、1693个去重对象及10份Codex历史原件继续保留。原有2056文件中**2055个字节未变**；唯一改动是明确获准扩展的`docs/reports/README.md`索引。没有搬迁或改写旧证据。
- UI Lab下载包中的**33/33 PNG与原有Git截图完全相同**，只增加新运行来源关系，图片复制0张；两份原provenance未变。五份JSON回执对应不同提交/1821、1822、1823、1825、1873文件节点，非同名推断或重复副本。
- 两份旧工作笔记明确为`HISTORICAL_WORKING_NOTE / NOT_AUTHORITATIVE`，未发现独立权威证据，因此只保存哈希/排除理由，不上传原文，不覆盖当前状态。

## SHA-256与实际恢复

两份Manifest优先从原始Artifact13391716581恢复，ZIP digest与GitHub独立核对，构建源3116e5fa7a56ce6f299ea45d5a45745f1aed4466，Run41974364095；未使用本地副本替换或重新生成XML。

| 文件 | 字节 | SHA-256 |
|---|---:|---|
| debug-manifest.xml |29156|`28041227a94df05d7de5dad09d37b3f405f51e4afaf8bc0455b1f6a31c524461`|
| release-manifest.xml |5576|`c3b0fa84a4eb36aafc82dcf23b2b2be58cd84d82f0b5dbeda70d5443d0936d45`|

所有19项在本地和实际远程Blob字节中重新计算大小/SHA-256。阶段2独立恢复17/17成功，最终检查点独立恢复**19/19成功**。临时副本中故意改动同尺寸字节、以及越界路径，都得到非零退出码；原仓库未被篡改。详细来源：[恢复来源](RECOVERED_SOURCES.json)、[备份清单](BACKUP_INVENTORY.json)、[最终远程快照](FINAL_REMOTE_SNAPSHOT.json)。后续本报告/快照本身的远程字节在推送后独立核验，避免自引用哈希。

## 自动化结果

| 范围 | 实际数量 | Failures / Errors / Skipped |
|---|---:|---|
| 新增清单验证器测试 |29|0 /0 /0|
| 新增Workspace收尾检查测试 |13|0 /0 /0|
| 完整本地辅助测试（含原有71项） |113|0 /0 /0|
| 最终备份CI的新增工具测试（上面集合的子集） |42|0 /0 /0|
| 备份清单完整性 |19项|全部通过|

不可将本地113、阶段111和云端42相加为独立测试数。Cloud日志42362105036确认`Ran29 tests`、`Ran13 tests`和19项无错；42362105039确认原历史归档门禁通过。实际执行了空/坏JSON、缺失/同尺寸损坏、大小/哈希不符、重复键/路径、绝对/Windows/目录穿越/符号链接、缺来源、错误UTC/类型等负面测试。新增收尾检查还验证未推送提交、被忽略证据、凭据目录拒读、设计Token作为资料候选以及远程不可用失败关闭。YAML解析、actionlint、语法、diff与原有回归均通过。

本轮没有执行Android/JVM APK测试或构建；既有运动数据与硬件验收数字仍只是历史证据，不冒充新运行。

## UI Lab长期记录

- GitHub原运行**42354349622**，源码**76e4086c46ca91b8817256806ffb933dde072b19**，三个Job均success。实际读取原Artifact13567361353的59个成员，ZIP digest一致；原始JSON1303组检查、10主题×3视口、0控制台错误、0外部请求。
- [设计分支固定归档](https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/blob/335da1bbccb5216715e99d15175f34e5aa1dbfec/ui-lab/docs/acceptance/2026-11-07-run-42354349622.md)含原JSON、完整Artifact ID/名称/大小/digest/期限、原成员哈希及33张图片对应关系。
- 本次文档推送的既有验证运行**42360254448**，提交a20f0af，frontend/deploy/verify-live全部success；下载前端Artifact13569746249和线上Artifact13570236342，实际JSON分别1303组通过，ZIP digest验证。后续335da1b只修正来源说明措辞/校验文件，产品、原PNG不变，未为此再跑一轮浏览器。
- 设计源码、Tokens、原生实现指南、模拟页面继续留在设计分支。浏览器截图/模拟数据不代表Android原生、H303、真实BLE/GNSS验收。

## 五分支状态与治理

下表是最终报告之前、实际远程验证检查点的HEAD；后续仅本报告提交推进P0.2。实时值使用`git ls-remote --heads origin`，不是伪造本报告自引用SHA。

| 分支 | 核验HEAD | 状态 |
|---|---|---|
| `design/p0-ui-lab` | `335da1bbccb5216715e99d15175f34e5aa1dbfec` | 本次文档/备份维护 |
| `feature/heart-rate-monitoring` | `121ea381fd11111427073fe291dfb5ddabf8b03e` | 受保护，未变 |
| `feature/p0-2-gps-ui` | `1fda133adcf17d3409cd2f0a306f140a26c943db` | 本次文档/备份维护 |
| `feature/p0-core-migration` | `60daeb21aa50bdbceec6e6ad354a431b8c1be434` | 受保护，未变 |
| `main` | `9b5d7b06afc1eb8e3315ba73874ac088fa4d5169` | 受保护，未变 |

[五分支目录](../BRANCH_CATALOG.md)记录真实派生基线、首次独有提交时间（不是伪造创建时间）、Merge Base及分叉关系。UI Lab由3116分叉，不能描述为继承最终P0/P0.2。[治理规范](../BRANCH_GOVERNANCE.md)规定创建、阶段结束、资料归档、合并与长期保留边界。本次没有新建project-records或统一改动.gitignore。

## 使用与维护

```sh
python3 -B tools/verify_backup_inventory.py
python3 -B -m unittest discover -s .github/tests -p test_backup_inventory.py -v
python3 -B -m unittest discover -s .github/tests -p test_workspace_sync.py -v
python3 -B tools/check_workspace_sync.py
```

[恢复说明](RESTORE_AND_VERIFY.md)提供真实单文件/批量恢复命令及失败处理。[Workspace收尾清单](WORKSPACE_SYNC_CHECKLIST.md)覆盖未跟踪、忽略规则、显式仓库外候选、远程HEAD/Blob、来源/哈希、Actions及未完成事项。工具只读，不自动上传、不遍历凭据/其他项目；增量清单可继续扩展，坏文件/来源/路径返回非零。

备份工作流仅P0.2 push/目标PR，contents:read、官方完整SHA固定checkout、无Android SDK/额外Token/签名Secrets。现有Android工作流未改。工具提交用skip CI，紧随的正常docs-only提交启动备份/历史门禁；这是避免触发原有Android流水线的明确操作边界。

## 失败、排除与未解决边界

- 首次新增非默认分支工作流dispatch返回HTTP404，无运行ID；没有记成通过。改用仅文档push注册并执行，之后实际门禁成功。没有新PAT、Secrets变更或main修改。
- 收尾检查曾因本轮生成的未跟踪核验报告返回非零，清洁同步快照重新检查后通过；没有降低门禁。
- **已归档/已验证**：本次12份恢复原件、两份新本地回执、必要来源/清单/说明；旧归档和UI33原图均保留。
- **仅Artifact保存**：历史未选择的HTML/大日志/模拟器输出/trace，以及UI浏览器trace；清单保留其位置/来源，不声称这些大文件已全部进入Git。两份最终P0.1合并Manifest已从此类提升为Git原件。
- **仅本地/明确不归档**：两份非权威工作笔记、下载缓存、拼接审图、重复/临时日志。来源说明保留两份笔记的排除理由。其他项目/个人数据/凭据不在本任务扫描与备份范围。
- **无法恢复**：本次选择范围内0份；没有把未选择资料称为已备份。
- GitHub Release中的APK保持原状，不复制到Git，不重签/重建；五个原Tag对象/目标、五个Release及资产ID/大小/digest、main/心率/P0 HEAD均未变。
- H303实机、RF断连重连、户外GNSS、OEM后台/功耗等仍NEEDS_DEVICE_VALIDATION；本次不扩展功能。
- 仓库内可恢复归档仍依赖GitHub/Git副本可获得性。独立异地灾难恢复备份尚未建立，需要另行授权与方案；不是本轮遗漏的已承诺交付。

最终报告之后的云端门禁和远程字节复核将在交付消息给出实际运行/提交链接。不得为补报告移动Tag或重新构建APK。
