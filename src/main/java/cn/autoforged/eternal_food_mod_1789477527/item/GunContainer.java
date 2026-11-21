package cn.autoforged.eternal_food_mod_1789477527.item;

import java.util.List;
import net.minecraft.core.NonNullList;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** 发射器枪的 9 格弹仓：读写都直接落到物品 NBT（{@link EternalDispenserGunItem#getAmmo}）。 */
public class GunContainer implements Container {

    private final ItemStack gun;
    private final NonNullList<ItemStack> items = NonNullList.withSize(9, ItemStack.EMPTY);

    public GunContainer(ItemStack gun) {
        this.gun = gun;
        List<ItemStack> ammo = EternalDispenserGunItem.readAllAmmo(gun);
        for (int i = 0; i < 9; i++) {
            this.items.set(i, ammo.get(i));
        }
    }

    public ItemStack getGun() {
        return this.gun;
    }

    @Override
    public int getContainerSize() {
        return 9;
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack stack : this.items) {
            if (!stack.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        return this.items.get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack stack = this.items.get(slot);
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack result = stack.split(amount);
        if (!result.isEmpty()) {
            this.setChanged();
        }
        return result;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        ItemStack stack = this.items.get(slot);
        this.items.set(slot, ItemStack.EMPTY);
        return stack;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        this.items.set(slot, stack);
        this.setChanged();
    }

    @Override
    public void setChanged() {
        if (!this.gun.isEmpty()) {
            EternalDispenserGunItem.writeAllAmmo(this.gun, this.items);
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public void clearContent() {
        for (int i = 0; i < 9; i++) {
            this.items.set(i, ItemStack.EMPTY);
        }
        this.setChanged();
    }
}
