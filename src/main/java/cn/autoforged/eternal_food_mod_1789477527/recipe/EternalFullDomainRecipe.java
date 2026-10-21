package cn.autoforged.eternal_food_mod_1789477527.recipe;

import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.level.Level;

/**
 * 永恒满置域 的合成配方：无序合成，原料为 4 组（4×64 = 256 个）石头、4 组（256 个）深板岩与 1 个下界之星。
 *
 * <p>原版 {@link ShapelessRecipe} 只按「每种原料占一格」匹配、忽略每格数量，因此这里额外要求
 * 这两类原料的总数分别为 {@value #STONE_REQUIRED} 与 {@value #DEEPSLATE_REQUIRED}（即 4 个满堆）。
 * 由于原版合成台单格最多 64 个，总数达标必然对应 4 个各 64 的满堆。
 * 整堆的消耗由 {@code EternalFullDomainCraftHandler}（FORGE bus 的 {@code ItemCraftedEvent}）
 * 在取出成品时清空这些材料格实现。
 */
public class EternalFullDomainRecipe extends ShapelessRecipe {

    /** 石头所需总数：4 组 × 64 = 256。 */
    public static final int STONE_REQUIRED = 256;
    /** 深板岩所需总数：4 组 × 64 = 256。 */
    public static final int DEEPSLATE_REQUIRED = 256;

    public EternalFullDomainRecipe(ResourceLocation id, String group, CraftingBookCategory category,
            ItemStack result, NonNullList<Ingredient> ingredients) {
        super(id, group, category, result, ingredients);
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.FULL_DOMAIN_SERIALIZER.get();
    }

    @Override
    public boolean matches(CraftingContainer container, Level level) {
        if (!super.matches(container, level)) {
            return false;
        }
        int stone = 0;
        int deepslate = 0;
        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack stack = container.getItem(i);
            if (stack.is(Items.STONE)) {
                stone += stack.getCount();
            } else if (stack.is(Items.DEEPSLATE)) {
                deepslate += stack.getCount();
            }
        }
        return stone == STONE_REQUIRED && deepslate == DEEPSLATE_REQUIRED;
    }
}
