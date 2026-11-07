# Runner Gardenia P0 UI Lab

在线体验：**https://foxmaybeoi1761640545.github.io/Runner_Gardenia-06/**

长期保留的设计分支：`design/p0-ui-lab`。参考 Tag `test-p0-v0.1.0-p0.1`，提交 `3116e5fa7a56ce6f299ea45d5a45745f1aed4466`。

独立、无框架的 HTML5 / CSS3 / JavaScript 网站：十套完整可交互设计，共享运动、GPS、历史/详情、心率设备/图表、GPX 与设置。此原型不是正式 Android UI，所有数据均为模拟。

## 使用

下载此分支后直接用浏览器打开 `ui-lab/index.html`，无需 npm、构建、后端、API Key 或网络。若浏览器或组织策略阻止 file://，可在本机用 `python -m http.server 8080 --directory ui-lab`，访问 `http://localhost:8080/`。

- 手机内点击四栏导航和具体操作，长页面在手机屏幕内滚动。
- 手机外的状态面板切换 GPS、心率、记录、空历史和导出失败。
- 下方选择十套方案；方向键切换；鼠标拖动/触摸滑动探索。切换主题保留当前页。
- 默认每 8 秒自动轮播；操作后暂停；悬停/聚焦时暂停；点击播放恢复；减少动态效果默认关闭自动播放。
- 手动重连入口在心率页直接可见，自动重连不保证恢复读数。
- GPX 导出产生明确标为 SIMULATION 的有效合成示例，支持成功、失败、重试，设备支持文件分享时可分享。
- 收藏保存的只有方案 ID。模拟运动仅在内存，不会保存或上传真实位置/设备信息。

## 十套方案

01 Precision Pro · 02 Quiet Minimal · 03 Outdoor Explorer · 04 Neon Pulse · 05 Swiss Editorial · 06 Industrial Gauge · 07 Calm Wellness · 08 Aurora Glass · 09 Racing Performance · 10 Data Brutalism。

每套使用不同的布局/排版/组件造型，并共享完整核心功能，详见 [设计规范](docs/DESIGN_SPEC.md)。

## 文档

- [Android 技术栈审查](docs/ANDROID_TECH_STACK_AUDIT.md)：真实 P0 源码与职责路径。
- [原生 UI 实现指南](docs/NATIVE_UI_IMPLEMENTATION_GUIDE.md)：Java + XML + ViewBinding、组件映射、十套 Token、状态与兼容性。
- [设计规范](docs/DESIGN_SPEC.md)、[页面结构](docs/PAGE_STRUCTURE.md)。
- [测试与部署记录](docs/TEST_REPORT.md)、[实际截图](previews/README.md)。

## 独立验收

网站运行不需要安装依赖。开发或 CI 验收：

```sh
python ui-lab/tests/check_static.py
python -m pip install -r ui-lab/tests/requirements.txt
python -m playwright install --with-deps chromium
python ui-lab/tests/browser_acceptance.py
```

GitHub Actions 使用托管 Ubuntu runner 已预装的 Google Chrome（记录实际版本），不额外下载浏览器；测试所需中文 fallback 是 `tests/fonts/` 内附许可的本地子集，安装到 runner 系统字体目录，不作为网站资源加载。上面的 Playwright install 命令仅供缺少浏览器的开发环境；已有 Chrome 时可设置 `CHROMIUM_EXECUTABLE`。测试在自己的临时本地端口提供静态文件；可用 `UI_LAB_URL` 指向已部署网站。验收生成 `previews/` 实际截图、`test-results/results.json`、失败截图与 Playwright trace。测试失败不会部署。三个尺寸是 1440×1000、1366×768、390×844，覆盖每个主题。

## GitHub Pages

新工作流 `.github/workflows/p0-ui-lab-pages.yml` 仅监听本设计分支 push（仅 UI Lab 和其工作流路径）及 workflow_dispatch，并在任务级拒绝其他分支。前端静态与真实 Chromium 检查通过后才进入独立 Pages 部署任务；发布后使用 GitHub 返回的 `page_url` 再运行完整 Chrome 验收，确认正式站点 HTTP 200、十套主题与全部页面状态正常。仅使用 SHA 固定版本的官方 Actions，OIDC 与最小权限。不构建 Android、不签名、不建 Tag/Release、不合并、不删除分支。

初次工作流仅存在非默认分支时，GitHub 可能不在默认分支的 Actions UI 展示手动运行入口；正常 push 自动部署不受影响。不要为获取入口修改 main；有新修改正常 push 即可运行。

本仓库已开启 **Pages → Source: GitHub Actions**，并在 **Environments → github-pages → Deployment branches and tags** 中为 `design/p0-ui-lab` 追加精确分支许可，保留原有 `main` 规则。首次成功部署与实际访问记录见 [验收报告](docs/TEST_REPORT.md)。线上浏览器验收 Artifact 名称为 `p0-ui-lab-live-*` / `p0-ui-lab-live-previews-*`。

## 隔离

本分支相对基线只新增 `ui-lab/` 和指定 Pages 工作流。不修改 main、业务分支、project-records、Android 源码、Gradle、Manifest 或已有 Android 工作流。后续原生实现继续使用 Java/XML，禁止用 WebView 承载此原型。
