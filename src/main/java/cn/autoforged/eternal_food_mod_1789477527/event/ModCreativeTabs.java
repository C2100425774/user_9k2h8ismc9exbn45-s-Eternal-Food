package cn.autoforged.eternal_food_mod_1789477527.event;

import cn.autoforged.eternal_food_mod_1789477527.EternalFoodMod;
import cn.autoforged.eternal_food_mod_1789477527.item.EternalSpawnEggItem;
import cn.autoforged.eternal_food_mod_1789477527.item.ModItems;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SuspiciousStewItem;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

/**
 * 本模组独立的创造模式物品栏页签。
 *
 * <p>把所有永恒物品单开一页，避免全部塞进原版「食物与饮品」页签后在物品多起来时难以查找。
 */
public class ModCreativeTabs {

    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, EternalFoodMod.MOD_ID);

    public static final RegistryObject<CreativeModeTab> ETERNAL_FOOD_TAB = CREATIVE_MODE_TABS.register("eternal_food",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup." + EternalFoodMod.MOD_ID))
                    .icon(() -> new ItemStack(ModItems.ETERNAL_MILK_PLUS.get()))
                    .displayItems((parameters, output) -> {
                        // ITEMS.getEntries() 覆盖全部注册物品（含 milk_plus），保持注册顺序。
                        // 无效果的永恒药水 / 永恒迷之炖菜不直接展示，改用带效果的示范栈代替。
                        for (RegistryObject<Item> item : ModItems.ITEMS.getEntries()) {
                            Item value = item.get();
                            if (value == ModItems.ETERNAL_POTION.get()) {
                                output.accept(potion(ModItems.ETERNAL_POTION.get(), Potions.STRONG_HEALING));
                            } else if (value == ModItems.ETERNAL_SPLASH_POTION.get()) {
                                output.accept(potion(ModItems.ETERNAL_SPLASH_POTION.get(), Potions.STRONG_HARMING));
                            } else if (value == ModItems.ETERNAL_LINGERING_POTION.get()) {
                                output.accept(lingeringWeakness());
                            } else if (value == ModItems.ETERNAL_TIPPED_ARROW.get()) {
                                // 创造模式页签中的永恒药箭示范栈：瞬间伤害 II（与原版药箭默认中毒不同）。
                                output.accept(potion(ModItems.ETERNAL_TIPPED_ARROW.get(), Potions.STRONG_HARMING));
                            } else if (value == ModItems.ETERNAL_SUSPICIOUS_STEW.get()) {
                                output.accept(nightVisionStew());
                            } else if (value == ModItems.ETERNAL_FIREWORK_ROCKET.get()) {
                                output.accept(fireworkRocket());
                            } else if (value == ModItems.ETERNAL_SPAWN_EGG.get()) {
                                // 创造模式页签中的两个永恒刷怪蛋：闪电束（苦力怕配色）与
                                // 唤魔者尖牙（唤魔者配色）。物品名均为「永恒刷怪蛋」，描述里写明生成的生物。
                                output.accept(spawnEgg(EntityType.LIGHTNING_BOLT));
                                output.accept(spawnEgg(EntityType.EVOKER_FANGS));
                            } else {
                                output.accept(value);
                            }
                        }
                    })
                    .build());

    /** 构造带指定药水类型的永恒药水栈（原版药水类型 NBT），创造性示范用。 */
    private static ItemStack potion(Item item, net.minecraft.world.item.alchemy.Potion potion) {
        ItemStack stack = new ItemStack(item);
        PotionUtils.setPotion(stack, potion);
        return stack;
    }

    /**
     * 创造性示范用的永恒滞留型虚弱药水。
     *
     * <p>用原版长效虚弱药水（{@link Potions#LONG_WEAKNESS}，4800 tick / 4 分钟）作为药水类型；
     * 滞留药水云会对基础药水效果再 /4（见 {@code AreaEffectCloud}），实际生效 1200 tick = 1 分钟。
     * 颜色同样取原版虚弱效果色，而非自定义效果栈。
     */
    private static ItemStack lingeringWeakness() {
        ItemStack stack = new ItemStack(ModItems.ETERNAL_LINGERING_POTION.get());
        PotionUtils.setPotion(stack, Potions.LONG_WEAKNESS);
        return stack;
    }

    /** 构造带夜视效果的永恒迷之炖菜栈（原版虞美人迷之炖菜 = 夜视 5 秒 = 100 tick），创造性示范用。 */
    private static ItemStack nightVisionStew() {
        ItemStack stack = new ItemStack(ModItems.ETERNAL_SUSPICIOUS_STEW.get());
        SuspiciousStewItem.saveMobEffect(stack, MobEffects.NIGHT_VISION, 100);
        return stack;
    }

    /**
     * 创造性示范用的永恒烟花火箭：飞行时间 3（{@code Fireworks.Flight = 3}），且不含任何爆炸
     * （无 {@code Explosions} 列表，即没有烟火之星效果），与 vanilla 纯火箭一致。
     */
    private static ItemStack fireworkRocket() {
        ItemStack stack = new ItemStack(ModItems.ETERNAL_FIREWORK_ROCKET.get());
        stack.getOrCreateTagElement("Fireworks").putByte("Flight", (byte) 3);
        return stack;
    }

    /** 构造指定生物类型的永恒刷怪蛋栈（类型存 EntityTag.id）。 */
    private static ItemStack spawnEgg(EntityType<?> type) {
        ItemStack stack = new ItemStack(ModItems.ETERNAL_SPAWN_EGG.get());
        EternalSpawnEggItem.setEntityType(stack, type);
        return stack;
    }

    public static void register(IEventBus modEventBus) {
        CREATIVE_MODE_TABS.register(modEventBus);
    }
}
