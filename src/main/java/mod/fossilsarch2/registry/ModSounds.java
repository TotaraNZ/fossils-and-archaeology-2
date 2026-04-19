package mod.fossilsarch2.registry;

import java.util.HashMap;
import java.util.Map;

import mod.fossilsarch2.FossilsArch2Mod;
import mod.fossilsarch2.dinosaur.Dinosaur;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Dynamically registers sound events for all dinosaur species.
 */
public final class ModSounds {

	private static final DeferredRegister<SoundEvent> SOUND_EVENTS =
			DeferredRegister.create(Registries.SOUND_EVENT, FossilsArch2Mod.MOD_ID);

	private static final Map<String, DeferredHolder<SoundEvent, SoundEvent>> ALL = new HashMap<>();

	public static void register(IEventBus modEventBus, Map<String, Dinosaur> dinosaurs) {
		for (String id : dinosaurs.keySet()) {
			registerSound(id + ".ambient");
			registerSound(id + ".hurt");
			registerSound(id + ".death");
		}
		SOUND_EVENTS.register(modEventBus);
	}

	private static void registerSound(String path) {
		Identifier id = Identifier.fromNamespaceAndPath(FossilsArch2Mod.MOD_ID, path);
		ALL.put(path, SOUND_EVENTS.register(path, () -> SoundEvent.createVariableRangeEvent(id)));
	}

	public static SoundEvent get(String path) {
		DeferredHolder<SoundEvent, SoundEvent> holder = ALL.get(path);
		return holder != null ? holder.get() : null;
	}

	private ModSounds() {}
}
