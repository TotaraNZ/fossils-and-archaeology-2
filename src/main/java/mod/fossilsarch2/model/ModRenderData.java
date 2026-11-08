package mod.fossilsarch2.model;

import mod.fossilsarch2.FossilsArch2Mod;
import net.minecraft.resources.Identifier;
import com.geckolib.constant.dataticket.DataTicket;

/**
 * Per-entity values captured onto each GeckoLib render state. Models are shared by every
 * entity of a type, so anything that varies per entity must travel on the render state.
 */
public final class ModRenderData {

    /** Texture to render, resolved from the entity's species and variant at extraction time. */
    public static final DataTicket<Identifier> TEXTURE = DataTicket.create(FossilsArch2Mod.MOD_ID + ":texture", Identifier.class);

    private ModRenderData() {}
}
