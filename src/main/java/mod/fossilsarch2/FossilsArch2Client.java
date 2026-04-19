package mod.fossilsarch2;

import java.util.Map;

import mod.fossilsarch2.entity.DinosaurEntity;
import mod.fossilsarch2.registry.ModEntities;
import mod.fossilsarch2.registry.ModScreenHandlers;
import mod.fossilsarch2.render.DinoEggRenderer;
import mod.fossilsarch2.render.DinosaurRenderer;
import mod.fossilsarch2.screen.AnalyserScreen;
import mod.fossilsarch2.screen.CultivatorScreen;
import mod.fossilsarch2.screen.FeederScreen;
import mod.fossilsarch2.screen.WorktableScreen;
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
	}

	private static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
		for (Map.Entry<String, DeferredHolder<EntityType<?>, EntityType<DinosaurEntity>>> entry : ModEntities.TYPES.entrySet()) {
			String dinosaurId = entry.getKey();
			event.registerEntityRenderer(entry.getValue().get(),
					ctx -> new DinosaurRenderer<>(ctx, dinosaurId));
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
