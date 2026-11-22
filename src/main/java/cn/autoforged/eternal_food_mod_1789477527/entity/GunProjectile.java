package cn.autoforged.eternal_food_mod_1789477527.entity;

import cn.autoforged.eternal_food_mod_1789477527.event.GunProjectileTracker;
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
 * 发射器枪发射「非投掷物」时使用的物品弹射物：渲染成物品本身，
 * 落地 / 命中后变成掉落物；如果物品是刷怪蛋则生成对应生物。
 */
public class GunProjectile extends ThrowableItemProjectile {

    public static final int MODE_ITEM = 0;
    public static final int MODE_SPAWN_EGG = 1;

    private static final EntityDataAccessor<Integer> DATA_MODE =
            SynchedEntityData.defineId(GunProjectile.class, EntityDataSerializers.INT);


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

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_MODE, MODE_ITEM);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("GunMode", this.getMode());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.setMode(tag.getInt("GunMode"));
    }

    @Override
    public void tick() {
        super.tick();
        if (this.tickCount > GunProjectileTracker.POWERED_MAX_TICKS) {
            this.discard();
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
