package cn.autoforged.eternal_food_mod_1789477527.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * 永恒蛋糕（方块）。
 *
 * <p>外形与原版蛋糕一致（1/16 缩进、8/16 高），但**没有咬痕状态**：
 * 右键只是让玩家吃到一整块蛋糕的营养（14 饥饿值 / 0.1 饱和系数），方块本身不会被消耗，
 * 也不会变成「咬过的蛋糕」。
 *
 * <p>之所以不复用原版 {@link net.minecraft.world.level.block.CakeBlock}：
 * 它自带 BITES 属性（0~6）并在 {@code use} 里递增，与「永恒、不消耗」的设定冲突；
 * 直接继承 {@link Block} 可以把状态机压到最简单，也避免 blockstate / 模型要多写 7 个变体。
 */
public class EternalCakeBlock extends Block {

    /** 与原版蛋糕一致的外形：横向缩进 1 像素，高 8 像素。 */
    private static final VoxelShape SHAPE = Block.box(1.0D, 0.0D, 1.0D, 15.0D, 8.0D, 15.0D);

    public EternalCakeBlock() {
        super(BlockBehaviour.Properties.of()
                .mapColor(MapColor.CLAY)
                .forceSolidOn()
                .strength(0.5F)
                .sound(SoundType.WOOL)
                .pushReaction(PushReaction.DESTROY));
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    /**
     * 右键食用：吃一整个蛋糕的量，但方块不消失。
     *
     * <p>饥饿值已满时返回 {@link InteractionResult#PASS}，与其它食物保持一致（不会「空吃」）。
     */
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
            InteractionHand hand, BlockHitResult hit) {
        if (!player.canEat(false)) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide) {
            // 传一个新的 ItemStack：Player#eat 会缩减传入的栈，这里不希望碰到玩家手上的东西。
            player.eat(level, new ItemStack(Items.CAKE));
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
