package cn.autoforged.eternal_food_mod_1789477527.item;

import cn.autoforged.eternal_food_mod_1789477527.EternalFoodMod;
import cn.autoforged.eternal_food_mod_1789477527.entity.GunProjectile;
import cn.autoforged.eternal_food_mod_1789477527.entity.ThrownEternalMilk;
import cn.autoforged.eternal_food_mod_1789477527.entity.ThrownEternalPotion;
import cn.autoforged.eternal_food_mod_1789477527.event.GunProjectileTracker;
import cn.autoforged.eternal_food_mod_1789477527.menu.EternalDispenserGunMenu;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.AbstractHurtingProjectile;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.entity.projectile.LargeFireball;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.SmallFireball;
import net.minecraft.world.entity.projectile.Snowball;
import net.minecraft.world.entity.projectile.SpectralArrow;
import net.minecraft.world.entity.projectile.ThrownEgg;
import net.minecraft.world.entity.projectile.ThrownEnderpearl;
import net.minecraft.world.entity.projectile.ThrownExperienceBottle;
import net.minecraft.world.entity.projectile.ThrownPotion;
import net.minecraft.world.entity.projectile.ThrownTrident;
import net.minecraft.world.entity.projectile.WitherSkull;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkHooks;

/**
 * 发射器枪：便携式手持发射器。
 *
 * <ul>
 *   <li>右键：朝准星方向发射弹仓里的弹药（力道 / 散布与发射器一致）；</li>
 *   <li>长按右键：按发射频率循环发射（2 ~ 30 tick，默认 20 tick = 1 秒）；</li>
 *   <li>Shift + 右键：打开界面（上面 3×3 弹仓，下面发射频率滑条）；</li>
 *   <li>附魔：无限 / 力量 / 火矢 / 多重射击 / 穿透 / 激流。</li>
 * </ul>
 */
public class EternalDispenserGunItem extends Item {

    /** 最快：每 tick 一次 = 1 tick。 */
    public static final int MIN_DELAY_TICKS = 1;
    /** 最慢：1.5 秒 = 30 tick。 */
    public static final int MAX_DELAY_TICKS = 30;
    /** 默认：1 秒。 */
    public static final int DEFAULT_DELAY_TICKS = 20;
    /** 无限附魔的箭落地后 0.5 秒消失（具体见 {@code GunProjectileTracker}）。 */
    public static final int INFINITE_LIFETIME_TICKS = GunProjectileTracker.INFINITE_STUCK_TICKS;

    public static final String TAG_ITEMS = "Items";
    public static final String TAG_DELAY = "FireDelay";
    private static final String TAG_SLOT = "Slot";
    private static final String TAG_STACK = "Item";

    public EternalDispenserGunItem(Properties properties) {
        super(properties);
    }

    // ------------------------------------------------------------------
    // 弹仓 / 发射频率（存在物品 NBT 里，界面关闭 / 存档都保留）
    // ------------------------------------------------------------------

