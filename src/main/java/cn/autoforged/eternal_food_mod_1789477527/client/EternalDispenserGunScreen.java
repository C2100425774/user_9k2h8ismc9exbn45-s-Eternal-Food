package cn.autoforged.eternal_food_mod_1789477527.client;

import cn.autoforged.eternal_food_mod_1789477527.item.EternalDispenserGunItem;
import cn.autoforged.eternal_food_mod_1789477527.menu.EternalDispenserGunMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/**
 * 发射器枪的弹药界面：完全照原版发射器（3×3 弹仓 + 玩家物品栏），
 * 发射频率等设置已经移到 Shift+右键的配置界面。
 */
public class EternalDispenserGunScreen extends AbstractContainerScreen<EternalDispenserGunMenu> {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation("minecraft:textures/gui/container/dispenser.png");
    private static final int SLOT0_X = 62;
    private static final int SLOT0_Y = 17;

    public EternalDispenserGunScreen(EternalDispenserGunMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
        this.inventoryLabelY = 72;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderInfiniteSlotHint(graphics);
        this.renderTooltip(graphics, mouseX, mouseY);
    }

    /** 开了「无限」时，把第 1 格标成金色，提示这一格会无限发射。 */
    private void renderInfiniteSlotHint(GuiGraphics graphics) {
        ItemStack gun = this.menu.getGun();
        if (gun.isEmpty()
                || !EternalDispenserGunItem.getToggle(gun, EternalDispenserGunItem.CFG_INFINITY)) {
            return;
        }
        int x = this.leftPos + SLOT0_X - 2;
        int y = this.topPos + SLOT0_Y - 2;
        int color = 0xFFFFD700;
        graphics.fill(x, y, x + 20, y + 1, color);
        graphics.fill(x, y + 19, x + 20, y + 20, color);
        graphics.fill(x, y + 1, x + 1, y + 19, color);
        graphics.fill(x + 19, y + 1, x + 20, y + 19, color);
        graphics.fill(x + 1, y + 1, x + 19, y + 19, 0x30FFD700);
    }
}
