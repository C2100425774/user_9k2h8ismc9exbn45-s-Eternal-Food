package cn.autoforged.eternal_food_mod_1789477527.entity;

import cn.autoforged.eternal_food_mod_1789477527.EternalFoodMod;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** 本模组的自定义实体类型。 */
public class ModEntities {

    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, EternalFoodMod.MOD_ID);

    /** 永恒喷溅型奶瓶投掷物。 */
    public static final RegistryObject<EntityType<ThrownEternalMilk>> THROWN_ETERNAL_MILK =
            ENTITY_TYPES.register("thrown_eternal_milk", () -> EntityType.Builder
                    .<ThrownEternalMilk>of(ThrownEternalMilk::new, MobCategory.MISC)
                    .sized(0.25F, 0.25F)
                    .clientTrackingRange(4)
                    .updateInterval(10)
                    .build("thrown_eternal_milk"));

    /** 永恒喷溅型 / 滞留型药水投掷物。 */
    public static final RegistryObject<EntityType<ThrownEternalPotion>> THROWN_ETERNAL_POTION =
            ENTITY_TYPES.register("thrown_eternal_potion", () -> EntityType.Builder
                    .<ThrownEternalPotion>of(ThrownEternalPotion::new, MobCategory.MISC)
                    .sized(0.25F, 0.25F)
                    .clientTrackingRange(4)
                    .updateInterval(10)
                    .build("thrown_eternal_potion"));

    public static void register(IEventBus modEventBus) {
        ENTITY_TYPES.register(modEventBus);
    }
}
