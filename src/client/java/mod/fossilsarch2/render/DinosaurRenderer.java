package mod.fossilsarch2.render;

import mod.fossilsarch2.entity.DinosaurEntity;
import mod.fossilsarch2.model.DinosaurModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.GeoRenderState;

public class DinosaurRenderer<R extends LivingEntityRenderState & GeoRenderState>
        extends GeoEntityRenderer<DinosaurEntity, R> {
    private static final float ADULT_SHADOW_RADIUS = 0.8f;

    public DinosaurRenderer(Context ctx, String dinosaurId) {
        super(ctx, new DinosaurModel(dinosaurId));
        this.shadowRadius = ADULT_SHADOW_RADIUS;
    }

    @Override
    public void extractRenderState(DinosaurEntity entity, R state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);

        float dynamicScale = entity.getScale() * entity.getAgeScale();
        state.scale = dynamicScale;
        state.ageScale = entity.getAgeScale();
        state.isBaby = entity.isBaby();
        this.shadowRadius = ADULT_SHADOW_RADIUS * dynamicScale;
    }

    @Override
    public void fireCompileRenderStateEvent(DinosaurEntity animatable, Void context, R renderState, float partialTick) {
        super.fireCompileRenderStateEvent(animatable, context, renderState, partialTick);
        // Pass variant to model before texture lookup
        ((DinosaurModel) getGeoModel()).setVariant(animatable.getVariant());
    }
}
