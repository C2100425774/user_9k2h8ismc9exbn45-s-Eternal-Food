package cn.autoforged.eternal_food_mod_1789477527.client;

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
 * Shift+右键打开的发射器枪配置界面（紧凑面板，不占满整个屏幕）：
 * 开关按钮绿色=开启、红色=关闭；力量 0~5、穿透 0~10 各一条一格的滑条。
 */
public class GunConfigScreen extends Screen {

    private static final int PANEL_W = 320;
    private static final int PANEL_H = 214;
    private static final int ROW = 24;

    private final ItemStack gun;
    private final List<ToggleButton> toggles = new ArrayList<>();
    private StepSlider powerSlider;
    private StepSlider piercingSlider;
    private DelaySlider delaySlider;
    private int panelX;
    private int panelY;

    public GunConfigScreen(ItemStack gun) {
        super(Component.translatable("gui.eternal_food_mod_1789477527.dispenser_gun.config"));
        this.gun = gun;
    }

    public static void open(ItemStack gun) {
        Minecraft.getInstance().setScreen(new GunConfigScreen(gun));
    }

    @Override
    protected void init() {
        this.panelX = (this.width - PANEL_W) / 2;
        this.panelY = (this.height - PANEL_H) / 2;
        int left = this.panelX + 10;
        int top = this.panelY + 40;
        this.toggles.clear();
        for (int i = 0; i < EternalDispenserGunItem.TOGGLE_KEYS.size(); i++) {
            String key = EternalDispenserGunItem.TOGGLE_KEYS.get(i);
            int col = i % 2;
            int row = i / 2;
            ToggleButton button = new ToggleButton(left + col * 150, top + row * ROW, 145, 20, key,
                    EternalDispenserGunItem.getToggle(this.gun, key));
            this.toggles.add(button);
            this.addRenderableWidget(button);
        }
        int sliderY = top + 3 * ROW + 4;
        this.powerSlider = new StepSlider(left, sliderY, PANEL_W - 20, 20,
                EternalDispenserGunItem.CFG_POWER, 0, 5, EternalDispenserGunItem.getPower(this.gun),
                "gui.eternal_food_mod_1789477527.dispenser_gun.power");
        this.addRenderableWidget(this.powerSlider);
        this.piercingSlider = new StepSlider(left, sliderY + 26, PANEL_W - 20, 20,
                EternalDispenserGunItem.CFG_PIERCING, 0, 10, EternalDispenserGunItem.getPiercing(this.gun),
                "gui.eternal_food_mod_1789477527.dispenser_gun.piercing");
        this.addRenderableWidget(this.piercingSlider);
        this.delaySlider = new DelaySlider(left, sliderY + 52, PANEL_W - 20 - 46, 20);
        this.addRenderableWidget(this.delaySlider);
        this.addRenderableWidget(Button.builder(
                        Component.translatable("gui.eternal_food_mod_1789477527.dispenser_gun.default"),
                        button -> this.delaySlider.setTicks(EternalDispenserGunItem.DEFAULT_DELAY_TICKS))
                .bounds(left + PANEL_W - 20 - 44, sliderY + 52, 44, 20)
                .build());
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // 像原版容器一样：E 或 Esc 直接关闭
        if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_E
                || keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE) {
            this.onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        // 和打开原版发射器一样，不暂停游戏
        return false;
    }

