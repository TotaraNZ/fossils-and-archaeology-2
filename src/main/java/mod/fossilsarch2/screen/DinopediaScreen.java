package mod.fossilsarch2.screen;

import mod.fossilsarch2.FossilsArch2Mod;
import mod.fossilsarch2.dinosaur.Dinosaur;
import mod.fossilsarch2.entity.DinoEggEntity;
import mod.fossilsarch2.entity.DinosaurEntity;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/**
 * Faithful re-creation of the original Fossils & Archaeology dinopedia GUI.
 * Layout matches GuiPedia.java + EntityDinosaur.ShowPedia() from the revival mod.
 * Icons rendered from a single atlas texture.
 */
public class DinopediaScreen extends Screen {

    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath(FossilsArch2Mod.MOD_ID, "textures/gui/dinopedia.png");
    private static final Identifier ICONS =
            Identifier.fromNamespaceAndPath(FossilsArch2Mod.MOD_ID, "textures/gui/pedia_icons.png");

    // Icon positions within the 32x16 atlas
    private static final int ICON_CLOCK_U = 0;
    private static final int ICON_HEART_U = 10;
    private static final int ICON_FOOD_U = 20;
    private static final int ATLAS_W = 32;
    private static final int ATLAS_H = 16;

    // Original GUI constants from GuiPedia.java
    private static final int GUI_WIDTH = 256;
    private static final int GUI_HEIGHT = 174;
    private static final int RIGHT_INDENT = 150;
    private static final int LEFT_INDENT = 30;
    private static final int LINE_HEIGHT = 12;

    // Original colors (alpha must be 0xFF for 26.1 rendering)
    private static final int TEXT_COLOR = 0xFF404040;
    private static final int BLUE_COLOR = 0xFF285AF5;
    private static final int BLACK_COLOR = 0xFF000000;

    private final DinosaurEntity dinosaur;
    private final DinoEggEntity egg;
    private int leftLineCounter;

    private DinopediaScreen(DinosaurEntity dinosaur, DinoEggEntity egg) {
        super(Component.translatable("gui.fossilsarch2.dinopedia"));
        this.dinosaur = dinosaur;
        this.egg = egg;
    }

    public static DinopediaScreen forDinosaur(DinosaurEntity dinosaur) {
        return new DinopediaScreen(dinosaur, null);
    }

    public static DinopediaScreen forEgg(DinoEggEntity egg) {
        return new DinopediaScreen(null, egg);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(context, mouseX, mouseY, partialTick);

        int x = (this.width - GUI_WIDTH) / 2;
        int y = (this.height - GUI_HEIGHT) / 2;

        context.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x, y, 0f, 0f,
                GUI_WIDTH, GUI_HEIGHT, 256, 256);

        leftLineCounter = 0;

