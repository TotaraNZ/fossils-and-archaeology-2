package mod.fossilsarch2.screen;

import mod.fossilsarch2.FossilsArch2Mod;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public class FeederScreen extends AbstractContainerScreen<FeederScreenHandler> {

    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath(FossilsArch2Mod.MOD_ID, "textures/gui/feeder.png");

    public FeederScreen(FeederScreenHandler handler, Inventory inventory, Component title) {
        super(handler, inventory, title);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor context, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(context, mouseX, mouseY, partialTick);

        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        context.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x, y, 0f, 0f,
                this.imageWidth, this.imageHeight, 256, 256);

        // Draw meat level bar (left bar)
        int meatHeight = (int) (40.0f * menu.getMeatLevel() / menu.getMaxFood());
        if (meatHeight > 0) {
            context.fill(x + 60, y + 10 + 40 - meatHeight, x + 72, y + 10 + 40, 0xFFCC3333);
        }

        // Draw veg level bar (right bar)
        int vegHeight = (int) (40.0f * menu.getVegLevel() / menu.getMaxFood());
        if (vegHeight > 0) {
            context.fill(x + 94, y + 10 + 40 - vegHeight, x + 106, y + 10 + 40, 0xFF33CC33);
        }
    }

    @Override
    protected void init() {
        super.init();
        titleLabelX = (imageWidth - font.width(title)) / 2;
    }
}
