package cn.autoforged.eternal_food_mod_1789477527.event;

import cn.autoforged.eternal_food_mod_1789477527.item.EternalLighterItem;
import cn.autoforged.eternal_food_mod_1789477527.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockSource;
import net.minecraft.core.Direction;
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.state.BlockState;

/** 发射器：开盖时像打火石一样点火；合盖时按普通物品发射出去。 */
public class EternalLighterDispenseBehavior extends DefaultDispenseItemBehavior {

    @Override
    protected ItemStack execute(BlockSource source, ItemStack stack) {
        if (!(stack.getItem() instanceof EternalLighterItem)) {
            return super.execute(source, stack);
        }
        if (stack.getDamageValue() >= stack.getMaxDamage()) {
            return stack;
        }
        Direction facing = source.getBlockState().getValue(DispenserBlock.FACING);
        BlockPos front = source.getPos().relative(facing);
        BlockState state = source.getLevel().getBlockState(front);
        boolean success;
        if (EternalLighterItem.isOpen(stack)) {
            if (EternalLighterItem.isFluidLighter(stack) && !EternalLighterItem.hasFuel(stack)) {
                return stack;
            }
            // 开盖：点燃正前方（火焰就放在发射器口前一格）
            success = EternalLighterItem.tryIgniteBlock(source.getLevel(), front, state,
                    null, facing, facing, true);
            if (success) {
                success = EternalLighterItem.consumeIgnition(source.getLevel(), stack, null);
            }
        } else {
            // 合盖：扑灭正前方的营火 / 炉灶；液体打火机免费
            success = EternalLighterItem.tryExtinguishBlock(source.getLevel(), front, state, null);
            if (success && EternalLighterItem.isDurableLighter(stack)) {
                EternalLighterItem.damage(source.getLevel(), stack, null);
            }
        }
        if (success) {
            playSound(source);
            playAnimation(source, facing);
        }
        return stack;
    }
}
