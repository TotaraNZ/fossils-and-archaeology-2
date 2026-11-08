#!/usr/bin/env python3
"""Register converted species in the mod: species data, sounds, lang, item and egg textures.

Usage: python3 tools/legacy_models/species_assets.py
Idempotent; existing textures are left alone so hand-painted replacements survive reruns.
"""
import json
import os
import shutil
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import species  # noqa: E402
import texture  # noqa: E402

RES = os.path.join(HERE, "..", "..", "src", "main", "resources")
ASSETS = os.path.join(RES, "assets", "fossilsarch2")
DATA = os.path.join(RES, "data", "fossilsarch2", "fossilsarch2", "dinosaurs")

# "sounds" maps each kind to original Fossils & Archaeology clips (in assets/fossilsarch2/sounds),
# as a name or (name, pitch); species with no recorded death call reuse the hurt clip lower.
SPECIES = {
    "stegosaurus": {
        "display_name": "Stegosaurus", "diet": "herbivore",
        "combat": {"health": 50.0, "speed": 0.2, "attack_damage": 8.0, "combat_modifier": 1.2,
                   "confidence": 1.1, "defensive_progress": 0.45, "fear_ratio": 1.6},
        "growth": {"width": 1.0, "height": 0.9, "baby_scale": 0.4, "adult_scale": 2.6,
                   "max_age": 12, "grow_time": 12000, "hatch_time": 3000},
        "food": {"meat_nutrition": 4, "meat_saturation": 0.4, "cooked_meat_nutrition": 8,
                 "cooked_meat_saturation": 0.8, "meat_drop_count": 4},
        "animations": {"attack": "tail_swipe", "special": ["look"]},
        "sounds": {"ambient": ["stegosaurus_living1", "stegosaurus_living2", "stegosaurus_living3"],
                   "hurt": ["stegosaurus_hurt"], "death": ["stegosaurus_death"]},
    },
    "brachiosaurus": {
        "display_name": "Brachiosaurus", "diet": "herbivore",
        "combat": {"health": 100.0, "speed": 0.18, "attack_damage": 10.0, "combat_modifier": 1.3,
                   "confidence": 1.3, "defensive_progress": 0.5, "fear_ratio": 2.0},
        "growth": {"width": 1.0, "height": 2.0, "baby_scale": 0.35, "adult_scale": 2.0,
                   "max_age": 16, "grow_time": 12000, "hatch_time": 4000},
        "food": {"meat_nutrition": 5, "meat_saturation": 0.5, "cooked_meat_nutrition": 10,
                 "cooked_meat_saturation": 1.0, "meat_drop_count": 6},
        "animations": {"attack": "stomp", "special": ["look"]},
        "sounds": {"ambient": ["brachiosaurus_living1", "brachiosaurus_living2"],
                   "hurt": ["brachiosaurus_hurt1"], "death": ["brachiosaurus_death1"]},
    },
    "dilophosaurus": {
        "display_name": "Dilophosaurus", "diet": "carnivore",
        "combat": {"health": 30.0, "speed": 0.27, "attack_damage": 6.0, "combat_modifier": 1.1,
                   "confidence": 1.1, "chase_speed": 1.4, "min_hunt_progress": 0.5,
                   "player_aggression_progress": 0.8, "prey_ratio_max": 0.85, "fear_ratio": 1.4},
        "growth": {"width": 0.9, "height": 1.4, "baby_scale": 0.35, "adult_scale": 1.3,
                   "max_age": 8, "grow_time": 12000, "hatch_time": 3000},
        "food": {"meat_nutrition": 3, "meat_saturation": 0.3, "cooked_meat_nutrition": 7,
                 "cooked_meat_saturation": 0.7, "meat_drop_count": 3},
        "animations": {"eat": "", "special": ["roar"]},
        "variants": ["", "lime"],
        "sounds": {"ambient": ["dilophosaurus_living", "dilophosaurus_call1", "dilophosaurus_call2"],
                   "hurt": ["dilophosaurus_hurt"], "death": ["dilophosaurus_death"]},
    },
    "velociraptor": {
        "display_name": "Velociraptor", "diet": "carnivore",
        "combat": {"health": 22.0, "speed": 0.33, "attack_damage": 5.0, "combat_modifier": 1.3,
                   "confidence": 1.3, "chase_speed": 1.6, "min_hunt_progress": 0.45,
                   "player_aggression_progress": 0.75, "prey_ratio_max": 0.9, "fear_ratio": 1.3},
        "growth": {"width": 0.8, "height": 1.3, "baby_scale": 0.35, "adult_scale": 1.0,
                   "max_age": 8, "grow_time": 9000, "hatch_time": 2400},
        "food": {"meat_nutrition": 3, "meat_saturation": 0.3, "cooked_meat_nutrition": 6,
                 "cooked_meat_saturation": 0.6, "meat_drop_count": 2},
        "animations": {"eat": "", "special": ["roar"]},
        "variants": ["", "green", "blue"],
        "sounds": {"ambient": ["velociraptor_living_wild1", "velociraptor_living_wild2"],
                   "hurt": ["velociraptor_hurt1", "velociraptor_hurt2", "velociraptor_hurt3", "velociraptor_hurt4"],
                   "death": [("velociraptor_hurt2", 0.75)]},
    },
    "pterosaur": {
        "display_name": "Pterosaur", "diet": "carnivore",
        "combat": {"health": 16.0, "speed": 0.26, "attack_damage": 3.0, "combat_modifier": 0.9,
                   "confidence": 0.9, "min_hunt_progress": 0.6, "player_aggression_progress": 1.0,
                   "prey_ratio_max": 0.7, "fear_ratio": 1.2},
        "growth": {"width": 0.9, "height": 0.9, "baby_scale": 0.4, "adult_scale": 1.2,
                   "max_age": 8, "grow_time": 9000, "hatch_time": 2400},
        "food": {"meat_nutrition": 2, "meat_saturation": 0.2, "cooked_meat_nutrition": 5,
                 "cooked_meat_saturation": 0.5, "meat_drop_count": 2},
        "animations": {"eat": "", "special": ["flap"]},
        "sounds": {"ambient": ["pterosaur_living1", "pterosaur_living2"],
                   "hurt": ["pterosaur_hurt"], "death": [("pterosaur_hurt", 0.75)]},
    },
    "mosasaurus": {
        "display_name": "Mosasaurus", "diet": "carnivore", "habitat": "aquatic",
        "combat": {"health": 60.0, "speed": 0.32, "attack_damage": 9.0, "combat_modifier": 1.3,
                   "confidence": 1.4, "chase_speed": 1.5, "min_hunt_progress": 0.4,
                   "player_aggression_progress": 0.8, "prey_ratio_max": 0.9, "fear_ratio": 1.6},
        "growth": {"width": 1.2, "height": 0.8, "baby_scale": 0.35, "adult_scale": 2.5,
                   "max_age": 12, "grow_time": 12000, "hatch_time": 3000},
        "food": {"meat_nutrition": 4, "meat_saturation": 0.4, "cooked_meat_nutrition": 8,
                 "cooked_meat_saturation": 0.8, "meat_drop_count": 4},
        "animations": {"eat": ""},
        "sounds": {"ambient": ["mosasaurus_living1", "mosasaurus_living2"],
                   "hurt": ["mosasaurus_hurt"], "death": ["mosasaurus_death1", "mosasaurus_death2"]},
    },
    "plesiosaurus": {
        "display_name": "Plesiosaurus", "diet": "carnivore", "habitat": "aquatic",
        "combat": {"health": 35.0, "speed": 0.28, "attack_damage": 5.0, "combat_modifier": 1.0,
                   "confidence": 1.0, "chase_speed": 1.4, "min_hunt_progress": 0.4,
                   "player_aggression_progress": 1.0, "prey_ratio_max": 0.8, "fear_ratio": 1.3},
        "growth": {"width": 1.0, "height": 0.9, "baby_scale": 0.35, "adult_scale": 1.8,
                   "max_age": 10, "grow_time": 10000, "hatch_time": 3000},
        "food": {"meat_nutrition": 3, "meat_saturation": 0.3, "cooked_meat_nutrition": 7,
                 "cooked_meat_saturation": 0.7, "meat_drop_count": 3},
        "animations": {"eat": ""},
        "sounds": {"ambient": ["plesiosaur_living"], "hurt": ["plesiosaur_hurt"],
                   "death": [("plesiosaur_hurt", 0.75)]},
    },
}

