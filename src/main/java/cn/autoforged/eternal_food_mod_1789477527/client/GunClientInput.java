package cn.autoforged.eternal_food_mod_1789477527.client;

import cn.autoforged.eternal_food_mod_1789477527.EternalFoodMod;
import cn.autoforged.eternal_food_mod_1789477527.item.EternalDispenserGunItem;
import cn.autoforged.eternal_food_mod_1789477527.network.GunFirePacket;
import cn.autoforged.eternal_food_mod_1789477527.network.ModNetwork;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 发射器枪改为左键发射：
 * <ul>
 *   <li>左键按下：取消原版攻击（不挖方块 / 不打怪），发一包请求发射；</li>
 *   <li>按住左键：每客户端 tick 发一次请求，由服务端按发射间隔限速。</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = EternalFoodMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE,
        value = Dist.CLIENT)
public final class GunClientInput {

    private GunClientInput() {
    }

    @SubscribeEvent
    public static void onInteraction(InputEvent.InteractionKeyMappingTriggered event) {
        if (!event.isAttack()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }
        if (minecraft.player.getMainHandItem().getItem() instanceof EternalDispenserGunItem) {
            event.setCanceled(true);
            ModNetwork.CHANNEL.sendToServer(new GunFirePacket());
        }
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.screen != null) {
            return;
        }
        if (minecraft.options.keyAttack.isDown()
                && minecraft.player.getMainHandItem().getItem() instanceof EternalDispenserGunItem) {
            ModNetwork.CHANNEL.sendToServer(new GunFirePacket());
        }
    }
}
