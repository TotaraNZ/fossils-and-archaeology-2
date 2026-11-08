"""Placeholder textures for converted rigs, painted onto each cube's box-UV net.

The legacy textures were never published with the source, so this paints a readable base:
darker backs, lighter bellies, an accent colour on display parts (plates, crests, claws),
shaded faces and a little deterministic mottling. The UV layout matches the model, so the
result can be repainted in Blockbench directly.

Standard library only (writes PNGs with zlib).
"""
from __future__ import annotations

import random
import struct
import zlib


def write_png(path: str, width: int, height: int, pixels: list[list[tuple[int, int, int, int]]]) -> None:
    raw = b"".join(b"\x00" + bytes(c for px in row for c in px) for row in pixels)

    def chunk(tag: bytes, data: bytes) -> bytes:
        return struct.pack(">I", len(data)) + tag + data + struct.pack(">I", zlib.crc32(tag + data) & 0xFFFFFFFF)

    with open(path, "wb") as f:
        f.write(b"\x89PNG\r\n\x1a\n")
        f.write(chunk(b"IHDR", struct.pack(">IIBBBBB", width, height, 8, 6, 0, 0, 0)))
        f.write(chunk(b"IDAT", zlib.compress(raw, 9)))
        f.write(chunk(b"IEND", b""))


def _shade(c: tuple[int, int, int], f: float) -> tuple[int, int, int]:
    return tuple(max(0, min(255, int(v * f))) for v in c)


def paint(geometry: dict, colors: dict, accent_parts: set[str], seed: str) -> tuple[int, int, list]:
    """Paint every cube; cubes tagged ("_part") with a name in accent_parts get the accent colour."""
    desc = geometry["minecraft:geometry"][0]["description"]
    w, h = desc["texture_width"], desc["texture_height"]
    px = [[(0, 0, 0, 0) for _ in range(w)] for _ in range(h)]
    rnd = random.Random(seed)

    def fill(x0: int, y0: int, fw: int, fh: int, base: tuple[int, int, int], light: float) -> None:
        for y in range(max(0, y0), min(h, y0 + fh)):
            for x in range(max(0, x0), min(w, x0 + fw)):
                edge = x in (x0, x0 + fw - 1) or y in (y0, y0 + fh - 1)
                f = light * (0.82 if edge else 1.0) * (0.92 + 0.16 * rnd.random())
                px[y][x] = (*_shade(base, f), 255)

    for bone in geometry["minecraft:geometry"][0]["bones"]:
        for cube in bone.get("cubes", []):
            accent = cube.get("_part") in accent_parts
            u, v = cube["uv"]
            sx, sy, sz = (max(0, int(round(s))) for s in cube["size"])
            top = colors["accent"] if accent else _shade(colors["base"], 0.85)
            side = colors["accent"] if accent else colors["base"]
            fill(u + sz, v, sx, sz, top, 1.1)                       # up
            fill(u + sz + sx, v, sx, sz, colors["belly"], 1.0)      # down
            fill(u, v + sz, sz, sy, side, 0.9)                      # side
            fill(u + sz, v + sz, sx, sy, side, 1.0)                 # front
            fill(u + sz + sx, v + sz, sz, sy, side, 0.9)            # side
            fill(u + 2 * sz + sx, v + sz, sx, sy, side, 0.8)        # back
            # Lighter underside band along the bottom of the side faces.
            if not accent and sy >= 3:
                fill(u, v + sz + sy - 1, 2 * (sx + sz), 1, colors["belly"], 0.95)
    return w, h, px


def read_png(path: str) -> tuple[int, int, list[list[tuple[int, int, int, int]]]]:
    """Decode an 8-bit, non-interlaced RGB/RGBA PNG (enough for this mod's textures)."""
    data = open(path, "rb").read()
    pos, idat, w = 8, b"", 0
    while pos < len(data):
        length = struct.unpack(">I", data[pos:pos + 4])[0]
        tag, body = data[pos + 4:pos + 8], data[pos + 8:pos + 8 + length]
        if tag == b"IHDR":
            w, h, depth, ctype, _, _, interlace = struct.unpack(">IIBBBBB", body)
            if depth != 8 or ctype not in (2, 6) or interlace:
                raise ValueError(f"unsupported PNG {path}")
            bpp = 4 if ctype == 6 else 3
        elif tag == b"IDAT":
            idat += body
        pos += 12 + length
    raw, stride = zlib.decompress(idat), w * bpp
    rows, prev, i = [], bytearray(stride), 0
    for _ in range(h):
        ftype, line = raw[i], bytearray(raw[i + 1:i + 1 + stride])
        i += 1 + stride
        for x in range(stride):
            a = line[x - bpp] if x >= bpp else 0
            b, c = prev[x], prev[x - bpp] if x >= bpp else 0
            if ftype == 1:
                line[x] = (line[x] + a) & 255
            elif ftype == 2:
                line[x] = (line[x] + b) & 255
            elif ftype == 3:
                line[x] = (line[x] + (a + b) // 2) & 255
            elif ftype == 4:
                p = a + b - c
                pa, pb, pc = abs(p - a), abs(p - b), abs(p - c)
                line[x] = (line[x] + (a if pa <= pb and pa <= pc else b if pb <= pc else c)) & 255
        rows.append([tuple(line[x:x + bpp]) + ((255,) if bpp == 3 else ()) for x in range(0, stride, bpp)])
        prev = line
    return w, h, rows


def tint(src: str, dst: str, color: tuple[int, int, int], strength: float = 0.75) -> None:
    """Recolour a texture toward color, keeping its shading (luminance) and alpha."""
    w, h, px = read_png(src)
    out = []
    for row in px:
        new = []
        for r, g, b, a in row:
            lum = (0.299 * r + 0.587 * g + 0.114 * b) / 255
            t = tuple(int(lum * 1.6 * c) for c in color)
            new.append(tuple(max(0, min(255, int(o * (1 - strength) + n * strength))) for o, n in zip((r, g, b), t)) + (a,))
        out.append(new)
    write_png(dst, w, h, out)
