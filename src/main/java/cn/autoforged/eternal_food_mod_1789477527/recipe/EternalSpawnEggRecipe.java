package cn.autoforged.eternal_food_mod_1789477527.recipe;

import cn.autoforged.eternal_food_mod_1789477527.item.EternalSpawnEggItem;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapelessRecipe;

/**
 * 永恒刷怪蛋 的合成配方：行为与 {@code crafting_shapeless} 相同（任意原版刷怪蛋 + 下界之星），
 * 但会把所用刷怪蛋对应的生物类型写入结果的 {@code EntityTag.id}，让永恒刷怪蛋继承该刷怪蛋的类型。
 *
 * <p>颜色覆盖层由 {@link EternalSpawnEggItem#getLayerColor} 在客户端按栈内生物类型从对应原版
 * 刷怪蛋取色，因此合成出的永恒刷怪蛋会自动继承对应刷怪蛋的配色。
 */
public class EternalSpawnEggRecipe extends ShapelessRecipe {

    public EternalSpawnEggRecipe(ResourceLocation id, String group, CraftingBookCategory category,
            ItemStack result, NonNullList<Ingredient> ingredients) {
        super(id, group, category, result, ingredients);
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.SPAWN_EGG_SERIALIZER.get();
    }

    @Override
    public ItemStack assemble(CraftingContainer container, RegistryAccess registryAccess) {
        ItemStack result = super.assemble(container, registryAccess);
        EntityType<?> type = findSpawnEggType(container);
        if (type != null) {
            EternalSpawnEggItem.setEntityType(result, type);
        }
        return result;
    }

    /** 找出配方中使用的原版刷怪蛋，并返回其对应的生物类型。 */
    private static EntityType<?> findSpawnEggType(CraftingContainer container) {
        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack ingredient = container.getItem(i);
            if (ingredient.getItem() instanceof SpawnEggItem egg
                    && !(egg instanceof EternalSpawnEggItem)) {
                return egg.getType(ingredient.getTag());
            }
        }
        return null;
    }
}
