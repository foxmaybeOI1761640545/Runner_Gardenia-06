# Runner Gardenia P0.3 原生主题交付报告

已完成四个原生主题家族、八套系统跟随配色的代码与 API 35 验收，并发布固定签名测试 Pre-release。公开 APK 已通过不带 Authorization 的 GitHub 公开资产 API 重新下载，版本、签名与内容摘要均通过独立验证。

- 分支：`feature/theme-system-p0-3`。
- 原始基线：`test-p0-v0.1.0-p0.2` 解引用得到 `4454d716a5ae010b92d3eefa1ac1c0ebedde0063`，未使用 P0.2 分支后续报告 HEAD。
- 实际构建与 Tag 源码：[`77ef053b76fd5e7c6c0c9ce1023e365fa3d7c471`](https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/commit/77ef053b76fd5e7c6c0c9ce1023e365fa3d7c471)。本报告和截图的后续提交不重建 APK。
- 原始构建与完整测试：[Run 42443873771](https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/actions/runs/42443873771)，其中应用测试与签名任务成功；原发布步骤 HTTP 415 失败记录保留。后续创建草稿遭工作流令牌 HTTP 403，改由既有仓库连接创建新草稿；导入作用域错误修复后重新执行完整附件发布校验。
- 同一签名 APK 的重新截图验收：[Run 42466614968](https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/actions/runs/42466614968)，不重建、不重签。
- 发布恢复：[Run 42468308461](https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/actions/runs/42468308461)。
- Release：[test-p0-v0.1.0-p0.3](https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/releases/tag/test-p0-v0.1.0-p0.3)，Pre-release、非 Latest；未覆盖 P0.2。
- APK：[RunnerGardenia-P0-Release.apk](https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/releases/download/test-p0-v0.1.0-p0.3/RunnerGardenia-P0-Release.apk)。
- 包名 `com.fox.runner`，Version Name `v0.1.0-p0.3`，Version Code `6774`，大小 `2799792` 字节。

```text
APK SHA-256: 24225e5a28827eda1fa5799e80230d03464416a3aaa72c0cf34ccb4be090a3ff
Signer SHA-256: E3E7D46BFCB869F5A521A7FBA6FD1BA5790D7DBCAF37F722DDC69D7BB1439A22
```

## 阶段门禁与分支隔离

Phase 1 完成全部七项门禁后自动进入本阶段，原始门禁见 [phase1-final-gate.json](phase1-final-gate.json)，完整网页交付见 [Phase 1 报告](https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/blob/design/p0-ui-lab/ui-lab/docs/THEME_SYSTEM_DELIVERY.md)。网页继续提供十家族、二十套配色、System Preview 与双版本比较；未把整个 UI Lab 合并到 Android，也未执行历史资料归档任务。基线、旧分支和 Tag 的保护记录见 [baseline.json](baseline.json)，最终复查见 [protected-refs.json](acceptance/protected-refs.json)。

## 原生实现与八套映射

使用 Java、XML、ViewBinding、原生 Drawable 和既有 ViewModel / 数据层。设置中只选择家族，带四个原生绘制的结构预览。只持久化家族 ID，默认 Precision Pro；`AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM` 为唯一外观策略。Android 的 `values` / `values-night` 在系统改变时自动切换，无需用户重新选择或重启。

沿用 `AbstractActivity → PreferencesUtils → ThemeUtils` 路径，没有平行的主题管理器。配置重建保留 ViewModel 和页面 ID；从设置返回的 Activity 只在家族改变时重建。GPX 选择使用可恢复 Fragment 对话框。

| 家族 | Light：背景 / 正文 / 强调 | Dark：背景 / 正文 / 强调 |
|---|---|---|
| Precision Pro | `#f3f6f2` / `#1c281e` / `#ccf96b` | `#121614` / `#f2f5ec` / `#ccf96b` |
| Quiet Minimal | `#fafbf9` / `#24352e` / `#3c6a54` | `#111e18` / `#e8f0e8` / `#a0c9ad` |
| Outdoor Explorer | `#f0eee2` / `#233e30` / `#345c3d` | `#121e18` / `#e9eddb` / `#b2cc85` |
| Neon Pulse | `#f4f0fc` / `#29203e` / `#086d70` | `#0c0e1b` / `#f1ecff` / `#70f4ed` |

完整语义角色、状态色、图表色和元数据见 [原生 Token](../../src/main/assets/gardenia-theme-tokens.json)；编译资源见 [values](../../src/main/res/values/gardenia_colors.xml)、[values-night](../../src/main/res/values-night/gardenia_colors.xml) 与 [主题样式](../../src/main/res/values/gardenia_styles.xml)。原生 APK 只有 01–04，05–10 仅保留在网页。

Precision Pro 保留大数字与精密格线；Quiet Minimal 使用开放、弱边框排版；Outdoor Explorer 使用原生地形矢量图、自然色和户外标签；Neon Pulse 以心率和青色原生曲线优先。四个 XML 首页的组件顺序和结构不同，均适配手机滚动区域和固定操作栏。

## 覆盖范围

