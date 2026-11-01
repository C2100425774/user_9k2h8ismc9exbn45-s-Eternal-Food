package cn.autoforged.eternal_food_mod_1789477527.client;

import cn.autoforged.eternal_food_mod_1789477527.network.ClockActionPacket;
import cn.autoforged.eternal_food_mod_1789477527.network.ModNetwork;
import cn.autoforged.eternal_food_mod_1789477527.util.ClockClientState;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;

/**
 * 永恒时钟的操作面板。
 *
 * <p>视觉照物品栏：整块底色 {@code #C6C6C6}，标题红色，文字深灰；不用贴图，全部用色块 + 原版按钮绘制。
 *
 * <p>布局（自上而下）：
 * <ol>
 *   <li>标题「永恒时钟」（红）</li>
 *   <li>信息三行：当前时间 / 当前刻 / 当前天气</li>
 *   <li>六个时间刻预设按钮：上排是名字（日出 / 正午 / 日落 / 入夜 / 午夜 / 凌晨），下排是具体钟点</li>
 *   <li>六个时间微调按钮：-10000 / -5000 / -1000 / +1000 / +5000 / +10000 刻</li>
 *   <li>锁定时间按钮 + 数字输入框（0~24000 刻）+ 执行</li>
 *   <li>锁定天气 / 晴天 / 雨天 / 雷暴</li>
 * </ol>
 *
 * <p>时间运算全部走 {@code Math.floorMod(x, 24000)}，所以减到负数或超过 24000 都会自动绕回，
 * 不会出现"不存在的时间刻"。
 */
public class EternalClockScreen extends Screen {

    private static final int WIDTH = 292;
    private static final int HEIGHT = 176;

    private static final int COLOR_PANEL = 0xFFC6C6C6;
    private static final int COLOR_BORDER = 0xFF555555;
    private static final int COLOR_TEXT = 0xFF3F3F3F;
    private static final int COLOR_TITLE = 0xFFFF5555;
    private static final int COLOR_VALUE = 0xFF202020;

    /** 六个预设时间刻（游戏刻）与对应的名字 key。0 tick = 06:00。 */
    private static final int[] PRESET_TICKS = {0, 6000, 12000, 14000, 18000, 22000};
    private static final String[] PRESET_KEYS = {
            "dawn", "noon", "dusk", "nightfall", "midnight", "predawn"};

    /** 时间微调步长。 */
    private static final int[] SHIFT_STEPS = {-10000, -5000, -1000, 1000, 5000, 10000};

    private static final long WEATHER_PENDING_MS = 2000L;
    /** 改时间后乐观显示多久（毫秒）；服务端回包会在这之前到达。 */
    private static final long TIME_PENDING_MS = 1500L;

    private final Map<AbstractWidget, Component> tooltips = new LinkedHashMap<>();
    private final List<PresetButton> presetButtons = new ArrayList<>();
    private final List<Button> weatherButtons = new ArrayList<>();
    private int left;
    private int top;
    private EditBox input;
    private IceToggleButton lockTimeButton;
    private IceToggleButton lockWeatherButton;
    private boolean timeLocked;
    private boolean weatherLocked;
    private int pendingWeather = -1;
    private long pendingWeatherUntil;
    /** 刚点过改时间的乐观显示目标值；-1 表示没有待确认的值。 */
    private long pendingDayTime = -1L;
    private long pendingDayTimeUntil;

    public EternalClockScreen() {
        super(Component.translatable("gui.eternal_food_mod_1789477527.clock.title"));
    }

