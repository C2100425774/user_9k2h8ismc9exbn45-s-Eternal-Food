package cn.autoforged.eternal_food_mod_1789477527.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;

/** 客户端专用包处理（只会在物理客户端被类加载）。 */
public final class ClientPacketHandler {

    private ClientPacketHandler() {
    }

    /** 用给定物品在屏幕上播放一次原版不死图腾激活动画。 */
    public static void showTotemAnimation(ItemStack stack) {
        Minecraft.getInstance().gameRenderer.displayItemActivation(stack);
    }
}
