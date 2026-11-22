package cn.autoforged.eternal_food_mod_1789477527.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import java.util.List;

/** 永恒烟花火箭导弹：由永恒烟花火箭 + TNT 合成，继承一个烟火之星与飞行时长。 */
public class EternalFireworkMissileItem extends EternalFireworkRocketItem {

    public EternalFireworkMissileItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        // 隐藏原版烟花信息，只显示导弹特性
        tooltip.add(Component.translatable(
                "tooltip.eternal_food_mod_1789477527.eternal_firework_missile.damage")
                .withStyle(ChatFormatting.RED));
        tooltip.add(Component.translatable(
                "tooltip.eternal_food_mod_1789477527.eternal_firework_missile.star")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable(
                "tooltip.eternal_food_mod_1789477527.eternal_use")
                .withStyle(ChatFormatting.GOLD));
    }
}
