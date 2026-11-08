# Fossils & Archaeology 2

A faithful re-creation of the original prehistoric Minecraft experience by [FlammaRilva](https://github.com/fls81245), built for modern Minecraft on NeoForge.

## Development

Requires JDK 25 (the Gradle toolchain targets Java 25).

| Task | Command |
| --- | --- |
| Build the mod jar | `./gradlew build` |
| Format code | `./gradlew spotlessApply` |
| Lint (formatting + Checkstyle) | `./gradlew check` |
| Run GameTests headlessly | `./gradlew gameTest` |
| Regenerate data (recipes, loot, models…) | `./gradlew regenData` |
| Dev client / server | `./gradlew runClient` / `./gradlew runServer` |

GameTests live in `src/gametest` and are loaded in dev runs only; in a dev world, `/test runall` runs them too.

In VS Code, install the recommended extensions: format-on-save uses the same Eclipse profile as Spotless (`config/eclipse-formatter.xml`) and Checkstyle reads `config/checkstyle/checkstyle.xml`.

### Adding a dinosaur

A species is data plus assets, with no new Java:

- `data/fossilsarch2/fossilsarch2/dinosaurs/<id>.json`: stats, diet, growth, animation names
- `assets/fossilsarch2/geckolib/models/<id>.geo.json` and `geckolib/animations/<id>.animation.json`
- `assets/fossilsarch2/textures/entity/<id>/<id>[_<variant>].png` and `textures/entity/egg/<id>.png`
- sounds `<id>.ambient`, `<id>.hurt`, `<id>.death` in `sounds.json`, plus lang entries

## License

[MIT](LICENSE)
