package cn.autoforged.eternal_food_mod_1789477527.network;

import cn.autoforged.eternal_food_mod_1789477527.client.GunConfigScreen;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/** 服务端要求客户端打开发射器枪配置界面（Shift+右键）。 */
public class OpenGunConfigPacket {

    private final ItemStack gun;

    public OpenGunConfigPacket(ItemStack gun) {
        this.gun = gun;
    }

    public static void encode(OpenGunConfigPacket packet, FriendlyByteBuf buffer) {
        buffer.writeItem(packet.gun);
    }

    public static OpenGunConfigPacket decode(FriendlyByteBuf buffer) {
        return new OpenGunConfigPacket(buffer.readItem());
    }

    public static void handle(OpenGunConfigPacket packet, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> GunConfigScreen.open(packet.gun)));
        context.setPacketHandled(true);
    }
}
