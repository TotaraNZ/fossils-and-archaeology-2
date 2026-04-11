package mod.fossilsarch2.registry;

import java.util.HashMap;
import java.util.Map;

import mod.fossilsarch2.FossilsArch2Mod;
import mod.fossilsarch2.dinosaur.Dinosaur;
import mod.fossilsarch2.dinosaur.DinosaurUtils;
import mod.fossilsarch2.entity.DinoEggEntity;
import mod.fossilsarch2.entity.DinosaurEntity;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;

public final class ModEntities {
    public static final Map<String, EntityType<DinosaurEntity>> TYPES = new HashMap<>();

    public static EntityType<DinoEggEntity> DINO_EGG;

    public static void registerDinosaurEntities() {
        for (var entry : DinosaurRegistry.all().entrySet()) {
            Identifier dinoId = entry.getKey();
            Dinosaur d = entry.getValue();
            String ns = DinosaurUtils.getNamespace(d);

            RegistryKey<EntityType<?>> key = RegistryKey.of(RegistryKeys.ENTITY_TYPE,
                    Identifier.of(ns, d.id));

            EntityType<DinosaurEntity> type = EntityType.Builder
                    .create(DinosaurEntity::new, SpawnGroup.CREATURE)
                    .dimensions(d.width, d.height)
                    .build(key);

            Registry.register(Registries.ENTITY_TYPE, key, type);
            FabricDefaultAttributeRegistry.register(type, DinosaurEntity.createAttributes(d));

            TYPES.put(d.id, type);
        }

        // Register egg entity
        RegistryKey<EntityType<?>> eggKey = RegistryKey.of(RegistryKeys.ENTITY_TYPE,
                Identifier.of(FossilsArch2Mod.MOD_ID, "dino_egg"));

        DINO_EGG = EntityType.Builder
                .create(DinoEggEntity::new, SpawnGroup.MISC)
                .dimensions(0.4f, 0.5f)
                .build(eggKey);

        Registry.register(Registries.ENTITY_TYPE, eggKey, DINO_EGG);
    }
}
