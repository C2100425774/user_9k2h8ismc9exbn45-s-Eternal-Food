package cn.autoforged.eternal_food_mod_1789477527.item;

import com.mojang.datafixers.util.Pair;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffectUtil;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Fox;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SuspiciousStewItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.event.entity.EntityTeleportEvent;

/**
 * 永恒食物：可无限次食用而不被消耗。
 *
 * <p>食用完成后把物品数量重置回 1，没有冷却，可立即再次食用。食用效果
 * （饥饿值 / 饱和度 / 状态效果 / 特殊处理）完全沿用原版食物本身，因此与原版一致。
 *
 * <p>对生物右键时复用原版食物的实体交互（猪 / 马 / 驴 / 骡繁殖与喂食、僵尸村民治愈、
 * 鹦鹉吃曲奇中毒等）以及吸引生物，详见
 * {@code cn.autoforged.eternal_food_mod_1789477527.event.EternalInteractionHandler}。
 */
public class EternalFoodItem extends Item {

    private final Item baseItem;
    private final UseAnim useAnimation;
    private final int useDuration;
    /** true = 牛奶变体只清除负面效果（永恒牛奶plus），false = 正常行为。 */
    private final boolean debuffOnly;
    /** 提示框额外说明的翻译键，null = 无。 */
    private final String hintKey;
    /** 提示框说明文字的颜色，默认蓝色。 */
    private final ChatFormatting hintColor;
    /** true = 用本物品自身 FoodProperties 列出效果（含红字 debuff），而非按 baseItem 分支。 */
    private final boolean showOwnFoodEffects;

    public EternalFoodItem(Item baseItem, Properties properties) {
        this(baseItem, properties, false, null);
    }

    public EternalFoodItem(Item baseItem, Properties properties, boolean debuffOnly) {
        this(baseItem, properties, debuffOnly, null);
    }

    public EternalFoodItem(Item baseItem, Properties properties, boolean debuffOnly, String hintKey) {
        this(baseItem, properties, debuffOnly, hintKey, ChatFormatting.BLUE, false);
    }

    public EternalFoodItem(Item baseItem, Properties properties, boolean debuffOnly, String hintKey,
            ChatFormatting hintColor, boolean showOwnFoodEffects) {
        super(properties);
        this.baseItem = baseItem;
        this.debuffOnly = debuffOnly;
        this.hintKey = hintKey;
        this.hintColor = hintColor;
        this.showOwnFoodEffects = showOwnFoodEffects;

        ItemStack baseStack = new ItemStack(baseItem);
        UseAnim animation = baseItem.getUseAnimation(baseStack);
        int duration = baseItem.getUseDuration(baseStack);
        // 蛋糕等原版不可直接食用，回退到标准进食动画/时长。
        this.useAnimation = animation == UseAnim.NONE ? UseAnim.EAT : animation;
        this.useDuration = duration <= 0 ? 32 : duration;
    }

    public Item getBaseItem() {
        return baseItem;
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        // 沿用原版逻辑：加饥饿值 / 饱和度 / 状态效果 / 播放音效 / 触发统计。
        ItemStack result = super.finishUsingItem(stack, level, entity);

        if (!level.isClientSide) {
            if (baseItem == net.minecraft.world.item.Items.MILK_BUCKET) {
                // 不能用 curePotionEffects：它逐个效果按 curativeItems（默认只有 minecraft:milk_bucket）
                // 比对物品，永恒牛奶桶不是原版奶桶，永远匹配不上、等于没有效果。
                // 直接清空全部效果，与原版奶桶一致，并同样派发 MobEffectEvent.Remove 以兼容其他模组。
                if (debuffOnly) {
                    // 永恒牛奶plus：只清除 HARMFUL 效果，保留正面 / 中性效果。
                    // 先复制集合，避免遍历时 removeEffect 修改 activeEffects 抛 ConcurrentModificationException。
                    for (MobEffectInstance instance : List.copyOf(entity.getActiveEffects())) {
                        if (instance.getEffect().getCategory() == MobEffectCategory.HARMFUL) {
                            entity.removeEffect(instance.getEffect());
                        }
                    }
                } else {
                    entity.removeAllEffects();
                }
            } else if (baseItem == net.minecraft.world.item.Items.HONEY_BOTTLE) {
                entity.removeEffect(MobEffects.POISON);
            } else if (baseItem == net.minecraft.world.item.Items.SUSPICIOUS_STEW) {
                // 迷之炖菜的效果存在 NBT 里（合成时由花朵决定），必须手动读取并施加，
                // 才能"继承合成时会赋予玩家的 buff"。
                applyStewEffects(stack, entity);
            } else if (baseItem == net.minecraft.world.item.Items.CHORUS_FRUIT) {
                // 原版紫颂果食用后随机传送，逻辑在 ChorusFruitItem.finishUsingItem 里而非
                // FoodProperties，故此处复刻，保持"食用效果沿用原版"。
                // 传送成功的同时让客户端用本物品播放一次不死图腾激活动画（见 ModNetwork），
                // 否则原版进食动画结束后容易误以为已经吃完。
                if (teleportLikeChorusFruit(level, entity)) {
                    playTotemAnimation(level, entity, stack);
                }
            }
        }

        // 永恒特性：不消耗物品，数量始终维持 1 个。
        result.setCount(1);
        return result;
    }

