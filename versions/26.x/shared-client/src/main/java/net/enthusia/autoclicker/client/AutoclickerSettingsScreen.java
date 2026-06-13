package net.enthusia.autoclicker.client;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.enthusia.autoclicker.ActionMode;
import net.enthusia.autoclicker.AutoclickerConfig;
import net.enthusia.autoclicker.DurationParser;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

public final class AutoclickerSettingsScreen extends Screen {
    private static final int PAGE_MARGIN = 16;
    private static final int ROW_HEIGHT = 27;
    private static final int SECTION_HEIGHT = 15;
    private static final int CONTROL_WIDTH = 150;
    private static final int RESET_WIDTH = 48;
    private static final int TAB_WIDTH = 120;
    private static final int LEFT_COLOR = 0xFFFF8C32;
    private static final int RIGHT_COLOR = 0xFF4CC9F0;
    private static final int GENERAL_COLOR = 0xFFC77DFF;
    private static final int SAFETY_COLOR = 0xFFFF5D73;
    private static final int FOOD_COLOR = 0xFF65D46E;

    private final AutoclickerConfig config;
    private final @Nullable Screen parent;
    private final List<ConfigRow> rows = new ArrayList<>();
    private final List<SectionHeading> headings = new ArrayList<>();

    private Page page = Page.CLICKER;
    private boolean draftLeftEnabled;
    private boolean draftRightEnabled;
    private boolean draftFoodEnabled;
    private boolean draftStatusMessages;
    private boolean draftDurabilityGuard;
    private boolean draftArmorStandEating;
    private boolean draftAutoRestock;
    private boolean draftStopWhenOutOfFood;
    private ActionMode draftLeftMode;
    private ActionMode draftRightMode;
    private String draftLeftInterval;
    private String draftRightInterval;
    private String draftFoodThreshold;
    private String draftRunDuration;
    private String draftMinimumDurability;
    private String draftRestockAtCount;

    private EditBox leftInterval;
    private EditBox rightInterval;
    private EditBox foodThreshold;
    private EditBox minimumDurability;
    private EditBox restockAtCount;
    private Button leftEnabledButton;
    private Button rightEnabledButton;
    private Button foodEnabledButton;
    private Button statusMessagesButton;
    private Button durabilityGuardButton;
    private Button armorStandEatingButton;
    private Button autoRestockButton;
    private Button stopWhenOutOfFoodButton;
    private Button leftModeButton;
    private Button rightModeButton;
    private Component validationMessage = Component.empty();

    public AutoclickerSettingsScreen(AutoclickerConfig config, @Nullable Screen parent) {
        super(Component.translatable("screen.enthusia_autoclicker.title"));
        this.config = config;
        this.parent = parent;
        draftLeftEnabled = config.leftEnabled();
        draftRightEnabled = config.rightEnabled();
        draftFoodEnabled = config.foodEnabled();
        draftStatusMessages = config.statusMessages();
        draftDurabilityGuard = config.durabilityGuard();
        draftArmorStandEating = config.armorStandEating();
        draftAutoRestock = config.autoRestock();
        draftStopWhenOutOfFood = config.stopWhenOutOfFood();
        draftLeftMode = config.leftMode();
        draftRightMode = config.rightMode();
        draftLeftInterval = DurationParser.formatTicks(config.leftIntervalMillis());
        draftRightInterval = DurationParser.formatTicks(config.rightIntervalMillis());
        draftFoodThreshold = Integer.toString(config.foodLevelThreshold());
        draftRunDuration = DurationParser.formatTicks(config.runDurationMillis());
        draftMinimumDurability = Integer.toString(config.minimumDurability());
        draftRestockAtCount = Integer.toString(config.restockAtCount());
    }

    @Override
    protected void init() {
        rows.clear();
        headings.clear();
        clearWidgetReferences();
        addTabs();

        int y = page == Page.CLICKER ? initClickerPage() : initExtrasPage();
        int footerY = Math.min(height - 26, y + 7);
        addRenderableWidget(Button.builder(
            Component.translatable("gui.cancel"),
            button -> onClose()
        ).bounds(width / 2 - 154, footerY, 150, 20).build());
        addRenderableWidget(Button.builder(
            Component.translatable("screen.enthusia_autoclicker.save"),
            button -> saveAndClose()
        ).bounds(width / 2 + 4, footerY, 150, 20).build());

        refreshControls();
    }