        if (dinosaur != null) {
            renderDinosaurPage(context, x, y);
        } else if (egg != null) {
            renderEggPage(context, x, y);
        }
    }

    private void renderDinosaurPage(GuiGraphicsExtractor context, int gx, int gy) {
        Dinosaur dino = dinosaur.getDinosaur();
        if (dino == null) return;

        // ===== RIGHT PAGE (absolute positions matching ShowPedia) =====

        // Custom name (blue) at y=24
        if (dinosaur.hasCustomName()) {
            context.text(font, dinosaur.getCustomName().getString(),
                    gx + RIGHT_INDENT, gy + 24, BLUE_COLOR, false);
        }

        // Species name (black) at y=34
        context.text(font, dino.displayName(),
                gx + RIGHT_INDENT, gy + 34, BLACK_COLOR, false);

        // Clock icon (8x8) + age at y=46
        drawIcon(context, gx + RIGHT_INDENT, gy + 46, ICON_CLOCK_U, 0, 8, 8);
        String dayKey = dinosaur.getDinoAge() == 1
                ? "gui.fossilsarch2.dinopedia.age.day"
                : "gui.fossilsarch2.dinopedia.age.days";
        context.text(font, dinosaur.getDinoAge() + " " + Component.translatable(dayKey).getString(),
                gx + RIGHT_INDENT + 12, gy + 46, TEXT_COLOR, false);

        // Heart icon (9x9) + health at y=58
        drawIcon(context, gx + RIGHT_INDENT, gy + 58, ICON_HEART_U, 0, 9, 9);
        context.text(font,
                String.format("%.0f/%.0f", dinosaur.getHealth(), dinosaur.getMaxHealth()),
                gx + RIGHT_INDENT + 12, gy + 58, TEXT_COLOR, false);

        // Food icon (9x9) + hunger at y=70
        drawIcon(context, gx + RIGHT_INDENT, gy + 70, ICON_FOOD_U, 0, 9, 9);
        context.text(font,
                dinosaur.getHunger() + "/" + dinosaur.getMaxHunger(),
                gx + RIGHT_INDENT + 12, gy + 70, TEXT_COLOR, false);

        // ===== LEFT PAGE (auto-stacking matching AddStringLR) =====

        if (dinosaur.isTame()) {
            addLeftLine(context, gx, gy, Component.translatable("gui.fossilsarch2.dinopedia.owner").getString());
            LivingEntity owner = EntityReference.getLivingEntity(dinosaur.getOwnerReference(), dinosaur.level());
            String ownerName = owner != null ? owner.getName().getString() : "Unknown";
            if (ownerName.length() > 11) ownerName = ownerName.substring(0, 11);
            addLeftLine(context, gx, gy, ownerName);
        }

        addLeftLine(context, gx, gy,
                Component.translatable("gui.fossilsarch2.dinopedia.diet", dino.diet().name()).getString());

        int growthPct = (int) (dinosaur.getGrowthProgress() * 100);
        addLeftLine(context, gx, gy,
                Component.translatable("gui.fossilsarch2.dinopedia.growth", growthPct).getString());
    }

    private void renderEggPage(GuiGraphicsExtractor context, int gx, int gy) {
        String dinoId = egg.getDinoId();
        Dinosaur dino = mod.fossilsarch2.dinosaur.DinosaurUtils.getBySpeciesId(egg.level(), dinoId);
        if (dino == null) return;

        // Egg page: PrintStringLR positions at x = 70 + 81 = 151, y = 12*(line+1)
        int rx = gx + 151;

        // Species name (blue)
        context.text(font, dino.displayName(),
                rx, gy + LINE_HEIGHT * 2, BLUE_COLOR, false);

        // Status
        int progress = egg.getHatchProgress();
        String statusKey = progress >= 0
                ? "gui.fossilsarch2.dinopedia.warm"
                : "gui.fossilsarch2.dinopedia.cold";

        context.text(font,
                Component.translatable("gui.fossilsarch2.dinopedia.status").getString(),
                rx, gy + LINE_HEIGHT * 3, BLUE_COLOR, false);
        context.text(font,
                Component.translatable(statusKey).getString(),
                rx, gy + LINE_HEIGHT * 4, TEXT_COLOR, false);

        // Progress (only if positive)
        if (progress >= 0) {
            int hatchPct = (int) Math.floor((float) progress / (float) dino.growth().hatchTime() * 100.0f);
            context.text(font,
                    Component.translatable("gui.fossilsarch2.dinopedia.progress").getString(),
                    rx, gy + LINE_HEIGHT * 5, BLUE_COLOR, false);
            context.text(font, hatchPct + "/100",
                    rx, gy + LINE_HEIGHT * 6, TEXT_COLOR, false);
        }
    }

    private void drawIcon(GuiGraphicsExtractor context, int x, int y, int u, int v, int w, int h) {
        context.blit(RenderPipelines.GUI_TEXTURED, ICONS, x, y, (float) u, (float) v, w, h, ATLAS_W, ATLAS_H);
    }

    private void addLeftLine(GuiGraphicsExtractor context, int gx, int gy, String text) {
        context.text(font, text,
                gx + LEFT_INDENT, gy + LINE_HEIGHT * (leftLineCounter + 1), TEXT_COLOR, false);
        leftLineCounter++;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