    /** 读取迷之炖菜 NBT 中的效果并施加到实体上，逻辑与原版 {@link SuspiciousStewItem} 一致。 */
    private static void applyStewEffects(ItemStack stack, LivingEntity entity) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(SuspiciousStewItem.EFFECTS_TAG, 9)) {
            return;
        }
        ListTag list = tag.getList(SuspiciousStewItem.EFFECTS_TAG, 10);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            int duration = entry.contains(SuspiciousStewItem.EFFECT_DURATION_TAG, 99)
                    ? entry.getInt(SuspiciousStewItem.EFFECT_DURATION_TAG)
                    : SuspiciousStewItem.DEFAULT_DURATION;
            MobEffect effect = MobEffect.byId(entry.getInt(SuspiciousStewItem.EFFECT_ID_TAG));
            effect = ForgeHooks.loadMobEffect(entry, "forge:effect_id", effect);
            if (effect != null) {
                entity.addEffect(new MobEffectInstance(effect, duration));
            }
        }
    }

    /**
     * 复刻原版 {@link net.minecraft.world.item.ChorusFruitItem} 的随机传送（仅服务端调用）。
     *
     * @return 是否成功传送（16 次尝试全部失败或被事件取消时为 false）。
     */
    private boolean teleportLikeChorusFruit(Level level, LivingEntity entity) {
        double d0 = entity.getX();
        double d1 = entity.getY();
        double d2 = entity.getZ();
        for (int i = 0; i < 16; i++) {
            double d3 = entity.getX() + (entity.getRandom().nextDouble() - 0.5D) * 16.0D;
            double d4 = Mth.clamp(entity.getY() + (double) (entity.getRandom().nextInt(16) - 8),
                    level.getMinBuildHeight(),
                    level.getMinBuildHeight() + ((ServerLevel) level).getLogicalHeight() - 1);
            double d5 = entity.getZ() + (entity.getRandom().nextDouble() - 0.5D) * 16.0D;
            if (entity.isPassenger()) {
                entity.stopRiding();
            }
            Vec3 vec3 = entity.position();
            level.gameEvent(GameEvent.TELEPORT, vec3, GameEvent.Context.of(entity));
            EntityTeleportEvent.ChorusFruit event = ForgeEventFactory.onChorusFruitTeleport(entity, d3, d4, d5);
            if (event.isCanceled()) {
                return false;
            }
            if (entity.randomTeleport(event.getTargetX(), event.getTargetY(), event.getTargetZ(), true)) {
                SoundEvent sound = entity instanceof Fox ? SoundEvents.FOX_TELEPORT : SoundEvents.CHORUS_FRUIT_TELEPORT;
                level.playSound(null, d0, d1, d2, sound, SoundSource.PLAYERS, 1.0F, 1.0F);
                entity.playSound(sound, 1.0F, 1.0F);
                return true;
            }
        }
        return false;
    }

    /**
     * 让 {@code entity}（必须是服务端玩家）的客户端用 {@code stack} 播放一次不死图腾激活动画。
     * 该动画由原版实体事件 35 提供视觉效果，但原版固定用图腾贴图，故改走自定义网络包。
     */
    private static void playTotemAnimation(Level level, LivingEntity entity, ItemStack stack) {
        if (!level.isClientSide && entity instanceof net.minecraft.server.level.ServerPlayer player) {
            cn.autoforged.eternal_food_mod_1789477527.network.ModNetwork
                    .sendTotemAnimation(player, stack.copy());
        }
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return useDuration;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return useAnimation;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        // 附魔金苹果在原版靠附魔光效区分，永恒版本保留该光效。
        return baseItem == net.minecraft.world.item.Items.ENCHANTED_GOLDEN_APPLE || super.isFoil(stack);
    }

    @Override
    public SoundEvent getDrinkingSound() {
        return baseItem.getDrinkingSound();
    }

    @Override
    public SoundEvent getEatingSound() {
        return baseItem.getEatingSound();
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltipComponents, TooltipFlag isAdvanced) {
        if (showOwnFoodEffects) {
            // 永恒亵渎之鱼等：用本物品自身的 FoodProperties 列出效果（debuff 红字）。
            appendFoodEffects(stack, tooltipComponents, false);
        } else if (baseItem == net.minecraft.world.item.Items.GOLDEN_APPLE
                || baseItem == net.minecraft.world.item.Items.ENCHANTED_GOLDEN_APPLE) {
            // 金苹果 / 附魔金苹果：用蓝色字列出所带 buff 与时长。
            appendFoodEffects(stack, tooltipComponents, false);
        } else if (baseItem == net.minecraft.world.item.Items.ROTTEN_FLESH
                || baseItem == net.minecraft.world.item.Items.SPIDER_EYE
                || baseItem == net.minecraft.world.item.Items.PUFFERFISH
                || baseItem == net.minecraft.world.item.Items.CHICKEN
                || baseItem == net.minecraft.world.item.Items.POISONOUS_POTATO) {
            // 腐肉 / 蜘蛛眼 / 河豚 / 生鸡肉 / 毒马铃薯：用红色字列出所带 debuff 与时长。
            // 生鸡肉与毒马铃薯额外列出获得 debuff 的概率（毒马铃薯为 60%）；
            // 腐肉 80%、蜘蛛眼/河豚 100% 不单独标注。
            appendFoodEffects(stack, tooltipComponents,
                    baseItem == net.minecraft.world.item.Items.CHICKEN
                            || baseItem == net.minecraft.world.item.Items.POISONOUS_POTATO);
        } else if (baseItem == net.minecraft.world.item.Items.SUSPICIOUS_STEW) {
            appendStewEffects(stack, tooltipComponents);
        }
        if (hintKey != null) {
            tooltipComponents.add(Component.translatable(hintKey).withStyle(hintColor));
        }
        tooltipComponents.add(Component.translatable("tooltip.eternal_food_mod_1789477527.eternal")
                .withStyle(ChatFormatting.GOLD));
    }

    /** 在提示框中列出食物自带的状态效果及其时长；{@code showChance} 为 true 时附带获得概率。 */
    private void appendFoodEffects(ItemStack stack, List<Component> tooltipComponents, boolean showChance) {
        // 读取本物品自身的 FoodProperties：对按 baseItem 分支的旧物品，其值与 baseItem 一致；
        // 对永恒亵渎之鱼这种无 base 效果的物品，则正确列出其自身 debuff。
        FoodProperties food = getFoodProperties(stack, null);
        if (food == null) {
            return;
        }
        for (Pair<MobEffectInstance, Float> effect : food.getEffects()) {
            tooltipComponents.add(describeEffect(effect.getFirst(),
                    showChance ? effect.getSecond() : null));
        }
    }

    /** 在提示框中用蓝色字列出迷之炖菜 NBT 里继承来的效果名与时长（原版只在创造模式高级提示框显示）。 */
    private static void appendStewEffects(ItemStack stack, List<Component> tooltipComponents) {
        int shown = 0;
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
                    tooltipComponents.add(describeEffect(new MobEffectInstance(effect, duration)));
                    shown++;
                }
            }
        }
        if (shown == 0) {
            // 无效果的迷之炖菜：灰字提示"无效果"。
            tooltipComponents.add(Component.translatable("tooltip.eternal_food_mod_1789477527.no_effect")
                    .withStyle(ChatFormatting.GRAY));
        }
    }

    /**
     * 按原版药水提示的格式（效果名 + 等级 + 时长）构造文本：debuff 用红字、buff 用蓝字。
     * {@code chance} 非 null 时在末尾附上获得该效果的概率（0~1，例如生鸡肉 0.3 → 30%）。
     */
    protected static Component describeEffect(MobEffectInstance instance) {
        return describeEffect(instance, null);
    }

    private static Component describeEffect(MobEffectInstance instance, Float chance) {
        MutableComponent description = Component.translatable(instance.getDescriptionId());
        if (instance.getAmplifier() > 0) {
            description = Component.translatable("potion.withAmplifier", description,
                    Component.translatable("potion.potency." + instance.getAmplifier()));
        }
        description = Component.translatable("potion.withDuration", description,
                MobEffectUtil.formatDuration(instance, 1.0F));
        ChatFormatting color = instance.getEffect().getCategory() == MobEffectCategory.HARMFUL
                ? ChatFormatting.RED
                : ChatFormatting.BLUE;
        description = description.withStyle(color);
        if (chance != null) {
            description = Component.translatable("tooltip.eternal_food_mod_1789477527.effect_chance",
                    description, Math.round(chance * 100.0F)).withStyle(color);
        }
        return description;
    }
}
