package cn.autoforged.eternal_food_mod_1789477527.item;

import cn.autoforged.eternal_food_mod_1789477527.EternalFoodMod;
import cn.autoforged.eternal_food_mod_1789477527.entity.GunProjectile;
import cn.autoforged.eternal_food_mod_1789477527.item.EternalTippedArrowItem;
import cn.autoforged.eternal_food_mod_1789477527.entity.ThrownEternalMilk;
import cn.autoforged.eternal_food_mod_1789477527.entity.ThrownEternalPotion;
import cn.autoforged.eternal_food_mod_1789477527.event.GunProjectileTracker;
import cn.autoforged.eternal_food_mod_1789477527.menu.EternalDispenserGunMenu;
import cn.autoforged.eternal_food_mod_1789477527.network.ModNetwork;
import cn.autoforged.eternal_food_mod_1789477527.network.OpenGunConfigPacket;
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
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.network.PacketDistributor;

/**
 * 鍙戝皠鍣ㄦ灙锛氫究鎼哄紡鎵嬫寔鍙戝皠鍣ㄣ€? *
 * <ul>
 *   <li>鍙抽敭锛氭湞鍑嗘槦鏂瑰悜鍙戝皠寮逛粨閲岀殑寮硅嵂锛堝姏閬?/ 鏁ｅ竷涓庡彂灏勫櫒涓€鑷达級锛?/li>
 *   <li>闀挎寜鍙抽敭锛氭寜鍙戝皠棰戠巼寰幆鍙戝皠锛? ~ 30 tick锛岄粯璁?20 tick = 1 绉掞級锛?/li>
 *   <li>Shift + 鍙抽敭锛氭墦寮€鐣岄潰锛堜笂闈?3脳3 寮逛粨锛屼笅闈㈠彂灏勯鐜囨粦鏉★級锛?/li>
 *   <li>闄勯瓟锛氭棤闄?/ 鍔涢噺 / 鐏煝 / 澶氶噸灏勫嚮 / 绌块€?/ 婵€娴併€?/li>
 * </ul>
 */
public class EternalDispenserGunItem extends Item {

    /** 鏈€蹇細姣?tick 涓€娆?= 1 tick銆?*/
    public static final int MIN_DELAY_TICKS = 1;
    /** 鏈€鎱細1.5 绉?= 30 tick銆?*/
    public static final int MAX_DELAY_TICKS = 30;
    /** 榛樿锛? 绉掋€?*/
    public static final int DEFAULT_DELAY_TICKS = 20;
    /** 鏃犻檺闄勯瓟鐨勭钀藉湴鍚?0.5 绉掓秷澶憋紙鍏蜂綋瑙?{@code GunProjectileTracker}锛夈€?*/
    public static final int INFINITE_LIFETIME_TICKS = GunProjectileTracker.INFINITE_STUCK_TICKS;

    public static final String TAG_ITEMS = "Items";
    public static final String TAG_DELAY = "FireDelay";
    private static final String TAG_SLOT = "Slot";
    private static final String TAG_STACK = "Item";

    public EternalDispenserGunItem(Properties properties) {
        super(properties);
    }

    // ------------------------------------------------------------------
    // 寮逛粨 / 鍙戝皠棰戠巼锛堝瓨鍦ㄧ墿鍝?NBT 閲岋紝鐣岄潰鍏抽棴 / 瀛樻。閮戒繚鐣欙級
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

    /** 9 鏍煎脊浠撳叏閮ㄥ鍑猴紙缁?GUI 瀹瑰櫒鐢級銆?*/
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
    // 閰嶇疆锛堝師鏉ョ殑闄勯瓟鏀规垚鏋嚜韬殑寮€鍏筹紝瑙?Shift+鍙抽敭鐣岄潰锛?    // ------------------------------------------------------------------

    public static final String CFG_INFINITY = "CfgInfinity";
    public static final String CFG_FLAME = "CfgFlame";
    public static final String CFG_MULTISHOT = "CfgMultishot";
    public static final String CFG_PIERCING = "CfgPiercing";
    public static final String CFG_WATERPROOF = "CfgWaterproof";
    public static final String CFG_SILK = "CfgSilkTouch";
    public static final String CFG_POWER = "CfgPower";
    /** 力量上限 3 级。 */
    public static final int MAX_POWER = 3;
    public static final String CFG_DELAY = TAG_DELAY;
    private static final String CFG_LAST_FIRE = "CfgLastFire";

