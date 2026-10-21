package cn.autoforged.eternal_food_mod_1789477527.item;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

/**
 * 永恒燃料：可无限次作为熔炉燃料使用而不被消耗。
 *
 * <ul>
 *   <li>燃烧时长返回 {@link Integer#MAX_VALUE}（2147483647 tick），配合"不消耗"即无限燃料。</li>
 *   <li>把自身当作合成剩余物返回（{@code hasCraftingRemainingItem}/{@code getCraftingRemainingItem}），
 *       原版熔炉点火时会把燃料槽里的它替换为自身副本，因此永不消耗
 *       （见 AbstractFurnaceBlockEntity#serverTick）。</li>
 *   <li>放入酿造台燃料槽时补充 20 能量，见
 *       {@code cn.autoforged.eternal_food_mod_1789477527.event.EternalInteractionHandler}。</li>
 * </ul>
 */
public class EternalFuelItem extends Item {

    public EternalFuelItem(Properties properties) {
        super(properties);
    }

    /** 2147483647 tick 燃烧时长（int 上限）。 */
    @Override
    public int getBurnTime(ItemStack stack, RecipeType<?> recipeType) {
        return Integer.MAX_VALUE;
    }

    /** 始终视为有合成剩余物，返回自身，从而在熔炉中不被消耗。 */
    @Override
    public boolean hasCraftingRemainingItem(ItemStack stack) {
        return true;
    }

    @Override
    public ItemStack getCraftingRemainingItem(ItemStack stack) {
        return stack.copy();
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltipComponents,
            TooltipFlag isAdvanced) {
        tooltipComponents.add(Component.translatable("tooltip.eternal_food_mod_1789477527.eternal_fuel")
                .withStyle(ChatFormatting.GOLD));
    }
}
