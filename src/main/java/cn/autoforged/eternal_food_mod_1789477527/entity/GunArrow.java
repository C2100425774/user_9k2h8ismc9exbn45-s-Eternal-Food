package cn.autoforged.eternal_food_mod_1789477527.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.level.Level;

/**
 * 发射器枪用的普通箭：防水开启时把水阻力从原版 0.6 改成三叉戟的 0.99。
 * 不改原版类，避免和其它修改 {@code getWaterInertia} 的模组冲突。
 */
public class GunArrow extends Arrow {

    private boolean waterproof;

    public GunArrow(EntityType<? extends GunArrow> type, Level level) {
        super(type, level);
    }

    public GunArrow(Level level, double x, double y, double z) {
        super(level, x, y, z);
    }

    public GunArrow(Level level, LivingEntity owner) {
        super(level, owner);
    }

    public void setWaterproof(boolean waterproof) {
        this.waterproof = waterproof;
    }

    @Override
    protected float getWaterInertia() {
        return this.waterproof ? 0.99F : super.getWaterInertia();
    }
}
