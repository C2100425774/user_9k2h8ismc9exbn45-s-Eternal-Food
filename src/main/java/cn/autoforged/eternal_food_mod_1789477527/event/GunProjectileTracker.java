package cn.autoforged.eternal_food_mod_1789477527.event;

import cn.autoforged.eternal_food_mod_1789477527.EternalFoodMod;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 发射器枪发射的原版 / 模组弹射物需要的外部记录：
 * <ul>
 *   <li>无限附魔：60 tick 后强制消失（避免连续发射时实体堆积卡顿）；</li>
 *   <li>激流附魔：在水里时把速度恢复成 tick 前的值，等于无视水阻力（只有 1 级效果）。</li>
 * </ul>
 * 物品弹射物（{@code GunProjectile}）自己处理这两件事，不需要登记。
 */
@Mod.EventBusSubscriber(modid = EternalFoodMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class GunProjectileTracker {

    private static final Map<Entity, Long> EXPIRES = Collections.synchronizedMap(new WeakHashMap<>());
    private static final Set<Entity> RIPTIDE = Collections.synchronizedSet(
            Collections.newSetFromMap(new WeakHashMap<>()));
    private static final Map<Entity, Vec3> PRE_TICK = Collections.synchronizedMap(new WeakHashMap<>());

    private GunProjectileTracker() {
    }

    /** 无限附魔发射的弹射物：存活 {@code ticks} tick 后丢弃。 */
    public static void markInfinite(Entity entity, int ticks) {
        if (entity.level() instanceof ServerLevel) {
            EXPIRES.put(entity, entity.level().getGameTime() + ticks);
        }
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
        long now = level.getGameTime();
        if (event.phase == TickEvent.Phase.START) {
            for (Entity entity : RIPTIDE) {
                if (entity.isAlive() && entity.level() == level && entity.isInWater()) {
                    PRE_TICK.put(entity, entity.getDeltaMovement());
                }
            }
            return;
        }
        // END：先处理到期丢弃
        Iterator<Map.Entry<Entity, Long>> it = EXPIRES.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Entity, Long> entry = it.next();
            Entity entity = entry.getKey();
            if (!entity.isAlive() || entity.level() != level) {
                it.remove();
                continue;
            }
            if (now >= entry.getValue()) {
                entity.discard();
                it.remove();
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
