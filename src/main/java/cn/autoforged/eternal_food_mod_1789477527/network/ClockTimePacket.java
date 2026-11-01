package cn.autoforged.eternal_food_mod_1789477527.network;

import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/**
 * 服务端 → 客户端：每秒推一次「服务端的时间与锁定状态」。
 *
 * <p>为什么需要它：{@code ClientLevel#tickTime()} 会按客户端自己的节奏把 dayTime 往前加，
 * 而服务端一卡（TPS 掉下来）服务端的时间就跑得比客户端慢，下一次原版 SetTime 同步就会
 * 把客户端的显示往回拉 —— 表现就是"卡顿时时间反复往回跳"。
 *
 * <p>所以这里改成<b>时间只认服务端</b>：客户端不自己算，只用这个包带来的值。
 * 一秒一次对 HH:MM 的显示完全够用（一分钟 = 1000 刻 = 50 秒）。
 */
public class ClockTimePacket {

    private final long dayTime;
    private final boolean timeLocked;
    private final boolean weatherLocked;

    public ClockTimePacket(long dayTime, boolean timeLocked, boolean weatherLocked) {
        this.dayTime = dayTime;
        this.timeLocked = timeLocked;
        this.weatherLocked = weatherLocked;
    }

    public static void encode(ClockTimePacket msg, FriendlyByteBuf buf) {
        buf.writeLong(msg.dayTime);
        buf.writeBoolean(msg.timeLocked);
        buf.writeBoolean(msg.weatherLocked);
    }

    public static ClockTimePacket decode(FriendlyByteBuf buf) {
        return new ClockTimePacket(buf.readLong(), buf.readBoolean(), buf.readBoolean());
    }

    public static void handle(ClockTimePacket msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> cn.autoforged.eternal_food_mod_1789477527.client.ClientHooks
                        .applyClockTime(msg.dayTime, msg.timeLocked, msg.weatherLocked)));
        context.setPacketHandled(true);
    }
}
