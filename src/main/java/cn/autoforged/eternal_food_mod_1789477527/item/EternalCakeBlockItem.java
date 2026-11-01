package cn.autoforged.eternal_food_mod_1789477527.item;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

/**
 * 永恒蛋糕的方块物品。
 *
 * <p>只是给 {@link BlockItem} 加一行提示，说明「放下之后右键食用也不消耗」——
 * 其它永恒食物是「拿在手里吃」，蛋糕是「放下再吃」，提示语要区分开，否则玩家会以为
 * 蛋糕和别的永恒食物一样能直接啃。
 */
public class EternalCakeBlockItem extends BlockItem {

    public EternalCakeBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltipComponents,
            TooltipFlag isAdvanced) {
        super.appendHoverText(stack, level, tooltipComponents, isAdvanced);
        tooltipComponents.add(Component
                .translatable("tooltip.eternal_food_mod_1789477527.eternal_place_eat")
                .withStyle(ChatFormatting.GOLD));
    }
}
