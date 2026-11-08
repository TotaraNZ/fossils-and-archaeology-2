"""Procedural GeckoLib animations for the converted rigs, driven by bone roles.

Values are GeckoLib file coordinates in degrees: +X pitches a forward-pointing bone down (so a
hanging leg's foot swings back), +Y yaws, +Z rolls the animal's left (+x) side up. Loops are
sampled into catmull-rom keyframes so they stay editable in Blockbench.
"""
from __future__ import annotations

import math

from rig import Rig

TAU = 2 * math.pi
SAMPLES = 8


def _bones(rig: Rig, role_prefix: str) -> list[str]:
    return [b.name for b in rig.bones if b.role.startswith(role_prefix)]


def _bone(rig: Rig, role: str) -> str | None:
    for b in rig.bones:
        if b.role == role:
            return b.name
    return None


class Clip:
    def __init__(self, length: float, loop: bool):
        self.length = length
        self.loop = loop
        self.bones: dict[str, dict[str, dict[float, list[float]]]] = {}

    def key(self, bone: str | None, channel: str, t: float, value) -> None:
        if bone is None:
            return
        frames = self.bones.setdefault(bone, {}).setdefault(channel, {})
        old = frames.get(round(t, 4), [0.0, 0.0, 0.0])
        frames[round(t, 4)] = [round(old[i] + value[i], 3) for i in range(3)]

    def wave(self, bone: str | None, channel: str, fn) -> None:
        """Sample fn(phase) -> [x, y, z] over one loop."""
        for i in range(SAMPLES + 1):
            phase = TAU * i / SAMPLES
            self.key(bone, channel, self.length * i / SAMPLES, fn(phase))

    def to_json(self) -> dict:
        out = {"loop": self.loop, "animation_length": self.length, "bones": {}}
        for bone, channels in self.bones.items():
            out["bones"][bone] = {
                ch: {f"{t:g}": {"vector": v, "lerp_mode": "catmullrom"} for t, v in sorted(frames.items())}
                for ch, frames in channels.items()
            }
        return out


def _is_quadruped(rig: Rig) -> bool:
    return bool(_bones(rig, "front_leg_"))


def _is_swimmer(rig: Rig) -> bool:
    return bool(_bones(rig, "front_flipper_"))


def walk(rig: Rig, period: float, stride: float) -> Clip:
    c = Clip(period, True)
    body = _bone(rig, "body")
    tails = _bones(rig, "tail")
    necks = _bones(rig, "neck")

    if _is_swimmer(rig):
        # Swimming: paddle the flippers in diagonal pairs and drive with a travelling tail wave.
        for side, sign in (("l", 1), ("r", -1)):
            c.wave(_bone(rig, f"front_flipper_{side}"), "rotation",
                   lambda p, s=sign: [stride * math.sin(p + (0 if s > 0 else math.pi)), 0, s * stride * 0.6 * math.cos(p)])
            c.wave(_bone(rig, f"back_flipper_{side}"), "rotation",
                   lambda p, s=sign: [stride * 0.8 * math.sin(p + (math.pi if s > 0 else 0)), 0, s * stride * 0.5 * math.cos(p)])
        c.wave(body, "rotation", lambda p: [0, 3 * math.sin(p), 0])
        for i, t in enumerate(tails):
            c.wave(t, "rotation", lambda p, i=i: [0, (8 + 6 * i) * math.sin(p - 0.7 * (i + 1)), 0])
        for i, n in enumerate(necks):
            c.wave(n, "rotation", lambda p, i=i: [0, -2 * math.sin(p - 0.4 * i), 0])
        return c

    if _is_quadruped(rig):
        # Diagonal gait: front-left with back-right.
        legs = {"front_leg_l": 0.0, "back_leg_r": 0.0, "front_leg_r": math.pi, "back_leg_l": math.pi}
    else:
        legs = {"back_leg_l": 0.0, "back_leg_r": math.pi}
    for role, offset in legs.items():
        upper = _bone(rig, role)
        lower = _bone(rig, role.replace("_leg_", "_lower_"))
        foot = _bone(rig, "foot_" + role[-1]) if role.startswith("back") else None
        c.wave(upper, "rotation", lambda p, o=offset: [stride * math.sin(p + o), 0, 0])
        # The lower leg trails the upper one, flexing as the foot swings forward.
        c.wave(lower, "rotation", lambda p, o=offset: [stride * 0.6 * max(0.0, math.sin(p + o - math.pi / 2)), 0, 0])
        if foot:
            c.wave(foot, "rotation", lambda p, o=offset: [-stride * 0.5 * math.sin(p + o), 0, 0])

    for side, offset in (("l", math.pi), ("r", 0.0)):  # arms swing against the legs
        c.wave(_bone(rig, f"arm_{side}"), "rotation", lambda p, o=offset: [stride * 0.3 * math.sin(p + o), 0, 0])
        c.wave(_bone(rig, f"wing_{side}"), "rotation", lambda p, o=offset: [stride * 0.2 * math.sin(p + o), 0, 0])

    c.wave(body, "position", lambda p: [0, 0.35 * (1 - math.cos(2 * p)) / 2, 0])
    c.wave(body, "rotation", lambda p: [0, 2 * math.sin(p), 1.5 * math.sin(p)])
    for i, t in enumerate(tails):
        c.wave(t, "rotation", lambda p, i=i: [1.5 * math.sin(2 * p), (4 + 2 * i) * math.sin(p - 0.6 * (i + 1)), 0])
    for i, n in enumerate(necks):
        c.wave(n, "rotation", lambda p, i=i: [1.5 * math.sin(2 * p - 0.3 * i), -1.5 * math.sin(p), 0])
    c.wave(_bone(rig, "head"), "rotation", lambda p: [-2 * math.sin(2 * p), 1.5 * math.sin(p), 0])
    return c


