package cn.autoforged.eternal_food_mod_1789477527.network;

import cn.autoforged.eternal_food_mod_1789477527.item.EternalDispenserGunItem;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

/** 配置界面里改动开关 / 力量等级时同步到服务端。 */
public class GunConfigPacket {

    private final String key;
    private final int value;

    public GunConfigPacket(String key, int value) {
        this.key = key;
        this.value = value;
    }

    public static void encode(GunConfigPacket packet, FriendlyByteBuf buffer) {
        buffer.writeUtf(packet.key);
        buffer.writeVarInt(packet.value);
    }

    public static GunConfigPacket decode(FriendlyByteBuf buffer) {
        return new GunConfigPacket(buffer.readUtf(), buffer.readVarInt());
    }

    public static void handle(GunConfigPacket packet, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) {
                return;
            }
            ItemStack gun = player.getMainHandItem();
            EternalDispenserGunItem.applyConfig(gun, packet.key, packet.value);
            // 改的是物品 NBT，主动标记并广播一次，保证关闭界面后客户端/存档都拿到新值
            player.getInventory().setChanged();
            player.containerMenu.broadcastChanges();
        });
        context.setPacketHandled(true);
    }
}
