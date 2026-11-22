# 同一导航问题的快捷入口补齐

复核根页面所有真实入口时发现：首页 `p0_devices` 和设置分类内的设备/历史管理快捷入口仍使用默认 Intent，即使底部导航保持实例唯一，也能通过这些入口新建重复一级 Activity。Manifest 中这些 Activity 都是默认 launch mode。此问题属于本轮“一级页面不重复创建”的导航修复范围。

将已有根 Intent 的两项 flags 集中为 P0Navigation.rootIntent，底部导航、首页心率和设置设备/历史管理入口共用。地图仍为正常二级 Intent，保留 Settings 分类返回关系。历史根复用时仅对明确 MANAGEMENT 新 Intent 进入已有管理模式，普通切换保留详情/选择状态；批量工作 busy 时不改变状态。不更改历史业务 ViewModel、数据库或导出逻辑，不改 GPS/BLE/运动服务。

生产改动现在为五个导航相关文件：P0Navigation、P0SettingsActivity、P0HistoryActivity、P0Activity（一行入口）和 P05SettingsSectionFragment（一级入口 Intent）。未重写导航架构。

在新增 T10/T11 中保留全部原断言，补充从已有 History/Heart 经设置快捷入口或首页心率按钮再次进入，必须 `assertSame` 原 Activity、所有根唯一；历史管理模式仍正常、原运动数据不变，返回 Settings 后原分类栈仍可正常返回。测试仍为 T01～T18，截图矩阵仍为25张，没有移除失败用例。

160 项本地 Python 全部通过。中间源码 0e92c44 的全量运行尚未形成完成验收，已被包含这些必要修复的新候选替代，取消必须记为 cancelled 而非通过。新的完整源码测试/API35/视觉/固定签名运行门禁全部待实际执行。本轮没有发布任何 P0.7.1 Tag/Release，P0.8.0 未开始。
