package cn.autoforged.eternal_food_mod_1789477527.entity;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;

/**
 * 3 级力量发射的「子弹」：外观是原版箭，但命中逻辑完全自定义——
 * 不走原版 {@code AbstractArrow#onHitEntity} 的 hurt/弹开流程，
 * 命中生物时清掉无敌帧并手动造成伤害，然后立即消失。
 */
public class GunBullet extends Arrow {

    private int bulletDamage = 1;

    public GunBullet(EntityType<? extends GunBullet> type, Level level) {
        super(type, level);
    }

    public GunBullet(Level level, LivingEntity owner) {
        super(level, owner);
        this.setOwner(owner);
    }

    public void setBulletDamage(int damage) {
        this.bulletDamage = Math.max(1, damage);
    }

    public void setBulletItem(ItemStack stack) {
        if (stack.is(net.minecraft.world.item.Items.TIPPED_ARROW)) {
            this.setEffectsFromItem(stack);
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        if (this.level().isClientSide) {
            return;
        }
        Entity target = result.getEntity();
        if (target instanceof LivingEntity living && living != this.getOwner()) {
            living.invulnerableTime = 0;
            living.hurtTime = 0;
            float absorption = living.getAbsorptionAmount();
            if (absorption > 0.0F) {
                living.setAbsorptionAmount(0.0F);
            }
            DamageSource source = this.damageSources().arrow(this, this.getOwner());
            living.hurt(source, this.bulletDamage);
            if (absorption > 0.0F) {
                living.setAbsorptionAmount(absorption);
            }
        }
        this.discard();
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        this.discard();
    }
}
