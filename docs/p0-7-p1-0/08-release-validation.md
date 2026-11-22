# P0.7固定签名运行与发布验证

当前PENDING。阶段1～3代码及Debug全量回归已验收，但正式P0.7签名APK完整运行、公开发布未完成。所有失败均保留，不将构建/签名成功等同发布成功。

## 首次签名运行失败

[Run44984776201](https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/actions/runs/44984776201)，激活提交5fefd7a，实际产品源码f794dce。独立approved-tests准入成功，android-release原固定密钥构建/身份/证书/16KiB对齐/R8及资源优化验证成功。候选APK3,236,595 bytes，SHA-256 ba8bc298d6bdd9c2272ef1e93fb0425a03134b9fd98beb555ec7b0430a1eeebc；版本6780、包名com.fox.runner、原固定证书均一致。候选Artifact14739378132保留；不是正式Release。

实际运行完成88张原生页面矩阵及真实地图导入z7后，detail-z12等待完整受支持瓦片失败。错误为“Signed native viewport did not complete every file-supported tile”。已下载失败Artifact14739679246并重算ZIP SHA-256 40488437ca4f37e04ef8a7d43b44198e955a22afc8eb17e49a669ff038cf11a5；失败日志、候选build-info、最后实际原生截图/XML和失败摘要见evidence/run44984776201-signed-failed。完整7张真实地图验收未完成，未生成正式Artifact、Tag或公开Release。Run继续保持failure。

## 原因分析与最小修复

前一场景的实际首页XML显示GPS Ready、精度5m，来自新加坡模拟位置；新任务要求前台预览持续，进入地图时已经有新鲜Fix。旧P0.6.1验收脚本假定冷GPS，先点击单次“My position”再注入广州Fix。P06MapActivity在旧新鲜Fix立即定位成功后清除centerPending；随后广州Fix更新覆盖层，但不自动改变全图查看页的手动视口，因此可能留下新加坡空白视口。最终实际截图与此一致，旧脚本未持久化超时前最后诊断，本轮明确保留该证据边界。

修复只改签名真实地图验收脚本：四个连续普通模拟GNSS Fix后再次通过真实原生定位按钮定位；detail-z12和new-process额外断言视口中心在广州输入附近，并持久化最后完整诊断以便失败复核。未直接设置生产地图视口、未增加生产测试入口、未改GPS过滤或用户设置，全部原受支持瓦片CACHED/实际缩放/渲染/私有文件/新进程/飞行模式/轨迹断言保留。生产树仍为1b3c03950ef0ba319bb3d79abb02841c35028f0c。

此原因判断与修复尚需新候选的完整Actions回归和签名运行实际成功验证。因验收脚本已修改，原源码的完整测试证明不冒充当前源码；重新执行所有阶段3完整门禁，再绑定新的源提交和实际审查记录进入签名。仅py_compile静态检查已执行，不把它算作Android或Helper测试通过。P0.8及后续保持未启动。

## 修复后的完整回归

467f208的[完整Run44987372985](https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/actions/runs/44987372985)已SUCCESS，原始146 Python、167 JVM、471 API35（执行470，历史Skip1）重新核验，492张本轮原生截图已全部实际查看。完整新证据在evidence/run44987372985-full。此结果授权再次进入原固定密钥签名及完整运行门禁；尚不宣称旧签名失败已被新的实际签名运行证明修复，尚未发布。

## 第二次签名运行与实际视觉审查成功

[Run45008081843](https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/actions/runs/45008081843) SUCCESS，激活87c87b7、实际源码467f208，approved-tests与受保护android-release signed全部成功。正式Artifact14749309660，ZIP SHA-256 9111a4f64e4c64a4c444c112e3913962a808a217b394f537308e39cdf4c82433。已独立下载核验MANIFEST、APK、全部截图归档、99个PNG和对应原生窗口XML摘要，再实际查看全部99图（13张审查拼图），通过视觉审查。

正式候选APK3,236,595bytes（比P0.6.1增加1,876bytes），SHA-256 3c6ad819e740f82b24063d3be6da9e259f69b959bb9a54c1145b6974b085beb7，com.fox.runner/v0.1.0-p0.7.0/6780，固定证书E3E7D46BFCB869F5A521A7FBA6FD1BA5790D7DBCAF37F722DDC69D7BB1439A22，debuggable=false，16KiB对齐、实际R8/资源优化均核验。

原P0.6.1覆盖升级保留配置/历史/地图，实际模拟GPS记录38点、暂停恢复两段，空闲预览不生成历史，运动后用户GPS偏好保留；强制停止数据保留及显式恢复、GPX1.1导出成功。88张页面、4张离线夹具地图和7张真实地图均完整通过；热GPS情形下原生定位按钮修正实际验证成功，z12和新进程视口中心核验及全部原瓦片/渲染/缩放断言均通过。真实地图经SAF导入、源文件删除、私有文件校验、飞行模式新进程重载；实际地图平移缩放和关闭地图继续运动均通过。320dp大字体Import .map文字换行略生硬但控件完整可用，作为外观限制记录，不隐藏。证据见evidence/p07-signed-release。

旧失败Run44984776201仍保留failure及所有证据。当前仅签名运行及审查成功，公开发布与独立公开下载核验尚待完成；不重构建/重签名正式包，未启动P0.8。
