package cn.autoforged.eternal_food_mod_1789477527.event;

import cn.autoforged.eternal_food_mod_1789477527.EternalFoodMod;
import cn.autoforged.eternal_food_mod_1789477527.entity.ThrownEternalMilk;
import cn.autoforged.eternal_food_mod_1789477527.entity.ThrownEternalPotion;
import cn.autoforged.eternal_food_mod_1789477527.item.EternalTippedArrowItem;
import cn.autoforged.eternal_food_mod_1789477527.item.ModItems;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockSource;
import net.minecraft.core.Direction;
import net.minecraft.core.Position;
import net.minecraft.core.dispenser.AbstractProjectileDispenseBehavior;
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.AbstractHurtingProjectile;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.entity.projectile.LargeFireball;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.SmallFireball;
import net.minecraft.world.entity.projectile.Snowball;
import net.minecraft.world.entity.projectile.ThrownExperienceBottle;
import net.minecraft.world.entity.projectile.WitherSkull;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import org.slf4j.Logger;

/**
 * 永恒投掷物的发射器行为：永恒药箭 / 永恒喷溅型药水 / 永恒滞留型药水 /
 * 永恒喷溅型奶瓶（含 plus）被发射器发射时都不消耗。
 *
 * <p>原版 {@link AbstractProjectileDispenseBehavior#execute} 会对槽位里的栈执行
 * {@code shrink(1)}。这里在副本上调用父类逻辑（消耗副本、发射投射物），
 * 再返回原栈，使发射器槽位中的永恒物品数量不变。
 */
