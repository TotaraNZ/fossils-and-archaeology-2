package mod.fossilsarch2.registry;

import mod.fossilsarch2.FossilsArch2Mod;
import mod.fossilsarch2.screen.AnalyserScreenHandler;
import mod.fossilsarch2.screen.CultivatorScreenHandler;
import mod.fossilsarch2.screen.FeederScreenHandler;
import mod.fossilsarch2.screen.WorktableScreenHandler;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.inventory.MenuType;

public final class ModScreenHandlers {

    public static final MenuType<AnalyserScreenHandler> ANALYSER = Registry.register(
            BuiltInRegistries.MENU,
            Identifier.fromNamespaceAndPath(FossilsArch2Mod.MOD_ID, "analyser"),
            new MenuType<>(AnalyserScreenHandler::new, FeatureFlagSet.of()));

    public static final MenuType<CultivatorScreenHandler> CULTIVATOR = Registry.register(
            BuiltInRegistries.MENU,
            Identifier.fromNamespaceAndPath(FossilsArch2Mod.MOD_ID, "cultivator"),
            new MenuType<>(CultivatorScreenHandler::new, FeatureFlagSet.of()));

    public static final MenuType<FeederScreenHandler> FEEDER = Registry.register(
            BuiltInRegistries.MENU,
            Identifier.fromNamespaceAndPath(FossilsArch2Mod.MOD_ID, "feeder"),
            new MenuType<>(FeederScreenHandler::new, FeatureFlagSet.of()));

    public static final MenuType<WorktableScreenHandler> WORKTABLE = Registry.register(
            BuiltInRegistries.MENU,
            Identifier.fromNamespaceAndPath(FossilsArch2Mod.MOD_ID, "worktable"),
            new MenuType<>(WorktableScreenHandler::new, FeatureFlagSet.of()));

    public static void init() {
    }
}
