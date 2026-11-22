package cn.autoforged.eternal_food_mod_1789477527.event;

import cn.autoforged.eternal_food_mod_1789477527.EternalFoodMod;
import cn.autoforged.eternal_food_mod_1789477527.enchantment.ModEnchantments;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 永恒食物附魔：食用时按附魔等级施加对应效果（数值参考 orefood）。
 * 附魔本身通过附魔台刷在永恒食物上，并可在铁砧中用两件食物融合升级。
 */
@Mod.EventBusSubscriber(modid = EternalFoodMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class EternalFoodEnchantmentHandler {

    private static final double ROLL_CHANCE = 0.25D;

    private EternalFoodEnchantmentHandler() {
    }

    public static boolean isEternalFood(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        var key = net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(stack.getItem());
        return key != null && EternalFoodMod.MOD_ID.equals(key.getNamespace())
                && stack.getItem().getFoodProperties() != null;
    }

    /** 附魔食物在物品提示里显示每个附魔的作用。 */
    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        if (!isEternalFood(stack)) {
            return;
        }
        for (var entry : ModEnchantments.ALL) {
            Enchantment enchantment = entry.get();
            int level = EnchantmentHelper.getItemEnchantmentLevel(enchantment, stack);
            if (level <= 0) {
                continue;
            }
            event.getToolTip().add(net.minecraft.network.chat.Component.translatable(
                    enchantment.getDescriptionId() + ".desc")
                    .withStyle(net.minecraft.ChatFormatting.GRAY));
        }
    }

    @SubscribeEvent
    public static void onUseFinished(LivingEntityUseItemEvent.Finish event) {
        ItemStack stack = event.getItem();
        if (!isEternalFood(stack)) {
            return;
        }
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide()) {
            return;
        }
        int recovery = level(stack, ModEnchantments.POWERFUL_RECOVERY);
        int adrenaline = level(stack, ModEnchantments.ADRENALINE);
        int fitness = level(stack, ModEnchantments.FITNESS);
        int satiety = level(stack, ModEnchantments.SATIETY);
        int feast = level(stack, ModEnchantments.FEAST);

        if (satiety > 0 && entity instanceof Player player) {
            // 每级额外 2 点饥饿值 + 4 点饱和度
            player.getFoodData().eat(satiety * 2, 2.0F);
        }
        if (recovery > 0) {
            int amplifier = Math.max(0, recovery - 1);
            add(entity, MobEffects.HEAL, 1, amplifier);
            add(entity, MobEffects.REGENERATION, recovery * 100, amplifier);
        }
        if (adrenaline > 0) {
            int amplifier = Math.max(0, adrenaline - 1);
            add(entity, MobEffects.DAMAGE_BOOST, adrenaline * 100, amplifier);
            add(entity, MobEffects.MOVEMENT_SPEED, adrenaline * 200, amplifier);
        }
        if (fitness > 0) {
            add(entity, MobEffects.ABSORPTION, fitness * 400, 1);
            add(entity, MobEffects.DAMAGE_RESISTANCE, fitness * 400, Math.max(0, fitness - 1));
        }
        if (feast > 0) {
            add(entity, MobEffects.SATURATION, feast * 20 * 60, 0);
        }
    }

    private static int level(ItemStack stack, RegistryObject<Enchantment> enchantment) {
        return EnchantmentHelper.getItemEnchantmentLevel(enchantment.get(), stack);
    }

    private static void add(LivingEntity entity, net.minecraft.world.effect.MobEffect effect,
            int duration, int amplifier) {
        entity.addEffect(new MobEffectInstance(effect, duration, amplifier));
    }
}