@Mod.EventBusSubscriber(modid = EternalFoodMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModDispenseBehaviors {

    private static final Logger LOG = LogUtils.getLogger();

    @SubscribeEvent
    public static void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> DispenserBlock.registerBehavior(ModItems.ETERNAL_TIPPED_ARROW.get(),
                new AbstractProjectileDispenseBehavior() {
                    @Override
                    protected Projectile getProjectile(Level level, Position position, ItemStack stack) {
                        Arrow arrow = new Arrow(level, position.x(), position.y(), position.z());
                        EternalTippedArrowItem.applyEffects(arrow, stack);
                        arrow.pickup = AbstractArrow.Pickup.CREATIVE_ONLY;
                        return arrow;
                    }

                    @Override
                    public ItemStack execute(BlockSource source, ItemStack stack) {
                        super.execute(source, stack.copy());
                        return stack;
                    }
                }));

        // 永恒喷溅型 / 滞留型药水：飞行轨迹与原版对应药水一致（power ×1.25、uncertainty ×0.5，
        // 与原版 DispenseItemBehavior 对 SPLASH_POTION / LINGERING_POTION 的处理相同），
        // 且发射后不消耗。滞留型在命中时由 ThrownEternalPotion#onHit 切换为滞留云。
        AbstractProjectileDispenseBehavior eternalPotionBehavior = new AbstractProjectileDispenseBehavior() {
            @Override
            protected Projectile getProjectile(Level level, Position position, ItemStack stack) {
                ThrownEternalPotion potion =
                        new ThrownEternalPotion(level, position.x(), position.y(), position.z());
                potion.setItem(stack);
                return potion;
            }

            @Override
            protected float getUncertainty() {
                return super.getUncertainty() * 0.5F;
            }

            @Override
            protected float getPower() {
                return super.getPower() * 1.25F;
            }

            @Override
            public ItemStack execute(BlockSource source, ItemStack stack) {
                super.execute(source, stack.copy());
                return stack;
            }
        };
        event.enqueueWork(() -> {
            DispenserBlock.registerBehavior(ModItems.ETERNAL_SPLASH_POTION.get(), eternalPotionBehavior);
            DispenserBlock.registerBehavior(ModItems.ETERNAL_LINGERING_POTION.get(), eternalPotionBehavior);
        });

        // 永恒喷溅型奶瓶 / plus：以与原版喷溅型药水相同的轨迹发射，且不消耗。
        AbstractProjectileDispenseBehavior eternalMilkBehavior = new AbstractProjectileDispenseBehavior() {
            @Override
            protected Projectile getProjectile(Level level, Position position, ItemStack stack) {
                ThrownEternalMilk bottle =
                        new ThrownEternalMilk(level, position.x(), position.y(), position.z());
                bottle.setItem(stack);
                return bottle;
            }

            @Override
            protected float getUncertainty() {
                return super.getUncertainty() * 0.5F;
            }

            @Override
            protected float getPower() {
                return super.getPower() * 1.25F;
            }

            @Override
            public ItemStack execute(BlockSource source, ItemStack stack) {
                super.execute(source, stack.copy());
                return stack;
            }
        };
        event.enqueueWork(() -> {
            DispenserBlock.registerBehavior(ModItems.ETERNAL_SPLASH_MILK_BOTTLE.get(), eternalMilkBehavior);
            DispenserBlock.registerBehavior(ModItems.ETERNAL_SPLASH_MILK_BOTTLE_PLUS.get(), eternalMilkBehavior);
        });

        // 永恒附魔之瓶：以与原版附魔之瓶相同的轨迹发射（power ×1.25、uncertainty ×0.5），
        // 且发射后不消耗。
        AbstractProjectileDispenseBehavior eternalExperienceBottleBehavior = new AbstractProjectileDispenseBehavior() {
            @Override
            protected Projectile getProjectile(Level level, Position position, ItemStack stack) {
                ThrownExperienceBottle bottle =
                        new ThrownExperienceBottle(level, position.x(), position.y(), position.z());
                bottle.setItem(stack);
                return bottle;
            }

            @Override
            protected float getUncertainty() {
                return super.getUncertainty() * 0.5F;
            }

            @Override
            protected float getPower() {
                return super.getPower() * 1.25F;
            }

            @Override
            public ItemStack execute(BlockSource source, ItemStack stack) {
                super.execute(source, stack.copy());
                return stack;
            }
        };
        event.enqueueWork(() -> DispenserBlock.registerBehavior(ModItems.ETERNAL_EXPERIENCE_BOTTLE.get(),
                eternalExperienceBottleBehavior));

        // 永恒雪球：以与原版雪球相同的轨迹发射，且不消耗。
        AbstractProjectileDispenseBehavior eternalSnowballBehavior = new AbstractProjectileDispenseBehavior() {
            @Override
            protected Projectile getProjectile(Level level, Position position, ItemStack stack) {
                Snowball snowball = new Snowball(level, position.x(), position.y(), position.z());
                snowball.setItem(stack);
                return snowball;
            }

            @Override
            public ItemStack execute(BlockSource source, ItemStack stack) {
                super.execute(source, stack.copy());
                return stack;
            }
        };
        event.enqueueWork(() -> DispenserBlock.registerBehavior(ModItems.ETERNAL_SNOWBALL.get(),
                eternalSnowballBehavior));

        // 永恒火焰弹：复刻原版火焰弹的发射器行为（小火球 + 方向上的三角形散布），但不消耗。
        DefaultDispenseItemBehavior eternalFireChargeBehavior = new DefaultDispenseItemBehavior() {
            @Override
            public ItemStack execute(BlockSource source, ItemStack stack) {
                Direction direction = source.getBlockState().getValue(DispenserBlock.FACING);
                Position position = DispenserBlock.getDispensePosition(source);
                double d0 = position.x() + direction.getStepX() * 0.3F;
                double d1 = position.y() + direction.getStepY() * 0.3F;
                double d2 = position.z() + direction.getStepZ() * 0.3F;
                Level level = source.getLevel();
                net.minecraft.util.RandomSource random = level.random;
                double d3 = random.triangle(direction.getStepX(), 0.11485000000000001);
                double d4 = random.triangle(direction.getStepY(), 0.11485000000000001);
                double d5 = random.triangle(direction.getStepZ(), 0.11485000000000001);
                SmallFireball fireball = new SmallFireball(level, d0, d1, d2, d3, d4, d5);
                fireball.setItem(stack);
                level.addFreshEntity(fireball);
                // 不 shrink：发射不消耗物品。
                return stack;
            }

            @Override
            protected void playSound(BlockSource source) {
                source.getLevel().levelEvent(1018, source.getPos(), 0);
            }
        };
        event.enqueueWork(() -> DispenserBlock.registerBehavior(ModItems.ETERNAL_FIRE_CHARGE.get(),
                eternalFireChargeBehavior));

        // 永恒火球（恶魂大火球）/ 永恒凋灵之首 / 永恒蓝色凋灵之首：
        // 沿发射器朝向发射对应投射物且不消耗。这类投射物的飞行方向由构造函数写入的
        // xPower/yPower/zPower 决定，故用位置 + 手动设置能量（与原版恶魂 / 凋灵一致）。
        DefaultDispenseItemBehavior eternalHeavyProjectileBehavior = new DefaultDispenseItemBehavior() {
            @Override
            public ItemStack execute(BlockSource source, ItemStack stack) {
                Direction direction = source.getBlockState().getValue(DispenserBlock.FACING);
                Position position = DispenserBlock.getDispensePosition(source);
                double x = position.x() + direction.getStepX() * 0.3F;
                double y = position.y() + direction.getStepY() * 0.3F;
                double z = position.z() + direction.getStepZ() * 0.3F;
                Level level = source.getLevel();
                net.minecraft.util.RandomSource random = level.random;
                double dx = random.triangle(direction.getStepX(), 0.11485000000000001);
                double dy = random.triangle(direction.getStepY(), 0.11485000000000001);
                double dz = random.triangle(direction.getStepZ(), 0.11485000000000001);
                Item item = stack.getItem();
                AbstractHurtingProjectile projectile;
                if (item == ModItems.ETERNAL_FIREBALL.get()) {
                    projectile = new LargeFireball(EntityType.FIREBALL, level);
                } else {
                    WitherSkull skull = new WitherSkull(EntityType.WITHER_SKULL, level);
                    skull.setDangerous(item == ModItems.ETERNAL_WITHER_SKULL_BLUE.get());
                    projectile = skull;
                }
                projectile.setPos(x, y, z);
                setDirectionalPower(projectile, dx, dy, dz);
                level.addFreshEntity(projectile);
                // 不 shrink：发射不消耗物品。
                return stack;
            }
        };
        event.enqueueWork(() -> {
            DispenserBlock.registerBehavior(ModItems.ETERNAL_FIREBALL.get(), eternalHeavyProjectileBehavior);
            DispenserBlock.registerBehavior(ModItems.ETERNAL_WITHER_SKULL.get(), eternalHeavyProjectileBehavior);
            DispenserBlock.registerBehavior(ModItems.ETERNAL_WITHER_SKULL_BLUE.get(), eternalHeavyProjectileBehavior);
        });

        // 永恒烟花火箭：以与原版烟花火箭相同的发射方式（沿朝向 0.5 速度、poking out of block），
        // 且发射后不消耗。原版 DefaultDispenseItemBehavior 会 shrink(1)，这里改为返回原栈。
        DefaultDispenseItemBehavior eternalFireworkBehavior = new DefaultDispenseItemBehavior() {
            @Override
            protected ItemStack execute(BlockSource source, ItemStack stack) {
                Direction direction = source.getBlockState().getValue(DispenserBlock.FACING);
                FireworkRocketEntity rocket = new FireworkRocketEntity(
                        source.getLevel(), stack, source.x(), source.y(), source.z(), true);
                setEntityPokingOutOfBlock(source, rocket, direction);
                rocket.shoot(direction.getStepX(), direction.getStepY(), direction.getStepZ(), 0.5F, 1.0F);
                source.getLevel().addFreshEntity(rocket);
                // 不 shrink：发射不消耗物品。
                return stack;
            }

            @Override
            protected void playSound(BlockSource source) {
                source.getLevel().levelEvent(1004, source.getPos(), 0);
            }
        };
        event.enqueueWork(() -> DispenserBlock.registerBehavior(ModItems.ETERNAL_FIREWORK_ROCKET.get(),
                eternalFireworkBehavior));

        // 永恒刷怪蛋：与原版刷怪蛋发射器行为一致（按类型生成生物、沿朝向偏移），但不消耗。
        DefaultDispenseItemBehavior eternalSpawnEggBehavior = new DefaultDispenseItemBehavior() {
            @Override
            protected ItemStack execute(BlockSource source, ItemStack stack) {
                Direction direction = source.getBlockState().getValue(DispenserBlock.FACING);
                EntityType<?> type = ((SpawnEggItem) stack.getItem()).getType(stack.getTag());
                try {
                    type.spawn(source.getLevel(), stack, null, source.getPos().relative(direction),
                            MobSpawnType.DISPENSER, direction != Direction.UP, false);
                } catch (Exception exception) {
                    LOG.error("Error while dispensing eternal spawn egg from dispenser at {}",
                            source.getPos(), exception);
                    return ItemStack.EMPTY;
                }
                source.getLevel().gameEvent(null, GameEvent.ENTITY_PLACE, source.getPos());
                // 不 shrink：发射不消耗物品。
                return stack;
            }
        };
        event.enqueueWork(() -> DispenserBlock.registerBehavior(ModItems.ETERNAL_SPAWN_EGG.get(),
                eternalSpawnEggBehavior));
    }

    /**
     * 按方向设置 {@link AbstractHurtingProjectile} 的推进能量（归一化后 ×0.1，与原版
     * 恶魂 / 凋灵发射弹射物时构造函数写入的值一致）。
     */
    private static void setDirectionalPower(AbstractHurtingProjectile projectile, double dx, double dy, double dz) {
        double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (length != 0.0D) {
            projectile.xPower = dx / length * 0.1D;
            projectile.yPower = dy / length * 0.1D;
            projectile.zPower = dz / length * 0.1D;
        }
    }

    /**
     * 复刻原版 {@code DispenseItemBehavior.setEntityPokingOutOfBlock}（该方法为包内私有，无法直接调用），
     * 把发射出的实体摆到发射器口沿。
     */
    private static void setEntityPokingOutOfBlock(BlockSource source, net.minecraft.world.entity.Entity entity,
            Direction direction) {
        entity.setPos(
                source.x() + direction.getStepX() * (0.5000099999997474 - entity.getBbWidth() / 2.0),
                source.y() + direction.getStepY() * (0.5000099999997474 - entity.getBbHeight() / 2.0)
                        - entity.getBbHeight() / 2.0,
                source.z() + direction.getStepZ() * (0.5000099999997474 - entity.getBbWidth() / 2.0));
    }
}