    @Override
    protected void init() {
        this.presetButtons.clear();
        this.weatherButtons.clear();
        this.left = (this.width - WIDTH) / 2;
        this.top = (this.height - HEIGHT) / 2;

        // ---- 第一排：六个时间刻预设（名字在上、钟点在下）----
        int presetW = 40;
        int presetH = 24;
        int presetGap = 4;
        int x = this.left + (WIDTH - (presetW * 6 + presetGap * 5)) / 2;
        int y = this.top + 60;
        for (int i = 0; i < PRESET_TICKS.length; i++) {
            final int ticks = PRESET_TICKS[i];
            PresetButton button = new PresetButton(x, y, presetW, presetH,
                    Component.translatable("gui.eternal_food_mod_1789477527.clock.preset." + PRESET_KEYS[i]),
                    Component.literal(ticksToClock(ticks)),
                    b -> setTime(ticks));
            this.addRenderableWidget(button);
            this.presetButtons.add(button);
            this.tooltips.put(button, Component.translatable(
                    "gui.eternal_food_mod_1789477527.clock.preset.tip", ticks));
            x += presetW + presetGap;
        }

        // ---- 第二排：时间微调 ----
        int shiftW = 45;
        int shiftH = 18;
        int shiftGap = 2;
        x = this.left + (WIDTH - (shiftW * 6 + shiftGap * 5)) / 2;
        y = this.top + 90;
        for (int step : SHIFT_STEPS) {
            String label = (step > 0 ? "+" : "") + step + "刻";
            Button button = Button.builder(Component.literal(label), b -> shiftTime(step))
                    .bounds(x, y, shiftW, shiftH)
                    .build();
            this.addRenderableWidget(button);
            this.tooltips.put(button, Component.translatable(
                    "gui.eternal_food_mod_1789477527.clock.shift.tip", Math.abs(step)));
            x += shiftW + shiftGap;
        }

        // ---- 第三排：锁定时间 / 输入框 / 执行 ----
        int rowY = this.top + 116;
        this.lockTimeButton = new IceToggleButton(this.left + 16, rowY, 20, 18,
                Component.translatable("gui.eternal_food_mod_1789477527.clock.lock_short"),
                b -> ModNetwork.CHANNEL.sendToServer(
                        new ClockActionPacket(ClockActionPacket.ACTION_TOGGLE_TIME_LOCK, 0L)));
        this.addRenderableWidget(this.lockTimeButton);
        this.tooltips.put(this.lockTimeButton,
                Component.translatable("gui.eternal_food_mod_1789477527.clock.lock_time.tip"));

        this.input = new EditBox(this.font, this.left + 40, rowY, 160, 18,
                Component.translatable("gui.eternal_food_mod_1789477527.clock.input"));
        this.input.setMaxLength(6);
        this.input.setHint(Component.literal("0 ~ 24000"));
        this.input.setFilter(s -> s.matches("[0-9]*"));
        this.input.setResponder(s -> {
        });
        this.addRenderableWidget(this.input);

        Button execute = Button.builder(
                        Component.translatable("gui.eternal_food_mod_1789477527.clock.execute"),
                        b -> applyInput())
                .bounds(this.left + 204, rowY, 64, 18)
                .build();
        this.addRenderableWidget(execute);
        this.tooltips.put(execute,
                Component.translatable("gui.eternal_food_mod_1789477527.clock.execute.tip"));

        // ---- 第四排：天气 ----
        int weatherY = this.top + 144;
        int weatherW = 60;
        int weatherGap = 4;
        int wx = this.left + (WIDTH - (weatherW * 4 + weatherGap * 3)) / 2;
        this.lockWeatherButton = new IceToggleButton(wx, weatherY, weatherW, 18,
                Component.translatable("gui.eternal_food_mod_1789477527.clock.lock_weather"),
                b -> ModNetwork.CHANNEL.sendToServer(
                        new ClockActionPacket(ClockActionPacket.ACTION_TOGGLE_WEATHER_LOCK, 0L)));
        this.addRenderableWidget(this.lockWeatherButton);
        this.weatherButtons.add(this.lockWeatherButton);
        this.tooltips.put(this.lockWeatherButton,
                Component.translatable("gui.eternal_food_mod_1789477527.clock.lock_weather.tip"));
        wx += weatherW + weatherGap;
        for (int i = 0; i < 3; i++) {
            final int weather = i;
            Button button = Button.builder(
                            Component.translatable("gui.eternal_food_mod_1789477527.clock.weather." + weather),
                            b -> setWeather(weather))
                    .bounds(wx, weatherY, weatherW, 18)
                    .build();
            this.addRenderableWidget(button);
            this.weatherButtons.add(button);
            wx += weatherW + weatherGap;
        }

        // 没有原版天气的维度（下界 / 末地 / 关掉天空光的模组维度）：
        // 原版天气不会生效，点了也看不到变化，所以整排天气按钮直接禁用，信息行显示「无天气」。
        if (!weatherSupported(Minecraft.getInstance().level)) {
            for (Button button : this.weatherButtons) {
                button.active = false;
            }
        }

        // 打开面板 → 向服务端要一次锁定状态
        ModNetwork.CHANNEL.sendToServer(new ClockActionPacket(ClockActionPacket.ACTION_REQUEST_STATE, 0L));
    }

