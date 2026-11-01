package cn.autoforged.eternal_food_mod_1789477527.item;

import cn.autoforged.eternal_food_mod_1789477527.network.ModNetwork;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * 永恒紫颂果 plus 系列。三个变体共用同一套"食用 → 传送、不消耗"的逻辑，差别只在传送目标：
 *
 * <ul>
 *   <li>{@link Mode#RESPAWN}「复活点」：传送到玩家的重生点（床 / 重生锚），没有重生点时回退到
 *       主世界出生点；</li>
 *   <li>{@link Mode#WORLD_SPAWN}「生成点」：无视重生点，直接传送到主世界的世界出生点；</li>
 *   <li>{@link Mode#BOUND}「绑定」：传送到磁石绑定记录下来的坐标（y + 1，落在磁石顶上）。</li>
 * </ul>
 *
 * <p>绑定版本只由「拿着复活点 / 生成点右键磁石」产生（见 {@code ChorusFruitBindHandler}），
 * 且<b>不能再次绑定</b>，避免误触覆盖掉最初记下的坐标。
 *
 * <p>基础物品用苹果：{@link EternalFoodItem} 对 {@code Items.CHORUS_FRUIT} 会复刻原版的
 * <em>随机</em>传送，那不是这里想要的行为，所以换一个普通食物基类，由本类自行实现定点传送。
 */
public class EternalChorusFruitPlusItem extends EternalFoodItem {

    /** three 变体的传送模式。 */
    public enum Mode {
        RESPAWN,
        WORLD_SPAWN,
        BOUND
    }

    /** 绑定信息的 NBT 键：维度 id + 方块坐标。 */
    public static final String BIND_DIMENSION = "bind_dimension";
    public static final String BIND_X = "bind_x";
    public static final String BIND_Y = "bind_y";
    public static final String BIND_Z = "bind_z";

    private final Mode mode;

    public EternalChorusFruitPlusItem(Properties properties, Mode mode) {
        super(Items.APPLE, properties, false, mode == Mode.BOUND ? null : hintKey(mode));
        this.mode = mode;
    }

    private static String hintKey(Mode mode) {
        return mode == Mode.WORLD_SPAWN
                ? "tooltip.eternal_food_mod_1789477527.chorus_fruit_plus_spawn"
                : "tooltip.eternal_food_mod_1789477527.chorus_fruit_plus_respawn";
    }

    /** 和干海带一样快：16 tick（原版普通食物 32 tick）。 */
    @Override
    public int getUseDuration(ItemStack stack) {
        return 16;
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        ItemStack result = super.finishUsingItem(stack, level, entity);
        if (!level.isClientSide && entity instanceof ServerPlayer player) {
            if (teleportByMode(player, stack)) {
                // 传送的同时让客户端用本物品播放一次不死图腾激活动画，避免误以为已经吃完。
                ModNetwork.sendTotemAnimation(player, stack.copy());
            }
        }
        return result;
    }

    /**
     * 按本变体的模式执行一次主动传送（仅服务端调用）：
     * 复活点 / 世界出生点 / 磁石绑定点。
     *
     * @return true 表示传送成功；false 表示失败（例如绑定版没有绑定过磁石）
     */
    public boolean teleportByMode(ServerPlayer player, ItemStack stack) {
        switch (this.mode) {
            case RESPAWN:
                teleportToRespawn(player);
                return true;
            case WORLD_SPAWN:
                teleportToWorldSpawn(player);
                return true;
            case BOUND:
            default:
                return teleportToBound(player, stack);
        }
    }

    /** 把玩家传送到其重生点；没有有效重生点则传到主世界世界出生点（仅服务端调用）。 */
    private static void teleportToRespawn(ServerPlayer player) {
        ServerLevel target = player.getServer() == null
                ? null
                : player.getServer().getLevel(player.getRespawnDimension());
        Vec3 destination = null;
        if (target != null && player.getRespawnPosition() != null) {
            // 末参 true = 不消耗重生锚充能，只解析落点。
            destination = Player.findRespawnPositionAndUseSpawnBlock(target, player.getRespawnPosition(),
                    player.getRespawnAngle(), player.isRespawnForced(), true).orElse(null);
        }
        if (destination == null) {
            target = player.getServer().overworld();
            BlockPos spawn = target.getSharedSpawnPos();
            destination = new Vec3(spawn.getX() + 0.5D, spawn.getY(), spawn.getZ() + 0.5D);
        }
        moveTo(player, target, destination);
    }

    /** 无视个人重生点，直接回到主世界的世界出生点（仅服务端调用）。 */
    private static void teleportToWorldSpawn(ServerPlayer player) {
        ServerLevel target = player.getServer().overworld();
        BlockPos spawn = target.getSharedSpawnPos();
        moveTo(player, target, new Vec3(spawn.getX() + 0.5D, spawn.getY(), spawn.getZ() + 0.5D));
    }

    /** 传送到磁石绑定坐标：x/z 取方块中心，y 加 1 让玩家正好站在磁石顶上。 */
    private static boolean teleportToBound(ServerPlayer player, ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(BIND_DIMENSION, 8)) {
            player.displayClientMessage(
                    Component.translatable("message.eternal_food_mod_1789477527.chorus_fruit_plus_unbound"),
                    true);
            return false;
        }
        ResourceLocation dimensionId = ResourceLocation.tryParse(tag.getString(BIND_DIMENSION));
        ServerLevel target = dimensionId == null ? null
                : player.getServer().getLevel(ResourceKey.create(Registries.DIMENSION, dimensionId));
        if (target == null) {
            player.displayClientMessage(
                    Component.translatable("message.eternal_food_mod_1789477527.chorus_fruit_plus_dim_gone"),
                    true);
            return false;
        }
        Vec3 destination = new Vec3(
                tag.getInt(BIND_X) + 0.5D,
                tag.getInt(BIND_Y) + 1.0D,
                tag.getInt(BIND_Z) + 0.5D);
        moveTo(player, target, destination);
        return true;
    }

    private static void moveTo(ServerPlayer player, ServerLevel target, Vec3 destination) {
        player.stopRiding();
        player.teleportTo(target, destination.x, destination.y, destination.z,
                player.getYRot(), player.getXRot());
        target.playSound(null, destination.x, destination.y, destination.z,
                SoundEvents.CHORUS_FRUIT_TELEPORT, SoundSource.PLAYERS, 1.0F, 1.0F);
    }

    /** 把磁石信息写入物品 NBT（由右键磁石的事件调用）。 */
    public static void bindTo(ItemStack stack, ResourceKey<Level> dimension, BlockPos pos) {
        CompoundTag tag = stack.getOrCreateTag();
        tag.putString(BIND_DIMENSION, dimension.location().toString());
        tag.putInt(BIND_X, pos.getX());
        tag.putInt(BIND_Y, pos.getY());
        tag.putInt(BIND_Z, pos.getZ());
    }

    /** 物品是否已经绑定过磁石（绑定版不能二次绑定）。 */
    public static boolean isBound(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag != null && tag.contains(BIND_DIMENSION, 8);
    }

    /**
     * 把维度 id 转成玩家看得懂的名字：三个原版维度走翻译键，其它维度（模组维度）直接显示 id。
     * 放在这里而不是硬编码中文，是为了让 en_us 也能正确显示。
     */
    public static Component dimensionName(String dimensionId) {
        switch (dimensionId) {
            case "minecraft:overworld":
                return Component.translatable("dim.eternal_food_mod_1789477527.overworld");
            case "minecraft:the_nether":
                return Component.translatable("dim.eternal_food_mod_1789477527.the_nether");
            case "minecraft:the_end":
                return Component.translatable("dim.eternal_food_mod_1789477527.the_end");
            default:
                return Component.literal(dimensionId);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltipComponents,
            TooltipFlag isAdvanced) {
        if (this.mode == Mode.BOUND) {
            CompoundTag tag = stack.getTag();
            if (tag != null && tag.contains(BIND_DIMENSION, 8)) {
                tooltipComponents.add(Component
                        .translatable("tooltip.eternal_food_mod_1789477527.chorus_fruit_plus_bound_pos",
                                tag.getInt(BIND_X), tag.getInt(BIND_Y), tag.getInt(BIND_Z),
                                dimensionName(tag.getString(BIND_DIMENSION)))
                        .withStyle(ChatFormatting.BLUE));
                tooltipComponents.add(Component
                        .translatable("tooltip.eternal_food_mod_1789477527.chorus_fruit_plus_bound_trait")
                        .withStyle(ChatFormatting.BLUE));
                tooltipComponents.add(Component
                        .translatable("tooltip.eternal_food_mod_1789477527.chorus_fruit_plus_bound_locked")
                        .withStyle(ChatFormatting.GRAY));
            }
        }
        super.appendHoverText(stack, level, tooltipComponents, isAdvanced);
        // 三个变体都具备的被动：坠入虚空受到虚空伤害时自动传送到安全位置（见 VoidRescueHandler）
        tooltipComponents.add(Component
                .translatable("tooltip.eternal_food_mod_1789477527.chorus_fruit_plus_void")
                .withStyle(ChatFormatting.DARK_PURPLE));
    }
}
