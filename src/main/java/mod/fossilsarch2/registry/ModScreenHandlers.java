package mod.fossilsarch2.registry;

import mod.fossilsarch2.FossilsArch2Mod;
import mod.fossilsarch2.screen.AnalyserScreenHandler;
import mod.fossilsarch2.screen.CultivatorScreenHandler;
import mod.fossilsarch2.screen.FeederScreenHandler;
import mod.fossilsarch2.screen.WorktableScreenHandler;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.resource.featuretoggle.FeatureSet;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.util.Identifier;

public final class ModScreenHandlers {

    public static final ScreenHandlerType<AnalyserScreenHandler> ANALYSER = Registry.register(
            Registries.SCREEN_HANDLER,
            Identifier.of(FossilsArch2Mod.MOD_ID, "analyser"),
            new ScreenHandlerType<>(AnalyserScreenHandler::new, FeatureSet.empty()));

    public static final ScreenHandlerType<CultivatorScreenHandler> CULTIVATOR = Registry.register(
            Registries.SCREEN_HANDLER,
            Identifier.of(FossilsArch2Mod.MOD_ID, "cultivator"),
            new ScreenHandlerType<>(CultivatorScreenHandler::new, FeatureSet.empty()));

    public static final ScreenHandlerType<FeederScreenHandler> FEEDER = Registry.register(
            Registries.SCREEN_HANDLER,
            Identifier.of(FossilsArch2Mod.MOD_ID, "feeder"),
            new ScreenHandlerType<>(FeederScreenHandler::new, FeatureSet.empty()));

    public static final ScreenHandlerType<WorktableScreenHandler> WORKTABLE = Registry.register(
            Registries.SCREEN_HANDLER,
            Identifier.of(FossilsArch2Mod.MOD_ID, "worktable"),
            new ScreenHandlerType<>(WorktableScreenHandler::new, FeatureSet.empty()));

    public static void init() {
    }
}
