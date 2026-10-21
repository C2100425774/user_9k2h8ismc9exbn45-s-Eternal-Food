package cn.autoforged.eternal_food_mod_1789477527.event;

import cn.autoforged.eternal_food_mod_1789477527.EternalFoodMod;
import cn.autoforged.eternal_food_mod_1789477527.item.EternalFoodItem;
import cn.autoforged.eternal_food_mod_1789477527.item.ModItems;
import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.animal.Ocelot;
import net.minecraft.world.entity.animal.Parrot;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.monster.ZombieVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 让永恒食物复用原版食物的「对生物右键」与「吸引生物」逻辑。
 *
 * <p>原版这些判定都直接对比原版物品（{@code Pig.isFood} 用
 * {@code Ingredient.of(Items.CARROT, ...)}、{@code AbstractHorse.handleEating} 用
 * {@code ItemStack#is(原版物品)}、{@code Parrot.mobInteract} 用 {@code is(Items.COOKIE)}、
 * {@code ZombieVillager.mobInteract} 用 {@code is(Items.GOLDEN_APPLE)}），永恒食物不在其中，
 * 所以不会被识别。这里做两件事：
 *
 * <ul>
 *   <li>右键生物：在 {@link PlayerInteractEvent.EntityInteract}（原版 {@code entity.interact}
 *       之前触发）里把手中临时替换成对应的原版食物、调用 {@code target.interact} 复用原版全部逻辑，
 *       再还原永恒食物并取消事件，避免原版再跑一遍。相比在 {@code interactLivingEntity}
 *       里兜底，这样能覆盖「原版 entity.interact 已经消耗掉动作」的情况（野生马
 *       {@code makeMad}、已驯服鹦鹉切换坐下等）。</li>
 *   <li>吸引：原版猪 / 马 / 猫 / 豹猫的 {@link TemptGoal} 用的是硬编码 {@code Ingredient}
 *       （不是物品标签），永恒食物永远匹配不上。给这些生物额外挂一个引用永恒食物的
 *       {@link TemptGoal}。</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = EternalFoodMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class EternalInteractionHandler {

    /** 已挂过额外 TemptGoal 的生物（弱引用，区块重载生成新实例时会重新挂）。 */
    private static final Set<Mob> TEMPT_ADDED =
            Collections.newSetFromMap(new WeakHashMap<>());

    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        ItemStack stack = event.getItemStack();
        if (!(stack.getItem() instanceof EternalFoodItem eternal)) {
            return;
        }
        Entity target = event.getTarget();
        Item swap = vanillaEquivalent(target, eternal.getBaseItem());
        if (swap == null) {
            // 该生物本来就不吃这种原版食物，交给原版处理。
            return;
        }

        Player player = event.getEntity();
        InteractionHand hand = event.getHand();
        ItemStack held = player.getItemInHand(hand);

        // 狼：原版喂食只回 1 倍 nutrition，需求要求回 2 倍（= 该永恒食物给玩家恢复的饥饿值）。
        // 先记下额外要补的一份，等原版 interact 跑完（已回 1 倍）后再补 1 倍。
        // 仅服务端、仅已驯服的受伤狼，避免客户端预测抖动与治疗未驯服/满血个体。
        int wolfExtraHeal = 0;
        if (target instanceof Wolf wolf && wolf.isTame() && !wolf.level().isClientSide
                && wolf.getHealth() < wolf.getMaxHealth()) {
            FoodProperties food = held.getFoodProperties(wolf);
            if (food != null) {
                wolfExtraHeal = food.getNutrition();
            }
        }

        ItemStack vanillaStack = new ItemStack(swap, held.getCount());
        player.setItemInHand(hand, vanillaStack);

        InteractionResult result;
        try {
            result = target.interact(player, hand);
        } finally {
            player.setItemInHand(hand, held);
        }

        if (wolfExtraHeal > 0 && target instanceof Wolf wolf) {
            wolf.heal(wolfExtraHeal);
        }

        if (result.consumesAction()) {
            event.setCancellationResult(result);
            event.setCanceled(true);
        }
    }

    /**
     * 把永恒食物映射成对该生物真正有效的原版食物；返回 {@code null} 表示不做替换。
     */
    private static Item vanillaEquivalent(Entity target, Item base) {
        if (target instanceof Wolf) {
            // 原版狼只吃 isEdible() && isMeat() 的食物；永恒肉类与其原版对应物一致。
            return isWolfFood(base) ? base : null;
        }
        if (target instanceof Cat || target instanceof Ocelot) {
            // 原版猫 / 豹猫只用生鳕鱼 / 生鲑鱼吸引、驯服 / 获取信任与繁殖（熟鱼无效）。
            return (base == Items.COD || base == Items.SALMON) ? base : null;
        }
        if (target instanceof Pig) {
            // 原版猪只吃 胡萝卜 / 马铃薯 / 甜菜根。金胡萝卜不参与（用户明确：猪不吃金胡萝卜）。
            return (base == Items.CARROT || base == Items.BEETROOT || base == Items.POTATO) ? base : null;
        }
        if (target instanceof AbstractHorse) {
            // 马 / 驴 / 骡：永恒胡萝卜按苹果处理（+3 Temper）；金胡萝卜、金苹果、附魔金苹果
            // 都有各自的原版效果（繁殖 / +10 Temper），直接按原版对应物处理。
            if (base == Items.CARROT) {
                return Items.APPLE;
            }
            if (base == Items.APPLE || base == Items.GOLDEN_CARROT
                    || base == Items.GOLDEN_APPLE || base == Items.ENCHANTED_GOLDEN_APPLE) {
                return base;
            }
            return null;
        }
        if (target instanceof Parrot) {
            // 原版鹦鹉：曲奇 = 毒死（POISONOUS_FOOD）。
            return base == Items.COOKIE ? base : null;
        }
        if (target instanceof ZombieVillager) {
            // 原版僵尸村民：金苹果 + 虚弱 = 治愈。
            return base == Items.GOLDEN_APPLE ? base : null;
        }
        return null;
    }

    /** 原版 {@code Wolf.isFood}：可食用且为肉类。 */
    private static boolean isWolfFood(Item base) {
        FoodProperties food = base.getFoodProperties();
        return base.isEdible() && food != null && food.isMeat();
    }

    /**
     * 永恒燃料作为酿造台燃料：对着酿造台右键时补满 20 能量（物品不消耗）。
     *
     * <p>原版 {@code BrewingStandBlockEntity} 的燃料判定与燃料槽校验硬编码为烈焰粉
     * （{@code itemstack.is(Items.BLAZE_POWDER)}），永恒燃料无法放入燃料槽，故通过右键交互
     * 直接补充能量；{@code fuel} 字段已由 Access Transformer 开放。
     */
    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }
        ItemStack stack = event.getItemStack();
        if (!stack.is(ModItems.ETERNAL_FUEL.get())) {
            return;
        }
        Level level = event.getLevel();
        BlockPos pos = event.getPos();
        BlockState state = level.getBlockState(pos);
        if (!state.is(Blocks.BREWING_STAND)) {
            return;
        }
        if (!(level.getBlockEntity(pos) instanceof BrewingStandBlockEntity stand)) {
            return;
        }
        if (level.isClientSide) {
            return;
        }
        // 与烈焰粉一致：一份就是 20 能量（BrewingStandBlockEntity.FUEL_USES）。
        stand.fuel = 20;
        stand.setChanged();
    }

    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (!(event.getEntity() instanceof PathfinderMob mob)) {
            return;
        }
        Ingredient ingredient = temptationIngredient(mob);
        if (ingredient == null || !TEMPT_ADDED.add(mob)) {
            return;
        }
        // 优先级 / 速度 / 惊吓语义尽量对齐原版 TemptGoal：
        // 猫 priority 4 / 速度 0.6 / 会惊吓，豹猫 priority 3 / 速度 0.6，猪与马科沿用既有值。
        int priority;
        double speed;
        boolean scare;
        if (mob instanceof Cat) {
            priority = 4;
            speed = 0.6D;
            scare = true;
        } else if (mob instanceof Ocelot) {
            priority = 3;
            speed = 0.6D;
            scare = true;
        } else if (mob instanceof AbstractHorse) {
            priority = 4;
            speed = 1.2D;
            scare = false;
        } else {
            priority = 5;
            speed = 1.2D;
            scare = false;
        }
        mob.goalSelector.addGoal(priority, new EternalTemptGoal(mob, speed, ingredient, scare));
    }

    /** 该生物会被哪些永恒食物吸引；返回 {@code null} 表示不需要额外 TemptGoal。 */
    private static Ingredient temptationIngredient(PathfinderMob mob) {
        if (mob instanceof Cat || mob instanceof Ocelot) {
            return Ingredient.of(ModItems.ETERNAL_COD.get(), ModItems.ETERNAL_SALMON.get());
        }
        if (mob instanceof Pig) {
            return Ingredient.of(ModItems.ETERNAL_CARROT.get(), ModItems.ETERNAL_BEETROOT.get(),
                    ModItems.ETERNAL_POTATO.get());
        }
        if (mob instanceof AbstractHorse) {
            return Ingredient.of(ModItems.ETERNAL_GOLDEN_CARROT.get(),
                    ModItems.ETERNAL_GOLDEN_APPLE.get(), ModItems.ETERNAL_ENCHANTED_GOLDEN_APPLE.get());
        }
        return null;
    }

    /**
     * 让永恒食物的 TemptGoal 保持原版语义：未驯服的猫才被吸引（{@code Cat.CatTemptGoal}）。
     * 原版该内部类不可访问，故自建。豹猫的 {@code isTrusting()} 为包级私有，无法从外部读取，
     * 因此不复刻 {@code Ocelot.OcelotTemptGoal.canScare} 的信任细节，沿用 {@link TemptGoal} 默认行为。
     */
    private static final class EternalTemptGoal extends TemptGoal {

        private final Cat cat;

        private EternalTemptGoal(PathfinderMob mob, double speed, Ingredient items, boolean canScare) {
            super(mob, speed, items, canScare);
            this.cat = mob instanceof Cat c ? c : null;
        }

        @Override
        public boolean canUse() {
            return (this.cat == null || !this.cat.isTame()) && super.canUse();
        }
    }
}
