package mod.fossilsarch2.model;

import mod.fossilsarch2.FossilsArch2Mod;
import mod.fossilsarch2.entity.DinoEggEntity;
import net.minecraft.resources.Identifier;
import com.geckolib.model.GeoModel;
import com.geckolib.renderer.base.GeoRenderState;

public class DinoEggModel extends GeoModel<DinoEggEntity> {

    private String currentDinoId = "triceratops";

    public void setDinoId(String dinoId) {
        this.currentDinoId = dinoId;
    }

    @Override
    public Identifier getModelResource(GeoRenderState renderState) {
        return Identifier.fromNamespaceAndPath(FossilsArch2Mod.MOD_ID, "dino_egg");
    }

    @Override
    public Identifier getTextureResource(GeoRenderState renderState) {
        return Identifier.fromNamespaceAndPath(FossilsArch2Mod.MOD_ID, "textures/entity/egg/" + currentDinoId + ".png");
    }

    @Override
    public Identifier getAnimationResource(DinoEggEntity animatable) {
        return Identifier.fromNamespaceAndPath(FossilsArch2Mod.MOD_ID, "dino_egg");
    }
}
