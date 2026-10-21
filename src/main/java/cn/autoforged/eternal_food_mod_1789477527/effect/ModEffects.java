package cn.autoforged.eternal_food_mod_1789477527.effect;

import cn.autoforged.eternal_food_mod_1789477527.EternalFoodMod;
import net.minecraft.world.effect.MobEffect;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** 本模组的状态效果注册表。 */
public class ModEffects {

    public static final DeferredRegister<MobEffect> EFFECTS =
            DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, EternalFoodMod.MOD_ID);

    /** 「饕餮」：持续期间满饥饿也能正常进食。图标为 textures/mob_effect/gulosity.png。 */
    public static final RegistryObject<MobEffect> GULOSITY =
            EFFECTS.register("gulosity", GulosityEffect::new);

    public static void register(IEventBus modEventBus) {
        EFFECTS.register(modEventBus);
    }
}
