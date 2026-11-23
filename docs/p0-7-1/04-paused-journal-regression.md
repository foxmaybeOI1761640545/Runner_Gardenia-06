# 原有暂停恢复断言发现问题并保留门禁

完整 Run [45060769567](https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/actions/runs/45060769567) 对源码 `00e7904962e6847bcda6c0eda74966a873717152` 失败。Python160、JVM167、导航18、GPS45、首页地图2和权限检查通过，但原有 `P05DashboardStatusNavigationTest.statusNavigationPreservesRecordingConnectionAndPausedJournal` 第240行失败。

| Suite | Tests | Executed | Passed | Failures | Errors | Skipped |
|---|---:|---:|---:|---:|---:|---:|
| API35 full | 489 | 488 | 487 | 1 | 0 | 1 |

实际场景为：记录中进入心率页，回首页暂停，再进入同一个心率页并返回。心率页保留的 P0RecordingViewModel 离开时还处于 RECORDING；重新连接服务时，看到服务非记录便误以为运动已结束，写 FINISHED 删除了共享恢复日志。原数据库 Track 仍存在，不能把日志缺失说成数据删除，也不能把失败当成通过。

这是一级页面实例复用直接暴露的稳定性问题，需要一个小范围 UI 状态同步修复。重新观察服务前，仅对本地未完成会话从原 SessionStore 读取当前意图（与新 ViewModel 构造时的恢复方式一致）；状态观察使用服务当前同步状态，防止快速重新连接收到过期排队状态。没有改 RecordingSession 状态机、SessionStore 格式、TrackRecordingService、GPS/BLE 驱动/owner/采样生命周期、运动数据写入算法或数据库 schema43。

新增修复文件 P0RecordingViewModel 仅涉及重新观察时同步状态，因此生产范围为五个导航文件加一个 UI ViewModel。原有失败测试逐字保留，所有原 Java 测试继续受字节保护。该原有类的两项测试加入 focused GPS/业务门禁，45项增加到47项；新增 T16 断言暂停后反复切换全部一级页面两次，日志 Track ID 保留且恢复同一运动。四项签名驱动 Python 回归也纳入最低160项门禁。

当前160项本地 Python通过；新提交必须重新执行完整 Actions/API35、原生视觉、固定签名和升级门禁。旧运行的原始 JUnit、失败测试日志、gate和助手回执已保存到 evidence/run45060769567-full-failed。没有修改或删除原失败断言，没有新增 Skip/Ignore，没有创建 P0.7.1 发布，也未开始 P0.8.0。
