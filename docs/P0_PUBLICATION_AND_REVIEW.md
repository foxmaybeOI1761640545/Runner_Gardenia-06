# P0 测试发布与最终技术审查

本轮按用户新指令先发布已验收 APK，再进行技术审查；此前 Artifact-only/不创建 Tag 或 Release 的限制由本轮明确授权替代。main 和其他原有分支仍受保护。

| 阶段 | 状态 | 证据 |
| --- | --- | --- |
| 0 原始发布证据 | PASSED | 新下载 Artifact13391972069，2,715,993bytes，SHA25694d3322310e8b6050504eb4c8d8c72fa09f0190f8add35ae0100b9bc93cc6ab0；aapt2身份与非Debug；独立apksig9.5.0验证SDK26–37/v2及固定证书；run41974364095成功/构建3116e5f；e8973de及b4d8070均只含docs变更 |
| 1 测试 annotated Tag | PASSED | test-p0-v0.1.0-p0.1远程重新读取，解析目标3116e5fa7a56ce6f299ea45d5a45745f1aed4466；原有3个rc Tag均保留 |
| 2 Draft Pre-release | PASSED | Draft434907895，title[TEST][P0]v0.1.0-p0.1，prerelease=true，target_commitish=构建SHA；make_latest=false；原仓库没有Latest稳定Release（API404） |
| 3 附件准备与上传 | IN_PROGRESS | 原始APK及SHA256SUMS.txt、e8973de原始两份文档、build-info/runtime-smoke共6个附件；工作区uploads.github.com认证401，而api.github.com可正常创建/读取Draft |
| 4 Draft复核、发布、公开下载复核 | PENDING | 全部资产重新下载核验后才允许发布 |
| 5–13 技术审查及最终收口 | PENDING | 必须在阶段4通过后执行；尚未开始 |

上传认证恢复不需要新的PAT/密钥：仅给专用附件传递Job contents:write/actions:read，使用GitHub Actions自己的短期Token；不配置Environment，不访问签名Secrets，不运行Gradle，不重建或重新签名APK。上传Job只操作既有Draft434907895，重新下载原始固定Artifact和每一份Draft资产，发现差异即失败，禁止clobber/删除资产/修改Tag。

正常源码推送仍需原有完整P0测试和签名门禁。只有显式[p0-existing-apk]提交、与已验收构建相比仅改变本次发布脚本/工作流/新范围测试/文档，才走原始APK附件传递路线；任何生产源码、Gradle/版本/签名或原有测试/门禁变更均拒绝该路线。新增5项范围回归和原有39项辅助测试在本地均通过。
