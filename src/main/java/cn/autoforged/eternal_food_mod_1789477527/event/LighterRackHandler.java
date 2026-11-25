package cn.autoforged.eternal_food_mod_1789477527.event;

import cn.autoforged.eternal_food_mod_1789477527.EternalFoodMod;
import cn.autoforged.eternal_food_mod_1789477527.item.EternalLighterItem;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 在物品 useOn 之前处理森罗物语厨具架：调用厨具架自己的挂载逻辑，
 * 再消耗手里的打火机，避免创造模式绕过消耗。
 */
@Mod.EventBusSubscriber(modid = EternalFoodMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class LighterRackHandler {

    private LighterRackHandler() {
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        ItemStack stack = event.getItemStack();
        if (!(stack.getItem() instanceof EternalLighterItem)
                || event.getEntity().isShiftKeyDown()) {
            return;
        }
        if (!EternalLighterItem.isKitchenwareRack(event.getLevel(), event.getPos())) {
            return;
        }
        if (EternalLighterItem.hangOnRack(event.getLevel(), event.getPos(),
                event.getEntity(), stack)) {
            event.setUseBlock(Event.Result.DENY);
            event.setUseItem(Event.Result.DENY);
            event.setCanceled(true);
        }
    }
}
