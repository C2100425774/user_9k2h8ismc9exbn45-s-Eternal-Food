package cn.autoforged.eternal_food_mod_1789477527.recipe;

import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapelessRecipe;

/**
 * 永恒烟花火箭 的合成配方：行为与 {@code crafting_shapeless} 相同（成品烟花火箭 + 下界之星），
 * 但会把所用成品烟花火箭的 NBT（{@code Fireworks}：飞行时间 {@code Flight} 与爆炸
 * {@code Explosions}）完整复制到结果上，让永恒烟花火箭继承合成前的所有烟花效果。
 */
public class EternalFireworkRocketRecipe extends ShapelessRecipe {

    public EternalFireworkRocketRecipe(ResourceLocation id, String group, CraftingBookCategory category,
            ItemStack result, NonNullList<Ingredient> ingredients) {
        super(id, group, category, result, ingredients);
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.FIREWORK_ROCKET_SERIALIZER.get();
    }

    @Override
    public ItemStack assemble(CraftingContainer container, RegistryAccess registryAccess) {
        ItemStack result = super.assemble(container, registryAccess);
        ItemStack source = findFireworkSource(container);
        if (source != null && source.getTag() != null) {
            result.setTag(source.getTag().copy());
        }
        return result;
    }

    /** 找出配方中的烟花来源：成品烟花火箭。 */
    private static ItemStack findFireworkSource(CraftingContainer container) {
        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack ingredient = container.getItem(i);
            if (ingredient.is(Items.FIREWORK_ROCKET)) {
                return ingredient;
            }
        }
        return null;
    }
}
