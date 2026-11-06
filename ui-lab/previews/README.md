# 实际浏览器截图

`theme-01.png` 至 `theme-10.png`：同一应用状态（模拟记录中）的十套主题截图，均由 `tests/browser_acceptance.py` 在实际 Chromium 中渲染本站后截取手机外框。

`desktop-1440.png`、`desktop-1366.png`、`mobile-390.png`：1440×1000、1366×768、390×844 视口的整页截图。

首次 CI 将生成并保留截图 Artifact，下载审查后提交到本目录。未生成的图片不作为已完成交付。脚本/trace/results.json 留在 Actions 测试 Artifact 中，不用外部设计图片替代实际预览。
