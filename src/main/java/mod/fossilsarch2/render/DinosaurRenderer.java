package mod.fossilsarch2.render;

import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.RenderPassInfo;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import mod.fossilsarch2.entity.DinosaurEntity;
import mod.fossilsarch2.model.DinosaurModel;
import mod.fossilsarch2.model.ModRenderData;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;

/**
 * Renderer shared by every entity of one species. Per-entity values (growth scale, variant
 * texture) are written to the render state, never to renderer or model fields, because
 * extraction for all visible entities runs before any of them render.
 */
public class DinosaurRenderer<R extends LivingEntityRenderState & GeoRenderState>
        extends
            GeoEntityRenderer<DinosaurEntity, R> {
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
        // Land dinosaurs use xRot for head pitch only; swimmers tilt their whole body.
        float swimPitch = entity.isAquatic() && entity.isInWater() ? entity.getXRot(partialTick) : 0.0f;
        state.addGeckolibData(ModRenderData.SWIM_PITCH, swimPitch);
    }

    @Override
    protected void applyRotations(RenderPassInfo<R> renderPassInfo, PoseStack poseStack, float nativeScale) {
        super.applyRotations(renderPassInfo, poseStack, nativeScale);
        float pitch = renderPassInfo.renderState().getOrDefaultGeckolibData(ModRenderData.SWIM_PITCH, 0.0f);
        if (pitch != 0.0f) {
            // Tilt about the middle of the body rather than the feet.
            float midHeight = renderPassInfo.renderState().boundingBoxHeight / 2.0f;
            poseStack.translate(0.0f, midHeight, 0.0f);
            // Positive xRot dives; in GeckoLib's y-up model space the nose (-z) dips under a negative X turn.
            poseStack.rotateDegrees(Axis.XP, -pitch);
            poseStack.translate(0.0f, -midHeight, 0.0f);
        }
    }

    @Override
    protected float getShadowRadius(R state) {
        return ADULT_SHADOW_RADIUS * state.scale;
    }
}
