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
| `texture.py` | Placeholder box-UV textures, used only when a species has no texture yet. |
| `species_assets.py` | Registers converted species (land and aquatic): data JSON, variants, sounds, lang. |
| `blockbench_export.py` | Loads a species into Blockbench over MCP, validates it as GeckoLib and saves the `.bbmodel`. |

The editable Blockbench projects are in `art/models/*.bbmodel` (GeckoLib Animated Model format).
Converted files are overwritten on rerun, except textures, so repainted textures survive.

## Original textures and sounds

The upstream source has no art, but community copies of the original mod carry it. Their models
have UV layouts identical to the ones converted here (checked box by box with `techne.py`), so
the original textures drop straight in:

- Entity textures (including velociraptor green/blue variants) and DNA/egg/meat item icons:
  [Shadowbeast/FossilArcheologyTest](https://github.com/Shadowbeast/FossilArcheologyTest) `fossil/textures`.
- Dilophosaurus (and its lime variant) and every species' sounds:
  [smallpred/FossilArcheology](https://github.com/smallpred/FossilArcheology) `assets/fossil`.

Velociraptor, pterosaur and plesiosaurus have no recorded death call; they reuse a hurt clip
at a lower pitch. Egg-entity textures and cooked meat icons are still generated.
