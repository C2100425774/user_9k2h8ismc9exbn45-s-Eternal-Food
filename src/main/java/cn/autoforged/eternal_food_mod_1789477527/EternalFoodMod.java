package cn.autoforged.eternal_food_mod_1789477527;

import cn.autoforged.eternal_food_mod_1789477527.block.ModBlocks;
import cn.autoforged.eternal_food_mod_1789477527.block.entity.ModBlockEntities;
import cn.autoforged.eternal_food_mod_1789477527.effect.ModEffects;
import cn.autoforged.eternal_food_mod_1789477527.entity.ModEntities;
import cn.autoforged.eternal_food_mod_1789477527.event.ModCreativeTabs;
import cn.autoforged.eternal_food_mod_1789477527.item.ModItems;
import cn.autoforged.eternal_food_mod_1789477527.network.ModNetwork;
import cn.autoforged.eternal_food_mod_1789477527.recipe.ModRecipes;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(EternalFoodMod.MOD_ID)
public class EternalFoodMod {
    public static final String MOD_ID = "eternal_food_mod_1789477527";

    public EternalFoodMod() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        // 方块必须先于物品注册：永恒蛋糕的物品是 BlockItem，取 Block 时方块得已经在注册表里。
        ModBlocks.register(modEventBus);
        ModBlockEntities.register(modEventBus);
        ModItems.register(modEventBus);
        ModEffects.register(modEventBus);
        ModEntities.register(modEventBus);
        ModCreativeTabs.register(modEventBus);
        ModRecipes.register(modEventBus);
        ModNetwork.register();
    }
}
