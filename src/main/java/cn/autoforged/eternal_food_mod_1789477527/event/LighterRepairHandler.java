package cn.autoforged.eternal_food_mod_1789477527.event;

import cn.autoforged.eternal_food_mod_1789477527.EternalFoodMod;
import cn.autoforged.eternal_food_mod_1789477527.item.EternalLighterItem;
import cn.autoforged.eternal_food_mod_1789477527.item.ModItems;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraftforge.event.AnvilUpdateEvent;
import net.minecraftforge.event.GrindstoneEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** 打火机的修复：工作台/铁砧/砂轮中用燧石补满耐久，并保留附魔。 */
@Mod.EventBusSubscriber(modid = EternalFoodMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class LighterRepairHandler {

    private LighterRepairHandler() {
    }

    private static boolean isLighter(ItemStack stack) {
        return stack.is(ModItems.ETERNAL_LIGHTER.get());
    }

    private static ItemStack repaired(ItemStack lighter) {
        ItemStack output = lighter.copy();
        output.setDamageValue(0);
        return output;
    }

    @SubscribeEvent
    public static void onCrafted(PlayerEvent.ItemCraftedEvent event) {
        ItemStack output = event.getCrafting();
        if (!isLighter(output)) {
            return;
        }
        output.setDamageValue(0);
        for (int i = 0; i < event.getInventory().getContainerSize(); i++) {
            ItemStack ingredient = event.getInventory().getItem(i);
            if (isLighter(ingredient) && !EnchantmentHelper.getEnchantments(ingredient).isEmpty()) {
                EnchantmentHelper.setEnchantments(EnchantmentHelper.getEnchantments(ingredient), output);
                break;
            }
        }
    }

    @SubscribeEvent
    public static void onAnvil(AnvilUpdateEvent event) {
        ItemStack left = event.getLeft();
        ItemStack right = event.getRight();
        if (isLighter(left) && right.is(Items.FLINT)) {
            event.setOutput(repaired(left));
            event.setCost(1);
            event.setMaterialCost(1);
        } else if (left.is(Items.FLINT) && isLighter(right)) {
            event.setOutput(repaired(right));
            event.setCost(1);
            event.setMaterialCost(1);
        }
    }

    @SubscribeEvent
    public static void onGrindstone(GrindstoneEvent.OnPlaceItem event) {
        if (isLighter(event.getTopItem()) && event.getBottomItem().is(Items.FLINT)) {
            event.setOutput(repaired(event.getTopItem()));
            event.setXp(0);
        } else if (event.getTopItem().is(Items.FLINT) && isLighter(event.getBottomItem())) {
            event.setOutput(repaired(event.getBottomItem()));
            event.setXp(0);
        }
    }
}
