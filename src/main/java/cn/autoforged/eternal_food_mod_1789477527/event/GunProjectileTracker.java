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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 发射器枪弹射物的辅助逻辑。
 *
 * <ul>
 *   <li>3 级「子弹」：关掉原版箭碰撞（noPhysics），每 tick 自己用扫掠 AABB 检测生物、
 *       用 clip 检测方块；命中生物时清掉无敌帧、手动造成伤害，然后立即 discard。
 *       不经过原版箭的 onHitEntity，所以不会出现「hurt 失败 → 速度反向」的弹开。</li>
 *   <li>无限：箭类落地/卡住很快消失，区块卸载直接消失。</li>
 *   <li>防水：水里无视阻力。</li>
 *   <li>兜底：其它等级被弹开时恢复重力并向下掉。</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = EternalFoodMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class GunProjectileTracker {

    public static final int INFINITE_STUCK_TICKS = 5;
    public static final int INFINITE_MAX_TICKS = 50;
    public static final int POWERED_MAX_TICKS = 400;

    private static final Set<Entity> INFINITE = Collections.synchronizedSet(
            Collections.newSetFromMap(new WeakHashMap<>()));
    private static final Map<Entity, Integer> STUCK_TICKS = Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<Entity, Integer> POWERED = Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<Entity, Vec3> LAUNCH = Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<Entity, Vec3> PRE_TICK = Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<Entity, Vec3> LAST_POS = Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<Entity, Integer> POWER_STILL = Collections.synchronizedMap(new WeakHashMap<>());

    private GunProjectileTracker() {
    }

    public static void markInfinite(Entity entity) {
        INFINITE.add(entity);
        entity.getPersistentData().putBoolean("EternalGunInfinite", true);
    }

    public static void markPowered(Entity entity, int level) {
        int clamped = Math.max(0, Math.min(3, level));
        if (clamped <= 0) {
            return;
        }
        POWERED.put(entity, clamped);
        Vec3 velocity = entity.getDeltaMovement();
        if (velocity.lengthSqr() > 1.0E-6) {
            LAUNCH.put(entity, velocity.normalize());
        }
    }

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (!(event.level instanceof ServerLevel level)) {
            return;
        }
        if (event.phase == TickEvent.Phase.START) {
            for (Entity entity : new ArrayList<>(POWERED.keySet())) {
                if (!entity.isAlive() || entity.level() != level) {
                    continue;
                }
                PRE_TICK.put(entity, entity.getDeltaMovement());
                if (entity.tickCount >= POWERED_MAX_TICKS) {
                    entity.discard();
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
            boolean stuckNow = noMove || entity.onGround()
                    || level.getBlockState(entity.blockPosition()).isSolid()
                    || (entity instanceof AbstractArrow arrow
                            && arrow.getDeltaMovement().lengthSqr() < 1.0E-4);
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
        for (Entity entity : new ArrayList<>(POWERED.keySet())) {
            if (!entity.isAlive() || entity.level() != level) {
                cleanup(entity);
                continue;
            }
            if (entity.tickCount >= POWERED_MAX_TICKS) {
                entity.discard();
                cleanup(entity);
                continue;
            }
            Vec3 launch = LAUNCH.get(entity);
            Vec3 now = entity.getDeltaMovement();
            Vec3 before = PRE_TICK.remove(entity);
            boolean deflected = (launch != null && now.dot(launch) < -0.01)
                    || (before != null && now.dot(before) < 0.0);
            if (before != null && before.lengthSqr() > 0.01
                    && now.lengthSqr() < before.lengthSqr() * 0.25) {
                deflected = true;
            }
            if (deflected) {
                entity.setNoGravity(false);
                entity.setDeltaMovement(0.0, -0.5, 0.0);
                continue;
            }
            if (before == null) {
                continue;
            }
            if (!entity.onGround() && now.lengthSqr() < 4.0E-4) {
                int still = POWER_STILL.merge(entity, 1, Integer::sum);
                if (still >= 20) {
                    entity.discard();
                }
            } else {
                POWER_STILL.remove(entity);
            }
        }
    }

    private static void cleanup(Entity entity) {
        PRE_TICK.remove(entity);
        POWERED.remove(entity);
        LAUNCH.remove(entity);
        POWER_STILL.remove(entity);
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
