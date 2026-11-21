# 阶段2：应用前台全局定位（候选，未验收）

启动前提：阶段1源码 e08887d 的全量回归及35张原生视觉检查通过，验收报告提交 c69ac06 已推送并核验远程 HEAD。阶段2不包含阶段3的临时运动采样模式/会话恢复改造，也不分配版本或签名。

实际变更：Startup 注册应用生命周期控制器 ForegroundGpsController；以已启动 Activity 的 identity set 维护一个前台需求，普通后台立即释放，配置重建只允许500ms过渡，绑定完成时再次检查权限及前台状态。所有页面复用 TrackRecordingService、SensorManager 和现有 GpsHandler，前台需求不添加第二个 mapGpsOwners 客户端。服务统一裁决实际定位需求；空闲预览使用现有 INTERNAL/1Hz 临时读取，不写 GPS 来源/间隔。显式心率预览意图独立于应用 GPS 生命周期。现有 GpsMotionFilter 的独立预览状态过滤跳点，正式记录过滤状态保持独立。

新增 P07ForegroundGpsTest 覆盖真实 Application 回调、首页/设置/历史/地图、重建、同一驱动、空闲历史隔离、后台释放、前后台恢复、弱/过期/无时间/跳点、provider恢复、地图请求去重、默认心率偏好与显式心率预览。原 P061GpsPreferencesTest、P06GpsOwnershipTest 文件逐字保留，CI在执行前校验原文件。原 P0RecordingUiTest 的取消后 sensors=false 断言保留，先新增前台仍有GPS且无运动记录断言，再实际移至后台执行原有断言，以符合新需求。首页地图的跨洲位置测试明确重连合成 provider，仍保留全部几何/无要素/截图断言；真正跳点过滤由新增测试覆盖。

定向门禁29项（新增4、原偏好4、owner5、准备13、原生运动UI3），随后原有真实地图截图检查、全部Python/JVM/API35回归。所有结果待实际 GitHub Actions 返回，当前不宣称测试通过。权限拒绝的独立新进程烟测、系统服务恢复和实体手机功耗仍待后续验证，不能以已授权模拟器场景替代。

下一阶段启动条件：修复全部失败；取得当前源码完整非空报告、唯一历史 Skip、原生视觉复核及阶段报告提交/推送/远程核验。阶段2通过前不开始阶段3正式实现。

补充独立权限门禁：P07PermissionStateTest 不自行授予位置权限；API35 独立设备上使用两次实际进程分别测试 denied/granted，强制检查运行时真实权限状态、前台预览是否启动、零运动历史、后台释放和不同 PID，并保留实际原生截图。普通全量回归也执行该测试的当前权限分支，但不能替代独立 denied/granted 两次运行。新增该项后完整 Android 预期至少464项（463实际执行、1项历史Skip），定向GPS子集仍为29项。

## 保留的首轮失败及修复

源码 ec47d24 / [Run 43747382885](https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/actions/runs/43747382885)：GPS定向 Tests29 / Executed29 / Failures1 / Errors0 / Skipped0，完整回归未执行。原有 owner5及GPS偏好4项断言全部执行成功；新增跨页面测试在 ActivityScenario.recreate 失败，因为嵌套 launch 建立独立测试任务后销毁原首页。原始XML、gate/helpers和失败摘要见 `evidence/run43747382885-failed/`。修复改用应用原生导航按钮、Activity.recreate及实际返回流程；同驱动、1Hz、owner数量、零运动历史和偏好断言全部保留，没有 Ignore 或放宽预期。修复候选必须重新执行完整门禁。

本轮用户范围已收敛到阶段1～3及P0.7交付，完成后暂停，不进入P0.8。

同一测试任务栈问题也发生于前一个源码 f76d556 / Run 43747070609：Tests29 / Executed29 / Failures1 / Errors0 / Skipped0。已独立下载并核对 ZIP 摘要，原始失败 XML 和摘要保留于 `evidence/run43747070609-failed/`。两次失败均未重新标记通过，未执行的完整回归不计成功。

源码 fcb0c69 / Run 44936917983：GPS定向29/29/0/0/0通过；地图定向2/2/1/0/0失败，完整回归和独立权限烟测未执行。新增跳点过滤需要连续一致位置，而旧截图等待只判断直接绑定的地图中心及瓦片缓存，尚未等真实服务确认新 Fix，原生 GPS Ready 断言因此正确失败。修复要求服务中的新鲜坐标、FIX状态、实际原生状态文本和真实瓦片同时满足；原有Ready和可见标记断言全部保留。证据见 `evidence/run44936917983-failed/`，不得计作阶段通过。