EXISTING_SOUNDS = {
    "triceratops": {"ambient": ["triceratops_living1", "triceratops_living2", "triceratops_living3"],
                    "hurt": ["triceratops_hurt"], "death": ["triceratops_death"]},
    "tyrannosaurus": {"ambient": ["tyrannosaurus_living1", "tyrannosaurus_living2", "tyrannosaurus_living3"],
                      "hurt": ["tyrannosaurus_hurt"], "death": ["tyrannosaurus_death1", "tyrannosaurus_death2"]},
}
SUBTITLE_VERBS = {"ambient": "calls", "hurt": "hurts", "death": "dies"}


def write_json(path, data, indent=4):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        json.dump(data, f, indent=indent, ensure_ascii=False)
        f.write("\n")


def main():
    colors = {r.id: r.colors for r in species.ALL}
    sounds_path = os.path.join(ASSETS, "sounds.json")
    sounds = json.load(open(sounds_path, encoding="utf-8"))
    lang_path = os.path.join(ASSETS, "lang", "en_us.json")
    lang_text = open(lang_path, encoding="utf-8").read()
    lang = json.loads(lang_text)
    new_lang = {}

    for sid, spec in SPECIES.items():
        data = {k: v for k, v in spec.items() if k != "sounds"}
        write_json(os.path.join(DATA, f"{sid}.json"), data)

        for kind in ("ambient", "hurt", "death"):
            entries = []
            for clip in spec["sounds"][kind]:
                name, pitch = clip if isinstance(clip, tuple) else (clip, None)
                entries.append({"name": f"fossilsarch2:{name}", **({"pitch": pitch} if pitch else {})})
            sounds[f"{sid}.{kind}"] = {"sounds": entries, "subtitle": f"subtitles.fossilsarch2.{sid}.{kind}"}
            new_lang[f"subtitles.fossilsarch2.{sid}.{kind}"] = f"{spec['display_name']} {SUBTITLE_VERBS[kind]}"

        name = spec["display_name"]
        new_lang[f"entity.fossilsarch2.{sid}"] = name
        new_lang[f"item.fossilsarch2.{sid}_dna"] = f"{name} DNA"
        new_lang[f"item.fossilsarch2.{sid}_egg"] = f"{name} Egg"
        new_lang[f"item.fossilsarch2.{sid}_meat"] = f"Raw {name} Meat"
        new_lang[f"item.fossilsarch2.{sid}_cooked_meat"] = f"Cooked {name} Meat"

        items = os.path.join(ASSETS, "textures", "item")
        c = colors[sid]
        for suffix, color in (("dna", c["accent"]), ("egg", c["base"])):
            dst = os.path.join(items, f"{sid}_{suffix}.png")
            if not os.path.exists(dst):
                texture.tint(os.path.join(items, f"triceratops_{suffix}.png"), dst, color)
        for suffix in ("meat", "cooked_meat"):
            dst = os.path.join(items, f"{sid}_{suffix}.png")
            if not os.path.exists(dst):
                shutil.copyfile(os.path.join(items, f"triceratops_{suffix}.png"), dst)
        egg = os.path.join(ASSETS, "textures", "entity", "egg", f"{sid}.png")
        if not os.path.exists(egg):
            texture.tint(os.path.join(ASSETS, "textures", "entity", "egg", "triceratops.png"), egg, c["base"], 0.6)

    # Existing species: point their sounds at the subtitles the lang file already defines.
    for sid in EXISTING_SOUNDS:
        for kind in ("ambient", "hurt", "death"):
            if f"subtitles.fossilsarch2.{sid}.{kind}" in lang:
                sounds[f"{sid}.{kind}"]["subtitle"] = f"subtitles.fossilsarch2.{sid}.{kind}"
    write_json(sounds_path, sounds, indent=4)

    # Append new lang keys, keeping the hand-maintained grouping of the existing file.
    missing = {k: v for k, v in new_lang.items() if k not in lang}
    if missing:
        body = lang_text.rstrip().rstrip("}").rstrip()
        extra = ",\n\n" + ",\n".join(f"    {json.dumps(k)}: {json.dumps(v, ensure_ascii=False)}" for k, v in missing.items())
        with open(lang_path, "w", encoding="utf-8") as f:
            f.write(body + extra + "\n}\n")
    print(f"registered {len(SPECIES)} species, {len(missing)} lang keys")


if __name__ == "__main__":
    main()
