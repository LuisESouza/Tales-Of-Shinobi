package com.cachorrovascaino.plugin.Data.Jutsus;

public enum ElementType {
    NONE("Nenhum"),
    FIRE("Katon"),
    WATER("Suiton"),
    WIND("Fūton"),
    EARTH("Doton"),
    LIGHTNING("Raiton"),
    YANG("Yinton/Shōsen");

    private final String displayName;

    ElementType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() { return displayName; }
}