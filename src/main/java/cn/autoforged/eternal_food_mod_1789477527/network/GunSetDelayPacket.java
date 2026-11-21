package cn.autoforged.eternal_food_mod_1789477527.network;

import cn.autoforged.eternal_food_mod_1789477527.EternalFoodMod;
import cn.autoforged.eternal_food_mod_1789477527.menu.EternalDispenserGunMenu;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

/** 客户端拖动发射频率滑条时，把新的间隔（tick）告诉服务端。 */
public class GunSetDelayPacket {

    private final int delay;

    public GunSetDelayPacket(int delay) {
        this.delay = delay;
    }

    public static void encode(GunSetDelayPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.delay);
    }

    public static GunSetDelayPacket decode(FriendlyByteBuf buffer) {
        return new GunSetDelayPacket(buffer.readVarInt());
    }

    public static void handle(GunSetDelayPacket packet, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null && player.containerMenu instanceof EternalDispenserGunMenu menu) {
                menu.applyDelay(packet.delay);
            }
        });
        context.setPacketHandled(true);
    }
}
