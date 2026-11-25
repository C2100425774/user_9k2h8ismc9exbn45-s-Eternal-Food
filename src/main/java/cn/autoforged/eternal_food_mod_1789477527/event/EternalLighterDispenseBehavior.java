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
        if (!(stack.getItem() instanceof EternalLighterItem) || !EternalLighterItem.isOpen(stack)) {
            // 合盖：以物品形式发射
            return super.execute(source, stack);
        }
        if (stack.getDamageValue() >= stack.getMaxDamage()) {
            return stack;
        }
        Direction facing = source.getBlockState().getValue(DispenserBlock.FACING);
        BlockPos front = source.getPos().relative(facing);
        BlockState state = source.getLevel().getBlockState(front);
        if (EternalLighterItem.tryIgniteBlock(source.getLevel(), front, state, null, facing, facing)) {
            EternalLighterItem.damage(source.getLevel(), stack);
            playSound(source);
            playAnimation(source, facing);
        }
        return stack;
    }
}
