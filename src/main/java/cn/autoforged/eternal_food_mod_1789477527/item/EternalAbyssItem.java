package cn.autoforged.eternal_food_mod_1789477527.item;

import cn.autoforged.eternal_food_mod_1789477527.effect.ModEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

/**
 * 无底坑（eternal_abyss）：可无限食用而不被消耗。
 *
 * <p>食用后给予自身 8 分钟（9600 tick）「饕餮」效果；该效果期间玩家满饥饿 / 满饱和度也能
 * 正常进食（见 {@code mixin/PlayerMixin}），从而可以靠进食累积饱和度快速回血或触发食物效果。
 *
 * <p>基础物品用干海带，仅用于继承「快速进食」动画（16 tick）与进食音效；饱食判定沿用
 * {@code alwaysEat}，因此满饥饿时也能食用本物品来获得效果。
 */
public class EternalAbyssItem extends EternalFoodItem {

    /** 8 分钟 = 480 秒 × 20 tick。 */
    private static final int GULOSITY_DURATION = 9600;

    public EternalAbyssItem(Properties properties) {
        super(Items.DRIED_KELP, properties, false, "tooltip.eternal_food_mod_1789477527.abyss");
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        ItemStack result = super.finishUsingItem(stack, level, entity);
        if (!level.isClientSide) {
            entity.addEffect(new MobEffectInstance(ModEffects.GULOSITY.get(), GULOSITY_DURATION));
        }
        return result;
    }
}
