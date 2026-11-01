package cn.autoforged.eternal_food_mod_1789477527.util;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

/**
 * 找一个「附近的安全位置」并把玩家传送过去。
 *
 * <p>思路和紫颂果一致：在玩家周围 ±16 格内随机挑一列，从上往下扫，
 * 找到第一个「上方两格是空的」的实心方块，把玩家放到它顶上。
 * 区别是这里不会用玩家当前的 Y 去偏移（人已经掉到虚空里了，按 Y 偏移只会继续往下掉）。
 *
 * <p>找不到安全点时**不自己兜底**，而是返回 false，由调用方改走别的方案
 * （紫颂果 plus 的虚空救援会退化成"触发主动效果"）。
 */
public final class SafeTeleport {

    private static final int ATTEMPTS = 48;
    private static final int RADIUS = 16;

    private SafeTeleport() {
    }

    /**
     * 尝试把玩家传送到附近的安全位置（仅服务端调用）。
     *
     * @return true 表示已成功传送；false 表示附近找不到安全位置，玩家没动
     */
    public static boolean rescueNearby(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        RandomSource random = player.getRandom();

        for (int attempt = 0; attempt < ATTEMPTS; attempt++) {
            int dx = random.nextInt(RADIUS * 2 + 1) - RADIUS;
            int dz = random.nextInt(RADIUS * 2 + 1) - RADIUS;
            if (dx == 0 && dz == 0) {
                continue;
            }
            BlockPos destination = findSafePos(level,
                    player.getBlockX() + dx, player.getBlockZ() + dz);
            if (destination != null) {
                teleport(player, level, destination);
                return true;
            }
        }
        return false;
    }

    /** 最后兜底：世界出生点上方的地表（仅服务端调用）。 */
    public static void fallbackToSpawn(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        BlockPos spawn = level.getSharedSpawnPos();
        BlockPos destination = findSafePos(level, spawn.getX(), spawn.getZ());
        if (destination == null) {
            destination = new BlockPos(spawn.getX(), level.getHeight(
                    Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, spawn.getX(), spawn.getZ()), spawn.getZ());
        }
        teleport(player, level, destination);
    }

    /** 在 (x,z) 这一列从上往下找安全落点；找不到返回 null。 */
    private static BlockPos findSafePos(ServerLevel level, int x, int z) {
        // 从 3/4 世界高度往下扫，而不是从最顶部：
        // 下界顶部是基岩天花板，从最顶上扫会把玩家放到天花板上面（那上面是空的），
        // 会导致"传送到虚空 → 再次掉进虚空"的死循环。
        int minY = level.getMinBuildHeight();
        int startY = minY + level.getLogicalHeight() * 3 / 4;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int y = startY; y > minY + 2; y--) {
            cursor.set(x, y, z);
            BlockState floor = level.getBlockState(cursor);
            // 需要实心、且不是液体（避免落进岩浆 / 水里）
            if (!floor.blocksMotion() || !floor.getFluidState().isEmpty()) {
                continue;
            }
            BlockPos stand = cursor.above();
            if (isFree(level, stand) && isFree(level, stand.above())) {
                return stand.immutable();
            }
        }
        return null;
    }

    /** 该位置没有碰撞箱也没有液体，可以站人。 */
    private static boolean isFree(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.getFluidState().isEmpty() && state.getCollisionShape(level, pos).isEmpty();
    }

    private static void teleport(ServerPlayer player, ServerLevel level, BlockPos pos) {
        player.stopRiding();
        player.setDeltaMovement(Vec3.ZERO);
        player.resetFallDistance();
        player.teleportTo(level, pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D,
                player.getYRot(), player.getXRot());
        level.playSound(null, pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D,
                SoundEvents.CHORUS_FRUIT_TELEPORT, SoundSource.PLAYERS, 1.0F, 1.0F);
    }
}
