# 验收与部署记录

本文件由实际运行结果补充，不能以计划代替通过记录。

- 参考 Tag 对象已 fetch 核实：`c3af195d4d99eb468dc09420bee43336b295bbd2`。
- 基线提交：`3116e5fa7a56ce6f299ea45d5a45745f1aed4466`。
- 设计分支：`design/p0-ui-lab`。
- 当前阶段：实现与静态检查，真实 Chromium 验收和部署等待首次设计分支 Actions 执行。
- 本地预览限制：浏览器环境阻止 file://，Sites preview relay 不可用。没有据此宣称本地视觉验收通过。
- 真实浏览器测试在本分支 GitHub Actions 内运行，失败保留日志、截图、trace；通过后再允许 Pages 部署。
- 仓库初始 `has_pages=false`；现有网站未被覆盖。

待填入：检查提交、运行链接、三个尺寸×十套主题结果、截图来源、控制台/网络检查、Pages 配置与实际 URL、剩余限制。
