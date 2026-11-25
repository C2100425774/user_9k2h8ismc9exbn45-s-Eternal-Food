package cn.autoforged.eternal_food_mod_1789477527.item;

import java.lang.reflect.Method;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.CandleCakeBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidHandlerItemStack;
import net.minecraft.tags.FluidTags;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.ChatFormatting;
import java.util.List;

/**
 * 打火机：64 耐久，行为与原版打火石一致；Shift+右键开关盖子。
 * 开盖才能点火，点火消耗 1 耐久；耐久耗尽不会消失，可用燧石修复。
 *
 * <p>兼容：营火/蜡烛、农夫乐事炉灶、森罗物语：厨房炉灶；
 * 也可挂在森罗物语：厨房的厨具架上。
 */
public class EternalLighterItem extends Item {

    public static final String OPEN_TAG = "LighterOpen";
    public static final int MAX_DURABILITY = 64;
    public static final int FLUID_CAPACITY = 200;

    /** 默认可燃流体标签。 */
    private static final List<TagKey<Fluid>> FUEL_TAGS = List.of(
            FluidTags.create(new ResourceLocation("forge", "biodiesel")),
            FluidTags.create(new ResourceLocation("forge", "diesel")),
            FluidTags.create(new ResourceLocation("forge", "ethanol")),
            FluidTags.create(new ResourceLocation("forge", "gasoline")),
            FluidTags.create(new ResourceLocation("forge", "plantoil")));

    public EternalLighterItem(Properties properties) {
        super(properties);
    }

    public static boolean isOpen(ItemStack stack) {
        return stack.hasTag() && stack.getTag().getBoolean(OPEN_TAG);
    }

    private static void setOpen(ItemStack stack, boolean open) {
        stack.getOrCreateTag().putBoolean(OPEN_TAG, open);
    }

    @Override
    public ICapabilityProvider initCapabilities(ItemStack stack, CompoundTag nbt) {
        return new FluidHandlerItemStack(stack, FLUID_CAPACITY) {
            @Override
            public boolean canFillFluidType(FluidStack fluid) {
                return isFuel(fluid);
            }
        };
    }

