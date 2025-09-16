package com.roguelike.ui;

public final class PassiveItemSpec {
    private final PassiveId id;
    private final String displayName;
    private final String iconPath; // assets/images/passive item/xxx.png
    private final double[] values; // 5级数值（倍率或加值）

    public PassiveItemSpec(PassiveId id, String displayName, String iconPath, double[] values) {
        this.id = id;
        this.displayName = displayName;
        this.iconPath = iconPath;
        this.values = values != null ? values.clone() : new double[0];
    }

    public PassiveId getId() { return id; }
    public String getDisplayName() { return displayName; }
    public String getIconPath() { return iconPath; }
    public double getValueAtLevel(int level) {
        int lv = Math.max(1, Math.min(5, level));
        if (values.length >= lv) return values[lv - 1];
        return values.length > 0 ? values[values.length - 1] : 0.0;
    }
}


