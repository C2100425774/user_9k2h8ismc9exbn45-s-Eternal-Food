package cn.autoforged.eternal_food_mod_1789477527.event;

import cn.autoforged.eternal_food_mod_1789477527.EternalFoodMod;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.entity.projectile.ThrownPotion;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 发射器枪发射的原版 / 模组弹射物的外部记录。
 *
 * <ul>
 *   <li>力量：1~4 级按比例补回重力和水平阻力；5 级只关重力（不再每 tick 改速度，
 *       避免命中无敌目标被弹开后又被推回去）；被弹开（速度反向）时直接改成垂直落地；</li>
 *   <li>无限：箭类落地/卡住很快消失，区块卸载直接消失；</li>
 *   <li>防水：水里无视阻力。</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = EternalFoodMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class GunProjectileTracker {

    /** 无限附魔：箭类停住后的存活时间（0.25 秒）。 */
    public static final int INFINITE_STUCK_TICKS = 5;
    /** 无限附魔弹射物的安全寿命上限（2.5 秒）。 */
    public static final int INFINITE_MAX_TICKS = 50;
    /** 力量弹射物的安全寿命上限（20 秒）。 */
    public static final int POWERED_MAX_TICKS = 400;

    private static final Set<Entity> INFINITE = Collections.synchronizedSet(
            Collections.newSetFromMap(new WeakHashMap<>()));
    private static final Map<Entity, Integer> STUCK_TICKS = Collections.synchronizedMap(new WeakHashMap<>());
    private static final Set<Entity> RIPTIDE = Collections.synchronizedSet(
            Collections.newSetFromMap(new WeakHashMap<>()));
    private static final Map<Entity, Integer> POWERED = Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<Entity, Vec3> LAUNCH = Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<Entity, Vec3> PRE_TICK = Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<Entity, Vec3> LAST_POS = Collections.synchronizedMap(new WeakHashMap<>());

    private GunProjectileTracker() {
    }

    public static void markInfinite(Entity entity) {
        INFINITE.add(entity);
        entity.getPersistentData().putBoolean("EternalGunInfinite", true);
    }

    public static void markRiptide(Entity entity) {
        RIPTIDE.add(entity);
    }

    public static void markPowered(Entity entity, int level) {
        int clamped = Math.max(0, Math.min(5, level));
        if (clamped > 0) {
            POWERED.put(entity, clamped);
            Vec3 velocity = entity.getDeltaMovement();
            if (velocity.lengthSqr() > 1.0E-6) {
                LAUNCH.put(entity, velocity.normalize());
            }
        }
    }

    private static double gravityOf(Entity entity) {
        if (entity instanceof AbstractArrow) {
            return 0.05;
        }
        if (entity instanceof ThrownPotion) {
            return 0.05;
        }
        if (entity instanceof ThrowableProjectile) {
            return 0.03;
        }
        return 0.0;
    }

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (!(event.level instanceof ServerLevel level)) {
            return;
        }
        if (event.phase == TickEvent.Phase.START) {
            // 力量 V：只关重力；1~4 级记录 tick 前速度
            for (Entity entity : new ArrayList<>(POWERED.keySet())) {
                if (!entity.isAlive() || entity.level() != level) {
                    continue;
                }
                int power = POWERED.get(entity);
                if (power >= 5) {
                    entity.setNoGravity(true);
                    if (entity.tickCount >= POWERED_MAX_TICKS) {
                        entity.discard();
                    }
                } else {
                    PRE_TICK.put(entity, entity.getDeltaMovement());
                }
            }
            for (Entity entity : new ArrayList<>(RIPTIDE)) {
                if (entity.isAlive() && entity.level() == level && entity.isInWater()
                        && !PRE_TICK.containsKey(entity)) {
                    PRE_TICK.put(entity, entity.getDeltaMovement());
                }
            }
            return;
        }
        // END -----------------------------------------------------------------
        for (Entity entity : new ArrayList<>(INFINITE)) {
            if (!entity.isAlive() || entity.level() != level) {
                INFINITE.remove(entity);
                STUCK_TICKS.remove(entity);
                LAST_POS.remove(entity);
                continue;
            }
            boolean unloaded = !level.hasChunkAt(entity.blockPosition());
            Vec3 pos = entity.position();
            Vec3 last = LAST_POS.put(entity, pos);
            boolean noMove = last != null && last.distanceToSqr(pos) < 0.05;
            boolean onGround = entity.onGround();
            boolean inSolid = level.getBlockState(entity.blockPosition()).isSolid();
            boolean tinySpeed = entity instanceof AbstractArrow arrow
                    && arrow.getDeltaMovement().lengthSqr() < 1.0E-4;
            boolean stuckNow = noMove || onGround || inSolid || tinySpeed;
            int old = STUCK_TICKS.getOrDefault(entity, 0);
            int stillTicks = stuckNow ? old + 1 : Math.max(0, old - 1);
            if (unloaded || stillTicks >= INFINITE_STUCK_TICKS
                    || entity.tickCount >= INFINITE_MAX_TICKS) {
                entity.discard();
                INFINITE.remove(entity);
                STUCK_TICKS.remove(entity);
                LAST_POS.remove(entity);
            } else {
                STUCK_TICKS.put(entity, stillTicks);
            }
        }
        // 力量弹射物：被弹开就垂直落地；1~4 级补回重力和水平阻力
        for (Entity entity : new ArrayList<>(POWERED.keySet())) {
            if (!entity.isAlive() || entity.level() != level) {
                PRE_TICK.remove(entity);
                POWERED.remove(entity);
                LAUNCH.remove(entity);
                continue;
            }
            if (entity.tickCount >= POWERED_MAX_TICKS) {
                entity.discard();
                PRE_TICK.remove(entity);
                POWERED.remove(entity);
                LAUNCH.remove(entity);
                continue;
            }
            Vec3 launch = LAUNCH.get(entity);
            Vec3 now = entity.getDeltaMovement();
            if (launch != null && now.dot(launch) < -0.01) {
                // 命中无敌目标被弹开：直接垂直落地，避免原路飞回
                entity.setDeltaMovement(0.0, -1.0, 0.0);
                entity.setNoGravity(false);
                PRE_TICK.remove(entity);
                continue;
            }
            Vec3 before = PRE_TICK.remove(entity);
            if (before == null) {
                continue;
            }
            int power = POWERED.get(entity);
            double f = power / 5.0;
            double gravity = gravityOf(entity) * f;
            double nx = now.x + (before.x - now.x) * f;
            double nz = now.z + (before.z - now.z) * f;
            entity.setDeltaMovement(nx, now.y + gravity, nz);
        }
        // 防水：水中无视阻力
        for (Entity entity : new ArrayList<>(RIPTIDE)) {
            Vec3 before = PRE_TICK.remove(entity);
            if (!entity.isAlive() || entity.level() != level) {
                continue;
            }
            if (before != null && entity.isInWater()) {
                entity.setDeltaMovement(before);
            }
        }
    }

    /** 每 2 tick 扫描一次所有已加载实体，兜底清理带持久标记的无限弹射物。 */
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        if (event.getServer().getTickCount() % 2 != 0) {
            return;
        }
        for (ServerLevel level : event.getServer().getAllLevels()) {
            List<Entity> toDiscard = new ArrayList<>();
            for (Entity entity : level.getAllEntities()) {
                if (entity == null || !entity.isAlive()
                        || !entity.getPersistentData().getBoolean("EternalGunInfinite")) {
                    continue;
                }
                boolean stuck = entity.tickCount >= INFINITE_MAX_TICKS || isStuck(entity, level);
                if (!stuck) {
                    entity.getPersistentData().putInt("EternalGunStuck", 0);
                    continue;
                }
                int count = entity.getPersistentData().getInt("EternalGunStuck") + 1;
                if (count >= 2) {
                    toDiscard.add(entity);
                } else {
                    entity.getPersistentData().putInt("EternalGunStuck", count);
                }
            }
            for (Entity entity : toDiscard) {
                entity.discard();
            }
        }
    }

    private static boolean isStuck(Entity entity, ServerLevel level) {
        return entity.onGround()
                || level.getBlockState(entity.blockPosition()).isSolid()
                || entity.getDeltaMovement().lengthSqr() < 1.0E-4;
    }
}
