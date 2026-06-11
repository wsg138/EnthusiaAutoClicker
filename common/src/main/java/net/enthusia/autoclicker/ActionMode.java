package net.enthusia.autoclicker;

public enum ActionMode {
    CLICK,
    HOLD;

    public ActionMode next() {
        ActionMode[] values = values();
        return values[(ordinal() + 1) % values.length];
    }
}
