package cn.autoforged.eternal_food_mod_1789477527.item;

import cn.autoforged.eternal_food_mod_1789477527.util.StarEffectHelper;
import java.util.List;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

/**
 * 永恒之星（时间 / 力量）：由下界之星等 9 种材料无序合成，可无限次食用而不被消耗。
 * 两个变体可以在工作台里 1:1 互相转化。
 *
 * <ul>
 *   <li>{@link Mode#TIME}「永恒之星 - 时间」：食用后把自身当前所有 buff 与 debuff 的持续时间
 *       改为无限（{@link MobEffectInstance#INFINITE_DURATION} = -1，即指令里的「无限」），
 *       等级与可见性等属性保持不变；</li>
 *   <li>{@link Mode#POWER}「永恒之星 - 力量」：食用后把自身当前所有正面 buff 的等级 +1
 *       （amplifier+1），debuff 不动；等级上限只有 1 的 buff（见
 *       {@link StarEffectHelper#isSingleLevelBuff}）保持不变。</li>
 * </ul>
 *
 * <p>两个变体都还能在铁砧中与药水 / 永恒药箭 / 附魔永恒之鱼合成，分别把其效果「变为无限」或
 * 「等级 +1」，详见 {@code cn.autoforged.eternal_food_mod_1789477527.event.AnvilCombineHandler}。
 */
public class EternalStarItem extends EternalFoodItem {

    /** 永恒之星的两个变体。 */
    public enum Mode {
        /** 时间：所有 buff / debuff 持续时间变为无限。 */
        TIME("tooltip.eternal_food_mod_1789477527.eternal_star"),
        /** 力量：所有正面 buff 等级 +1。 */
        POWER("tooltip.eternal_food_mod_1789477527.eternal_star_power");

        private final String hintKey;

        Mode(String hintKey) {
            this.hintKey = hintKey;
        }

        public String hintKey() {
            return this.hintKey;
        }
    }

    private final Mode mode;

    public EternalStarItem(Properties properties, Mode mode) {
        super(Items.NETHER_STAR, properties, false, mode.hintKey());
        this.mode = mode;
    }

    public Mode getMode() {
        return this.mode;
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
                if (this.mode == Mode.TIME) {
                    entity.addEffect(StarEffectHelper.withInfiniteDuration(instance));
                } else {
                    // 力量：只有真正能 +1 的效果才写回，避免对「等级上限只有 1」的 buff 做无意义刷新。
                    if (StarEffectHelper.canRaiseLevel(instance)) {
                        entity.addEffect(StarEffectHelper.withRaisedLevel(instance));
                    }
                }
            }
        }
        return result;
    }
}
