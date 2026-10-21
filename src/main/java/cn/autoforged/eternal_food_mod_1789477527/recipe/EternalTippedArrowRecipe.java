package cn.autoforged.eternal_food_mod_1789477527.recipe;

import cn.autoforged.eternal_food_mod_1789477527.item.ModItems;
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
 * 永恒药箭 的合成配方：行为与 {@code crafting_shapeless} 相同，
 * 但会把原料药水（原版药箭 / 永恒滞留型药水）的 {@code Potion} / {@code CustomPotionEffects}
 * 等 NBT 复制到结果上，让永恒药箭继承对应的药水效果与颜色。
 */
public class EternalTippedArrowRecipe extends ShapelessRecipe {

    public EternalTippedArrowRecipe(ResourceLocation id, String group, CraftingBookCategory category,
            ItemStack result, NonNullList<Ingredient> ingredients) {
        super(id, group, category, result, ingredients);
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.TIPPED_ARROW_SERIALIZER.get();
    }

    @Override
    public ItemStack assemble(CraftingContainer container, RegistryAccess registryAccess) {
        ItemStack result = super.assemble(container, registryAccess);
        ItemStack source = findPotionSource(container);
        if (source != null && source.getTag() != null) {
            result.setTag(source.getTag().copy());
        }
        return result;
    }

    /** 找出配方中的药水来源：原版药箭或永恒滞留型药水。 */
    private static ItemStack findPotionSource(CraftingContainer container) {
        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack ingredient = container.getItem(i);
            if (ingredient.is(Items.TIPPED_ARROW)
                    || ingredient.is(ModItems.ETERNAL_LINGERING_POTION.get())) {
                return ingredient;
            }
        }
        return null;
    }
}