def idle(rig: Rig) -> Clip:
    c = Clip(4.0, True)
    c.wave(_bone(rig, "chest") or _bone(rig, "body"), "rotation", lambda p: [-1.2 * math.sin(p), 0, 0])
    for i, n in enumerate(_bones(rig, "neck")):
        c.wave(n, "rotation", lambda p, i=i: [1.5 * math.sin(p - 0.4 * i), 2 * math.sin(p / 1 + 1.0), 0])
    c.wave(_bone(rig, "head"), "rotation", lambda p: [-1.5 * math.sin(p - 0.8), 3 * math.sin(p + 2.0), 0])
    for i, t in enumerate(_bones(rig, "tail")):
        c.wave(t, "rotation", lambda p, i=i: [0, (2 + i) * math.sin(p - 0.5 * i), 0])
    c.wave(_bone(rig, "jaw"), "rotation", lambda p: [1.5 * (1 - math.cos(p)) / 2, 0, 0])
    for side, s in (("l", 1), ("r", -1)):
        c.wave(_bone(rig, f"wing_{side}"), "rotation", lambda p, s=s: [0, 0, s * 2 * math.sin(p)])
        c.wave(_bone(rig, f"front_flipper_{side}"), "rotation", lambda p, s=s: [3 * math.sin(p), 0, s * 3 * math.cos(p)])
    return c


def bite(rig: Rig) -> Clip:
    """Lunge with the neck and snap the jaw: carnivores, swimmers and the pterosaur."""
    c = Clip(0.6, False)
    necks = _bones(rig, "neck") or [_bone(rig, "chest")]
    for t, neck, head, jaw in ((0, 0, 0, 0), (0.15, -8, -10, 30), (0.3, 12, 14, 0), (0.6, 0, 0, 0)):
        for n in necks:
            c.key(n, "rotation", t, [neck / max(1, len(necks)) * 1.5, 0, 0])
        c.key(_bone(rig, "head"), "rotation", t, [head, 0, 0])
        c.key(_bone(rig, "jaw"), "rotation", t, [jaw, 0, 0])
    for side in ("l", "r"):  # raptors kick with the sickle claw as they strike
        for t, v in ((0, 0), (0.2, -45), (0.35, 20), (0.6, 0)):
            c.key(_bone(rig, f"claw_{side}"), "rotation", t, [v, 0, 0])
    return c


