package cn.autoforged.eternal_food_mod_1789477527.mixin;

import cn.autoforged.eternal_food_mod_1789477527.effect.ModEffects;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 「饕餮」效果：让持有该效果的玩家在满饥饿 / 满饱和度时也能进食。
 *
 * <p>原版进食入口 {@code Item#use} 用 {@code Player#canEat(food.canAlwaysEat())} 拦截，
 * 该方法默认在玩家不无敌、食物非 alwaysEat、且 {@code FoodData#needsFood()} 为 false
 * （饥饿值已满）时返回 false，从而拒绝进食。这里在 HEAD 处判断玩家是否持有「饕餮」，
 * 持有则直接返回 true，使任何食物都能在满饥饿时正常食用。蛋糕方块（{@code CakeBlock}）
 * 同样走 {@code canEat}，因此也一并放开。
 */
@Mixin(Player.class)
public abstract class PlayerMixin {

    @Inject(method = "canEat", at = @At("HEAD"), cancellable = true)
    private void eternal_food$gulosityAllowsEating(boolean canAlwaysEat, CallbackInfoReturnable<Boolean> cir) {
        if (!canAlwaysEat && ((Player) (Object) this).hasEffect(ModEffects.GULOSITY.get())) {
            cir.setReturnValue(true);
        }
    }
}
