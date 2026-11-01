package cn.autoforged.eternal_food_mod_1789477527.item;

import cn.autoforged.eternal_food_mod_1789477527.client.ClientHooks;
import cn.autoforged.eternal_food_mod_1789477527.util.ClockClientState;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;

/**
 * 永恒数字钟的<b>物品形态</b>（拿在手里的那只）。
 *
 * <ul>
 *   <li>物品栏里是一张平面贴图（像告示牌那样贴着格子）；</li>
 *   <li><b>右键</b>：打开时间 / 天气控制面板；</li>
 *   <li><b>潜行 + 右键</b>：放置成方块形态；</li>
 *   <li>提示框里显示当前游戏时间。</li>
 * </ul>
 */
public class EternalClockItem extends BlockItem {

    public EternalClockItem(Block block, Properties properties) {
        super(block, properties);
    }

    /** 显示「当前」时间；锁定后改用服务端快照，避免客户端本地时间往回跳。 */
    public static String formatTime(Level level) {
        return ClockClientState.format(ClockClientState.effectiveDayTime(level));
    }

    private static void openScreen(Level level) {
        if (level.isClientSide) {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> ClientHooks::openClockScreen);
        }
    }

    /** 右键空气：打开面板（潜行时交给父类，等于什么都不做）。 */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (player.isShiftKeyDown()) {
            return super.use(level, player, hand);
        }
        openScreen(level);
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide);
    }

    /** 右键方块：潜行才放置，否则打开面板。 */
    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player != null && player.isShiftKeyDown()) {
            return super.useOn(context);
        }
        openScreen(context.getLevel());
        return InteractionResult.sidedSuccess(context.getLevel().isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltipComponents,
            TooltipFlag isAdvanced) {
        super.appendHoverText(stack, level, tooltipComponents, isAdvanced);
        if (level != null) {
            // 和面板信息区一致的三行：当前时间 / 当前刻 / 当前天气
            tooltipComponents.add(Component
                    .translatable("gui.eternal_food_mod_1789477527.clock.current", formatTime(level))
                    .withStyle(ChatFormatting.AQUA));
            tooltipComponents.add(Component
                    .translatable("gui.eternal_food_mod_1789477527.clock.current_tick",
                            Math.floorMod(ClockClientState.effectiveDayTime(level), 24000L))
                    .withStyle(ChatFormatting.GRAY));
            // 没有原版天气的维度（下界 / 末地等）直接显示「无天气」，而不是假装晴天
            Component weatherText = level.dimensionType().hasSkyLight()
                    ? Component.translatable("gui.eternal_food_mod_1789477527.clock.weather."
                            + (level.isThundering() ? 2 : (level.isRaining() ? 1 : 0)))
                    : Component.translatable("gui.eternal_food_mod_1789477527.clock.weather.none");
            tooltipComponents.add(Component
                    .translatable("gui.eternal_food_mod_1789477527.clock.current_weather")
                    .append(weatherText)
                    .withStyle(ChatFormatting.DARK_AQUA));
        }
        // 提示文字白色，「黄色」「蓝色」两个词用 lang 里的 §e / §9 上色（和表盘同色）
        tooltipComponents.add(Component
                .translatable("tooltip.eternal_food_mod_1789477527.eternal_clock_daynight",
                        Component.translatable(
                                "tooltip.eternal_food_mod_1789477527.eternal_clock_daynight.day"),
                        Component.translatable(
                                "tooltip.eternal_food_mod_1789477527.eternal_clock_daynight.night"))
                .withStyle(ChatFormatting.WHITE));
        tooltipComponents.add(Component
                .translatable("tooltip.eternal_food_mod_1789477527.eternal_clock_open")
                .withStyle(ChatFormatting.GRAY));
        tooltipComponents.add(Component
                .translatable("tooltip.eternal_food_mod_1789477527.eternal_clock_place")
                .withStyle(ChatFormatting.GRAY));
    }
}
