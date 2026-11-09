# Phase 1：运动首页顶部布局

状态：PASSED。Phase 0 已由 Actions `42572215358` 验收，报告/原始收据已在 `facd752` 推送；本阶段仅实施功能 A。

## 修改范围与现状

五份 binding 合同布局（`p0_home.xml` 与四套 `gardenia_home_*.xml`）删除 `p03_family_label` 控件，同时删除 Renderer 和 Debug 预览宿主的相关绑定。标题容器随内容自然缩小；不存在透明/空白占位主题 View，不保留其 marginTop。保留标题与下一行间正常的 6dp 间距。

新增 `p05_compact_controls` 原生水平容器：跑步/骑行 RadioGroup 占两份权重，GPS/心率状态各占一份；采用 wrap_content/min48dp、短标签、图标、真实选中状态。状态文字包含状态含义，完整动态 ContentDescription 提供状态与操作。GPS 与设备入口继续使用原有回调。沿用原记录阶段的运动类型隐藏逻辑，避免重建时展示错误的可选类型；服务操作及真实数据不变。

Precision 分割数据网格、Quiet 中轴留白、Explorer 轨迹叠加卡片、Neon 心率优先与原生图表均保留。新样式与中英文短标签在 `p05_dashboard_styles.xml` / `p05_dashboard_strings.xml`；其他语言回退英文。没有新增运行时依赖、WebView、网络权限或业务管理器。

## 实际验收清单

- API35：4 主题 × Light/Dark × 360/393/412dp × 字体 1.0/1.3，共 48 个真实原生配置；截图明确标记 UI fixture。
- 原主题 View 已删除；标题容器只有品牌与运动标题，后续行贴合正常标题间距；四控件同一行、至少 48dp、无重叠/横向溢出/裁字。
- 跑步/骑行互斥选中；GPS 未定位/搜星/有效定位、心率连接/断连来自真实状态投影，动态描述正确；真实 Activity 点击进入系统定位设置和原设备页面。
- 保留原 192 张原生八状态矩阵和相关真实服务/导航/记录/定位准备/心率回归，不以 synthetic fixture 代替业务测试。
- 完整 JVM 测试、Debug/Lint、非零 Android/JVM XML；分别记录 Tests/Executed/Failures/Errors/Skipped，失败不推进。
- 下载精确 SHA 的 Artifact，校验 ZIP/PNG 摘要、实际像素尺寸与来源；人工检查关键八配色及窄屏大字体截图，发现问题修复重测。

## 当前结果

静态 XML/绑定合同、四主题结构/八配色冻结和成熟业务源码范围检查通过。Android 运行、截图、人工视觉验收与本阶段正式 SHA/Actions 收据仍 PENDING；不能据此宣布 Phase 1 通过，Phase 2 尚未开始。

## 候选失败记录：098dbac / 42572848848

Android 32 / Executed 32 / Failures 1 / Errors 0 / Skipped 0；JVM 118 / Executed 118 / Failures 0 / Errors 0 / Skipped 0。原 192 张状态矩阵生成，新增 48 张矩阵在首次单选测试即失败，不能宣布新矩阵通过。RadioButton 的 performClick 返回值只代表监听器处理结果，不代表框架选中状态；改用 Espresso 真实触摸并保留选中、互斥、可操作性断言。另发现 Gradle clean 删除先前的 helpers.json，需将精确执行收据保存在清理范围之外。原始失败 XML 与收据保留于 evidence/phase1-failed-42572848848；没有删除测试、降低业务断言或进入 Phase 2。

## 最终验收：1e93950 / 42574249001

[Actions](https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/actions/runs/42574249001) success。Android Tests=32 / Executed=32 / Failures=0 / Errors=0 / Skipped=0；JVM 118 / 118 / 0 / 0 / 0；辅助测试 87 / 87 / 0 / 0 / 0。Debug、Lint、原192张状态矩阵、新48张配置矩阵通过。已独立下载并核对两个Artifact摘要，原始XML及48张PNG归档于 evidence/phase1-passed-42574249001。

已人工审查六张汇总图中的全部48个真实Android截图：标题主题名称与占位区域消失，紧凑控件没有溢出或重叠，48dp与完整文字断言通过；保留四主题个性。GPS入口验证发出的系统设置Intent（拦截外部启动），心率入口实际进入设备Activity。记录和暂停状态/记录ID/GPS对象/连接ID回归通过。截图中的运动数据为测试fixture，不能证明真实GNSS/BLE硬件。Phase 2可以进入。
