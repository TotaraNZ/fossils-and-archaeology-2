package mod.fossilsarch2.registry;

import mod.fossilsarch2.FossilsArch2Mod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

public final class ModItemTags {

    public static final TagKey<Item> DINO_MEAT = of("dino_meat");
    public static final TagKey<Item> FEEDER_MEAT = of("feeder_meat");
    public static final TagKey<Item> FEEDER_VEGETABLE = of("feeder_vegetable");

    private ModItemTags() {
    }

    private static TagKey<Item> of(String path) {
        return TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(FossilsArch2Mod.MOD_ID, path));
    }
}