    /** 閰嶇疆鐣岄潰閲岀殑寮€鍏虫寜閽紙椤哄簭鍗崇晫闈㈤『搴忥級銆?*/
    public static final List<String> TOGGLE_KEYS = List.of(
            CFG_INFINITY, CFG_FLAME, CFG_MULTISHOT, CFG_WATERPROOF, CFG_SILK);

    @Override
    public boolean isEnchantable(ItemStack stack) {
        return false;
    }

    @Override
    public int getEnchantmentValue() {
        return 0;
    }

    @Override
    public boolean canApplyAtEnchantingTable(ItemStack stack, Enchantment enchantment) {
        return false;
    }

    @Override
    public boolean isBookEnchantable(ItemStack stack, ItemStack book) {
        return false;
    }

    public static boolean getToggle(ItemStack gun, String key) {
        return !gun.isEmpty() && gun.hasTag() && gun.getTag().getBoolean(key);
    }

    public static void setToggle(ItemStack gun, String key, boolean on) {
        gun.getOrCreateTag().putBoolean(key, on);
    }

    public static int getPower(ItemStack gun) {
        if (gun.isEmpty()) {
            return 0;
        }
        int power = gun.hasTag() ? gun.getTag().getInt(CFG_POWER) : 0;
        if (power <= 0) {
            // 兼容旧版本用附魔「力量」的枪
            power = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.POWER_ARROWS, gun);
        }
        return Math.max(0, Math.min(MAX_POWER, power));
    }

    public static void setPower(ItemStack gun, int power) {
        gun.getOrCreateTag().putInt(CFG_POWER, Math.max(0, Math.min(MAX_POWER, power)));
    }

    /** 穿透等级 0~10（实际箭最多 4 级，界面允许调到 10）。 */
    public static int getPiercing(ItemStack gun) {
        if (gun.isEmpty() || !gun.hasTag()) {
            return 0;
        }
        return Math.max(0, Math.min(10, gun.getTag().getInt(CFG_PIERCING)));
    }

    public static void setPiercing(ItemStack gun, int level) {
        gun.getOrCreateTag().putInt(CFG_PIERCING, Math.max(0, Math.min(10, level)));
    }

    public static String optionNameKey(String key) {
        return "gui.eternal_food_mod_1789477527.dispenser_gun.option." + shortName(key);
    }

    public static String optionDescKey(String key) {
        return "tooltip.eternal_food_mod_1789477527.dispenser_gun.option." + shortName(key);
    }

    private static String shortName(String key) {
        return switch (key) {
            case CFG_INFINITY -> "infinity";
            case CFG_FLAME -> "flame";
            case CFG_MULTISHOT -> "multishot";
            case CFG_PIERCING -> "piercing";
            case CFG_WATERPROOF -> "waterproof";
            case CFG_SILK -> "silk_touch";
            default -> "unknown";
        };
    }

    // ------------------------------------------------------------------
    // 浣跨敤
    // ------------------------------------------------------------------

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide) {
            if (player.isShiftKeyDown()) {
                // Shift+鍙抽敭锛氭墦寮€閰嶇疆鐣岄潰锛堟寜閽缃灙鐨勬晥鏋滐級
                if (player instanceof ServerPlayer serverPlayer) {
                    ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> serverPlayer),
                            new OpenGunConfigPacket(stack.copy()));
                }
            } else if (level instanceof ServerLevel server) {
                // 鍙抽敭锛氭墦寮€寮硅嵂鐣岄潰
                openMenu(server, player, hand, stack);
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null) {
            return InteractionResult.PASS;
        }
        ItemStack stack = context.getItemInHand();
        if (!context.getLevel().isClientSide) {
            if (player.isShiftKeyDown()) {
                if (player instanceof ServerPlayer serverPlayer) {
                    ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> serverPlayer),
                            new OpenGunConfigPacket(stack.copy()));
                }
            } else if (context.getLevel() instanceof ServerLevel server) {
                openMenu(server, player, context.getHand(), stack);
            }
        }
        return InteractionResult.sidedSuccess(context.getLevel().isClientSide());
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return 0;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.NONE;
    }

    /** 宸﹂敭锛堝鎴风鍙戞潵鐨勫彂灏勫寘锛夊湪鏈嶅姟绔墽琛岋紝甯﹀彂灏勯棿闅旈檺鍒躲€?*/
    public static void tryFire(ServerPlayer player) {
        ItemStack gun = player.getMainHandItem();
        if (gun.isEmpty() || !(gun.getItem() instanceof EternalDispenserGunItem)) {
            return;
        }
        long now = player.serverLevel().getGameTime();
        long last = gun.hasTag() ? gun.getTag().getLong(CFG_LAST_FIRE) : 0L;
        if (now - last < getFireDelay(gun)) {
            return;
        }
        gun.getOrCreateTag().putLong(CFG_LAST_FIRE, now);
        fire(player.serverLevel(), player, gun);
    }

    /** 渚涢厤缃晫闈㈡敼瀹屾暟鍊煎悗鍚屾鏈嶅姟绔?NBT銆?*/
    public static void applyConfig(ItemStack gun, String key, int value) {
        if (gun.isEmpty() || !(gun.getItem() instanceof EternalDispenserGunItem)) {
            return;
        }
        if (CFG_POWER.equals(key)) {
            setPower(gun, value);
        } else if (CFG_PIERCING.equals(key)) {
            setPiercing(gun, value);
        } else if (CFG_DELAY.equals(key)) {
            setFireDelay(gun, value);
        } else {
            setToggle(gun, key, value != 0);
        }
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
    // 鍙戝皠
    // ------------------------------------------------------------------

    private static void fire(ServerLevel level, Player player, ItemStack gun) {
        boolean infinity = getToggle(gun, CFG_INFINITY);
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

        int powerLevel = getPower(gun);
        boolean flame = getToggle(gun, CFG_FLAME);
        // 3 级是「子弹」：不穿透，命中敌人直接消失（穿透滑块在 3 级时暂时无效）
        int piercing = powerLevel >= 3 ? 0 : getPiercing(gun);
        boolean multishot = getToggle(gun, CFG_MULTISHOT);
        boolean waterproof = getToggle(gun, CFG_WATERPROOF);
        // 
        boolean silk = getToggle(gun, CFG_SILK);
        float uncertainty = silk ? 0.0F : 6.0F;

        List<Vec3> directions = new ArrayList<>(3);
        if (multishot) {
            directions.add(rotateY(look, -10.0));
            directions.add(look);
            directions.add(rotateY(look, 10.0));
        } else {
            directions.add(look);
        }

        boolean spawnEgg = !infinity && single.getItem() instanceof SpawnEggItem;
        // 开了「无限」时，末影珍珠只能当一般物品发射，避免连发传送暴毙
        boolean enderPearl = single.is(Items.ENDER_PEARL)
                || single.is(ModItems.ETERNAL_ENDER_PEARL.get());
        boolean projectileAmmo = spawnEgg
                || (isProjectileAmmo(single) && !(infinity && enderPearl));
        boolean eternalAmmo = isEternalItem(single);

        for (int i = 0; i < directions.size(); i++) {
            Vec3 direction = directions.get(i);
            boolean sideArrow = directions.size() > 1 && i != 1;
            if (projectileAmmo) {
                spawnProjectileAmmo(level, player, single.copy(), origin, direction,
                        powerLevel, flame, piercing, infinity, waterproof, uncertainty, silk, sideArrow);
            } else {
                spawnItemAmmo(level, player, single.copy(), origin, direction,
                        powerLevel, waterproof, uncertainty);
            }
        }

        // 
        boolean noConsume = projectileAmmo && (eternalAmmo || infinity);
        if (!noConsume) {
            ammo.shrink(1);
            setAmmo(gun, slot, ammo.isEmpty() ? ItemStack.EMPTY : ammo);
        }
        // 鍜屽彂灏勫櫒涓€鑷寸殑闊虫晥 / 绮掑瓙锛?002 = 鍙戝皠鍣ㄥ彂灏勪簨浠讹級
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

    /** 鍒锋€泲璧?{@link GunProjectile}锛屽懡涓椂鐢熸垚鐢熺墿锛涘叾浠栭潪鎶曟幏鐗╄蛋鐗╁搧妯″紡銆?*/
    private static void spawnItemAmmo(ServerLevel level, Player player, ItemStack ammo, Vec3 origin,
            Vec3 direction, int powerLevel, boolean riptide, float uncertainty) {
        GunProjectile projectile = new GunProjectile(level, origin.x, origin.y, origin.z, ammo);
        projectile.setMode(GunProjectile.MODE_ITEM);
        projectile.setPowerLevel(powerLevel);
        projectile.setRiptide(riptide);
        projectile.setOwner(player);
        // 鍔涢噺 V锛氬儚鐏悆涓€鏍锋部鍑嗘槦鐩寸嚎椋烇紝鍘绘帀鍙戝皠鍣ㄥ師鏈夌殑 +0.1 鎶崌
        double vy = powerLevel >= 5 ? direction.y : direction.y + 0.1;
        projectile.shoot(direction.x, vy, direction.z, projectileSpeed(powerLevel), uncertainty);
        level.addFreshEntity(projectile);
    }

    /** 鍙戝皠鍣ㄦ灙涔熻兘鍙戝皠妯＄粍鐨勬案鎭掔墿鍝佲€斺€旇繖浜涚墿鍝佷笉娑堣€椼€?*/
    private static boolean isEternalItem(ItemStack stack) {
        var key = net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(stack.getItem());
        return key != null && EternalFoodMod.MOD_ID.equals(key.getNamespace());
    }

    /** 3 级子弹使用的弹药类型。 */
    private static boolean isBulletAmmo(ItemStack stack) {
        return stack.is(Items.ARROW) || stack.is(Items.TIPPED_ARROW)
                || stack.is(Items.SPECTRAL_ARROW)
                || stack.is(ModItems.ETERNAL_TIPPED_ARROW.get());
    }

    private static void spawnProjectileAmmo(ServerLevel level, Player player, ItemStack ammo, Vec3 origin,
            Vec3 direction, int powerLevel, boolean flame, int piercing, boolean infinity,
            boolean riptide, float uncertainty, boolean silk, boolean sideArrow) {
        if (ammo.getItem() instanceof SpawnEggItem) {
            GunProjectile projectile = new GunProjectile(level, origin.x, origin.y, origin.z, ammo);
            projectile.setMode(GunProjectile.MODE_SPAWN_EGG);
            projectile.setInfinite(false);
            projectile.setRiptide(riptide);
            projectile.setPowerLevel(powerLevel);
            projectile.setOwner(player);
            double vy = powerLevel >= 5 ? direction.y : direction.y + 0.1;
            projectile.shoot(direction.x, vy, direction.z, projectileSpeed(powerLevel), uncertainty);
            level.addFreshEntity(projectile);
            return;
        }
        if (powerLevel >= 3 && isBulletAmmo(ammo)) {
            // 3 级：外观是箭的自定义子弹，完全绕过原版箭的命中/弹开逻辑
            cn.autoforged.eternal_food_mod_1789477527.entity.GunBullet bullet =
                    new cn.autoforged.eternal_food_mod_1789477527.entity.GunBullet(level, player);
            if (ammo.is(ModItems.ETERNAL_TIPPED_ARROW.get())) {
                EternalTippedArrowItem.applyEffects(bullet, ammo);
            } else if (ammo.is(Items.TIPPED_ARROW)) {
                bullet.setEffectsFromItem(ammo);
            }
            bullet.setBulletDamage(Math.max(1,
                    (int) Math.ceil(projectileSpeed(powerLevel) * 2.0)));
            bullet.shoot(direction.x, direction.y, direction.z, projectileSpeed(powerLevel), uncertainty);
            level.addFreshEntity(bullet);
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
            double vy = powerLevel >= 5 ? direction.y : direction.y + 0.1;
            projectile.shoot(direction.x, vy, direction.z, projectileSpeed(powerLevel), uncertainty);
        }
        projectile.setOwner(player);
        if (flame) {
            projectile.setSecondsOnFire(100);
        }
        if (piercing > 0 && projectile instanceof AbstractArrow arrow) {
            arrow.setPierceLevel((byte) Math.min(piercing, 10));
        }
        if (infinity) {
            GunProjectileTracker.markInfinite(projectile);
        }
        if (projectile instanceof AbstractArrow arrow) {
            // 榛樿鍜屽師鐗堜竴鏍峰彲鎹″洖鑳屽寘锛涗互涓嬫儏鍐典笉鍙鐢熷瓨鐜╁鎷惧彇锛堝垱閫犳ā寮忔崱鍒颁絾鎷夸笉鍒颁笢瑗匡紝
            // 涓庡師鐗?CREATIVE_ONLY 涓€鑷达級锛?            //   闄勯瓟鏃犻檺 / 澶氶噸灏勫嚮鐨勪袱鏀晶绠?/ 姘告亽鑽
            boolean unpickable = infinity || sideArrow
                    || ammo.is(ModItems.ETERNAL_TIPPED_ARROW.get());
            arrow.pickup = unpickable ? AbstractArrow.Pickup.CREATIVE_ONLY
                    : AbstractArrow.Pickup.ALLOWED;
        }
        if (riptide) {
            GunProjectileTracker.markRiptide(projectile);
        }
        // 力量：减少空气阻力 + 重力（火球 / 烟花这类不按普通物理飞的不处理）
        if (!(projectile instanceof AbstractHurtingProjectile)
                && !(projectile instanceof FireworkRocketEntity)) {
            GunProjectileTracker.markPowered(projectile, powerLevel);
        }
        // 3 级子弹：关闭原版箭碰撞，命中完全由 GunProjectileTracker 手动处理
        if (powerLevel >= 3 && projectile instanceof AbstractArrow arrow) {
            arrow.setNoPhysics(true);
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

    /** 鎭堕瓊鐏悆 / 鍑嬬伒涔嬮锛氫笌鍙戝皠鍣ㄤ竴鑷达紝鐢ㄤ笁瑙掑舰鏁ｅ竷 + 0.1 鎺ㄨ繘鍔涳紙绮惧噯閲囬泦鏃舵暎甯冧负 0锛夈€?*/
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


    /** 打印速度：力量越高越快，5 级对齐原版弩的 3.15（约 100 格射程）。 */
    private static float projectileSpeed(int powerLevel) {
        // 0 级 = 原版发射器；1 级 = 弓；2 级及以上 = 原版弩。
        int power = Math.max(0, Math.min(MAX_POWER, powerLevel));
        return switch (power) {
            case 0 -> 1.1F;
            case 1 -> 3.0F;
            default -> 3.15F;
        };
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
        tooltip.add(Component.translatable("tooltip.eternal_food_mod_1789477527.dispenser_gun.open_ammo")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.eternal_food_mod_1789477527.dispenser_gun.open_config")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.eternal_food_mod_1789477527.dispenser_gun.delay",
                String.format(java.util.Locale.ROOT, "%.2f", getFireDelay(stack) / 20.0F))
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable(
                "tooltip.eternal_food_mod_1789477527.dispenser_gun.shift")
                .withStyle(ChatFormatting.DARK_GRAY));
    }

    /** Shift 鎻愮ず鐢細褰撳墠寮€鍚殑閰嶇疆璇存槑銆?*/
    public static List<Component> activeConfigLines(ItemStack gun) {
        List<Component> lines = new ArrayList<>();
        if (getToggle(gun, CFG_INFINITY)) {
            lines.add(effectLine("infinity"));
        }
        int power = getPower(gun);
        if (power > 0) {
            lines.add(Component.translatable(
                    "tooltip.eternal_food_mod_1789477527.dispenser_gun.effect.power",
                    Component.translatable("enchantment.level." + power))
                    .withStyle(ChatFormatting.GRAY));
        }
        if (getToggle(gun, CFG_FLAME)) {
            lines.add(effectLine("flame"));
        }
        if (getToggle(gun, CFG_MULTISHOT)) {
            lines.add(effectLine("multishot"));
        }
        int piercing = getPiercing(gun);
        if (piercing > 0) {
            lines.add(Component.translatable(
                    "tooltip.eternal_food_mod_1789477527.dispenser_gun.effect.piercing", piercing)
                    .withStyle(ChatFormatting.GRAY));
        }
        if (getToggle(gun, CFG_WATERPROOF)) {
            lines.add(effectLine("waterproof"));
        }
        if (getToggle(gun, CFG_SILK)) {
            lines.add(effectLine("silk_touch"));
        }
        return lines;
    }

    private static Component effectLine(String name) {
        return Component.translatable(
                "tooltip.eternal_food_mod_1789477527.dispenser_gun.effect." + name)
                .withStyle(ChatFormatting.GRAY);
    }
}

