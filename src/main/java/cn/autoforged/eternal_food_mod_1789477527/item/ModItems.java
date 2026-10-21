package cn.autoforged.eternal_food_mod_1789477527.item;

import cn.autoforged.eternal_food_mod_1789477527.EternalFoodMod;
import cn.autoforged.eternal_food_mod_1789477527.block.ModBlocks;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.ChatFormatting;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * 永恒食物物品注册表。每个原版食物都对应一个永恒版本。
 *
 * <p>{@link #ETERNAL_TO_BASE} 记录永恒物品 → 原版食物的映射，供数据生成
 * （配方：原版食物 + 下界之星）与创造模式物品栏使用。
 */
public class ModItems {

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, EternalFoodMod.MOD_ID);

    /** 永恒物品 → 原版基础食物，保持注册顺序。 */
    public static final Map<RegistryObject<Item>, Item> ETERNAL_TO_BASE = new LinkedHashMap<>();

    public static final RegistryObject<Item> ETERNAL_APPLE = food("eternal_apple", Items.APPLE);
    public static final RegistryObject<Item> ETERNAL_GOLDEN_APPLE = food("eternal_golden_apple", Items.GOLDEN_APPLE);
    public static final RegistryObject<Item> ETERNAL_ENCHANTED_GOLDEN_APPLE =
            food("eternal_enchanted_golden_apple", Items.ENCHANTED_GOLDEN_APPLE);
    public static final RegistryObject<Item> ETERNAL_BREAD = food("eternal_bread", Items.BREAD);
    public static final RegistryObject<Item> ETERNAL_COOKIE = food("eternal_cookie", Items.COOKIE);
    // 永恒蛋糕已升级为真正的方块（见 block/EternalCakeBlock）。
    // 注册名保持 eternal_cake_slice 不变：旧存档里的永恒蛋糕物品升级后直接变成这个方块物品，不会丢失。
    // 食用不再靠物品，而是放下方块后右键方块（营养 14 / 饱和系数 0.1，方块不消耗）。
    public static final RegistryObject<Item> ETERNAL_CAKE_SLICE = ITEMS.register("eternal_cake_slice",
            () -> new BlockItem(ModBlocks.ETERNAL_CAKE.get(),
                    new Item.Properties().stacksTo(1).rarity(Rarity.RARE)));

    static {
        // 仍保留「原版蛋糕 + 下界之星 → 永恒蛋糕」的配方映射（配方 JSON 见 ModRecipeProvider）。
        ETERNAL_TO_BASE.put(ETERNAL_CAKE_SLICE, Items.CAKE);
    }
    public static final RegistryObject<Item> ETERNAL_PUMPKIN_PIE = food("eternal_pumpkin_pie", Items.PUMPKIN_PIE);
    public static final RegistryObject<Item> ETERNAL_CARROT = food("eternal_carrot", Items.CARROT);
    public static final RegistryObject<Item> ETERNAL_GOLDEN_CARROT = food("eternal_golden_carrot", Items.GOLDEN_CARROT);
    // 永恒马铃薯对应原版生马铃薯：可用来吸引 / 繁殖猪（见 EternalInteractionHandler）。
    public static final RegistryObject<Item> ETERNAL_POTATO = food("eternal_potato", Items.POTATO);
    public static final RegistryObject<Item> ETERNAL_BAKED_POTATO = food("eternal_baked_potato", Items.BAKED_POTATO);
    public static final RegistryObject<Item> ETERNAL_POISONOUS_POTATO =
            food("eternal_poisonous_potato", Items.POISONOUS_POTATO);
    public static final RegistryObject<Item> ETERNAL_BEETROOT = food("eternal_beetroot", Items.BEETROOT);
    public static final RegistryObject<Item> ETERNAL_BEETROOT_SOUP = food("eternal_beetroot_soup", Items.BEETROOT_SOUP);
    public static final RegistryObject<Item> ETERNAL_DRIED_KELP = food("eternal_dried_kelp", Items.DRIED_KELP);
    public static final RegistryObject<Item> ETERNAL_MELON_SLICE = food("eternal_melon_slice", Items.MELON_SLICE);
    public static final RegistryObject<Item> ETERNAL_SWEET_BERRIES = food("eternal_sweet_berries", Items.SWEET_BERRIES);
    public static final RegistryObject<Item> ETERNAL_GLOW_BERRIES = food("eternal_glow_berries", Items.GLOW_BERRIES);
    // alwaysEat：由原版 Foods.CHORUS_FRUIT 继承而来，满饥饿 / 满饱和度时仍可食用。此处显式保留。
    public static final RegistryObject<Item> ETERNAL_CHORUS_FRUIT = special("eternal_chorus_fruit", Items.CHORUS_FRUIT,
            new FoodProperties.Builder().nutrition(4).saturationMod(0.3F).alwaysEat().build());
    // 永恒紫颂果plus：由永恒紫颂果 + 末影珍珠 + 龙息无序合成（配方见 ModRecipeProvider）。
    // 食用后无论身处哪个维度都传送到自己的出生点（见 EternalChorusFruitPlusItem），食用不消耗、无冷却。
    // 没有对应的原版基础食物，因此不进入 ETERNAL_TO_BASE。营养 / 饱和度沿用紫颂果（4 / 0.1）。
    public static final RegistryObject<Item> ETERNAL_CHORUS_FRUIT_PLUS = ITEMS.register("eternal_chorus_fruit_plus",
            () -> new EternalChorusFruitPlusItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE)
                    .food(new FoodProperties.Builder().nutrition(4).saturationMod(0.1F).alwaysEat().build())));
    public static final RegistryObject<Item> ETERNAL_MUSHROOM_STEW = food("eternal_mushroom_stew", Items.MUSHROOM_STEW);
    // 永恒红蘑菇 / 永恒棕蘑菇：食用速度与干海带一致（16 tick），各回复 1 饥饿值、1 饱和度
    //（实际饱和度 = nutrition × saturationMod × 2，故 nutrition 1 + saturationMod 0.5）。
    // 食用后玩家直接就地躺下睡觉，不再需要站在床上；创造模式飞行时可在半空中直接躺下。
    // 两者时间门槛互补：红蘑菇仅夜间 / 雷暴可入睡（睡醒后把时间设为 1000 即白天，跳过夜晚），白天
    // 使用会提示「你只能在夜间或雷暴中入眠」且不睡觉；棕蘑菇仅白天可入睡（睡醒后把时间设为 13000
    // 即夜晚，见 event/EternalSleepHandler），夜晚使用会提示「你只能在白天使用永恒灰蘑菇睡觉」且不睡觉。
    // 睡觉走原版睡眠代码（可点击起床提前醒来），但不调用 setRespawnPosition、不重置重生点
    //（实现见 EternalSleepMushroomItem）。
    // 两者的合成配方额外需要一张床（任意颜色，见 ModRecipeProvider）。
    // sleepDuringDay：false = 红蘑菇（夜间 / 雷暴），true = 棕蘑菇（白天）。
    public static final RegistryObject<Item> ETERNAL_RED_MUSHROOM = sleepingMushroom("eternal_red_mushroom",
            Items.RED_MUSHROOM, "tooltip.eternal_food_mod_1789477527.red_mushroom",
            "tooltip.eternal_food_mod_1789477527.red_mushroom_offhand", false);
    public static final RegistryObject<Item> ETERNAL_BROWN_MUSHROOM = sleepingMushroom("eternal_brown_mushroom",
            Items.BROWN_MUSHROOM, "tooltip.eternal_food_mod_1789477527.brown_mushroom",
            "tooltip.eternal_food_mod_1789477527.brown_mushroom_offhand", true);
    public static final RegistryObject<Item> ETERNAL_RABBIT_STEW = food("eternal_rabbit_stew", Items.RABBIT_STEW);
    public static final RegistryObject<Item> ETERNAL_SUSPICIOUS_STEW =
            food("eternal_suspicious_stew", Items.SUSPICIOUS_STEW);
    public static final RegistryObject<Item> ETERNAL_COOKED_BEEF = food("eternal_cooked_beef", Items.COOKED_BEEF);
    public static final RegistryObject<Item> ETERNAL_COOKED_PORKCHOP =
            food("eternal_cooked_porkchop", Items.COOKED_PORKCHOP);
    public static final RegistryObject<Item> ETERNAL_COOKED_CHICKEN =
            food("eternal_cooked_chicken", Items.COOKED_CHICKEN);
    public static final RegistryObject<Item> ETERNAL_COOKED_MUTTON =
            food("eternal_cooked_mutton", Items.COOKED_MUTTON);
    public static final RegistryObject<Item> ETERNAL_COOKED_RABBIT =
            food("eternal_cooked_rabbit", Items.COOKED_RABBIT);
    public static final RegistryObject<Item> ETERNAL_COOKED_COD = food("eternal_cooked_cod", Items.COOKED_COD);
    public static final RegistryObject<Item> ETERNAL_COOKED_SALMON = food("eternal_cooked_salmon", Items.COOKED_SALMON);
    public static final RegistryObject<Item> ETERNAL_RABBIT = food("eternal_rabbit", Items.RABBIT);
    public static final RegistryObject<Item> ETERNAL_BEEF = food("eternal_beef", Items.BEEF);
    public static final RegistryObject<Item> ETERNAL_PORKCHOP = food("eternal_porkchop", Items.PORKCHOP);
    public static final RegistryObject<Item> ETERNAL_CHICKEN = food("eternal_chicken", Items.CHICKEN);
    public static final RegistryObject<Item> ETERNAL_MUTTON = food("eternal_mutton", Items.MUTTON);
    public static final RegistryObject<Item> ETERNAL_COD = food("eternal_cod", Items.COD);
    public static final RegistryObject<Item> ETERNAL_SALMON = food("eternal_salmon", Items.SALMON);
    public static final RegistryObject<Item> ETERNAL_TROPICAL_FISH = food("eternal_tropical_fish", Items.TROPICAL_FISH);
    public static final RegistryObject<Item> ETERNAL_PUFFERFISH = food("eternal_pufferfish", Items.PUFFERFISH);
    // 永恒之鱼：由 9 种永恒食物无序合成得到（配方见 ModRecipeProvider），没有对应的原版基础食物，
    // 因此不进入 ETERNAL_TO_BASE（否则会生成"原版食物 + 下界之星"的错误配方）。
    // 基础物品用干海带，仅用于继承"快速进食"动画与音效；食用时长由 food(...).fast() 提供（16 tick）。
    // 营养 20、饱和系数 0.5（实际饱和度 = 20 × 0.5 × 2 = 20）。
    // alwaysEat：与金苹果一致，满饥饿度时仍可食用。
    public static final RegistryObject<Item> ETERNAL_FISH = ITEMS.register("eternal_fish",
            () -> new EternalFoodItem(Items.DRIED_KELP,
                    new Item.Properties().stacksTo(1).rarity(Rarity.RARE)
                            .food(new FoodProperties.Builder().nutrition(20).saturationMod(0.5F).fast()
                                    .alwaysEat().build()),
                    false, "tooltip.eternal_food_mod_1789477527.fish_fast"));
    // 附魔永恒之鱼：永恒之鱼与「有 buff 的食物」在铁砧中合成的产物（见 event/AnvilCombineHandler）。
    // 继承被消耗物品的正面效果（存 NBT），可反复在铁砧中继续合成（同名效果取最高等级与最长时长），
    // 与牛奶在铁砧中合成会退回普通永恒之鱼（清除全部继承效果）。贴图沿用 eternal_fish 并带附魔光效。
    // 没有对应的原版基础食物，因此不进入 ETERNAL_TO_BASE。
    public static final RegistryObject<Item> ETERNAL_ENCHANTED_FISH = ITEMS.register("eternal_enchanted_fish",
            () -> new EternalEnchantedFishItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE)
                    .food(new FoodProperties.Builder().nutrition(20).saturationMod(0.5F).fast()
                            .alwaysEat().build())));
    // 永恒亵渎之鱼：由永恒之鱼 + 永恒腐肉 + 永恒毒马铃薯 + 永恒河豚 + 永恒蜘蛛眼无序合成得到
    //（配方见 ModRecipeProvider），没有对应的原版基础食物，因此不进入 ETERNAL_TO_BASE。
    // 与永恒之鱼同速（fast() + 干海带 base = 16 tick），回复 6 饥饿值、1 点饱食度
    //（原版公式：实际饱和度 = nutrition × saturationMod × 2，故 saturationMod = 1/12），
    // 并附带 30 秒饥饿 XL（amplifier 39）与 30 秒中毒 II（amplifier 1）。
    // alwaysEat：与金苹果一致，满饥饿度时仍可食用。
    // 效果沿用 FoodProperties.getEffects() 由原版食用逻辑自动施加（LivingEntity.addEatEffect）。
    // 描述用红字（hintColor）+ 以其自身 FoodProperties 列出红字 debuff。
    public static final RegistryObject<Item> ETERNAL_BLASPHEMOUS_FISH = ITEMS.register("eternal_blasphemous_fish",
            () -> new EternalFoodItem(Items.DRIED_KELP,
                    new Item.Properties().stacksTo(1).rarity(Rarity.RARE)
                            .food(new FoodProperties.Builder().nutrition(6).saturationMod(1.0F / 12.0F).fast()
                                    .alwaysEat()
                                    .effect(new MobEffectInstance(MobEffects.HUNGER, 600, 39), 1.0F)
                                    .effect(new MobEffectInstance(MobEffects.POISON, 600, 1), 1.0F)
                                    .build()),
                    false, "tooltip.eternal_food_mod_1789477527.blasphemous_curse", ChatFormatting.RED, true));
    public static final RegistryObject<Item> ETERNAL_ROTTEN_FLESH = food("eternal_rotten_flesh", Items.ROTTEN_FLESH);
    public static final RegistryObject<Item> ETERNAL_SPIDER_EYE = food("eternal_spider_eye", Items.SPIDER_EYE);
    // 原版蜂蜜瓶的 use 走 ItemUtils.startUsingInstantly，绕过饥饿检查（满饥饿也能喝）。
    // 直接复制 Foods.HONEY_BOTTLE 不含 alwaysEat，会被 EternalFoodItem 继承的 Item#use
    // 用 canEat 拦下，导致饱食时无法饮用，故补上 alwaysEat（数值仍与 Foods.HONEY_BOTTLE 一致）。
    public static final RegistryObject<Item> ETERNAL_HONEY_BOTTLE = special("eternal_honey_bottle", Items.HONEY_BOTTLE,
            new FoodProperties.Builder().nutrition(6).saturationMod(0.1F).alwaysEat().build(),
            "tooltip.eternal_food_mod_1789477527.honey_poison");
    public static final RegistryObject<Item> ETERNAL_MILK_BUCKET = special("eternal_milk_bucket", Items.MILK_BUCKET,
            new FoodProperties.Builder().alwaysEat().build(),
            "tooltip.eternal_food_mod_1789477527.milk_bucket");
    // 永恒牛奶plus：只清除负面效果。配方为 永恒牛奶桶 + 龙息（返还玻璃瓶），
    // 由手写 recipe JSON 提供（自定义 serializer 以支持容器返还），因此不进入
    // ETERNAL_TO_BASE（否则会被 DataGen 生成"原版食物 + 下界之星"的配方）。
    public static final RegistryObject<Item> ETERNAL_MILK_PLUS = ITEMS.register("eternal_milk_plus",
            () -> new EternalFoodItem(Items.MILK_BUCKET,
                    new Item.Properties().stacksTo(1).rarity(Rarity.RARE)
                            .food(new FoodProperties.Builder().alwaysEat().build()),
                    true, "tooltip.eternal_food_mod_1789477527.milk_plus"));
    // 永恒喷溅型奶瓶 / plus：右键投掷，落点范围清除全部效果 / 仅负面效果，投掷不消耗。
    // 不进入 ETERNAL_TO_BASE（没有"原版食物 + 下界之星"配方），通过酿造台获得，
    // 配方见 recipe/ModBrewingRecipes。
    public static final RegistryObject<Item> ETERNAL_SPLASH_MILK_BOTTLE =
            ITEMS.register("eternal_splash_milk_bottle",
                    () -> new EternalSplashMilkBottleItem(
                            new Item.Properties().stacksTo(1).rarity(Rarity.RARE),
                            "tooltip.eternal_food_mod_1789477527.milk_bucket"));
    public static final RegistryObject<Item> ETERNAL_SPLASH_MILK_BOTTLE_PLUS =
            ITEMS.register("eternal_splash_milk_bottle_plus",
                    () -> new EternalSplashMilkBottleItem(
                            new Item.Properties().stacksTo(1).rarity(Rarity.RARE),
                            "tooltip.eternal_food_mod_1789477527.milk_plus"));

    // 永恒药水：与原版药水完全一致（药水类型存 NBT），区别仅在于使用后不消耗物品。
    // 不进入 ETERNAL_TO_BASE（没有"原版食物 + 下界之星"合成，靠酿造台获得，见 recipe/ModBrewingRecipes）。
    public static final RegistryObject<Item> ETERNAL_POTION = ITEMS.register("eternal_potion",
            () -> new EternalPotionItem(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> ETERNAL_SPLASH_POTION = ITEMS.register("eternal_splash_potion",
            () -> new EternalThrowablePotionItem(new Item.Properties().stacksTo(1), false));
    public static final RegistryObject<Item> ETERNAL_LINGERING_POTION = ITEMS.register("eternal_lingering_potion",
            () -> new EternalThrowablePotionItem(new Item.Properties().stacksTo(1), true));

    // 永恒药箭：与原版药箭完全一致（药水类型存 NBT），区别仅在于作为弓 / 弩 / 发射器的弹药时不消耗。
    // 不进入 ETERNAL_TO_BASE（没有"原版食物 + 下界之星"合成，配方见 ModRecipeProvider），
    // 需加入 minecraft:arrows 物品标签才会被弓 / 弩识别为弹药（见 data/minecraft/tags/items/arrows.json）。
    public static final RegistryObject<Item> ETERNAL_TIPPED_ARROW = ITEMS.register("eternal_tipped_arrow",
            () -> new EternalTippedArrowItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE)));

    // 永恒燃料：熔炉燃料（燃烧时长 int 上限、不消耗）+ 酿造台燃料（右键补充 20 能量）。
    // 纹理固定为 eternal_blaze_powder（见 ModItemModelProvider）。不进入 ETERNAL_TO_BASE，
    // 配方为 烈焰粉 + 干海带块 + 煤炭块 + 木炭 + 烈焰棒 + 岩浆桶 + 下界之星 无序合成（返还空桶）。
    public static final RegistryObject<Item> ETERNAL_FUEL = ITEMS.register("eternal_fuel",
            () -> new EternalFuelItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE)));

    // 永恒附魔之瓶：与原版附魔之瓶完全一致，右键投掷 / 被发射器发射都不消耗。
    // 不进入 ETERNAL_TO_BASE（配方为 附魔之瓶 + 下界之星，见 ModRecipeProvider）；
    // 底部金字说明为「永恒：扔出后不会消耗」。
    public static final RegistryObject<Item> ETERNAL_EXPERIENCE_BOTTLE =
            ITEMS.register("eternal_experience_bottle",
                    () -> new EternalExperienceBottleItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE)));

    // 永恒烟花火箭：右键放置 / 鞘翅加速 / 发射器 / 弩 发射都不消耗。
    // 不进入 ETERNAL_TO_BASE；配方为 成品烟花火箭 + 下界之星，且复制 {@code Fireworks} NBT
    // 从而继承合成前的所有烟花效果（见 EternalFireworkRocketRecipe）。
    public static final RegistryObject<Item> ETERNAL_FIREWORK_ROCKET =
            ITEMS.register("eternal_firework_rocket",
                    () -> new EternalFireworkRocketItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE)));

    // 永恒之星：由下界之星、潮涌核心、嗅探兽蛋、龙息、下界合金锭、龙蛋、附魔金苹果、
    // 紫水晶碎片、回响碎片无序合成（见 ModRecipeProvider）。可无限食用，食用后把玩家当前
    // 所有 buff 与 debuff 的持续时间改为无限（-1）；并可在铁砧中与药水 / 附魔永恒之鱼合成，
    // 把其效果变为无限（见 event/AnvilCombineHandler）。不进入 ETERNAL_TO_BASE。
    public static final RegistryObject<Item> ETERNAL_STAR = ITEMS.register("eternal_star",
            () -> new EternalStarItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)
                    .food(new FoodProperties.Builder().nutrition(0).saturationMod(0.0F).alwaysEat().fast().build())));

    // 永恒满置域：长按 10 秒使用一次且不消耗，只能在主世界使用；蓄力时显示拉弓姿态，完成后
    // 播放不死图腾动画，创造模式飞行时也能用。使用后把玩家所在区块从「玩家下半身所在高度 - 1」
    // 向下到可放置方块最低高度之间的方块替换为石头 / 深板岩（基岩保留）；玩家 y <= 0 时全填深板岩，
    // 否则 y > 0 填石头、y <= 0 填深板岩。配方为 4 石头 + 4 深板岩 + 1 下界之星
    // 无序合成（共 9 个材料，恰好放满 3x3 工作台；因原版合成台单格无法承载整组 64 个，
    // 此处按「4 个石头 + 4 个深板岩 + 1 个下界之星」实现）。不进入 ETERNAL_TO_BASE。
    public static final RegistryObject<Item> ETERNAL_FULL_DOMAIN = ITEMS.register("eternal_full_domain",
            () -> new EternalFullDomainItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE)));

    // 无底坑（eternal_abyss）：食用后获得 8 分钟「饕餮」效果（该效果期间满饥饿 / 满饱和度也能
    // 正常进食），食用不消耗。由 永恒河豚 + 永恒腐肉 + 永恒生鸡肉 无序合成（见 ModRecipeProvider）。
    // 贴图为 abyss.png（见 ModItemModelProvider 特判）。不进入 ETERNAL_TO_BASE
    //（不是「原版食物 + 下界之星」配方）。
    public static final RegistryObject<Item> ETERNAL_ABYSS = ITEMS.register("eternal_abyss",
            () -> new EternalAbyssItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE)
                    .food(new FoodProperties.Builder().nutrition(6).saturationMod(0.5F).fast()
                            .alwaysEat().build())));

    // 永恒弹射物：右键发射对应原版投射物且不消耗，底部金字说明为「永恒：扔出时不消耗」。
    // 除永恒末影珍珠外，其余均可被发射器发射且不消耗（见 event/ModDispenseBehaviors）。
    // 永恒雪球 / 永恒末影珍珠 / 永恒火焰弹 = 下界之星 + 对应材料无序合成；
    // 永恒火球 = 永恒火焰弹 + TNT；永恒凋灵之首 = 下界之星 + 凋灵骷髅头颅；
    // 永恒蓝色凋灵之首 = 永恒凋灵之首 + TNT。都不进入 ETERNAL_TO_BASE（非食物）。
    public static final RegistryObject<Item> ETERNAL_SNOWBALL = projectile("eternal_snowball",
            EternalProjectileItem.Kind.SNOWBALL);
    // 永恒末影珍珠可传送玩家，不扣血也不进入冷却；按需求不支持发射器发射。
    public static final RegistryObject<Item> ETERNAL_ENDER_PEARL = ITEMS.register("eternal_ender_pearl",
            () -> new EternalEnderPearlItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE)));
    // 永恒火焰弹发射的是烈焰人的投射物（小火球），永恒火球发射的是恶魂的投射物（大火球）。
    public static final RegistryObject<Item> ETERNAL_FIRE_CHARGE = projectile("eternal_fire_charge",
            EternalProjectileItem.Kind.SMALL_FIREBALL);
    public static final RegistryObject<Item> ETERNAL_FIREBALL = projectile("eternal_fireball",
            EternalProjectileItem.Kind.LARGE_FIREBALL);
    public static final RegistryObject<Item> ETERNAL_WITHER_SKULL = projectile("eternal_wither_skull",
            EternalProjectileItem.Kind.WITHER_SKULL);
    public static final RegistryObject<Item> ETERNAL_WITHER_SKULL_BLUE = projectile("eternal_wither_skull_blue",
            EternalProjectileItem.Kind.BLUE_WITHER_SKULL);

    // 永恒刷怪蛋：单个物品，通过 NBT（EntityTag.id）记录要生成的生物类型，完全继承原版刷怪蛋行为
    //（方块放置 / 液体生成 / 刷怪笼改类型且不写入附加数据 / 右键成年体生成幼年体 / 发射器发射），
    // 但任何途径都不消耗。纹理分三层渲染，颜色取自对应原版刷怪蛋（见 EternalSpawnEggItem）。
    // 不进入 ETERNAL_TO_BASE（不是食物，无「原版食物 + 下界之星」配方）。
    public static final RegistryObject<Item> ETERNAL_SPAWN_EGG = ITEMS.register("eternal_spawn_egg",
            () -> new EternalSpawnEggItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE)));

    private static RegistryObject<Item> projectile(String name, EternalProjectileItem.Kind kind) {
        return ITEMS.register(name,
                () -> new EternalProjectileItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE), kind));
    }

    private static RegistryObject<Item> food(String name, Item base) {
        return special(name, base, base.getFoodProperties());
    }

    /**
     * 注册会「睡觉」的永恒蘑菇：以原版蘑菇为基础物品加入 {@link #ETERNAL_TO_BASE}
     *（从而拥有「原版蘑菇 + 下界之星」的配方），但食用属性替换为 1 饥饿值 / 1 饱和度 / 快速进食。
     */
    private static RegistryObject<Item> sleepingMushroom(String name, Item base, String hintKey,
            String offhandHintKey, boolean sleepDuringDay) {
        FoodProperties food = new FoodProperties.Builder().nutrition(1).saturationMod(0.5F).fast()
                .alwaysEat().build();
        RegistryObject<Item> registered = ITEMS.register(name, () -> new EternalSleepMushroomItem(base,
                new Item.Properties().stacksTo(1).rarity(Rarity.RARE).food(food), hintKey, offhandHintKey,
                sleepDuringDay));
        ETERNAL_TO_BASE.put(registered, base);
        return registered;
    }

    private static RegistryObject<Item> special(String name, Item base, FoodProperties food) {
        return special(name, base, food, null);
    }

    private static RegistryObject<Item> special(String name, Item base, FoodProperties food, String hintKey) {
        RegistryObject<Item> registered = ITEMS.register(name,
                () -> new EternalFoodItem(base,
                        new Item.Properties().stacksTo(1).rarity(Rarity.RARE).food(food), false, hintKey));
        ETERNAL_TO_BASE.put(registered, base);
        return registered;
    }

    public static void register(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
    }
}
