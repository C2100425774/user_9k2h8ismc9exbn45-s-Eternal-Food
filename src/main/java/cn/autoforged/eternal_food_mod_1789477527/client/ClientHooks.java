package cn.autoforged.eternal_food_mod_1789477527.client;

import cn.autoforged.eternal_food_mod_1789477527.util.ClockClientState;
import net.minecraft.client.Minecraft;

/** 客户端侧的小入口：打开时钟面板、把服务端同步来的状态塞回面板与缓存。 */
public final class ClientHooks {

    private ClientHooks() {
    }

    public static void openClockScreen() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen instanceof EternalClockScreen) {
            return;
        }
        minecraft.setScreen(new EternalClockScreen());
    }

    /** 面板请求的锁定状态回包（同时带上服务端当时的 dayTime）。 */
    public static void applyClockState(boolean timeLocked, boolean weatherLocked, long dayTime) {
        ClockClientState.acceptServerTime(dayTime, timeLocked, weatherLocked);
        if (Minecraft.getInstance().screen instanceof EternalClockScreen screen) {
            screen.applyState(timeLocked, weatherLocked);
        }
    }

    /** 服务端时间 + 锁定状态推送（未锁定时每 tick 一次）。 */
    public static void applyClockTime(long dayTime, boolean timeLocked, boolean weatherLocked) {
        boolean flagsChanged = timeLocked != ClockClientState.timeLocked
                || weatherLocked != ClockClientState.weatherLocked;
        ClockClientState.acceptServerTime(dayTime, timeLocked, weatherLocked);
        // 只有锁定状态真的变了才去动按钮，避免每 tick 都重设控件
        if (flagsChanged && Minecraft.getInstance().screen instanceof EternalClockScreen screen) {
            screen.applyState(timeLocked, weatherLocked);
        }
    }
}
