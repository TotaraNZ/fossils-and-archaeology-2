package mod.fossilsarch2.render;

import mod.fossilsarch2.entity.DinosaurEntity;
import mod.fossilsarch2.model.DinosaurModel;
import net.minecraft.client.render.entity.EntityRendererFactory.Context;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.base.GeoRenderState;

public class DinosaurRenderer<R extends LivingEntityRenderState & GeoRenderState>
        extends GeoEntityRenderer<DinosaurEntity, R> {

    public DinosaurRenderer(Context ctx, String dinosaurId) {
        super(ctx, new DinosaurModel(dinosaurId));
        this.shadowRadius = 0.8f;
    }

    @Override
    public void fireCompileRenderStateEvent(DinosaurEntity animatable, Void context, R renderState) {
        super.fireCompileRenderStateEvent(animatable, context, renderState);
        // Pass variant to model before texture lookup
        ((DinosaurModel) getGeoModel()).setVariant(animatable.getVariant());
    }
}
