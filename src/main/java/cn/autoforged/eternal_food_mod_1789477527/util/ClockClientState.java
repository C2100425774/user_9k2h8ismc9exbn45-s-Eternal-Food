package cn.autoforged.eternal_food_mod_1789477527.util;

import net.minecraft.world.level.Level;

/**
 * 客户端侧的「时间显示」缓存 —— 时间只认服务端。
 *
 * <p>{@code ClientLevel#tickTime()} 会按客户端自己的节奏把 dayTime 往前加，服务端一卡
 * （TPS 下降）两端就对不上，下一次原版同步会把显示往回拉，表现就是"卡顿时时间反复往回跳"。
 * 因此客户端完全不自己算时间，只用服务端每秒推来的值：
 *
 * <ul>
 *   <li>{@link #serverDayTime}：服务端最近一次推送的 dayTime（显示的唯一来源）；</li>
 *   <li>{@link #serverSynced}：还没收到首包时退回读本地世界时间，避免显示空白；</li>
 *   <li>{@link #timeLocked} / {@link #weatherLocked}：只用于面板按钮的冰蓝高亮，
 *       由每秒的推送和面板的请求回包刷新，不再依赖登录事件预置。</li>
 * </ul>
 *
 * <p>字段都是普通静态字段（不引用任何客户端类），服务端加载也安全。
 */
public final class ClockClientState {

    /** 时间是否被锁定（仅用于面板按钮显示）。 */
    public static boolean timeLocked;
    /** 天气是否被锁定（仅用于面板按钮显示）。 */
    public static boolean weatherLocked;
    /** 服务端最近一次推送的 dayTime —— 显示的唯一依据。 */
    public static long serverDayTime;
    /** 是否已经收到过服务端的时间。 */
    public static boolean serverSynced;

    private ClockClientState() {
    }

    /** 取「该显示的时间」：有服务端推送值就用它，否则退回本地世界时间（首包到达前的兜底）。 */
    public static long effectiveDayTime(Level level) {
        if (serverSynced) {
            return serverDayTime;
        }
        return level == null ? 0L : level.getDayTime();
    }

    /** 服务端时间 / 锁定状态推送。 */
    public static void acceptServerTime(long dayTime, boolean timeLocked, boolean weatherLocked) {
        serverDayTime = dayTime;
        serverSynced = true;
        ClockClientState.timeLocked = timeLocked;
        ClockClientState.weatherLocked = weatherLocked;
    }

    /** 游戏刻 → HH:MM（0 tick = 06:00）。 */
    public static String format(long dayTime) {
        long wrapped = Math.floorMod(dayTime, 24000L);
        int hour = (int) (((wrapped / 1000L) + 6L) % 24L);
        int minute = (int) ((wrapped % 1000L) * 60L / 1000L);
        return String.format("%02d:%02d", hour, minute);
    }
}
