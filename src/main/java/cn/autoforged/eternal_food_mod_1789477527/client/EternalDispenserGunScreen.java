package cn.autoforged.eternal_food_mod_1789477527.client;

import cn.autoforged.eternal_food_mod_1789477527.EternalFoodMod;
import cn.autoforged.eternal_food_mod_1789477527.item.EternalDispenserGunItem;
import cn.autoforged.eternal_food_mod_1789477527.menu.EternalDispenserGunMenu;
import cn.autoforged.eternal_food_mod_1789477527.network.GunSetDelayPacket;
import cn.autoforged.eternal_food_mod_1789477527.network.ModNetwork;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * 发射器枪界面：上面 3×3 弹仓，中间发射频率滑条 + 「默认」按钮，下面玩家物品栏。
 */
public class EternalDispenserGunScreen extends AbstractContainerScreen<EternalDispenserGunMenu> {

    private static final ResourceLocation TEXTURE = new ResourceLocation(EternalFoodMod.MOD_ID,
            "textures/gui/container/eternal_dispenser_gun.png");

    private DelaySlider slider;

    public EternalDispenserGunScreen(EternalDispenserGunMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 198;
        this.titleLabelY = 6;
        this.inventoryLabelY = 104;
    }

    @Override
    protected void init() {
        super.init();
        this.slider = new DelaySlider(this.leftPos + 8, this.topPos + 84, 112, 16);
        this.addRenderableWidget(this.slider);
        this.addRenderableWidget(Button.builder(
                        Component.translatable("gui.eternal_food_mod_1789477527.dispenser_gun.default"),
                        button -> this.slider.setTicks(EternalDispenserGunItem.DEFAULT_DELAY_TICKS))
                .bounds(this.leftPos + 126, this.topPos + 84, 42, 16)
                .build());
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShaderTexture(0, TEXTURE);
        graphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        this.renderTooltip(graphics, mouseX, mouseY);
    }

    private class DelaySlider extends AbstractSliderButton {

        private int lastSent;
        private int lastShown;

        DelaySlider(int x, int y, int width, int height) {
            super(x, y, width, height, Component.empty(),
                    (EternalDispenserGunScreen.this.menu.getFireDelay()
                            - EternalDispenserGunItem.MIN_DELAY_TICKS)
                            / (double) (EternalDispenserGunItem.MAX_DELAY_TICKS
                                    - EternalDispenserGunItem.MIN_DELAY_TICKS));
            this.lastSent = this.ticks();
            this.lastShown = this.ticks();
            this.updateMessage();
        }

        private int ticks() {
            return EternalDispenserGunItem.MIN_DELAY_TICKS
                    + (int) Math.round(this.value * (EternalDispenserGunItem.MAX_DELAY_TICKS
                            - EternalDispenserGunItem.MIN_DELAY_TICKS));
        }

        /** 「默认」按钮：立即回到默认间隔并同步到服务端。 */
        void setTicks(int ticks) {
            this.value = (ticks - EternalDispenserGunItem.MIN_DELAY_TICKS)
                    / (double) (EternalDispenserGunItem.MAX_DELAY_TICKS
                            - EternalDispenserGunItem.MIN_DELAY_TICKS);
            this.updateMessage();
            this.push();
        }

        private void push() {
            int ticks = this.ticks();
            if (ticks != this.lastSent) {
                this.lastSent = ticks;
                ModNetwork.CHANNEL.sendToServer(new GunSetDelayPacket(ticks));
            }
        }

        @Override
        protected void updateMessage() {
            int ticks = this.ticks();
            if (ticks == this.lastShown && !this.getMessage().getString().isEmpty()) {
                return;
            }
            this.lastShown = ticks;
            this.setMessage(Component.translatable(
                    "gui.eternal_food_mod_1789477527.dispenser_gun.delay",
                    String.format(java.util.Locale.ROOT, "%.1f", ticks / 20.0F)));
        }

        @Override
        protected void applyValue() {
            // 拖动过程中只更新显示；数值变化才发包（避免每像素一个包导致卡顿感）
            this.updateMessage();
            this.push();
        }

        @Override
        public void onRelease(double mouseX, double mouseY) {
            super.onRelease(mouseX, mouseY);
            this.push();
        }
    }
}
