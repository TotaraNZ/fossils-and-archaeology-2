package mod.fossilsarch2.item;

import java.util.List;
import java.util.function.Consumer;

import net.minecraft.component.type.TooltipDisplayComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class TooltipItem extends Item {

    private final List<String> tooltipKeys;

    public TooltipItem(Settings settings, String... tooltipKeys) {
        super(settings);
        this.tooltipKeys = List.of(tooltipKeys);
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, TooltipDisplayComponent displayComponent,
            Consumer<Text> textConsumer, TooltipType type) {
        super.appendTooltip(stack, context, displayComponent, textConsumer, type);

        for (String tooltipKey : tooltipKeys) {
            textConsumer.accept(Text.translatable(tooltipKey).formatted(Formatting.GRAY));
        }
    }
}