    private int initClickerPage() {
        int settingsHeight = ROW_HEIGHT * 10 + SECTION_HEIGHT * 3;
        int y = Math.max(62, (height - settingsHeight) / 2 + 7);

        y = addSection(y, "screen.enthusia_autoclicker.section.left", LEFT_COLOR);
        leftEnabledButton = addToggleRow(
            y,
            "screen.enthusia_autoclicker.left_enabled",
            "screen.enthusia_autoclicker.left_enabled.tooltip",
            LEFT_COLOR,
            () -> draftLeftEnabled,
            value -> draftLeftEnabled = value,
            AutoclickerConfig.DEFAULT_LEFT_ENABLED
        );
        y += ROW_HEIGHT;
        leftModeButton = addModeRow(
            y,
            "screen.enthusia_autoclicker.left_mode",
            "screen.enthusia_autoclicker.left_mode.tooltip",
            LEFT_COLOR,
            () -> draftLeftMode,
            value -> draftLeftMode = value,
            AutoclickerConfig.DEFAULT_LEFT_MODE
        );
        y += ROW_HEIGHT;
        leftInterval = addIntervalRow(
            y,
            "screen.enthusia_autoclicker.left_interval",
            "screen.enthusia_autoclicker.left_interval.tooltip",
            LEFT_COLOR,
            () -> draftLeftInterval,
            value -> draftLeftInterval = value,
            DurationParser.formatTicks(AutoclickerConfig.DEFAULT_LEFT_INTERVAL_MILLIS)
        );
        y += ROW_HEIGHT;
        foodEnabledButton = addToggleRow(
            y,
            "screen.enthusia_autoclicker.food_enabled",
            "screen.enthusia_autoclicker.food_enabled.tooltip",
            LEFT_COLOR,
            () -> draftFoodEnabled,
            value -> draftFoodEnabled = value,
            AutoclickerConfig.DEFAULT_FOOD_ENABLED
        );
        y += ROW_HEIGHT;
        foodThreshold = addNumberRow(
            y,
            "screen.enthusia_autoclicker.food_threshold",
            "screen.enthusia_autoclicker.food_threshold.tooltip",
            LEFT_COLOR,
            () -> draftFoodThreshold,
            value -> draftFoodThreshold = value,
            Integer.toString(AutoclickerConfig.DEFAULT_FOOD_LEVEL_THRESHOLD)
        );
        y += ROW_HEIGHT;

        y = addSection(y, "screen.enthusia_autoclicker.section.right", RIGHT_COLOR);
        rightEnabledButton = addToggleRow(
            y,
            "screen.enthusia_autoclicker.right_enabled",
            "screen.enthusia_autoclicker.right_enabled.tooltip",
            RIGHT_COLOR,
            () -> draftRightEnabled,
            value -> draftRightEnabled = value,
            AutoclickerConfig.DEFAULT_RIGHT_ENABLED
        );
        y += ROW_HEIGHT;
        rightModeButton = addModeRow(
            y,
            "screen.enthusia_autoclicker.right_mode",
            "screen.enthusia_autoclicker.right_mode.tooltip",
            RIGHT_COLOR,
            () -> draftRightMode,
            value -> draftRightMode = value,
            AutoclickerConfig.DEFAULT_RIGHT_MODE
        );
        y += ROW_HEIGHT;
        rightInterval = addIntervalRow(
            y,
            "screen.enthusia_autoclicker.right_interval",
            "screen.enthusia_autoclicker.right_interval.tooltip",
            RIGHT_COLOR,
            () -> draftRightInterval,
            value -> draftRightInterval = value,
            DurationParser.formatTicks(AutoclickerConfig.DEFAULT_RIGHT_INTERVAL_MILLIS)
        );
        y += ROW_HEIGHT;

        y = addSection(y, "screen.enthusia_autoclicker.section.general", GENERAL_COLOR);
        addNumberRow(
            y,
            "screen.enthusia_autoclicker.run_duration",
            "screen.enthusia_autoclicker.run_duration.tooltip",
            GENERAL_COLOR,
            () -> draftRunDuration,
            value -> draftRunDuration = value,
            DurationParser.formatTicks(AutoclickerConfig.DEFAULT_RUN_DURATION_MILLIS)
        );
        y += ROW_HEIGHT;
        statusMessagesButton = addToggleRow(
            y,
            "screen.enthusia_autoclicker.status_messages",
            "screen.enthusia_autoclicker.status_messages.tooltip",
            GENERAL_COLOR,
            () -> draftStatusMessages,
            value -> draftStatusMessages = value,
            AutoclickerConfig.DEFAULT_STATUS_MESSAGES
        );
        return y + ROW_HEIGHT;
    }

