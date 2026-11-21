package cn.autoforged.eternal_food_mod_1789477527.menu;

import cn.autoforged.eternal_food_mod_1789477527.EternalFoodMod;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** 本模组的界面类型。 */
public class ModMenus {

    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(ForgeRegistries.MENU_TYPES, EternalFoodMod.MOD_ID);

    public static final RegistryObject<MenuType<EternalDispenserGunMenu>> GUN =
            MENUS.register("eternal_dispenser_gun", () -> IForgeMenuType.create(
                    (id, inventory, buffer) -> new EternalDispenserGunMenu(id, inventory,
                            buffer.readEnum(InteractionHand.class))));

    public static void register(IEventBus modEventBus) {
        MENUS.register(modEventBus);
    }
}
