package cn.autoforged.eternal_food_mod_1789477527.network;

import cn.autoforged.eternal_food_mod_1789477527.EternalFoodMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

/**
 * 本 mod 的网络通道。
 *
 * <p>目前只有一件事需要走网络：让客户端用指定物品播放一次原版「不死图腾」激活动画
 * （{@code GameRenderer#displayItemActivation}）。原版该动画由实体事件 35 触发，但客户端
 * 固定用图腾物品的贴图（{@code ClientPacketListener#findTotem}），无法换成永恒紫颂果，
 * 因此自己发包把要显示的物品传给客户端。
 */
public final class ModNetwork {

    private static final String PROTOCOL_VERSION = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(EternalFoodMod.MOD_ID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals);

    private ModNetwork() {
    }

    public static void register() {
        int id = 0;
        CHANNEL.registerMessage(id++, TotemAnimationPacket.class,
                TotemAnimationPacket::encode,
                TotemAnimationPacket::decode,
                TotemAnimationPacket::handle);
        CHANNEL.registerMessage(id++, ClockActionPacket.class,
                ClockActionPacket::encode,
                ClockActionPacket::decode,
                ClockActionPacket::handle);
        CHANNEL.registerMessage(id++, ClockStatePacket.class,
                ClockStatePacket::encode,
                ClockStatePacket::decode,
                ClockStatePacket::handle);
        CHANNEL.registerMessage(id++, ClockTimePacket.class,
                ClockTimePacket::encode,
                ClockTimePacket::decode,
                ClockTimePacket::handle);
    }

    /** 让指定玩家客户端用 {@code stack} 播放一次不死图腾激活动画（仅服务端调用）。 */
    public static void sendTotemAnimation(ServerPlayer player, ItemStack stack) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new TotemAnimationPacket(stack));
    }

    /** 把时钟面板的锁定状态同步给客户端（仅服务端调用）。 */
    public static void sendClockState(ServerPlayer player, boolean timeLocked, boolean weatherLocked,
            long dayTime) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new ClockStatePacket(timeLocked, weatherLocked, dayTime));
    }

    /** 每秒把服务端的 dayTime 与锁定状态推给客户端（仅服务端调用）。 */
    public static void sendClockTime(ServerPlayer player, long dayTime, boolean timeLocked,
            boolean weatherLocked) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new ClockTimePacket(dayTime, timeLocked, weatherLocked));
    }
}
