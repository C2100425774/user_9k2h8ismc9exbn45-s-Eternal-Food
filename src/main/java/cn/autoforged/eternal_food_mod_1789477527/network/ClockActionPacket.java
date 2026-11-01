package cn.autoforged.eternal_food_mod_1789477527.network;

import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameRules;
import net.minecraftforge.network.NetworkEvent;

/**
 * 客户端 → 服务端：永恒时钟面板的操作。
 *
 * <pre>
 * action 0 : 设定时间（value = 游戏刻）
 * action 1 : 设定天气（value 0 晴 / 1 雨 / 2 雷暴）
 * action 2 : 切换「锁定时间」—— 服务端翻转 doDaylightCycle 后回包同步状态
 * action 3 : 切换「锁定天气」—— 服务端翻转 doWeatherCycle 后回包同步状态
 * action 4 : 请求同步当前锁定状态（面板打开时用）
 * </pre>
 *
 * <p>锁定做成「服务端翻转 + 回包」而不是「客户端决定锁/解锁」：客户端读不到 gamerule，
 * 靠本地状态猜会导致「别人已经锁了，自己再点反而又锁一次」这种解不开的 bug。
 */
public class ClockActionPacket {

    public static final int ACTION_SET_TIME = 0;
    public static final int ACTION_SET_WEATHER = 1;
    public static final int ACTION_TOGGLE_TIME_LOCK = 2;
    public static final int ACTION_TOGGLE_WEATHER_LOCK = 3;
    public static final int ACTION_REQUEST_STATE = 4;

    private final int action;
    private final long value;

    public ClockActionPacket(int action, long value) {
        this.action = action;
        this.value = value;
    }

    public static void encode(ClockActionPacket msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.action);
        buf.writeLong(msg.value);
    }

    public static ClockActionPacket decode(FriendlyByteBuf buf) {
        return new ClockActionPacket(buf.readVarInt(), buf.readLong());
    }

    public static void handle(ClockActionPacket msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) {
                return;
            }
            ServerLevel level = player.serverLevel();
            GameRules rules = level.getGameRules();
            switch (msg.action) {
                case ACTION_SET_TIME:
                    level.setDayTime(Math.floorMod(msg.value, 24000L));
                    break;
                case ACTION_SET_WEATHER:
                    applyWeather(level, (int) msg.value);
                    break;
                case ACTION_TOGGLE_TIME_LOCK: {
                    GameRules.BooleanValue rule = rules.getRule(GameRules.RULE_DAYLIGHT);
                    rule.set(!rule.get(), level.getServer());
                    break;
                }
                case ACTION_TOGGLE_WEATHER_LOCK: {
                    GameRules.BooleanValue rule = rules.getRule(GameRules.RULE_WEATHER_CYCLE);
                    rule.set(!rule.get(), level.getServer());
                    break;
                }
                case ACTION_REQUEST_STATE:
                default:
                    break;
            }
            // 任何操作后都把最新状态推回客户端（锁定按钮的高亮 / 锁定后的时间快照都依赖它）
            ModNetwork.sendClockState(player,
                    !rules.getBoolean(GameRules.RULE_DAYLIGHT),
                    !rules.getBoolean(GameRules.RULE_WEATHER_CYCLE),
                    level.getDayTime());
        });
        context.setPacketHandled(true);
    }

    private static void applyWeather(ServerLevel level, int weather) {
        switch (weather) {
            case 0:
                level.setWeatherParameters(12000, 0, false, false);
                break;
            case 1:
                level.setWeatherParameters(0, 12000, true, false);
                break;
            case 2:
                level.setWeatherParameters(0, 12000, true, true);
                break;
            default:
                break;
        }
    }
}
