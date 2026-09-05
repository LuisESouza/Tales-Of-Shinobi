package com.cachorrovascaino.plugin.Data;

public enum MangekyouType {
    OBITO("Mangekyo_Sharingan_Obito_HD", "Kamui"),
    ITACHI("Mangekyo_Sharingan_Itachi_HD", "Amaterasu & Tsukuyomi"),
    SASUKE("Mangekyo_Sharingan_Sasuke_HD", "Kagutsuchi"),
    MADARA("Mangekyo_Sharingan_Madara_HD", "Chokoe Tomoe"),
    SHISUI("Mangekyo_Sharingan_Shisui_HD", "Kotoamatsukami");

    private final String eyeAsset;
    private final String specialAbilityName;

    public static MangekyouType fromName(String name) {
        if (name == null || name.isBlank()) return null;
        for (MangekyouType type : MangekyouType.values()) {
            if (type.name().equalsIgnoreCase(name) || type.getSpecialAbilityName().equalsIgnoreCase(name)) {
                return type;
            }
        }
        return null;
    }

    MangekyouType(String eyeAsset, String specialAbilityName) {
        this.eyeAsset = eyeAsset;
        this.specialAbilityName = specialAbilityName;
    }

    public String getEyeAsset() { return eyeAsset; }
    public String getSpecialAbilityName() { return specialAbilityName; }
}
