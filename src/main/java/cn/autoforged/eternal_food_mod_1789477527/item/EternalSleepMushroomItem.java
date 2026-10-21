package cn.autoforged.eternal_food_mod_1789477527.item;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/**
 * 会「睡觉」的永恒蘑菇（永恒红蘑菇 / 永恒棕蘑菇）。
 *
 * <p>食用后不消耗，玩家直接就地躺下睡觉，<b>不再需要床</b>：睡觉位置所在方块是否为床由
 * {@code SleepingLocationCheckEvent} 放行（见 event/EternalSleepHandler），因此玩家即使站在
 * 普通地面上、或在创造模式飞行时身处半空中，也能直接躺下睡觉。
 *
 * <p>两种蘑菇有互补的时间门槛（由 {@code sleepDuringDay} 区分）：
 * <ul>
 *   <li>永恒红蘑菇（{@code sleepDuringDay = false}）：仅能在 <b>夜间或雷暴</b>（即
 *       {@code !level.isDay()}，雷暴会降低 skyDarken）入睡，入睡并睡够后把时间设为
 *       {@link #RED_WAKE_TIME}（1000 = 白天），从而跳过夜晚；白天使用则提示
 *       「你只能在夜间或雷暴中入眠」并且不睡觉。</li>
 *   <li>永恒棕蘑菇（{@code sleepDuringDay = true}）：仅能在 <b>白天</b>（{@code level.isDay()}）
 *       入睡，睡醒后把时间设为 {@link #BROWN_WAKE_TIME}（13000 = 夜晚）；夜晚使用则提示
 *       「你只能在白天使用永恒灰蘑菇睡觉」并且不睡觉。</li>
 * </ul>
 * 时间推进依赖把玩家登记进睡眠列表（{@link ServerLevel#updateSleepingPlayerList()}），
 * 因此只在时间门槛满足时才登记。由于没有真正的床，本蘑菇不调用 {@code setRespawnPosition}，
 * 玩家的重生点不会被重置。
 */
public class EternalSleepMushroomItem extends EternalFoodItem {

    /** 记录玩家正处于「蘑菇引发的睡眠」的持久化 NBT 键（用于让白天也能保持睡眠）。 */
    public static final String SLEEP_TAG = "eternal_food_mod_1789477527:mushroom_sleep";

    /** 记录玩家正处于「永恒棕蘑菇引发的睡眠」的持久化 NBT 键：醒来后把时间设为 {@link #BROWN_WAKE_TIME}。 */
    public static final String BROWN_SLEEP_TAG = "eternal_food_mod_1789477527:brown_mushroom_sleep";

    /** 记录玩家正处于「永恒红蘑菇引发的睡眠」的持久化 NBT 键：醒来后把时间设为 {@link #RED_WAKE_TIME}。 */
    public static final String RED_SLEEP_TAG = "eternal_food_mod_1789477527:red_mushroom_sleep";

    /** 永恒棕蘑菇睡醒后要设置的时间刻（13000 = 夜晚，等价于原版 /time set night）。 */
    public static final long BROWN_WAKE_TIME = 13000L;

    /** 永恒红蘑菇睡醒后要设置的时间刻（1000 = 白天，等价于原版 /time set day）。 */
    public static final long RED_WAKE_TIME = 1000L;

    /** 红蘑菇在白天使用（非夜间 / 雷暴）时显示的提示键。 */
    public static final String NIGHT_ONLY_MESSAGE = "message.eternal_food_mod_1789477527.red_mushroom_wrong_time";
    /** 棕蘑菇在夜晚使用（非白天）时显示的提示键。 */
    public static final String DAY_ONLY_MESSAGE = "message.eternal_food_mod_1789477527.brown_mushroom_wrong_time";

    /** true = 只能在白天入睡（永恒棕蘑菇）；false = 只能在夜间或雷暴入睡（永恒红蘑菇）。 */
    private final boolean sleepDuringDay;

    /** 副手自动食用说明的翻译键（在提示框中额外显示）。 */
    private final String offhandHintKey;

    public EternalSleepMushroomItem(Item baseItem, Properties properties, String hintKey,
            String offhandHintKey, boolean sleepDuringDay) {
        super(baseItem, properties, false, hintKey);
        this.offhandHintKey = offhandHintKey;
        this.sleepDuringDay = sleepDuringDay;
    }

    /** true = 只能在白天入睡（永恒棕蘑菇）；false = 只能在夜间或雷暴入睡（永恒红蘑菇）。 */
    public boolean isSleepDuringDay() {
        return sleepDuringDay;
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltipComponents,
            TooltipFlag isAdvanced) {
        super.appendHoverText(stack, level, tooltipComponents, isAdvanced);
        if (offhandHintKey != null) {
            // 需求要求此说明显示在「永恒：食用后不会消耗」之前；该行由父类固定追加在末尾，
            // 故插到列表最后一个元素之前。
            Component offhandHint = Component.translatable(offhandHintKey).withStyle(ChatFormatting.BLUE);
            int eternalIndex = tooltipComponents.size() - 1;
            tooltipComponents.add(eternalIndex, offhandHint);
        }
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        // 与干海带一致：16 tick 快速进食（基础物品蘑菇本身不可食用，故显式指定）。
        return 16;
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        ItemStack result = super.finishUsingItem(stack, level, entity);
        if (!level.isClientSide && entity instanceof ServerPlayer player) {
            // 时间门槛：红蘑菇只能夜间 / 雷暴入睡，棕蘑菇只能白天入睡。
            // level.isDay() = skyDarken < 4，雷暴 / 下雨会提前变暗从而让 isDay() 返回 false，
            // 因此"夜间或雷暴"等价于 !level.isDay()。
            if (level.isDay() != sleepDuringDay) {
                player.displayClientMessage(Component.translatable(
                        sleepDuringDay ? DAY_ONLY_MESSAGE : NIGHT_ONLY_MESSAGE), true);
                return result;
            }
            // 就地躺下睡觉：不校验脚下是否有床（sleeping pos 会同步给客户端，客户端随即显示
            // 床上的睡眠界面与「起床」按钮）。
            player.getPersistentData().putBoolean(SLEEP_TAG, true);
            // 分别打标记：醒来时由 EternalSleepHandler 把时间设为各自的时间刻
            //（棕蘑菇 = 13000 夜晚，红蘑菇 = 1000 白天）。
            if (sleepDuringDay) {
                player.getPersistentData().putBoolean(BROWN_SLEEP_TAG, true);
            } else {
                player.getPersistentData().putBoolean(RED_SLEEP_TAG, true);
            }
            player.startSleeping(player.blockPosition());
            // 登记进睡眠列表：睡够后由 ServerLevel 原版逻辑让玩家醒来，随后 EternalSleepHandler
            // 把时间设为各自的时间刻（红蘑菇跳过夜晚、棕蘑菇变为夜晚）。门槛不满足时不会走到这里。
            ((ServerLevel) level).updateSleepingPlayerList();
        }
        return result;
    }
}
