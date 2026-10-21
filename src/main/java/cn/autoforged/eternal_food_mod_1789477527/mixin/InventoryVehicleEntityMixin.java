package cn.autoforged.eternal_food_mod_1789477527.mixin;

import cn.autoforged.eternal_food_mod_1789477527.item.ModItems;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * 「沉浸式飞机」(Immersive Aircraft) 兼容：飞行器火箭助推不消耗「永恒烟花火箭」。
 *
 * <p>该 mod 的飞行器有一个 booster 槽位（仅接受 {@code FireworkRocketItem}）。玩家按助推键时，
 * {@code InventoryVehicleEntity#boost()} 会遍历 booster 槽里的烟花，读取其
 * {@code Fireworks.Flight} 作为推进时长，并对每个烟花执行 {@code ItemStack#shrink(1)} 将其消耗。
 * 该消耗逻辑位于编译期生成的合成 lambda 方法 {@code lambda$boost$0(ItemStack)} 中。
 *
 * <p>本 mixin 用字符串 {@code targets} 引用该 mod 的类（编译期不依赖该 mod），并把该 lambda 里的
 * {@code ItemStack#shrink(I)V} 调用重定向为：当物品是永恒烟花火箭时不做任何事，其余物品照常消耗。
 * 这样永恒烟花火箭留在 booster 槽中可反复助推。
 *
 * <p>本 mixin 标 {@link Pseudo}，编译期不要求目标类存在（未安装该 mod 也能编译）；运行时目标不存在
 * 则跳过。它同时放在单独的 {@code required:false} 配置里，未安装「沉浸式飞机」时会被安全跳过。
 * 目标方法名 {@code lambda$boost$0} 是该 mod 0.6.2 ~ 1.5.0 各 1.20.1 版本中唯一持有
 * {@code shrink} 调用的 booster 助推 lambda。
 */
@Pseudo
@Mixin(targets = "immersive_aircraft.entity.InventoryVehicleEntity")
public class InventoryVehicleEntityMixin {

    @Redirect(method = "lambda$boost$0", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/item/ItemStack;shrink(I)V"))
    private static void eternal_food$keepEternalFireworkRocket(ItemStack stack, int amount) {
        if (stack.is(ModItems.ETERNAL_FIREWORK_ROCKET.get())) {
            return;
        }
        stack.shrink(amount);
    }
}
