package cn.autoforged.eternal_food_mod_1789477527.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.SpectralArrow;
import net.minecraft.world.level.Level;

/** 防水版光灵箭：水阻力 0.99（与三叉戟一致），不修改原版类。 */
public class GunSpectralArrow extends SpectralArrow {

    private boolean waterproof;

    public GunSpectralArrow(EntityType<? extends GunSpectralArrow> type, Level level) {
        super(type, level);
    }

    public GunSpectralArrow(Level level, double x, double y, double z) {
        super(level, x, y, z);
    }

    public GunSpectralArrow(Level level, LivingEntity owner) {
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
