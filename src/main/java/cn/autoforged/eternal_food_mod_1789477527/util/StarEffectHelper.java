package cn.autoforged.eternal_food_mod_1789477527.util;

import cn.autoforged.eternal_food_mod_1789477527.effect.ModEffects;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

/**
 * 两种永恒之星的共用效果处理（食用、铁砧合成两条路径都走这里）。
 *
 * <ul>
 *   <li>「永恒之星 - 时间」：把持续时间改为无限（{@link MobEffectInstance#INFINITE_DURATION}）；</li>
 *   <li>「永恒之星 - 力量」：把所有状态的等级 +1（amplifier+1，buff 与 debuff 都算；
 *       负面效果可以用永恒奶桶plus 洗掉，所以给 debuff 升级也是有用的），
 *       等级上限只有 1 的效果（原版里只有「有 / 无」两态的效果）保持原等级。</li>
 * </ul>
 */
public final class StarEffectHelper {

    private StarEffectHelper() {
    }

    /**
     * 等级提升没有实际意义的效果（不分正面 / 负面）。
     *
     * <p>原版这些效果只有「生效 / 不生效」两态，amplifier 再高也不会改变效果，
     * 所以「力量之星」不对它们 +1。本模组的「饕餮」同理。
     */
    public static boolean isSingleLevelEffect(MobEffect effect) {
        return effect == MobEffects.FIRE_RESISTANCE
                || effect == MobEffects.WATER_BREATHING
                || effect == MobEffects.INVISIBILITY
                || effect == MobEffects.NIGHT_VISION
                || effect == MobEffects.SLOW_FALLING
                || effect == MobEffects.CONDUIT_POWER
                || effect == MobEffects.DOLPHINS_GRACE
                // 负面 / 中立里同样只有「有 / 无」两态的效果
                || effect == MobEffects.BLINDNESS
                || effect == MobEffects.CONFUSION
                || effect == MobEffects.DARKNESS
                || effect == MobEffects.GLOWING
                || effect == ModEffects.GULOSITY.get();
    }

    /** 复制一个效果并把持续时间改为无限（-1）；瞬间效果没有时长概念，保持原时长。 */
    public static MobEffectInstance withInfiniteDuration(MobEffectInstance instance) {
        int duration = instance.getEffect().isInstantenous()
                ? instance.getDuration()
                : MobEffectInstance.INFINITE_DURATION;
        return new MobEffectInstance(instance.getEffect(), duration, instance.getAmplifier(),
                instance.isAmbient(), instance.isVisible(), instance.showIcon());
    }

    /**
     * 复制一个效果并把它的等级 +1（buff 与 debuff 都算）。
     *
     * <p>{@link #isSingleLevelEffect} 里的效果原样复制（等级不变）；其余属性（时长、是否环境音、
     * 是否显示粒子 / 图标）保持不变。
     */
    public static MobEffectInstance withRaisedLevel(MobEffectInstance instance) {
        MobEffect effect = instance.getEffect();
        int amplifier = instance.getAmplifier();
        if (!isSingleLevelEffect(effect)) {
            amplifier += 1;
        }
        return new MobEffectInstance(effect, instance.getDuration(), amplifier,
                instance.isAmbient(), instance.isVisible(), instance.showIcon());
    }

    /** 该效果是否会被「力量之星」提升等级（供铁砧判断「合成了也没变化」时用）。 */
    public static boolean canRaiseLevel(MobEffectInstance instance) {
        return !isSingleLevelEffect(instance.getEffect());
    }
}
