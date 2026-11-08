#!/usr/bin/env python3
"""Convert FlammaRilva's original Fossils & Archaeology models into GeckoLib assets.

Usage:
    python3 tools/legacy_models/convert.py <path to FossilArcheology/src> [species ...]

Writes, per species, into src/main/resources/assets/fossilsarch2/:
    geckolib/models/<id>.geo.json
    geckolib/animations/<id>.animation.json
    textures/entity/<id>/<id>.png        (placeholder, only if no texture exists yet)

The rigs live in species.py; see rig.py and anim.py for how bones and animations are built.
Standard library only.
"""
from __future__ import annotations

import json
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)

import anim  # noqa: E402
import rig  # noqa: E402
import species  # noqa: E402
import techne  # noqa: E402
import texture  # noqa: E402

ASSETS = os.path.join(HERE, "..", "..", "src", "main", "resources", "assets", "fossilsarch2")

# Per-species motion: walk loop length (s), stride (deg), attack style, extra clips.
MOTION = {
    "stegosaurus": (1.2, 16, "tail_swipe", ["eat", "look"]),
    "brachiosaurus": (1.8, 14, "stomp", ["eat", "look"]),
    "dilophosaurus": (0.8, 28, "bite", ["roar"]),
    "velociraptor": (0.6, 32, "bite", ["roar"]),
    "pterosaur": (0.7, 24, "bite", ["flap"]),
    "mosasaurus": (1.4, 20, "bite", []),
    "plesiosaurus": (1.6, 22, "bite", []),
}
# Legacy parts painted in the species' accent colour: plates, crests, frills, claws.
ACCENT_PARTS = {
    "stegosaurus": {"T1", "T2", "T3", "T4"},
    "dilophosaurus": {"Crest", "Crest2", "Crest3", "Crest4", "Frill1", "Frill2"},
    "velociraptor": {"left_nail_1", "left_nail_2", "right_nail_1", "right_nail_2"},
    "pterosaur": {"crown"},
    "mosasaurus": {"Tail_2_dec", "Tail_3_dec", "Upper_Teeth"},
}


def _write_json(path: str, data: dict) -> None:
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        json.dump(data, f, indent=2)
        f.write("\n")


def convert(src_dir: str, r: rig.Rig) -> None:
    model = techne.parse(os.path.join(src_dir, r.source + ".java"))
    geometry = rig.build_geometry(r, model)
    _write_json(os.path.join(ASSETS, "geckolib", "models", f"{r.id}.geo.json"), geometry)

    period, stride, attack, extras = MOTION[r.id]
    _write_json(os.path.join(ASSETS, "geckolib", "animations", f"{r.id}.animation.json"),
                anim.build(r, period, stride, attack, extras))

    tex_path = os.path.join(ASSETS, "textures", "entity", r.id, f"{r.id}.png")
    if not os.path.exists(tex_path):
        os.makedirs(os.path.dirname(tex_path), exist_ok=True)
        w, h, px = texture.paint(rig.build_geometry(r, model, tag_parts=True), r.colors,
                                 ACCENT_PARTS.get(r.id, set()), r.id)
        texture.write_png(tex_path, w, h, px)
    print(f"{r.id}: {len(r.bones)} bones")


def main() -> None:
    src_dir = sys.argv[1]
    wanted = set(sys.argv[2:])
    for r in species.ALL:
        if not wanted or r.id in wanted:
            convert(src_dir, r)


if __name__ == "__main__":
    main()
