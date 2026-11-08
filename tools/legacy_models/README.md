# Legacy model converter

Converts FlammaRilva's original Fossils & Archaeology entity models (Techne-generated Java,
[fls81245/FossilArcheology](https://github.com/fls81245/FossilArcheology)) into GeckoLib assets.

```sh
git clone https://github.com/fls81245/FossilArcheology ../reference/FossilArcheology
python3 tools/legacy_models/convert.py ../reference/FossilArcheology/src        # models, animations, textures
python3 tools/legacy_models/species_assets.py                                    # species data, items, sounds, lang
python3 tools/legacy_models/blockbench_export.py stegosaurus                     # .bbmodel (needs Blockbench + MCP plugin)
./gradlew regenData
```

Standard library Python only.

| File | Purpose |
| --- | --- |
| `techne.py` | Parses the legacy Java models (parts, boxes, pivots, rotations, `addChild`). |
| `species.py` | Rig specs: groups legacy parts into a real bone hierarchy (neck/tail chains, knees, jaws). |
| `rig.py` | Builds Bedrock/GeckoLib geometry; the rest pose matches the legacy model exactly. |
| `anim.py` | Role-driven procedural animations (walk/swim, idle, bite, tail swipe, stomp, eat, look, roar, flap). |
| `texture.py` | Placeholder box-UV textures (the original textures were never published with the source). |
| `species_assets.py` | Registers converted land species: data JSON, item/egg textures, sounds, lang. |
| `blockbench_export.py` | Loads a species into Blockbench over MCP, validates it as GeckoLib and saves the `.bbmodel`. |

The editable Blockbench projects are in `art/models/*.bbmodel` (GeckoLib Animated Model format).
Converted files are overwritten on rerun, except textures, so repainted textures survive.

Mosasaurus and plesiosaurus are converted but not registered: they need swimming movement first.
