package cn.autoforged.eternal_food_mod_1789477527.event;

import cn.autoforged.eternal_food_mod_1789477527.EternalFoodMod;
import cn.autoforged.eternal_food_mod_1789477527.item.EternalSpawnEggItem;
import cn.autoforged.eternal_food_mod_1789477527.item.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
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
                                // 无限时长夜视药水（示例栈）：与「永恒之星 + 药水」在铁砧中合成的产物一致。
                                output.accept(infinitePotion(ModItems.ETERNAL_POTION.get(),
                                        MobEffects.NIGHT_VISION.getColor(), null,
                                        infiniteEffect(MobEffects.NIGHT_VISION, 0)));
                                // 信标药水：六种信标效果全部无限时长（对应原版 /give 指令）。
                                output.accept(beaconPotion());
                            } else if (value == ModItems.ETERNAL_SPLASH_POTION.get()) {
                                output.accept(potion(ModItems.ETERNAL_SPLASH_POTION.get(), Potions.STRONG_HARMING));
                                // 无限时长「抗性提升 V」喷溅型药水（指令等级 5 = amplifier 4）。
                                // 颜色固定用 10044730（#99453A，旧版抗性提升色）：1.19.4 起原版
                                // DAMAGE_RESISTANCE 的效果色改成了 #9146F0，直接用 effect.getColor()
                                // 会变成紫色（看起来像不可合成的药水），所以这里写死作者指定的旧色。
                                output.accept(infinitePotion(ModItems.ETERNAL_SPLASH_POTION.get(),
                                        10044730, null,
                                        infiniteEffect(MobEffects.DAMAGE_RESISTANCE, 4)));
                            } else if (value == ModItems.ETERNAL_LINGERING_POTION.get()) {
                                output.accept(lingeringWeakness());
                            } else if (value == ModItems.ETERNAL_TIPPED_ARROW.get()) {
                                // 创造模式页签中的永恒药箭示范栈：瞬间伤害 II（与原版药箭默认中毒不同）。
                                output.accept(potion(ModItems.ETERNAL_TIPPED_ARROW.get(), Potions.STRONG_HARMING));
                            } else if (value == ModItems.ETERNAL_SUSPICIOUS_STEW.get()) {
                                output.accept(nightVisionStew());
                            } else if (value == ModItems.ETERNAL_FIREWORK_ROCKET.get()) {
                                output.accept(fireworkRocket());
                                output.accept(fireworkMissile());
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
     * 构造「无限时长」的自定义效果药水栈（创造模式示范用）。
     *
     * <p>写法与 {@code event/AnvilCombineHandler} 中「永恒之星 + 药水 → 无限药水」的产物完全一致：
     * 不写 {@code Potion} 标签，而是把效果以 {@code CustomPotionEffects} 写回，时长用
     * {@link MobEffectInstance#INFINITE_DURATION}（-1，等同于指令里的 {@code infinite}）。
     * 这样 {@code PotionUtils.addPotionTooltip} 会把时长显示成 {@code **:**}（原版
     * {@code effect.duration.infinite}），饮用 / 投掷时原版逻辑也会直接施加无限时长效果
     *（喷溅型的距离衰减走 {@code MobEffectInstance#mapDuration}，无限时长不会被衰减）。
     *
     * <p>必须同时写 {@code CustomPotionColor}：没有 {@code Potion} 标签、或效果全部隐藏粒子时
     * {@code PotionUtils.getColor} 都会退回默认色，导致图标液体颜色不对。
     *
     * @param item      永恒药水物品（普通 / 喷溅 / 滞留）
     * @param color     {@code CustomPotionColor}（液体颜色）
     * @param hoverName 自定义名字（可为 null）；非 null 时写入 {@code display.Name}，
     *                  原版会显示在物品名处、按组件样式着色、并按组件里的 italic 决定是否斜体
     * @param effects   效果列表（应使用 {@link #infiniteEffect} 构造）
     */
    private static ItemStack infinitePotion(Item item, int color, Component hoverName,
            MobEffectInstance... effects) {
        ItemStack stack = new ItemStack(item);
        ListTag list = new ListTag();
        for (MobEffectInstance effect : effects) {
            list.add(effect.save(new CompoundTag()));
        }
        CompoundTag tag = stack.getOrCreateTag();
        tag.put(PotionUtils.TAG_CUSTOM_POTION_EFFECTS, list);
        tag.putInt(PotionUtils.TAG_CUSTOM_POTION_COLOR, color);
        if (hoverName != null) {
            stack.setHoverName(hoverName);
        }
        return stack;
    }

    /**
     * 无限时长 + 隐藏粒子的单个效果，对应指令里的 {@code Duration:-1, ShowParticles:0b}。
     *
     * <p>{@code visible=false} 只关粒子；{@code showIcon=true} 保留 HUD 效果图标
     *（指令里不写 {@code ShowIcon} 时默认也是 true）。
     */
    private static MobEffectInstance infiniteEffect(MobEffect effect, int amplifier) {
        return new MobEffectInstance(effect, MobEffectInstance.INFINITE_DURATION, amplifier,
                false, false, true);
    }

    /**
     * 创造性示范用的「信标药水」：六种信标效果全部无限时长、隐藏粒子，淡紫色名字。
     *
     * <p>对应原版指令：
     * {@code /give @p minecraft:potion{CustomPotionColor:6085589,CustomPotionEffects:[
     * {Id:1,...,Amplifier:1},{Id:3,...,Amplifier:1},{Id:11,...,Amplifier:1},
     * {Id:8,...,Amplifier:1},{Id:5,...,Amplifier:1},{Id:10,...,Amplifier:0}],
     * display:{Name:'{"text":"信标药水","italic":false,"color":"light_purple"}'}}}（时长全部 -1）。
     * 效果 ID：1 = 速度、3 = 急迫、11 = 抗性提升、8 = 跳跃提升、5 = 力量、10 = 生命恢复。
     */
    private static ItemStack beaconPotion() {
        return infinitePotion(ModItems.ETERNAL_POTION.get(), 6085589,
                Component.translatable("potion." + EternalFoodMod.MOD_ID + ".beacon")
                        .withStyle(style -> style.withColor(ChatFormatting.LIGHT_PURPLE).withItalic(false)),
                infiniteEffect(MobEffects.MOVEMENT_SPEED, 1),
                infiniteEffect(MobEffects.DIG_SPEED, 1),
                infiniteEffect(MobEffects.DAMAGE_RESISTANCE, 1),
                infiniteEffect(MobEffects.JUMP, 1),
                infiniteEffect(MobEffects.DAMAGE_BOOST, 1),
                infiniteEffect(MobEffects.REGENERATION, 0));
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

    /** 永恒烟花火箭导弹：一个红色大型球状星，飞行 3。 */
    private static ItemStack fireworkMissile() {
        ItemStack stack = new ItemStack(ModItems.ETERNAL_FIREWORK_ROCKET.get());
        net.minecraft.nbt.CompoundTag fireworks = stack.getOrCreateTagElement("Fireworks");
        fireworks.putByte("Flight", (byte) 3);
        net.minecraft.nbt.ListTag explosions = new net.minecraft.nbt.ListTag();
        net.minecraft.nbt.CompoundTag explosion = new net.minecraft.nbt.CompoundTag();
        explosion.putByte("Type", (byte) 4);
        explosion.putBoolean("Trail", true);
        explosion.putIntArray("Colors", new int[]{16711680});
        for (int i = 0; i < 8; i++) {
            explosions.add(explosion.copy());
        }
        fireworks.put("Explosions", explosions);
        net.minecraft.nbt.CompoundTag tag = stack.getOrCreateTag();
        fireworks.putBoolean("EternalMissile", true);
        tag.putBoolean("EternalMissile", true);
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
