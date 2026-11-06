[TEST][P0.2] v0.1.0-p0.2

GPS 定位准备阶段：可靠定位前不创建运动、不计时和累计距离；支持取消/超时继续等待，GPS 丢失时运动继续。
运动首页优化：运动状态、距离/时间/速度/配速、心率摘要及随状态变化的操作。

BLE 自动连接本轮仅完成源码审查，未修复 H303 首次连接无 BPM 的实机问题。
H303/RF 断连重连、户外 GNSS 精度/实际1Hz、锁屏长期运行和耗电均为 NEEDS_DEVICE_VALIDATION。

- Application ID: `com.fox.runner`
- Version Name: `v0.1.0-p0.2`
- Version Code: `6773`
- Source: `4454d716a5ae010b92d3eefa1ac1c0ebedde0063`
- APK bytes: 2753061
- APK SHA-256: `48c4a855402b7bec0b310794a228f15323ef8f09b2eb81d4f6913ab56ac6d046`
- Certificate SHA-256: `E3E7D46BFCB869F5A521A7FBA6FD1BA5790D7DBCAF37F722DDC69D7BB1439A22`
- Actions: https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/actions/runs/42251766485
- Test counts: `{"android": {"tests": 395, "executed": 394, "failures": 0, "errors": 0, "skipped": 1}, "unit": {"tests": 110, "executed": 110, "failures": 0, "errors": 0, "skipped": 0}}`
- Fixed-key signature, non-debuggable identity, alignment, R8/resources, P0.1 in-place upgrade, retained GPS/raw HR/device configuration and GPX export verified on API35 AOSP.

Android8+：下载 APK，核对 SHA256SUMS，直接覆盖 P0.1 安装；保留应用数据，不要卸载或清除数据。测试版，请先备份重要运动记录。
全部历史/当前报告：https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/blob/feature/p0-2-gps-ui/docs/reports/README.md
