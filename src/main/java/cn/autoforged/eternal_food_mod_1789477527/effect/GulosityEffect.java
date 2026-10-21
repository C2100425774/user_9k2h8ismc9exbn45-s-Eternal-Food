package cn.autoforged.eternal_food_mod_1789477527.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * 「饕餮」（gulosity）状态效果：持续期间玩家无视饥饿值上限，满饥饿 / 满饱和度也能正常进食。
 *
 * <p>效果本身不需要每 tick 做任何事——进食判定在 {@code Player#canEat} 里，由
 * {@code mixin/PlayerMixin} 检测玩家是否持有本效果。因此这里只登记类别与颜色（决定
 * HUD 效果条颜色与粒子颜色，具体图标取自 {@code textures/mob_effect/gulosity.png}）。
 */
public class GulosityEffect extends MobEffect {

    public GulosityEffect() {
        super(MobEffectCategory.BENEFICIAL, 0x7A4B2A);
    }
}
