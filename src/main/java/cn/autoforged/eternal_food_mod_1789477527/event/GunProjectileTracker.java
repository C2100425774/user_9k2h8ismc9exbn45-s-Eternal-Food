package cn.autoforged.eternal_food_mod_1789477527.event;

import cn.autoforged.eternal_food_mod_1789477527.EternalFoodMod;
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
 * 发射器枪发射的原版 / 模组弹射物需要的外部记录：
 * <ul>
 *   <li>力量附魔：按等级减少弹射物的空气阻力与重力；
 *       V 级时每 tick 把速度还原成 tick 前的值 = 无阻力、无重力、指哪打哪；</li>
 *   <li>无限附魔：箭类停住 0.5 秒后消失，避免连发卡顿；</li>
 *   <li>激流附魔：在水中把速度还原成 tick 前 = 无视水阻力。</li>
 * </ul>
 * 未附魔时不跟踪，弹射物行为与原版一致。
 */
@Mod.EventBusSubscriber(modid = EternalFoodMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class GunProjectileTracker {

    /** 无限附魔：箭停住后的存活时间（0.5 秒）。 */
    public static final int INFINITE_STUCK_TICKS = 10;

    private static final Set<Entity> INFINITE = Collections.synchronizedSet(
            Collections.newSetFromMap(new WeakHashMap<>()));
    private static final Map<Entity, Integer> STUCK_TICKS = Collections.synchronizedMap(new WeakHashMap<>());
    private static final Set<Entity> RIPTIDE = Collections.synchronizedSet(
            Collections.newSetFromMap(new WeakHashMap<>()));
    private static final Map<Entity, Integer> POWERED = Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<Entity, Vec3> PRE_TICK = Collections.synchronizedMap(new WeakHashMap<>());

    private GunProjectileTracker() {
    }

    /** 无限附魔发射的弹射物：箭类停住 0.5 秒后丢弃。 */
    public static void markInfinite(Entity entity) {
        INFINITE.add(entity);
    }

    /** 激流附魔发射的弹射物：水里无视阻力。 */
    public static void markRiptide(Entity entity) {
        RIPTIDE.add(entity);
    }

    /** 力量附魔发射的弹射物：减少空气阻力 + 重力（等级 1~5）。 */
    public static void markPowered(Entity entity, int level) {
        int clamped = Math.max(0, Math.min(5, level));
        if (clamped > 0) {
            POWERED.put(entity, clamped);
        }
    }

    /** 每 tick 的重力（不同弹射物不一样）。 */
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
            // 先记录 tick 前速度（力量和激流都用它作为目标速度）
            for (Entity entity : new java.util.ArrayList<>(POWERED.keySet())) {
                if (entity.isAlive() && entity.level() == level) {
                    Vec3 before = entity.getDeltaMovement();
                    PRE_TICK.put(entity, before);
                    int power = POWERED.get(entity);
                    double f = power / 5.0;
                    double gravity = gravityOf(entity) * f;
                    if (gravity > 0.0) {
                        entity.setDeltaMovement(before.add(0.0, gravity, 0.0));
                    }
                }
            }
            for (Entity entity : new java.util.ArrayList<>(RIPTIDE)) {
                if (entity.isAlive() && entity.level() == level && entity.isInWater()
                        && !PRE_TICK.containsKey(entity)) {
                    PRE_TICK.put(entity, entity.getDeltaMovement());
                }
            }
            return;
        }
        // END
        Iterator<Entity> infiniteIt = INFINITE.iterator();
        while (infiniteIt.hasNext()) {
            Entity entity = infiniteIt.next();
            if (!entity.isAlive() || entity.level() != level) {
                infiniteIt.remove();
                STUCK_TICKS.remove(entity);
                continue;
            }
            if (entity instanceof AbstractArrow arrow && arrow.getDeltaMovement().lengthSqr() < 1.0E-4) {
                int ticks = STUCK_TICKS.getOrDefault(entity, 0) + 1;
                if (ticks >= INFINITE_STUCK_TICKS) {
                    arrow.discard();
                    infiniteIt.remove();
                    STUCK_TICKS.remove(entity);
                } else {
                    STUCK_TICKS.put(entity, ticks);
                }
            }
        }
        // 力量：把本 tick 损失的「空气阻力 + 重力」补回 power/5（5 级 = 完全还原）
        for (Entity entity : new java.util.ArrayList<>(POWERED.keySet())) {
            Vec3 before = PRE_TICK.remove(entity);
            if (!entity.isAlive() || entity.level() != level) {
                POWERED.remove(entity);
                continue;
            }
            if (before == null) {
                continue;
            }
            int power = POWERED.get(entity);
            double f = power / 5.0;
            Vec3 now = entity.getDeltaMovement();
            entity.setDeltaMovement(now.add(before.subtract(now).scale(f)));
        }
        // 激流：水里无视阻力（最后应用，优先级最高）
        for (Entity entity : new java.util.ArrayList<>(RIPTIDE)) {
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
