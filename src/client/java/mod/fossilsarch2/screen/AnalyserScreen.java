package mod.fossilsarch2.screen;

import mod.fossilsarch2.FossilsArch2Mod;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class AnalyserScreen extends HandledScreen<AnalyserScreenHandler> {

    private static final Identifier TEXTURE =
            Identifier.of(FossilsArch2Mod.MOD_ID, "textures/gui/analyser.png");

    // Progress bar: 21px wide, 9px tall, at GUI position (80, 22)
    // Texture source at (177, 18)
    private static final int PROGRESS_X = 80;
    private static final int PROGRESS_Y = 22;
    private static final int PROGRESS_MAX_WIDTH = 21;
    private static final int PROGRESS_HEIGHT = 9;
    private static final int PROGRESS_TEX_X = 177;
    private static final int PROGRESS_TEX_Y = 18;

    public AnalyserScreen(AnalyserScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        this.backgroundWidth = 176;
        this.backgroundHeight = 166;
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        int x = (this.width - this.backgroundWidth) / 2;
        int y = (this.height - this.backgroundHeight) / 2;

        context.drawTexture(RenderLayer::getGuiTextured, TEXTURE, x, y, 0, 0,
                this.backgroundWidth, this.backgroundHeight, 256, 256);

        float progress = handler.getProgressRatio();
        if (progress > 0) {
            int progressWidth = (int) (PROGRESS_MAX_WIDTH * progress) + 1;
            context.drawTexture(RenderLayer::getGuiTextured, TEXTURE,
                    x + PROGRESS_X, y + PROGRESS_Y,
                    PROGRESS_TEX_X, PROGRESS_TEX_Y,
                    progressWidth, PROGRESS_HEIGHT, 256, 256);
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        drawMouseoverTooltip(context, mouseX, mouseY);
    }

    @Override
    protected void init() {
        super.init();
        titleX = (backgroundWidth - textRenderer.getWidth(title)) / 2;
    }
}
