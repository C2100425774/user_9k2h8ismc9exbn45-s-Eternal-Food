package cn.autoforged.eternal_food_mod_1789477527.entity;

import cn.autoforged.eternal_food_mod_1789477527.item.EternalSpawnEggItem;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * 发射器枪发射「非投掷物物品」时用的通用弹射物：
 * 渲染成被发射的物品本身，落到方块 / 命中实体后变成掉落物（或生成刷怪蛋对应的生物）。
 *
 * <p>力量附魔只影响这里的物品模式（减少空气阻力，5 级无阻力）；激流只影响水中的阻力。
 */
public class GunProjectile extends ThrowableItemProjectile {

    public static final int MODE_ITEM = 0;
    public static final int MODE_SPAWN_EGG = 1;

    private static final EntityDataAccessor<Integer> DATA_MODE =
            SynchedEntityData.defineId(GunProjectile.class, EntityDataSerializers.INT);

    private int powerLevel;
    private boolean infinite;
    private boolean riptide;
    /** 力量 V 时固定使用的出膛速度（保证绝对直线）。 */
    private Vec3 straightVelocity;

    public GunProjectile(EntityType<? extends GunProjectile> type, Level level) {
        super(type, level);
    }

    public GunProjectile(Level level, double x, double y, double z, ItemStack stack) {
        super(ModEntities.GUN_PROJECTILE.get(), level);
        this.setPos(x, y, z);
        this.setItem(stack);
    }

    @Override
    protected Item getDefaultItem() {
        return Items.SNOWBALL;
    }

    public void setMode(int mode) {
        this.entityData.set(DATA_MODE, mode);
    }

    public int getMode() {
        return this.entityData.get(DATA_MODE);
    }

    public void setPowerLevel(int level) {
        this.powerLevel = level;
    }

    public void setInfinite(boolean infinite) {
        this.infinite = infinite;
    }

    public void setRiptide(boolean riptide) {
        this.riptide = riptide;
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_MODE, MODE_ITEM);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("GunMode", this.getMode());
        tag.putInt("GunPower", this.powerLevel);
        tag.putBoolean("GunInfinite", this.infinite);
        tag.putBoolean("GunRiptide", this.riptide);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.setMode(tag.getInt("GunMode"));
        this.powerLevel = tag.getInt("GunPower");
        this.infinite = tag.getBoolean("GunInfinite");
        this.riptide = tag.getBoolean("GunRiptide");
    }

    @Override
    public void tick() {
        Vec3 before = this.getDeltaMovement();
        double f = Math.max(0, Math.min(this.powerLevel, 5)) / 5.0;
        if (f >= 1.0) {
            // 力量 V：关重力 + 固定速度，走绝对直线（不会上下抖）
            if (this.straightVelocity == null) {
                this.straightVelocity = before;
            }
            this.setNoGravity(true);
            this.setDeltaMovement(this.straightVelocity);
            super.tick();
            return;
        }
        super.tick();
        if (this.riptide && this.isInWater()) {
            // 激流：完全无视水阻力
            this.setDeltaMovement(before);
        } else if (f > 0.0) {
            // 力量 1~4：补回一部分重力 + 一部分水平阻力
            Vec3 now = this.getDeltaMovement();
            double nx = now.x + (before.x - now.x) * f;
            double nz = now.z + (before.z - now.z) * f;
            this.setDeltaMovement(nx, now.y + 0.03 * f, nz);
        }
    }

    @Override
    protected void onHit(HitResult result) {
        if (this.level().isClientSide) {
            return;
        }
        if (this.getMode() == MODE_SPAWN_EGG) {
            spawnEggMob();
        } else {
            dropAsItem();
        }
    }

    private void dropAsItem() {
        ItemStack stack = this.getItem().copy();
        if (!stack.isEmpty()) {
            ItemEntity item = new ItemEntity(this.level(), this.getX(), this.getY(), this.getZ(), stack);
            Vec3 v = this.getDeltaMovement().scale(0.3);
            item.setDeltaMovement(v.x, 0.1, v.z);
            item.setDefaultPickUpDelay();
            this.level().addFreshEntity(item);
        }
        this.discard();
    }

    private void spawnEggMob() {
        ItemStack stack = this.getItem();
        if (stack.getItem() instanceof SpawnEggItem egg && this.level() instanceof ServerLevel server) {
            EntityType<?> type = egg.getType(stack.getTag());
            if (type != null) {
                Entity entity = type.create(server);
                if (entity != null) {
                    entity.moveTo(this.getX(), this.getY(), this.getZ(), this.getYRot(), 0.0F);
                    server.addFreshEntity(entity);
                }
            }
        }
        this.level().levelEvent(2004, BlockPos.containing(this.position()), 0);
        this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                SoundEvents.CHICKEN_EGG, SoundSource.NEUTRAL, 0.5F,
                0.4F / (this.level().getRandom().nextFloat() * 0.4F + 0.8F));
        this.discard();
    }
}
