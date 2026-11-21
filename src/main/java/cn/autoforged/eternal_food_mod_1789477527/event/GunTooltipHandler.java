package cn.autoforged.eternal_food_mod_1789477527.event;

import cn.autoforged.eternal_food_mod_1789477527.EternalFoodMod;
import cn.autoforged.eternal_food_mod_1789477527.item.EternalDispenserGunItem;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 发射器枪的「按住 Shift 查看附魔效果」提示。
 *
 * <p>按住 Shift 时把整份提示**替换**成「物品名 + 附魔效果」：
 * 原版会自动把附魔名字追加在提示末尾，这里连它们一起去掉，避免重复和提示框过长；
 * 松开 Shift 就回到普通说明 + 原版附魔名字。
 */
@Mod.EventBusSubscriber(modid = EternalFoodMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE,
        value = Dist.CLIENT)
public final class GunTooltipHandler {

    private GunTooltipHandler() {
    }

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        if (stack.isEmpty() || !(stack.getItem() instanceof EternalDispenserGunItem)) {
            return;
        }
        if (!Screen.hasShiftDown()) {
            return;
        }
        List<Component> tooltip = event.getToolTip();
        tooltip.clear();
        tooltip.add(stack.getHoverName());
        boolean any = false;
        for (Enchantment enchantment : EternalDispenserGunItem.supportedEnchantments()) {
            if (EnchantmentHelper.getItemEnchantmentLevel(enchantment, stack) <= 0) {
                continue;
            }
            any = true;
            tooltip.add(Component.translatable(
                    EternalDispenserGunItem.enchantEffectKey(enchantment))
                    .withStyle(ChatFormatting.GRAY));
        }
        if (!any) {
            tooltip.add(Component.translatable(
                    "tooltip.eternal_food_mod_1789477527.dispenser_gun.no_enchant")
                    .withStyle(ChatFormatting.DARK_GRAY));
        }
    }
}
