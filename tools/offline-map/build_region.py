#!/usr/bin/env python3
"""Build a continuous OSM polygon for Runner Gardenia's requested region."""
from __future__ import annotations

import argparse
import json
from pathlib import Path

import osmium
from pyproj import Transformer
from shapely.geometry import LineString, mapping, shape
from shapely.ops import transform, unary_union

TARGETS = {
    "广州": {"广州市", "广州", "guangzhou"},
    "深圳": {"深圳市", "深圳", "shenzhen"},
    "珠海": {"珠海市", "珠海", "zhuhai"},
    "佛山": {"佛山市", "佛山", "foshan"},
    "东莞": {"东莞市", "东莞", "dongguan"},
    "中山": {"中山市", "中山", "zhongshan"},
    "江门": {"江门市", "江门", "jiangmen"},
    "惠州": {"惠州市", "惠州", "huizhou"},
    "肇庆": {"肇庆市", "肇庆", "zhaoqing"},
    "汕尾": {"汕尾市", "汕尾", "shanwei"},
    "揭阳": {"揭阳市", "揭阳", "jieyang"},
    "汕头": {"汕头市", "汕头", "shantou"},
    "潮州": {"潮州市", "潮州", "chaozhou"},
    "漳州": {"漳州市", "漳州", "zhangzhou"},
}
MEIZHOU_NAMES = {"梅州市", "梅州", "meizhou"}
ROAD_CLASSES = {
    "motorway", "motorway_link", "trunk", "trunk_link",
    "primary", "primary_link", "secondary", "secondary_link",
    "tertiary", "tertiary_link", "unclassified",
    "cycleway", "track", "path",
}
MEIZHOU_CORRIDOR_DISTANCE_M = 20_000
MEIZHOU_ROAD_BUFFER_M = 1_000
GAP_CLOSING_RADIUS_M = 2_500
OUTER_SAFETY_BUFFER_M = 500

TO_M = Transformer.from_crs("EPSG:4326", "EPSG:6933", always_xy=True).transform
TO_WGS84 = Transformer.from_crs("EPSG:6933", "EPSG:4326", always_xy=True).transform
GEOJSON = osmium.geom.GeoJSONFactory()


def norm(value: str) -> str:
    return "".join(ch for ch in value.strip().lower() if not ch.isspace()).replace("市", "")


def canonical_name(tags: dict[str, str]) -> str | None:
    values = set()
    for key in ("name", "name:zh", "name:zh-Hans", "official_name", "short_name", "alt_name", "name:en"):
        value = tags.get(key)
        if value:
            values.update(part.strip() for part in value.split(";") if part.strip())
    normalized = {norm(v) for v in values}
    for city, aliases in TARGETS.items():
        if normalized.intersection({norm(a) for a in aliases}):
            return city
    if normalized.intersection({norm(a) for a in MEIZHOU_NAMES}):
        return "__梅州__"
    return None


class Collector(osmium.SimpleHandler):
    def __init__(self):
        super().__init__()
        self.boundaries: dict[str, list[dict]] = {}
        self.meizhou_roads: list[LineString] = []

    def area(self, area):
        tags = dict(area.tags)
        if tags.get("boundary") != "administrative":
            return
        level = str(tags.get("admin_level", ""))
        if level not in {"4", "5", "6", "7", "8"}:
            return
        canonical = canonical_name(tags)
        if canonical is None:
            return
        try:
            geom = shape(json.loads(GEOJSON.create_multipolygon(area)))
            if geom.is_empty:
                return
            if not geom.is_valid:
                geom = geom.buffer(0)
            if geom.is_empty or geom.area <= 0:
                return
            self.boundaries.setdefault(canonical, []).append({
                "geometry": geom,
                "admin_level": level,
                "name": tags.get("name") or tags.get("name:zh") or canonical,
                "area_id": int(getattr(area, "id", 0) or 0),
            })
        except Exception:
            return

    def way(self, way):
        highway = way.tags.get("highway")
        if highway not in ROAD_CLASSES:
            return
        coords = []
        for node in way.nodes:
            if node.location.valid():
                coords.append((node.location.lon, node.location.lat))
        if len(coords) < 2:
            return
        lons = [p[0] for p in coords]
        lats = [p[1] for p in coords]
        if max(lons) < 115.0 or min(lons) > 117.9 or max(lats) < 23.0 or min(lats) > 25.5:
            return
        try:
            self.meizhou_roads.append(LineString(coords))
        except Exception:
            return