    public static ItemStack getAmmo(ItemStack gun, int slot) {
        CompoundTag tag = gun.getTag();
        if (tag == null || !tag.contains(TAG_ITEMS, 9)) {
            return ItemStack.EMPTY;
        }
        ListTag list = tag.getList(TAG_ITEMS, 10);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            if (entry.getInt(TAG_SLOT) == slot) {
                return ItemStack.of(entry.getCompound(TAG_STACK));
            }
        }
        return ItemStack.EMPTY;
    }

    public static void setAmmo(ItemStack gun, int slot, ItemStack ammo) {
        CompoundTag tag = gun.getOrCreateTag();
        ListTag list = tag.contains(TAG_ITEMS, 9) ? tag.getList(TAG_ITEMS, 10) : new ListTag();
        for (int i = list.size() - 1; i >= 0; i--) {
            if (list.getCompound(i).getInt(TAG_SLOT) == slot) {
                list.remove(i);
            }
        }
        if (!ammo.isEmpty()) {
            CompoundTag entry = new CompoundTag();
            entry.putInt(TAG_SLOT, slot);
            entry.put(TAG_STACK, ammo.copy().save(new CompoundTag()));
            list.add(entry);
        }
        if (list.isEmpty()) {
            tag.remove(TAG_ITEMS);
        } else {
            tag.put(TAG_ITEMS, list);
        }
        if (tag.isEmpty()) {
            gun.setTag(null);
        }
    }

    /** 9 格弹仓全部导出（给 GUI 容器用）。 */
    public static List<ItemStack> readAllAmmo(ItemStack gun) {
        List<ItemStack> result = new ArrayList<>(9);
        for (int i = 0; i < 9; i++) {
            result.add(getAmmo(gun, i));
        }
        return result;
    }

    public static void writeAllAmmo(ItemStack gun, List<ItemStack> ammo) {
        for (int i = 0; i < 9; i++) {
            setAmmo(gun, i, i < ammo.size() ? ammo.get(i) : ItemStack.EMPTY);
        }
    }

    public static int getFireDelay(ItemStack gun) {
        CompoundTag tag = gun.getTag();
        int delay = tag == null ? DEFAULT_DELAY_TICKS : tag.getInt(TAG_DELAY);
        return Math.max(MIN_DELAY_TICKS, Math.min(MAX_DELAY_TICKS, delay));
    }

    public static void setFireDelay(ItemStack gun, int ticks) {
        gun.getOrCreateTag().putInt(TAG_DELAY,
                Math.max(MIN_DELAY_TICKS, Math.min(MAX_DELAY_TICKS, ticks)));
    }

    // ------------------------------------------------------------------
    // 附魔
    // ------------------------------------------------------------------

    @Override
    public boolean isEnchantable(ItemStack stack) {
        return true;
    }

    @Override
    public int getEnchantmentValue() {
        return 15;
    }

    @Override
    public boolean canApplyAtEnchantingTable(ItemStack stack, Enchantment enchantment) {
        return enchantment == Enchantments.INFINITY_ARROWS
                || enchantment == Enchantments.POWER_ARROWS
                || enchantment == Enchantments.FLAMING_ARROWS
                || enchantment == Enchantments.MULTISHOT
                || enchantment == Enchantments.PIERCING
                || enchantment == Enchantments.RIPTIDE
                || enchantment == Enchantments.SILK_TOUCH
                || super.canApplyAtEnchantingTable(stack, enchantment);
    }

    @Override
    public boolean isBookEnchantable(ItemStack stack, ItemStack book) {
        return true;
    }

    private static int ench(ItemStack stack, Enchantment enchantment) {
        if (stack.isEmpty()) {
            return 0;
        }
        return EnchantmentHelper.getItemEnchantmentLevel(enchantment, stack);
    }

    // ------------------------------------------------------------------
    // 使用
    // ------------------------------------------------------------------

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown()) {
            if (level instanceof ServerLevel server) {
                openMenu(server, player, hand, stack);
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
        }
        if (level instanceof ServerLevel server) {
            fire(server, player, stack);
        }
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null) {
            return InteractionResult.PASS;
        }
        ItemStack stack = context.getItemInHand();
        if (player.isShiftKeyDown()) {
            if (context.getLevel() instanceof ServerLevel server) {
                openMenu(server, player, context.getHand(), stack);
            }
            return InteractionResult.sidedSuccess(context.getLevel().isClientSide());
        }
        if (context.getLevel() instanceof ServerLevel server) {
            fire(server, player, stack);
        }
        player.startUsingItem(context.getHand());
        return InteractionResult.sidedSuccess(context.getLevel().isClientSide());
    }

    @Override
    public void onUseTick(Level level, LivingEntity living, ItemStack stack, int remaining) {
        if (level.isClientSide() || !(living instanceof Player player)
                || !(level instanceof ServerLevel server)) {
            return;
        }
        int elapsed = this.getUseDuration(stack) - remaining;
        int delay = getFireDelay(stack);
        if (elapsed > 0 && elapsed % delay == 0) {
            fire(server, player, stack);
        }
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return 72000;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.NONE;
    }

    private static void openMenu(ServerLevel level, Player player, InteractionHand hand, ItemStack stack) {
        if (player instanceof ServerPlayer serverPlayer) {
            NetworkHooks.openScreen(serverPlayer, new SimpleMenuProvider(
                    (id, inventory, p) -> new EternalDispenserGunMenu(id, inventory, hand),
                    stack.getHoverName()),
                    buffer -> buffer.writeEnum(hand));
        }
    }

    // ------------------------------------------------------------------
    // 发射
    // ------------------------------------------------------------------

    private void fire(ServerLevel level, Player player, ItemStack gun) {
        boolean infinity = ench(gun, Enchantments.INFINITY_ARROWS) > 0;
        int slot = findAmmoSlot(gun, infinity);
        if (slot < 0) {
            return;
        }
        ItemStack ammo = getAmmo(gun, slot);
        if (ammo.isEmpty()) {
            return;
        }
        ItemStack single = ammo.copy();
        single.setCount(1);

        Vec3 look = player.getLookAngle();
        Vec3 origin = player.getEyePosition().add(look.scale(0.35));

        int powerLevel = Math.max(0, Math.min(5, ench(gun, Enchantments.POWER_ARROWS)));
        boolean flame = ench(gun, Enchantments.FLAMING_ARROWS) > 0;
        int piercing = ench(gun, Enchantments.PIERCING);
        boolean multishot = ench(gun, Enchantments.MULTISHOT) > 0;
        boolean riptide = ench(gun, Enchantments.RIPTIDE) > 0;
        // 精准采集：发射出去的物品不再偏移（散布为 0）
        boolean silk = ench(gun, Enchantments.SILK_TOUCH) > 0;
        float uncertainty = silk ? 0.0F : 6.0F;

        List<Vec3> directions = new ArrayList<>(3);
        if (multishot) {
            directions.add(rotateY(look, -10.0));
            directions.add(look);
            directions.add(rotateY(look, 10.0));
        } else {
            directions.add(look);
        }

        boolean spawnEgg = single.getItem() instanceof SpawnEggItem;
        boolean projectileAmmo = spawnEgg || isProjectileAmmo(single);
        boolean eternalAmmo = isEternalItem(single);

        for (Vec3 direction : directions) {
            if (projectileAmmo) {
                spawnProjectileAmmo(level, player, single.copy(), origin, direction,
                        powerLevel, flame, piercing, infinity, riptide, uncertainty, silk);
            } else {
                spawnItemAmmo(level, player, single.copy(), origin, direction,
                        powerLevel, riptide, uncertainty);
            }
        }

        // 模组的永恒物品发射后不消耗；无限附魔只对「弹射物类」生效；其余照常消耗
        if (!eternalAmmo && !(infinity && projectileAmmo)) {
            ammo.shrink(1);
            setAmmo(gun, slot, ammo.isEmpty() ? ItemStack.EMPTY : ammo);
        }
        // 和发射器一致的音效 / 粒子（1002 = 发射器发射事件）
        level.levelEvent(1002, player.blockPosition(), 0);
    }

    private static int findAmmoSlot(ItemStack gun, boolean infinity) {
        if (infinity) {
            return getAmmo(gun, 0).isEmpty() ? -1 : 0;
        }
        for (int i = 0; i < 9; i++) {
            if (!getAmmo(gun, i).isEmpty()) {
                return i;
            }
        }
        return -1;
    }

    private static boolean isProjectileAmmo(ItemStack stack) {
        Item item = stack.getItem();
        if (item == Items.ARROW || item == Items.TIPPED_ARROW || item == Items.SPECTRAL_ARROW
                || item == Items.SNOWBALL || item == Items.EGG || item == Items.ENDER_PEARL
                || item == Items.EXPERIENCE_BOTTLE || item == Items.POTION
                || item == Items.SPLASH_POTION || item == Items.LINGERING_POTION
                || item == Items.FIRE_CHARGE || item == Items.FIREWORK_ROCKET
                || item == Items.TRIDENT) {
            return true;
        }
        return stack.is(ModItems.ETERNAL_TIPPED_ARROW.get())
                || stack.is(ModItems.ETERNAL_POTION.get())
                || stack.is(ModItems.ETERNAL_SPLASH_POTION.get())
                || stack.is(ModItems.ETERNAL_LINGERING_POTION.get())
                || stack.is(ModItems.ETERNAL_SPLASH_MILK_BOTTLE.get())
                || stack.is(ModItems.ETERNAL_SPLASH_MILK_BOTTLE_PLUS.get())
                || stack.is(ModItems.ETERNAL_EXPERIENCE_BOTTLE.get())
                || stack.is(ModItems.ETERNAL_SNOWBALL.get())
                || stack.is(ModItems.ETERNAL_ENDER_PEARL.get())
                || stack.is(ModItems.ETERNAL_FIRE_CHARGE.get())
                || stack.is(ModItems.ETERNAL_FIREBALL.get())
                || stack.is(ModItems.ETERNAL_WITHER_SKULL.get())
                || stack.is(ModItems.ETERNAL_WITHER_SKULL_BLUE.get())
                || stack.is(ModItems.ETERNAL_FIREWORK_ROCKET.get());
    }

    /** 刷怪蛋走 {@link GunProjectile}，命中时生成生物；其他非投掷物走物品模式。 */
    private void spawnItemAmmo(ServerLevel level, Player player, ItemStack ammo, Vec3 origin,
            Vec3 direction, int powerLevel, boolean riptide, float uncertainty) {
        GunProjectile projectile = new GunProjectile(level, origin.x, origin.y, origin.z, ammo);
        projectile.setMode(GunProjectile.MODE_ITEM);
        projectile.setPowerLevel(powerLevel);
        projectile.setRiptide(riptide);
        projectile.setOwner(player);
        projectile.shoot(direction.x, direction.y + 0.1, direction.z, 1.1F, uncertainty);
        level.addFreshEntity(projectile);
    }

    /** 发射器枪也能发射模组的永恒物品——这些物品不消耗。 */
    private static boolean isEternalItem(ItemStack stack) {
        var key = net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(stack.getItem());
        return key != null && EternalFoodMod.MOD_ID.equals(key.getNamespace());
    }

    private void spawnProjectileAmmo(ServerLevel level, Player player, ItemStack ammo, Vec3 origin,
            Vec3 direction, int powerLevel, boolean flame, int piercing, boolean infinity,
            boolean riptide, float uncertainty, boolean silk) {
        if (ammo.getItem() instanceof SpawnEggItem) {
            GunProjectile projectile = new GunProjectile(level, origin.x, origin.y, origin.z, ammo);
            projectile.setMode(GunProjectile.MODE_SPAWN_EGG);
            projectile.setInfinite(false);
            projectile.setRiptide(riptide);
            projectile.setOwner(player);
            projectile.shoot(direction.x, direction.y + 0.1, direction.z, 1.1F, uncertainty);
            level.addFreshEntity(projectile);
            return;
        }
        Projectile projectile = createProjectile(level, player, ammo, origin, direction, silk);
        if (projectile == null) {
            return;
        }
        boolean customShot = projectile instanceof FireworkRocketEntity
                || projectile instanceof SmallFireball
                || projectile instanceof LargeFireball
                || projectile instanceof WitherSkull;
        if (!customShot) {
            projectile.shoot(direction.x, direction.y + 0.1, direction.z, 1.1F, uncertainty);
        }
        projectile.setOwner(player);
        if (flame) {
            projectile.setSecondsOnFire(100);
        }
        if (piercing > 0 && projectile instanceof AbstractArrow arrow) {
            arrow.setPierceLevel((byte) Math.min(piercing, 4));
        }
        if (infinity) {
            // 只有无限附魔的弹射物不可拾取；箭类落地停住 0.5 秒后消失
            if (projectile instanceof AbstractArrow arrow) {
                arrow.pickup = AbstractArrow.Pickup.CREATIVE_ONLY;
            }
            GunProjectileTracker.markInfinite(projectile);
        }
        if (riptide) {
            GunProjectileTracker.markRiptide(projectile);
        }
        level.addFreshEntity(projectile);
    }

    private static Projectile createProjectile(ServerLevel level, Player player, ItemStack ammo,
            Vec3 origin, Vec3 direction, boolean silk) {
        Item item = ammo.getItem();
        RandomSource random = level.random;

        if (item == Items.FIRE_CHARGE || ammo.is(ModItems.ETERNAL_FIRE_CHARGE.get())) {
            double dx = silk ? direction.x : random.triangle(direction.x, 0.11485);
            double dy = silk ? direction.y : random.triangle(direction.y, 0.11485);
            double dz = silk ? direction.z : random.triangle(direction.z, 0.11485);
            SmallFireball fireball = new SmallFireball(level,
                    origin.x + direction.x * 0.3, origin.y + direction.y * 0.3, origin.z + direction.z * 0.3,
                    dx, dy, dz);
            fireball.setItem(ammo);
            return fireball;
        }
        if (ammo.is(ModItems.ETERNAL_FIREBALL.get())) {
            return heavyProjectile(level, origin, direction, random, true, silk);
        }
        if (ammo.is(ModItems.ETERNAL_WITHER_SKULL.get()) || ammo.is(ModItems.ETERNAL_WITHER_SKULL_BLUE.get())) {
            return heavyProjectile(level, origin, direction, random, false, silk);
        }
        if (item == Items.FIREWORK_ROCKET || ammo.is(ModItems.ETERNAL_FIREWORK_ROCKET.get())) {
            FireworkRocketEntity rocket = new FireworkRocketEntity(level, ammo,
                    origin.x, origin.y, origin.z, true);
            rocket.shoot(direction.x, direction.y, direction.z, 0.5F, silk ? 0.0F : 1.0F);
            return rocket;
        }
        if (item == Items.ARROW || item == Items.TIPPED_ARROW) {
            Arrow arrow = new Arrow(level, origin.x, origin.y, origin.z);
            if (item == Items.TIPPED_ARROW) {
                arrow.setEffectsFromItem(ammo);
            }
            return arrow;
        }
        if (ammo.is(ModItems.ETERNAL_TIPPED_ARROW.get())) {
            Arrow arrow = new Arrow(level, origin.x, origin.y, origin.z);
            EternalTippedArrowItem.applyEffects(arrow, ammo);
            return arrow;
        }
        if (item == Items.SPECTRAL_ARROW) {
            return new SpectralArrow(level, origin.x, origin.y, origin.z);
        }
        if (item == Items.SNOWBALL || ammo.is(ModItems.ETERNAL_SNOWBALL.get())) {
            Snowball snowball = new Snowball(level, origin.x, origin.y, origin.z);
            snowball.setItem(ammo);
            return snowball;
        }
        if (item == Items.EGG) {
            return new ThrownEgg(level, origin.x, origin.y, origin.z);
        }
        if (item == Items.ENDER_PEARL || ammo.is(ModItems.ETERNAL_ENDER_PEARL.get())) {
            ThrownEnderpearl pearl = new ThrownEnderpearl(level, player);
            pearl.setPos(origin.x, origin.y, origin.z);
            return pearl;
        }
        if (item == Items.POTION || item == Items.SPLASH_POTION || item == Items.LINGERING_POTION) {
            ThrownPotion potion = new ThrownPotion(level, origin.x, origin.y, origin.z);
            potion.setItem(ammo);
            return potion;
        }
        if (ammo.is(ModItems.ETERNAL_POTION.get()) || ammo.is(ModItems.ETERNAL_SPLASH_POTION.get())
                || ammo.is(ModItems.ETERNAL_LINGERING_POTION.get())) {
            ThrownEternalPotion potion = new ThrownEternalPotion(level, origin.x, origin.y, origin.z);
            potion.setItem(ammo);
            return potion;
        }
        if (ammo.is(ModItems.ETERNAL_SPLASH_MILK_BOTTLE.get())
                || ammo.is(ModItems.ETERNAL_SPLASH_MILK_BOTTLE_PLUS.get())) {
            ThrownEternalMilk milk = new ThrownEternalMilk(level, origin.x, origin.y, origin.z);
            milk.setItem(ammo);
            return milk;
        }
        if (item == Items.EXPERIENCE_BOTTLE || ammo.is(ModItems.ETERNAL_EXPERIENCE_BOTTLE.get())) {
            ThrownExperienceBottle bottle = new ThrownExperienceBottle(level, origin.x, origin.y, origin.z);
            bottle.setItem(ammo);
            return bottle;
        }
        if (item == Items.TRIDENT) {
            ThrownTrident trident = new ThrownTrident(level, player, ammo);
            trident.setPos(origin.x, origin.y, origin.z);
            return trident;
        }
        return null;
    }

    /** 恶魂火球 / 凋灵之首：与发射器一致，用三角形散布 + 0.1 推进力（精准采集时散布为 0）。 */
    private static AbstractHurtingProjectile heavyProjectile(ServerLevel level, Vec3 origin,
            Vec3 direction, RandomSource random, boolean fireball, boolean silk) {
        AbstractHurtingProjectile projectile;
        if (fireball) {
            projectile = new LargeFireball(EntityType.FIREBALL, level);
        } else {
            WitherSkull skull = new WitherSkull(EntityType.WITHER_SKULL, level);
            projectile = skull;
        }
        projectile.setPos(origin.x + direction.x * 0.3, origin.y + direction.y * 0.3,
                origin.z + direction.z * 0.3);
        double dx = silk ? direction.x : random.triangle(direction.x, 0.11485);
        double dy = silk ? direction.y : random.triangle(direction.y, 0.11485);
        double dz = silk ? direction.z : random.triangle(direction.z, 0.11485);
        double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (length != 0.0) {
            projectile.xPower = dx / length * 0.1;
            projectile.yPower = dy / length * 0.1;
            projectile.zPower = dz / length * 0.1;
        }
        return projectile;
    }

    private static Vec3 rotateY(Vec3 vector, double degrees) {
        double radians = Math.toRadians(degrees);
        double cos = Math.cos(radians);
        double sin = Math.sin(radians);
        return new Vec3(vector.x * cos + vector.z * sin, vector.y,
                -vector.x * sin + vector.z * cos).normalize();
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.eternal_food_mod_1789477527.dispenser_gun.usage")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.eternal_food_mod_1789477527.dispenser_gun.delay",
                String.format(java.util.Locale.ROOT, "%.1f", getFireDelay(stack) / 20.0F))
                .withStyle(ChatFormatting.GRAY));
        Boolean shift = DistExecutor.unsafeCallWhenOn(Dist.CLIENT,
                () -> () -> net.minecraft.client.gui.screens.Screen.hasShiftDown());
        if (Boolean.TRUE.equals(shift)) {
            List<Enchantment> list = List.of(Enchantments.INFINITY_ARROWS,
                    Enchantments.POWER_ARROWS, Enchantments.FLAMING_ARROWS,
                    Enchantments.MULTISHOT, Enchantments.PIERCING,
                    Enchantments.RIPTIDE, Enchantments.SILK_TOUCH);
            boolean any = false;
            for (Enchantment enchantment : list) {
                if (ench(stack, enchantment) <= 0) {
                    continue;
                }
                any = true;
                tooltip.add(Component.translatable(enchantEffectKey(enchantment))
                        .withStyle(ChatFormatting.GRAY));
            }
            if (!any) {
                tooltip.add(Component.translatable(
                        "tooltip.eternal_food_mod_1789477527.dispenser_gun.no_enchant")
                        .withStyle(ChatFormatting.DARK_GRAY));
            }
        } else {
            tooltip.add(Component.translatable(
                    "tooltip.eternal_food_mod_1789477527.dispenser_gun.shift")
                    .withStyle(ChatFormatting.DARK_GRAY));
        }
    }

    private static String enchantEffectKey(Enchantment enchantment) {
        if (enchantment == Enchantments.INFINITY_ARROWS) {
            return "tooltip.eternal_food_mod_1789477527.dispenser_gun.enchant.infinity";
        }
        if (enchantment == Enchantments.POWER_ARROWS) {
            return "tooltip.eternal_food_mod_1789477527.dispenser_gun.enchant.power";
        }
        if (enchantment == Enchantments.FLAMING_ARROWS) {
            return "tooltip.eternal_food_mod_1789477527.dispenser_gun.enchant.flame";
        }
        if (enchantment == Enchantments.MULTISHOT) {
            return "tooltip.eternal_food_mod_1789477527.dispenser_gun.enchant.multishot";
        }
        if (enchantment == Enchantments.PIERCING) {
            return "tooltip.eternal_food_mod_1789477527.dispenser_gun.enchant.piercing";
        }
        if (enchantment == Enchantments.RIPTIDE) {
            return "tooltip.eternal_food_mod_1789477527.dispenser_gun.enchant.riptide";
        }
        return "tooltip.eternal_food_mod_1789477527.dispenser_gun.enchant.silk_touch";
    }
}
