package cn.autoforged.eternal_food_mod_1789477527.item;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * 永恒刷怪蛋：单个物品，通过 NBT（{@code EntityTag.id}）记录要生成的生物类型。
 *
 * <p>完全继承原版 {@link SpawnEggItem} 的行为（方块放置、液体中生成、刷怪笼改类型、右键成年体生成幼年体、
 * 发射器发射），区别仅在于**任何途径都不消耗**该物品。
 *
 * <p>渲染分三层（见 datagen/ModItemModelProvider）：layer0 = {@code eternal_spawn_egg}（金色永恒描边，
 * 不染色），layer1 = {@code spawn_egg}（用背景色相乘），layer2 = {@code spawn_egg_overlay}（用高光色相乘）。
 * 颜色按栈内生物类型从对应原版刷怪蛋取用（闪电束用苦力怕配色、唤魔者尖牙用唤魔者配色）。
 */
public class EternalSpawnEggItem extends SpawnEggItem {

    /** NBT 中记录生物类型的键，与原版刷怪蛋一致。 */
    public static final String ENTITY_TAG = "EntityTag";
    public static final String ID_TAG = "id";

    public EternalSpawnEggItem(Properties properties) {
        // 默认类型传 null，避免污染原版 SpawnEggItem.BY_ID 映射；实际类型从 NBT 读取。
        super(null, 0xFFFFFF, 0xFFFFFF, properties);
    }

    /** 没有 NBT 时的回退类型（保证 requiredFeatures() 等不 NPE）。 */
    @Override
    protected EntityType<?> getDefaultType() {
        return EntityType.PIG;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (!(level instanceof ServerLevel serverLevel)) {
            return InteractionResult.SUCCESS;
        }

        ItemStack stack = context.getItemInHand();
        BlockPos pos = context.getClickedPos();
        Direction direction = context.getClickedFace();
        BlockState state = level.getBlockState(pos);

        // 刷怪笼：只改生成的生物 id，不写入刷怪蛋里生物的其他附加数据（与原版一致）。
        if (state.is(Blocks.SPAWNER)) {
            BlockEntity blockentity = level.getBlockEntity(pos);
            if (blockentity instanceof SpawnerBlockEntity spawner) {
                EntityType<?> type = this.getType(stack.getTag());
                spawner.setEntityId(type, level.getRandom());
                blockentity.setChanged();
                level.sendBlockUpdated(pos, state, state, 3);
                level.gameEvent(context.getPlayer(), GameEvent.BLOCK_CHANGE, pos);
                // 不 shrink：使用不消耗。
                return InteractionResult.CONSUME;
            }
        }

        BlockPos spawnPos = state.getCollisionShape(level, pos).isEmpty() ? pos : pos.relative(direction);
        EntityType<?> type = this.getType(stack.getTag());
        if (type.spawn(serverLevel, stack, context.getPlayer(), spawnPos, MobSpawnType.SPAWN_EGG, true,
                !Objects.equals(pos, spawnPos) && direction == Direction.UP) != null) {
            // 不 shrink：使用不消耗。
            level.gameEvent(context.getPlayer(), GameEvent.ENTITY_PLACE, pos);
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        BlockHitResult hit = getPlayerPOVHitResult(level, player, ClipContext.Fluid.SOURCE_ONLY);
        if (hit.getType() != HitResult.Type.BLOCK) {
            return InteractionResultHolder.pass(stack);
        }
        if (!(level instanceof ServerLevel serverLevel)) {
            return InteractionResultHolder.success(stack);
        }

        BlockPos pos = hit.getBlockPos();
        if (!(level.getBlockState(pos).getBlock() instanceof LiquidBlock)) {
            return InteractionResultHolder.pass(stack);
        }
        if (!level.mayInteract(player, pos) || !player.mayUseItemAt(pos, hit.getDirection(), stack)) {
            return InteractionResultHolder.fail(stack);
        }

        EntityType<?> type = this.getType(stack.getTag());
        Entity entity = type.spawn(serverLevel, stack, player, pos, MobSpawnType.SPAWN_EGG, false, false);
        if (entity == null) {
            return InteractionResultHolder.pass(stack);
        }
        // 不 shrink：使用不消耗。
        player.awardStat(Stats.ITEM_USED.get(this));
        level.gameEvent(player, GameEvent.ENTITY_PLACE, entity.position());
        return InteractionResultHolder.consume(stack);
    }

    /**
     * 右键生物生成幼年体。逻辑与原版一致，唯一区别是**不消耗**刷怪蛋。
     * 对可繁殖生物走 {@link AgeableMob#getBreedOffspring(ServerLevel, AgeableMob)}
     *（保留“有概率生成复合生物”的原版行为），否则用 {@link EntityType#create(Level)}。
     */
    @Override
    public Optional<Mob> spawnOffspringFromSpawnEgg(Player player, Mob mob, EntityType<? extends Mob> type,
            ServerLevel level, Vec3 pos, ItemStack stack) {
        if (!this.spawnsEntity(stack.getTag(), type)) {
            return Optional.empty();
        }

        Mob baby;
        if (mob instanceof AgeableMob ageable) {
            baby = ageable.getBreedOffspring(level, ageable);
        } else {
            baby = type.create(level);
        }
        if (baby == null) {
            return Optional.empty();
        }
        baby.setBaby(true);
        if (!baby.isBaby()) {
            return Optional.empty();
        }

        baby.moveTo(pos.x(), pos.y(), pos.z(), 0.0F, 0.0F);
        level.addFreshEntityWithPassengers(baby);
        if (stack.hasCustomHoverName()) {
            baby.setCustomName(stack.getHoverName());
        }
        // 不 shrink：右键成年体生成幼年体不消耗。
        return Optional.of(baby);
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip,
            TooltipFlag flag) {
        EntityType<?> type = this.getType(stack.getTag());
        tooltip.add(Component
                .translatable("tooltip.eternal_food_mod_1789477527.spawn_egg_spawns", type.getDescription())
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.eternal_food_mod_1789477527.eternal_spawn_egg_use")
                .withStyle(ChatFormatting.GOLD));
    }

    /**
     * 物品染色（客户端侧由 RegisterColorHandlersEvent.Item 注册）：
     * tintIndex 0 = 永恒金色描边层（不染色，返回 -1），1 = 背景色层，2 = 高光色层。
     * 颜色从对应原版刷怪蛋取得；闪电束用苦力怕配色、唤魔者尖牙用唤魔者配色。
     */
    public static int getLayerColor(ItemStack stack, int tintIndex) {
        if (tintIndex == 0) {
            return -1;
        }
        SpawnEggItem base = baseEgg(stack);
        return base == null ? 0xFFFFFF : base.getColor(tintIndex - 1);
    }

    private static SpawnEggItem baseEgg(ItemStack stack) {
        if (!(stack.getItem() instanceof EternalSpawnEggItem egg)) {
            return null;
        }
        EntityType<?> type = egg.getType(stack.getTag());
        if (type == EntityType.LIGHTNING_BOLT) {
            return (SpawnEggItem) Items.CREEPER_SPAWN_EGG;
        }
        if (type == EntityType.EVOKER_FANGS) {
            return (SpawnEggItem) Items.EVOKER_SPAWN_EGG;
        }
        return SpawnEggItem.byId(type);
    }

    /** 供创造模式页签 / 指令构造带类型 NBT 的永恒刷怪蛋。 */
    public static void setEntityType(ItemStack stack, EntityType<?> type) {
        CompoundTag entityTag = stack.getOrCreateTagElement(ENTITY_TAG);
        entityTag.putString(ID_TAG, EntityType.getKey(type).toString());
    }
}
