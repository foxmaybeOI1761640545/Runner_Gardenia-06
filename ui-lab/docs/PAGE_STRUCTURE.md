# 页面结构与状态说明

## 文件

- `index.html`：实验室标题、方案介绍、模拟手机、外部演示状态控制、主题轮播、规范链接。
- `styles.css`：网站布局、等比手机外壳、共享原型组件、十套结构与视觉主题、移动布局、减少动态效果。
- `app.js`：单一模拟状态、页面渲染、主题切换、状态同步、轮播、GPX 示例下载、焦点管理。无框架、无模块加载与后端依赖。
- `assets/gardenia.svg`：本地矢量标志。
- `previews/`：本站实际 Chromium 截图与生成说明。
- `docs/`：真实源码审查、原生实现指南、设计规范、页面结构、验收和部署记录。
- `tests/`：独立开发验收工具；不影响浏览网站，无需 npm。测试才需 Python/Playwright。

## 应用页面

```text
运动 run ── GPS 状态 gps
  ├─ 空闲 / 记录中 / 暂停
  └─ 最近记录 → 运动详情 detail
历史 history
  ├─ 空状态
  └─ 记录列表 → 运动详情 detail
                    ├─ 心率详情 heart-chart → 返回所选 detail
                    └─ GPX 导出模态框 → 保存 / 分享 / 成功 / 失败 / 重试
心率 heart
  ├─ 设备 / 扫描 / 连接 / 重连 / 过期 / 接触状态
  ├─ 手动重连与断开 / 自动重连
  └─ 心率详情 heart-chart → 返回 heart
设置 settings
  ├─ 公里 / 英里
  ├─ 配速 / 速度
  ├─ 自动重连
  └─ 隐私 / 离线 / 应用信息
```

四个主导航始终可达；模态框开启时先关闭再操作应用导航。GPS 返回运动，详情返回历史，心率详情回到调用它的父页。主题切换不会重建业务状态或换页。

## 演示状态

| 模型字段 | 值 | 补充 |
|---|---|---|
| GPS | searching / fixed / lost | fixed 演示精度 ±5m；无真实定位 API |
| Heart | disconnected / scanning / available / connecting / connected / reconnecting / stale | 外部面板切换主要状态；扫描和设备操作还呈现发现设备、连接中 |
| Session | idle / recording / paused | 缺少有效定位时不从 idle 进入 recording；GPS loss 不等于 paused |
| Contact | true / false | 手机心率页的“模拟接触良好”仅用于演示，不迁移为正式设置 |
| Empty history | true / false | 仅改变列表呈现，便于比较空状态 |
| Export failure | true / false | 测试导出失败和重试，不删除任何数据 |
| Unit / rate | km / mi、pace / speed | 所有相关页一致换算；主题切换保留 |

默认一场正在进行的模拟运动（5.24km / 28:34 / 148BPM）。最近历史来自固定合成样本。结束模拟运动会在本页内存添加摘要，刷新后清空。收藏主题 ID 会保存在本地浏览器；没有真实个人运动信息。

## 与 Android 的差别

四栏 UI、SVG 路线、模拟扫描、固定模拟图表、外部状态面板和演示定时器不等于当前 APK 能力。原生页面与服务边界见审查和迁移规范；实际业务不得复用浏览器状态替代。
