package cn.autoforged.eternal_food_mod_1789477527.item;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrownEnderpearl;
import net.minecraft.world.item.EnderpearlItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/**
 * 永恒末影珍珠：与原版末影珍珠行为一致（投掷并传送玩家），区别在于投掷不消耗物品、
 * 不进入物品冷却，也不会对玩家造成传送伤害。
 *
 * <p>不消耗与无冷却由本类 {@link #use} 直接实现；传送伤害由
 * {@code event/EternalProjectileHandler} 在 {@code EntityTeleportEvent.EnderPearl}
 * 中把攻击伤害置零来消除（该事件只在服务端对玩家触发）。
 * 按需求，永恒末影珍珠不支持发射器发射。
 */
public class EternalEnderPearlItem extends EnderpearlItem {

    public EternalEnderPearlItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENDER_PEARL_THROW, SoundSource.NEUTRAL, 0.5F,
                0.4F / (level.getRandom().nextFloat() * 0.4F + 0.8F));
        // 不调用 player.getCooldowns().addCooldown(...)：不进入冷却。
        if (!level.isClientSide) {
            ThrownEnderpearl pearl = new ThrownEnderpearl(level, player);
            pearl.setItem(stack);
            pearl.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, 1.5F, 1.0F);
            level.addFreshEntity(pearl);
        }
        player.awardStat(Stats.ITEM_USED.get(this));
        // 不 shrink：投掷不消耗物品（创造模式与否都不消耗）。
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.eternal_food_mod_1789477527.eternal_throw_no_consume")
                .withStyle(ChatFormatting.GOLD));
    }
}
