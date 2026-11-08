package mod.fossilsarch2.model;

import mod.fossilsarch2.FossilsArch2Mod;
import mod.fossilsarch2.entity.DinoEggEntity;
import net.minecraft.resources.Identifier;
import com.geckolib.model.GeoModel;
import com.geckolib.renderer.base.GeoRenderState;

/** Shared egg model; the per-species texture ({@code textures/entity/egg/<path>.png}) comes from the render state. */
public class DinoEggModel extends GeoModel<DinoEggEntity> {

    private static final Identifier MODEL = Identifier.fromNamespaceAndPath(FossilsArch2Mod.MOD_ID, "dino_egg");
    private static final Identifier FALLBACK_TEXTURE = texture(Identifier.fromNamespaceAndPath(FossilsArch2Mod.MOD_ID, "triceratops"));

    public static Identifier texture(Identifier speciesId) {
        return speciesId.withPath("textures/entity/egg/" + speciesId.getPath() + ".png");
    }

    @Override
    public Identifier getModelResource(GeoRenderState renderState) {
        return MODEL;
    }

    @Override
    public Identifier getTextureResource(GeoRenderState renderState) {
        return renderState.getOrDefaultGeckolibData(ModRenderData.TEXTURE, FALLBACK_TEXTURE);
    }

    @Override
    public Identifier getAnimationResource(DinoEggEntity animatable) {
        return MODEL;
    }
}
