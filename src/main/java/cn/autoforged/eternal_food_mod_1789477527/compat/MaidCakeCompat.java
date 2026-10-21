package cn.autoforged.eternal_food_mod_1789477527.compat;

import com.github.tartaricacid.touhoulittlemaid.api.ILittleMaid;
import com.github.tartaricacid.touhoulittlemaid.api.LittleMaidExtension;
import com.github.tartaricacid.touhoulittlemaid.entity.ai.edible.MaidEdibleBlockManager;

/**
 * 车万女仆（Touhou Little Maid）扩展入口。
 *
 * <p>TLM 启动时会扫描所有模组 jar 里带 {@link LittleMaidExtension} 注解的类并实例化
 * （见 TLM 的 {@code AnnotatedInstanceUtil#getModExtensions}），这里把
 * {@link EternalCakeEdible} 注册进「女仆可食用方块」列表。
 *
 * <p>注意：本类只在 TLM 存在时才会被它加载；没装 TLM 时这个类不会被任何东西触碰，
 * 因此本模组对 TLM 是纯软依赖（{@code compileOnly}）。
 */
@LittleMaidExtension
public class MaidCakeCompat implements ILittleMaid {

    @Override
    public void registerMaidEdibleBlock(MaidEdibleBlockManager manager) {
        manager.add(new EternalCakeEdible());
    }
}
