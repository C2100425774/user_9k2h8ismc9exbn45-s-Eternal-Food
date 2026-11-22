package cn.autoforged.eternal_food_mod_1789477527.mixin;

import net.minecraft.world.entity.projectile.AbstractArrow;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 防水：参考三叉戟 {@code ThrownTrident#getWaterInertia()} 的 0.99，
 * 让带 {@code EternalGunWaterproof} 标记的箭 / 子弹在水中几乎不损失速度。
 */
@Mixin(AbstractArrow.class)
public abstract class AbstractArrowWaterMixin {

    @Inject(method = "getWaterInertia", at = @At("HEAD"), cancellable = true)
    private void eternal$waterInertia(CallbackInfoReturnable<Float> cir) {
        AbstractArrow self = (AbstractArrow) (Object) this;
        if (self.getPersistentData().getBoolean("EternalGunWaterproof")) {
            cir.setReturnValue(0.99F);
        }
    }
}
