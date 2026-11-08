package mod.fossilsarch2.render;

import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.GeoRenderState;
import mod.fossilsarch2.entity.DinoEggEntity;
import mod.fossilsarch2.model.DinoEggModel;
import mod.fossilsarch2.model.ModRenderData;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.resources.Identifier;

public class DinoEggRenderer<R extends EntityRenderState & GeoRenderState>
        extends
            GeoEntityRenderer<DinoEggEntity, R> {

    public DinoEggRenderer(Context ctx) {
        super(ctx, new DinoEggModel());
        this.shadowRadius = 0.25f;
    }

    @Override
    public void captureDefaultRenderState(DinoEggEntity entity, Void input, R state, float partialTick) {
        super.captureDefaultRenderState(entity, input, state, partialTick);
        Identifier speciesId = entity.getSpeciesId();
        if (speciesId != null) {
            state.addGeckolibData(ModRenderData.TEXTURE, DinoEggModel.texture(speciesId));
        }
    }
}