    private int initExtrasPage() {
        int settingsHeight = ROW_HEIGHT * 6 + SECTION_HEIGHT * 2;
        int y = Math.max(76, (height - settingsHeight) / 2);

        y = addSection(y, "screen.enthusia_autoclicker.section.safety", SAFETY_COLOR);
        durabilityGuardButton = addToggleRow(
            y,
            "screen.enthusia_autoclicker.durability_guard",
            "screen.enthusia_autoclicker.durability_guard.tooltip",
            SAFETY_COLOR,
            () -> draftDurabilityGuard,
            value -> draftDurabilityGuard = value,
            AutoclickerConfig.DEFAULT_DURABILITY_GUARD
        );
        y += ROW_HEIGHT;
        minimumDurability = addNumberRow(
            y,
            "screen.enthusia_autoclicker.minimum_durability",
            "screen.enthusia_autoclicker.minimum_durability.tooltip",
            SAFETY_COLOR,
            () -> draftMinimumDurability,
            value -> draftMinimumDurability = value,
            Integer.toString(AutoclickerConfig.DEFAULT_MINIMUM_DURABILITY)
        );
        y += ROW_HEIGHT;

        y = addSection(y, "screen.enthusia_autoclicker.section.food_automation", FOOD_COLOR);
        armorStandEatingButton = addToggleRow(
            y,
            "screen.enthusia_autoclicker.armor_stand_eating",
            "screen.enthusia_autoclicker.armor_stand_eating.tooltip",
            FOOD_COLOR,
            () -> draftArmorStandEating,
            value -> draftArmorStandEating = value,
            AutoclickerConfig.DEFAULT_ARMOR_STAND_EATING
        );
        y += ROW_HEIGHT;
        autoRestockButton = addToggleRow(
            y,
            "screen.enthusia_autoclicker.auto_restock",
            "screen.enthusia_autoclicker.auto_restock.tooltip",
            FOOD_COLOR,
            () -> draftAutoRestock,
            value -> draftAutoRestock = value,
            AutoclickerConfig.DEFAULT_AUTO_RESTOCK
        );
        y += ROW_HEIGHT;
        restockAtCount = addNumberRow(
            y,
            "screen.enthusia_autoclicker.restock_at_count",
            "screen.enthusia_autoclicker.restock_at_count.tooltip",
            FOOD_COLOR,
            () -> draftRestockAtCount,
            value -> draftRestockAtCount = value,
            Integer.toString(AutoclickerConfig.DEFAULT_RESTOCK_AT_COUNT)
        );
        y += ROW_HEIGHT;
        stopWhenOutOfFoodButton = addToggleRow(
            y,
            "screen.enthusia_autoclicker.stop_when_out_of_food",
            "screen.enthusia_autoclicker.stop_when_out_of_food.tooltip",
            FOOD_COLOR,
            () -> draftStopWhenOutOfFood,
            value -> draftStopWhenOutOfFood = value,
            AutoclickerConfig.DEFAULT_STOP_WHEN_OUT_OF_FOOD
        );
        return y + ROW_HEIGHT;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        for (ConfigRow row : rows) {
            boolean hovered = row.contains(mouseX, mouseY, width);
            graphics.fill(
                PAGE_MARGIN,
                row.y(),
                width - PAGE_MARGIN,
                row.y() + ROW_HEIGHT - 1,
                hovered ? 0x663A3A46 : 0x44202028
            );
            graphics.fill(PAGE_MARGIN, row.y(), PAGE_MARGIN + 3, row.y() + ROW_HEIGHT - 1, row.color());
        }

        super.extractRenderState(graphics, mouseX, mouseY, partialTick);

        graphics.centeredText(font, title, width / 2, 10, 0xFFFFFFFF);
        int selectedTabX = page == Page.CLICKER ? width / 2 - TAB_WIDTH - 2 : width / 2 + 2;
        graphics.fill(selectedTabX, 53, selectedTabX + TAB_WIDTH, 55, 0xFFFFA13D);

        for (SectionHeading heading : headings) {
            graphics.text(font, heading.title(), PAGE_MARGIN + 7, heading.y() + 2, heading.color());
        }
        for (ConfigRow row : rows) {
            graphics.text(
                font,
                row.label(),
                PAGE_MARGIN + 12,
                row.y() + (ROW_HEIGHT - font.lineHeight) / 2,
                row.control().active ? 0xFFFFFFFF : 0xFF777777
            );
            if (row.contains(mouseX, mouseY, width)) {
                graphics.setTooltipForNextFrame(row.tooltip(), mouseX, mouseY);
            }
        }
        if (!validationMessage.getString().isEmpty()) {
            graphics.centeredText(font, validationMessage, width / 2, height - 10, 0xFFFF5555);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        if (minecraft != null) {
            minecraft.setScreen(parent);
        }
    }

    private void addTabs() {
        addRenderableWidget(Button.builder(tabLabel(Page.CLICKER), button -> switchPage(Page.CLICKER))
            .bounds(width / 2 - TAB_WIDTH - 2, 31, TAB_WIDTH, 22)
            .build());
        addRenderableWidget(Button.builder(tabLabel(Page.EXTRAS), button -> switchPage(Page.EXTRAS))
            .bounds(width / 2 + 2, 31, TAB_WIDTH, 22)
            .build());
    }

    private void switchPage(Page nextPage) {
        if (page != nextPage) {
            page = nextPage;
            validationMessage = Component.empty();
            rebuildWidgets();
        }
    }

    private Component tabLabel(Page tab) {
        String key = tab == Page.CLICKER
            ? "screen.enthusia_autoclicker.tab.clicker"
            : "screen.enthusia_autoclicker.tab.extras";
        return Component.translatable(key).withStyle(
            tab == page ? ChatFormatting.GOLD : ChatFormatting.GRAY,
            tab == page ? ChatFormatting.BOLD : ChatFormatting.RESET
        );
    }

    private int addSection(int y, String titleKey, int color) {
        headings.add(new SectionHeading(y, Component.translatable(titleKey), color));
        return y + SECTION_HEIGHT;
    }

    private Button addToggleRow(
        int y,
        String labelKey,
        String tooltipKey,
        int color,
        BooleanSupplier getter,
        Consumer<Boolean> setter,
        boolean defaultValue
    ) {
        Button control = addRenderableWidget(Button.builder(toggleLabel(getter.getAsBoolean()), button -> {
            setter.accept(!getter.getAsBoolean());
            refreshControls();
        }).bounds(controlX(), y + 3, CONTROL_WIDTH, 20).build());
        addResetButton(y, () -> {
            setter.accept(defaultValue);
            refreshControls();
        });
        rows.add(new ConfigRow(
            y,
            Component.translatable(labelKey),
            Component.translatable(tooltipKey),
            color,
            control
        ));
        return control;
    }

    private Button addModeRow(
        int y,
        String labelKey,
        String tooltipKey,
        int color,
        Supplier<ActionMode> getter,
        Consumer<ActionMode> setter,
        ActionMode defaultValue
    ) {
        Button control = addRenderableWidget(Button.builder(modeLabel(getter.get()), button -> {
            setter.accept(getter.get().next());
            refreshControls();
        }).bounds(controlX(), y + 3, CONTROL_WIDTH, 20).build());
        addResetButton(y, () -> {
            setter.accept(defaultValue);
            refreshControls();
        });
        rows.add(new ConfigRow(
            y,
            Component.translatable(labelKey),
            Component.translatable(tooltipKey),
            color,
            control
        ));
        return control;
    }

    private EditBox addNumberRow(
        int y,
        String labelKey,
        String tooltipKey,
        int color,
        Supplier<String> getter,
        Consumer<String> setter,
        String defaultValue
    ) {
        return addNumericRow(
            y,
            labelKey,
            tooltipKey,
            color,
            getter,
            setter,
            defaultValue,
            false
        );
    }

    private EditBox addIntervalRow(
        int y,
        String labelKey,
        String tooltipKey,
        int color,
        Supplier<String> getter,
        Consumer<String> setter,
        String defaultValue
    ) {
        return addNumericRow(
            y,
            labelKey,
            tooltipKey,
            color,
            getter,
            setter,
            defaultValue,
            true
        );
    }

    private EditBox addNumericRow(
        int y,
        String labelKey,
        String tooltipKey,
        int color,
        Supplier<String> getter,
        Consumer<String> setter,
        String defaultValue,
        boolean allowDecimal
    ) {
        EditBox control = addRenderableWidget(new EditBox(
            font,
            controlX(),
            y + 3,
            CONTROL_WIDTH,
            20,
            Component.translatable(labelKey)
        ));
        control.setMaxLength(12);
        control.setValue(getter.get());
        control.setResponder(value -> {
            String filtered = filterNumericInput(value, allowDecimal);
            if (!filtered.equals(value)) {
                control.setValue(filtered);
                return;
            }
            setter.accept(value);
        });
        addResetButton(y, () -> control.setValue(defaultValue));
        rows.add(new ConfigRow(
            y,
            Component.translatable(labelKey),
            Component.translatable(tooltipKey),
            color,
            control
        ));
        return control;
    }

    private static String filterNumericInput(String value, boolean allowDecimal) {
        StringBuilder filtered = new StringBuilder(value.length());
        boolean decimalSeen = false;
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            if (Character.isDigit(character)) {
                filtered.append(character);
            } else if (allowDecimal && character == '.' && !decimalSeen) {
                filtered.append(character);
                decimalSeen = true;
            }
        }
        return filtered.toString();
    }

