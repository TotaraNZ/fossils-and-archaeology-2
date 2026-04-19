package mod.fossilsarch2.screen;

import mod.fossilsarch2.FossilsArch2Mod;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public class AnalyserScreen extends AbstractContainerScreen<AnalyserScreenHandler> {

    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath(FossilsArch2Mod.MOD_ID, "textures/gui/analyser.png");

    // Progress bar: 21px wide, 9px tall, at GUI position (80, 22)
    // Texture source at (177, 18)
    private static final int PROGRESS_X = 80;
    private static final int PROGRESS_Y = 22;
    private static final int PROGRESS_MAX_WIDTH = 21;
    private static final int PROGRESS_HEIGHT = 9;
    private static final int PROGRESS_TEX_X = 177;
    private static final int PROGRESS_TEX_Y = 18;

    public AnalyserScreen(AnalyserScreenHandler handler, Inventory inventory, Component title) {
        super(handler, inventory, title);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor context, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(context, mouseX, mouseY, partialTick);

        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        context.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x, y, 0f, 0f,
                this.imageWidth, this.imageHeight, 256, 256);

        float progress = menu.getProgressRatio();
        if (progress > 0) {
            int progressWidth = (int) (PROGRESS_MAX_WIDTH * progress) + 1;
            context.blit(RenderPipelines.GUI_TEXTURED, TEXTURE,
                    x + PROGRESS_X, y + PROGRESS_Y,
                    (float) PROGRESS_TEX_X, (float) PROGRESS_TEX_Y,
                    progressWidth, PROGRESS_HEIGHT, 256, 256);
        }
    }

    @Override
    protected void init() {
        super.init();
        titleLabelX = (imageWidth - font.width(title)) / 2;
    }
}
