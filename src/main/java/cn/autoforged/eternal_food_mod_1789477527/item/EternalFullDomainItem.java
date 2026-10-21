package cn.autoforged.eternal_food_mod_1789477527.item;

import cn.autoforged.eternal_food_mod_1789477527.network.ModNetwork;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 永恒满置域：长按 5 秒使用一次，不消耗物品。
 *
 * <p>只能在主世界（{@link Level#OVERWORLD}）使用。使用期间客户端显示「拉弓」蓄力姿态
 * （{@link UseAnim#BOW}），让蓄力过程可见；使用完成后再让该玩家客户端播放一次原版
 * 「不死图腾」激活动画作为完成反馈。创造模式玩家在空中 / 飞行时同样可以使用。
 *
 * <p>使用完成后，在玩家当前所在区块（16×16）内，从「玩家下半身所在高度 - 1」（即玩家站立的
 * 方块）开始，一直向下到可放置方块的最低高度（{@link Level#getMinBuildHeight()}，主世界为 -64）：
 * <ul>
 *   <li>遇到<b>完整方块</b>（碰撞形状占满整格，如石头 / 泥土 / 基岩等）直接<b>跳过不填充</b>，
 *       并统计跳过数量（基岩属于完整方块，因此底部屏障自然得到保留）；</li>
 *   <li><b>不完整方块</b>（空气 / 水 / 花草 / 火把 / 台阶 / 楼梯等）则被<b>替换</b>为石头或深板岩；</li>
 * </ul>
 * 若玩家所在高度 y &le; 0，则一律填充深板岩；否则按方块自身高度：高度 &gt; 0 填充石头、
 * 高度 &le; 0 填充深板岩。填充完成后向玩家提示石头 / 深板岩的填充数量与跳过的方块数量。
 */
public class EternalFullDomainItem extends Item {

    /** 使用时长：5 秒 = 100 tick。 */
    public static final int USE_DURATION = 100;

    public EternalFullDomainItem(Properties properties) {
        super(properties);
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return USE_DURATION;
    }

    /** 蓄力时显示拉弓姿态，让「正在使用」这件事直观可见。 */
    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BOW;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        // 只能在主世界使用：非主世界直接不进入蓄力并提示。
        if (!level.isClientSide && level.dimension() != Level.OVERWORLD) {
            player.displayClientMessage(Component.translatable(
                    "message.eternal_food_mod_1789477527.full_domain_only_overworld"), true);
            return InteractionResultHolder.fail(stack);
        }
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (!level.isClientSide && entity instanceof ServerPlayer player) {
            fillDomain(level, player);
            // 使用完成后让该玩家客户端播放一次不死图腾激活动画作为完成反馈。
            ModNetwork.sendTotemAnimation(player, stack.copy());
        }
        // 永恒特性：不消耗物品。
        return stack;
    }

    /** 在主世界玩家所在区块内执行填充，并统计石头 / 深板岩 / 跳过的数量。 */
    private static void fillDomain(Level level, ServerPlayer player) {
        ChunkPos chunk = player.chunkPosition();
        int startY = player.blockPosition().getY() - 1;
        int minY = level.getMinBuildHeight();
        if (startY < minY) {
            return;
        }
        // 玩家所在高度 <= 0 时只填充深板岩，不再按方块自身高度区分。
        boolean onlyDeepslate = player.blockPosition().getY() <= 0;

        int stone = 0;
        int deepslate = 0;
        int skipped = 0;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int x = chunk.getMinBlockX(); x <= chunk.getMaxBlockX(); x++) {
            for (int z = chunk.getMinBlockZ(); z <= chunk.getMaxBlockZ(); z++) {
                for (int y = startY; y >= minY; y--) {
                    pos.set(x, y, z);
                    BlockState state = level.getBlockState(pos);
                    // 完整方块（含基岩）直接跳过不填充，只统计跳过数量。
                    if (state.isCollisionShapeFullBlock(level, pos)) {
                        skipped++;
                        continue;
                    }
                    // 玩家 y<=0 时只填深板岩；否则 y>0 填石头、y<=0 填深板岩。
                    boolean useStone = !onlyDeepslate && y > 0;
                    level.setBlock(pos, (useStone ? Blocks.STONE : Blocks.DEEPSLATE).defaultBlockState(), 2);
                    if (useStone) {
                        stone++;
                    } else {
                        deepslate++;
                    }
                }
            }
        }

        player.displayClientMessage(Component.translatable(
                "message.eternal_food_mod_1789477527.full_domain_filled", stone, deepslate, skipped), false);
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltipComponents,
            TooltipFlag isAdvanced) {
        tooltipComponents.add(Component.translatable(
                "tooltip.eternal_food_mod_1789477527.full_domain").withStyle(ChatFormatting.BLUE));
        tooltipComponents.add(Component.translatable(
                "tooltip.eternal_food_mod_1789477527.eternal_use").withStyle(ChatFormatting.GOLD));
    }
}
