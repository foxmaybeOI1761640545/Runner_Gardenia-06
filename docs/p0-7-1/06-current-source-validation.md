# P0.7.1 当前候选源码验收

当前产品候选 `857c744321825a2656d77f45d57c6e43b4334962`，`src/main` tree `3abf6c6f16e0f7893158d56fb10f128b02460e48`。维护分支 `fix/p0-7-1-navigation-backstack`，Draft PR [#6](https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/pull/6)，版本 `v0.1.0-p0.7.1 / 6781`。它取代 01 文档中旧候选的当前签名资格；所有失败和旧候选记录保留，不冒充本次证据。

[Actions 45086993985](https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/actions/runs/45086993985) 全部门禁成功。独立下载完整报告 Artifact `14781383497`（208149455 bytes），验证 ZIP SHA-256 `0bcf13ba58af569e6da905aa527c7e0e1d724c57c0f914753b02b11a6418934d`，解析实际 JUnit XML 并重新运行交付检查。原始 XML、JSON、GPX 和独立复核结果保存在 `evidence/run45086993985-full/`。

| Suite | Tests | Executed | Passed | Failures | Errors | Skipped |
|---|---:|---:|---:|---:|---:|---:|
| Python | 160 | 160 | 160 | 0 | 0 | 0 |
| JVM | 167 | 167 | 167 | 0 | 0 | 0 |
| API 35 full | 489 | 488 | 488 | 0 | 0 | 1 |
| T01–T18 focused | 18 | 18 | 18 | 0 | 0 | 0 |
| GPS/business focused | 47 | 47 | 47 | 0 | 0 | 0 |
| Home-map focused | 2 | 2 | 2 | 0 | 0 | 0 |
| Independent permission processes | 3 | 3 | 3 | 0 | 0 | 0 |

Focused/full 用例重叠，不能累加为不同测试。唯一历史 Skip 是 `TrackRecordingServiceStateMachineTest.recording_stopGPS_noop`；原有 158 个 Java 测试文件保持不变，没有新增 Skip/Ignore 或降低断言。Release assemble、Lint、GPX/离线、数据和地图持久化及新进程恢复均通过。GPS/business 47 项包含先前发现暂停日志回归的两个原有 `P05DashboardStatusNavigationTest` 用例。

T01–T18 全部通过。T07 在全部根节点执行系统 Back；T08 在 API35 启用并确认 gestural mode=2 后执行真实边缘 swipe；内部设置、历史详情、设备选择和删除 DialogFragment 均先处理内部返回。T10/T11 验证快捷入口复用既有一级 Activity；T16 连续两轮暂停、四页切换、恢复并核对同一 workout journal；T17 验证合成 GPS/BLE 样本的服务和持久化连续性。Activity 重建、真实旋转、后台恢复和八配色切换通过。模拟器手势和合成 GPS/BLE 数据不等于实体手机/OEM 动画、卫星或蓝牙射频验收。

完整交付清单的 517 张 PNG 已实际查看：40 张联系表覆盖 480 张，另外 37 张复用同一源码且 SHA 完全一致的 focused 原生截图审查。T16 原图另行打开，确认联系表边缘切分不是应用原图裁切。另审查原有状态导航测试的 idle/recording/paused 三张原图，保存到 `evidence/run45086993985-supplemental-status/`，不将其混入 517 张的严格审批集合。四套主题八配色、字号/尺寸、历史批量操作、设置内部页面、导航弹窗、地图边界、多级真实地图、重建及记录恢复画面未发现本次改动引入的显示异常。UI fixture 中性地图占位不作为真实地图证明；实际 Mapsforge 底图独立审查。

生产修改仅六个文件：共享一级导航、设置/历史根返回、首页和设置快捷入口、记录 ViewModel 重新观察时的既有会话恢复。导航复用暴露的 stale ViewModel 会话问题及其最小修复见 04；未改变 RecordingSession、TrackRecordingService、GPS/BLE owner、SQLite schema 43、地图渲染或主题生产代码。受保护的 210 个核心文件及原有测试字节校验见 source isolation review。

目前通过的是源码自动化、API35 和源码构建原生视觉验收。固定签名 APK 实际运行、P0.7.0 覆盖升级、APK 原生截图、发布、公开下载验证和最终交付报告仍待执行。P0.8.0 尚未建立分支或修改生产代码，只有本版完整交付后才自动开始。实体手机和心率带测试保留 `NEEDS_DEVICE_VALIDATION`。
