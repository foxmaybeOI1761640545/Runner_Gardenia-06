# Phase 1 交付报告：十个主题家族、二十套配色

**状态：全部门禁通过，自动进入 Phase 2。**

- 在线设计实验室：https://foxmaybeoi1761640545.github.io/Runner_Gardenia-06/
- 二十套原始浏览器截图与各页面入口：[截图画廊](../previews/appearances/index.html)。每套主题 Light/Dark 对比截图、三个视口及文件 SHA-256 均保存在 `previews/appearances/`。
- 完整清单、设计方向、Token 结构与比较功能：[主题规范](THEME_SYSTEM_PHASE1.md)。十套原布局、字体、数字、图表、圆角、边框、按钮与导航特色均保留；原布局 styles.css 未改动。
- 已部署并验收的源码：`61b9c889772bb3a7b36760d58cecedbe8dc524f3`；原设计基线：`335da1bbccb5216715e99d15175f34e5aa1dbfec`。本报告后续提交只增加证据，不重新构建或替换站点源码。
- Actions：https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/actions/runs/42385487073 。前置浏览器测试、Pages 部署、真实线上 Chrome 验收三个 Job 全部成功。

## 实现

Precision Pro、Quiet Minimal、Outdoor Explorer、Neon Pulse、Swiss Editorial、Industrial Gauge、Calm Wellness、Aurora Glass、Racing Performance、Data Brutalism 全部具备完整 Light/Dark 语义 Token。`assets/design-tokens.json` 为唯一规范源，含背景、表面、正文、强调、状态、图表、形状、字体、组件和原生迁移元数据；生成 CSS/JS 无外部运行依赖，无反色滤镜。

保留十套选择器并增加模式显示与 Light/Dark 入口；独立 Light、Dark、System Preview 控制支持手动覆盖和真实系统媒体查询响应。同步比较显示同一页面/模拟状态的两种配色，手机上下排列，可独立滚动；主原型继续处理所有动作。切换不会重置页面、运动、GPS、BLE、历史、弹窗或指标位置。

## 验收结果

- 本地与云端前置、云端线上完整原有回归均为 **1,303 项断言通过**；保留导航、GPS、BLE、运动记录/暂停/继续、历史详情、心率图表、GPX、指标交换、键盘、真实触摸与离线打开检查。
- 二十套配色 × 三种视口 = **60 个组合**，每轮 **330 张真实截图**：300 张主页/心率/历史/设置/弹窗截图，30 张双版本比较截图。视口为 1440×1000、1366×768、390×844。
- 云端线上完整回归之外，当前环境独立访问真实 Pages 并重复全部 60 个配色组合、330 张截图、系统主题变化、手动覆盖、比较与状态保持检查：通过；JS 错误 0，资源失败 0，横向溢出 0。
- HTTP 200；线上主要 JS、CSS 与 Token 字节和验收源码一致。Token 正文/状态/按钮对比度达到 4.5:1；额外检查历史汇总及已选设备文字的实际合成对比度。
- 已下载云端前置及线上紧凑截图包，核对 GitHub Artifact SHA-256，并逐一核对每包 330 张截图 SHA-256。

证据：[最终阶段门禁](acceptance/theme-system/phase1-final-gate.json)、[云端完整运行](acceptance/theme-system/cloud-success.json)、[云端前置回归](acceptance/theme-system/cloud-frontend-results.json)、[云端线上回归](acceptance/theme-system/cloud-live-results.json)、[云端线上截图清单](acceptance/theme-system/cloud-live-appearance-provenance.json)、[独立线上浏览器](acceptance/theme-system/independent-live-browser.json)、[HTTP 与源码字节核验](acceptance/theme-system/live-http.json)。

线上截图包 Artifact `13580072558`：`160233c35e33643f57fec141fba058d254294d7c0c3c999f8a0de92391e61354`。前置包 Artifact `13579497590`：`7ea0a8780c0441f24a89d8627afb34bfb1bf64829fc751f12480185c5b82eebb`。完整诊断包和 trace 随 Actions 保留 30 天；设计分支保留原始配色截图、清单与报告。

## 修复与保留记录

视觉审查修复 Quiet/Aurora 历史汇总和 Brutal 已选设备行的文字/背景映射，并加入浏览器实际对比度断言。被取代的首个云端候选在部署前取消，运行记录保留。受管 Chromium 禁止 file:// 的首次本地失败也保留；未更改管理策略，改用官方 Playwright Chrome for Testing 完整重跑并通过离线测试，没有跳过断言。

## Phase 2 交接

只从 P0.2 Tag 解引用源码 `4454d716a5ae010b92d3eefa1ac1c0ebedde0063` 创建 `feature/theme-system-p0-3`，迁移前四套主题和八套配色。Android 固定 FOLLOW_SYSTEM，仅持久化家族 ID；不合并整个 UI Lab，不移植 HTML/CSS，不修改 GPS、BLE、数据库或 GPX 业务算法。
