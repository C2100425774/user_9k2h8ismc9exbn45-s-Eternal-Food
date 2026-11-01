package cn.autoforged.eternal_food_mod_1789477527.event;

import cn.autoforged.eternal_food_mod_1789477527.EternalFoodMod;
import cn.autoforged.eternal_food_mod_1789477527.item.EternalChorusFruitPlusItem;
import cn.autoforged.eternal_food_mod_1789477527.item.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 「永恒紫颂果 plus」与磁石的绑定：
 * 拿着<b>复活点</b>或<b>生成点</b>右键磁石，手里的物品就地变成<b>绑定版</b>并记下磁石的维度与坐标。
 *
 * <ul>
 *   <li>创造模式同样生效：直接替换手上的 ItemStack，不依赖"消耗物品"逻辑；</li>
 *   <li>绑定版<b>不会</b>再次响应磁石（防止误触覆盖掉最初记下的坐标）；</li>
 *   <li>客户端也执行替换，保证不出现"手上还是旧物品"的短暂错位。</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = EternalFoodMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ChorusFruitBindHandler {

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        ItemStack held = event.getItemStack();
        boolean bindable = held.is(ModItems.ETERNAL_CHORUS_FRUIT_PLUS.get())
                || held.is(ModItems.ETERNAL_CHORUS_FRUIT_PLUS_SPAWN.get());
        if (!bindable) {
            // 绑定版走到这里说明玩家想再绑一次：直接什么都不做，交给原版。
            return;
        }

        Level level = event.getLevel();
        BlockPos pos = event.getPos();
        if (!level.getBlockState(pos).is(Blocks.LODESTONE)) {
            return;
        }

        Player player = event.getEntity();
        ItemStack bound = new ItemStack(ModItems.ETERNAL_CHORUS_FRUIT_PLUS_BIND.get());
        EternalChorusFruitPlusItem.bindTo(bound, level.dimension(), pos);
        // 两端都改手里的栈：客户端立刻生效，服务端负责真正同步，避免"手上还是旧物品"的闪烁。
        player.setItemInHand(event.getHand(), bound);

        if (!level.isClientSide) {
            level.playSound(null, pos, SoundEvents.LODESTONE_COMPASS_LOCK, SoundSource.PLAYERS, 1.0F, 1.0F);
            player.displayClientMessage(
                    Component.translatable("message.eternal_food_mod_1789477527.chorus_fruit_plus_bound",
                            pos.getX(), pos.getY(), pos.getZ(),
                            EternalChorusFruitPlusItem.dimensionName(level.dimension().location().toString()))
                            .withStyle(ChatFormatting.AQUA),
                    true);
        }

        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
    }
}
