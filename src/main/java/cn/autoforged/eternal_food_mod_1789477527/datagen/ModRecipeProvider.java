package cn.autoforged.eternal_food_mod_1789477527.datagen;

import cn.autoforged.eternal_food_mod_1789477527.EternalFoodMod;
import cn.autoforged.eternal_food_mod_1789477527.item.EternalSpawnEggItem;
import cn.autoforged.eternal_food_mod_1789477527.item.ModItems;
import cn.autoforged.eternal_food_mod_1789477527.recipe.ModRecipes;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.data.recipes.SimpleCookingRecipeBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraftforge.registries.RegistryObject;

public class ModRecipeProvider extends RecipeProvider {

    public ModRecipeProvider(PackOutput output) {
        super(output);
    }

    @Override
    protected void buildRecipes(Consumer<FinishedRecipe> writer) {
        // 每个永恒食物 = 对应的原版食物 + 下界之星（无序合成，背包 2x2 与工作台均可）。
        for (Map.Entry<RegistryObject<Item>, Item> entry : ModItems.ETERNAL_TO_BASE.entrySet()) {
            Item result = entry.getKey().get();
            if (entry.getValue() == Items.SUSPICIOUS_STEW) {
                // 迷之炖菜使用自定义配方，把原版炖菜的效果 NBT 复制到结果上。
                saveSuspiciousStewRecipe(writer, result);
                continue;
            }
            if (entry.getValue() == Items.RED_MUSHROOM || entry.getValue() == Items.BROWN_MUSHROOM) {
                // 永恒红 / 永恒棕蘑菇额外需要一张床（任意颜色，标签 minecraft:beds）。
                ShapelessRecipeBuilder.shapeless(RecipeCategory.FOOD, result)
                        .requires(entry.getValue())
                        .requires(Items.NETHER_STAR)
                        .requires(ItemTags.BEDS)
                        .unlockedBy("has_nether_star", has(Items.NETHER_STAR))
                        .save(writer);
                continue;
            }
            ShapelessRecipeBuilder.shapeless(RecipeCategory.FOOD, result)
                    .requires(entry.getValue())
                    .requires(Items.NETHER_STAR)
                    .unlockedBy("has_nether_star", has(Items.NETHER_STAR))
                    .save(writer);
        }

        // 永恒之鱼：由 9 种永恒食物无序合成，没有原版基础食物，因此不进入 ETERNAL_TO_BASE。
        // 注：需求中的"永恒猪排"未写"生/熟"，此处按熟猪排处理（与牛排/熟羊肉/熟鳕鱼等熟食主题一致）。
        ShapelessRecipeBuilder.shapeless(RecipeCategory.FOOD, ModItems.ETERNAL_FISH.get())
                .requires(ModItems.ETERNAL_COOKED_BEEF.get())
                .requires(ModItems.ETERNAL_GOLDEN_CARROT.get())
                .requires(ModItems.ETERNAL_COOKED_PORKCHOP.get())
                .requires(ModItems.ETERNAL_CAKE_SLICE.get())
                .requires(ModItems.ETERNAL_COOKED_MUTTON.get())
                .requires(ModItems.ETERNAL_BAKED_POTATO.get())
                .requires(ModItems.ETERNAL_COOKED_COD.get())
                .requires(ModItems.ETERNAL_PUMPKIN_PIE.get())
                .requires(ModItems.ETERNAL_MELON_SLICE.get())
                .unlockedBy("has_eternal_cooked_beef", has(ModItems.ETERNAL_COOKED_BEEF.get()))
                .save(writer);

        // 永恒亵渎之鱼：永恒之鱼 + 永恒腐肉 + 永恒毒马铃薯 + 永恒河豚 + 永恒蜘蛛眼无序合成。
        ShapelessRecipeBuilder.shapeless(RecipeCategory.FOOD, ModItems.ETERNAL_BLASPHEMOUS_FISH.get())
                .requires(ModItems.ETERNAL_FISH.get())
                .requires(ModItems.ETERNAL_ROTTEN_FLESH.get())
                .requires(ModItems.ETERNAL_POISONOUS_POTATO.get())
                .requires(ModItems.ETERNAL_PUFFERFISH.get())
                .requires(ModItems.ETERNAL_SPIDER_EYE.get())
                .unlockedBy("has_eternal_fish", has(ModItems.ETERNAL_FISH.get()))
                .save(writer);

        // 无底坑：永恒河豚 + 永恒腐肉 + 永恒生鸡肉无序合成。
        ShapelessRecipeBuilder.shapeless(RecipeCategory.FOOD, ModItems.ETERNAL_ABYSS.get())
                .requires(ModItems.ETERNAL_PUFFERFISH.get())
                .requires(ModItems.ETERNAL_ROTTEN_FLESH.get())
                .requires(ModItems.ETERNAL_CHICKEN.get())
                .unlockedBy("has_eternal_pufferfish", has(ModItems.ETERNAL_PUFFERFISH.get()))
                .save(writer);

        // 永恒紫颂果plus：永恒紫颂果 + 末影珍珠 + 龙息无序合成。
        ShapelessRecipeBuilder.shapeless(RecipeCategory.FOOD, ModItems.ETERNAL_CHORUS_FRUIT_PLUS.get())
                .requires(ModItems.ETERNAL_CHORUS_FRUIT.get())
                .requires(Items.ENDER_PEARL)
                .requires(Items.DRAGON_BREATH)
                .unlockedBy("has_eternal_chorus_fruit", has(ModItems.ETERNAL_CHORUS_FRUIT.get()))
                .save(writer);

        // 永恒药箭：① 成品药箭 + 下界之星；② 永恒滞留型药水 + 箭。
        // 两者都会把原料药水的 NBT 复制到结果上（见 EternalTippedArrowRecipe）。
        saveTippedArrowRecipe(writer, "eternal_tipped_arrow_from_tipped_arrow",
                Items.TIPPED_ARROW, Items.NETHER_STAR);
        saveTippedArrowRecipe(writer, "eternal_tipped_arrow_from_lingering_potion",
                ModItems.ETERNAL_LINGERING_POTION.get(), Items.ARROW);

        // 永恒燃料：烈焰粉 + 干海带块 + 煤炭块 + 木炭 + 烈焰棒 + 岩浆桶 + 下界之星 无序合成。
        // 岩浆桶的合成剩余物（空桶）由原版 Recipe#getRemainingItems 自动返还，无需自定义序列化器。
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModItems.ETERNAL_FUEL.get())
                .requires(Items.BLAZE_POWDER)
                .requires(Items.DRIED_KELP_BLOCK)
                .requires(Items.COAL_BLOCK)
                .requires(Items.CHARCOAL)
                .requires(Items.BLAZE_ROD)
                .requires(Items.LAVA_BUCKET)
                .requires(Items.NETHER_STAR)
                .unlockedBy("has_nether_star", has(Items.NETHER_STAR))
                .save(writer, new ResourceLocation(EternalFoodMod.MOD_ID, "eternal_fuel"));

