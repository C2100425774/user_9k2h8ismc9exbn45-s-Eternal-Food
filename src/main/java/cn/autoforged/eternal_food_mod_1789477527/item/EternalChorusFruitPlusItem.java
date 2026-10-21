package cn.autoforged.eternal_food_mod_1789477527.item;

import cn.autoforged.eternal_food_mod_1789477527.network.ModNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * 永恒紫颂果plus：食用后无论身处哪个维度，都把玩家传送到其出生点（床 / 重生锚），
 * 没有出生点时回退到主世界世界出生点。和所有永恒食物一样食用后不消耗、无冷却。
 *
 * <p>基础物品用苹果：{@link EternalFoodItem} 对 {@code Items.CHORUS_FRUIT} 会复刻原版的
 * <em>随机</em>传送，那不是这里想要的行为，所以换一个普通食物基类，由本类自行实现定点传送。
 */
public class EternalChorusFruitPlusItem extends EternalFoodItem {

    public EternalChorusFruitPlusItem(Properties properties) {
        super(Items.APPLE, properties, false, "tooltip.eternal_food_mod_1789477527.chorus_fruit_plus");
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        ItemStack result = super.finishUsingItem(stack, level, entity);
        if (!level.isClientSide && entity instanceof ServerPlayer player) {
            teleportToSpawn(player);
            // 传送的同时让客户端用本物品播放一次不死图腾激活动画，避免误以为已经吃完。
            ModNetwork.sendTotemAnimation(player, stack.copy());
        }
        return result;
    }

    /** 把玩家传送到其重生点；没有有效重生点则传到主世界世界出生点（仅服务端调用）。 */
    private static void teleportToSpawn(ServerPlayer player) {
        ServerLevel target = player.getServer() == null
                ? null
                : player.getServer().getLevel(player.getRespawnDimension());
        Vec3 destination = null;
        if (target != null && player.getRespawnPosition() != null) {
            // 末参 true = 不消耗重生锚充能，只解析落点。
            destination = Player.findRespawnPositionAndUseSpawnBlock(target, player.getRespawnPosition(),
                    player.getRespawnAngle(), player.isRespawnForced(), true).orElse(null);
        }
        if (destination == null) {
            target = player.getServer().overworld();
            BlockPos spawn = target.getSharedSpawnPos();
            destination = new Vec3(spawn.getX() + 0.5D, spawn.getY(), spawn.getZ() + 0.5D);
        }
        player.stopRiding();
        player.teleportTo(target, destination.x, destination.y, destination.z,
                player.getYRot(), player.getXRot());
        target.playSound(null, destination.x, destination.y, destination.z,
                SoundEvents.CHORUS_FRUIT_TELEPORT, SoundSource.PLAYERS, 1.0F, 1.0F);
    }
}
