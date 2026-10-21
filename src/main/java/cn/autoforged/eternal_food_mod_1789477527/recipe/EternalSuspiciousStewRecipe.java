package cn.autoforged.eternal_food_mod_1789477527.recipe;

import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SuspiciousStewItem;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapelessRecipe;

/**
 * 永恒迷之炖菜 的合成配方：行为与 {@code crafting_shapeless} 相同，
 * 但会把所使用迷之炖菜的 {@code Effects} NBT（合成普通迷之炖菜时由花朵决定）
 * 复制到结果上，从而让永恒迷之炖菜继承原版会赋予玩家的 buff。
 */
public class EternalSuspiciousStewRecipe extends ShapelessRecipe {

    public EternalSuspiciousStewRecipe(ResourceLocation id, String group, CraftingBookCategory category,
            ItemStack result, NonNullList<Ingredient> ingredients) {
        super(id, group, category, result, ingredients);
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.STEW_SERIALIZER.get();
    }

    @Override
    public ItemStack assemble(CraftingContainer container, RegistryAccess registryAccess) {
        ItemStack result = super.assemble(container, registryAccess);
        ListTag effects = findEffects(container);
        if (effects != null) {
            result.getOrCreateTag().put(SuspiciousStewItem.EFFECTS_TAG, effects);
        }
        return result;
    }

    private static ListTag findEffects(CraftingContainer container) {
        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack ingredient = container.getItem(i);
            if (ingredient.is(Items.SUSPICIOUS_STEW)) {
                CompoundTag tag = ingredient.getTag();
                if (tag != null && tag.contains(SuspiciousStewItem.EFFECTS_TAG, 9)) {
                    return tag.getList(SuspiciousStewItem.EFFECTS_TAG, 10).copy();
                }
            }
        }
        return null;
    }
}
