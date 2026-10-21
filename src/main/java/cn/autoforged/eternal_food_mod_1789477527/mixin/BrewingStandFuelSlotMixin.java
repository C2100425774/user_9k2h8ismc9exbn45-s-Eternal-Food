package cn.autoforged.eternal_food_mod_1789477527.mixin;

import cn.autoforged.eternal_food_mod_1789477527.item.ModItems;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 让酿造台 GUI 的燃料槽接受永恒燃料。
 *
 * <p>原版 {@code BrewingStandMenu.FuelSlot.mayPlaceItem} 硬编码只接受烈焰粉，
 * 这里在判定开头放行永恒燃料。
 */
@Mixin(targets = "net.minecraft.world.inventory.BrewingStandMenu$FuelSlot")
public class BrewingStandFuelSlotMixin {

    @Inject(method = "mayPlaceItem", at = @At("HEAD"), cancellable = true)
    private static void eternal_food$allowEternalFuel(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (stack.is(ModItems.ETERNAL_FUEL.get())) {
            cir.setReturnValue(true);
        }
    }
}
