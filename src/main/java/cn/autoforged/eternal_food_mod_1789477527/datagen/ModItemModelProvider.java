package cn.autoforged.eternal_food_mod_1789477527.datagen;

import cn.autoforged.eternal_food_mod_1789477527.EternalFoodMod;
import cn.autoforged.eternal_food_mod_1789477527.item.ModItems;
import net.minecraft.data.PackOutput;
import net.minecraft.world.item.Item;
import net.minecraftforge.client.model.generators.ItemModelProvider;
import net.minecraftforge.client.model.generators.ModelFile;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.registries.RegistryObject;

public class ModItemModelProvider extends ItemModelProvider {

    public ModItemModelProvider(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(output, EternalFoodMod.MOD_ID, existingFileHelper);
    }

    @Override
    protected void registerModels() {
        // 遍历全部注册物品，确保 eternal_milk_plus 也有物品模型。
        for (RegistryObject<Item> item : ModItems.ITEMS.getEntries()) {
            Item value = item.get();
            // 注意：永恒蛋糕虽然是 BlockItem，但背包图标仍然用原来的平面物品贴图
            //（textures/item/eternal_cake_slice.png），所以这里照常走 basicItem 生成
            // item/generated 模型，不要跳过。
            if (value == ModItems.ETERNAL_POTION.get()
                    || value == ModItems.ETERNAL_SPLASH_POTION.get()
                    || value == ModItems.ETERNAL_LINGERING_POTION.get()) {
                potionItemModel(item.getId().getPath());
            } else if (value == ModItems.ETERNAL_TIPPED_ARROW.get()) {
                tippedArrowItemModel(item.getId().getPath());
            } else if (value == ModItems.ETERNAL_FUEL.get()) {
                // 物品 id 为 eternal_fuel，但用户指定纹理为 eternal_blaze_powder。
                singleTexture(item.getId().getPath(), mcLoc("item/generated"),
                        "layer0", modLoc("item/eternal_blaze_powder"));
            } else if (value == ModItems.ETERNAL_ENCHANTED_FISH.get()) {
                // 附魔永恒之鱼沿用永恒之鱼的纹理，附魔光效由物品的 isFoil 提供。
                singleTexture(item.getId().getPath(), mcLoc("item/generated"),
                        "layer0", modLoc("item/eternal_fish"));
            } else if (value == ModItems.ETERNAL_ABYSS.get()) {
                // 物品 id 为 eternal_abyss，但用户指定纹理为 abyss。
                singleTexture(item.getId().getPath(), mcLoc("item/generated"),
                        "layer0", modLoc("item/abyss"));
            } else if (value == ModItems.ETERNAL_SPAWN_EGG.get()) {
                spawnEggItemModel(item.getId().getPath());
            } else {
                basicItem(value);
            }
        }
    }

    /**
     * 药水物品模型：layer0 = 液体覆盖层（potion_overlay，运行时用对应药水颜色染色），
     * layer1 = 瓶身纹理（eternal_potion 等，不参与染色）。
     */
    private void potionItemModel(String name) {
        getBuilder(name)
                .parent(new ModelFile.UncheckedModelFile("item/generated"))
                .texture("layer0", modLoc("item/potion_overlay"))
                .texture("layer1", modLoc("item/" + name));
    }

    /**
     * 永恒刷怪蛋物品模型（三层）：
     * layer0 = eternal_spawn_egg 金色描边（不染色），
     * layer1 = spawn_egg 背景色层（tintindex 1，运行时用对应刷怪蛋背景色相乘），
     * layer2 = spawn_egg_overlay 高光色层（tintindex 2，运行时用对应刷怪蛋高光色相乘）。
     */
    private void spawnEggItemModel(String name) {
        getBuilder(name)
                .parent(new ModelFile.UncheckedModelFile("item/generated"))
                .texture("layer0", modLoc("item/eternal_spawn_egg"))
                .texture("layer1", modLoc("item/spawn_egg"))
                .texture("layer2", modLoc("item/spawn_egg_overlay"));
    }

    /**
     * 药箭物品模型（与原版药箭同结构）：layer0 = 箭头覆盖层（tipped_arrow_head，运行时用对应
     * 药水颜色相乘染色），layer1 = 箭身纹理（eternal_tipped_arrow_base，不参与染色）。
     */
    private void tippedArrowItemModel(String name) {
        getBuilder(name)
                .parent(new ModelFile.UncheckedModelFile("item/generated"))
                .texture("layer0", modLoc("item/tipped_arrow_head"))
                .texture("layer1", modLoc("item/eternal_tipped_arrow_base"));
    }
}
