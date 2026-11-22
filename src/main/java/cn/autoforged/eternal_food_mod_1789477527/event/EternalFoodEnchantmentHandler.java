package cn.autoforged.eternal_food_mod_1789477527.event;

import cn.autoforged.eternal_food_mod_1789477527.EternalFoodMod;
import cn.autoforged.eternal_food_mod_1789477527.enchantment.ModEnchantments;
import java.util.List;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.RegistryObject;

/**
 * 永恒食物附魔：
 * <ul>
 *   <li>合成永恒食物时 25% 概率随机刷出一个附魔（1~最大等级）；</li>
 *   <li>食用时按附魔等级施加对应效果（数值参考 orefood）。</li>
 * </ul>
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

    @SubscribeEvent
    public static void onCrafted(PlayerEvent.ItemCraftedEvent event) {
        ItemStack stack = event.getCrafting();
        if (!isEternalFood(stack) || hasAnyEnchantment(stack)) {
            return;
        }
        if (event.getEntity().level().isClientSide()) {
            return;
        }
        if (event.getEntity().getRandom().nextDouble() >= ROLL_CHANCE) {
            return;
        }
        List<RegistryObject<Enchantment>> pool = ModEnchantments.ALL;
        Enchantment enchantment = pool.get(event.getEntity().getRandom().nextInt(pool.size())).get();
        int level = 1 + event.getEntity().getRandom().nextInt(enchantment.getMaxLevel());
        stack.enchant(enchantment, level);
    }

    private static boolean hasAnyEnchantment(ItemStack stack) {
        for (RegistryObject<Enchantment> entry : ModEnchantments.ALL) {
            if (EnchantmentHelper.getItemEnchantmentLevel(entry.get(), stack) > 0) {
                return true;
            }
        }
        return false;
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