    private void addResetButton(int y, Runnable resetAction) {
        addRenderableWidget(Button.builder(
            Component.translatable("screen.enthusia_autoclicker.reset"),
            button -> resetAction.run()
        ).bounds(width - PAGE_MARGIN - RESET_WIDTH, y + 3, RESET_WIDTH, 20).build());
    }

    private int controlX() {
        return width - PAGE_MARGIN - RESET_WIDTH - 8 - CONTROL_WIDTH;
    }

    private void clearWidgetReferences() {
        leftInterval = null;
        rightInterval = null;
        foodThreshold = null;
        minimumDurability = null;
        restockAtCount = null;
        leftEnabledButton = null;
        rightEnabledButton = null;
        foodEnabledButton = null;
        statusMessagesButton = null;
        durabilityGuardButton = null;
        armorStandEatingButton = null;
        autoRestockButton = null;
        stopWhenOutOfFoodButton = null;
        leftModeButton = null;
        rightModeButton = null;
    }

    private void refreshControls() {
        setToggleMessage(leftEnabledButton, draftLeftEnabled);
        setToggleMessage(rightEnabledButton, draftRightEnabled);
        setToggleMessage(foodEnabledButton, draftFoodEnabled);
        setToggleMessage(statusMessagesButton, draftStatusMessages);
        setToggleMessage(durabilityGuardButton, draftDurabilityGuard);
        setToggleMessage(armorStandEatingButton, draftArmorStandEating);
        setToggleMessage(autoRestockButton, draftAutoRestock);
        setToggleMessage(stopWhenOutOfFoodButton, draftStopWhenOutOfFood);

        if (leftModeButton != null) {
            leftModeButton.setMessage(modeLabel(draftLeftMode));
            leftModeButton.active = draftLeftEnabled;
        }
        if (rightModeButton != null) {
            rightModeButton.setMessage(modeLabel(draftRightMode));
            rightModeButton.active = draftRightEnabled;
        }
        setActive(leftInterval, draftLeftEnabled && draftLeftMode == ActionMode.CLICK);
        setActive(foodEnabledButton, draftLeftEnabled);
        setActive(foodThreshold, draftLeftEnabled && draftFoodEnabled);
        setActive(rightInterval, draftRightEnabled && draftRightMode == ActionMode.CLICK);
        setActive(minimumDurability, draftDurabilityGuard);
        setActive(armorStandEatingButton, draftFoodEnabled);
        setActive(autoRestockButton, draftFoodEnabled);
        setActive(restockAtCount, draftFoodEnabled && draftAutoRestock);
        setActive(stopWhenOutOfFoodButton, draftFoodEnabled);
    }

