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
import net.minecraft.world.level.Level;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

/**
 * 永恒烟花火箭导弹配方：永恒烟花火箭 + TNT。
 * 只继承来源烟花的第一个烟火之星（颜色/形状/踪迹/闪烁）和 Flight 飞行时长。
 */
public class EternalFireworkMissileRecipe extends ShapelessRecipe {

    public EternalFireworkMissileRecipe(ResourceLocation id, String group, CraftingBookCategory category,
            ItemStack result, NonNullList<Ingredient> ingredients) {
        super(id, group, category, result, ingredients);
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.FIREWORK_MISSILE_SERIALIZER.get();
    }

    @Override
    public boolean matches(CraftingContainer container, Level level) {
        if (!super.matches(container, level)) {
            return false;
        }
        ItemStack source = findFireworkSource(container);
        if (source == null || source.getTag() == null) {
            return false;
        }
        CompoundTag fireworks = source.getTagElement("Fireworks");
        return fireworks != null && !fireworks.getList("Explosions", Tag.TAG_COMPOUND).isEmpty();
    }

    @Override
    public ItemStack assemble(CraftingContainer container, RegistryAccess registryAccess) {
        ItemStack result = super.assemble(container, registryAccess);
        ItemStack source = findFireworkSource(container);
        if (source == null || source.getTag() == null) {
            return result;
        }
        CompoundTag sourceFireworks = source.getTagElement("Fireworks");
        if (sourceFireworks == null) {
            return result;
        }
        CompoundTag fireworks = new CompoundTag();
        fireworks.putByte("Flight", sourceFireworks.getByte("Flight"));
        ListTag explosions = sourceFireworks.getList("Explosions", Tag.TAG_COMPOUND);
        // 只继承第一颗星，复制 8 份：原版伤害 = 5 + 2×8 = 21
        CompoundTag star = explosions.getCompound(0).copy();
        ListTag eight = new ListTag();
        for (int i = 0; i < 8; i++) {
            eight.add(star.copy());
        }
        fireworks.put("Explosions", eight);
        CompoundTag tag = result.getOrCreateTag();
        tag.put("Fireworks", fireworks);
        tag.putBoolean("EternalMissile", true);
        return result;
    }

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
