package cn.autoforged.eternal_food_mod_1789477527.enchantment;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentCategory;

/**
 * 永恒食物专用附魔：不出现在附魔台 / 附魔书 / 村民交易里（isDiscoverable=false），
 * 只能通过合成永恒食物时随机刷出。
 */
public class EternalFoodEnchantment extends Enchantment {

    private final int maxLevel;

    public EternalFoodEnchantment(Rarity rarity, EnchantmentCategory category, int maxLevel) {
        super(rarity, category, new EquipmentSlot[]{EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND});
        this.maxLevel = maxLevel;
    }

    @Override
    public int getMaxLevel() {
        return this.maxLevel;
    }

    @Override
    public int getMinCost(int level) {
        return 1 + (level - 1) * 11;
    }

    @Override
    public int getMaxCost(int level) {
        return getMinCost(level) + 20;
    }

    /** 允许出现在附魔台候选池里，但能否应用由 canApplyAtEnchantingTable 决定。 */
    @Override
    public boolean isDiscoverable() {
        return true;
    }

    /** 只有永恒食物能在附魔台里刷出这些附魔。 */
    @Override
    public boolean canApplyAtEnchantingTable(ItemStack stack) {
        return stack.getItem() instanceof cn.autoforged.eternal_food_mod_1789477527.item.EternalFoodItem;
    }

    /** 不进入附魔书，因此不会占附魔书/村民交易的权重。 */
    @Override
    public boolean isAllowedOnBooks() {
        return false;
    }

    @Override
    public boolean isTradeable() {
        return false;
    }

    @Override
    public boolean isTreasureOnly() {
        return false;
    }
}
