package cn.autoforged.eternal_food_mod_1789477527.network;

import cn.autoforged.eternal_food_mod_1789477527.item.EternalDispenserGunItem;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

/** 左键发射：客户端按住左键时每 tick 发一次，服务端按发射间隔限速。 */
public class GunFirePacket {

    public static void encode(GunFirePacket packet, FriendlyByteBuf buffer) {
    }

    public static GunFirePacket decode(FriendlyByteBuf buffer) {
        return new GunFirePacket();
    }

    public static void handle(GunFirePacket packet, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                EternalDispenserGunItem.tryFire(player);
            }
        });
        context.setPacketHandled(true);
    }
}
