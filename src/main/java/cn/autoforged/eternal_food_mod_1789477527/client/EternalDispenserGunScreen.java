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
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;

/**
 * 发射器枪界面：上面 3×3 弹仓，中间发射频率滑条 + 「默认」按钮，下面玩家物品栏。
 *
 * <p>滑条的拖动由本 Screen 自己转发（容器的 {@code mouseDragged} 不会把事件交给组件），
 * 拖动时完全不发网络包，松手同步一次，保证跟手。
 */
public class EternalDispenserGunScreen extends AbstractContainerScreen<EternalDispenserGunMenu> {

    private static final ResourceLocation TEXTURE = new ResourceLocation(EternalFoodMod.MOD_ID,
            "textures/gui/container/eternal_dispenser_gun.png");
    private static final int SLOT0_X = 62;
    private static final int SLOT0_Y = 17;

    private DelaySlider slider;
    private boolean draggingSlider;

    public EternalDispenserGunScreen(EternalDispenserGunMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 194;
        this.titleLabelY = 6;
        this.inventoryLabelY = 100;
    }

    @Override
    protected void init() {
        super.init();
        this.slider = new DelaySlider(this.leftPos + 8, this.topPos + 74, 112, 20);
        this.addRenderableWidget(this.slider);
        this.addRenderableWidget(Button.builder(
                        Component.translatable("gui.eternal_food_mod_1789477527.dispenser_gun.default"),
                        button -> this.slider.setTicks(EternalDispenserGunItem.DEFAULT_DELAY_TICKS))
                .bounds(this.leftPos + 126, this.topPos + 74, 42, 20)
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
        renderInfiniteSlotHint(graphics);
        this.renderTooltip(graphics, mouseX, mouseY);
    }

    /** 附魔无限时，把第 1 格标成金色，提示这一格会无限发射。 */
    private void renderInfiniteSlotHint(GuiGraphics graphics) {
        ItemStack gun = this.menu.getGun();
        if (gun.isEmpty()
                || EnchantmentHelper.getItemEnchantmentLevel(Enchantments.INFINITY_ARROWS, gun) <= 0) {
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

    // ---------------------------------------------------------------
    // 滑条拖动（容器界面不会自动把拖动事件发给子组件，这里手动转发）
    // ---------------------------------------------------------------

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && this.slider != null && this.slider.isOver(mouseX, mouseY)) {
            this.draggingSlider = true;
            this.slider.setFocused(true);
            this.slider.dragTo(mouseX);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (this.draggingSlider) {
            this.slider.dragTo(mouseX);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (this.draggingSlider) {
            this.draggingSlider = false;
            this.slider.endDrag();
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    private class DelaySlider extends AbstractSliderButton {

        private int lastShown;

        DelaySlider(int x, int y, int width, int height) {
            super(x, y, width, height, Component.empty(),
                    (EternalDispenserGunScreen.this.menu.getFireDelay()
                            - EternalDispenserGunItem.MIN_DELAY_TICKS)
                            / (double) (EternalDispenserGunItem.MAX_DELAY_TICKS
                                    - EternalDispenserGunItem.MIN_DELAY_TICKS));
            this.lastShown = this.ticks();
            this.updateMessage();
        }

        int ticks() {
            return EternalDispenserGunItem.MIN_DELAY_TICKS
                    + (int) Math.round(this.value * (EternalDispenserGunItem.MAX_DELAY_TICKS
                            - EternalDispenserGunItem.MIN_DELAY_TICKS));
        }

        boolean isOver(double mouseX, double mouseY) {
            return mouseX >= this.getX() && mouseX <= this.getX() + this.width
                    && mouseY >= this.getY() && mouseY <= this.getY() + this.height;
        }

        void dragTo(double mouseX) {
            double raw = (mouseX - (this.getX() + 4)) / (double) (this.width - 8);
            int ticks = EternalDispenserGunItem.MIN_DELAY_TICKS
                    + (int) Math.round(Mth.clamp(raw, 0.0, 1.0)
                            * (EternalDispenserGunItem.MAX_DELAY_TICKS
                                    - EternalDispenserGunItem.MIN_DELAY_TICKS));
            this.value = (ticks - EternalDispenserGunItem.MIN_DELAY_TICKS)
                    / (double) (EternalDispenserGunItem.MAX_DELAY_TICKS
                            - EternalDispenserGunItem.MIN_DELAY_TICKS);
            this.updateMessage();
        }

        void endDrag() {
            this.updateMessage();
            this.push();
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
            ModNetwork.CHANNEL.sendToServer(new GunSetDelayPacket(this.ticks()));
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
                    String.format(java.util.Locale.ROOT, "%.2f", ticks / 20.0F)));
        }

        @Override
        protected void applyValue() {
            // 拖动由 Screen 转发，这里不需要额外处理
        }

        @Override
        public void onRelease(double mouseX, double mouseY) {
            super.onRelease(mouseX, mouseY);
            this.endDrag();
        }
    }
}
