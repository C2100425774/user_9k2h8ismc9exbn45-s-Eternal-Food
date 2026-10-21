package cn.autoforged.eternal_food_mod_1789477527.compat;

import cn.autoforged.eternal_food_mod_1789477527.block.ModBlocks;
import com.github.tartaricacid.touhoulittlemaid.api.block.IMaidEdibleBlock;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 让车万女仆（Touhou Little Maid）把「永恒蛋糕」当成蛋糕。
 *
 * <p>触发条件照抄 TLM 自带的原版蛋糕实现（{@code CakeEdible}）：
 * 蛋糕必须放在「零食柜」（{@code #touhou_little_maid:maid_snack_stand_block}）正上方，
 * 女仆才会走过去吃。
 *
 * <p>与原版蛋糕的区别：原版吃 6 口就消失，这里**方块永久保留**，只播放进食音效、
 * 照常给好感度（见 {@link #getFavorabilityPoints}）。
 */
public class EternalCakeEdible implements IMaidEdibleBlock {

    private static Level level(EntityMaid maid) {
        return maid.level();
    }

    @Override
    public boolean shouldMoveTo(EntityMaid maid, BlockPos pos, BlockState state) {
        return state.is(ModBlocks.ETERNAL_CAKE.get()) && IMaidEdibleBlock.belowIsSnackStand(maid, pos);
    }

    @Override
    public int getFavorabilityPoints(EntityMaid maid, BlockPos pos, BlockState state) {
        return 1;
    }

    @Override
    public boolean consume(EntityMaid maid, BlockPos pos, BlockState state) {
        Level level = level(maid);
        if (level.isClientSide) {
            return true;
        }
        maid.swing(InteractionHand.MAIN_HAND);
        level.playSound(null, pos, SoundEvents.GENERIC_EAT, SoundSource.BLOCKS, 1.0F, 1.0F);
        // 关键：不修改 BlockState、不移除方块 —— 永恒蛋糕被吃多少次都还在。
        return true;
    }

    /** 让女仆能主动把背包里的永恒蛋糕摆到零食柜上（对应 TLM 的「放食物」行为）。 */
    @Override
    public boolean canPlaceAsFood(EntityMaid maid, ItemStack stack, int slotIndex) {
        return stack.is(ModBlocks.ETERNAL_CAKE.get().asItem());
    }
}
