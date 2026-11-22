# 首次签名运行门禁失败，禁止发布

Run [45059576993](https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/actions/runs/45059576993) 的固定签名、Release 优化、APK 包名/版本/证书/对齐/离线属性检查通过，但签名运行门禁失败，没有创建 Tag 或 Release。

在覆盖 P0.7.0 后，脚本实际执行 Settings 根 Back，将任务放到后台；接着用 `adb am start -W` 恢复已有 Home。API35 已将原任务 TO_FRONT（logcat 03:20:00），最终 focus 和原生截图也显示 Home 正在记录、时间持续增长，但 `am -W` 仍等待 launch-drawn 事件，40 秒超时。原始失败、完整运行 Artifact 摘要、候选 build-info、最终 focus/window/截图和关键系统日志均保存在 evidence/run45059576993-signed-driver-failed，完整 logcat 在该 Run Artifact 中。

只改签名验收驱动：启动命令不使用 `-W`，随后仍独立要求实际 Home 窗口恢复；根 Back 后必须首先观察非本应用实际窗口（null 不算）。没有删除返回断言、没有跳过运动连续性检查，没有修改生产代码。新增四个 Python 驱动回归，覆盖恢复成功、返回另一个一级页面不能算后台、未恢复 Home 不能成功、二级地图不能充当根页面。160 项本地 Python 全部通过。

原源码测试/视觉收据继续保留，但签名运行失败不能记为通过。新提交的完整 Actions/API35/视觉和固定签名运行门禁必须重新执行；旧候选从未验收完成或发布，不作为可交付 APK。P0.8.0 仍未开始。
