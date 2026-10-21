package cn.autoforged.eternal_food_mod_1789477527.event;

import cn.autoforged.eternal_food_mod_1789477527.EternalFoodMod;
import cn.autoforged.eternal_food_mod_1789477527.item.ModItems;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingGetProjectileEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 让永恒药箭 / 永恒烟花火箭作为弩的弹药时不消耗。
 *
 * <p>弩在 {@code CrossbowItem#loadProjectile} 里对 {@code getProjectile} 返回的栈执行
 * {@code split(1)}。这里在该栈为永恒药箭 / 永恒烟花火箭时改为返回副本，消耗的是副本，
 * 玩家物品栏里的原物品保持不变（弓另有 {@code ArrowItem#isInfinite} 处理，发射器见
 * {@link ModDispenseBehaviors}）。弩能选中永恒烟花火箭由 {@code mixin/CrossbowItemMixin} 提供。
 */
@Mod.EventBusSubscriber(modid = EternalFoodMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ModAmmoHandler {

    @SubscribeEvent
    public static void onGetProjectile(LivingGetProjectileEvent event) {
        ItemStack ammo = event.getProjectileItemStack();
        if (ammo.is(ModItems.ETERNAL_TIPPED_ARROW.get())
                || ammo.is(ModItems.ETERNAL_FIREWORK_ROCKET.get())) {
            event.setProjectileItemStack(ammo.copy());
        }
    }
}
