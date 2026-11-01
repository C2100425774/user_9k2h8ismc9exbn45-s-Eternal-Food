package cn.autoforged.eternal_food_mod_1789477527.block.entity;

import cn.autoforged.eternal_food_mod_1789477527.util.ClockClientState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 永恒数字钟的方块实体。
 *
 * <p>它不保存任何数据：显示内容完全由世界时间推导（{@link net.minecraft.world.level.Level#getDayTime()}），
 * 因此不需要 ticker，也不需要 NBT 同步。
 *
 * <p>注意：时间被锁定后不能再用客户端本地时间（客户端会自己往前走），
 * 这里统一走 {@link ClockClientState#effectiveDayTime}，锁定后用服务端回包快照。
 */
public class EternalClockBlockEntity extends BlockEntity {

    public EternalClockBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ETERNAL_CLOCK.get(), pos, state);
    }

    /** 当前显示的小时（0~23，24 小时制）。 */
    public int hour() {
        long dayTime = ClockClientState.effectiveDayTime(this.level);
        return (int) (((Math.floorMod(dayTime, 24000L) / 1000L) + 6L) % 24L);
    }

    /** 当前显示的分钟（0~59）。 */
    public int minute() {
        long dayTime = ClockClientState.effectiveDayTime(this.level);
        return (int) ((Math.floorMod(dayTime, 24000L) % 1000L) * 60L / 1000L);
    }
}
