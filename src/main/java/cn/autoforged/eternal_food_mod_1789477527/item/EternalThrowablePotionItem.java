package cn.autoforged.eternal_food_mod_1789477527.item;

import cn.autoforged.eternal_food_mod_1789477527.entity.ThrownEternalPotion;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.ThrowablePotionItem;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/**
 * 永恒投掷型药水：包含喷溅型（splash）与滞留型（lingering）两种变体。
 *
 * <p>投掷物沿用 {@link ThrownEternalPotion}（继承原版 {@code ThrownPotion}），
 * 落点效果 / 范围 / 滞留云与原版药水完全一致；投掷不消耗物品，可无限次使用。
 *
 * <p>提示框中 buff 用蓝字、debuff 用红字列出（复用原版 {@code PotionUtils.addPotionTooltip}），
 * 底部金字的说明为"永恒：扔出后不会消耗"。
 */
public class EternalThrowablePotionItem extends ThrowablePotionItem {

    /** true = 滞留型（龙息），false = 喷溅型（火药）。 */
    private final boolean lingering;

    public EternalThrowablePotionItem(Properties properties, boolean lingering) {
        super(properties);
        this.lingering = lingering;
    }

    @Override
    public String getDescriptionId(ItemStack stack) {
        // 统一使用物品自身的通用名称（"永恒喷溅型药水" / "永恒滞留型药水"），不随药水类型变化。
        return this.getDescriptionId();
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                lingering ? SoundEvents.LINGERING_POTION_THROW : SoundEvents.SPLASH_POTION_THROW,
                lingering ? SoundSource.NEUTRAL : SoundSource.PLAYERS, 0.5F,
                0.4F / (level.getRandom().nextFloat() * 0.4F + 0.8F));
        if (!level.isClientSide) {
            ThrownEternalPotion potion = new ThrownEternalPotion(level, player);
            potion.setItem(stack);
            potion.shootFromRotation(player, player.getXRot(), player.getYRot(), -20.0F, 0.5F, 1.0F);
            level.addFreshEntity(potion);
        }
        player.awardStat(Stats.ITEM_USED.get(this));
        // 不 shrink：投掷不消耗物品（创造模式与否都不消耗）。
        stack.setCount(1);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        // 投掷型药水同样继承原版 PotionItem.useOn 的泥土转化，但永恒版本的水不会消耗，
        // 因此复用 EternalPotionItem 的转化逻辑：不消耗药水、也不返还空玻璃瓶
        // （原版创造模式下会把空玻璃瓶塞进背包，正是这个 bug）。
        return EternalPotionItem.convertMud(context);
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        // 滞留型与原版一致按 0.25 缩短时长显示，喷溅型按 1.0。
        PotionUtils.addPotionTooltip(stack, tooltip, lingering ? 0.25F : 1.0F);
        // 仅由（喷溅型 / 滞留型）水瓶酿成的永恒药水：蓝字说明右键转化泥土 + 砸中时的水流效果。
        if (PotionUtils.getPotion(stack) == Potions.WATER && PotionUtils.getMobEffects(stack).isEmpty()) {
            tooltip.add(Component.translatable("tooltip.eternal_food_mod_1789477527.water_convert")
                    .withStyle(ChatFormatting.BLUE));
            tooltip.add(Component.translatable("tooltip.eternal_food_mod_1789477527.water_splash")
                    .withStyle(ChatFormatting.BLUE));
        }
        tooltip.add(Component.translatable("tooltip.eternal_food_mod_1789477527.eternal_throw")
                .withStyle(ChatFormatting.GOLD));
    }
}
