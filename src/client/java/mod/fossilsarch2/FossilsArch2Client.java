package mod.fossilsarch2;

import java.util.Map;

import mod.fossilsarch2.entity.DinoEggEntity;
import mod.fossilsarch2.entity.DinosaurEntity;
import mod.fossilsarch2.network.DinopediaPayload;
import mod.fossilsarch2.registry.ModBlocks;
import mod.fossilsarch2.registry.ModEntities;
import mod.fossilsarch2.registry.ModScreenHandlers;
import mod.fossilsarch2.render.DinoEggRenderer;
import mod.fossilsarch2.render.DinosaurRenderer;
import mod.fossilsarch2.screen.AnalyserScreen;
import mod.fossilsarch2.screen.CultivatorScreen;
import mod.fossilsarch2.screen.DinopediaScreen;
import mod.fossilsarch2.screen.FeederScreen;
import mod.fossilsarch2.screen.WorktableScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.gui.screen.ingame.HandledScreens;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;

@Environment(EnvType.CLIENT)
public class FossilsArch2Client implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		// Dinosaur entity renderers
		for (Map.Entry<String, EntityType<DinosaurEntity>> entry : ModEntities.TYPES.entrySet()) {
			String dinosaurId = entry.getKey();
			EntityType<DinosaurEntity> type = entry.getValue();

			EntityRendererRegistry.register(type, ctx -> new DinosaurRenderer<>(ctx, dinosaurId));
		}

		// Egg entity renderer
		EntityRendererRegistry.register(ModEntities.DINO_EGG, DinoEggRenderer::new);

		// Block render layers
		BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.FERN, RenderLayer.getCutout());

		// Screen registrations
		HandledScreens.register(ModScreenHandlers.ANALYSER, AnalyserScreen::new);
		HandledScreens.register(ModScreenHandlers.CULTIVATOR, CultivatorScreen::new);
		HandledScreens.register(ModScreenHandlers.FEEDER, FeederScreen::new);
		HandledScreens.register(ModScreenHandlers.WORKTABLE, WorktableScreen::new);

		// Dinopedia packet receiver
		ClientPlayNetworking.registerGlobalReceiver(DinopediaPayload.ID, (payload, context) -> {
			int entityId = payload.entityId();
			context.client().execute(() -> {
				if (context.client().world == null) return;
				Entity entity = context.client().world.getEntityById(entityId);
				if (entity instanceof DinosaurEntity dino) {
					context.client().setScreen(DinopediaScreen.forDinosaur(dino));
				} else if (entity instanceof DinoEggEntity egg) {
					context.client().setScreen(DinopediaScreen.forEgg(egg));
				}
			});
		});
	}
}
