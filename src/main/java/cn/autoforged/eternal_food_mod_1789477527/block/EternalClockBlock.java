package cn.autoforged.eternal_food_mod_1789477527.block;

import cn.autoforged.eternal_food_mod_1789477527.block.entity.EternalClockBlockEntity;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;

import java.util.Optional;

/**
 * 永恒数字钟方块：一块贴在墙上的薄板（3/16 格厚），正面是数码屏。
 *
 * <p>{@link #FACING} 表示<b>屏幕朝向</b>；数字由 {@code EternalClockRenderer} 用 7 段数码管实时绘制。
 *
 * <p>方块特性：
 * <ul>
 *   <li>有碰撞箱（薄板体积），能挡住玩家和实体；</li>
 *   <li><b>可含水</b>：实现 {@link SimpleWaterloggedBlock}，放进水里会自动变成含水方块，
 *       破坏时把水放回去（与台阶、楼梯一致）；</li>
 *   <li>硬度 0.5 / 爆炸抗性 0.5（与标靶一致），不发光，不需要支撑；</li>
 *   <li>活塞推动时直接破坏并掉落（{@link PushReaction#DESTROY}）。</li>
 * </ul>
 *
 * <p>关于“水从哪些面通过”：原版流体的「能不能流进这一格」只由一个布尔量决定
 * （{@code canPassThroughWall} 看目标面是否为完整支撑面），**没有按面配置的 API**。
 * 因此只有两种标准做法：整块不透水（就含水方块，水被锁在格子里），或整块透水（就没有碰撞箱、
 * 会被水直接冲掉）。这里选前者：能挡水、能含水，代价是格内的水不会自己从其它面流出去。
 */
public class EternalClockBlock extends BaseEntityBlock implements SimpleWaterloggedBlock {

    /** 屏幕朝向（背离所贴墙面）。 */
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    /** 是否为含水方块。 */
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

    /** 薄板厚度：3/16 格。 */
    private static final double THICKNESS = 3.0D;

    private static final VoxelShape SHAPE_NORTH = Block.box(0.0D, 0.0D, 16.0D - THICKNESS, 16.0D, 16.0D, 16.0D);
    private static final VoxelShape SHAPE_SOUTH = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 16.0D, THICKNESS);
    private static final VoxelShape SHAPE_EAST = Block.box(0.0D, 0.0D, 0.0D, THICKNESS, 16.0D, 16.0D);
    private static final VoxelShape SHAPE_WEST = Block.box(16.0D - THICKNESS, 0.0D, 0.0D, 16.0D, 16.0D, 16.0D);

    public EternalClockBlock() {
        super(BlockBehaviour.Properties.of()
                .mapColor(MapColor.GOLD)
                // 硬度 / 爆炸抗性与标靶一致
                .strength(0.5F, 0.5F)
                .sound(SoundType.METAL)
                .noOcclusion()
                // 活塞推不动，直接把方块顶掉并掉落成物品
                .pushReaction(PushReaction.DESTROY));
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(WATERLOGGED, Boolean.FALSE));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, WATERLOGGED);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction clicked = context.getClickedFace();
        Direction facing = clicked.getAxis().isHorizontal()
                ? clicked
                : context.getHorizontalDirection().getOpposite();
        FluidState fluid = context.getLevel().getFluidState(context.getClickedPos());
        return this.defaultBlockState()
                .setValue(FACING, facing)
                .setValue(WATERLOGGED, fluid.getType() == Fluids.WATER);
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shapeOf(state);
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos,
            CollisionContext context) {
        return shapeOf(state);
    }

    private static VoxelShape shapeOf(BlockState state) {
        switch (state.getValue(FACING)) {
            case SOUTH:
                return SHAPE_SOUTH;
            case EAST:
                return SHAPE_EAST;
            case WEST:
                return SHAPE_WEST;
            case NORTH:
            default:
                return SHAPE_NORTH;
        }
    }

    // ---------------------------------------------------------------------
    // 含水方块
    // ---------------------------------------------------------------------

    @Override
    public FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    @Override
    public boolean canPlaceLiquid(BlockGetter level, BlockPos pos, BlockState state, Fluid fluid) {
        return !state.getValue(WATERLOGGED) && fluid == Fluids.WATER;
    }

    @Override
    public boolean placeLiquid(LevelAccessor level, BlockPos pos, BlockState state, FluidState fluidState) {
        if (!state.getValue(WATERLOGGED) && fluidState.getType() == Fluids.WATER) {
            if (!level.isClientSide()) {
                level.setBlock(pos, state.setValue(WATERLOGGED, Boolean.TRUE), 3);
                level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
            }
            return true;
        }
        return false;
    }

    @Override
    public ItemStack pickupBlock(LevelAccessor level, BlockPos pos, BlockState state) {
        if (state.getValue(WATERLOGGED)) {
            level.setBlock(pos, state.setValue(WATERLOGGED, Boolean.FALSE), 3);
            return new ItemStack(net.minecraft.world.item.Items.WATER_BUCKET);
        }
        return ItemStack.EMPTY;
    }

    @Override
    public Optional<SoundEvent> getPickupSound() {
        return Optional.of(net.minecraft.sounds.SoundEvents.BUCKET_FILL);
    }

    /** 相邻方块变化时，如果自己含着水，让水按原版节奏重新评估（与台阶一致）。 */
    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
            LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (state.getValue(WATERLOGGED)) {
            level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }
        return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }

    /** 破坏时把格内的水放回去，避免凭空消失一桶水。 */
    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock()) && state.getValue(WATERLOGGED) && !level.isClientSide) {
            level.setBlock(pos, Blocks.WATER.defaultBlockState(), 3);
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    /** 右键方块：打开时间 / 天气控制面板（和物品形态是同一个界面）。 */
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
            InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                    () -> () -> cn.autoforged.eternal_food_mod_1789477527.client.ClientHooks
                            .openClockScreen());
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new EternalClockBlockEntity(pos, state);
    }
}
