# Phase 2：主副指标交换

状态：IN_PROGRESS。Phase 1 真实收据与48张截图已通过，验收提交 ffc35b9，Actions 42574249001。

采用纯Java DashboardMetric / DashboardMetricLayout：DISTANCE、DURATION、PACE_SPEED、HEART_RATE恰好出现一次，GPS精度不进入模型。交换只对主位置与被点击位置执行一次交换。默认Neon心率主显示，其他三主题距离主显示。位置经PreferencesUtils存入既有默认SharedPreferences，按主题独立键保存；支持恢复当前主题默认值，非法存储回退本主题默认值。

五份XML增加稳定的主位置/三个可点击副位置、动态标签，复用现有数值View。副位置有触摸、键盘焦点、按钮无障碍角色与动态描述，点击后保持同一位置焦点并发出主显示公告。Neon图表保留独立BPM说明；定位精度不绑定交换操作。缺失/过期心率仅在仪表盘指标中显示--，原传感器/历史页面未修改。

DashboardState仍来自原Snapshot。单位转换复用已有DistanceFormatter/SpeedFormatter；公制原格式复用MotionFormat。只显示当前配速或速度偏好。Renderer更新现有View，返回页面重新投影现有Snapshot，不调用记录命令或重启传感器。

验收：模型默认/准确交换/24种排列/防重复与序列化损坏回退；全部主题原生真实点击与渲染、重建、独立保存；无心率仍可交换；固定精度不可点击；真实记录服务的track/GPS对象/连接ID保持；通过现有专用文件Runner的write→force-stop→read和不同PID验证四主题偏好持久化；保留Phase1矩阵与相关回归。旧UI测试保留速度与配速验证，改为显式切换真实偏好后读取同一显示位置，不再假设两种速率同时展示。

当前仅完成实现与本地可用静态检查。Android、长时长/单位/键盘与公告专项截图等实际门禁尚待补齐与远端执行，不能宣布本阶段通过，也不能进入Phase 3。所有数据fixture均为测试数据，不表示实际GNSS或BLE硬件通过。
