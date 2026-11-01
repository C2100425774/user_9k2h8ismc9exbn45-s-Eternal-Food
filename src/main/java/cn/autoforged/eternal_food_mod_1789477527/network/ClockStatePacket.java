package cn.autoforged.eternal_food_mod_1789477527.network;

import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/**
 * 服务端 → 客户端：同步时钟面板的状态。
 *
 * <p>{@code dayTime} 是服务端当时的真实 dayTime。锁定后客户端不能再用本地时间
 * （客户端会自己往前走，见 {@code ClockClientState} 的注释），必须用这个快照。
 */
public class ClockStatePacket {

    private final boolean timeLocked;
    private final boolean weatherLocked;
    private final long dayTime;

    public ClockStatePacket(boolean timeLocked, boolean weatherLocked, long dayTime) {
        this.timeLocked = timeLocked;
        this.weatherLocked = weatherLocked;
        this.dayTime = dayTime;
    }

    public boolean timeLocked() {
        return this.timeLocked;
    }

    public boolean weatherLocked() {
        return this.weatherLocked;
    }

    public long dayTime() {
        return this.dayTime;
    }

    public static void encode(ClockStatePacket msg, FriendlyByteBuf buf) {
        buf.writeBoolean(msg.timeLocked);
        buf.writeBoolean(msg.weatherLocked);
        buf.writeLong(msg.dayTime);
    }

    public static ClockStatePacket decode(FriendlyByteBuf buf) {
        return new ClockStatePacket(buf.readBoolean(), buf.readBoolean(), buf.readLong());
    }

    public static void handle(ClockStatePacket msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> cn.autoforged.eternal_food_mod_1789477527.client.ClientHooks
                        .applyClockState(msg.timeLocked, msg.weatherLocked, msg.dayTime)));
        context.setPacketHandled(true);
    }
}
