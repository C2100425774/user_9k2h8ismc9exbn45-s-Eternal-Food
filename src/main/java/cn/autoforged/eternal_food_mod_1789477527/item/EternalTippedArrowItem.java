package cn.autoforged.eternal_food_mod_1789477527.item;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.Level;

/**
 * 永恒药箭：与原版药箭效果一致（命中后施加 1/8 时长的药水效果），区别仅在于
 * 作为弓、弩、发射器的弹药时不会被消耗，可无限次发射。
 *
 * <p>药水类型保存在物品 NBT（{@code Potion} / {@code CustomPotionEffects}）里，与原版一致，
 * {@link ArrowItem#createArrow} 会自动把效果复制到箭矢实体上。提示框中 buff 用蓝字、
 * debuff 用红字列出效果名与时长（{@link PotionUtils#addPotionTooltip}，与原版药箭同用 0.125 系数），
 * 底部金字说明为"永恒：弹药不会被消耗"。
 *
 * <p>名称固定为物品通用名（"永恒药箭"），不随药水类型变化；药水种类通过提示框效果列表与
 * 箭头染色区分。
 */
public class EternalTippedArrowItem extends ArrowItem {

    public EternalTippedArrowItem(Properties properties) {
        super(properties);
    }

    /**
     * 创造模式物品栏与 {@code /give} 的默认栈为瞬间伤害 II（与原版药箭默认中毒不同）。
     */
    @Override
    public ItemStack getDefaultInstance() {
        return PotionUtils.setPotion(super.getDefaultInstance(), Potions.STRONG_HARMING);
    }

    @Override
    public String getDescriptionId(ItemStack stack) {
        // 原版药箭会把名字改成具体药水类型名；永恒版本统一使用物品自身的通用名称。
        return this.getDescriptionId();
    }

    /**
     * 弓在 {@code BowItem#releaseUsing} 里据此判定"无限箭"，返回 true 则射击后不消耗弹药，
     * 并把发射出的箭设为仅创造模式可拾取，避免被捡回形成复制。
     */
    @Override
    public boolean isInfinite(ItemStack stack, ItemStack bow, Player player) {
        return true;
    }

    /**
     * 原版 {@code Arrow#setEffectsFromItem} 硬编码只接受 {@code Items.TIPPED_ARROW}，
     * 因此先用同 NBT 的原版药箭栈把效果灌进箭矢实体，再作为永恒药箭的投射物返回。
     */
    @Override
    public AbstractArrow createArrow(Level level, ItemStack stack, LivingEntity shooter) {
        Arrow arrow = new Arrow(level, shooter);
        applyEffects(arrow, stack);
        // 弹药不消耗，故发射出的箭不允许被捡回，避免被复制成原版药箭。
        arrow.pickup = AbstractArrow.Pickup.CREATIVE_ONLY;
        return arrow;
    }

    /** 把药箭 NBT（{@code Potion} / {@code CustomPotionEffects} / {@code CustomPotionColor}）灌入箭矢实体。 */
    public static void applyEffects(Arrow arrow, ItemStack stack) {
        ItemStack vanillaTippedArrow = new ItemStack(Items.TIPPED_ARROW);
        if (stack.getTag() != null) {
            vanillaTippedArrow.setTag(stack.getTag().copy());
        }
        arrow.setEffectsFromItem(vanillaTippedArrow);
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        // 与原版药箭一致：buff 蓝字、debuff 红字，时长按 1/8 折算。
        PotionUtils.addPotionTooltip(stack, tooltip, 0.125F);
        tooltip.add(Component.translatable("tooltip.eternal_food_mod_1789477527.eternal_ammo")
                .withStyle(ChatFormatting.GOLD));
    }
}