| 范围 | 实现与验收 |
|---|---|
| 运动首页、GPS 等待、记录、暂停与继续 | 四套独立原生结构；系统切换保留等待、记录、GPS 与连接状态 |
| 心率设备、当前读数、心率曲线 | 既有服务预览的只读有界投影；合成通知经既有处理链驱动原生 ChartView |
| 历史列表与详情 | 原有数据库/读取器；六笔合成通知形成可见曲线，保留断线与暂停间隔 |
| GPX 导出与对话框 | 既有保存/分享动作，原生 Fragment 对话框在系统切换后恢复 |
| 设置、按钮、输入/偏好控件、状态提示 | 全局语义颜色与 Material DayNight 样式；状态同时提供文字 |
| 空、错误、加载状态 | 保留原有业务状态文本并使用当前家族语义色 |
| 系统状态栏、导航栏、手势/IME 区域 | 原有兼容 Insets 机制与系统深浅色图标策略 |

没有使用 WebView；未修改 GPS、BLE、记录服务、数据库、GPX 核心算法。图表改动是语义色、显示空间、单点时间范围与数据到达后的重绘；历史读取继续复用现有实现。

## 实际测试结果

| 验收 | 结果 |
|---|---|
| 原始构建 Python 辅助测试 | 68 执行通过；发布恢复另有 74 项通过 |
| 编译与 Lint、资源与离线 Manifest 审计 | 通过 |
| JVM 测试 | 113 执行通过，0 失败、0 错误 |
| 完整 API 35 Android 套件 | 404 执行通过，0 失败、0 错误；另 1 项原有跳过 |
| 新原生主题测试 | 10 项通过，覆盖四个选择、持久化、八套资源、运行时系统变化、页面/运动/GPS/连接状态、对话框、窄屏与 1.3× 字体 |
| 独立窗口工具自检 | API35 两个独立 shell 进程均生成新的真实 Launcher 窗口、原生 View ID 和 XML；通过后才允许签名 |
| 原生主题截图 | 8 配色 × 7 页 = 56 张，另四张 1.3× 字体窄屏截图，全部真实 API 35 PNG |
| GPX 1.1 XSD | 9 份实际 Android 导出文件通过 |
| 强制停止后的数据库持久化 | 独立写/读进程均通过，PID 不同 |
| 实际签名 APK 安装、GPS、暂停/继续、强制停止恢复 | 通过 |
| P0.2 原位覆盖升级 | 保留历史、GPS 点、原始心率与设备配置，并实际导出 GPX/XSD 通过 |
| 签名包家族选择与系统深浅色 | 八套通过，32 张实际签名包截图，32 项目标 Activity/原生页面 ID/XML 断言，16 项实际像素与 Token 匹配，进程重启后家族保留 |
| 公开 APK | GitHub 公开资产 API 无鉴权 HTTP 200 下载；与正式 Artifact 字节及 SHA-256 相同，签名、版本和包名通过复核 |

原有跳过为 `TrackRecordingServiceStateMachineTest.recording_stopGPS_noop`，没有新增跳过。CI 的 JVM、系统和仪器测试时区使用 Asia/Singapore；生产仍使用手机时区。

原始 [JVM XML](acceptance/jvm-tests/manifest.json)、[测试 JSON](acceptance/tests.json)、[API35 XML](acceptance/api35-tests.xml)、[签名安装收据](acceptance/runtime-smoke.json)、[八配色签名包收据](acceptance/native-theme-smoke.json)、[公开发布收据](acceptance/publication.json) 和 [独立 APK 验证](acceptance/independent-public-apk.json) 均与上述源码/摘要绑定。八个公开附件的独立无鉴权下载结果见 [independent-public-assets.json](acceptance/independent-public-assets.json)，最终发布辅助测试的实际日志摘要见 [publication-helper-tests.json](acceptance/publication-helper-tests.json)。此前失败记录保留在 [failures](failures/)。普通浏览器附件路径在当前出口返回 HTTP 504（P0.2 路径亦然），最终公开下载验收使用 GitHub 公开资产 API，未携带 Authorization；API 与浏览器路径指向同一 Release 资产。原签名截图的视觉复查发现动画与前一页面残留，首次发布恢复被主动取消；最终截图由同一 APK 的后续 API35 重验产生，未使用被拒绝截图。发布只复用已通过原始完整业务测试、后续页面/视觉复核的同一 APK。

## 截图与下载证据

- [八套原生截图索引](previews/README.md)：每套 home、recording、heart、history、detail、dialog、settings 七页原始 PNG 与各自 SHA-256。
- [1.3× 字体、360×640dp 截图](previews/font130/manifest.json)。
- [正式签名 APK 原始截图/XML](previews/signed/manifest.json)，以及 Release 附件 `theme-screenshots.zip`：32 张正式签名 APK 原生截图、对应 XML 与矩阵收据。
- Release 同时保留 APK、build-info.json、SHA256SUMS、test-summary.json、runtime-smoke.json、native-theme-smoke.json 和 RELEASE_NOTES.md。
- [构建信息](acceptance/build-info.json) 和 [最终自动验收门禁](acceptance/phase2-final-gate.json)。

## 尚未完成的实体设备验收

API 35 仿真验收、自动状态/触摸目标和字体检查已通过；`minSdk=26` 与 Lint 兼容性检查通过，未另运行 API 26 模拟器。已提供可读标签、选择状态和非颜色状态文字，但真实 TalkBack 语音/手势操作仍需人工设备验收。H303 实体射频、户外 GNSS 和耗电继续保留原项目的 `NEEDS_DEVICE_VALIDATION` 边界；合成数据测试不替代这些结果。
