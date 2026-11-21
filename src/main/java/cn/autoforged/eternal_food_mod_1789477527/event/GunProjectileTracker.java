package cn.autoforged.eternal_food_mod_1789477527.event;

import cn.autoforged.eternal_food_mod_1789477527.EternalFoodMod;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
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
 * 发射器枪发射的原版 / 模组弹射物需要的外部记录。
 *
 * <p>力量附魔：
 * <ul>
 *   <li>5 级：关闭重力并把速度每 tick 固定为出膛速度 → 绝对直线、不会上下抖；</li>
 *   <li>1~4 级：每 tick 补回「重力 × power/5」（减重）+ 补回水平阻力的一部分（减阻）。</li>
 * </ul>
 * 另外还负责：无限附魔箭落地 0.5 秒消失、激流水里无视阻力。
 */
@Mod.EventBusSubscriber(modid = EternalFoodMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class GunProjectileTracker {

    /** 无限附魔：箭停住后的存活时间（0.5 秒）。 */
    public static final int INFINITE_STUCK_TICKS = 10;
    /** 无限附魔弹射物的安全寿命上限（防止卡在奇怪位置永远不消失）。 */
    public static final int INFINITE_MAX_TICKS = 200;

    private static final Set<Entity> INFINITE = Collections.synchronizedSet(
            Collections.newSetFromMap(new WeakHashMap<>()));
    private static final Map<Entity, Integer> STUCK_TICKS = Collections.synchronizedMap(new WeakHashMap<>());
    private static final Set<Entity> RIPTIDE = Collections.synchronizedSet(
            Collections.newSetFromMap(new WeakHashMap<>()));
    private static final Map<Entity, Integer> POWERED = Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<Entity, Vec3> LAUNCH = Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<Entity, Vec3> PRE_TICK = Collections.synchronizedMap(new WeakHashMap<>());

    private GunProjectileTracker() {
    }

    public static void markInfinite(Entity entity) {
        INFINITE.add(entity);
    }

    public static void markRiptide(Entity entity) {
        RIPTIDE.add(entity);
    }

    /** 力量附魔：记录等级与出膛速度。 */
    public static void markPowered(Entity entity, int level) {
        int clamped = Math.max(0, Math.min(5, level));
        if (clamped > 0) {
            POWERED.put(entity, clamped);
            LAUNCH.put(entity, entity.getDeltaMovement());
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
            for (Entity entity : new ArrayList<>(POWERED.keySet())) {
                if (!entity.isAlive() || entity.level() != level) {
                    continue;
                }
                int power = POWERED.get(entity);
                if (power >= 5) {
                    // 5 级：关重力 + 固定速度，走绝对直线
                    entity.setNoGravity(true);
                    Vec3 launch = LAUNCH.get(entity);
                    if (launch != null) {
                        entity.setDeltaMovement(launch);
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
        Iterator<Entity> infiniteIt = INFINITE.iterator();
        while (infiniteIt.hasNext()) {
            Entity entity = infiniteIt.next();
            if (!entity.isAlive() || entity.level() != level) {
                infiniteIt.remove();
                STUCK_TICKS.remove(entity);
                continue;
            }
            boolean stuck = entity instanceof AbstractArrow arrow
                    && (arrow.onGround() || arrow.getDeltaMovement().lengthSqr() < 0.0025);
            if (entity.tickCount >= INFINITE_MAX_TICKS) {
                stuck = true;
            }
            if (stuck) {
                int ticks = STUCK_TICKS.getOrDefault(entity, 0) + 1;
                if (ticks >= INFINITE_STUCK_TICKS) {
                    entity.discard();
                    infiniteIt.remove();
                    STUCK_TICKS.remove(entity);
                } else {
                    STUCK_TICKS.put(entity, ticks);
                }
            }
        }
        // 力量 1~4：补回一部分重力 + 一部分水平阻力（不做整体还原，避免上下抖）
        for (Entity entity : new ArrayList<>(POWERED.keySet())) {
            Vec3 before = PRE_TICK.remove(entity);
            if (!entity.isAlive() || entity.level() != level) {
                POWERED.remove(entity);
                LAUNCH.remove(entity);
                continue;
            }
            if (before == null) {
                continue;
            }
            int power = POWERED.get(entity);
            double f = power / 5.0;
            Vec3 now = entity.getDeltaMovement();
            double gravity = gravityOf(entity) * f;
            double nx = now.x + (before.x - now.x) * f;
            double nz = now.z + (before.z - now.z) * f;
            entity.setDeltaMovement(nx, now.y + gravity, nz);
        }
        // 激流：水中无视阻力（最后应用）
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
}
