package cn.autoforged.eternal_food_mod_1789477527.client;

import cn.autoforged.eternal_food_mod_1789477527.EternalFoodMod;
import cn.autoforged.eternal_food_mod_1789477527.item.EternalDispenserGunItem;
import cn.autoforged.eternal_food_mod_1789477527.menu.EternalDispenserGunMenu;
import cn.autoforged.eternal_food_mod_1789477527.network.GunSetDelayPacket;
import cn.autoforged.eternal_food_mod_1789477527.network.ModNetwork;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/** 发射器枪界面：原版发射器贴图 + 底部发射频率滑条（2 ~ 30 tick）。 */
public class EternalDispenserGunScreen extends AbstractContainerScreen<EternalDispenserGunMenu> {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation("minecraft", "textures/gui/container/dispenser.png");

    private DelaySlider slider;

    public EternalDispenserGunScreen(EternalDispenserGunMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
        this.titleLabelY = 6;
        this.inventoryLabelY = 72;
    }

    @Override
    protected void init() {
        super.init();
        this.slider = new DelaySlider(this.leftPos + 8, this.topPos + 170, 160, 20);
        this.addRenderableWidget(this.slider);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShaderTexture(0, TEXTURE);
        graphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);
        // 滑条背景（比原版界面多出来的一条）
        graphics.fill(this.leftPos, this.topPos + this.imageHeight,
                this.leftPos + this.imageWidth, this.topPos + this.imageHeight + 26, 0xFF3F3F3F);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        this.renderTooltip(graphics, mouseX, mouseY);
    }

    private class DelaySlider extends AbstractSliderButton {

        DelaySlider(int x, int y, int width, int height) {
            super(x, y, width, height, Component.empty(),
                    (EternalDispenserGunScreen.this.menu.getFireDelay() - EternalDispenserGunItem.MIN_DELAY_TICKS)
                            / (double) (EternalDispenserGunItem.MAX_DELAY_TICKS
                                    - EternalDispenserGunItem.MIN_DELAY_TICKS));
            this.updateMessage();
        }

        private int ticks() {
            return EternalDispenserGunItem.MIN_DELAY_TICKS
                    + (int) Math.round(this.value * (EternalDispenserGunItem.MAX_DELAY_TICKS
                            - EternalDispenserGunItem.MIN_DELAY_TICKS));
        }

        @Override
        protected void updateMessage() {
            this.setMessage(Component.translatable(
                    "gui.eternal_food_mod_1789477527.dispenser_gun.delay",
                    String.format(java.util.Locale.ROOT, "%.1f", this.ticks() / 20.0F)));
        }

        @Override
        protected void applyValue() {
            ModNetwork.CHANNEL.sendToServer(new GunSetDelayPacket(this.ticks()));
        }
    }
}
