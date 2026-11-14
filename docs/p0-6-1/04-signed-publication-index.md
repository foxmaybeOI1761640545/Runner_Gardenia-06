# 固定签名、原生复核与发布索引

实际APK源码6cee1958a23bfd1a1fe3b6a9b547717c528cbc75；签名批准8958de3422e62b6e1ad69e26690754ad8c3d069e。

[签名运行43613996030](https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/actions/runs/43613996030)：同仓库、独立分支、push、attempt1，终态success。scope、signed、signed-review-transfer成功；该模式checks/map-smoke/native-window-helper/map-geometry/review-transfer/publish按设计跳过。最终源码完整测试仍绑定43609863706，未拿其他提交替代。

正式Artifact14121714342，RunnerGardenia-P061-Release-43613996030，16056306字节，ZIP SHA256 `1055fa97a881b0d69d65613c11c817bf86c92202bd73d33cb5c62c9d341e85fa`。诊断报告14121769085，21896952字节，ZIP SHA256 `50955ecbc06f16935bab5bed34072d45545a13f8c0c5fa9a6210d383ffdd7c7f`。实际下载、计算摘要、逐项校验15个payload与ARTIFACT-MANIFEST；正式APK与早期签名候选完全同字节，未重新签名。

实际APK3234719字节，SHA256 `b6df2f1ad0431c5a8ccd805209d7452171eb5fdbd611bc2b8f3f4d9bcf4d8e11`，6779/v0.1.0-p0.6.1/com.fox.runner，原固定证书E3E7D46BFCB869F5A521A7FBA6FD1BA5790D7DBCAF37F722DDC69D7BB1439A22。R8、资源优化、zipalign、apksigner、非debuggable及原始完整测试绑定实际通过。

P0.6/6778安装-r升级未卸载、未清数据：原GPS点1、原始心率2、合成历史、schema43、GPS=NONE/7秒、主题/单位/速率、HR设备配置、原schema1地图索引与105834033字节地图摘要保留。新记录37个GPS点、2个人工分段，距离20.087764m，实际录制时长45793ms/墙钟57794ms；准备不建运动、暂停恢复、新进程、GPX schema通过。P0.4/6775另独立覆盖安装通过（35 GPS点/2分段），该版本本来无地图，所以其baselineMapRetained=false不是P0.6地图丢失。

99张签名原生PNG及其窗口XML/focus/SHA均已验证和直接查看：88张四主题×明暗×11页，4张原合成地图运行图，7张真实100MB地图图。逐文件记录见signed-native-review.json；不能把合成公园图算真实地区。真实文件场景包含系统SAF、私有SHA、飞行模式、源文件删除、新进程、z7/12/16、实际触摸拖动中心变化、缩小15/恢复16、320dp568dp字体1.5、普通模拟器GNSS实时轨迹、关闭地图运动继续/偏好不变、损坏导入保留。真实图片中的道路/水系/建筑/中文地名、紫色位置与青色轨迹已直接确认。320dp大字按钮仍可操作，文件名和按钮文字会换行，地图可视高度明显缩短；没有把该情形写成理想的手机大字体验。

发布批准只允许本文件、报告与.github/p061/publication-approval.json；完整源/测试/版本/签名构建代码不可改变。对应发布运行可从本提交的Actions检查进入；它将再次验证正式Artifact、原始XML、全部15个payload、APK证书，创建新唯一annotated Tag及非Latest Pre-release，先核对Draft全部附件再公开，并匿名下载实际APK再执行apksigner。运行的P061-publication-proof Artifact内publication.json才是公开发布/匿名下载回执。最终交付消息提供准确发布run_id/URL与最终分支HEAD。

公开后固定地址：[Release](https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/releases/tag/test-p0-v0.1.0-p0.6.1)。附件包含全部15个payload与ARTIFACT-MANIFEST，共16项；theme-screenshots.zip、offline-map-screenshots.zip、real-map-screenshots.zip是可公开下载的原生PNG/XML证据。未将私人录屏/真实运动数据上传。

真实手机、户外GNSS、Magene H303、长期后台/耗电仍为NEEDS_DEVICE_VALIDATION。所有32个原ref于发布前重新比对未变；服务器保护查询403不能等同已检查服务器保护。

## 已完成的公开发布与匿名复核

[发布运行43617601031](https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/actions/runs/43617601031)已终态success，head_sha=c8efde1d73469b027dc7d28d41b7056d6efa5fbc，event=push，branch=feature/p0-6-map-real-validation，attempt1。scope132515971848、publish132516050007成功；其他模式Job按设计跳过。2026-11-14T16:50:38Z公开Release445443618，非Draft/Pre-release/非Latest，16项附件完整uploaded。新annotated Tag对象4b85d6a73a49e75d52b27497c3fb85ff666ee39b只指向验收源码6cee1958。32个历史ref再次保持原值。

CI匿名下载与实际apksigner通过；独立匿名public asset API下载HTTP200，无Authorization，3234719字节、SHA256 b6df2f1ad0431c5a8ccd805209d7452171eb5fdbd611bc2b8f3f4d9bcf4d8e11，与正式签名Artifact逐字节相同。全部16项Release资产的大小/摘要与正式包一致。原仓库Latest endpoint为404（无正式Latest），新测试版未成为Latest。

publication-receipt.json为发布Job实际生成的原始回执；independent-publication-verification.json记录独立下载及保护ref核验。回执Artifact14122364720、3314字节、ZIP SHA256 4b6b14736e484d70d135c051b48c56e35e17dc954009f736169f1298291f70e6已实际计算匹配。

后续报告提交仅保存已完成的事实。现有工作流配置paths-ignore=[docs/**, AGENTS.md]，文档提交不触发新运行；因此不存在文档HEAD对应的新验收或复核run_id。实际完整/签名/发布依据仍分别是43609863706、43613996030、43617601031，不以文档提交冒充另一次测试。没有重构建、重签、重复分配版本或覆盖任何附件。最终文档HEAD由最终交付消息和分支提交记录单独标明。
