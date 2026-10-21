package cn.autoforged.eternal_food_mod_1789477527.event;

import cn.autoforged.eternal_food_mod_1789477527.EternalFoodMod;
import cn.autoforged.eternal_food_mod_1789477527.item.ModItems;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.EntityTeleportEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 永恒弹射物的服务端行为补丁。
 *
 * <p>永恒末影珍珠沿用原版 {@code ThrownEnderpearl} 的传送逻辑，但需求要求传送时不扣玩家血。
 * 原版在落地时通过 {@code EntityTeleportEvent.EnderPearl} 的 {@code attackDamage}（默认 5.0）
 * 对玩家造成坠落伤害；这里仅对「弹射物就是永恒末影珍珠」的传送事件把攻击伤害置零，
 * 不影响普通末影珍珠。攻击伤害为 0 时 {@code LivingEntity#actuallyHurt} 直接返回，玩家不扣血。
 */
@Mod.EventBusSubscriber(modid = EternalFoodMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class EternalProjectileHandler {

    @SubscribeEvent
    public static void onEnderPearlLand(EntityTeleportEvent.EnderPearl event) {
        ItemStack pearl = event.getPearlEntity().getItem();
        if (pearl.is(ModItems.ETERNAL_ENDER_PEARL.get())) {
            event.setAttackDamage(0.0F);
        }
    }
}