    private void saveAndClose() {
        try {
            long parsedLeftInterval = DurationParser.parseIntervalTicks(draftLeftInterval);
            long parsedRightInterval = DurationParser.parseIntervalTicks(draftRightInterval);
            long parsedRunDuration = DurationParser.parseOptionalDurationTicks(draftRunDuration);
            int parsedFoodThreshold = Integer.parseInt(draftFoodThreshold);
            int parsedMinimumDurability = Integer.parseInt(draftMinimumDurability);
            int parsedRestockAtCount = Integer.parseInt(draftRestockAtCount);

            config.setLeftEnabled(draftLeftEnabled);
            config.setRightEnabled(draftRightEnabled);
            config.setFoodEnabled(draftFoodEnabled);
            config.setStatusMessages(draftStatusMessages);
            config.setDurabilityGuard(draftDurabilityGuard);
            config.setArmorStandEating(draftArmorStandEating);
            config.setAutoRestock(draftAutoRestock);
            config.setStopWhenOutOfFood(draftStopWhenOutOfFood);
            config.setLeftMode(draftLeftMode);
            config.setRightMode(draftRightMode);
            config.setLeftIntervalMillis(parsedLeftInterval);
            config.setRightIntervalMillis(parsedRightInterval);
            config.setRunDurationMillis(parsedRunDuration);
            config.setFoodLevelThreshold(parsedFoodThreshold);
            config.setMinimumDurability(parsedMinimumDurability);
            config.setRestockAtCount(parsedRestockAtCount);
            config.save();
            onClose();
        } catch (NumberFormatException exception) {
            validationMessage = Component.translatable(
                "screen.enthusia_autoclicker.validation.whole_number"
            ).withStyle(ChatFormatting.RED);
        } catch (IllegalArgumentException exception) {
            validationMessage = Component.literal(exception.getMessage()).withStyle(ChatFormatting.RED);
        }
    }

