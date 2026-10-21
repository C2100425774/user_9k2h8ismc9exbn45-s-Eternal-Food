package cn.autoforged.eternal_food_mod_1789477527.mixin;

import cn.autoforged.eternal_food_mod_1789477527.block.ModBlocks;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import java.util.List;
import net.minecraft.world.item.crafting.Ingredient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 让女仆把「永恒蛋糕」也当成"诱惑物"，从而在玩家手持时跑过来卖萌（乞讨动画）。
 *
 * <p>车万女仆的 {@code MaidBegTask} 判定条件是
 * {@code owner.getTemptationItem().test(玩家手上物品)}，而
 * {@code EntityMaid#getTemptationItem()} 只返回配置项 {@code MaidTemptationItem}
 * （默认 {@code minecraft:cake}），**没有**对应的物品标签可以扩展。
 * 因此这里在返回值上追加一个 Ingredient，把永恒蛋糕并进去。
 *
 * <p>本 mixin 属于 {@code *.compat.mixins.json}（{@code required: false}）：
 * 没装车万女仆时目标类不存在，Mixin 会直接跳过，不会影响启动。
 */
@Mixin(EntityMaid.class)
public abstract class EntityMaidTemptationMixin {

    /** 缓存合并后的 Ingredient：这个方法每个 AI tick 都会被调用，别每次都重新构造。 */
    private static Ingredient eternal_food$temptationCache;

    @Inject(method = "getTemptationItem", at = @At("RETURN"), cancellable = true)
    private void eternal_food$addEternalCakeAsTemptation(CallbackInfoReturnable<Ingredient> cir) {
        Ingredient cached = eternal_food$temptationCache;
        if (cached == null) {
            cached = Ingredient.merge(List.of(cir.getReturnValue(),
                    Ingredient.of(ModBlocks.ETERNAL_CAKE.get().asItem())));
            eternal_food$temptationCache = cached;
        }
        cir.setReturnValue(cached);
    }
}
