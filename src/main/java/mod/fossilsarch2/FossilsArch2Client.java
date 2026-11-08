package mod.fossilsarch2;

import java.util.Map;

import mod.fossilsarch2.entity.DinoEggEntity;
import mod.fossilsarch2.entity.DinosaurEntity;
import mod.fossilsarch2.network.DinopediaPayload;
import mod.fossilsarch2.screen.DinopediaScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;
import mod.fossilsarch2.registry.ModEntities;
import mod.fossilsarch2.registry.ModScreenHandlers;
import mod.fossilsarch2.render.DinoEggRenderer;
import mod.fossilsarch2.render.DinosaurRenderer;
import mod.fossilsarch2.screen.AnalyserScreen;
import mod.fossilsarch2.screen.CultivatorScreen;
import mod.fossilsarch2.screen.FeederScreen;
import mod.fossilsarch2.screen.WorktableScreen;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.registries.DeferredHolder;

@Mod(value = FossilsArch2Mod.MOD_ID, dist = Dist.CLIENT)
public final class FossilsArch2Client {

	public FossilsArch2Client(IEventBus modEventBus) {
		modEventBus.addListener(FossilsArch2Client::onRegisterRenderers);
		modEventBus.addListener(FossilsArch2Client::onRegisterScreens);
		modEventBus.addListener(FossilsArch2Client::onRegisterClientPayloadHandlers);
	}

	private static void onRegisterClientPayloadHandlers(RegisterClientPayloadHandlersEvent event) {
		event.register(DinopediaPayload.TYPE, (payload, context) -> openDinopediaScreen(payload.entityId()));
	}

	private static void openDinopediaScreen(int entityId) {
		Minecraft client = Minecraft.getInstance();
		if (client.level == null) return;
		Entity entity = client.level.getEntity(entityId);
		if (entity instanceof DinosaurEntity dino) {
			client.gui.setScreen(DinopediaScreen.forDinosaur(dino));
		} else if (entity instanceof DinoEggEntity egg) {
			client.gui.setScreen(DinopediaScreen.forEgg(egg));
		}
	}

	private static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
		for (Map.Entry<String, DeferredHolder<EntityType<?>, EntityType<DinosaurEntity>>> entry : ModEntities.TYPES.entrySet()) {
			Identifier speciesId = entry.getValue().getId();
			event.registerEntityRenderer(entry.getValue().get(),
					ctx -> new DinosaurRenderer<>(ctx, speciesId));
		}
		event.registerEntityRenderer(ModEntities.DINO_EGG.get(), DinoEggRenderer::new);
	}

	private static void onRegisterScreens(RegisterMenuScreensEvent event) {
		event.register(ModScreenHandlers.ANALYSER.get(), AnalyserScreen::new);
		event.register(ModScreenHandlers.CULTIVATOR.get(), CultivatorScreen::new);
		event.register(ModScreenHandlers.FEEDER.get(), FeederScreen::new);
		event.register(ModScreenHandlers.WORKTABLE.get(), WorktableScreen::new);
	}
}
