package mod.fossilsarch2.screen;

import mod.fossilsarch2.FossilsArch2Mod;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class FeederScreen extends HandledScreen<FeederScreenHandler> {

    private static final Identifier TEXTURE =
            Identifier.of(FossilsArch2Mod.MOD_ID, "textures/gui/feeder.png");

    public FeederScreen(FeederScreenHandler handler, PlayerInventory inventory, Text title) {
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

        // Draw meat level bar (left bar)
        int meatHeight = (int) (40.0f * handler.getMeatLevel() / handler.getMaxFood());
        if (meatHeight > 0) {
            context.fill(x + 60, y + 10 + 40 - meatHeight, x + 72, y + 10 + 40, 0xFFCC3333);
        }

        // Draw veg level bar (right bar)
        int vegHeight = (int) (40.0f * handler.getVegLevel() / handler.getMaxFood());
        if (vegHeight > 0) {
            context.fill(x + 94, y + 10 + 40 - vegHeight, x + 106, y + 10 + 40, 0xFF33CC33);
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
