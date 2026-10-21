package cn.autoforged.eternal_food_mod_1789477527.recipe;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.core.NonNullList;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;

/**
 * 反序列化 {@link EternalFullDomainRecipe}，JSON / 网络格式与原版 shapeless 完全一致。
 *
 * <p>配方里的石头 / 深板岩按「一整堆（64）」计：原版 {@link Ingredient} 的 JSON 表达不了数量，
 * 所以这里在加载时把这两类原料重建为 count = {@value #FULL_STACK} 的 {@link ItemStack}，
 * 这样 JEI / 配方书里会正确显示每格 64 个，而不是看起来只要 4 个就能合。
 * 判定是否凑齐仍然由 {@link EternalFullDomainRecipe#matches} 按总数校验（4 × 64 = 256）。
 */
public class EternalFullDomainRecipeSerializer implements RecipeSerializer<EternalFullDomainRecipe> {

    /** 每格石头 / 深板岩的数量：一整堆。 */
    public static final int FULL_STACK = 64;

    @Override
    public EternalFullDomainRecipe fromJson(ResourceLocation recipeId, JsonObject json) {
        String group = GsonHelper.getAsString(json, "group", "");
        CraftingBookCategory category = CraftingBookCategory.CODEC
                .byName(GsonHelper.getAsString(json, "category", null), CraftingBookCategory.MISC);

        JsonArray array = GsonHelper.getAsJsonArray(json, "ingredients");
        NonNullList<Ingredient> ingredients = NonNullList.create();
        for (int i = 0; i < array.size(); i++) {
            ingredients.add(withFullStackCount(Ingredient.fromJson(array.get(i), false)));
        }
        if (ingredients.isEmpty()) {
            throw new com.google.gson.JsonParseException("No ingredients for eternal full domain recipe");
        }

        ItemStack result = ShapedRecipe.itemStackFromJson(GsonHelper.getAsJsonObject(json, "result"));
        return new EternalFullDomainRecipe(recipeId, group, category, result, ingredients);
    }

    @Override
    public EternalFullDomainRecipe fromNetwork(ResourceLocation recipeId, FriendlyByteBuf buffer) {
        String group = buffer.readUtf();
        CraftingBookCategory category = buffer.readEnum(CraftingBookCategory.class);
        int size = buffer.readVarInt();
        NonNullList<Ingredient> ingredients = NonNullList.withSize(size, Ingredient.EMPTY);
        for (int i = 0; i < size; i++) {
            ingredients.set(i, withFullStackCount(Ingredient.fromNetwork(buffer)));
        }
        ItemStack result = buffer.readItem();
        return new EternalFullDomainRecipe(recipeId, group, category, result, ingredients);
    }

    @Override
    public void toNetwork(FriendlyByteBuf buffer, EternalFullDomainRecipe recipe) {
        buffer.writeUtf(recipe.getGroup());
        buffer.writeEnum(recipe.category());
        NonNullList<Ingredient> ingredients = recipe.getIngredients();
        buffer.writeVarInt(ingredients.size());
        for (Ingredient ingredient : ingredients) {
            ingredient.toNetwork(buffer);
        }
        buffer.writeItem(recipe.getResultItem(null));
    }

    /**
     * 把石头 / 深板岩原料重建为「一整堆」。
     *
     * <p>为什么不在 JSON 里写 {@code "count": 64}：1.20.1 原版 {@code Ingredient} 的 JSON
     * 反序列化只认 {@code item} / {@code tag}，多写的 count 会被静默忽略，看起来改了其实没改。
     * 因此数量必须在加载阶段补上。
     */
    private static Ingredient withFullStackCount(Ingredient original) {
        ItemStack[] stacks = original.getItems();
        if (stacks.length != 1) {
            return original;
        }
        ItemStack only = stacks[0];
        if (only.is(Items.STONE)) {
            return Ingredient.of(new ItemStack(Items.STONE, FULL_STACK));
        }
        if (only.is(Items.DEEPSLATE)) {
            return Ingredient.of(new ItemStack(Items.DEEPSLATE, FULL_STACK));
        }
        return original;
    }
}
