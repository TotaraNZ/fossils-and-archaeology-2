package mod.fossilsarch2.data;

import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

import mod.fossilsarch2.FossilsArch2Mod;
import mod.fossilsarch2.dinosaur.Dinosaur;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;

public final class DinosaurJsonLoader {

    private static final Gson GSON = new Gson();

    public static List<Dinosaur> load() {
        List<Dinosaur> dinosaurs = new ArrayList<>();

        for (ModContainer mod : FabricLoader.getInstance().getAllMods()) {
            String modId = mod.getMetadata().getId();
            Path dinoDir = mod.findPath("data/" + modId + "/dinosaurs").orElse(null);

            if (dinoDir == null || !Files.exists(dinoDir)) {
                continue;
            }

            try (DirectoryStream<Path> stream = Files.newDirectoryStream(dinoDir, "*.json")) {
                for (Path path : stream) {
                    if (!Files.isRegularFile(path)) {
                        continue;
                    }

                    try {
                        Dinosaur dinosaur = GSON.fromJson(Files.newBufferedReader(path), Dinosaur.class);

                        if (dinosaur == null || dinosaur.id == null) {
                            FossilsArch2Mod.LOGGER.error("Failed to parse dinosaur JSON: " + path);
                            continue;
                        }

                        String error = dinosaur.validate();
                        if (error != null) {
                            FossilsArch2Mod.LOGGER.error("Invalid dinosaur '{}' in {}: {}", dinosaur.id, path, error);
                            continue;
                        }

                        String animationError = validateAnimationReferences(mod, dinosaur);
                        if (animationError != null) {
                            FossilsArch2Mod.LOGGER.error("Invalid dinosaur '{}' in {}: {}", dinosaur.id, path, animationError);
                            continue;
                        }

                        // Tag with source mod namespace for addon support
                        dinosaur.namespace = modId;
                        dinosaurs.add(dinosaur);

                    } catch (JsonSyntaxException e) {
                        FossilsArch2Mod.LOGGER.error("Malformed dinosaur JSON: " + path, e);
                    } catch (Exception e) {
                        FossilsArch2Mod.LOGGER.error("Error reading dinosaur JSON: " + path, e);
                    }
                }
            } catch (Exception e) {
                FossilsArch2Mod.LOGGER.error("Failed scanning dinosaur directory for mod: " + modId, e);
            }
        }

        return dinosaurs;
    }

    private static String validateAnimationReferences(ModContainer mod, Dinosaur dinosaur) {
        Path animationPath = mod.findPath("assets/" + mod.getMetadata().getId() + "/geckolib/animations/" + dinosaur.id + ".animation.json")
                .orElse(null);
        if (animationPath == null || !Files.isRegularFile(animationPath)) {
            return "missing animation file assets/" + mod.getMetadata().getId() + "/geckolib/animations/" + dinosaur.id + ".animation.json";
        }

        try (var reader = Files.newBufferedReader(animationPath)) {
            JsonObject root = GSON.fromJson(reader, JsonObject.class);
            JsonObject animations = root != null ? root.getAsJsonObject("animations") : null;
            if (animations == null) {
                return "animation file has no 'animations' object";
            }

            Set<String> names = new HashSet<>(animations.keySet());
            if (!dinosaur.attack_animation.isEmpty() && !names.contains(dinosaur.attack_animation)) {
                return "attack_animation '" + dinosaur.attack_animation + "' is missing from " + animationPath.getFileName();
            }

            if (dinosaur.special_animations != null) {
                for (String animation : dinosaur.special_animations) {
                    if (!names.contains(animation)) {
                        return "special animation '" + animation + "' is missing from " + animationPath.getFileName();
                    }
                }
            }
        } catch (Exception e) {
            return "could not validate animation file: " + e.getMessage();
        }

        return null;
    }
}
