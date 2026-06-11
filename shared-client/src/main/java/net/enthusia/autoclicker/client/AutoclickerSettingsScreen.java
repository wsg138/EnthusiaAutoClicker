package net.enthusia.autoclicker.client;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import net.enthusia.autoclicker.ActionMode;
import net.enthusia.autoclicker.AutoclickerConfig;
import net.enthusia.autoclicker.DurationParser;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
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
    private static final int LEFT_COLOR = 0xFFFF8C32;
    private static final int RIGHT_COLOR = 0xFF4CC9F0;
    private static final int GENERAL_COLOR = 0xFFC77DFF;

    private final AutoclickerConfig config;
    private final @Nullable Screen parent;
    private final List<ConfigRow> rows = new ArrayList<>();
    private final List<SectionHeading> headings = new ArrayList<>();

    private boolean draftLeftEnabled;
    private boolean draftRightEnabled;
    private boolean draftFoodEnabled;
    private boolean draftStatusMessages;
    private ActionMode draftLeftMode;
    private ActionMode draftRightMode;
    private EditBox leftInterval;
    private EditBox rightInterval;
    private EditBox foodThreshold;
    private EditBox runDuration;
    private Button leftEnabledButton;
    private Button rightEnabledButton;
    private Button foodEnabledButton;
    private Button statusMessagesButton;
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
        draftLeftMode = config.leftMode();
        draftRightMode = config.rightMode();
    }

    @Override
    protected void init() {
        rows.clear();
        headings.clear();

        int settingsHeight = ROW_HEIGHT * 10 + SECTION_HEIGHT * 3;
        int y = Math.max(38, (height - settingsHeight) / 2 - 7);

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
        leftInterval = addNumberRow(
            y,
            "screen.enthusia_autoclicker.left_interval",
            "screen.enthusia_autoclicker.left_interval.tooltip",
            LEFT_COLOR,
            DurationParser.formatTicks(config.leftIntervalMillis()),
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
            Integer.toString(config.foodLevelThreshold()),
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
        rightInterval = addNumberRow(
            y,
            "screen.enthusia_autoclicker.right_interval",
            "screen.enthusia_autoclicker.right_interval.tooltip",
            RIGHT_COLOR,
            DurationParser.formatTicks(config.rightIntervalMillis()),
            DurationParser.formatTicks(AutoclickerConfig.DEFAULT_RIGHT_INTERVAL_MILLIS)
        );
        y += ROW_HEIGHT;

        y = addSection(y, "screen.enthusia_autoclicker.section.general", GENERAL_COLOR);
        runDuration = addNumberRow(
            y,
            "screen.enthusia_autoclicker.run_duration",
            "screen.enthusia_autoclicker.run_duration.tooltip",
            GENERAL_COLOR,
            DurationParser.formatTicks(config.runDurationMillis()),
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

        int footerY = Math.min(height - 26, y + ROW_HEIGHT + 7);
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

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
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

        super.render(graphics, mouseX, mouseY, partialTick);

        graphics.drawCenteredString(font, title, width / 2, 14, 0xFFFFFFFF);
        for (SectionHeading heading : headings) {
            graphics.drawString(font, heading.title(), PAGE_MARGIN + 7, heading.y() + 2, heading.color());
        }
        for (ConfigRow row : rows) {
            graphics.drawString(
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
            graphics.drawCenteredString(font, validationMessage, width / 2, height - 10, 0xFFFF5555);
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
        int controlX = controlX();
        Button control = addRenderableWidget(Button.builder(toggleLabel(getter.getAsBoolean()), button -> {
            setter.accept(!getter.getAsBoolean());
            refreshControls();
        }).bounds(controlX, y + 3, CONTROL_WIDTH, 20).build());
        Button reset = addResetButton(y, () -> {
            setter.accept(defaultValue);
            refreshControls();
        });
        rows.add(new ConfigRow(
            y,
            Component.translatable(labelKey),
            Component.translatable(tooltipKey),
            color,
            control,
            reset
        ));
        return control;
    }

    private Button addModeRow(
        int y,
        String labelKey,
        String tooltipKey,
        int color,
        java.util.function.Supplier<ActionMode> getter,
        Consumer<ActionMode> setter,
        ActionMode defaultValue
    ) {
        int controlX = controlX();
        Button control = addRenderableWidget(Button.builder(modeLabel(getter.get()), button -> {
            setter.accept(getter.get().next());
            refreshControls();
        }).bounds(controlX, y + 3, CONTROL_WIDTH, 20).build());
        Button reset = addResetButton(y, () -> {
            setter.accept(defaultValue);
            refreshControls();
        });
        rows.add(new ConfigRow(
            y,
            Component.translatable(labelKey),
            Component.translatable(tooltipKey),
            color,
            control,
            reset
        ));
        return control;
    }

    private EditBox addNumberRow(
        int y,
        String labelKey,
        String tooltipKey,
        int color,
        String value,
        String defaultValue
    ) {
        EditBox control = addRenderableWidget(new EditBox(
            font,
            controlX(),
            y + 3,
            CONTROL_WIDTH,
            20,
            Component.translatable(labelKey)
        ));
        control.setFilter(candidate -> candidate.matches("\\d*"));
        control.setMaxLength(12);
        control.setValue(value);
        Button reset = addResetButton(y, () -> control.setValue(defaultValue));
        rows.add(new ConfigRow(
            y,
            Component.translatable(labelKey),
            Component.translatable(tooltipKey),
            color,
            control,
            reset
        ));
        return control;
    }

    private Button addResetButton(int y, Runnable resetAction) {
        return addRenderableWidget(Button.builder(
            Component.translatable("screen.enthusia_autoclicker.reset"),
            button -> resetAction.run()
        ).bounds(width - PAGE_MARGIN - RESET_WIDTH, y + 3, RESET_WIDTH, 20).build());
    }

    private int controlX() {
        return width - PAGE_MARGIN - RESET_WIDTH - 8 - CONTROL_WIDTH;
    }

    private void refreshControls() {
        if (leftEnabledButton == null) {
            return;
        }
        leftEnabledButton.setMessage(toggleLabel(draftLeftEnabled));
        rightEnabledButton.setMessage(toggleLabel(draftRightEnabled));
        foodEnabledButton.setMessage(toggleLabel(draftFoodEnabled));
        statusMessagesButton.setMessage(toggleLabel(draftStatusMessages));
        leftModeButton.setMessage(modeLabel(draftLeftMode));
        rightModeButton.setMessage(modeLabel(draftRightMode));

        leftModeButton.active = draftLeftEnabled;
        leftInterval.active = draftLeftEnabled && draftLeftMode == ActionMode.CLICK;
        foodEnabledButton.active = draftLeftEnabled;
        foodThreshold.active = draftLeftEnabled && draftFoodEnabled;
        rightModeButton.active = draftRightEnabled;
        rightInterval.active = draftRightEnabled && draftRightMode == ActionMode.CLICK;
    }

    private void saveAndClose() {
        try {
            long parsedLeftInterval = DurationParser.parseIntervalTicks(leftInterval.getValue());
            long parsedRightInterval = DurationParser.parseIntervalTicks(rightInterval.getValue());
            long parsedRunDuration = DurationParser.parseOptionalDurationTicks(runDuration.getValue());
            int parsedFoodThreshold = Integer.parseInt(foodThreshold.getValue());

            config.setLeftEnabled(draftLeftEnabled);
            config.setRightEnabled(draftRightEnabled);
            config.setFoodEnabled(draftFoodEnabled);
            config.setStatusMessages(draftStatusMessages);
            config.setLeftMode(draftLeftMode);
            config.setRightMode(draftRightMode);
            config.setLeftIntervalMillis(parsedLeftInterval);
            config.setRightIntervalMillis(parsedRightInterval);
            config.setRunDurationMillis(parsedRunDuration);
            config.setFoodLevelThreshold(parsedFoodThreshold);
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

    private record ConfigRow(
        int y,
        Component label,
        Component tooltip,
        int color,
        AbstractWidget control,
        Button reset
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
