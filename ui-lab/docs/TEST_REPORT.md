# 验收与部署记录

## 本轮增强：主副指标双向位置交换

- 分支：`design/p0-ui-lab`，长期保留；同步起点为实际远端最新 HEAD `0de2481a85eeee192ea0f203429bf086865ba681`。工作区干净，以 fast-forward 同步，未从旧 df900b1 重置。
- 功能源码提交：`6fd9c73a5b90109db757bfbe0299729ef8df6759`。完整发布与正式站点验收：[Actions 42353043971](https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/actions/runs/42353043971)，frontend / deploy / verify-live 全部 success。
- 正式访问地址：https://foxmaybeoi1761640545.github.io/Runner_Gardenia-06/ 。部署返回的该地址实际 HTTP 200，线上 Chrome 直接访问此地址并测试新交换按钮与全部原有功能。
- 本报告与截图的归档提交同样触发原有三阶段工作流。为避免提交 SHA 自引用，最终 HEAD 的完整 SHA 与运行链接以正式站点自动生成的 [BROWSER_ACCEPTANCE_RESULTS.json](https://foxmaybeoi1761640545.github.io/Runner_Gardenia-06/docs/BROWSER_ACCEPTANCE_RESULTS.json) 的 `sourceCommit` / `actionsRun` 为准；最终交付消息也提供完整提交链接。本仓库 JSON 是上述已通过运行的固定归档。

### 实际修改的文件

`app.js`、`styles.css`、`index.html`、`tests/browser_acceptance.py`、新增 `tests/metric_swap_acceptance.py` / `tests/build_ci_font.py`、测试专用字体子集和码点表、`docs/METRIC_SWAP_SPEC.md`、`docs/NATIVE_UI_IMPLEMENTATION_GUIDE.md`、本报告及 `docs/BROWSER_ACCEPTANCE_RESULTS.json`；`previews/` 更新 13 张实际默认/整页截图，另归档 20 张交换/长时长截图及其来源清单。专用 `.github/workflows/p0-ui-lab-pages.yml` 仅为紧凑 Artifact 增加交换截图路径，原检查、权限和发布任务均保留。

### 实现和逐主题验收

统一的 `metricLayoutsByTheme` 保存每套主题的主指标和有序副指标。每次仅交换主位置与被点击的副位置，其他槽位不动；GPS 精度固定在第四副位置。模型独立于模拟运动状态，切换主题、页面或重置模拟记录均保留选择。三个核心副指标为原生 button，支持鼠标、触屏、Tab/Enter/Space 和可见焦点；交换后焦点保持原副位置。提示位于普通文档流，不遮挡指标。

主副数据统一使用 `metricData(motion())`，每秒仅更新现有文本节点，不重建整页。保留十套原 hero 容器与地图、仪表、霓虹、编辑、玻璃等视觉。Neon 默认心率主显示，副区为距离、时长、配速和精度；辅助波形明确标注“心率曲线 · BPM”。

| 主题 | 默认主指标（已核对源码） | 三视口交换、位置不变与唯一性 | 状态、格式、键盘和实时更新 |
|---|---|---|---|
| 01 Precision Pro | 运动距离 | 通过 | 通过 |
| 02 Quiet Minimal | 运动距离 | 通过 | 通过 |
| 03 Outdoor Explorer | 运动距离 | 通过 | 通过 |
| 04 Neon Pulse | 实时心率 | 通过 | 通过 |
| 05 Swiss Editorial | 运动距离 | 通过 | 通过 |
| 06 Industrial Gauge | 运动距离 | 通过 | 通过 |
| 07 Calm Wellness | 运动距离 | 通过 | 通过 |
| 08 Aurora Glass | 运动距离 | 通过 | 通过 |
| 09 Racing Performance | 运动距离 | 通过 | 通过 |
| 10 Data Brutalism | 运动距离 | 通过 | 通过 |

### 实际浏览器结果

Chrome `156.0.8078.79` / Playwright `1.62.0`，发布前和正式 HTTPS 站点均通过 **1303 组检查**（每组可能包含多项断言）。控制台错误 **0**，外部网络请求 **0**。没有删除原检查、跳过失败项或放宽断言。本地另外完成指标专项 122 组与十主题真实 Chromium 触屏输入检查。

| 验收内容 | 实测结果 |
|---|---|
| 1440×1000 / 1366×768 / 390×844 | 每种尺寸全部 10 主题通过，主数据、单位、副区、导航、手机比例与轮播无溢出或遮挡 |
| 连续真实位置互换 | 四种核心指标均可成为主显示；旧主指标进入被点击槽位，其他槽位完全不变，无重复或丢失 |
| 会话保留 | 主题 01 / 02 独立修改再返回、历史/心率/设置返回运动均通过 |
| GPS 搜星 / 丢失 | 距离、配速/速度显示 --，精度 --；主副一致，无虚构有效数值；恢复与轨迹缺口回归通过 |
| BLE 未连接 / 过期 / 已连接 | 无效时主副均 -- BPM，连接后有效值；保留扫描、手动重连等旧流程 |
| 记录 / 暂停 / 恢复 / 结束 | 暂停距离、时长冻结，配速/速度 --，交换不改变记录逻辑；原生命周期通过 |
| 公里 / 英里 | 距离两位小数，基准 5.24 km / 3.26 mi，移动位置后格式相同 |
| 配速 / 速度 | 5′27″/km、8′46″/mi、11.0 km/h、6.8 mph；原模拟速率对应 11.0，未改造记录模型 |
| 时长 | mm:ss 与超过一小时 01:25:30；用原模拟 ticker 实际推进，全部主题与尺寸完整显示 |
| 键盘 / 触屏 | 三个副指标均为可访问按钮，Tab/Enter/Space 和焦点恢复通过；全部 10 主题实际 touchscreen tap 通过 |
| 既有功能回归 | GPS、BLE、历史/详情、曲线、GPX 下载/失败/重试/分享回退、设置、轮播、导航和减少动态效果全部保留并通过 |
| 完全离线 | 实际 Chromium file:// 且 networking disabled，原离线打开与导航检查通过 |
| 线上新功能 | 使用正式 URL 运行同一套交换、状态、格式和回归检查，通过 |

### 截图、来源与隔离

[默认与整页截图](../previews/README.md)共 13 张，[交换和长时长截图](../previews/metric-swaps/README.md)共 20 张，全部来自上述正式 Pages 浏览器运行。已取回审查；来源提交、运行、图像尺寸和每张 SHA-256 见各自 PROVENANCE.json。下载包 Artifact `13565999440` 的 SHA-256 为 `195ae6160ae59b58445d9e6e6409f6b9d90f030f1bf33fa3b827130eb063e320`，已与 GitHub 返回 digest 核对一致。完整结果见 [BROWSER_ACCEPTANCE_RESULTS.json](BROWSER_ACCEPTANCE_RESULTS.json)，trace/下载 GPX 在该 Actions Artifact 保留 30 天，截图已长期归档于分支。

Android 业务、GPS/BLE 原生逻辑、Gradle、Manifest、数据库、签名/Android 工作流、Release、main 和其他业务分支均未修改。不引入 WebView，Java + XML + ViewBinding 原生路线不变；原生指南补充了未来指标位置交换的 UI 状态约定。HTML 浏览器触屏模拟不等同于 Android 真机验收。

实现与交互细节见 [METRIC_SWAP_SPEC.md](METRIC_SWAP_SPEC.md)。下文保留原 UI Lab 的历史验收与首次发布记录。


日期：2026-11-07，Asia/Shanghai。设计分支长期保留，不合并 main。

**正式访问地址：https://foxmaybeoi1761640545.github.io/Runner_Gardenia-06/**

首次正式部署已成功，实际 HTTPS 请求返回 HTTP 200。GitHub 返回的 `environment_url` 与上述地址一致。发布权限阻塞已解除，后续发布增加独立的线上 Chrome 全量验收任务。

## 交付与基线

- 分支：`design/p0-ui-lab`。
- 已 fetch 并核实 annotated tag `test-p0-v0.1.0-p0.1`。
- Tag 对象：`c3af195d4d99eb468dc09420bee43336b295bbd2`。
- 基线提交：`3116e5fa7a56ce6f299ea45d5a45745f1aed4466`，与指定值一致。
- 本次归档的 UI 实测提交：`6910416c2125fbdf8bf0719db00759062c7944a7`。
- 实测运行：[42269278260](https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/actions/runs/42269278260)。该历史运行的 **Frontend and real-browser acceptance 成功**，当时 Pages 尚未开启；正式部署的后续成功记录见下文。
- 最后补齐截图资源与来源说明的提交可从 [分支 HEAD](https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/tree/design/p0-ui-lab) 查看。每次 push 均重新进行完整检查，后续实测 JSON 以 [BROWSER_ACCEPTANCE_RESULTS.json](BROWSER_ACCEPTANCE_RESULTS.json) 与对应 Actions 为准。

## 十套方案

01 Precision Pro、02 Quiet Minimal、03 Outdoor Explorer、04 Neon Pulse、05 Swiss Editorial、06 Industrial Gauge、07 Calm Wellness、08 Aurora Glass、09 Racing Performance、10 Data Brutalism 均实现完整页面与共享状态模型。布局、排版、卡片、按钮、图表风格和导航造型有实际区别，不只是换色。

## 实际浏览器结果

Chrome `156.0.8078.79`，Playwright `1.62.0`，GitHub 托管 Ubuntu runner。30 个主题/视口组合通过；归档实测 **159 组检查通过**。这是一组包含多项断言的验收分组数量，不是页面数量。

| 视口 | 主题 | 页面/状态 | 横向溢出、手机比例与轮播遮挡 |
|---|---|---|---|
| 1440×1000 | 全部 10 套 | 通过 | 通过 |
| 1366×768 | 全部 10 套 | 通过 | 通过 |
| 390×844 | 全部 10 套 | 通过 | 通过 |

| 验收项目 | 实测结果 |
|---|---|
| 十套切换、选中、循环、上一套/下一套与键盘左右 | 通过 |
| 自动播放 10 → 01、操作后暂停、悬停/聚焦暂停、恢复播放 | 通过 |
| 触摸横扫事件、鼠标实际拖动轮播 | 通过 |
| 减少动态效果 | 通过，关闭自动播放 |
| 四个主导航 | 每个主题/尺寸通过 |
| GPS 搜星 / 有效定位 / 丢失 / 记录 / 主动暂停 | 通过，未定位时无坐标、指标占位、开始不可用 |
| GPS 恢复 | 通过，距离不回填；SVG 轨迹保留分段缺口 |
| 记录开始、暂停、恢复、结束 | 通过，暂停数值不增长，结束可进入模拟摘要 |
| BLE 未连接、扫描、连接中、已连接、重连、过期、接触不良 | 通过，无有效新鲜数据时 BPM 为 `--` |
| 手动重连、设备选择、RSSI、主动断开 | 通过，手动重连先清除旧 BPM 再模拟收到新数据 |
| 历史空列表 / 多条 / 进入详情 / 返回 | 通过 |
| 详情距离、时长、速度、平均/最高/最低心率、轨迹与心率曲线 | 通过 |
| 心率详情返回父页，切换主题保留当前页 | 通过 |
| GPX 模态框 / 失败 / 重试 / 成功 | 通过，实际下载 GPX 并以 XML 解析验证，包含两个轨迹分段 |
| GPX 保存/分享回退 | 通过，headless Linux 不支持系统文件分享时实际执行下载回退 |
| 模态 Tab 焦点循环、Escape、关闭恢复焦点 | 通过 |
| 设置英里/公里、速度/配速、自动重连跨主题保留 | 通过 |
| 中文字形、中文标点、内容宽度 | 实际平台字体检查 + 字形覆盖静态检查 + 截图审查；已修复缺字体和装饰溢出 |
| JS 控制台错误 | 0 |
| 页面外部网络请求 | 0；无 CDN、在线字体、外部图片或地图 |
| 断网直接打开 `index.html` | 实际 Chromium `file://` + networking disabled，页面、导航和主题操作通过 |
| 原生迁移规范 | 完整组件映射、实际源码路径、10 套 Token、字体/间距/圆角/阴影/图表/状态/交互规范已交付 |
| GitHub Actions | 前端验收与 Pages 部署已通过；后续增加 verify-live 正式站点全量验收 |
| GitHub Pages 实际访问 | **已完成首次部署：GitHub deployment success，正式 URL 实际 HTTPS 请求为 HTTP 200，页面标题与 app.js 入口正确** |

实际截图在 [previews](../previews/README.md)，来源、尺寸和哈希见 [PROVENANCE.json](../previews/PROVENANCE.json)。完整 trace、下载的示例 GPX、结果 JSON 在 Actions 验收 Artifact，保留 30 天。已下载的紧凑截图包 SHA-256：`01155efee3855f83059f5c7676ad77d7e2217abe4806efedfb565ee65d34a302`，与 GitHub 返回 digest 相同。

## 已修复问题

未绕过或删除失败检查。迭代修复了历史列表和导出关闭按钮的测试选择器、测试时钟相对加载时间、Calm Wellness 圆环变换的溢出、移动端背景光晕的 120% 宽度、runner 缺少中文字形及全角标点、粗野主义弱对比状态文字、设置变更后的焦点。浏览器包安装等待后改用 runner 已预装 Chrome，保留全部真实浏览器验收。

最初本地环境阻止 `file://` 且预览中继不可用，因此没有把本地预览当成通过证据。后续实际浏览器验收和直接打开验证均在 GitHub Actions 内执行。手机硬件和原生 Android 并未运行此 HTML 验收，不将响应式视口测试表述为真机 Android 测试。

## Android 审查与迁移

实际基线是 Java 17 + XML Views + ViewBinding + AppCompat/Material/Preference + AndroidViewModel/LiveData，无 Compose、DataBinding 或 WebView。ChartView 是现有 Canvas 自定义图表；P0 使用显式 Activity 导航，历史 ListView + ArrayAdapter。详见 [技术栈审查](ANDROID_TECH_STACK_AUDIT.md)。

主要视觉均能原生实现。未来新增四栏导航、详情图表 UI 接入、离线轨迹概览和部分仪表绘制，继续使用现有原生栈。玻璃模糊/高频光晕应简化。禁止为复用 HTML 引入 WebView；不重写 GPS、BLE、数据库或记录逻辑。真实 BLE 自动重连不稳定仍需真机验证，原型定时连接成功不是修复证据。

## 正式部署与访问校验

- 首次成功发布的源码：`df900b1c02bf4528b52c747c326ea804ad34397b`。
- 成功运行：[42270460362 / attempt 4](https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/actions/runs/42270460362/attempts/4)，前端验收与 Pages 部署均 success。
- GitHub deployment ID：`7617618200`，部署完成时间 `2026-11-07T06:25:47Z`（Asia/Shanghai 14:25:47）。
- GitHub 返回的 `environment_url`：https://foxmaybeoi1761640545.github.io/Runner_Gardenia-06/ 。实际网络请求 HTTP 200，响应包含本站标题 `P0 UI Design Lab` 和本地 `app.js` 入口。
- Pages 来源：`build_type=workflow`；已启用 HTTPS。没有覆盖既有站点，仓库最初 `has_pages=false`。
- 环境分支许可：`github-pages` 的精确 `branch` 规则同时包含 `main` 与 `design/p0-ui-lab`。管理员追加了设计分支，原有 main 规则保留。

此前开启 Pages 与追加环境分支许可的 API 请求均返回 403；管理员已完成这两项设置。attempt 3 被环境 main-only 规则拦截，追加设计分支后 attempt 4 成功。没有删除保护环境、改名绕过环境规则或跳过前端检查。

当前环境 Chrome 的代理 CA 信任检查失败（`ERR_CERT_AUTHORITY_INVALID`），Python HTTPS 客户端可在正常证书校验下取得 200；未禁用 TLS 验证。实际线上浏览器检查由 GitHub 托管 Chrome 执行。设计分支专用工作流现增加 **Verify published Pages in real Chrome** 任务，从 deploy job 的实际 `page_url` 获取 URL，先检查 HTTP 200 与页面身份，再对正式站点运行与发布前相同的完整验收（10 主题 × 3 视口、GPS/BLE、导航、详情、图表、GPX、设置、轮播、无外部依赖）。失败保留线上截图、JSON 和 trace，不更改已通过的检查。

后续线上验收结果见 [本分支最新 Actions](https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/actions?query=branch%3Adesign%2Fp0-ui-lab) 的 `Verify published Pages in real Chrome` 及 `p0-ui-lab-live-*` Artifact。截图来源记录中的 `actionsRun` / `sourceCommit` 绑定每次实际捕获，JSON 的 `url` 标明被测正式站点。

## 隔离确认与未完成项

相对基线的文件改动仅为 `ui-lab/**` 与 `.github/workflows/p0-ui-lab-pages.yml`。未修改 main、feature/p0-core-migration、P0.2、project-records、Android 源码、Gradle、Manifest 或已有工作流；未构建/签名 Android，未创建 Tag、Release 或合并，设计分支保留。

已无发布权限阻塞，正式站点可访问。最后一轮线上浏览器结果由上述发布后任务记录；最终方案选择与正式 Android UI 实现属于后续用户决策/开发任务。
