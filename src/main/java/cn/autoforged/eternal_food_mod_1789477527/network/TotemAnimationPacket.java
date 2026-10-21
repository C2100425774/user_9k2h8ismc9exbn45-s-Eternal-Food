package cn.autoforged.eternal_food_mod_1789477527.network;

import cn.autoforged.eternal_food_mod_1789477527.client.ClientPacketHandler;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/** 服务端 → 客户端：让客户端用携带的物品播放一次不死图腾激活动画。 */
public class TotemAnimationPacket {

    private final ItemStack stack;

    public TotemAnimationPacket(ItemStack stack) {
        this.stack = stack;
    }

    public static void encode(TotemAnimationPacket msg, FriendlyByteBuf buf) {
        buf.writeItem(msg.stack);
    }

    public static TotemAnimationPacket decode(FriendlyByteBuf buf) {
        return new TotemAnimationPacket(buf.readItem());
    }

    public static void handle(TotemAnimationPacket msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClientPacketHandler.showTotemAnimation(msg.stack)));
        context.setPacketHandled(true);
    }
}
