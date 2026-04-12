package mod.fossilsarch2.render;

import mod.fossilsarch2.entity.DinosaurEntity;
import mod.fossilsarch2.model.DinosaurModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.GeoRenderState;

public class DinosaurRenderer<R extends LivingEntityRenderState & GeoRenderState>
        extends GeoEntityRenderer<DinosaurEntity, R> {

    public DinosaurRenderer(Context ctx, String dinosaurId) {
        super(ctx, new DinosaurModel(dinosaurId));
        this.shadowRadius = 0.8f;
    }

    @Override
    public void fireCompileRenderStateEvent(DinosaurEntity animatable, Void context, R renderState, float partialTick) {
        super.fireCompileRenderStateEvent(animatable, context, renderState, partialTick);
        // Pass variant to model before texture lookup
        ((DinosaurModel) getGeoModel()).setVariant(animatable.getVariant());
    }
}
