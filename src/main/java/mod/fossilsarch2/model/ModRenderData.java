package mod.fossilsarch2.model;

import com.geckolib.constant.dataticket.DataTicket;
import mod.fossilsarch2.FossilsArch2Mod;
import net.minecraft.resources.Identifier;

/**
 * Per-entity values captured onto each GeckoLib render state. Models are shared by every
 * entity of a type, so anything that varies per entity must travel on the render state.
 */
public final class ModRenderData {

    /** Texture to render, resolved from the entity's species and variant at extraction time. */
    public static final DataTicket<Identifier> TEXTURE = DataTicket.create(FossilsArch2Mod.MOD_ID + ":texture",
            Identifier.class);

    /** Body pitch in degrees for a swimming aquatic dinosaur (0 otherwise), following its dive or climb. */
    public static final DataTicket<Float> SWIM_PITCH = DataTicket.create(FossilsArch2Mod.MOD_ID + ":swim_pitch",
            Float.class);

    private ModRenderData() {}
}
