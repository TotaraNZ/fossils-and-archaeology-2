#!/usr/bin/env python3
"""Generate a GameTest pen structure: a grass floor walled with barriers.

Usage: python3 tools/make_pen_structure.py <size> <out.nbt>
Writes a gzipped NBT structure template of size x 5 x size. DataVersion matches Minecraft 26.3.
"""
import gzip
import struct
import sys

DATA_VERSION = 5023
HEIGHT = 5


def _str(x):
    b = x.encode()
    return struct.pack('>H', len(b)) + b


def _tag(tag_type, name, payload):
    return bytes([tag_type]) + _str(name) + payload


def _int(v):
    return struct.pack('>i', v)


def _list(tag_type, items):
    return bytes([tag_type]) + _int(len(items)) + b''.join(items)


def _compound(fields):
    return b''.join(fields) + b'\x00'


def _block(x, y, z, state):
    return _compound([_tag(9, 'pos', _list(3, [_int(x), _int(y), _int(z)])), _tag(3, 'state', _int(state))])


def build(size):
    palette = [_compound([_tag(8, 'Name', _str('minecraft:grass_block'))]),
               _compound([_tag(8, 'Name', _str('minecraft:barrier'))])]
    blocks = []
    for x in range(size):
        for z in range(size):
            blocks.append(_block(x, 0, z, 0))
            if x in (0, size - 1) or z in (0, size - 1):
                blocks.extend(_block(x, y, z, 1) for y in range(1, HEIGHT))
    return _tag(10, '', _compound([
        _tag(3, 'DataVersion', _int(DATA_VERSION)),
        _tag(9, 'size', _list(3, [_int(size), _int(HEIGHT), _int(size)])),
        _tag(9, 'palette', _list(10, palette)),
        _tag(9, 'blocks', _list(10, blocks)),
        _tag(9, 'entities', _list(10, [])),
    ]))


if __name__ == '__main__':
    size, out = int(sys.argv[1]), sys.argv[2]
    with open(out, 'wb') as f:
        f.write(gzip.compress(build(size)))
