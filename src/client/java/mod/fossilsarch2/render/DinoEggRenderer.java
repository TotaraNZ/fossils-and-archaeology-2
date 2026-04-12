package mod.fossilsarch2.render;

import mod.fossilsarch2.entity.DinoEggEntity;
import mod.fossilsarch2.model.DinoEggModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.GeoRenderState;

public class DinoEggRenderer<R extends EntityRenderState & GeoRenderState>
        extends GeoEntityRenderer<DinoEggEntity, R> {

    public DinoEggRenderer(Context ctx) {
        super(ctx, new DinoEggModel());
        this.shadowRadius = 0.25f;
    }

    @Override
    public void captureDefaultRenderState(DinoEggEntity entity, Void input, R state, float partialTick) {
        super.captureDefaultRenderState(entity, input, state, partialTick);
        String dinoId = entity.getDinoId();
        if (!dinoId.isEmpty()) {
            ((DinoEggModel) getGeoModel()).setDinoId(dinoId);
        }
    }
}
