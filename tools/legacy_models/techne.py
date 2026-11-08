"""Parse legacy (Minecraft 1.2-era, Techne-generated) Java entity models.

Extracts each ModelRenderer part: texture offset, boxes, rotation point, base rotation,
mirror flag and addChild relationships. Coordinates are kept in the legacy Java model space
(y down, the ground at y = 24, angles in radians); conversion to Bedrock/GeckoLib space
happens in ``rig.py``.

Standard library only, so the tool runs without any setup.
"""
from __future__ import annotations

import re
from dataclasses import dataclass, field

_NUM = r"-?\d+(?:\.\d+)?(?:[eE]-?\d+)?"
_F = rf"({_NUM})[Ff]?"


@dataclass
class Box:
    x: float
    y: float
    z: float
    w: int
    h: int
    d: int
    inflate: float = 0.0


@dataclass
class Part:
    name: str
    u: int = 0
    v: int = 0
    boxes: list[Box] = field(default_factory=list)
    pivot: tuple[float, float, float] = (0.0, 0.0, 0.0)
    rotation: tuple[float, float, float] = (0.0, 0.0, 0.0)
    mirror: bool = False
    children: list[str] = field(default_factory=list)


@dataclass
class LegacyModel:
    name: str
    texture_width: int
    texture_height: int
    parts: dict[str, Part]


def _constructor_body(src: str, class_name: str) -> str:
    """The source of the model's no-arg constructor, where Techne puts all geometry."""
    m = re.search(rf"public\s+{re.escape(class_name)}\s*\(\s*\)\s*\{{", src)
    if not m:
        raise ValueError(f"no constructor found for {class_name}")
    depth, i = 1, m.end()
    while depth:
        depth += {"{": 1, "}": -1}.get(src[i], 0)
        i += 1
    return src[m.end():i - 1]


def parse(path: str) -> LegacyModel:
    src = open(path, encoding="utf-8", errors="replace").read()
    # Techne output and later hand edits leave whole parts commented out; drop comments first.
    src = re.sub(r"/\*.*?\*/", "", src, flags=re.S)
    src = re.sub(r"//[^\n]*", "", src)
    # Later hand edits qualify fields with "this." and may create parts in field initialisers.
    src = re.sub(r"\bthis\.(?=\w)", "", src)
    class_name = re.search(r"class\s+(\w+)", src).group(1)
    body = _constructor_body(src, class_name)
    ctor = re.search(rf"public\s+{re.escape(class_name)}\s*\(\s*\)", src).start()
    fields = [f for f in src[:ctor].split(";") if "new ModelRenderer" in f]
    body = ";".join(fields) + ";" + body

    tex_w, tex_h = 64, 32
    if m := re.search(rf"textureWidth\s*=\s*(\d+)", body):
        tex_w = int(m.group(1))
    if m := re.search(rf"textureHeight\s*=\s*(\d+)", body):
        tex_h = int(m.group(1))

    parts: dict[str, Part] = {}
    for stmt in body.split(";"):
        stmt = " ".join(stmt.split())
        if m := re.search(rf"(\w+)\s*=\s*\(?\s*new ModelRenderer\(\s*this\s*,\s*({_NUM})\s*,\s*({_NUM})\s*\)\s*\)?"
                          rf"(?:\.setTextureSize\(\s*(\d+)\s*,\s*(\d+)\s*\))?", stmt):
            parts[m.group(1)] = Part(m.group(1), int(m.group(2)), int(m.group(3)))
            if m.group(4):
                tex_w, tex_h = int(m.group(4)), int(m.group(5))
        elif m := re.search(rf"(\w+)\.addBox\(\s*{_F}\s*,\s*{_F}\s*,\s*{_F}\s*,\s*(\d+)\s*,\s*(\d+)\s*,\s*(\d+)"
                            rf"(?:\s*,\s*{_F})?\s*\)", stmt):
            g = m.groups()
            parts[g[0]].boxes.append(Box(float(g[1]), float(g[2]), float(g[3]), int(g[4]), int(g[5]), int(g[6]),
                                         float(g[7]) if g[7] else 0.0))
        elif m := re.search(rf"(\w+)\.setRotationPoint\(\s*{_F}\s*,\s*{_F}\s*,\s*{_F}\s*\)", stmt):
            parts[m.group(1)].pivot = (float(m.group(2)), float(m.group(3)), float(m.group(4)))
        elif m := re.search(rf"setRotation\(\s*(\w+)\s*,\s*{_F}\s*,\s*{_F}\s*,\s*{_F}\s*\)", stmt):
            parts[m.group(1)].rotation = (float(m.group(2)), float(m.group(3)), float(m.group(4)))
        elif m := re.search(rf"(\w+)\.rotateAngle([XYZ])\s*=\s*{_F}$", stmt):
            rot = list(parts[m.group(1)].rotation)
            rot["XYZ".index(m.group(2))] = float(m.group(3))
            parts[m.group(1)].rotation = tuple(rot)
        elif m := re.search(r"(\w+)\.mirror\s*=\s*(true|false)", stmt):
            parts[m.group(1)].mirror = m.group(2) == "true"
        elif m := re.search(r"(\w+)\.addChild\(\s*(\w+)\s*\)", stmt):
            parts[m.group(1)].children.append(m.group(2))

    return LegacyModel(class_name, tex_w, tex_h, parts)
