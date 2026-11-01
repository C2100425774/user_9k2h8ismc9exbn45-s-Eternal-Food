package cn.autoforged.eternal_food_mod_1789477527.block;

import cn.autoforged.eternal_food_mod_1789477527.EternalFoodMod;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * 方块注册表。
 *
 * <p>目前只有一个方块：永恒蛋糕。它的注册名与原来的物品名保持一致（{@code eternal_cake_slice}），
 * 这样旧存档里已有的「永恒蛋糕」物品升级后直接变成对应方块的物品，不会丢失。
 */
public class ModBlocks {

    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, EternalFoodMod.MOD_ID);

    public static final RegistryObject<Block> ETERNAL_CAKE =
            BLOCKS.register("eternal_cake_slice", EternalCakeBlock::new);

    /** 永恒数字钟：正面用 7 段数码管实时显示游戏时间（HH:MM）。 */
    public static final RegistryObject<Block> ETERNAL_CLOCK =
            BLOCKS.register("eternal_clock", EternalClockBlock::new);

    public static void register(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
    }
}
