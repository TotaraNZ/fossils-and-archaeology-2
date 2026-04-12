package mod.fossilsarch2.screen;

import mod.fossilsarch2.FossilsArch2Mod;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public class FeederScreen extends AbstractContainerScreen<FeederScreenHandler> {
    private static final int BAR_HEIGHT = 46;
    private static final int BAR_WIDTH = 3;
    private static final int MEAT_BAR_X = 66;
    private static final int VEG_BAR_X = 110;
    private static final int BAR_BOTTOM_Y = 55;
    private static final int MEAT_LABEL_X = 23;
    private static final int VEG_LABEL_X = 120;
    private static final int FOOD_LABEL_Y = 32;

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

        int meatHeight = BAR_HEIGHT * menu.getMeatLevel() / menu.getMaxFood();
        if (meatHeight > 0) {
            context.blit(RenderPipelines.GUI_TEXTURED, TEXTURE,
                    x + MEAT_BAR_X, y + BAR_BOTTOM_Y - meatHeight,
                    176f, (float) (BAR_HEIGHT - meatHeight),
                    BAR_WIDTH, meatHeight, 256, 256);
        }

        int vegHeight = BAR_HEIGHT * menu.getVegLevel() / menu.getMaxFood();
        if (vegHeight > 0) {
            context.blit(RenderPipelines.GUI_TEXTURED, TEXTURE,
                    x + VEG_BAR_X, y + BAR_BOTTOM_Y - vegHeight,
                    176f, (float) (BAR_HEIGHT - vegHeight),
                    BAR_WIDTH, vegHeight, 256, 256);
        }
    }

    @Override
    protected void init() {
        super.init();
        titleLabelX = imageWidth / 6 - font.width(title) / 2;
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor context, int mouseX, int mouseY) {
        super.extractLabels(context, mouseX, mouseY);
        context.text(font, String.valueOf(menu.getMeatLevel()), MEAT_LABEL_X, FOOD_LABEL_Y, 0xFF0000);
        context.text(font, String.valueOf(menu.getVegLevel()), VEG_LABEL_X, FOOD_LABEL_Y, 0x03B703);
    }
}
