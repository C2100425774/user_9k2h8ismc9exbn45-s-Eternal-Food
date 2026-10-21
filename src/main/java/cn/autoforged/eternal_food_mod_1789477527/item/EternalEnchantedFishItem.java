package cn.autoforged.eternal_food_mod_1789477527.item;

import com.mojang.datafixers.util.Pair;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SuspiciousStewItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.ForgeHooks;

/**
 * 附魔永恒之鱼：永恒之鱼与「有 buff 的食物」在铁砧中合成的产物（见
 * {@code cn.autoforged.eternal_food_mod_1789477527.event.AnvilCombineHandler}）。
 *
 * <p>继承被消耗物品的正面（{@link MobEffectCategory#BENEFICIAL}）状态效果，效果数据存在
 * {@link #BUFFS_TAG} NBT 列表中，可反复与新的 buff 食物在铁砧中合成，同名效果取最高等级与
 * 最长时长。食用后施加全部存储的效果，且同永恒食物一样不被消耗、无冷却。
 */
public class EternalEnchantedFishItem extends EternalFoodItem {

    /** 存储继承来的状态效果的 NBT 键（{@link MobEffectInstance#save}/{@link MobEffectInstance#load} 格式的列表）。 */
    public static final String BUFFS_TAG = "eternal_buffs";

    public EternalEnchantedFishItem(Item.Properties properties) {
        // 基础物品用干海带：继承「快速进食」动画与音效，且不会被基类当作需要特殊处理的食材。
        super(Items.DRIED_KELP, properties, false, null);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        ItemStack result = super.finishUsingItem(stack, level, entity);
        if (!level.isClientSide) {
            for (MobEffectInstance instance : getBuffs(stack)) {
                entity.addEffect(new MobEffectInstance(instance));
            }
        }
        return result;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        // 附魔永恒之鱼的贴图带附魔光效，与永恒之鱼区分。
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltipComponents, TooltipFlag isAdvanced) {
        // 先列出继承来的 buff，再交给基类追加「永恒：食用后不会消耗」等说明。
        for (MobEffectInstance instance : getBuffs(stack)) {
            tooltipComponents.add(describeEffect(instance));
        }
        tooltipComponents.add(Component.translatable("tooltip.eternal_food_mod_1789477527.enchanted_fish")
                .withStyle(ChatFormatting.LIGHT_PURPLE));
        super.appendHoverText(stack, level, tooltipComponents, isAdvanced);
    }

    /** 读取物品 NBT 中存储的继承效果。 */
    public static List<MobEffectInstance> getBuffs(ItemStack stack) {
        List<MobEffectInstance> buffs = new ArrayList<>();
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains(BUFFS_TAG, 9)) {
            ListTag list = tag.getList(BUFFS_TAG, 10);
            for (int i = 0; i < list.size(); i++) {
                MobEffectInstance instance = MobEffectInstance.load(list.getCompound(i));
                if (instance != null) {
                    buffs.add(instance);
                }
            }
        }
        return buffs;
    }

    /** 把继承效果写入物品 NBT；{@code buffs} 为空时移除该 NBT 键（供牛奶清除使用）。 */
    public static void setBuffs(ItemStack stack, List<MobEffectInstance> buffs) {
        if (buffs.isEmpty()) {
            CompoundTag tag = stack.getTag();
            if (tag != null) {
                tag.remove(BUFFS_TAG);
            }
            return;
        }
        ListTag list = new ListTag();
        for (MobEffectInstance instance : buffs) {
            list.add(instance.save(new CompoundTag()));
        }
        stack.getOrCreateTag().put(BUFFS_TAG, list);
    }

    /**
     * 收集一个物品能提供的全部状态效果：已存储的继承效果、食物自带效果、药水效果、
     * 以及迷之炖菜的 NBT 效果。buff 与 debuff 都会被继承（提示框再按类别着色）。
     */
    public static List<MobEffectInstance> collectBuffs(ItemStack stack) {
        List<MobEffectInstance> buffs = new ArrayList<>(getBuffs(stack));

        FoodProperties food = stack.getFoodProperties(null);
        if (food != null) {
            for (Pair<MobEffectInstance, Float> effect : food.getEffects()) {
                addEffect(buffs, effect.getFirst());
            }
        }

        for (MobEffectInstance instance : PotionUtils.getMobEffects(stack)) {
            addEffect(buffs, instance);
        }

        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains(SuspiciousStewItem.EFFECTS_TAG, 9)) {
            ListTag list = tag.getList(SuspiciousStewItem.EFFECTS_TAG, 10);
            for (int i = 0; i < list.size(); i++) {
                CompoundTag entry = list.getCompound(i);
                int duration = entry.contains(SuspiciousStewItem.EFFECT_DURATION_TAG, 99)
                        ? entry.getInt(SuspiciousStewItem.EFFECT_DURATION_TAG)
                        : SuspiciousStewItem.DEFAULT_DURATION;
                MobEffect effect = MobEffect.byId(entry.getInt(SuspiciousStewItem.EFFECT_ID_TAG));
                effect = ForgeHooks.loadMobEffect(entry, "forge:effect_id", effect);
                if (effect != null) {
                    addEffect(buffs, new MobEffectInstance(effect, duration));
                }
            }
        }
        return buffs;
    }

    /** 合并两组效果：同名效果取最高等级与最长时长（例如 1:30 力量 II + 8:00 力量 I → 8:00 力量 II）。 */
    public static List<MobEffectInstance> mergeBuffs(List<MobEffectInstance> first, List<MobEffectInstance> second) {
        Map<MobEffect, MobEffectInstance> merged = new LinkedHashMap<>();
        for (MobEffectInstance instance : first) {
            mergeInto(merged, instance);
        }
        for (MobEffectInstance instance : second) {
            mergeInto(merged, instance);
        }
        return new ArrayList<>(merged.values());
    }

    private static void mergeInto(Map<MobEffect, MobEffectInstance> merged, MobEffectInstance instance) {
        MobEffect effect = instance.getEffect();
        MobEffectInstance existing = merged.get(effect);
        if (existing == null) {
            merged.put(effect, new MobEffectInstance(instance));
            return;
        }
        int amplifier = Math.max(existing.getAmplifier(), instance.getAmplifier());
        int duration = Math.max(existing.getDuration(), instance.getDuration());
        merged.put(effect, new MobEffectInstance(effect, duration, amplifier));
    }

    private static void addEffect(List<MobEffectInstance> buffs, MobEffectInstance instance) {
        if (instance != null) {
            buffs.add(instance);
        }
    }

    /** 返回移除了全部 {@link MobEffectCategory#HARMFUL} 效果后的列表（供「只清除 debuff」的合成使用）。 */
    public static List<MobEffectInstance> removeDebuffs(List<MobEffectInstance> buffs) {
        List<MobEffectInstance> kept = new ArrayList<>();
        for (MobEffectInstance instance : buffs) {
            if (instance.getEffect().getCategory() != MobEffectCategory.HARMFUL) {
                kept.add(instance);
            }
        }
        return kept;
    }
}
