package mod.fossilsarch2.render;

import mod.fossilsarch2.entity.DinosaurEntity;
import mod.fossilsarch2.model.DinosaurModel;
import mod.fossilsarch2.model.ModRenderData;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.GeoRenderState;

/**
 * Renderer shared by every entity of one species. Per-entity values (growth scale, variant
 * texture) are written to the render state, never to renderer or model fields, because
 * extraction for all visible entities runs before any of them render.
 */
public class DinosaurRenderer<R extends LivingEntityRenderState & GeoRenderState>
        extends GeoEntityRenderer<DinosaurEntity, R> {
    private static final float ADULT_SHADOW_RADIUS = 0.8f;

    private final Identifier speciesId;

    public DinosaurRenderer(Context ctx, Identifier speciesId) {
        super(ctx, new DinosaurModel(speciesId));
        this.speciesId = speciesId;
        this.shadowRadius = ADULT_SHADOW_RADIUS;
    }

    @Override
    public void extractRenderState(DinosaurEntity entity, R state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        // GeckoLib scales the model by state.scale only, so fold the growth scale in here.
        state.ageScale = entity.getAgeScale();
        state.scale = entity.getScale() * state.ageScale;
        state.isBaby = entity.isJuvenile();
    }

    @Override
    public void captureDefaultRenderState(DinosaurEntity entity, Void input, R state, float partialTick) {
        super.captureDefaultRenderState(entity, input, state, partialTick);
        state.addGeckolibData(ModRenderData.TEXTURE, DinosaurModel.texture(speciesId, entity.getVariant()));
    }

    @Override
    protected float getShadowRadius(R state) {
        return ADULT_SHADOW_RADIUS * state.scale;
    }
}
