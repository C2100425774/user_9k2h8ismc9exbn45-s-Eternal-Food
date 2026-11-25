package cn.autoforged.eternal_food_mod_1789477527.recipe;

import cn.autoforged.eternal_food_mod_1789477527.item.EternalLighterItem;
import cn.autoforged.eternal_food_mod_1789477527.item.ModItems;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;

/** 两个同种燃料的打火机合成：修复满耐久，并合并流体（最多 200mB）。 */
public class LighterCombineRecipe extends ShapelessRecipe {

    public LighterCombineRecipe(ResourceLocation id, String group, CraftingBookCategory category,
            ItemStack result, NonNullList<Ingredient> ingredients) {
        super(id, group, category, result, ingredients);
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.LIGHTER_COMBINE_SERIALIZER.get();
    }

    @Override
    public boolean matches(CraftingContainer container, Level level) {
        if (!super.matches(container, level)) {
            return false;
        }
        ItemStack first = null;
        int lighters = 0;
        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack stack = container.getItem(i);
            if (stack.is(ModItems.ETERNAL_LIGHTER.get())) {
                lighters++;
                if (first == null) {
                    first = stack;
                }
            }
        }
        if (lighters != 2 || first == null) {
            return false;
        }
        FluidStack firstFluid = EternalLighterItem.getFluid(first);
        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack stack = container.getItem(i);
            if (stack == first || !stack.is(ModItems.ETERNAL_LIGHTER.get())) {
                continue;
            }
            FluidStack other = EternalLighterItem.getFluid(stack);
            if (firstFluid.isEmpty() != other.isEmpty()) {
                return false;
            }
            if (!firstFluid.isEmpty() && !firstFluid.isFluidEqual(other)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack assemble(CraftingContainer container, RegistryAccess registryAccess) {
        ItemStack output = super.assemble(container, registryAccess);
        FluidStack total = FluidStack.EMPTY;
        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack stack = container.getItem(i);
            if (!stack.is(ModItems.ETERNAL_LIGHTER.get())) {
                continue;
            }
            FluidStack fluid = EternalLighterItem.getFluid(stack);
            if (!fluid.isEmpty()) {
                if (total.isEmpty()) {
                    total = fluid.copy();
                } else {
                    total.setAmount(Math.min(EternalLighterItem.FLUID_CAPACITY,
                            total.getAmount() + fluid.getAmount()));
                }
            }
        }
        output.setDamageValue(0);
        FluidStack finalTotal = total;
        output.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).ifPresent(handler -> {
            handler.drain(EternalLighterItem.FLUID_CAPACITY, IFluidHandler.FluidAction.EXECUTE);
            if (!finalTotal.isEmpty()) {
                handler.fill(finalTotal, IFluidHandler.FluidAction.EXECUTE);
            }
        });
        return output;
    }
}
