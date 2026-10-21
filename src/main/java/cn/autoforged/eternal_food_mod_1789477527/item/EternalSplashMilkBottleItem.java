package cn.autoforged.eternal_food_mod_1789477527.item;

import cn.autoforged.eternal_food_mod_1789477527.entity.ThrownEternalMilk;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/**
 * 永恒喷溅型奶瓶：右键投掷，落点范围内清除状态效果。
 * 永恒特性：投掷不消耗物品，可无限次使用。
 */
public class EternalSplashMilkBottleItem extends Item {

    /** 提示框蓝色说明的翻译键。 */
    private final String hintKey;

    public EternalSplashMilkBottleItem(Properties properties, String hintKey) {
        super(properties);
        this.hintKey = hintKey;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.SPLASH_POTION_THROW, SoundSource.PLAYERS, 0.5F,
                0.4F / (level.getRandom().nextFloat() * 0.4F + 0.8F));
        if (!level.isClientSide) {
            ThrownEternalMilk bottle = new ThrownEternalMilk(level, player);
            bottle.setItem(stack);
            bottle.shootFromRotation(player, player.getXRot(), player.getYRot(), -20.0F, 0.5F, 1.0F);
            level.addFreshEntity(bottle);
        }
        player.awardStat(Stats.ITEM_USED.get(this));
        // 不 shrink：投掷不消耗物品（创造模式与否都不消耗）。
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltipComponents, TooltipFlag isAdvanced) {
        if (hintKey != null) {
            tooltipComponents.add(Component.translatable(hintKey).withStyle(ChatFormatting.BLUE));
        }
        tooltipComponents.add(Component.translatable("tooltip.eternal_food_mod_1789477527.eternal_throw")
                .withStyle(ChatFormatting.GOLD));
    }
}
