package mod.fossilsarch2.screen;

import mod.fossilsarch2.FossilsArch2Mod;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class WorktableScreen extends HandledScreen<WorktableScreenHandler> {

    private static final Identifier TEXTURE =
            Identifier.of(FossilsArch2Mod.MOD_ID, "textures/gui/worktable.png");

    public WorktableScreen(WorktableScreenHandler handler, PlayerInventory inventory, Text title) {
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

        // Burn time flame
        if (handler.isBurning()) {
            int burnHeight = (int) (12 * handler.getBurnTimeRatio());
            context.drawTexture(RenderLayer::getGuiTextured, TEXTURE,
                    x + 82, y + 36 + 12 - burnHeight,
                    176, 12 - burnHeight,
                    14, burnHeight + 2, 256, 256);
        }

        // Cook progress arrow
        float cookProgress = handler.getCookProgressRatio();
        if (cookProgress > 0) {
            int progressWidth = (int) (24 * cookProgress) + 1;
            context.drawTexture(RenderLayer::getGuiTextured, TEXTURE,
                    x + 79, y + 18,
                    176, 14,
                    progressWidth, 16, 256, 256);
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