        // 永恒生食物 → 永恒熟食物的烹饪配方（熔炉 / 烟熏炉 / 营火 + 灵魂营火）。
        cook(writer, ModItems.ETERNAL_BEEF, ModItems.ETERNAL_COOKED_BEEF);
        cook(writer, ModItems.ETERNAL_PORKCHOP, ModItems.ETERNAL_COOKED_PORKCHOP);
        cook(writer, ModItems.ETERNAL_CHICKEN, ModItems.ETERNAL_COOKED_CHICKEN);
        cook(writer, ModItems.ETERNAL_MUTTON, ModItems.ETERNAL_COOKED_MUTTON);
        cook(writer, ModItems.ETERNAL_RABBIT, ModItems.ETERNAL_COOKED_RABBIT);
        cook(writer, ModItems.ETERNAL_COD, ModItems.ETERNAL_COOKED_COD);
        cook(writer, ModItems.ETERNAL_SALMON, ModItems.ETERNAL_COOKED_SALMON);
        cook(writer, ModItems.ETERNAL_POTATO, ModItems.ETERNAL_BAKED_POTATO);

        // 永恒附魔之瓶：附魔之瓶 + 下界之星 无序合成。
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModItems.ETERNAL_EXPERIENCE_BOTTLE.get())
                .requires(Items.EXPERIENCE_BOTTLE)
                .requires(Items.NETHER_STAR)
                .unlockedBy("has_nether_star", has(Items.NETHER_STAR))
                .save(writer);

        // 永恒烟花火箭：成品烟花火箭 + 下界之星，并把成品烟花的 {@code Fireworks} NBT
        //（飞行时间 + 爆炸效果）复制到结果上，继承合成前的所有效果。
        saveFireworkRocketRecipe(writer, "eternal_firework_rocket", Items.FIREWORK_ROCKET, Items.NETHER_STAR);

        // 永恒刷怪蛋：任意原版刷怪蛋 + 下界之星，并把该刷怪蛋对应的生物类型写入结果 NBT。
        saveSpawnEggRecipe(writer);

        // 永恒之星：下界之星、潮涌核心、嗅探兽蛋、龙息、下界合金锭、龙蛋、附魔金苹果、
        // 紫水晶碎片、回响碎片 无序合成（恰好 9 种材料，3x3 工作台）。
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModItems.ETERNAL_STAR.get())
                .requires(Items.NETHER_STAR)
                .requires(Items.CONDUIT)
                .requires(Items.SNIFFER_EGG)
                .requires(Items.DRAGON_BREATH)
                .requires(Items.NETHERITE_INGOT)
                .requires(Items.DRAGON_EGG)
                .requires(Items.ENCHANTED_GOLDEN_APPLE)
                .requires(Items.AMETHYST_SHARD)
                .requires(Items.ECHO_SHARD)
                .unlockedBy("has_nether_star", has(Items.NETHER_STAR))
                .save(writer);

        // 永恒满置域：4 组石头（256 个）+ 4 组深板岩（256 个）+ 1 下界之星 无序合成。
        // 使用自定义序列化器：匹配时额外要求石头 / 深板岩各 256 个（4 个满堆），取出成品时整堆消耗。
        saveFullDomainRecipe(writer);

        // 永恒弹射物配方（均无序合成）：
        //   永恒雪球 / 永恒末影珍珠 / 永恒火焰弹 = 下界之星 + 对应材料；
        //   永恒火球 = 永恒火焰弹 + TNT；
        //   永恒凋灵之首 = 下界之星 + 凋灵骷髅头颅；
        //   永恒蓝色凋灵之首 = 永恒凋灵之首 + TNT。
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModItems.ETERNAL_SNOWBALL.get())
                .requires(Items.SNOWBALL)
                .requires(Items.NETHER_STAR)
                .unlockedBy("has_nether_star", has(Items.NETHER_STAR))
                .save(writer);
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModItems.ETERNAL_ENDER_PEARL.get())
                .requires(Items.ENDER_PEARL)
                .requires(Items.NETHER_STAR)
                .unlockedBy("has_nether_star", has(Items.NETHER_STAR))
                .save(writer);
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModItems.ETERNAL_FIRE_CHARGE.get())
                .requires(Items.FIRE_CHARGE)
                .requires(Items.NETHER_STAR)
                .unlockedBy("has_nether_star", has(Items.NETHER_STAR))
                .save(writer);
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModItems.ETERNAL_FIREBALL.get())
                .requires(ModItems.ETERNAL_FIRE_CHARGE.get())
                .requires(Items.TNT)
                .unlockedBy("has_eternal_fire_charge", has(ModItems.ETERNAL_FIRE_CHARGE.get()))
                .save(writer);
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModItems.ETERNAL_WITHER_SKULL.get())
                .requires(Items.WITHER_SKELETON_SKULL)
                .requires(Items.NETHER_STAR)
                .unlockedBy("has_nether_star", has(Items.NETHER_STAR))
                .save(writer);
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModItems.ETERNAL_WITHER_SKULL_BLUE.get())
                .requires(ModItems.ETERNAL_WITHER_SKULL.get())
                .requires(Items.TNT)
                .unlockedBy("has_eternal_wither_skull", has(ModItems.ETERNAL_WITHER_SKULL.get()))
                .save(writer);
    }

    /**
     * 烹饪：所有有对应"永恒熟食物"的永恒生食物，都可以用熔炉（smelting）、烟熏炉（smoking）、
     * 营火 / 灵魂营火（campfire_cooking，灵魂营火与营火共用该配方类型）做成对应的永恒熟食物。
     * 干海带本身就是熟食（没有"永恒生海带"），永恒胡萝卜等没有对应永恒熟食，均不生成配方。
     */
    private static void cook(Consumer<FinishedRecipe> writer, RegistryObject<Item> raw, RegistryObject<Item> cooked) {
        Item rawItem = raw.get();
        Item cookedItem = cooked.get();
        ResourceLocation cookedId = BuiltInRegistries.ITEM.getKey(cookedItem);
        String unlock = "has_" + BuiltInRegistries.ITEM.getKey(rawItem).getPath();
        SimpleCookingRecipeBuilder.smelting(Ingredient.of(rawItem), RecipeCategory.FOOD, cookedItem, 0.35F, 200)
                .unlockedBy(unlock, has(rawItem))
                .save(writer, new ResourceLocation(cookedId.getNamespace(), cookedId.getPath() + "_from_smelting"));
        SimpleCookingRecipeBuilder.smoking(Ingredient.of(rawItem), RecipeCategory.FOOD, cookedItem, 0.35F, 100)
                .unlockedBy(unlock, has(rawItem))
                .save(writer, new ResourceLocation(cookedId.getNamespace(), cookedId.getPath() + "_from_smoking"));
        SimpleCookingRecipeBuilder.campfireCooking(Ingredient.of(rawItem), RecipeCategory.FOOD, cookedItem, 0.35F, 600)
                .unlockedBy(unlock, has(rawItem))
                .save(writer, new ResourceLocation(cookedId.getNamespace(),
                        cookedId.getPath() + "_from_campfire_cooking"));
    }

    /**
     * 手写永恒药箭配方：无序合成 {@code potionSource + other}，使用会复制药水 NBT 的
     * 自定义序列化器。{@code potionSource} 为原版药箭或永恒滞留型药水。
     */
    private static void saveTippedArrowRecipe(Consumer<FinishedRecipe> writer, String name,
            Item potionSource, Item other) {
        ResourceLocation id = new ResourceLocation(EternalFoodMod.MOD_ID, name);
        ResourceLocation resultId = BuiltInRegistries.ITEM.getKey(ModItems.ETERNAL_TIPPED_ARROW.get());
        writer.accept(new FinishedRecipe() {
            @Override
            public void serializeRecipeData(JsonObject json) {
                json.addProperty("category", "misc");
                JsonArray ingredients = new JsonArray();
                ingredients.add(Ingredient.of(potionSource).toJson());
                ingredients.add(Ingredient.of(other).toJson());
                json.add("ingredients", ingredients);
                JsonObject resultJson = new JsonObject();
                resultJson.addProperty("item", resultId.toString());
                json.add("result", resultJson);
            }

            @Override
            public ResourceLocation getId() {
                return id;
            }

            @Override
            public RecipeSerializer<?> getType() {
                return ModRecipes.TIPPED_ARROW_SERIALIZER.get();
            }

            @Override
            public JsonObject serializeAdvancement() {
                return null;
            }

            @Override
            public ResourceLocation getAdvancementId() {
                return null;
            }
        });
    }

    /**
     * 手写永恒刷怪蛋配方：任意原版刷怪蛋 + 下界之星 无序合成，使用会把刷怪蛋生物类型写入结果
     * {@code EntityTag.id} 的自定义序列化器。原料列出全部原版刷怪蛋（不含永恒刷怪蛋本身），
     * 结果的颜色覆盖层会在客户端按该类型从对应原版刷怪蛋取色。
     */
    private static void saveSpawnEggRecipe(Consumer<FinishedRecipe> writer) {
        List<Item> eggs = new ArrayList<>();
        for (Item item : BuiltInRegistries.ITEM) {
            if (item instanceof SpawnEggItem && !(item instanceof EternalSpawnEggItem)) {
                eggs.add(item);
            }
        }
        if (eggs.isEmpty()) {
            return;
        }
        Ingredient spawnEgg = Ingredient.of(eggs.toArray(new Item[0]));
        Ingredient netherStar = Ingredient.of(Items.NETHER_STAR);
        ResourceLocation id = new ResourceLocation(EternalFoodMod.MOD_ID, "eternal_spawn_egg");
        ResourceLocation resultId = BuiltInRegistries.ITEM.getKey(ModItems.ETERNAL_SPAWN_EGG.get());
        writer.accept(new FinishedRecipe() {
            @Override
            public void serializeRecipeData(JsonObject json) {
                json.addProperty("category", "misc");
                JsonArray ingredients = new JsonArray();
                ingredients.add(spawnEgg.toJson());
                ingredients.add(netherStar.toJson());
                json.add("ingredients", ingredients);
                JsonObject resultJson = new JsonObject();
                resultJson.addProperty("item", resultId.toString());
                json.add("result", resultJson);
            }

            @Override
            public ResourceLocation getId() {
                return id;
            }

            @Override
            public RecipeSerializer<?> getType() {
                return ModRecipes.SPAWN_EGG_SERIALIZER.get();
            }

            @Override
            public JsonObject serializeAdvancement() {
                return null;
            }

            @Override
            public ResourceLocation getAdvancementId() {
                return null;
            }
        });
    }

    /**
     * 手写永恒烟花火箭配方：无序合成 {@code fireworkSource + other}，使用会复制烟花 NBT 的
     * 自定义序列化器。
     */
    private static void saveFireworkRocketRecipe(Consumer<FinishedRecipe> writer, String name,
            Item fireworkSource, Item other) {
        ResourceLocation id = new ResourceLocation(EternalFoodMod.MOD_ID, name);
        ResourceLocation resultId = BuiltInRegistries.ITEM.getKey(ModItems.ETERNAL_FIREWORK_ROCKET.get());
        writer.accept(new FinishedRecipe() {
            @Override
            public void serializeRecipeData(JsonObject json) {
                json.addProperty("category", "misc");
                JsonArray ingredients = new JsonArray();
                ingredients.add(Ingredient.of(fireworkSource).toJson());
                ingredients.add(Ingredient.of(other).toJson());
                json.add("ingredients", ingredients);
                JsonObject resultJson = new JsonObject();
                resultJson.addProperty("item", resultId.toString());
                json.add("result", resultJson);
            }

            @Override
            public ResourceLocation getId() {
                return id;
            }

            @Override
            public RecipeSerializer<?> getType() {
                return ModRecipes.FIREWORK_ROCKET_SERIALIZER.get();
            }

            @Override
            public JsonObject serializeAdvancement() {
                return null;
            }

            @Override
            public ResourceLocation getAdvancementId() {
                return null;
            }
        });
    }

    /** 手写迷之炖菜配方，使其使用会复制 {@code Effects} NBT 的自定义序列化器。 */
    private static void saveSuspiciousStewRecipe(Consumer<FinishedRecipe> writer, Item result) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(result);
        writer.accept(new FinishedRecipe() {
            @Override
            public void serializeRecipeData(JsonObject json) {
                json.addProperty("category", "misc");
                JsonArray ingredients = new JsonArray();
                ingredients.add(Ingredient.of(Items.SUSPICIOUS_STEW).toJson());
                ingredients.add(Ingredient.of(Items.NETHER_STAR).toJson());
                json.add("ingredients", ingredients);
                JsonObject resultJson = new JsonObject();
                resultJson.addProperty("item", id.toString());
                json.add("result", resultJson);
            }

            @Override
            public ResourceLocation getId() {
                return id;
            }

            @Override
            public RecipeSerializer<?> getType() {
                return ModRecipes.STEW_SERIALIZER.get();
            }

            @Override
            public JsonObject serializeAdvancement() {
                return null;
            }

            @Override
            public ResourceLocation getAdvancementId() {
                return null;
            }
        });
    }

    /**
     * 手写永恒满置域配方：4 组石头 + 4 组深板岩 + 1 下界之星 无序合成，使用会额外要求石头 /
     * 深板岩各 256 个（4 个满堆）、并在取出成品时整堆消耗的自定义序列化器。
     */
    private static void saveFullDomainRecipe(Consumer<FinishedRecipe> writer) {
        ResourceLocation id = new ResourceLocation(EternalFoodMod.MOD_ID, "eternal_full_domain");
        ResourceLocation resultId = BuiltInRegistries.ITEM.getKey(ModItems.ETERNAL_FULL_DOMAIN.get());
        Ingredient stone = Ingredient.of(Items.STONE);
        Ingredient deepslate = Ingredient.of(Items.DEEPSLATE);
        Ingredient netherStar = Ingredient.of(Items.NETHER_STAR);
        writer.accept(new FinishedRecipe() {
            @Override
            public void serializeRecipeData(JsonObject json) {
                json.addProperty("category", "misc");
                JsonArray ingredients = new JsonArray();
                for (int i = 0; i < 4; i++) {
                    ingredients.add(stone.toJson());
                }
                for (int i = 0; i < 4; i++) {
                    ingredients.add(deepslate.toJson());
                }
                ingredients.add(netherStar.toJson());
                json.add("ingredients", ingredients);
                JsonObject resultJson = new JsonObject();
                resultJson.addProperty("item", resultId.toString());
                json.add("result", resultJson);
            }

            @Override
            public ResourceLocation getId() {
                return id;
            }

            @Override
            public RecipeSerializer<?> getType() {
                return ModRecipes.FULL_DOMAIN_SERIALIZER.get();
            }

            @Override
            public JsonObject serializeAdvancement() {
                return null;
            }

            @Override
            public ResourceLocation getAdvancementId() {
                return null;
            }
        });
    }
}
