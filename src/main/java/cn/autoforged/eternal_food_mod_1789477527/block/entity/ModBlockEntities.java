package cn.autoforged.eternal_food_mod_1789477527.block.entity;

import cn.autoforged.eternal_food_mod_1789477527.EternalFoodMod;
import cn.autoforged.eternal_food_mod_1789477527.block.ModBlocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** 方块实体类型注册表。 */
public class ModBlockEntities {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, EternalFoodMod.MOD_ID);

    public static final RegistryObject<BlockEntityType<EternalClockBlockEntity>> ETERNAL_CLOCK =
            BLOCK_ENTITIES.register("eternal_clock",
                    () -> BlockEntityType.Builder
                            .of(EternalClockBlockEntity::new, ModBlocks.ETERNAL_CLOCK.get())
                            .build(null));

    public static void register(IEventBus modEventBus) {
        BLOCK_ENTITIES.register(modEventBus);
    }
}
