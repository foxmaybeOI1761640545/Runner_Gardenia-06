# 验收与部署记录

日期：2026-11-07，Asia/Shanghai。设计分支长期保留，不合并 main。

## 交付与基线

- 分支：`design/p0-ui-lab`。
- 已 fetch 并核实 annotated tag `test-p0-v0.1.0-p0.1`。
- Tag 对象：`c3af195d4d99eb468dc09420bee43336b295bbd2`。
- 基线提交：`3116e5fa7a56ce6f299ea45d5a45745f1aed4466`，与指定值一致。
- 本次归档的 UI 实测提交：`6910416c2125fbdf8bf0719db00759062c7944a7`。
- 实测运行：[42269278260](https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/actions/runs/42269278260)。该运行的 **Frontend and real-browser acceptance 成功**，部署任务因 Pages 未开启失败；不能把整个运行表述成部署成功。
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
| GitHub Actions | 前端真实验收通过；Pages 部署单独阻塞，见下文 |
| GitHub Pages 实际访问 | **未完成：站点未开启，未取得 GitHub 返回的访问 URL，未宣称已上线** |

实际截图在 [previews](../previews/README.md)，来源、尺寸和哈希见 [PROVENANCE.json](../previews/PROVENANCE.json)。完整 trace、下载的示例 GPX、结果 JSON 在 Actions 验收 Artifact，保留 30 天。已下载的紧凑截图包 SHA-256：`01155efee3855f83059f5c7676ad77d7e2217abe4806efedfb565ee65d34a302`，与 GitHub 返回 digest 相同。

## 已修复问题

未绕过或删除失败检查。迭代修复了历史列表和导出关闭按钮的测试选择器、测试时钟相对加载时间、Calm Wellness 圆环变换的溢出、移动端背景光晕的 120% 宽度、runner 缺少中文字形及全角标点、粗野主义弱对比状态文字、设置变更后的焦点。浏览器包安装等待后改用 runner 已预装 Chrome，保留全部真实浏览器验收。

最初本地环境阻止 `file://` 且预览中继不可用，因此没有把本地预览当成通过证据。后续实际浏览器验收和直接打开验证均在 GitHub Actions 内执行。手机硬件和原生 Android 并未运行此 HTML 验收，不将响应式视口测试表述为真机 Android 测试。

## Android 审查与迁移

实际基线是 Java 17 + XML Views + ViewBinding + AppCompat/Material/Preference + AndroidViewModel/LiveData，无 Compose、DataBinding 或 WebView。ChartView 是现有 Canvas 自定义图表；P0 使用显式 Activity 导航，历史 ListView + ArrayAdapter。详见 [技术栈审查](ANDROID_TECH_STACK_AUDIT.md)。

主要视觉均能原生实现。未来新增四栏导航、详情图表 UI 接入、离线轨迹概览和部分仪表绘制，继续使用现有原生栈。玻璃模糊/高频光晕应简化。禁止为复用 HTML 引入 WebView；不重写 GPS、BLE、数据库或记录逻辑。真实 BLE 自动重连不稳定仍需真机验证，原型定时连接成功不是修复证据。

## 部署阻塞与唯一管理员设置

仓库初始 `has_pages=false`，GET Pages 为 404，没有覆盖既有站点。自动 POST Pages（`build_type=workflow`）两次返回 **403 Resource not accessible by integration**。仓库账号有管理权限，但当前 GitHub 集成没有这项 Pages 管理 API 权限。独立部署任务在 configure-pages 阶段因站点不存在失败；前端检查未被跳过。

**唯一需要管理员变更的设置：仓库 Settings → Pages → Build and deployment → Source → GitHub Actions。** 开启后可重试本设计分支工作流；如果 Pages artifact 已过期，重新运行全流程生成即可，无需修改 main 或业务分支。当前没有实际成功 URL，不填猜测的地址。

## 隔离确认与未完成项

相对基线的文件改动仅为 `ui-lab/**` 与 `.github/workflows/p0-ui-lab-pages.yml`。未修改 main、feature/p0-core-migration、P0.2、project-records、Android 源码、Gradle、Manifest 或已有工作流；未构建/签名 Android，未创建 Tag、Release 或合并，设计分支保留。

剩余唯一发布阻塞：管理员开启 GitHub Actions Pages 后完成部署和实际 URL 访问验证。最终方案选择与正式 Android UI 实现属于后续用户决策/开发任务。
