# Release / Tag audit

Audit date: 2026-11-04 Asia/Singapore. Source baseline main: `24aa0d7d36d244852e796cca6307936e34d989ed`.

Remote refs, peeled commit IDs, all repository Releases (including drafts), asset lists, full Actions logs and retained artifact inventories were queried directly. GitHub's automatic source ZIP/tar.gz links are not uploaded APK assets. There were four local/remote version tags and one published Pre-release, with no empty or draft Release to remove.

| Tag / Code | Commit | Actions evidence | Signed APK / assets | Classification and action |
| --- | --- | --- | --- | --- |
| v0.1.0-rc1.1 / 6766 | ebc6549a6e228328b82a33995d2ebf2329a26579 | [#5](https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/actions/runs/41671404110), [#6](https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/actions/runs/41672584767) | Both retained only Debug/report artifacts; no signed artifact or Release. #6 failed reading `/res/xml/shortcuts.xml` after resource optimization. | Automated orphan; reserved in ledger; deletion pending durable ledger push. |
| v0.1.0-rc1.2 / 6767 | a9f1770fa81d803959434bc25d864a02a885376e | [#7](https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/actions/runs/41681169734) | Debug/CI gate/plan/reports only; no signed artifact or Release. Failed reading `res/xml/shortcuts.xml`. | Automated orphan; reserved in ledger; deletion pending durable ledger push. |
| v0.1.0-rc1.3 / 6768 | d0db0556fbd51de6d282f74fb667309acbf7e860 | [#8](https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/actions/runs/41683034188) | Debug/CI gate/plan/reports only; no signed artifact or Release. Optimized `res/Eq.xml` was found, but typed package binding verification failed. | Automated orphan; reserved in ledger; deletion pending durable ledger push. |
| v0.1.0-rc1.4 / 6769 | 24aa0d7d36d244852e796cca6307936e34d989ed | [#9](https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/actions/runs/41685585228) | Verified signed Artifact, publication receipt, public APK + SHA256SUMS + build-info.json. | Valid published Pre-release; preserve tag object and assets unchanged. |

## Valid release verification

Downloaded rc1.4's actual public APK, SHA256SUMS and build-info.json. AAPT2 reads `com.fox.runner`, Version Name `v0.1.0-rc1.4`, Code `6769`. Downloaded bytes, checksum file, build-info and GitHub asset digest all agree:

```text
e9988650cf0e847b7666eebbb81a07a7cfe5d98dc60f77232e2893213eadf7e0
```

The successful signed and publication jobs independently ran apksigner and identity verification. Their recorded certificate is `E3E7D46BFCB869F5A521A7FBA6FD1BA5790D7DBCAF37F722DDC69D7BB1439A22`. These are existing Actions verification results, not a claim that Android SDK/apksigner was installed locally. Public Release: https://github.com/foxmaybeOI1761640545/Runner_Gardenia-06/releases/tag/v0.1.0-rc1.4 .

## Allocation safety before cleanup

`release-ledger.json` preserves every original version name, Code, exact source SHA, tag-object ID and evidence run. High water is sequence **4**, Code **6769**; next planned version is **v0.1.0-rc1.5 / 6770**, assuming no new main release intervenes. The allocator checks both existing tags and this ledger, rejects conflicting identities, and validates the high-water record. The tag job rejects reuse of a historically reserved name or Code.

Regression tests remove the highest tag, then all tags, and still allocate rc1.5 / 6770. Corrupt watermarks, duplicate allocations, contradictory live tags and attempted historical name/Code reuse fail closed. Future authorized tag cleanup must first extend this record to cover all subsequently allocated versions.

## Cleanup result

Pending ledger commit/push and exact-ref recheck. No tag has been deleted at this point. Unknown-purpose tags: zero in this inventory. Recoverable unpublished signed versions: zero. No Git commits or source history will be deleted.