    // ------------------------------------------------------------------
    // 操作
    // ------------------------------------------------------------------

    /** 由 ClockStatePacket 回调（服务端是唯一权威）。 */
    public void applyState(boolean timeLocked, boolean weatherLocked) {
        this.timeLocked = timeLocked;
        this.weatherLocked = weatherLocked;
        if (this.lockTimeButton != null) {
            this.lockTimeButton.setActiveState(timeLocked);
        }
        if (this.lockWeatherButton != null) {
            this.lockWeatherButton.setActiveState(weatherLocked);
        }
    }

    private void setTime(long ticks) {
        // 乐观显示：点完立刻把「当前时间 / 当前刻」换成目标值，等服务端回包（<0.2s）后再以真实值为准，
        // 否则会先卡在旧值上等一个来回，看起来像"卡一下"。
        this.pendingDayTime = Math.floorMod(ticks, 24000L);
        this.pendingDayTimeUntil = Util.getMillis() + TIME_PENDING_MS;
        ModNetwork.CHANNEL.sendToServer(new ClockActionPacket(ClockActionPacket.ACTION_SET_TIME, ticks));
    }

    /** 当前应显示的时间：刚点过改时间就用目标值，否则用实际值。 */
    private long displayedDayTime(Level level) {
        if (this.pendingDayTime >= 0L && Util.getMillis() < this.pendingDayTimeUntil) {
            return this.pendingDayTime;
        }
        this.pendingDayTime = -1L;
        return ClockClientState.effectiveDayTime(level);
    }

    /** 在当前显示的时间上偏移；绕回 0~24000 由服务端的 floorMod 处理。 */
    private void shiftTime(int delta) {
        long now = ClockClientState.effectiveDayTime(Minecraft.getInstance().level);
        setTime(Math.floorMod(now, 24000L) + delta);
    }

    private void setWeather(int weather) {
        this.pendingWeather = weather;
        this.pendingWeatherUntil = Util.getMillis() + WEATHER_PENDING_MS;
        ModNetwork.CHANNEL.sendToServer(new ClockActionPacket(ClockActionPacket.ACTION_SET_WEATHER, weather));
    }

    private void applyInput() {
        String text = this.input.getValue().trim();
        int ticks;
        try {
            ticks = Integer.parseInt(text);
        } catch (NumberFormatException e) {
            reportInvalid();
            return;
        }
        if (ticks < 0 || ticks > 24000) {
            reportInvalid();
            return;
        }
        setTime(ticks);
    }

    private void reportInvalid() {
        if (this.minecraft != null && this.minecraft.player != null) {
            this.minecraft.player.displayClientMessage(
                    Component.translatable("message.eternal_food_mod_1789477527.clock_invalid")
                            .withStyle(ChatFormatting.RED), false);
        }
    }

    /** 游戏刻 → HH:MM（0 tick = 06:00）。 */
    static String ticksToClock(long ticks) {
        long wrapped = Math.floorMod(ticks, 24000L);
        int hour = (int) (((wrapped / 1000L) + 6L) % 24L);
        int minute = (int) ((wrapped % 1000L) * 60L / 1000L);
        return String.format("%02d:%02d", hour, minute);
    }

    /** 实际天气 → 0 晴 / 1 雨 / 2 雷暴。 */
    static int weatherId(Level level) {
        if (level.isThundering()) {
            return 2;
        }
        return level.isRaining() ? 1 : 0;
    }

    /**
     * 该维度是否支持原版天气。
     *
     * <p>下界、末地这类 {@code has_skylight=false} 的维度里原版天气不生效：
     * {@code isRaining()} 恒为 false，点"雨天"也不会有任何变化。
     * 关掉天空光的模组维度同理，所以按这个标志统一处理。
     *
     * <p>注意：这里只判断「原版天气是否可能生效」。模组自己的天气系统（暴风雪、龙卷风等）
     * 不改变原版雨量，本面板既读不到也控制不了，但也不会因此出错——最多显示"晴天"。
     */
    static boolean weatherSupported(Level level) {
        return level == null || level.dimensionType().hasSkyLight();
    }

    private Component displayedWeather(Level level) {
        if (!weatherSupported(level)) {
            return Component.translatable("gui.eternal_food_mod_1789477527.clock.weather.none");
        }
        if (this.pendingWeather >= 0 && Util.getMillis() < this.pendingWeatherUntil) {
            return Component.translatable(
                    "gui.eternal_food_mod_1789477527.clock.weather." + this.pendingWeather);
        }
        this.pendingWeather = -1;
        return Component.translatable(
                "gui.eternal_food_mod_1789477527.clock.weather." + (level == null ? 0 : weatherId(level)));
    }

