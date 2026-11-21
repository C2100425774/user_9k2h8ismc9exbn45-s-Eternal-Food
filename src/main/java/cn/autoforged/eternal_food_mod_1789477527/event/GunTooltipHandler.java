package cn.autoforged.eternal_food_mod_1789477527.event;

import cn.autoforged.eternal_food_mod_1789477527.EternalFoodMod;
import cn.autoforged.eternal_food_mod_1789477527.item.EternalDispenserGunItem;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 发射器枪的「按住 Shift 查看配置效果」提示。
 *
 * <p>按住 Shift 时把整份提示替换成「物品名 + 当前开启的配置说明」，
 * 原版追加的附魔名字也一起去掉；松开恢复普通说明。
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
        List<Component> effects = EternalDispenserGunItem.activeConfigLines(stack);
        if (effects.isEmpty()) {
            tooltip.add(Component.translatable(
                    "tooltip.eternal_food_mod_1789477527.dispenser_gun.no_enchant")
                    .withStyle(ChatFormatting.DARK_GRAY));
        } else {
            tooltip.addAll(effects);
        }
    }
}
