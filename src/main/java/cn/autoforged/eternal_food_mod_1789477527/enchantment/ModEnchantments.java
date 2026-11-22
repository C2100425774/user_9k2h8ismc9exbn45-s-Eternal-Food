package cn.autoforged.eternal_food_mod_1789477527.enchantment;

import cn.autoforged.eternal_food_mod_1789477527.EternalFoodMod;
import cn.autoforged.eternal_food_mod_1789477527.item.EternalFoodItem;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** 五个永恒食物附魔。数值参考 orefood 的同类附魔。 */
public final class ModEnchantments {

    private static final Predicate<Item> ETERNAL_FOOD = item -> item instanceof EternalFoodItem;

    public static final EnchantmentCategory ETERNAL_FOOD_CATEGORY =
            EnchantmentCategory.create("eternal_food", ETERNAL_FOOD);

    public static final DeferredRegister<Enchantment> ENCHANTMENTS =
            DeferredRegister.create(ForgeRegistries.ENCHANTMENTS, EternalFoodMod.MOD_ID);

    public static final RegistryObject<Enchantment> POWERFUL_RECOVERY =
            ENCHANTMENTS.register("powerful_recovery",
                    () -> new EternalFoodEnchantment(Enchantment.Rarity.COMMON, ETERNAL_FOOD_CATEGORY, 4));

    public static final RegistryObject<Enchantment> ADRENALINE =
            ENCHANTMENTS.register("adrenaline",
                    () -> new EternalFoodEnchantment(Enchantment.Rarity.COMMON, ETERNAL_FOOD_CATEGORY, 3));

    public static final RegistryObject<Enchantment> FITNESS =
            ENCHANTMENTS.register("fitness",
                    () -> new EternalFoodEnchantment(Enchantment.Rarity.COMMON, ETERNAL_FOOD_CATEGORY, 5));

    public static final RegistryObject<Enchantment> SATIETY =
            ENCHANTMENTS.register("satiety",
                    () -> new EternalFoodEnchantment(Enchantment.Rarity.COMMON, ETERNAL_FOOD_CATEGORY, 3));

    public static final RegistryObject<Enchantment> FEAST =
            ENCHANTMENTS.register("feast",
                    () -> new EternalFoodEnchantment(Enchantment.Rarity.COMMON, ETERNAL_FOOD_CATEGORY, 3));

    public static final List<RegistryObject<Enchantment>> ALL = List.of(
            POWERFUL_RECOVERY, ADRENALINE, FITNESS, SATIETY, FEAST);

    private ModEnchantments() {
    }

    public static void register(IEventBus modEventBus) {
        ENCHANTMENTS.register(modEventBus);
    }
}
