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
import net.minecraft.world.entity.projectile.ThrownExperienceBottle;
import net.minecraft.world.item.ExperienceBottleItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/**
 * 永恒附魔之瓶：与原版附魔之瓶完全一致（投掷后掉落经验球，且有附魔光效），
 * 区别仅在于右键投掷不消耗物品，且被发射器发射时也不消耗，可无限次使用。
 *
 * <p>底部金字说明为「永恒：扔出后不会消耗」（与原版附魔之瓶的附魔光效 {@code isFoil} 一并保留）。
 */
public class EternalExperienceBottleItem extends ExperienceBottleItem {

    public EternalExperienceBottleItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.EXPERIENCE_BOTTLE_THROW, SoundSource.NEUTRAL, 0.5F,
                0.4F / (level.getRandom().nextFloat() * 0.4F + 0.8F));
        if (!level.isClientSide) {
            ThrownExperienceBottle bottle = new ThrownExperienceBottle(level, player);
            bottle.setItem(stack);
            bottle.shootFromRotation(player, player.getXRot(), player.getYRot(), -20.0F, 0.7F, 1.0F);
            level.addFreshEntity(bottle);
        }
        player.awardStat(Stats.ITEM_USED.get(this));
        // 不 shrink：投掷不消耗物品（创造模式与否都不消耗）。
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.eternal_food_mod_1789477527.eternal_throw")
                .withStyle(ChatFormatting.GOLD));
    }
}
