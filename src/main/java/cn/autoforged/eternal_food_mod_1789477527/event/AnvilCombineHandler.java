package cn.autoforged.eternal_food_mod_1789477527.event;

import cn.autoforged.eternal_food_mod_1789477527.EternalFoodMod;
import cn.autoforged.eternal_food_mod_1789477527.item.EternalEnchantedFishItem;
import cn.autoforged.eternal_food_mod_1789477527.item.ModItems;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.PotionItem;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraftforge.event.AnvilUpdateEvent;
import net.minecraftforge.event.entity.player.AnvilRepairEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 永恒之鱼 / 附魔永恒之鱼的铁砧合成：
 *
 * <ul>
 *   <li>永恒之鱼 + 任意「有效果的食物」→ 附魔永恒之鱼，继承该食物的效果（buff 与 debuff 都继承）；</li>
 *   <li>附魔永恒之鱼 + 任意「有效果的食物」→ 附魔永恒之鱼，同名效果取最高等级与最长时长；</li>
 *   <li>附魔永恒之鱼 + 普通奶桶 / 永恒奶桶 / 永恒喷溅型奶瓶 → 永恒之鱼，清除全部继承效果；</li>
 *   <li>附魔永恒之鱼 + 永恒奶桶plus / 永恒喷溅型奶瓶plus → 只清除继承的 debuff；</li>
 *   <li>永恒之星 + 药水（普通 / 喷溅 / 滞留，原版或永恒）、永恒药箭或附魔永恒之鱼 → 对应物品，
 *       其全部 buff 与 debuff 的持续时间改为无限（-1）；药水 / 药箭同时保留原有颜色；</li>
 *   <li>以上操作均只消耗 1 级经验、无经验惩罚；铁砧破损沿用原版（{@code AnvilMenu#onTake}）。</li>
 * </ul>
 *
 * <p>取件时（{@link AnvilRepairEvent}）把被消耗的永恒之星 / 永恒奶制品重新放入玩家背包，
 * 因此这些材料实际不会被消耗（合成本身仍照常扣 1 级经验、照常触发铁砧破损）。
 */
@Mod.EventBusSubscriber(modid = EternalFoodMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class AnvilCombineHandler {

    @SubscribeEvent
    public static void onAnvilUpdate(AnvilUpdateEvent event) {
        ItemStack left = event.getLeft();
        ItemStack right = event.getRight();
        if (left.isEmpty() || right.isEmpty()) {
            return;
        }

        // 永恒之星 + 「有药水标签的物品」（药水 / 永恒药水 / 喷溅 / 滞留变体）/ 永恒药箭
        // 或附魔永恒之鱼：把其全部 buff 与 debuff 的持续时间改为无限（-1）。其他带 buff 的
        // 食物（亵渎之鱼、迷之炖菜等）不属于药水类，因此不会被识别。永恒之星本身在取件时返还
        // （见 onAnvilRepair）。
        boolean leftStar = left.is(ModItems.ETERNAL_STAR.get());
        boolean rightStar = right.is(ModItems.ETERNAL_STAR.get());
        if (leftStar != rightStar) {
            ItemStack other = leftStar ? right : left;
            ItemStack infinite = null;
            if (other.getItem() instanceof PotionItem) {
                infinite = makePotionInfinite(other);
            } else if (other.is(ModItems.ETERNAL_TIPPED_ARROW.get())) {
                infinite = makePotionInfinite(other);
            } else if (other.is(ModItems.ETERNAL_ENCHANTED_FISH.get())) {
                infinite = makeFishInfinite(other);
            }
            if (infinite != null) {
                acceptOutput(event, infinite);
                return;
            }
        }

        boolean leftIsFish = isFish(left);
        boolean rightIsFish = isFish(right);
        if (!leftIsFish && !rightIsFish) {
            return;
        }

        ItemStack fish = leftIsFish ? left : right;
        ItemStack other = leftIsFish ? right : left;

        // 附魔永恒之鱼 + 牛奶：普通奶桶 / 永恒奶桶 / 永恒喷溅型奶瓶清除全部继承效果并退回永恒之鱼；
        // 永恒奶桶plus / 永恒喷溅型奶瓶plus 只清除继承的 debuff，保留 buff。
        if (fish.is(ModItems.ETERNAL_ENCHANTED_FISH.get())) {
            if (isClearAllMilk(other)) {
                acceptOutput(event, new ItemStack(ModItems.ETERNAL_FISH.get()));
                return;
            }
            if (isDebuffOnlyMilk(other)) {
                List<MobEffectInstance> kept =
                        EternalEnchantedFishItem.removeDebuffs(EternalEnchantedFishItem.getBuffs(fish));
                ItemStack output = new ItemStack(ModItems.ETERNAL_ENCHANTED_FISH.get());
                EternalEnchantedFishItem.setBuffs(output, kept);
                acceptOutput(event, output);
                return;
            }
        }

        // 合并已有继承效果与另一物品提供的效果（buff 与 debuff 都继承）；
        // 没有可继承的效果时交还原版处理。
        List<MobEffectInstance> merged = EternalEnchantedFishItem.mergeBuffs(
                EternalEnchantedFishItem.getBuffs(fish),
                EternalEnchantedFishItem.collectBuffs(other));
        if (merged.isEmpty()) {
            return;
        }

        ItemStack output = new ItemStack(ModItems.ETERNAL_ENCHANTED_FISH.get());
        EternalEnchantedFishItem.setBuffs(output, merged);
        acceptOutput(event, output);
    }

    /** 只消耗 1 级经验（cost 1）、右侧材料仅消耗 1 个（materialCost 1），因此不会触发经验惩罚。 */
    private static void acceptOutput(AnvilUpdateEvent event, ItemStack output) {
        event.setOutput(output);
        event.setCost(1);
        event.setMaterialCost(1);
    }

    private static boolean isFish(ItemStack stack) {
        return stack.is(ModItems.ETERNAL_FISH.get()) || stack.is(ModItems.ETERNAL_ENCHANTED_FISH.get());
    }

    /** 普通奶桶 / 永恒奶桶 / 永恒喷溅型奶瓶：清除全部继承效果，附魔永恒之鱼退回永恒之鱼。 */
    private static boolean isClearAllMilk(ItemStack stack) {
        return stack.is(Items.MILK_BUCKET)
                || stack.is(ModItems.ETERNAL_MILK_BUCKET.get())
                || stack.is(ModItems.ETERNAL_SPLASH_MILK_BOTTLE.get());
    }

    /**
     * 只清除继承 debuff 的奶制品：永恒奶桶plus、永恒喷溅型奶瓶plus。
     * 用户明确要求「永恒喷溅型奶瓶plus」才只去除 debuff（基础喷溅奶瓶按「清空全部」处理）。
     */
    private static boolean isDebuffOnlyMilk(ItemStack stack) {
        return stack.is(ModItems.ETERNAL_MILK_PLUS.get())
                || stack.is(ModItems.ETERNAL_SPLASH_MILK_BOTTLE_PLUS.get());
    }

    /**
     * 把药水 / 永恒药箭（普通 / 喷溅 / 滞留，原版或永恒）的全部效果改为无限持续。原版药水 /
     * 药箭的时长由 {@code Potion} 类型定义、无法直接修改，故把所有效果（含自定义效果）取出后
     * 清空 {@code Potion} / {@code CustomPotionEffects}，再以 {@code CustomPotionEffects}
     * 写回无限持续版本。瞬间效果没有时长概念，保持原样。
     *
     * <p>清空 {@code Potion} 后 {@link PotionUtils#getColor} 会退化为空瓶色（见
     * PotionUtils.java:79），故这里先把原色算出并以 {@code CustomPotionColor} 写回，保证
     * 药水 / 药箭的颜色不丢失（{@code getColor} 优先返回 {@code CustomPotionColor}）。
     *
     * @return 无限持续且保留原色的药水 / 药箭栈；没有任何效果（如水瓶）时返回 null（表示不合成）。
     */
    private static ItemStack makePotionInfinite(ItemStack potion) {
        List<MobEffectInstance> effects = PotionUtils.getMobEffects(potion);
        if (effects.isEmpty()) {
            return null;
        }
        // 清空 TAG_POTION 前先记录原色，避免渲染回退为空瓶色。
        int color = PotionUtils.getColor(potion);
        ItemStack output = potion.copy();
        output.setCount(1);
        CompoundTag tag = output.getTag();
        if (tag != null) {
            tag.remove(PotionUtils.TAG_POTION);
            tag.remove(PotionUtils.TAG_CUSTOM_POTION_EFFECTS);
        }
        ListTag list = new ListTag();
        for (MobEffectInstance instance : effects) {
            list.add(toInfinite(instance).save(new CompoundTag()));
        }
        output.getOrCreateTag().put(PotionUtils.TAG_CUSTOM_POTION_EFFECTS, list);
        output.getOrCreateTag().putInt(PotionUtils.TAG_CUSTOM_POTION_COLOR, color);
        return output;
    }

    /**
     * 把附魔永恒之鱼存储的全部继承效果改为无限持续。
     *
     * @return 效果无限持续的附魔永恒之鱼栈；没有任何继承效果时返回 null（表示不合成）。
     */
    private static ItemStack makeFishInfinite(ItemStack fish) {
        List<MobEffectInstance> effects = EternalEnchantedFishItem.getBuffs(fish);
        if (effects.isEmpty()) {
            return null;
        }
        List<MobEffectInstance> infinite = new ArrayList<>();
        for (MobEffectInstance instance : effects) {
            infinite.add(toInfinite(instance));
        }
        ItemStack output = fish.copy();
        output.setCount(1);
        EternalEnchantedFishItem.setBuffs(output, infinite);
        return output;
    }

    /** 复制一个效果实例并把时长改为无限（-1）；瞬间效果无时长概念，保持原时长。 */
    private static MobEffectInstance toInfinite(MobEffectInstance instance) {
        int duration = instance.getEffect().isInstantenous()
                ? instance.getDuration()
                : MobEffectInstance.INFINITE_DURATION;
        return new MobEffectInstance(instance.getEffect(), duration, instance.getAmplifier(),
                instance.isAmbient(), instance.isVisible(), instance.showIcon());
    }

    /**
     * 铁砧取件时返还被消耗的「永恒之星」/ 永恒奶制品，实现「不消耗」。
     *
     * <p>需求要求合成本身照常消耗 1 级经验、照常引发铁砧破损，只是把作为材料消耗掉的
     * 永恒之星 / 永恒喷溅型奶瓶（含 plus）/ 永恒奶桶（含 plus）重新添加回玩家背包。
     */
    @SubscribeEvent
    public static void onAnvilRepair(AnvilRepairEvent event) {
        Player player = event.getEntity();
        // onTake 在客户端也会被调用以做本地预测，只在服务端真正发放返还物。
        if (player.level().isClientSide) {
            return;
        }
        ItemStack refund = refundOf(event.getLeft(), event.getRight());
        if (refund.isEmpty()) {
            return;
        }
        if (!player.getInventory().add(refund)) {
            player.drop(refund, false);
        }
    }

    /** 根据铁砧两个输入槽判断应返还的物品：永恒之星，或被消耗的永恒奶制品。 */
    private static ItemStack refundOf(ItemStack left, ItemStack right) {
        boolean leftStar = left.is(ModItems.ETERNAL_STAR.get());
        boolean rightStar = right.is(ModItems.ETERNAL_STAR.get());
        if (leftStar != rightStar) {
            ItemStack other = leftStar ? right : left;
            if (other.getItem() instanceof PotionItem
                    || other.is(ModItems.ETERNAL_TIPPED_ARROW.get())
                    || other.is(ModItems.ETERNAL_ENCHANTED_FISH.get())) {
                return new ItemStack(ModItems.ETERNAL_STAR.get());
            }
        }

        boolean leftFish = left.is(ModItems.ETERNAL_ENCHANTED_FISH.get());
        boolean rightFish = right.is(ModItems.ETERNAL_ENCHANTED_FISH.get());
        if (leftFish != rightFish) {
            ItemStack other = leftFish ? right : left;
            if (isEternalMilk(other)) {
                ItemStack refund = other.copy();
                refund.setCount(1);
                return refund;
            }
        }
        return ItemStack.EMPTY;
    }

    /** 需要「与附魔永恒之鱼合成后返还」的永恒奶制品（用户列出的四种）。 */
    private static boolean isEternalMilk(ItemStack stack) {
        return stack.is(ModItems.ETERNAL_MILK_BUCKET.get())
                || stack.is(ModItems.ETERNAL_MILK_PLUS.get())
                || stack.is(ModItems.ETERNAL_SPLASH_MILK_BOTTLE.get())
                || stack.is(ModItems.ETERNAL_SPLASH_MILK_BOTTLE_PLUS.get());
    }
}
