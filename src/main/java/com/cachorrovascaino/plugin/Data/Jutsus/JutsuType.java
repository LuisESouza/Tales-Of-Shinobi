package com.cachorrovascaino.plugin.Data.Jutsus;

public enum JutsuType {
    SUBSTITUTION("substitution_jutsu", "Substitution", 10, 2, 10.0f, 10),
    SHADOW_CLONE("shadow_clone", "Kage Bunshin", 15, 5, 25.0f, 20),
    FIREBALL("fireball_jutsu", "Goukakyuu no Jutsu", 25, 10, 40.0f, 20),
    WATERBALL("waterball_jutsu", "Suiryūdan no Jutsu", 25, 10, 40.0f, 20),
    METEORO("tengai_shinsei", "Tengai Shinsei", 25, 10, 50, 1);

    private final String id;
    private final String name;
    private final int requiredNinjutsu;
    private final int requiredPoints;
    private final float chakraCost;
    private final float cooldown;

    JutsuType(String id, String name, int requiredNinjutsu, int requiredPoints, float chakraCost, float cooldown) {
        this.id = id;
        this.name = name;
        this.requiredNinjutsu = requiredNinjutsu;
        this.requiredPoints = requiredPoints;
        this.chakraCost = chakraCost;
        this.cooldown = cooldown;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public int getRequiredNinjutsu() { return requiredNinjutsu; }
    public int getRequiredPoints() { return requiredPoints; }
    public float getChakraCost() { return chakraCost; }
    public float getCooldown(){ return cooldown; }

    public static JutsuType fromId(String id) {
        if (id == null) return null;
        for (JutsuType jutsu : values()) {
            if (jutsu.getId().equalsIgnoreCase(id)) return jutsu;
        }
        return null;
    }
}