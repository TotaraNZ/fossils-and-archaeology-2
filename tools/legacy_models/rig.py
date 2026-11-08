"""Rebuild a legacy Techne model as a GeckoLib rig: geometry, placeholder texture and animations.

Legacy models are flat lists of parts that fake joints by sharing rotation points. A rig spec
(see ``species.py``) groups parts into a real bone hierarchy with pivots at the joints, so
necks and tails bend segment by segment and legs bend at the knee. Each cube keeps its legacy
rest transform (as a per-cube rotation about the legacy rotation point), so the rest pose is
exactly the original model; bones only add the joints animations rotate around.

Output coordinates are GeckoLib/Bedrock file coordinates: y up from the ground, the model
facing -z, +x on the animal's left. Animation values are also in file coordinates.

Standard library only.
"""
from __future__ import annotations

import math
from dataclasses import dataclass, field

import techne

Vec = tuple[float, float, float]


@dataclass
class Bone:
    name: str
    parent: str | None
    parts: list[str]
    # Joint in legacy model space (y down, ground at 24), or a keyword computed from the parts'
    # bounds: "top" (hip/shoulder), "bottom", "front" (-z face), "back" (+z face), "center".
    pivot: Vec | str = "center"
    role: str = ""


@dataclass
class Rig:
    id: str
    source: str
    bones: list[Bone]
    offset: Vec = (0.0, 0.0, 0.0)  # legacy-space shift to centre models Techne left off-axis
    colors: dict = field(default_factory=dict)  # placeholder texture palette


# --- Legacy model space helpers ------------------------------------------------------------

def _rot(v: Vec, r: Vec) -> Vec:
    """Legacy ModelRenderer rotation: v' = Rz * Ry * Rx * v."""
    x, y, z = v
    rx, ry, rz = r
    c, s = math.cos(rx), math.sin(rx)
    y, z = y * c - z * s, y * s + z * c
    c, s = math.cos(ry), math.sin(ry)
    x, z = x * c + z * s, -x * s + z * c
    c, s = math.cos(rz), math.sin(rz)
    x, y = x * c - y * s, x * s + y * c
    return (x, y, z)


def _add(a: Vec, b: Vec) -> Vec:
    return (a[0] + b[0], a[1] + b[1], a[2] + b[2])


def world_parts(model: techne.LegacyModel, offset: Vec) -> dict[str, tuple[Vec, Vec]]:
    """Each part's world rotation point and rotation, flattening addChild chains.

    Child parts are positioned relative to their parent in legacy models. Chains here only ever
    rotate about one shared axis, so the composite is a single rotation about the child's
    world-space rotation point.
    """
    out: dict[str, tuple[Vec, Vec]] = {}

    def visit(name: str, parent: tuple[Vec, Vec] | None) -> None:
        p = model.parts[name]
        if parent is None:
            pivot, rot = _add(p.pivot, offset), p.rotation
        else:
            ppivot, prot = parent
            axes = [i for i in range(3) if abs(prot[i]) > 1e-9 or abs(p.rotation[i]) > 1e-9]
            if len(axes) > 1:
                raise ValueError(f"{name}: nested parts rotating about several axes are not supported")
            pivot = _add(_rot(p.pivot, prot), ppivot)
            rot = tuple(prot[i] + p.rotation[i] for i in range(3))
        out[name] = (pivot, rot)
        for child in p.children:
            if child in model.parts:
                visit(child, out[name])

    children = {c for p in model.parts.values() for c in p.children}
    for name in model.parts:
        if name not in children:
            visit(name, None)
    return out


def part_bounds(model: techne.LegacyModel, transforms, name: str) -> tuple[Vec, Vec]:
    p = model.parts[name]
    pivot, rot = transforms[name]
    pts = []
    for b in p.boxes:
        for dx in (0, b.w):
            for dy in (0, b.h):
                for dz in (0, b.d):
                    pts.append(_add(_rot((b.x + dx, b.y + dy, b.z + dz), rot), pivot))
    lo = tuple(min(q[i] for q in pts) for i in range(3))
    hi = tuple(max(q[i] for q in pts) for i in range(3))
    return lo, hi


# --- Conversion to GeckoLib/Bedrock file space -----------------------------------------------

def to_file_point(v: Vec) -> Vec:
    """Legacy model space -> Bedrock file space: x and y flip, the ground moves to y = 0."""
    return (-v[0], 24.0 - v[1], v[2])


def _r(v: float) -> float:
    v = round(v, 4)
    return 0.0 if v == 0 else v


def build_geometry(rig: Rig, model: techne.LegacyModel, tag_parts: bool = False) -> dict:
    """Bedrock geometry for the rig; tag_parts adds each cube's legacy part name as "_part"."""
    transforms = world_parts(model, rig.offset)
    assigned = {p for b in rig.bones for p in b.parts}
    missing = set(model.parts) - assigned
    if missing:
        raise ValueError(f"{rig.id}: parts not assigned to a bone: {sorted(missing)}")

    bones_json = []
    for bone in rig.bones:
        if isinstance(bone.pivot, tuple):
            pivot_legacy = _add(bone.pivot, rig.offset)
        else:
            if not bone.parts:
                raise ValueError(f"{rig.id}.{bone.name}: keyword pivot needs parts")
            bounds = [part_bounds(model, transforms, n) for n in bone.parts]
            lo = tuple(min(b[0][i] for b in bounds) for i in range(3))
            hi = tuple(max(b[1][i] for b in bounds) for i in range(3))
            c = tuple((lo[i] + hi[i]) / 2 for i in range(3))
            pivot_legacy = {
                "center": c,
                "top": (c[0], lo[1], c[2]),
                "bottom": (c[0], hi[1], c[2]),
                "front": (c[0], c[1], lo[2]),
                "back": (c[0], c[1], hi[2]),
            }[bone.pivot]
        entry = {"name": bone.name, "pivot": [_r(v) for v in to_file_point(pivot_legacy)]}
        if bone.parent:
            entry["parent"] = bone.parent

        cubes = []
        for name in bone.parts:
            part = model.parts[name]
            ppivot, prot = transforms[name]
            for box in part.boxes:
                lo = _add(ppivot, (box.x, box.y, box.z))
                hi = _add(lo, (box.w, box.h, box.d))
                cube = {
                    # Flipping x and y turns the legacy min corner into the Bedrock max corner.
                    "origin": [_r(-hi[0]), _r(24.0 - hi[1]), _r(lo[2])],
                    "size": [box.w, box.h, box.d],
                    "uv": [part.u, part.v],
                }
                if box.inflate:
                    cube["inflate"] = box.inflate
                if any(abs(a) > 1e-9 for a in prot):
                    cube["pivot"] = [_r(v) for v in to_file_point(ppivot)]
                    # Legacy angles carry over unchanged: flipping x and y negates the x and y
                    # rotations, and Bedrock files store those two negated again (GeckoLib and
                    # Blockbench flip them back on load). Verified against Blockbench renders.
                    cube["rotation"] = [_r(math.degrees(a)) for a in prot]
                if part.mirror:
                    cube["mirror"] = True
                if tag_parts:
                    cube["_part"] = name
                cubes.append(cube)
        if cubes:
            entry["cubes"] = cubes
        bones_json.append(entry)

    return {
        "format_version": "1.12.0",
        "minecraft:geometry": [{
            "description": {
                "identifier": f"geometry.{rig.id}",
                "texture_width": model.texture_width,
                "texture_height": model.texture_height,
            },
            "bones": bones_json,
        }],
    }
