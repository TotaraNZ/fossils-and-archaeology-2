package mod.fossilsarch2.registry;

import mod.fossilsarch2.FossilsArch2Mod;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

public final class ModAdvancements {

    public static final Identifier ROOT = id("main/root");
    public static final Identifier DISCOVER_BIO_FOSSIL = id("main/discover_bio_fossil");
    public static final Identifier EXTRACT_DNA = id("main/extract_dna");
    public static final Identifier CULTIVATE_EGG = id("main/cultivate_egg");
    public static final Identifier HATCH_DINOSAUR = id("main/hatch_dinosaur");

    public static final String HATCHED_DINOSAUR_CRITERION = "hatched_dinosaur";

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(FossilsArch2Mod.MOD_ID, path);
    }

    public static void grant(ServerPlayer player, Identifier advancementId, String criterion) {
        AdvancementHolder advancement = player.level().getServer().getAdvancements().get(advancementId);
        if (advancement != null) {
            player.getAdvancements().award(advancement, criterion);
        }
    }

    private ModAdvancements() {}
}