    private static void setToggleMessage(@Nullable Button button, boolean enabled) {
        if (button != null) {
            button.setMessage(toggleLabel(enabled));
        }
    }

    private static void setActive(@Nullable AbstractWidget widget, boolean active) {
        if (widget != null) {
            widget.active = active;
        }
    }

    private static Component toggleLabel(boolean enabled) {
        return Component.translatable(enabled
            ? "screen.enthusia_autoclicker.on"
            : "screen.enthusia_autoclicker.off"
        ).withStyle(enabled ? ChatFormatting.GREEN : ChatFormatting.RED, ChatFormatting.BOLD);
    }

    private static Component modeLabel(ActionMode mode) {
        return Component.translatable(
            "mode.enthusia_autoclicker." + mode.name().toLowerCase()
        ).withStyle(mode == ActionMode.CLICK ? ChatFormatting.AQUA : ChatFormatting.GOLD);
    }

    private enum Page {
        CLICKER,
        EXTRAS
    }

    private record ConfigRow(
        int y,
        Component label,
        Component tooltip,
        int color,
        AbstractWidget control
    ) {
        private boolean contains(int mouseX, int mouseY, int screenWidth) {
            return mouseX >= PAGE_MARGIN
                && mouseX < screenWidth - PAGE_MARGIN
                && mouseY >= y
                && mouseY < y + ROW_HEIGHT;
        }
    }

    private record SectionHeading(int y, Component title, int color) {
    }
}
