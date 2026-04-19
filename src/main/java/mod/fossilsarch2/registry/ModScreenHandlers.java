package mod.fossilsarch2.registry;

import mod.fossilsarch2.FossilsArch2Mod;
import mod.fossilsarch2.screen.AnalyserScreenHandler;
import mod.fossilsarch2.screen.CultivatorScreenHandler;
import mod.fossilsarch2.screen.FeederScreenHandler;
import mod.fossilsarch2.screen.WorktableScreenHandler;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModScreenHandlers {

	private static final DeferredRegister<MenuType<?>> MENUS =
			DeferredRegister.create(Registries.MENU, FossilsArch2Mod.MOD_ID);

	public static final DeferredHolder<MenuType<?>, MenuType<AnalyserScreenHandler>> ANALYSER =
			MENUS.register("analyser",
					() -> new MenuType<>(AnalyserScreenHandler::new, FeatureFlagSet.of()));

	public static final DeferredHolder<MenuType<?>, MenuType<CultivatorScreenHandler>> CULTIVATOR =
			MENUS.register("cultivator",
					() -> new MenuType<>(CultivatorScreenHandler::new, FeatureFlagSet.of()));

	public static final DeferredHolder<MenuType<?>, MenuType<FeederScreenHandler>> FEEDER =
			MENUS.register("feeder",
					() -> new MenuType<>(FeederScreenHandler::new, FeatureFlagSet.of()));

	public static final DeferredHolder<MenuType<?>, MenuType<WorktableScreenHandler>> WORKTABLE =
			MENUS.register("worktable",
					() -> new MenuType<>(WorktableScreenHandler::new, FeatureFlagSet.of()));

	public static void register(IEventBus modEventBus) {
		MENUS.register(modEventBus);
	}

	private ModScreenHandlers() {}
}