    public static FluidStack getFluid(ItemStack stack) {
        return stack.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM)
                .map(handler -> handler.getFluidInTank(0))
                .orElse(FluidStack.EMPTY);
    }

    public static boolean isFuel(FluidStack fluid) {
        if (fluid.isEmpty()) {
            return false;
        }
        for (TagKey<Fluid> tag : FUEL_TAGS) {
            if (fluid.getFluid().builtInRegistryHolder().is(tag)) {
                return true;
            }
        }
        return false;
    }

    public static boolean hasFuel(ItemStack stack) {
        FluidStack fluid = getFluid(stack);
        return !fluid.isEmpty() && isFuel(fluid) && fluid.getAmount() > 0;
    }

    /** 有点火流体就消耗 1mB，否则消耗 1 点耐久。 */
    public static void consumeFuelOrDurability(Level level, ItemStack stack, Player player) {
        var handler = stack.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).resolve();
        if (handler.isPresent()) {
            FluidStack fluid = handler.get().getFluidInTank(0);
            if (!fluid.isEmpty() && isFuel(fluid) && fluid.getAmount() > 0) {
                handler.get().drain(1, IFluidHandler.FluidAction.EXECUTE);
                return;
            }
        }
        damage(level, stack, player);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        ItemStack stack = context.getItemInHand();

        if (player != null && player.isShiftKeyDown()) {
            boolean open = !isOpen(stack);
            setOpen(stack, open);
            level.playSound(null, pos, SoundEvents.LEVER_CLICK, SoundSource.PLAYERS,
                    0.35F, open ? 1.2F : 0.8F);
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        BlockState state = level.getBlockState(pos);
        // 合盖：像锹一样扑灭营火 / 炉灶
        if (!isOpen(stack)) {
            if (tryExtinguishBlock(level, pos, state, player)) {
                damage(level, stack, player);
                return InteractionResult.sidedSuccess(level.isClientSide);
            }
            return InteractionResult.PASS;
        }
        if (!isCreative(player) && stack.getDamageValue() >= stack.getMaxDamage()
                && !hasFuel(stack)) {
            return InteractionResult.FAIL;
        }
        if (tryIgniteBlock(level, pos, state, player, context.getClickedFace(),
                context.getHorizontalDirection(), false)) {
            consumeFuelOrDurability(level, stack, player);
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return InteractionResult.PASS;
    }

    @Override
    public net.minecraft.world.InteractionResultHolder<ItemStack> use(Level level, Player player,
            InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown()) {
            boolean open = !isOpen(stack);
            setOpen(stack, open);
            level.playSound(null, player.blockPosition(), SoundEvents.LEVER_CLICK,
                    SoundSource.PLAYERS, 0.35F, open ? 1.2F : 0.8F);
            return net.minecraft.world.InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
        return net.minecraft.world.InteractionResultHolder.pass(stack);
    }

    /** 消耗 1 耐久；耐久附魔按原版概率抵消。 */
    public static void damage(Level level, ItemStack stack, Player player) {
        if (player != null && player.isCreative()) {
            return;
        }
        int unbreaking = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.UNBREAKING, stack);
        if (unbreaking > 0 && level.getRandom().nextInt(unbreaking + 1) > 0) {
            return;
        }
        stack.setDamageValue(Math.min(stack.getMaxDamage(), stack.getDamageValue() + 1));
    }

    /** 发射器与右键共用的点火逻辑。 */
    public static boolean tryIgniteBlock(Level level, BlockPos pos, BlockState state, Entity source,
            Direction face, Direction horizontal, boolean fireAtPos) {
        // 原版营火 / 蜡烛 / 蜡烛蛋糕
        if (CampfireBlock.canLight(state) || CandleBlock.canLight(state)
                || CandleCakeBlock.canLight(state)) {
            playIgniteSound(level, pos, source);
            level.setBlock(pos, state.setValue(BlockStateProperties.LIT, Boolean.TRUE), 11);
            level.gameEvent(source, GameEvent.BLOCK_CHANGE, pos);
            return true;
        }

        // 原版打火石也能点燃 TNT
        if (state.getBlock() instanceof net.minecraft.world.level.block.TntBlock tnt) {
            tnt.onCaughtFire(state, level, pos, face,
                    source instanceof net.minecraft.world.entity.LivingEntity living ? living : null);
            // onCaughtFire 只生成引信 TNT，不会移除原方块；不移除会复制 TNT
            level.removeBlock(pos, false);
            return true;
        }
        // 模组 TNT：复用其 onCaughtFire（原版 TntBlock.use 也是先调用它再移除方块）
        if (state.getBlock().getClass().getName().toLowerCase().contains("tnt")) {
            try {
                Method caught = state.getBlock().getClass().getMethod("onCaughtFire",
                        BlockState.class, Level.class, BlockPos.class, Direction.class,
                        net.minecraft.world.entity.LivingEntity.class);
                caught.invoke(state.getBlock(), state, level, pos, face,
                        source instanceof net.minecraft.world.entity.LivingEntity living ? living : null);
                level.removeBlock(pos, false);
                return true;
            } catch (Throwable ignored) {
                // 没有 onCaughtFire 的模组 TNT：继续走普通点火逻辑
            }
        }

        String className = state.getBlock().getClass().getName();

        // 农夫乐事：炉灶（需要同时点亮 BlockEntity）
        if (className.contains("farmersdelight") && className.contains("StoveBlock")) {
            if (!state.hasProperty(BlockStateProperties.LIT)
                    || state.getValue(BlockStateProperties.LIT)) {
                return false;
            }
            try {
                Method ignite = state.getBlock().getClass().getMethod("ignite",
                        Entity.class, LevelAccessor.class, BlockPos.class, BlockState.class);
                ignite.invoke(state.getBlock(), source, level, pos, state);
                return true;
            } catch (Throwable ignored) {
                // 退化为直接设置 LIT
            }
            playIgniteSound(level, pos, source);
            level.setBlock(pos, state.setValue(BlockStateProperties.LIT, Boolean.TRUE), 11);
            level.gameEvent(source, GameEvent.BLOCK_CHANGE, pos);
            return true;
        }

        // 森罗物语：厨房炉灶
        if (className.contains("kaleidoscopecookery") && className.contains("StoveBlock")) {
            if (!state.hasProperty(BlockStateProperties.LIT)
                    || state.getValue(BlockStateProperties.LIT)) {
                return false;
            }
            playIgniteSound(level, pos, source);
            level.setBlock(pos, state.setValue(BlockStateProperties.LIT, Boolean.TRUE), 11);
            level.gameEvent(source, GameEvent.BLOCK_CHANGE, pos);
            return true;
        }

        // 原版打火石：在点击面相邻的位置放火
        BlockPos firePos = fireAtPos ? pos : pos.relative(face);
        if (BaseFireBlock.canBePlacedAt(level, firePos, horizontal)) {
            playIgniteSound(level, pos, source);
            level.setBlock(firePos, BaseFireBlock.getState(level, firePos), 11);
            level.gameEvent(source, GameEvent.BLOCK_PLACE, pos);
            return true;
        }
        return false;
    }

    private static boolean isCreative(Player player) {
        return player != null && player.isCreative();
    }

    /** 合盖扑灭：营火或农夫乐事/森罗物语炉灶。 */
    public static boolean tryExtinguishBlock(Level level, BlockPos pos, BlockState state, Entity source) {
        // 火焰方块：合盖直接扑灭
        if (state.getBlock() instanceof BaseFireBlock) {
            level.removeBlock(pos, false);
            level.levelEvent(1009, pos, 0);
            return true;
        }
        if (state.getBlock() instanceof CampfireBlock
                && state.hasProperty(BlockStateProperties.LIT)
                && state.getValue(BlockStateProperties.LIT)) {
            CampfireBlock.dowse(source, level, pos, state);
            level.levelEvent(1009, pos, 0);
            level.setBlock(pos, state.setValue(BlockStateProperties.LIT, Boolean.FALSE), 11);
            return true;
        }
        String className = state.getBlock().getClass().getName();
        if (className.contains("farmersdelight") && className.contains("StoveBlock")
                && state.hasProperty(BlockStateProperties.LIT)
                && state.getValue(BlockStateProperties.LIT)) {
            try {
                Method extinguish = state.getBlock().getClass().getMethod("extinguish",
                        Entity.class, LevelAccessor.class, BlockPos.class, BlockState.class);
                extinguish.invoke(state.getBlock(), source, level, pos, state);
                return true;
            } catch (Throwable ignored) {
                level.setBlock(pos, state.setValue(BlockStateProperties.LIT, Boolean.FALSE), 11);
                return true;
            }
        }
        if (className.contains("kaleidoscopecookery") && className.contains("StoveBlock")
                && state.hasProperty(BlockStateProperties.LIT)
                && state.getValue(BlockStateProperties.LIT)) {
            level.playSound(source instanceof Player player ? player : null, pos,
                    SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 1.0F, 1.0F);
            level.setBlock(pos, state.setValue(BlockStateProperties.LIT, Boolean.FALSE), 11);
            return true;
        }
        return false;
    }

    /** 直接写厨具架的左/右槽；side 由点击位置计算，和厨具架自己的判断一致。 */
    public static boolean hangOnRack(Level level, BlockPos pos, Player player, ItemStack stack, boolean left) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be == null) {
            return false;
        }
        try {
            String getterName = left ? "getItemLeft" : "getItemRight";
            Method getter = be.getClass().getMethod(getterName);
            ItemStack current = (ItemStack) getter.invoke(be);
            if (!current.isEmpty()) {
                return false;
            }
            String setterName = left ? "setItemLeft" : "setItemRight";
            Method setter = be.getClass().getMethod(setterName, ItemStack.class);
            setter.invoke(be, stack.copyWithCount(1));
            be.setChanged();
            level.sendBlockUpdated(pos, level.getBlockState(pos), level.getBlockState(pos), 3);
            // 挂到架子上就是把手里的打火机放上去，创造模式也消耗
            stack.shrink(1);
            level.playSound(null, pos, SoundEvents.ITEM_FRAME_ADD_ITEM, SoundSource.BLOCKS, 0.8F, 1.0F);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static void playIgniteSound(Level level, BlockPos pos, Entity source) {
        level.playSound(source instanceof Player player ? player : null, pos,
                SoundEvents.FLINTANDSTEEL_USE, SoundSource.BLOCKS, 1.0F,
                level.getRandom().nextFloat() * 0.4F + 0.8F);
    }

    public static boolean isKitchenwareRack(Level level, BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        return be != null && be.getClass().getName().contains("KitchenwareRacksBlockEntity");
    }

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
        return enchantment == Enchantments.UNBREAKING || enchantment == Enchantments.MENDING;
    }

    @Override
    public boolean isValidRepairItem(ItemStack stack, ItemStack repair) {
        return repair.is(Items.FLINT);
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        FluidStack fluid = getFluid(stack);
        if (!fluid.isEmpty()) {
            tooltip.add(Component.translatable(
                    "tooltip.eternal_food_mod_1789477527.lighter.fluid",
                    fluid.getDisplayName(), fluid.getAmount(), FLUID_CAPACITY)
                    .withStyle(ChatFormatting.AQUA));
        }
        tooltip.add(Component.translatable(isOpen(stack)
                ? "tooltip.eternal_food_mod_1789477527.lighter.open"
                : "tooltip.eternal_food_mod_1789477527.lighter.closed")
                .withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.translatable("tooltip.eternal_food_mod_1789477527.lighter.toggle")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.eternal_food_mod_1789477527.lighter.repair")
                .withStyle(ChatFormatting.DARK_GRAY));
    }
}
