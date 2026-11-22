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
    private static final Map<Entity, Integer> POWER_STILL = Collections.synchronizedMap(new WeakHashMap<>());
    /** 3 级及以上的无重力飞行剩余 tick。 */
    private static final Map<Entity, Integer> FLIGHT = Collections.synchronizedMap(new WeakHashMap<>());
    /** 发射时的朝向，用于识别原版弹开（弹开会把 yRot 翻转 180°）。 */
    private static final Map<Entity, Float> LAUNCH_YAW = Collections.synchronizedMap(new WeakHashMap<>());
    private static final org.slf4j.Logger LOGGER =
            org.slf4j.LoggerFactory.getLogger("eternal_food_gun");

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
        if (clamped <= 0) {
            return;
        }
        POWERED.put(entity, clamped);
        LAUNCH_YAW.put(entity, entity.getYRot());
        LOGGER.info("[gun] markPowered level={} entity={}", clamped, entity.getType());
        Vec3 velocity = entity.getDeltaMovement();
        if (velocity.lengthSqr() > 1.0E-6) {
            LAUNCH.put(entity, velocity.normalize());
        }
        if (clamped >= 3) {
            // 3 级及以上：飞行期间无重力，带飞行寿命
            entity.setNoGravity(true);
            FLIGHT.put(entity, flightTicks(clamped));
        }
    }

    private static int flightTicks(int level) {
        return 40;
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
                // 只记录速度用于「被弹开/停住」判定；3 级及以上飞行中无重力
                PRE_TICK.put(entity, entity.getDeltaMovement());
                entity.setNoGravity(FLIGHT.containsKey(entity));
                if (entity.tickCount >= POWERED_MAX_TICKS) {
                    entity.discard();
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
                FLIGHT.remove(entity);
                LAUNCH_YAW.remove(entity);
                continue;
            }
            if (entity.tickCount >= POWERED_MAX_TICKS) {
                entity.discard();
                PRE_TICK.remove(entity);
                POWERED.remove(entity);
                LAUNCH.remove(entity);
                FLIGHT.remove(entity);
                LAUNCH_YAW.remove(entity);
                continue;
            }
            Vec3 launch = LAUNCH.get(entity);
            Vec3 now = entity.getDeltaMovement();
            Vec3 before = PRE_TICK.remove(entity);
            // 被弹开 = 速度方向反转（相对发射方向或上一 tick 速度），或速度骤降
            boolean deflected = (launch != null && now.dot(launch) < -0.01)
                    || (before != null && now.dot(before) < 0.0);
            Float launchYaw = LAUNCH_YAW.get(entity);
            if (launchYaw != null
                    && Math.abs(net.minecraft.util.Mth.wrapDegrees(entity.getYRot() - launchYaw)) > 90.0F) {
                // 原版弹开会把箭的朝向翻转 180°，这个信号最可靠
                deflected = true;
            }
            if (before != null && before.lengthSqr() > 0.01
                    && now.lengthSqr() < before.lengthSqr() * 0.25) {
                deflected = true;
            }
            if (deflected) {
                LOGGER.info("[gun] DEFLECT level={} entity={} now={}", POWERED.get(entity), entity.getType(), now);
                // 不管伤害有没有打出来：立刻恢复重力并结束飞行寿命
                entity.setNoGravity(false);
                entity.setDeltaMovement(0.0, -0.5, 0.0);
                FLIGHT.remove(entity);
                LAUNCH_YAW.remove(entity);
                POWER_STILL.remove(entity);
                continue;
            }
            if (before == null) {
                continue;
            }
            // 停在空中不动的力量弹射物：短暂停留后直接清掉，避免一直悬浮
            if (!entity.onGround() && now.lengthSqr() < 4.0E-4) {
                int still = POWER_STILL.merge(entity, 1, Integer::sum);
                if (still >= 20) {
                    entity.discard();
                }
                continue;
            }
            POWER_STILL.remove(entity);
            int power = POWERED.get(entity);
            if (power >= 3) {
                // 无重力飞行寿命：结束时恢复重力，子弹开始下坠
                int remaining = FLIGHT.getOrDefault(entity, 0) - 1;
                if (remaining <= 0) {
                    entity.setNoGravity(false);
                    FLIGHT.remove(entity);
                LAUNCH_YAW.remove(entity);
                } else {
                    FLIGHT.put(entity, remaining);
                }
            }
            // 3 级子弹落地后统一 5 tick 消失（无论是否无限，寿命一致）
            if (power >= 3 && !FLIGHT.containsKey(entity)) {
                boolean stuck = entity.onGround()
                        || level.getBlockState(entity.blockPosition()).isSolid()
                        || now.lengthSqr() < 0.0025;
                int count = STUCK_TICKS.getOrDefault(entity, 0);
                if (stuck) {
                    if (count + 1 >= 5) {
                        entity.discard();
                    } else {
                        STUCK_TICKS.put(entity, count + 1);
                    }
                } else {
                    STUCK_TICKS.remove(entity);
                }
            }
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
