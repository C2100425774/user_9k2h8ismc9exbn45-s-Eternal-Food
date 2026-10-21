package cn.autoforged.eternal_food_mod_1789477527.recipe;

import cn.autoforged.eternal_food_mod_1789477527.EternalFoodMod;
import cn.autoforged.eternal_food_mod_1789477527.item.ModItems;
import java.util.Set;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraftforge.common.brewing.BrewingRecipeRegistry;
import net.minecraftforge.common.brewing.IBrewingRecipe;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

/**
 * 永恒喷溅型奶瓶的酿造配方（酿造台：原料在顶部，药水在下方三个槽位）。
 *
 * <ul>
 *   <li>喷溅型水瓶 / 喷溅型平凡 / 浓稠 / 粗制的药水 + 永恒奶桶 → 永恒喷溅型奶瓶</li>
 *   <li>喷溅型水瓶 / 喷溅型平凡 / 浓稠 / 粗制的药水 + 永恒奶桶plus → 永恒喷溅型奶瓶plus</li>
 *   <li>永恒喷溅型奶瓶 + 龙息 → 永恒喷溅型奶瓶plus（无滞留型变种）</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = EternalFoodMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModBrewingRecipes {

    /** 可用来承载永恒奶的底料：喷溅型水瓶 / 平凡 / 浓稠 / 粗制的药水。 */
    private static final Set<Potion> BASE_POTIONS =
            Set.of(Potions.WATER, Potions.MUNDANE, Potions.THICK, Potions.AWKWARD);

    @SubscribeEvent
    public static void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> BrewingRecipeRegistry.addRecipe(new IBrewingRecipe() {
            @Override
            public boolean isInput(ItemStack input) {
                if (input.is(ModItems.ETERNAL_SPLASH_MILK_BOTTLE.get())) {
                    return true;
                }
                return input.is(Items.SPLASH_POTION) && BASE_POTIONS.contains(PotionUtils.getPotion(input));
            }

            @Override
            public boolean isIngredient(ItemStack ingredient) {
                return ingredient.is(ModItems.ETERNAL_MILK_BUCKET.get())
                        || ingredient.is(ModItems.ETERNAL_MILK_PLUS.get())
                        || ingredient.is(Items.DRAGON_BREATH);
            }

            @Override
            public ItemStack getOutput(ItemStack input, ItemStack ingredient) {
                if (input.is(ModItems.ETERNAL_SPLASH_MILK_BOTTLE.get()) && ingredient.is(Items.DRAGON_BREATH)) {
                    return new ItemStack(ModItems.ETERNAL_SPLASH_MILK_BOTTLE_PLUS.get());
                }
                if (input.is(Items.SPLASH_POTION) && BASE_POTIONS.contains(PotionUtils.getPotion(input))) {
                    if (ingredient.is(ModItems.ETERNAL_MILK_BUCKET.get())) {
                        return new ItemStack(ModItems.ETERNAL_SPLASH_MILK_BOTTLE.get());
                    }
                    if (ingredient.is(ModItems.ETERNAL_MILK_PLUS.get())) {
                        return new ItemStack(ModItems.ETERNAL_SPLASH_MILK_BOTTLE_PLUS.get());
                    }
                }
                return ItemStack.EMPTY;
            }
        }));

        // 永恒药水的酿造：
        //   普通/喷溅/滞留型原版药水 + 下界之星 → 对应的永恒药水
        //   永恒药水 + 火药 → 永恒喷溅型药水；永恒喷溅型药水 + 龙息 → 永恒滞留型药水
        // 输出复制输入的全部 NBT，保留药水类型 / 自定义效果 / 自定义颜色。
        event.enqueueWork(() -> BrewingRecipeRegistry.addRecipe(new IBrewingRecipe() {
            @Override
            public boolean isInput(ItemStack input) {
                return input.is(Items.POTION)
                        || input.is(Items.SPLASH_POTION)
                        || input.is(Items.LINGERING_POTION)
                        || input.is(ModItems.ETERNAL_POTION.get())
                        || input.is(ModItems.ETERNAL_SPLASH_POTION.get());
            }

            @Override
            public boolean isIngredient(ItemStack ingredient) {
                return ingredient.is(Items.NETHER_STAR)
                        || ingredient.is(Items.GUNPOWDER)
                        || ingredient.is(Items.DRAGON_BREATH);
            }

            @Override
            public ItemStack getOutput(ItemStack input, ItemStack ingredient) {
                if (ingredient.is(Items.NETHER_STAR)) {
                    if (input.is(Items.POTION)) {
                        return withSamePotion(input, ModItems.ETERNAL_POTION.get());
                    }
                    if (input.is(Items.SPLASH_POTION)) {
                        return withSamePotion(input, ModItems.ETERNAL_SPLASH_POTION.get());
                    }
                    if (input.is(Items.LINGERING_POTION)) {
                        return withSamePotion(input, ModItems.ETERNAL_LINGERING_POTION.get());
                    }
                }
                if (input.is(ModItems.ETERNAL_POTION.get()) && ingredient.is(Items.GUNPOWDER)) {
                    return withSamePotion(input, ModItems.ETERNAL_SPLASH_POTION.get());
                }
                if (input.is(ModItems.ETERNAL_SPLASH_POTION.get()) && ingredient.is(Items.DRAGON_BREATH)) {
                    return withSamePotion(input, ModItems.ETERNAL_LINGERING_POTION.get());
                }
                return ItemStack.EMPTY;
            }
        }));
    }

    /** 生成 {@code outputItem}，并把输入药水的全部 NBT（药水类型 / 自定义效果 / 颜色）复制过去。 */
    private static ItemStack withSamePotion(ItemStack input, net.minecraft.world.item.Item outputItem) {
        ItemStack output = new ItemStack(outputItem);
        if (input.getTag() != null) {
            output.setTag(input.getTag().copy());
        }
        return output;
    }
}
