package mod.fossilsarch2.screen;

import mod.fossilsarch2.FossilsArch2Mod;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public class WorktableScreen extends AbstractContainerScreen<WorktableScreenHandler> {

    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath(FossilsArch2Mod.MOD_ID, "textures/gui/worktable.png");

    public WorktableScreen(WorktableScreenHandler handler, Inventory inventory, Component title) {
        super(handler, inventory, title);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor context, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(context, mouseX, mouseY, partialTick);

        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        context.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x, y, 0f, 0f,
                this.imageWidth, this.imageHeight, 256, 256);

        // Burn time flame
        if (menu.isBurning()) {
            int burnHeight = (int) (12 * menu.getBurnTimeRatio());
            context.blit(RenderPipelines.GUI_TEXTURED, TEXTURE,
                    x + 82, y + 36 + 12 - burnHeight,
                    176f, (float) (12 - burnHeight),
                    14, burnHeight + 2, 256, 256);
        }

        // Cook progress arrow
        float cookProgress = menu.getCookProgressRatio();
        if (cookProgress > 0) {
            int progressWidth = (int) (24 * cookProgress) + 1;
            context.blit(RenderPipelines.GUI_TEXTURED, TEXTURE,
                    x + 79, y + 18,
                    176f, 14f,
                    progressWidth, 16, 256, 256);
        }
    }

    @Override
    protected void init() {
        super.init();
        titleLabelX = (imageWidth - font.width(title)) / 2;
    }
}
