package cn.autoforged.eternal_food_mod_1789477527.event;

import cn.autoforged.eternal_food_mod_1789477527.EternalFoodMod;
import cn.autoforged.eternal_food_mod_1789477527.item.ModItems;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 永恒满置域合成：原版 {@link net.minecraft.world.inventory.ResultSlot} 每次合成只会从每个材料格
 * 消耗 1 个（{@code removeItem(i, 1)}），因此无法直接消耗「4 组（256 个）」。
 *
 * <p>{@link PlayerEvent.ItemCraftedEvent} 由 {@code ResultSlot#onTake} 在逐格消耗<b>之前</b>触发
 * （{@code ResultSlot.java:51} 先于 {@code :68-86}），此时工作台内容仍完整。这里在服务端把
 * 石头 / 深板岩材料格清空，使随后原版循环面对空格不再重复消耗，从而实现一次合成消耗全部
 * 256 石头 + 256 深板岩（下界之星由原版循环正常消耗）。
 */
@Mod.EventBusSubscriber(modid = EternalFoodMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class EternalFullDomainCraftHandler {

    @SubscribeEvent
    public static void onItemCrafted(PlayerEvent.ItemCraftedEvent event) {
        if (event.getCrafting().getItem() != ModItems.ETERNAL_FULL_DOMAIN.get()) {
            return;
        }
        Player player = event.getEntity();
        Container matrix = event.getInventory();
        if (player.level().isClientSide) {
            return;
        }
        for (int i = 0; i < matrix.getContainerSize(); i++) {
            ItemStack stack = matrix.getItem(i);
            if (stack.is(Items.STONE) || stack.is(Items.DEEPSLATE)) {
                matrix.setItem(i, ItemStack.EMPTY);
            }
        }
        matrix.setChanged();
    }
}
