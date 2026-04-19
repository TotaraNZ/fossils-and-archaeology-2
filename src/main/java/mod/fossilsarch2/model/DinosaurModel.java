package mod.fossilsarch2.model;

import mod.fossilsarch2.FossilsArch2Mod;
import mod.fossilsarch2.entity.DinosaurEntity;
import net.minecraft.resources.Identifier;
import com.geckolib.model.GeoModel;
import com.geckolib.renderer.base.GeoRenderState;

public class DinosaurModel extends GeoModel<DinosaurEntity> {

    private final String dinosaurId;
    private String currentVariant = "";

    public DinosaurModel(String dinosaurId) {
        super();
        this.dinosaurId = dinosaurId;
    }

    public void setVariant(String variant) {
        this.currentVariant = variant != null ? variant : "";
    }

    @Override
    public Identifier getModelResource(GeoRenderState renderState) {
        return Identifier.fromNamespaceAndPath(FossilsArch2Mod.MOD_ID, dinosaurId);
    }

    @Override
    public Identifier getTextureResource(GeoRenderState renderState) {
        // If variant is set, use textures/entity/{id}/{id}_{variant}.png
        // Otherwise use textures/entity/{id}/{id}.png
        if (!currentVariant.isEmpty()) {
            return Identifier.fromNamespaceAndPath(FossilsArch2Mod.MOD_ID,
                    "textures/entity/" + dinosaurId + "/" + dinosaurId + "_" + currentVariant + ".png");
        }
        return Identifier.fromNamespaceAndPath(FossilsArch2Mod.MOD_ID,
                "textures/entity/" + dinosaurId + "/" + dinosaurId + ".png");
    }

    @Override
    public Identifier getAnimationResource(DinosaurEntity animatable) {
        return Identifier.fromNamespaceAndPath(FossilsArch2Mod.MOD_ID, dinosaurId);
    }
}
