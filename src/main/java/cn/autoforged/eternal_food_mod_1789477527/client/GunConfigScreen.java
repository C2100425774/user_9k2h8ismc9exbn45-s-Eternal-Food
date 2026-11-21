package cn.autoforged.eternal_food_mod_1789477527.client;

import cn.autoforged.eternal_food_mod_1789477527.EternalFoodMod;
import cn.autoforged.eternal_food_mod_1789477527.item.EternalDispenserGunItem;
import cn.autoforged.eternal_food_mod_1789477527.network.GunConfigPacket;
import cn.autoforged.eternal_food_mod_1789477527.network.ModNetwork;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * Shift+鍙抽敭鎵撳紑鐨勫彂灏勫櫒鏋厤缃晫闈細
 * 寮€鍏虫寜閽豢鑹?寮€鍚€佺孩鑹?鍏抽棴锛涘姏閲忔槸 0~5 鐨勬粦鏉★紱鎸夐挳鎮诞鏃朵細鏄剧ず璇存槑銆? */
public class GunConfigScreen extends Screen {

    private static final int ROW = 28;

    private final ItemStack gun;
    private final List<ToggleButton> toggles = new ArrayList<>();
    private PowerSlider powerSlider;

    public GunConfigScreen(ItemStack gun) {
        super(Component.translatable("gui.eternal_food_mod_1789477527.dispenser_gun.config"));
        this.gun = gun;
    }

    public static void open(ItemStack gun) {
        Minecraft.getInstance().setScreen(new GunConfigScreen(gun));
    }

    @Override
    protected void init() {
        int center = this.width / 2;
        int top = 56;
        this.toggles.clear();
        for (int i = 0; i < EternalDispenserGunItem.TOGGLE_KEYS.size(); i++) {
            String key = EternalDispenserGunItem.TOGGLE_KEYS.get(i);
            int col = i % 2;
            int row = i / 2;
            ToggleButton button = new ToggleButton(center - 140 + col * 145, top + row * ROW, 135, 20, key,
                    EternalDispenserGunItem.getToggle(this.gun, key));
            this.toggles.add(button);
            this.addRenderableWidget(button);
        }
        int sliderY = top + 3 * ROW + 12;
        this.powerSlider = new PowerSlider(center - 140, sliderY, 280, 20,
                EternalDispenserGunItem.getPower(this.gun));
        this.addRenderableWidget(this.powerSlider);
        this.addRenderableWidget(Button.builder(
                        Component.translatable("gui.eternal_food_mod_1789477527.dispenser_gun.done"),
                        button -> this.onClose())
                .bounds(center - 50, sliderY + 34, 100, 20)
                .build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, 20, 0xFFFFFF);
        graphics.drawCenteredString(this.font,
                Component.translatable("gui.eternal_food_mod_1789477527.dispenser_gun.config_hint"),
                this.width / 2, 34, 0xA0A0A0);
        for (ToggleButton button : this.toggles) {
            if (button.isMouseOver(mouseX, mouseY)) {
                graphics.renderTooltip(this.font,
                        Component.translatable(EternalDispenserGunItem.optionDescKey(button.key)),
                        mouseX, mouseY);
                break;
            }
        }
        if (this.powerSlider != null && this.powerSlider.isMouseOver(mouseX, mouseY)) {
            graphics.renderTooltip(this.font,
                    Component.translatable("tooltip.eternal_food_mod_1789477527.dispenser_gun.option.power"),
                    mouseX, mouseY);
        }
    }

    private class ToggleButton extends AbstractWidget {

        private final String key;
        private boolean state;

        ToggleButton(int x, int y, int width, int height, String key, boolean state) {
            super(x, y, width, height, Component.translatable(EternalDispenserGunItem.optionNameKey(key)));
            this.key = key;
            this.state = state;
        }

        @Override
        public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            int background = this.state ? 0xFF2E7D32 : 0xFF8E2A2A;
            graphics.fill(this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height,
                    background);
            int border = this.isHoveredOrFocused() ? 0xFFFFFFFF : 0xFF202020;
            graphics.fill(this.getX(), this.getY(), this.getX() + this.width, this.getY() + 1, border);
            graphics.fill(this.getX(), this.getY() + this.height - 1,
                    this.getX() + this.width, this.getY() + this.height, border);
            graphics.fill(this.getX(), this.getY(), this.getX() + 1, this.getY() + this.height, border);
            graphics.fill(this.getX() + this.width - 1, this.getY(),
                    this.getX() + this.width, this.getY() + this.height, border);
            graphics.drawCenteredString(GunConfigScreen.this.font, this.getMessage(),
                    this.getX() + this.width / 2, this.getY() + (this.height - 8) / 2, 0xFFFFFF);
        }

        @Override
        public void onClick(double mouseX, double mouseY) {
            this.state = !this.state;
            ModNetwork.CHANNEL.sendToServer(new GunConfigPacket(this.key, this.state ? 1 : 0));
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
            this.defaultButtonNarrationText(output);
        }
    }

    private class PowerSlider extends AbstractSliderButton {

        PowerSlider(int x, int y, int width, int height, int power) {
            super(x, y, width, height, Component.empty(), power / 5.0);
            this.updateMessage();
        }

        private void applyToGun() {
            int level = (int) Math.round(this.value * 5.0);
            ModNetwork.CHANNEL.sendToServer(new GunConfigPacket(EternalDispenserGunItem.CFG_POWER, level));
        }

        @Override
        protected void updateMessage() {
            int level = (int) Math.round(this.value * 5.0);
            this.setMessage(Component.translatable(
                    "gui.eternal_food_mod_1789477527.dispenser_gun.power", level));
        }

        @Override
        protected void applyValue() {
            this.updateMessage();
        }

        @Override
        public void onRelease(double mouseX, double mouseY) {
            super.onRelease(mouseX, mouseY);
            this.applyToGun();
        }
    }
}

