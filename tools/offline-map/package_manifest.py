#!/usr/bin/env python3
import argparse, hashlib, json, platform
from datetime import datetime, timezone
from pathlib import Path

def sha256(path):
    h = hashlib.sha256()
    with open(path, "rb") as f:
        for block in iter(lambda: f.read(1024 * 1024), b""):
            h.update(block)
    return h.hexdigest()

def main():
    p = argparse.ArgumentParser()
    p.add_argument("--region", required=True)
    p.add_argument("--map", required=True)
    p.add_argument("--guangdong-info", required=True)
    p.add_argument("--fujian-info", required=True)
    p.add_argument("--combined-info", required=True)
    p.add_argument("--out", required=True)
    a = p.parse_args()
    region = Path(a.region)
    map_file = Path(a.map)
    out = Path(a.out)
    audit = json.loads((region / "boundary-audit.json").read_text(encoding="utf-8"))
    manifest = {
        "product": "Runner Gardenia P0.6 offline vector map",
        "file": map_file.name,
        "format": "Mapsforge .map",
        "mapsforge_writer": "0.25.0 source tag",
        "mapsforge_reader_compatibility": "0.25.0 (same as P0.6)",
        "created_utc": datetime.now(timezone.utc).isoformat(),
        "bytes": map_file.stat().st_size,
        "sha256": sha256(map_file),
        "max_import_bytes": 2 * 1024 * 1024 * 1024,
        "sources": {
            "guangdong": "https://download.geofabrik.de/asia/china/guangdong-latest.osm.pbf",
            "fujian": "https://download.geofabrik.de/asia/china/fujian-latest.osm.pbf",
            "license": "OpenStreetMap ODbL; © OpenStreetMap contributors"
        },
        "boundary_audit": audit,
        "build_host": platform.platform()
    }
    (out / "manifest.json").write_text(json.dumps(manifest, ensure_ascii=False, indent=2), encoding="utf-8")
    readme = """# Runner Gardenia：珠三角—粤东—漳州东山岛离线地图

## 文件
- RunnerGardenia_GD_PRD_East_Fujian.map：P0.6 可导入的 Mapsforge 地图。
- manifest.json：文件大小、SHA-256、数据来源与边界审计。
- boundary-audit.json：14 个目标城市边界、梅州走廊及覆盖范围审计。
- region.geojson：实际用于裁切 OSM 数据的连续覆盖多边形。
- map-samples.csv：14 个城市内部抽样点，已由 Mapsforge 0.25.0 Reader 验证可读取。
- SHA256SUMS.txt：地图文件 SHA-256。
- guangdong-source.md5、fujian-source.md5：Geofabrik 源 PBF 校验记录。

## 覆盖范围
### 广东珠三角九市
广州、深圳、珠海、佛山、东莞、中山、江门、惠州、肇庆。

### 粤东四市
汕尾、揭阳、汕头、潮州。

### 福建
仅漳州市，包含漳州城区、龙海、漳浦及东山县/东山岛；不包含厦门、泉州等其他福建城市。

### 梅州
不包含梅州市全域。仅在目标区域边界附近 20 公里范围内，沿 OSM 主要道路/骑行道路建立约 1 公里缓冲走廊，以便连接可能经过的道路；不扩展为整个梅州市地图。

### 连续性与边界
- 以目标城市 OSM 行政边界为基础。
- 使用 2.5 公里半径的形态学闭合处理城市边界之间小于约 5 公里的细小缝隙。
- 增加 500 米外缘安全缓冲，避免边界道路被裁切。
- 排除阳江、茂名、湛江等非目标粤西地区，以及其他非目标福建城市。
- 为保证道路和多边形对象完整，OSM 提取可能保留少量跨边界道路的完整节点引用；最终地图对象仍按提取区域和地图范围进行处理。

## 导入 P0.6
1. 将 .map 文件下载到手机。
2. 打开 Runner Gardenia → 离线地图 → 导入 .map。
3. 选择本文件，等待校验与复制完成。
4. 导入成功后可删除 Downloads 中的原始文件；App 私有目录会保留副本。
5. 关闭网络或启用飞行模式，打开地图验证。

P0.6 单文件导入上限为 2 GiB。本构建生成后检查该上限，并使用 Mapsforge 0.25.0 Reader 对地图头、中心瓦片和 14 个目标城市样本点进行读取验证。

## OpenStreetMap 署名与许可
地图数据 © OpenStreetMap contributors，依据 Open Database License (ODbL) 提供。
数据来源：
- https://download.geofabrik.de/asia/china/guangdong.html
- https://download.geofabrik.de/asia/china/fujian.html
- https://www.openstreetmap.org/copyright

本文件为 OpenStreetMap 数据的区域提取与转换作品。请在分享或再分发时保留本说明和 ODbL 署名。
"""
    (out / "README_IMPORT_CN.md").write_text(readme, encoding="utf-8")
    print(json.dumps(manifest, ensure_ascii=False, indent=2))

if __name__ == "__main__":
    main()
