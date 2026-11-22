package cn.autoforged.eternal_food_mod_1789477527.event;

import cn.autoforged.eternal_food_mod_1789477527.EternalFoodMod;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 永恒烟花火箭导弹：把原版烟花的「5 + 2×星数」伤害改成固定中心 20 点，
 * 保持原版半径 5 与 sqrt 衰减；烟火之星只负责外观。
 */
@Mod.EventBusSubscriber(modid = EternalFoodMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class EternalMissileDamageHandler {

    private EternalMissileDamageHandler() {
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        Entity direct = event.getSource().getDirectEntity();
        if (!(direct instanceof FireworkRocketEntity rocket)) {
            return;
        }
        if (rocket.getItem().isEmpty() || !rocket.getItem().hasTag()
                || !rocket.getItem().getTag().getBoolean("EternalMissile")) {
            return;
        }
        double distance = rocket.distanceTo(event.getEntity());
        double factor = Math.sqrt(Math.max(0.0, (5.0 - distance) / 5.0));
        event.setAmount((float) (20.0 * factor));
    }
}
