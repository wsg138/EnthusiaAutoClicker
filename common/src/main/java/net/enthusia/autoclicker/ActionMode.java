package net.enthusia.autoclicker;

public enum ActionMode {
    OFF,
    CLICK,
    HOLD;

    public ActionMode next() {
        ActionMode[] values = values();
        return values[(ordinal() + 1) % values.length];
    }
}