def tail_swipe(rig: Rig) -> Clip:
    c = Clip(0.8, False)
    for i, tail in enumerate(_bones(rig, "tail")):
        for t, v in ((0, 0), (0.25, -15), (0.45, 35), (0.8, 0)):
            c.key(tail, "rotation", t, [0, v * (1 + 0.3 * i), 0])
    for t, v in ((0, 0), (0.25, 4), (0.45, -8), (0.8, 0)):
        c.key(_bone(rig, "body"), "rotation", t, [0, v, 0])
    return c


def stomp(rig: Rig) -> Clip:
    """Rear up on the hind legs and bring the front feet down."""
    c = Clip(1.0, False)
    for t, chest, legs in ((0, 0, 0), (0.4, -14, -30), (0.6, -14, -30), (0.75, 4, 10), (1.0, 0, 0)):
        c.key(_bone(rig, "chest"), "rotation", t, [chest, 0, 0])
        for side in ("l", "r"):
            c.key(_bone(rig, f"front_leg_{side}"), "rotation", t, [legs, 0, 0])
    return c


def eat(rig: Rig) -> Clip:
    c = Clip(1.2, True)
    necks = _bones(rig, "neck")
    for n in necks:
        c.wave(n, "rotation", lambda p: [(22 + 6 * math.sin(2 * p)) / len(necks), 0, 0])
    c.wave(_bone(rig, "head"), "rotation", lambda p: [10 + 8 * math.sin(2 * p), 0, 0])
    c.wave(_bone(rig, "jaw"), "rotation", lambda p: [8 * max(0.0, math.sin(2 * p)), 0, 0])
    return c


def look(rig: Rig) -> Clip:
    c = Clip(3.0, False)
    for t, y in ((0, 0), (0.6, 30), (1.4, 30), (2.0, -30), (2.6, -30), (3.0, 0)):
        c.key(_bone(rig, "head"), "rotation", t, [0, y * 0.6, 0])
        for n in _bones(rig, "neck"):
            c.key(n, "rotation", t, [0, y * 0.4 / max(1, len(_bones(rig, "neck"))), 0])
    return c


def roar(rig: Rig) -> Clip:
    c = Clip(1.6, False)
    for t, head, jaw in ((0, 0, 0), (0.4, -25, 0), (0.6, -30, 40), (1.2, -30, 40), (1.6, 0, 0)):
        c.key(_bone(rig, "head"), "rotation", t, [head, 0, 0])
        c.key(_bone(rig, "jaw"), "rotation", t, [jaw, 0, 0])
        c.key(_bone(rig, "frill"), "rotation", t, [0, 0, 0])
        c.key(_bone(rig, "frill"), "scale", t, [1 + jaw / 80, 1 + jaw / 80, 1])
    return c


def flap(rig: Rig) -> Clip:
    """Spread and beat the wings (a display on the ground)."""
    c = Clip(1.6, False)
    for t, wing, tip in ((0, 0, 0), (0.3, 50, -60), (0.55, 70, -80), (0.8, 30, -40), (1.05, 70, -80), (1.6, 0, 0)):
        c.key(_bone(rig, "wing_l"), "rotation", t, [0, 0, wing])
        c.key(_bone(rig, "wing_r"), "rotation", t, [0, 0, -wing])
        c.key(_bone(rig, "wingtip_l"), "rotation", t, [0, tip, 0])
        c.key(_bone(rig, "wingtip_r"), "rotation", t, [0, -tip, 0])
    return c


def build(rig: Rig, period: float, stride: float, attack: str, extras: list[str]) -> dict:
    clips = {"walk": walk(rig, period, stride), "idle": idle(rig)}
    clips[{"bite": "bite", "tail_swipe": "tail_swipe", "stomp": "stomp"}[attack]] = \
        {"bite": bite, "tail_swipe": tail_swipe, "stomp": stomp}[attack](rig)
    for name in extras:
        clips[name] = {"eat": eat, "look": look, "roar": roar, "flap": flap}[name](rig)
    return {"format_version": "1.8.0",
            "animations": {name: clip.to_json() for name, clip in clips.items()}}
