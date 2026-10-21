package cn.autoforged.eternal_food_mod_1789477527.mixin;

import cn.autoforged.eternal_food_mod_1789477527.item.ModItems;
import java.util.function.Predicate;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ProjectileWeaponItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 让弩把「永恒烟花火箭」当作原版烟花火箭识别：
 *
 * <p>原版用身份比较（{@code ItemStack.is(Items.FIREWORK_ROCKET)}）判断烟花，自定义物品无法直接匹配，
 * 因此这里：
 * <ul>
 *   <li>扩展弩的可装填弹药谓词，使永恒烟花火箭可被手持 / 背包选中；</li>
 *   <li>在 {@code shootProjectile} 里把「是否烟花」的判定扩展为包含永恒烟花火箭，发射出烟花实体；</li>
 *   <li>在 {@code getShootingPower} 里同步该判定，保持烟花应有的 1.6 射击力度。</li>
 * </ul>
 * 不消耗由 {@code event/ModAmmoHandler} 返回副本实现。
 */
@Mixin(CrossbowItem.class)
public class CrossbowItemMixin {

    @Inject(method = "getSupportedHeldProjectiles", at = @At("HEAD"), cancellable = true)
    private void eternal_food$includeHeldEternalFirework(CallbackInfoReturnable<Predicate<ItemStack>> cir) {
        cir.setReturnValue(ProjectileWeaponItem.ARROW_OR_FIREWORK.or(
                stack -> stack.is(ModItems.ETERNAL_FIREWORK_ROCKET.get())));
    }

    @Inject(method = "getAllSupportedProjectiles", at = @At("HEAD"), cancellable = true)
    private void eternal_food$includeEternalFirework(CallbackInfoReturnable<Predicate<ItemStack>> cir) {
        cir.setReturnValue(ProjectileWeaponItem.ARROW_ONLY.or(
                stack -> stack.is(ModItems.ETERNAL_FIREWORK_ROCKET.get())));
    }

    @Redirect(method = "shootProjectile", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/item/ItemStack;is(Lnet/minecraft/world/item/Item;)Z"))
    private static boolean eternal_food$treatAsFirework(ItemStack stack, net.minecraft.world.item.Item item) {
        return stack.is(item)
                || (item == Items.FIREWORK_ROCKET && stack.is(ModItems.ETERNAL_FIREWORK_ROCKET.get()));
    }

    @Redirect(method = "getShootingPower", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/item/CrossbowItem;containsChargedProjectile"
                    + "(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/Item;)Z"))
    private static boolean eternal_food$shootingPower(ItemStack crossbowStack, net.minecraft.world.item.Item item) {
        return CrossbowItem.containsChargedProjectile(crossbowStack, item)
                || CrossbowItem.containsChargedProjectile(crossbowStack,
                        ModItems.ETERNAL_FIREWORK_ROCKET.get());
    }
}
