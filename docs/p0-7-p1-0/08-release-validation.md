# P0.7固定签名运行与发布验证

当前PENDING。阶段1～3代码及Debug全量回归已验收，但正式P0.7签名APK完整运行、公开发布未完成。所有失败均保留，不将构建/签名成功等同发布成功。

## 首次签名运行失败

[Run44984776201](https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/actions/runs/44984776201)，激活提交5fefd7a，实际产品源码f794dce。独立approved-tests准入成功，android-release原固定密钥构建/身份/证书/16KiB对齐/R8及资源优化验证成功。候选APK3,236,595 bytes，SHA-256 ba8bc298d6bdd9c2272ef1e93fb0425a03134b9fd98beb555ec7b0430a1eeebc；版本6780、包名com.fox.runner、原固定证书均一致。候选Artifact14739378132保留；不是正式Release。

实际运行完成88张原生页面矩阵及真实地图导入z7后，detail-z12等待完整受支持瓦片失败。错误为“Signed native viewport did not complete every file-supported tile”。已下载失败Artifact14739679246并重算ZIP SHA-256 40488437ca4f37e04ef8a7d43b44198e955a22afc8eb17e49a669ff038cf11a5；失败日志、候选build-info、最后实际原生截图/XML和失败摘要见evidence/run44984776201-signed-failed。完整7张真实地图验收未完成，未生成正式Artifact、Tag或公开Release。Run继续保持failure。

## 原因分析与最小修复

前一场景的实际首页XML显示GPS Ready、精度5m，来自新加坡模拟位置；新任务要求前台预览持续，进入地图时已经有新鲜Fix。旧P0.6.1验收脚本假定冷GPS，先点击单次“My position”再注入广州Fix。P06MapActivity在旧新鲜Fix立即定位成功后清除centerPending；随后广州Fix更新覆盖层，但不自动改变全图查看页的手动视口，因此可能留下新加坡空白视口。最终实际截图与此一致，旧脚本未持久化超时前最后诊断，本轮明确保留该证据边界。

修复只改签名真实地图验收脚本：四个连续普通模拟GNSS Fix后再次通过真实原生定位按钮定位；detail-z12和new-process额外断言视口中心在广州输入附近，并持久化最后完整诊断以便失败复核。未直接设置生产地图视口、未增加生产测试入口、未改GPS过滤或用户设置，全部原受支持瓦片CACHED/实际缩放/渲染/私有文件/新进程/飞行模式/轨迹断言保留。生产树仍为1b3c03950ef0ba319bb3d79abb02841c35028f0c。

此原因判断与修复尚需新候选的完整Actions回归和签名运行实际成功验证。因验收脚本已修改，原源码的完整测试证明不冒充当前源码；重新执行所有阶段3完整门禁，再绑定新的源提交和实际审查记录进入签名。仅py_compile静态检查已执行，不把它算作Android或Helper测试通过。P0.8及后续保持未启动。
