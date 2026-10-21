package cn.autoforged.eternal_food_mod_1789477527.event;

import cn.autoforged.eternal_food_mod_1789477527.EternalFoodMod;
import cn.autoforged.eternal_food_mod_1789477527.item.EternalSleepMushroomItem;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerWakeUpEvent;
import net.minecraftforge.event.entity.player.SleepingLocationCheckEvent;
import net.minecraftforge.event.entity.player.SleepingTimeCheckEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 配合 {@link EternalSleepMushroomItem} 的原版睡眠行为。
 *
 * <p>蘑菇引发的睡眠不需要真正的床：原版每个 tick 都会调用
 * {@code ForgeEventFactory.fireSleepingLocationCheck}，脚下不是床就返回 false 从而把睡眠中的玩家
 * 叫醒。这里对「睡觉位置不是床」的睡眠返回 {@link Event.Result#ALLOW}，让玩家能直接躺在地上
 * （或在创造模式飞行时躺在半空中）保持睡眠。
 *
 * <p>原版还会调用 {@code ForgeEventFactory.fireSleepingTimeCheck}，白天返回 false 把玩家叫醒；
 * 对带 {@code SLEEP_TAG} 的「蘑菇引发的睡眠」返回 ALLOW，使其在不符合原版睡眠时间的时刻也能保持
 * 睡眠（棕蘑菇白天入睡、红蘑菇睡到天亮）。蘑菇自身的时间门槛（红蘑菇仅夜间 / 雷暴、棕蘑菇仅白天）
 * 在 {@link EternalSleepMushroomItem#finishUsingItem} 里判断，不满足时只提示、不进入睡眠。
 * 玩家真正醒来（点击起床、睡够被唤醒或被取消）时清除标记。
 */
@Mod.EventBusSubscriber(modid = EternalFoodMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class EternalSleepHandler {

    /**
     * 记录玩家上一次「副手自动食用蘑菇」尝试的<b>现实时间</b>（{@link System#currentTimeMillis()}），
     * 用于实现每 15 秒（现实时间，而非游戏 tick）尝试一次。
     */
    private static final String AUTO_USE_TAG = "eternal_food_mod_1789477527:last_offhand_auto_use";

    /** 副手自动食用间隔：现实时间 15000 毫秒（15 秒）。 */
    private static final long AUTO_USE_INTERVAL_MS = 15_000L;

    private EternalSleepHandler() {
    }

    /**
     * 副手自动食用：永恒红 / 棕蘑菇放在副手且当前时间满足其入睡门槛时，每现实 15 秒尝试自动食用一次，
     * 以便挂机时自动跳过黑夜 / 白天。判定与食用完全复用 {@link EternalSleepMushroomItem} 的睡眠逻辑，
     * 时间不满足时本次不食用（不刷屏提示），下一 15 秒再试。
     */
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        long now = System.currentTimeMillis();
        for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
            tryOffhandAutoUse(player, now);
        }
    }

    private static void tryOffhandAutoUse(ServerPlayer player, long now) {
        ItemStack offhand = player.getOffhandItem();
        if (!(offhand.getItem() instanceof EternalSleepMushroomItem mushroom)
                || !player.isAlive() || player.isSleeping()) {
            return;
        }
        CompoundTag data = player.getPersistentData();
        long last = data.getLong(AUTO_USE_TAG);
        if (last == 0L) {
            // 首次观察到：只记录起点，15 秒后再尝试。
            data.putLong(AUTO_USE_TAG, now);
            return;
        }
        if (now - last < AUTO_USE_INTERVAL_MS) {
            return;
        }
        data.putLong(AUTO_USE_TAG, now);
        // 仅在时间满足各自的入睡门槛时自动食用（红蘑菇夜间 / 雷暴、棕蘑菇白天）。
        if (player.level().isDay() == mushroom.isSleepDuringDay()) {
            offhand.finishUsingItem(player.level(), player);
        }
    }

    @SubscribeEvent
    public static void onSleepingTimeCheck(SleepingTimeCheckEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && player.isSleeping()
                && player.getPersistentData().getBoolean(EternalSleepMushroomItem.SLEEP_TAG)) {
            event.setResult(Event.Result.ALLOW);
        }
    }

    @SubscribeEvent
    public static void onSleepingLocationCheck(SleepingLocationCheckEvent event) {
        // 该事件在逻辑客户端与服务端都会触发（LivingEntity#tick 中的 checkBedExists），
        // 而 SLEEP_TAG 只在服务端设置、不会同步到客户端，因此这里改用「睡觉位置不是床」来判断，
        // 两端行为一致。睡在真正的床上时不干预，保持原版行为。
        if (event.getEntity() instanceof Player player && player.isSleeping()) {
            BlockPos pos = event.getSleepingLocation();
            if (!player.level().getBlockState(pos).isBed(player.level(), pos, player)) {
                event.setResult(Event.Result.ALLOW);
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerWakeUp(PlayerWakeUpEvent event) {
        Player player = event.getEntity();
        boolean brownMushroomSleep = player.getPersistentData()
                .getBoolean(EternalSleepMushroomItem.BROWN_SLEEP_TAG);
        boolean redMushroomSleep = player.getPersistentData()
                .getBoolean(EternalSleepMushroomItem.RED_SLEEP_TAG);
        player.getPersistentData().remove(EternalSleepMushroomItem.SLEEP_TAG);
        player.getPersistentData().remove(EternalSleepMushroomItem.BROWN_SLEEP_TAG);
        player.getPersistentData().remove(EternalSleepMushroomItem.RED_SLEEP_TAG);
        // 蘑菇的「睡醒时间」：自然睡醒（睡够被唤醒）后把时间设为各自的时间刻
        //（棕蘑菇 = 13000 夜晚，红蘑菇 = 1000 白天）。
        // 原版里「睡熟被唤醒」由 ServerLevel#wakeUpAllPlayers 触发 stopSleepInBed(false, false)
        // （updateLevel=false）；而中途点击「起床」由 STOP_SLEEPING 包触发 stopSleepInBed(false, true)
        // （updateLevel=true）。因此用 updateLevel() 区分：只有自然睡醒才改时间，中途起床不改。
        // 这些标签只在服务端设置，客户端读不到，故此处天然只在服务端生效。
        if (!event.updateLevel() && player.level() instanceof ServerLevel serverLevel) {
            if (brownMushroomSleep) {
                serverLevel.setDayTime(EternalSleepMushroomItem.BROWN_WAKE_TIME);
            } else if (redMushroomSleep) {
                serverLevel.setDayTime(EternalSleepMushroomItem.RED_WAKE_TIME);
            }
        }
    }
}