    @Override
    public void renderBackground(GuiGraphics graphics) {
        // 不要整屏变暗，只画中间的小面板
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        int x = this.panelX;
        int y = this.panelY;
        int w = PANEL_W;
        int h = PANEL_H;
        // 原版物品栏那种灰色面板 + 立体边框
        graphics.fill(x, y, x + w, y + h, 0xFFC6C6C6);
        graphics.fill(x, y, x + w, y + 1, 0xFFFFFFFF);
        graphics.fill(x, y, x + 1, y + h, 0xFFFFFFFF);
        graphics.fill(x, y + h - 1, x + w, y + h, 0xFF555555);
        graphics.fill(x + w - 1, y, x + w, y + h, 0xFF555555);
        graphics.fill(x + 1, y + 1, x + w - 1, y + 2, 0xFFB0B0B0);
        graphics.fill(x + 1, y + h - 2, x + w - 1, y + h - 1, 0xFFFFFFFF);
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, this.panelY + 8, 0x404040);
        graphics.drawCenteredString(this.font,
                Component.translatable("gui.eternal_food_mod_1789477527.dispenser_gun.config_hint"),
                this.width / 2, this.panelY + 24, 0x707070);
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
        if (this.piercingSlider != null && this.piercingSlider.isMouseOver(mouseX, mouseY)) {
            graphics.renderTooltip(this.font,
                    Component.translatable("tooltip.eternal_food_mod_1789477527.dispenser_gun.option.piercing"),
                    mouseX, mouseY);
        }
        if (this.delaySlider != null && this.delaySlider.isMouseOver(mouseX, mouseY)) {
            graphics.renderTooltip(this.font,
                    Component.translatable("tooltip.eternal_food_mod_1789477527.dispenser_gun.option.delay"),
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

    /** 一格一格的整数滑条（力量 0~5 / 穿透 0~10）。 */
    private class StepSlider extends AbstractSliderButton {

        private final String key;
        private final int min;
        private final int max;
        private final String labelKey;

        StepSlider(int x, int y, int width, int height, String key, int min, int max, int value,
                String labelKey) {
            super(x, y, width, height, Component.empty(), (value - min) / (double) (max - min));
            this.key = key;
            this.min = min;
            this.max = max;
            this.labelKey = labelKey;
            this.updateMessage();
        }

        int level() {
            return this.min + (int) Math.round(this.value * (this.max - this.min));
        }

        @Override
        protected void updateMessage() {
            this.setMessage(Component.translatable(this.labelKey, this.level()));
        }

        @Override
        protected void applyValue() {
            // 吸附到整数格，拖到哪一格就显示哪一格，不会出现半格
            int level = this.level();
            this.value = (level - this.min) / (double) (this.max - this.min);
            this.updateMessage();
        }

        @Override
        public void onRelease(double mouseX, double mouseY) {
            super.onRelease(mouseX, mouseY);
            ModNetwork.CHANNEL.sendToServer(new GunConfigPacket(this.key, this.level()));
        }
    }

    /** 发射间隔滑条：1~30 tick，显示秒（0.05 ~ 1.50）。 */
    private class DelaySlider extends AbstractSliderButton {

        DelaySlider(int x, int y, int width, int height) {
            super(x, y, width, height, Component.empty(),
                    (EternalDispenserGunItem.getFireDelay(GunConfigScreen.this.gun)
                            - EternalDispenserGunItem.MIN_DELAY_TICKS)
                            / (double) (EternalDispenserGunItem.MAX_DELAY_TICKS
                                    - EternalDispenserGunItem.MIN_DELAY_TICKS));
            this.updateMessage();
        }

        int ticks() {
            return EternalDispenserGunItem.MIN_DELAY_TICKS
                    + (int) Math.round(this.value * (EternalDispenserGunItem.MAX_DELAY_TICKS
                            - EternalDispenserGunItem.MIN_DELAY_TICKS));
        }

        /** 「默认」按钮：立即回到默认间隔并同步。 */
        void setTicks(int ticks) {
            this.value = (ticks - EternalDispenserGunItem.MIN_DELAY_TICKS)
                    / (double) (EternalDispenserGunItem.MAX_DELAY_TICKS
                            - EternalDispenserGunItem.MIN_DELAY_TICKS);
            this.updateMessage();
            ModNetwork.CHANNEL.sendToServer(new GunConfigPacket(
                    EternalDispenserGunItem.CFG_DELAY, this.ticks()));
        }

        @Override
        protected void updateMessage() {
            this.setMessage(Component.translatable(
                    "gui.eternal_food_mod_1789477527.dispenser_gun.delay",
                    String.format(java.util.Locale.ROOT, "%.2f", this.ticks() / 20.0F)));
        }

        @Override
        protected void applyValue() {
            // 吸附到整数 tick，一格一格走
            int ticks = this.ticks();
            this.value = (ticks - EternalDispenserGunItem.MIN_DELAY_TICKS)
                    / (double) (EternalDispenserGunItem.MAX_DELAY_TICKS
                            - EternalDispenserGunItem.MIN_DELAY_TICKS);
            this.updateMessage();
        }

        @Override
        public void onRelease(double mouseX, double mouseY) {
            super.onRelease(mouseX, mouseY);
            ModNetwork.CHANNEL.sendToServer(new GunConfigPacket(
                    EternalDispenserGunItem.CFG_DELAY, this.ticks()));
        }
    }
}
