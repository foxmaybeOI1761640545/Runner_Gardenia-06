# Phase 2：主副指标交换

状态：PASSED。Phase 1 真实收据与48张截图已通过，验收提交 ffc35b9，Actions 42574249001。

采用纯Java DashboardMetric / DashboardMetricLayout：DISTANCE、DURATION、PACE_SPEED、HEART_RATE恰好出现一次，GPS精度不进入模型。交换只对主位置与被点击位置执行一次交换。默认Neon心率主显示，其他三主题距离主显示。位置经PreferencesUtils存入既有默认SharedPreferences，按主题独立键保存；支持恢复当前主题默认值，非法存储回退本主题默认值。

五份XML增加稳定的主位置/三个可点击副位置、动态标签，复用现有数值View。副位置有触摸、键盘焦点、按钮无障碍角色与动态描述，点击后保持同一位置焦点并发出主显示公告。Neon图表保留独立BPM说明；定位精度不绑定交换操作。缺失/过期心率仅在仪表盘指标中显示--，原传感器/历史页面未修改。

DashboardState仍来自原Snapshot。单位转换复用已有DistanceFormatter/SpeedFormatter；公制原格式复用MotionFormat。只显示当前配速或速度偏好。Renderer更新现有View，返回页面重新投影现有Snapshot，不调用记录命令或重启传感器。

验收：模型默认/准确交换/24种排列/防重复与序列化损坏回退；全部主题原生真实点击与渲染、重建、独立保存；无心率仍可交换；固定精度不可点击；真实记录服务的track/GPS对象/连接ID保持；通过现有专用文件Runner的write→force-stop→read和不同PID验证四主题偏好持久化；保留Phase1矩阵与相关回归。旧UI测试保留速度与配速验证，改为显式切换真实偏好后读取同一显示位置，不再假设两种速率同时展示。

最终候选已完成下述实际门禁与人工截图审查。所有数据fixture均为测试数据，不表示实际GNSS或BLE硬件通过。

追加专项门禁：真实键盘Enter激活与点击位置焦点保持；UiAutomation捕获原生TYPE_ANNOUNCEMENT事件；360dp/1.3字体下125:25:30主显示的八配色截图、文本完整性检查；真实记录Snapshot的英制投影验证；不同PID读进程中校验实际主位置标题。以上新增专项也必须在最终候选中实际通过，首个候选即使基础测试成功，也不自动接受本阶段。

## 首轮失败：a13b840 / 42595186896

Android34 / Executed34 / Failures1 / Errors0 / Skipped0；JVM123 / 123 / 0 / 0 / 0。交换专项和其余回归通过，但原矩阵发现Neon360dp搜星操作按钮裁切（112px仅显示28px），不得接受本阶段。原因是主指标auto-size缩小字形却保留原72sp测量高度，另有新增图表独立说明间距。修复为Neon主位置显式设置实际测量字号（空心率30sp、有效心率70sp、其他指标40sp并允许换行），调整卡片正常内边距容纳独立BPM说明，保留原84dp曲线与按钮完整可见断言。失败XML及收据归档evidence/phase2-failed-42595186896；实际进程probe因原生失败尚未执行。

进一步补齐真实服务隔离场景：暂停时交换前后时长/距离/phase与记录对象、BLE状态不变；GPS禁用后交换不恢复旧坐标、不重新开启记录；BLE重连等待状态中交换不改变连接状态，恢复原槽位后继续原新会话接收断言。均在原测试中增加操作与断言，没有替代或删除原回归。

## 扩展测试候选：5092cce / 42595972500

Android35 / Executed35 / Failures2 / Errors0 / Skipped0；JVM124 / 124 / 0 / 0 / 0。两个失败为修复前的Neon搜星按钮裁切，以及新增英制投影测试误把无速度GPS样本交给SpeedFormatter得到0.0 mph作为预期（实际产品正确显示未知—）。键盘/焦点、原生公告、八配色长时长专项通过。英制测试补充真实定位通路中的明确3m/s测试输入，并要求样本确实带速度，再验证原生产投影的换算结果，不修改未知值防护。该候选使用修复前产品代码，失败收据完整保留。5d70592的待运行任务被GitHub并发队列自动替换为包含更多隔离断言的7355044（run42597630556），未把取消任务计作通过。7355044任务随后因已知测试输入缺陷主动取消，不计作通过；由修复后的8faa6ce重新执行全部门禁。

## 最终验收：8faa6ce / 42598521463

[Actions](https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/actions/runs/42598521463)结论SUCCESS；精确源码8faa6ce6c7a1ac9f5b21221b42ae49e24b59be14。Android Tests35 / Executed35 / Failures0 / Errors0 / Skipped0；JVM124 / 124 / 0 / 0 / 0；流水线辅助测试87 / 87 / 0 / 0 / 0。独立进程write PID1746 → force-stop → read PID2211，分别执行1条、失败/错误/跳过0，原数据库与四主题指标偏好及实际主标题均恢复，CI时区Asia/Singapore。

48张三宽度/两字体/八配色顶部截图及8张125:25:30主指标截图已逐组人工检查，7张联系表与原图、JUnit XML、进程收据、精确源码/Artifact SHA归档于[evidence/phase2-passed-42598521463](evidence/phase2-passed-42598521463/acceptance.json)。Neon360dp按钮裁切已修复；精密分割线、Quiet留白、Explorer地形卡片和Neon心率曲线保留。原192状态矩阵完整收集并通过其布局断言。本阶段是选定回归，不冒充Phase5全量测试。

关键文件：DashboardMetric.java、DashboardMetricLayout.java、DashboardRenderer.java、DashboardState.java、P0Activity.java、PreferencesUtils.java、四主题及兼容p0_home XML；模型/原生交换/服务隔离/真实进程恢复测试。未修改成熟GPS、BLE、记录、数据库算法。触摸、键盘Enter、焦点和原生无障碍公告事件通过；真实TalkBack语音和实际GNSS/BLE硬件仍NEEDS_DEVICE_VALIDATION。允许进入Phase3；尚未签名或发布P0.5，P0.4完全保留。
