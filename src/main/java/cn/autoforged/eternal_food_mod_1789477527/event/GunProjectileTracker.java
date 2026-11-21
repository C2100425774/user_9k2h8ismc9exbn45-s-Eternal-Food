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
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 发射器枪发射的原版 / 模组弹射物需要的外部记录：
 * <ul>
 *   <li>无限附魔：箭类在地上停住 0.5 秒（10 tick）后消失，避免连续发射时实体堆积卡顿；
 *       飞行中的弹射物与原版一样正常存在；</li>
 *   <li>激流附魔：在水里时把速度恢复成 tick 前的值，等于无视水阻力（只有 1 级效果）。</li>
 * </ul>
 * 未附魔无限时完全不跟踪，弹射物寿命与原版一致（可拾取 / 原版消失时间）。
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

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (!(event.level instanceof ServerLevel level)) {
            return;
        }
        if (event.phase == TickEvent.Phase.START) {
            for (Entity entity : RIPTIDE) {
                if (entity.isAlive() && entity.level() == level && entity.isInWater()) {
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
        // 激流：把水中那一 tick 的速度还原成 tick 前
        for (Entity entity : RIPTIDE) {
            if (!entity.isAlive() || entity.level() != level) {
                PRE_TICK.remove(entity);
                continue;
            }
            Vec3 before = PRE_TICK.remove(entity);
            if (before != null && entity.isInWater()) {
                entity.setDeltaMovement(before);
            }
        }
    }
}