def choose_boundary(candidates: list[dict], label: str) -> dict:
    if not candidates:
        raise RuntimeError(f"Missing OSM administrative boundary for {label}")
    rank = {"6": 0, "5": 1, "7": 1, "4": 2, "8": 2}
    candidates.sort(key=lambda x: (rank.get(x["admin_level"], 9), -x["geometry"].area))
    return candidates[0]


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--guangdong", required=True)
    parser.add_argument("--fujian", required=True)
    parser.add_argument("--out", required=True)
    args = parser.parse_args()
    out = Path(args.out)
    out.mkdir(parents=True, exist_ok=True)

    collector = Collector()
    for pbf in (args.guangdong, args.fujian):
        print(f"Reading administrative areas and Meizhou road candidates from {pbf}", flush=True)
        collector.apply_file(pbf, locations=True)

    selected = {city: choose_boundary(collector.boundaries.get(city, []), city) for city in TARGETS}
    projected = {city: transform(TO_M, row["geometry"]) for city, row in selected.items()}
    city_union = unary_union(list(projected.values()))

    # Close small slivers/gaps between neighbouring target-city polygons while
    # retaining original islands and narrow peninsulas.
    closed = city_union.union(city_union.buffer(GAP_CLOSING_RADIUS_M).buffer(-GAP_CLOSING_RADIUS_M))
    closed = closed.buffer(0)

    meizhou_row = None
    if collector.boundaries.get("__梅州__"):
        meizhou_row = choose_boundary(collector.boundaries["__梅州__"], "梅州")
    corridor_count = 0
    corridor_area = 0.0
    meizhou_area = 0.0
    if meizhou_row:
        meizhou = transform(TO_M, meizhou_row["geometry"])
        meizhou_area = meizhou.area
        corridor_parts = []
        for line in collector.meizhou_roads:
            projected_line = transform(TO_M, line)
            part = projected_line.intersection(meizhou)
            if part.is_empty or part.distance(closed) > MEIZHOU_CORRIDOR_DISTANCE_M:
                continue
            corridor_parts.append(part.buffer(MEIZHOU_ROAD_BUFFER_M))
            corridor_count += 1
        if corridor_parts:
            corridor = unary_union(corridor_parts)
            corridor_area = corridor.area
            closed = unary_union([closed, corridor])

    final_region = closed.buffer(OUTER_SAFETY_BUFFER_M).buffer(0)
    if final_region.is_empty or final_region.area <= 0:
        raise RuntimeError("The final coverage geometry is empty")

    wgs_geometry = transform(TO_WGS84, final_region)
    feature = {
        "type": "Feature",
        "properties": {
            "name": "Runner Gardenia Guangdong PRD + East Guangdong + Zhangzhou",
            "source": "OpenStreetMap contributors",
            "target_city_count": len(TARGETS),
            "gap_closing_radius_m": GAP_CLOSING_RADIUS_M,
            "outer_safety_buffer_m": OUTER_SAFETY_BUFFER_M,
            "meizhou_policy": "major-road corridors only within 20 km of target-region boundary; no full-city coverage",
        },
        "geometry": mapping(wgs_geometry),
    }
    (out / "region.geojson").write_text(
        json.dumps({"type": "FeatureCollection", "features": [feature]}, ensure_ascii=False),
        encoding="utf-8",
    )

    samples = []
    city_audit = []
    for city, row in selected.items():
        point = projected[city].representative_point()
        lon, lat = TO_WGS84(point.x, point.y)
        samples.append((city, lon, lat))
        city_audit.append({
            "city": city,
            "osm_name": row["name"],
            "admin_level": row["admin_level"],
            "area_id": row["area_id"],
            "area_km2": round(projected[city].area / 1_000_000, 3),
            "sample_lon": round(lon, 7),
            "sample_lat": round(lat, 7),
        })
    with (out / "map-samples.csv").open("w", encoding="utf-8") as f:
        f.write("city,longitude,latitude\n")
        for city, lon, lat in samples:
            f.write(f"{city},{lon:.8f},{lat:.8f}\n")

    audit = {
        "target_cities": city_audit,
        "target_city_count": len(city_audit),
        "meizhou": {
            "full_city_included": False,
            "boundary_found": meizhou_row is not None,
            "road_corridor_segments": corridor_count,
            "road_corridor_buffer_m": MEIZHOU_ROAD_BUFFER_M,
            "road_corridor_distance_from_target_m": MEIZHOU_CORRIDOR_DISTANCE_M,
            "corridor_area_km2": round(corridor_area / 1_000_000, 3),
            "full_city_area_km2": round(meizhou_area / 1_000_000, 3),
            "coverage_fraction_of_meizhou": round(corridor_area / meizhou_area, 5) if meizhou_area else 0,
            "policy": "Only mapped major-road corridors near the selected-region border; not the entire Meizhou administrative area.",
        },
        "geometry": {
            "gap_closing_radius_m": GAP_CLOSING_RADIUS_M,
            "outer_safety_buffer_m": OUTER_SAFETY_BUFFER_M,
            "components_before_closing": len(getattr(city_union, "geoms", [city_union])),
            "components_after_closing": len(getattr(closed, "geoms", [closed])),
            "area_km2": round(final_region.area / 1_000_000, 3),
            "bounds_wgs84": [round(v, 7) for v in wgs_geometry.bounds],
        },
        "excluded": ["Yangjiang", "Maoming", "Zhanjiang", "other non-target western Guangdong areas", "Xiamen", "Quanzhou", "other non-target Fujian cities"],
        "sources": [
            "https://download.geofabrik.de/asia/china/guangdong-latest.osm.pbf",
            "https://download.geofabrik.de/asia/china/fujian-latest.osm.pbf",
            "https://www.openstreetmap.org/copyright",
        ],
    }
    (out / "boundary-audit.json").write_text(json.dumps(audit, ensure_ascii=False, indent=2), encoding="utf-8")
    print(json.dumps(audit["geometry"], ensure_ascii=False), flush=True)
    print(f"Selected {len(selected)} target municipal boundaries; Meizhou corridor segments={corridor_count}", flush=True)


if __name__ == "__main__":
    main()
