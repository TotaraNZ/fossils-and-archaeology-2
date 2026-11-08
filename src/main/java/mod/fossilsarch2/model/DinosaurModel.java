package mod.fossilsarch2.model;

import mod.fossilsarch2.entity.DinosaurEntity;
import net.minecraft.resources.Identifier;
import com.geckolib.model.GeoModel;
import com.geckolib.renderer.base.GeoRenderState;

/**
 * One model per species, resolving assets by convention from the species ID:
 * <ul>
 *   <li>model: {@code geckolib/models/<path>.geo.json}</li>
 *   <li>animations: {@code geckolib/animations/<path>.animation.json}</li>
 *   <li>texture: {@code textures/entity/<path>/<path>[_<variant>].png}</li>
 * </ul>
 */
public class DinosaurModel extends GeoModel<DinosaurEntity> {

    private final Identifier speciesId;
    private final Identifier defaultTexture;

    public DinosaurModel(Identifier speciesId) {
        this.speciesId = speciesId;
        this.defaultTexture = texture(speciesId, "");
    }

    public static Identifier texture(Identifier speciesId, String variant) {
        String path = speciesId.getPath();
        String suffix = variant.isEmpty() ? "" : "_" + variant;
        return speciesId.withPath("textures/entity/" + path + "/" + path + suffix + ".png");
    }

    @Override
    public Identifier getModelResource(GeoRenderState renderState) {
        return speciesId;
    }

    @Override
    public Identifier getTextureResource(GeoRenderState renderState) {
        return renderState.getOrDefaultGeckolibData(ModRenderData.TEXTURE, defaultTexture);
    }

    @Override
    public Identifier getAnimationResource(DinosaurEntity animatable) {
        return speciesId;
    }
}
