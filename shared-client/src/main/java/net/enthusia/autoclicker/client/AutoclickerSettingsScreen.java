package net.enthusia.autoclicker.client;

import net.enthusia.autoclicker.AutoclickerConfig;
import net.enthusia.autoclicker.ActionMode;
import net.enthusia.autoclicker.DurationParser;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class AutoclickerSettingsScreen extends Screen {
    private static final int FIELD_WIDTH = 140;

    private final AutoclickerConfig config;
    private ActionMode draftLeftMode;
    private ActionMode draftRightMode;
    private EditBox leftInterval;
    private EditBox rightInterval;
    private EditBox runDuration;
    private Button leftMode;
    private Button rightMode;
    private Component validationMessage = Component.empty();

    public AutoclickerSettingsScreen(AutoclickerConfig config) {
        super(Component.translatable("screen.enthusia_autoclicker.title"));
        this.config = config;
        this.draftLeftMode = config.leftMode();
        this.draftRightMode = config.rightMode();
    }

    @Override
    protected void init() {
        int left = width / 2 - 155;
        int fieldLeft = width / 2 + 15;
        int y = height / 2 - 90;

        leftMode = addRenderableWidget(Button.builder(leftModeLabel(), button -> {
            draftLeftMode = draftLeftMode.next();
            button.setMessage(leftModeLabel());
        }).bounds(fieldLeft, y, FIELD_WIDTH, 20).build());

        rightMode = addRenderableWidget(Button.builder(rightModeLabel(), button -> {
            draftRightMode = draftRightMode.next();
            button.setMessage(rightModeLabel());
        }).bounds(fieldLeft, y + 28, FIELD_WIDTH, 20).build());

        leftInterval = addRenderableWidget(new EditBox(
            font,
            fieldLeft,
            y + 56,
            FIELD_WIDTH,
            20,
            Component.translatable("screen.enthusia_autoclicker.left_interval")
        ));
        leftInterval.setValue(DurationParser.format(config.leftIntervalMillis()));

        rightInterval = addRenderableWidget(new EditBox(
            font,
            fieldLeft,
            y + 84,
            FIELD_WIDTH,
            20,
            Component.translatable("screen.enthusia_autoclicker.right_interval")
        ));
        rightInterval.setValue(DurationParser.format(config.rightIntervalMillis()));

        runDuration = addRenderableWidget(new EditBox(
            font,
            fieldLeft,
            y + 112,
            FIELD_WIDTH,
            20,
            Component.translatable("screen.enthusia_autoclicker.run_duration")
        ));
        runDuration.setValue(DurationParser.format(config.runDurationMillis()));

        addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> saveAndClose())
            .bounds(width / 2 - 75, y + 153, 150, 20)
            .build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);

        int labelLeft = width / 2 - 155;
        int y = height / 2 - 90;
        graphics.drawString(font, title, width / 2 - font.width(title) / 2, y - 27, 0xFFFFFF);
        graphics.drawString(font, Component.translatable("screen.enthusia_autoclicker.left_mode"), labelLeft, y + 6, 0xFFFFFF);
        graphics.drawString(font, Component.translatable("screen.enthusia_autoclicker.right_mode"), labelLeft, y + 34, 0xFFFFFF);
        graphics.drawString(font, Component.translatable("screen.enthusia_autoclicker.left_interval"), labelLeft, y + 62, 0xFFFFFF);
        graphics.drawString(font, Component.translatable("screen.enthusia_autoclicker.right_interval"), labelLeft, y + 90, 0xFFFFFF);
        graphics.drawString(font, Component.translatable("screen.enthusia_autoclicker.run_duration"), labelLeft, y + 118, 0xFFFFFF);
        graphics.drawCenteredString(font, Component.translatable("screen.enthusia_autoclicker.hint"), width / 2, y + 137, 0xA0A0A0);
        graphics.drawCenteredString(font, validationMessage, width / 2, y + 178, 0xFF5555);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void saveAndClose() {
        try {
            long parsedLeftInterval = DurationParser.parseIntervalMillis(leftInterval.getValue());
            long parsedRightInterval = DurationParser.parseIntervalMillis(rightInterval.getValue());
            long parsedRunDuration = DurationParser.parseOptionalDurationMillis(runDuration.getValue());

            config.setLeftMode(draftLeftMode);
            config.setRightMode(draftRightMode);
            config.setLeftIntervalMillis(parsedLeftInterval);
            config.setRightIntervalMillis(parsedRightInterval);
            config.setRunDurationMillis(parsedRunDuration);
            config.save();
            onClose();
        } catch (IllegalArgumentException exception) {
            validationMessage = Component.literal(exception.getMessage()).withStyle(ChatFormatting.RED);
        }
    }

    private Component leftModeLabel() {
        return Component.translatable("mode.enthusia_autoclicker." + draftLeftMode.name().toLowerCase());
    }

    private Component rightModeLabel() {
        return Component.translatable("mode.enthusia_autoclicker." + draftRightMode.name().toLowerCase());
    }
}