    // ------------------------------------------------------------------
    // 渲染
    // ------------------------------------------------------------------

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);

        graphics.fill(this.left, this.top, this.left + WIDTH, this.top + HEIGHT, COLOR_PANEL);
        graphics.fill(this.left, this.top, this.left + WIDTH, this.top + 1, COLOR_BORDER);
        graphics.fill(this.left, this.top + HEIGHT - 1, this.left + WIDTH, this.top + HEIGHT, COLOR_BORDER);
        graphics.fill(this.left, this.top, this.left + 1, this.top + HEIGHT, COLOR_BORDER);
        graphics.fill(this.left + WIDTH - 1, this.top, this.left + WIDTH, this.top + HEIGHT, COLOR_BORDER);

        graphics.drawCenteredString(this.font, this.title, this.left + WIDTH / 2, this.top + 6, COLOR_TITLE);

        Level level = Minecraft.getInstance().level;
        long dayTime = displayedDayTime(level);
        String clock = ClockClientState.format(dayTime);
        long ticks = Math.floorMod(dayTime, 24000L);
        Component weather = displayedWeather(level);

        int x = this.left + 10;
        int y = this.top + 22;
        graphics.drawString(this.font,
                Component.translatable("gui.eternal_food_mod_1789477527.clock.current", clock),
                x, y, COLOR_VALUE, false);
        graphics.drawString(this.font,
                Component.translatable("gui.eternal_food_mod_1789477527.clock.current_tick", ticks),
                x, y + 12, COLOR_TEXT, false);
        graphics.drawString(this.font,
                Component.translatable("gui.eternal_food_mod_1789477527.clock.current_weather")
                        .append(weather).withStyle(ChatFormatting.DARK_AQUA),
                x, y + 24, COLOR_TEXT, false);

        super.render(graphics, mouseX, mouseY, partialTick);

        // 预设按钮的两行文字（按钮自带文字是空的，由这里画）；
        // 名字的日夜颜色由 lang 里的 §e / §9 决定，所以这里传白色让组件自带样式生效。
        for (PresetButton button : this.presetButtons) {
            int centerX = button.getX() + button.getWidth() / 2;
            graphics.drawCenteredString(this.font, button.nameLine,
                    centerX, button.getY() + 4, 0xFFFFFFFF);
            graphics.drawCenteredString(this.font, button.timeLine,
                    centerX, button.getY() + 14, 0xFF0A4F8A);
        }

        for (Map.Entry<AbstractWidget, Component> entry : this.tooltips.entrySet()) {
            AbstractWidget widget = entry.getKey();
            if (widget.visible && widget.isMouseOver(mouseX, mouseY)) {
                graphics.renderTooltip(this.font, entry.getValue(), mouseX, mouseY);
                break;
            }
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.input != null && this.input.isFocused() && keyCode == 257) {
            this.applyInput();
            return true;
        }
        if (this.minecraft != null && this.minecraft.options.keyInventory.matches(keyCode, scanCode)) {
            this.onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    /** 预设按钮：自带文字留空，由面板在 {@code render} 里画两行。 */
    static class PresetButton extends Button {

        private final Component nameLine;
        private final Component timeLine;

        PresetButton(int x, int y, int width, int height, Component nameLine, Component timeLine,
                OnPress onPress) {
            super(x, y, width, height, Component.empty(), onPress, DEFAULT_NARRATION);
            this.nameLine = nameLine;
            this.timeLine = timeLine;
        }
    }

    /** 已锁定时覆盖一层冰蓝 + 高光，做出冰材质的感觉。 */
    static class IceToggleButton extends Button {

        private boolean activeState;

        IceToggleButton(int x, int y, int width, int height, Component message, OnPress onPress) {
            super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
        }

        void setActiveState(boolean state) {
            this.activeState = state;
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            super.renderWidget(graphics, mouseX, mouseY, partialTick);
            if (this.activeState) {
                graphics.fill(getX(), getY(), getX() + this.width, getY() + this.height, 0x9922C8F0);
                graphics.fill(getX() + 1, getY() + 1, getX() + this.width - 1, getY() + 4, 0x55FFFFFF);
            }
        }
    }
}
