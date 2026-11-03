#!/usr/bin/env python3
"""Require actual managed-device PNG evidence; retain partial captures for failed-run diagnosis."""
import json
import shutil
import struct
from pathlib import Path


def collect(outputs=Path("build/outputs"), destination=Path("build/reports/heart-rate-ui")):
    sources = list(outputs.glob("**/heart-rate-ui"))
    if len(sources) != 1:
        raise ValueError(f"Expected one managed-device screenshot directory, found {len(sources)}")
    source = sources[0]
    destination.mkdir(parents=True, exist_ok=True)
    for path in source.iterdir():
        if path.suffix in {".png", ".json"}:
            shutil.copy2(path, destination / path.name)
    manifest = json.loads((source / "manifest.json").read_text())
    if manifest["physicalBleTested"] is not False or manifest["timezone"] != "Asia/Singapore":
        raise ValueError("Incorrect screenshot provenance/timezone")
    suffixes = ("home", "recording-connected", "recording-stale", "recording-reconnecting",
                "history", "time-chart", "distance-chart", "settings", "scan", "scan-fixture")
    expected = {f"{size}-{theme}-{suffix}.png" for size in ("phone", "narrow")
                for theme in ("light", "dark") for suffix in suffixes}
    captures = manifest["screenshots"]
    if len(captures) != 40 or {c["file"] for c in captures} != expected:
        raise ValueError("Incomplete UI scenario evidence")
    widths = set()
    for capture in captures:
        name = capture["file"]
        payload = (source / name).read_bytes()
        if payload[:8] != b"\x89PNG\r\n\x1a\n" or payload[12:16] != b"IHDR":
            raise ValueError(f"Invalid screenshot: {name}")
        width, height = struct.unpack(">II", payload[16:24])
        if (width, height) != (capture["width"], capture["height"]) or width < 600 or height < 1000:
            raise ValueError(f"Incorrect screenshot dimensions: {name}")
        if capture["nightMode"] != (32 if "-dark-" in name else 16):
            raise ValueError(f"Theme was not applied: {name}")
        if "narrow-" in name and capture["widthDp"] > 360:
            raise ValueError("Narrow-window scenario was not applied")
        widths.add(width)
    if len(widths) < 2:
        raise ValueError("Both screenshot scenarios used the same physical dimensions")
    print("40 actual UI screenshots verified: four theme/window profiles; synthetic fixtures explicitly identified.")


if __name__ == "__main__":
    collect()
