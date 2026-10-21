package cn.autoforged.eternal_food_mod_1789477527.item;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.PotionItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;

/**
 * 永恒药水（普通型）：饮用后施加与原版药水完全相同的效果（时长 / 等级 / 瞬间效果），
 * 但饮用后不消耗物品，可无限次使用。
 *
 * <p>药水类型保存在物品 NBT（{@code Potion} / {@code CustomPotionEffects}）里，与原版一致，
 * 因此可以直接复用 {@link PotionItem} 的全部行为。提示框中 buff 用蓝字、debuff 用红字列出，
 * 由 {@link PotionItem#appendHoverText} 复用原版 {@code PotionUtils.addPotionTooltip} 实现。
 *
 * <p>名称固定为物品名（"永恒药水"），不随药水类型变化；药水种类通过提示框效果列表与液体颜色区分。
 */
public class EternalPotionItem extends PotionItem {

    public EternalPotionItem(Properties properties) {
        super(properties);
    }

    @Override
    public String getDescriptionId(ItemStack stack) {
        // 原版 PotionItem 会把名字改成 "药水类型"（item.minecraft.potion.effect.xxx）；
        // 永恒版本统一使用物品自身的通用名称。
        return this.getDescriptionId();
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        ItemStack original = stack.copy();
        ItemStack result = super.finishUsingItem(stack, level, entity);
        // 原版饮用后会把空瓶替换成玻璃瓶；永恒版本恢复成原来的药水，实现"不消耗"。
        if (result.getItem() == Items.GLASS_BOTTLE) {
            original.setCount(1);
            return original;
        }
        result.setCount(1);
        return result;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        return convertMud(context);
    }

    /**
     * 永恒药水（普通 / 喷溅 / 滞留通用）右键土类方块转化为泥巴的逻辑。
     *
     * <p>原版 {@link PotionItem#useOn} 会用 {@code ItemUtils.createFilledResult} 把手中药水
     * 替换成玻璃瓶——创造模式下更是直接把一个空玻璃瓶塞进玩家背包。永恒药水里的水不会消耗，
     * 因此这里完整复用原版的音效 / 粒子 / 方块转化，但**既不消耗药水，也不返还空瓶**。
     */
    protected static InteractionResult convertMud(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();
        BlockState state = level.getBlockState(pos);
        if (context.getClickedFace() != Direction.DOWN
                && state.is(BlockTags.CONVERTABLE_TO_MUD)
                && PotionUtils.getPotion(stack) == Potions.WATER) {
            level.playSound(null, pos, SoundEvents.GENERIC_SPLASH, SoundSource.BLOCKS, 1.0F, 1.0F);
            if (player != null) {
                player.awardStat(Stats.ITEM_USED.get(stack.getItem()));
            }
            if (!level.isClientSide) {
                ServerLevel serverLevel = (ServerLevel) level;
                for (int i = 0; i < 5; i++) {
                    serverLevel.sendParticles(ParticleTypes.SPLASH,
                            pos.getX() + level.random.nextDouble(), pos.getY() + 1,
                            pos.getZ() + level.random.nextDouble(), 1, 0.0, 0.0, 0.0, 1.0);
                }
            }
            level.playSound(null, pos, SoundEvents.BOTTLE_EMPTY, SoundSource.BLOCKS, 1.0F, 1.0F);
            level.gameEvent((Entity) null, GameEvent.FLUID_PLACE, pos);
            level.setBlockAndUpdate(pos, Blocks.MUD.defaultBlockState());
            // 水无法消耗：保留手中药水数量，不返还空玻璃瓶。
            stack.setCount(1);
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return InteractionResult.PASS;
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        // 仅由水瓶酿成的永恒药水：蓝字说明右键转化泥土。
        if (PotionUtils.getPotion(stack) == Potions.WATER && PotionUtils.getMobEffects(stack).isEmpty()) {
            tooltip.add(Component.translatable("tooltip.eternal_food_mod_1789477527.water_convert")
                    .withStyle(ChatFormatting.BLUE));
        }
        tooltip.add(Component.translatable("tooltip.eternal_food_mod_1789477527.eternal")
                .withStyle(ChatFormatting.GOLD));
    }
}
