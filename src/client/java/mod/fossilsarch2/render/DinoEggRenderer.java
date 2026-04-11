package mod.fossilsarch2.render;

import mod.fossilsarch2.entity.DinoEggEntity;
import mod.fossilsarch2.model.DinoEggModel;
import net.minecraft.client.render.entity.EntityRendererFactory.Context;
import net.minecraft.client.render.entity.state.EntityRenderState;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.base.GeoRenderState;

public class DinoEggRenderer<R extends EntityRenderState & GeoRenderState>
        extends GeoEntityRenderer<DinoEggEntity, R> {

    public DinoEggRenderer(Context ctx) {
        super(ctx, new DinoEggModel());
        this.shadowRadius = 0.25f;
    }

    @Override
    public R captureDefaultRenderState(DinoEggEntity entity, Void input, R state, float partialTick) {
        R result = super.captureDefaultRenderState(entity, input, state, partialTick);
        String dinoId = entity.getDinoId();
        if (!dinoId.isEmpty()) {
            ((DinoEggModel) getGeoModel()).setDinoId(dinoId);
        }
        return result;
    }
}
