package cn.autoforged.eternal_food_mod_1789477527.item;

import java.util.List;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

/**
 * 永恒之星：由下界之星等 9 种材料无序合成，可无限次食用而不被消耗。
 *
 * <p>食用后把玩家当前拥有的所有 buff 与 debuff 的持续时间改为无限
 * （{@link MobEffectInstance#INFINITE_DURATION} = -1，即指令里表示的那一种「无限」，
 * 而不是一个超大数值），等级与可见性等属性保持不变。
 *
 * <p>此外永恒之星还能在铁砧中与药水 / 附魔永恒之鱼合成，把其效果的持续时间改为无限，
 * 详见 {@code cn.autoforged.eternal_food_mod_1789477527.event.AnvilCombineHandler}。
 */
public class EternalStarItem extends EternalFoodItem {

    public EternalStarItem(Properties properties) {
        super(Items.NETHER_STAR, properties, false,
                "tooltip.eternal_food_mod_1789477527.eternal_star");
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        // 与干海带一样快（原版 fast 食物 = 16 tick）；基类会从 baseItem（下界之星，非食物）
        // 取到 0 再回退成 32，故此处显式覆盖。
        return 16;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        // 永恒之星自带附魔光效。
        return true;
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        ItemStack result = super.finishUsingItem(stack, level, entity);
        if (!level.isClientSide) {
            // getActiveEffects 返回内部映射的实时视图；复制一份以免遍历时 addEffect 修改集合。
            for (MobEffectInstance instance : List.copyOf(entity.getActiveEffects())) {
                entity.addEffect(new MobEffectInstance(instance.getEffect(),
                        MobEffectInstance.INFINITE_DURATION, instance.getAmplifier(),
                        instance.isAmbient(), instance.isVisible(), instance.showIcon()));
            }
        }
        return result;
    }
}
