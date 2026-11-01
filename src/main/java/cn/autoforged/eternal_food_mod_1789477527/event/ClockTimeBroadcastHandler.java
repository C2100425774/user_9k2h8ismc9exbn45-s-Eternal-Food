package cn.autoforged.eternal_food_mod_1789477527.event;

import cn.autoforged.eternal_food_mod_1789477527.EternalFoodMod;
import cn.autoforged.eternal_food_mod_1789477527.network.ModNetwork;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameRules;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 把服务端的 dayTime 与锁定状态推给该维度的玩家 —— 这是时钟显示的唯一数据源。
 *
 * <p>客户端不自己算时间（{@code ClientLevel#tickTime()} 会自己加，服务端一卡就对不上，
 * 导致时间反复往回跳），但「每秒推一次」对钟点够用、对<b>刻数</b>不够：
 * 采样点固定在 {@code gameTime % 20 == 0}，日时间每次正好 +20，个位数字就被钉死了
 * （永远显示 …0 或 …4，只有十位在动）。所以改成：
 *
 * <ul>
 *   <li><b>时间在走时（未锁定）</b>：每 tick 都推 → 刻数逐 tick 增长，个位正常滚动；</li>
 *   <li><b>时间没变化时</b>：只做每秒一次的心跳，方便刚进服的玩家在 1 秒内拿到状态，
 *       平时锁定状态下几乎不产生流量。</li>
 * </ul>
 *
 * <p>不做「登录 / 复活 / 换维度」的额外校正：上面的推送已经能在 1 秒内纠正一切，
 * 再挂登录类事件只会和别的模组在这些事件上的处理互相牵扯。
 */
@Mod.EventBusSubscriber(modid = EternalFoodMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ClockTimeBroadcastHandler {

    /** 时间没有变化时的心跳间隔（tick）。 */
    private static final int HEARTBEAT_TICKS = 20;

    private static long lastDayTime = Long.MIN_VALUE;
    private static boolean lastTimeLocked;
    private static boolean lastWeatherLocked;

    private ClockTimeBroadcastHandler() {
    }

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        if (!(event.level instanceof ServerLevel level)) {
            return;
        }

        GameRules rules = level.getGameRules();
        long dayTime = level.getDayTime();
        boolean timeLocked = !rules.getBoolean(GameRules.RULE_DAYLIGHT);
        boolean weatherLocked = !rules.getBoolean(GameRules.RULE_WEATHER_CYCLE);

        boolean changed = dayTime != lastDayTime
                || timeLocked != lastTimeLocked
                || weatherLocked != lastWeatherLocked;
        if (!changed && level.getGameTime() % HEARTBEAT_TICKS != 0L) {
            return;
        }

        lastDayTime = dayTime;
        lastTimeLocked = timeLocked;
        lastWeatherLocked = weatherLocked;

        for (ServerPlayer player : level.players()) {
            ModNetwork.sendClockTime(player, dayTime, timeLocked, weatherLocked);
        }
    }
}
