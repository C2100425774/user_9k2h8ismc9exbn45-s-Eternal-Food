package cn.autoforged.eternal_food_mod_1789477527.item;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.item.FireworkRocketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * 永恒烟花火箭：与原版烟花火箭行为一致，但任何使用方式都不消耗物品。
 *
 * <p>覆盖四种使用途径：
 * <ul>
 *   <li>右键放置（{@link #useOn}）；</li>
 *   <li>鞘翅飞行时右键加速（{@link #use}）；</li>
 *   <li>发射器发射（见 {@code event/ModDispenseBehaviors}）；</li>
 *   <li>弩发射（见 {@code mixin/CrossbowItemMixin} 与 {@code event/ModAmmoHandler}）。</li>
 * </ul>
 *
 * <p>烟花效果（飞行时间 {@code Flight} 与爆炸 {@code Explosions}）保存在物品 NBT 里，
 * 由 {@code EternalFireworkRocketRecipe} 在合成时从成品烟花火箭继承，因此放置 / 发射出的
 * 烟花与原烟花完全相同（{@link FireworkRocketItem} 的提示框也会照常列出这些效果）。
 */
public class EternalFireworkRocketItem extends FireworkRocketItem {

    public EternalFireworkRocketItem(Properties properties) {
        super(properties);
    }

    /** 右键方块放置烟花：与原版一致，但不消耗物品。 */
    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (!level.isClientSide) {
            ItemStack stack = context.getItemInHand();
            Vec3 vec3 = context.getClickLocation();
            Direction direction = context.getClickedFace();
            FireworkRocketEntity rocket = new FireworkRocketEntity(level, context.getPlayer(),
                    vec3.x + direction.getStepX() * 0.15,
                    vec3.y + direction.getStepY() * 0.15,
                    vec3.z + direction.getStepZ() * 0.15,
                    stack);
            level.addFreshEntity(rocket);
            // 不 shrink：使用不消耗物品。
        }

        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    /** 鞘翅飞行时右键加速：与原版一致，但不消耗物品。 */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (player.isFallFlying()) {
            ItemStack stack = player.getItemInHand(hand);
            if (!level.isClientSide) {
                FireworkRocketEntity rocket = new FireworkRocketEntity(level, stack, player);
                level.addFreshEntity(rocket);
                player.awardStat(Stats.ITEM_USED.get(this));
            }

            return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide());
        } else {
            return InteractionResultHolder.pass(player.getItemInHand(hand));
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        // 原版会列出飞行时间与爆炸效果，永恒版本照常继承显示。
        super.appendHoverText(stack, level, tooltip, flag);
        tooltip.add(Component.translatable("tooltip.eternal_food_mod_1789477527.eternal_use")
                .withStyle(ChatFormatting.GOLD));
    }
}
