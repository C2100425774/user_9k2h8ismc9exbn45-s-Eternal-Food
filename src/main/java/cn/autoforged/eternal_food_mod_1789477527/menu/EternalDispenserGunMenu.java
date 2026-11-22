package cn.autoforged.eternal_food_mod_1789477527.menu;

import cn.autoforged.eternal_food_mod_1789477527.item.GunContainer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** 发射器枪界面：上面 3×3 弹仓（与发射器一致的位置），下面发射频率滑条。 */
public class EternalDispenserGunMenu extends AbstractContainerMenu {

    private final GunContainer container;
    private final ItemStack gun;

    public EternalDispenserGunMenu(int id, Inventory inventory, InteractionHand hand) {
        super(ModMenus.GUN.get(), id);
        this.gun = inventory.player.getItemInHand(hand);
        this.container = new GunContainer(this.gun);

        // 3×3 弹仓（与原版发射器同坐标）
        for (int i = 0; i < 9; i++) {
            this.addSlot(new Slot(this.container, i, 62 + (i % 3) * 18, 17 + (i / 3) * 18));
        }
        // 玩家背包 3×9（与原版发射器一致）
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
        // 快捷栏
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(inventory, col, 8 + col * 18, 142));
        }
    }

    /** 界面里显示用的枪物品栈（客户端用它画「无限附魔 → 第 1 格金色」提示）。 */
    public ItemStack getGun() {
        return this.gun;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot == null || !slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        result = stack.copy();
        if (index < 9) {
            if (!this.moveItemStackTo(stack, 9, 45, true)) {
                return ItemStack.EMPTY;
            }
        } else if (!this.moveItemStackTo(stack, 0, 9, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return result;
    }
}
