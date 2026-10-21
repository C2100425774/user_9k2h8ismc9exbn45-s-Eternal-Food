package cn.autoforged.eternal_food_mod_1789477527.mixin;

import cn.autoforged.eternal_food_mod_1789477527.brew.EternalFuelBrewingAccess;
import cn.autoforged.eternal_food_mod_1789477527.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 让永恒燃料在酿造台里等同于烈焰粉：放入燃料槽（0 号槽为顶部三个药水槽，4 号槽为燃料槽）
 * 即可补充 20 能量（可酿造 20 瓶药），且不消耗。
 *
 * <p>原版 {@code BrewingStandBlockEntity.serverTick} 硬编码只认可烈焰粉
 * （{@code itemstack.is(Items.BLAZE_POWDER)}）并把燃料消耗掉；这里在 tick 开头补一段：
 * 若燃料槽里是永恒燃料且当前没有能量，则补满 20 且不减少物品堆叠。
 */
@Mixin(BrewingStandBlockEntity.class)
public abstract class BrewingStandBlockEntityMixin implements EternalFuelBrewingAccess {

    // 之所以不用 @Shadow：
    //   @Shadow 字段在正式环境要靠 refmap 把 "fuel" 映射成 SRG 名 f_58979_，
    //   而 asar 生成/我们补的 refmap 条目一旦格式不对，Mixin 就会在启动阶段直接崩。
    //   accesstransformer.cfg 已经把 fuel 放开成 public，所以这里直接访问字段；
    //   ForgeGradle 的 reobf 会自动把字节码里的引用重写成 f_58979_，不依赖 refmap。
    private BrewingStandBlockEntity eternal_food$self() {
        return (BrewingStandBlockEntity) (Object) this;
    }

    @Override
    public int eternal_food$getFuel() {
        return this.eternal_food$self().fuel;
    }

    @Override
    public void eternal_food$setFuel(int value) {
        this.eternal_food$self().fuel = value;
    }

    @Inject(method = "serverTick", at = @At("HEAD"))
    private static void eternal_food$refillWithEternalFuel(Level level, BlockPos pos, BlockState state,
            BrewingStandBlockEntity brewingStand, CallbackInfo ci) {
        EternalFuelBrewingAccess access = (EternalFuelBrewingAccess) (Object) brewingStand;
        if (access.eternal_food$getFuel() > 0) {
            return;
        }
        ItemStack fuelStack = brewingStand.getItem(4);
        if (fuelStack.is(ModItems.ETERNAL_FUEL.get())) {
            access.eternal_food$setFuel(20);
            brewingStand.setChanged();
        }
    }

    @Inject(method = "canPlaceItem", at = @At("HEAD"), cancellable = true)
    private void eternal_food$allowEternalFuelInSlot(int index, ItemStack stack,
            CallbackInfoReturnable<Boolean> cir) {
        if (index == 4 && stack.is(ModItems.ETERNAL_FUEL.get())) {
            cir.setReturnValue(true);
        }
    }
}
