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
import net.minecraft.world.entity.projectile.LargeFireball;
import net.minecraft.world.entity.projectile.SmallFireball;
import net.minecraft.world.entity.projectile.Snowball;
import net.minecraft.world.entity.projectile.WitherSkull;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * 永恒弹射物：右键发射对应的原版投射物，且不消耗物品。
 *
 * <p>覆盖的具体投射物由 {@link Kind} 决定（雪球、烈焰人小火球、恶魂大火球、
 * 凋灵之首 / 蓝色凋灵之首）。发射后物品数量保持不变，底部金字说明为
 * 「永恒：扔出时不消耗」。被发射器发射时同样不消耗，见
 * {@code event/ModDispenseBehaviors}。
 */
public class EternalProjectileItem extends Item {

    public enum Kind {
        /** 永恒雪球：发射原版雪球。 */
        SNOWBALL,
        /** 永恒火焰弹：发射烈焰人的投射物（小火球）。 */
        SMALL_FIREBALL,
        /** 永恒火球：发射恶魂的投射物（大火球）。 */
        LARGE_FIREBALL,
        /** 永恒凋灵之首：发射凋灵之首。 */
        WITHER_SKULL,
        /** 永恒蓝色凋灵之首：发射蓝色（危险）凋灵之首。 */
        BLUE_WITHER_SKULL
    }

    private final Kind kind;

    public EternalProjectileItem(Properties properties, Kind kind) {
        super(properties);
        this.kind = kind;
    }

    public Kind getKind() {
        return kind;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        switch (kind) {
            case SNOWBALL -> {
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.SNOWBALL_THROW, SoundSource.NEUTRAL, 0.5F,
                        0.4F / (level.getRandom().nextFloat() * 0.4F + 0.8F));
                if (!level.isClientSide) {
                    Snowball snowball = new Snowball(level, player);
                    snowball.setItem(stack);
                    snowball.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, 1.5F, 1.0F);
                    level.addFreshEntity(snowball);
                }
            }
            case SMALL_FIREBALL -> launchFireball(level, player, stack, false);
            case LARGE_FIREBALL -> launchFireball(level, player, stack, true);
            case WITHER_SKULL -> launchSkull(level, player, false);
            case BLUE_WITHER_SKULL -> launchSkull(level, player, true);
        }
        player.awardStat(Stats.ITEM_USED.get(this));
        // 不 shrink：发射不消耗物品（创造模式与否都不消耗）。
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    /** 发射烈焰人小火球 / 恶魂大火球：从玩家眼睛位置沿视线方向飞出。 */
    private static void launchFireball(Level level, Player player, ItemStack stack, boolean large) {
        Vec3 look = player.getLookAngle();
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                large ? SoundEvents.GHAST_SHOOT : SoundEvents.BLAZE_SHOOT, SoundSource.NEUTRAL, 1.0F, 1.0F);
        if (!level.isClientSide) {
            double x = player.getX() + look.x;
            double y = player.getEyeY() - 0.1D;
            double z = player.getZ() + look.z;
            if (large) {
                LargeFireball fireball = new LargeFireball(level, player, look.x, look.y, look.z, 1);
                fireball.setItem(stack);
                fireball.setPos(x, y, z);
                level.addFreshEntity(fireball);
            } else {
                SmallFireball fireball = new SmallFireball(level, player, look.x, look.y, look.z);
                fireball.setItem(stack);
                fireball.setPos(x, y, z);
                level.addFreshEntity(fireball);
            }
        }
    }

    /** 发射凋灵之首；{@code dangerous} 为 true 时是蓝色（危险）变体。 */
    private static void launchSkull(Level level, Player player, boolean dangerous) {
        Vec3 look = player.getLookAngle();
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.WITHER_SHOOT, SoundSource.NEUTRAL, 1.0F, 1.0F);
        if (!level.isClientSide) {
            WitherSkull skull = new WitherSkull(level, player, look.x, look.y, look.z);
            skull.setDangerous(dangerous);
            skull.setPos(player.getX() + look.x, player.getEyeY() - 0.1D, player.getZ() + look.z);
            level.addFreshEntity(skull);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.eternal_food_mod_1789477527.eternal_throw_no_consume")
                .withStyle(ChatFormatting.GOLD));
    }
}
