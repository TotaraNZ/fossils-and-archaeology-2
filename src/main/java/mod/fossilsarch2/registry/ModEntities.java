package mod.fossilsarch2.registry;

import java.util.HashMap;
import java.util.Map;

import mod.fossilsarch2.FossilsArch2Mod;
import mod.fossilsarch2.dinosaur.Dinosaur;
import mod.fossilsarch2.dinosaur.DinosaurUtils;
import mod.fossilsarch2.entity.DinoEggEntity;
import mod.fossilsarch2.entity.DinosaurEntity;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

public final class ModEntities {
    public static final Map<String, EntityType<DinosaurEntity>> TYPES = new HashMap<>();

    public static EntityType<DinoEggEntity> DINO_EGG;

    public static void registerDinosaurEntities() {
        for (var entry : DinosaurRegistry.all().entrySet()) {
            Identifier dinoId = entry.getKey();
            Dinosaur d = entry.getValue();
            String ns = DinosaurUtils.getNamespace(d);

            ResourceKey<EntityType<?>> key = ResourceKey.create(Registries.ENTITY_TYPE,
                    Identifier.fromNamespaceAndPath(ns, d.id));

            EntityType<DinosaurEntity> type = EntityType.Builder
                    .of(DinosaurEntity::new, MobCategory.CREATURE)
                    .sized(d.width, d.height)
                    .build(key);

            Registry.register(BuiltInRegistries.ENTITY_TYPE, key, type);
            FabricDefaultAttributeRegistry.register(type, DinosaurEntity.createAttributes(d));

            TYPES.put(d.id, type);
        }

        // Register egg entity
        ResourceKey<EntityType<?>> eggKey = ResourceKey.create(Registries.ENTITY_TYPE,
                Identifier.fromNamespaceAndPath(FossilsArch2Mod.MOD_ID, "dino_egg"));

        DINO_EGG = EntityType.Builder
                .of(DinoEggEntity::new, MobCategory.MISC)
                .sized(0.4f, 0.5f)
                .build(eggKey);

        Registry.register(BuiltInRegistries.ENTITY_TYPE, eggKey, DINO_EGG);
    }
}
