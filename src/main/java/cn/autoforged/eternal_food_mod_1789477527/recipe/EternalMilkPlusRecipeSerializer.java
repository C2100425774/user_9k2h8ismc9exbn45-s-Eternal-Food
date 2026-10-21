package cn.autoforged.eternal_food_mod_1789477527.recipe;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.core.NonNullList;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;

/** 反序列化 {@link EternalMilkPlusRecipe}，JSON / 网络格式与原版 shapeless 完全一致。 */
public class EternalMilkPlusRecipeSerializer implements RecipeSerializer<EternalMilkPlusRecipe> {

    @Override
    public EternalMilkPlusRecipe fromJson(ResourceLocation recipeId, JsonObject json) {
        String group = GsonHelper.getAsString(json, "group", "");
        CraftingBookCategory category = CraftingBookCategory.CODEC
                .byName(GsonHelper.getAsString(json, "category", null), CraftingBookCategory.MISC);

        JsonArray array = GsonHelper.getAsJsonArray(json, "ingredients");
        NonNullList<Ingredient> ingredients = NonNullList.create();
        for (int i = 0; i < array.size(); i++) {
            ingredients.add(Ingredient.fromJson(array.get(i), false));
        }
        if (ingredients.isEmpty()) {
            throw new com.google.gson.JsonParseException("No ingredients for eternal milk plus recipe");
        }

        ItemStack result = ShapedRecipe.itemStackFromJson(GsonHelper.getAsJsonObject(json, "result"));
        return new EternalMilkPlusRecipe(recipeId, group, category, result, ingredients);
    }

    @Override
    public EternalMilkPlusRecipe fromNetwork(ResourceLocation recipeId, FriendlyByteBuf buffer) {
        String group = buffer.readUtf();
        CraftingBookCategory category = buffer.readEnum(CraftingBookCategory.class);
        int size = buffer.readVarInt();
        NonNullList<Ingredient> ingredients = NonNullList.withSize(size, Ingredient.EMPTY);
        for (int i = 0; i < size; i++) {
            ingredients.set(i, Ingredient.fromNetwork(buffer));
        }
        ItemStack result = buffer.readItem();
        return new EternalMilkPlusRecipe(recipeId, group, category, result, ingredients);
    }

    @Override
    public void toNetwork(FriendlyByteBuf buffer, EternalMilkPlusRecipe recipe) {
        buffer.writeUtf(recipe.getGroup());
        buffer.writeEnum(recipe.category());
        NonNullList<Ingredient> ingredients = recipe.getIngredients();
        buffer.writeVarInt(ingredients.size());
        for (Ingredient ingredient : ingredients) {
            ingredient.toNetwork(buffer);
        }
        buffer.writeItem(recipe.getResultItem(null));
    }
}
