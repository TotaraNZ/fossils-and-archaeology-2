package mod.fossilsarch2.item;

import mod.fossilsarch2.FossilsArch2Mod;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.item.ToolMaterial;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;

/**
 * Scarab gem tool material — diamond-tier harvest level with enhanced durability and enchantability.
 * Stats match the revival mod's custom "Scarab" material.
 */
public final class ScarabGemMaterial {

    public static final ToolMaterial SCARAB_GEM = new ToolMaterial(
            BlockTags.INCORRECT_FOR_DIAMOND_TOOL,   // same harvest level as diamond
            1861,                                     // durability (diamond=1561)
            8.0F,                                     // mining speed (diamond=8.0)
            4.0F,                                     // attack damage bonus (diamond=3.0)
            25,                                       // enchantability (diamond=10)
            TagKey.of(RegistryKeys.ITEM, Identifier.of(FossilsArch2Mod.MOD_ID, "scarab_gems"))
    );

    private ScarabGemMaterial() {}
}
