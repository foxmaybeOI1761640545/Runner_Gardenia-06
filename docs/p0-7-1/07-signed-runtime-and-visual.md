# P0.7.1 固定签名 APK、覆盖升级与原生视觉验收

[签名运行45092892094](https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/actions/runs/45092892094) 已成功完成。实际产品/构建源码 `857c744321825a2656d77f45d57c6e43b4334962`；工作流/源码审批 Commit `348b225c68e90f095fc4f9207ba872c9205bed66`。版本 `v0.1.0-p0.7.1 / 6781`，Application ID `com.fox.runner`。

正式不可变 Artifact `14782439925 / RunnerGardenia-P071-Release-45092892094`，ZIP14689354 bytes，SHA-256 `b30d075ca29163a10350534ef0dfbfbf636cd82b20884d6b7bf77a12f4477330`。报告 Artifact14783176982，ZIP20531826 bytes，SHA-256 `093ecbfa0d680109bc12e06718345c9605603551a0ba9d0b1d9391e69bca0c2d`。独立下载后均验证大小、摘要、安全路径，再核对正式清单15个载荷文件的全部SHA-256。

实际APK3238755 bytes，SHA-256 `1c68922da5052c8b9dc224194e719116ccfafd616905f00d6909b837ebd82759`；固定证书 `E3E7D46BFCB869F5A521A7FBA6FD1BA5790D7DBCAF37F722DDC69D7BB1439A22`。原JKS通过受保护android-release环境恢复并在所有结果下清理；R8、资源优化、apksigner、ZIP alignment、aapt身份和离线Manifest检查通过。无INTERNET权限、debuggable=false。候选与正式Artifact内APK字节相同，不重新构建/签名替代已验收包。

另在没有本地Android SDK或私钥的环境，用公开证书独立验证APK v2密码学签名、完整文件6个分块内容摘要、固定证书、ZIP CRC及166个存储条目的4字节对齐。签名数据篡改和内容篡改副本均被拒绝。代码、正/负检查收据见 candidate-signature/；这些是额外校验器检查，不混入应用测试数量。

实际API35安装P0.7.0公开正式APK（源码467f208、code6780、SHA3c6ad819…），使用install -r覆盖，无卸载/清数据。升级保留schema43、历史fixture1个GPS点/2条原始心率样本、主题/单位/速率、设备/重连配置、GPS来源NONE与7秒采样设置、原100MB地图文件SHA-256及索引选择。运行期间真实服务通过普通模拟器GNSS驱动生成39个有效GPS点、2个手工Segment、24.51米、46181ms活动时间/58314ms墙钟时间；暂停/强停/进程恢复/继续/结束与实际GPX1.1导出验证通过。准备定位不创建运动。原始数值见runtime-smoke.json。

34次实际根Back检查都将任务置于后台，再显式回到Home，未遍历旧一级页面。固定签名APK原生矩阵88页（四主题八配色，设置/版本/首页/心率/历史/多选/分享/删除/地图）与原数据签名保持验证通过，实际系统Sharesheet及批量GPX/ZIP导出不修改源记录。合成地图4场景与真实105834033-byte离线地图7场景通过SAF系统选择、外部源删除、新进程重开、飞行模式、缩放/拖动、真实记录叠加、坏文件保留地图、320dp大字体与GPS设置不变检查。

99张原生PNG及对应原生XML均按实际APK摘要独立核对，并已实际查看全部9张联系表；另打开320dp大字体和实时记录叠加原图。未发现本次改动引入的显示异常。320dp大字体导入按钮自然换行，文字/操作可用；原有主题和地图布局未在维护版扩改。视觉收据、原始JSON/XML/GPX及示例PNG在evidence/run45092892094-signed/。完整99图压缩包保留于正式Artifact，发布将原样作为Release资产。

源码完整测试结果仍为Python160/160、JVM167/167、API35 489Tests/488Executed/488Passed/0Failures/0Errors/1原有Skipped；导航18/18、GPS/business47/47通过。合成GPS/BLE和模拟器结果不冒充真实手机。实体GNSS、BLE射频、OEM预测性手势/后台策略仍为NEEDS_DEVICE_VALIDATION。

目前签名/升级/运行/原生视觉通过；Pre-release、匿名下载独立验证及最终交付报告尚待执行。此审批只授权从正式不可变Artifact发布同一份APK，P0.8.0仍等待本版完整独立交付。
