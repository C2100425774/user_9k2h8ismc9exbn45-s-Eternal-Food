package cn.autoforged.eternal_food_mod_1789477527.entity;

import cn.autoforged.eternal_food_mod_1789477527.item.ModItems;
import java.util.List;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;

/**
 * 永恒喷溅型奶瓶的投掷物：落点周围 4×2×4、直线距离 4 格内的生物清除状态效果，
 * 范围与大部分喷溅型药水一致（见原版 {@code ThrownPotion.applySplash}）。
 * 普通版清空全部效果，plus 版只清除 HARMFUL 效果。
 */
public class ThrownEternalMilk extends ThrowableItemProjectile {

    public ThrownEternalMilk(EntityType<? extends ThrownEternalMilk> type, Level level) {
        super(type, level);
    }

    public ThrownEternalMilk(Level level, LivingEntity shooter) {
        super(ModEntities.THROWN_ETERNAL_MILK.get(), shooter, level);
    }

    public ThrownEternalMilk(Level level, double x, double y, double z) {
        super(ModEntities.THROWN_ETERNAL_MILK.get(), x, y, z, level);
    }

    @Override
    protected Item getDefaultItem() {
        return ModItems.ETERNAL_SPLASH_MILK_BOTTLE.get();
    }

    @Override
    protected float getGravity() {
        // 与原版投掷型药水一致。
        return 0.05F;
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (!this.level().isClientSide) {
            boolean debuffOnly = this.getItem().is(ModItems.ETERNAL_SPLASH_MILK_BOTTLE_PLUS.get());
            this.splash(debuffOnly);
            // 2002 = 喷溅型药水的粒子/音效事件。
            this.level().levelEvent(2002, this.blockPosition(), 0xF5F5F5);
            this.discard();
        }
    }

    private void splash(boolean debuffOnly) {
        AABB area = this.getBoundingBox().inflate(4.0, 2.0, 4.0);
        for (LivingEntity target : this.level().getEntitiesOfClass(LivingEntity.class, area)) {
            if (!target.isAffectedByPotions() || this.distanceToSqr(target) >= 16.0) {
                continue;
            }
            if (debuffOnly) {
                // 只清除 HARMFUL 效果；先复制集合避免遍历时 removeEffect 修改 activeEffects。
                for (MobEffectInstance instance : List.copyOf(target.getActiveEffects())) {
                    if (instance.getEffect().getCategory() == MobEffectCategory.HARMFUL) {
                        target.removeEffect(instance.getEffect());
                    }
                }
            } else {
                target.removeAllEffects();
            }
        }
    }
}
