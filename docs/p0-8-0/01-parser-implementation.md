# 阶段A：独立模型与GPX解析（Actions待验收）

新增Route/RouteSegment/RoutePoint、RouteDistance、RouteLimits、GpxRouteImporter和对应测试，不引用历史数据库或GPS/BLE/运动业务。完整取舍与限制见00；本报告不是Repository/UI/跟随或最终发布完成声明。

本地使用现有Java21 compiler模块以source17/target17编译纯Java模型，43项JUnit通过，包含实际16MiB/20,000点/1024段/128路线/深度32/字段2048等上下边界。128MiB JVM堆下执行通过；本地没有Android SDK，运行不能代替实际Android API兼容和SAX行为验证。原160项Python回归加5项P08准入测试共165项通过，无Skip。原Java测试不变。

最初40项本地JUnit有1项失败：未知XML编码以UnsupportedEncodingException冒出，被归为普通READ。实现现区分ENCODING，测试要求精确分类；重新执行40项通过，再补UTF16、UTF8 BOM、Unicode别名截断及前向流哈希保护后43项通过。最初失败与后续输出保留在evidence/stage-A-local，不隐藏失败、不增加Ignore、不降低旧断言。早期本地性能收据只对应40项变体，不冒充最终Android峰值。

独立p08-stage Actions将执行原有完整JVM加43项新测试、165项Python、Debug/Lint/原生编译，并在API35运行8项专门平台测试，要求原始XML非空、所有配置类执行、无新增Skip，以及实际16MiB/20,000点平台时间与堆收据。此处API35仅是解析focused，原有完整API/UI/签名升级仍在F阶段执行。当前尚未有本阶段Actions通过记录，下一阶段B未开始。
