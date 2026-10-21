package cn.autoforged.eternal_food_mod_1789477527.recipe;

import cn.autoforged.eternal_food_mod_1789477527.EternalFoodMod;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** 本模组的自定义配方序列化器。 */
public class ModRecipes {

    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
            DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, EternalFoodMod.MOD_ID);

    /**
     * 与 {@code crafting_shapeless} 行为一致，但会把龙息换成玻璃瓶。
     * 原版 shapeless 只按 {@code Item#getCraftingRemainingItem} 返还容器，龙息没有该属性，
     * 所以需要自定义序列化器。
     */
    public static final RegistryObject<RecipeSerializer<?>> MILK_PLUS_SERIALIZER =
            RECIPE_SERIALIZERS.register("crafting_shapeless_with_bottle", EternalMilkPlusRecipeSerializer::new);

    /**
     * 与 {@code crafting_shapeless} 行为一致，但会把所用迷之炖菜的 {@code Effects} NBT
     * 复制到结果上，让永恒迷之炖菜继承原版 buff。
     */
    public static final RegistryObject<RecipeSerializer<?>> STEW_SERIALIZER =
            RECIPE_SERIALIZERS.register("crafting_shapeless_with_stew_effects",
                    EternalSuspiciousStewRecipeSerializer::new);

    /**
     * 与 {@code crafting_shapeless} 行为一致，但会把原料药水（原版药箭 / 永恒滞留型药水）的
     * 药水 NBT 复制到结果上，让永恒药箭继承对应的药水效果与颜色。
     */
    public static final RegistryObject<RecipeSerializer<?>> TIPPED_ARROW_SERIALIZER =
            RECIPE_SERIALIZERS.register("crafting_shapeless_with_tipped_arrow_effects",
                    EternalTippedArrowRecipeSerializer::new);

    /**
     * 与 {@code crafting_shapeless} 行为一致，但会把所用成品烟花火箭的 {@code Fireworks} NBT
     * 复制到结果上，让永恒烟花火箭继承合成前的所有烟花效果。
     */
    public static final RegistryObject<RecipeSerializer<?>> FIREWORK_ROCKET_SERIALIZER =
            RECIPE_SERIALIZERS.register("crafting_shapeless_with_firework_effects",
                    EternalFireworkRocketRecipeSerializer::new);

    /**
     * 与 {@code crafting_shapeless} 行为一致，但会把所用原版刷怪蛋对应的生物类型写入结果的
     * {@code EntityTag.id}，让永恒刷怪蛋继承该刷怪蛋的类型（并由此继承其配色）。
     */
    public static final RegistryObject<RecipeSerializer<?>> SPAWN_EGG_SERIALIZER =
            RECIPE_SERIALIZERS.register("crafting_shapeless_with_spawn_egg_type",
                    EternalSpawnEggRecipeSerializer::new);

    /**
     * 与 {@code crafting_shapeless} 行为一致（4 组石头 + 4 组深板岩 + 下界之星），但额外要求
     * 石头 / 深板岩各为 256 个（即 4 个满堆）；取出成品时由事件清空整堆材料实现 256+256 的消耗。
     */
    public static final RegistryObject<RecipeSerializer<?>> FULL_DOMAIN_SERIALIZER =
            RECIPE_SERIALIZERS.register("crafting_shapeless_full_domain",
                    EternalFullDomainRecipeSerializer::new);

    public static void register(IEventBus modEventBus) {
        RECIPE_SERIALIZERS.register(modEventBus);
    }
}
