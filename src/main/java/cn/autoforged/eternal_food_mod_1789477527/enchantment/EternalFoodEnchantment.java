package cn.autoforged.eternal_food_mod_1789477527.enchantment;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.enchantment.Enchantment;
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

    /** 不进入附魔台 / 附魔书 / 战利品附魔池。 */
    @Override
    public boolean isDiscoverable() {
        return false;
    }

    /** 村民不交易。 */
    @Override
    public boolean isTradeable() {
        return false;
    }

    @Override
    public boolean isTreasureOnly() {
        return false;
    }
}
