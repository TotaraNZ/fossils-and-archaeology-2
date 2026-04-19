package mod.fossilsarch2.registry;

import java.util.HashMap;
import java.util.Map;

import mod.fossilsarch2.FossilsArch2Mod;
import mod.fossilsarch2.dinosaur.Dinosaur;
import mod.fossilsarch2.entity.DinoEggEntity;
import mod.fossilsarch2.entity.DinosaurEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModEntities {

	private static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
			DeferredRegister.create(Registries.ENTITY_TYPE, FossilsArch2Mod.MOD_ID);

	public static final Map<String, DeferredHolder<EntityType<?>, EntityType<DinosaurEntity>>> TYPES = new HashMap<>();

	public static DeferredHolder<EntityType<?>, EntityType<DinoEggEntity>> DINO_EGG;

	private static final Map<String, Dinosaur> DINOSAURS_FOR_ATTRIBUTES = new HashMap<>();

	public static void register(IEventBus modEventBus, Map<String, Dinosaur> dinosaurs) {
		DINOSAURS_FOR_ATTRIBUTES.clear();
		DINOSAURS_FOR_ATTRIBUTES.putAll(dinosaurs);

		for (Map.Entry<String, Dinosaur> entry : dinosaurs.entrySet()) {
			String id = entry.getKey();
			Dinosaur d = entry.getValue();
			ResourceKey<EntityType<?>> key = ResourceKey.create(Registries.ENTITY_TYPE,
					Identifier.fromNamespaceAndPath(FossilsArch2Mod.MOD_ID, id));
			DeferredHolder<EntityType<?>, EntityType<DinosaurEntity>> holder = ENTITY_TYPES.register(
					id,
					() -> EntityType.Builder.of(DinosaurEntity::new, MobCategory.CREATURE)
							.sized(d.growth().width(), d.growth().height())
							.build(key));
			TYPES.put(id, holder);
		}

		ResourceKey<EntityType<?>> eggKey = ResourceKey.create(Registries.ENTITY_TYPE,
				Identifier.fromNamespaceAndPath(FossilsArch2Mod.MOD_ID, "dino_egg"));
		DINO_EGG = ENTITY_TYPES.register("dino_egg",
				() -> EntityType.Builder.of(DinoEggEntity::new, MobCategory.MISC)
						.sized(0.4f, 0.5f)
						.build(eggKey));

		ENTITY_TYPES.register(modEventBus);
	}

	public static void onEntityAttributes(EntityAttributeCreationEvent event) {
		for (Map.Entry<String, Dinosaur> entry : DINOSAURS_FOR_ATTRIBUTES.entrySet()) {
			DeferredHolder<EntityType<?>, EntityType<DinosaurEntity>> holder = TYPES.get(entry.getKey());
			if (holder == null) continue;
			event.put(holder.get(), DinosaurEntity.createAttributes(entry.getValue()).build());
		}
	}

	private ModEntities() {}
}
