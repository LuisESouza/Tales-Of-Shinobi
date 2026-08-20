package com.cachorrovascaino.plugin.Data.Jutsus;

import com.cachorrovascaino.plugin.Abstractions.SkillType;

public enum JutsuType {

    //NINJUTSU
    SUBSTITUTION("substitution_jutsu", "Substitution", SkillType.NINJUTSU, 0, 0, 10, 0, 0, 0, 2, 10.0f, 10),
    SHADOW_CLONE("shadow_clone", "Kage Bunshin", SkillType.NINJUTSU, 0, 0, 15, 0, 0, 5, 5, 25.0f, 15),
    FIREBALL("fireball_jutsu", "Goukakyuu no Jutsu", SkillType.NINJUTSU, 0, 0, 25, 0, 0, 0, 10, 40.0f, 20),
    WATERBALL("waterball_jutsu", "Suiryūdan no Jutsu", SkillType.NINJUTSU, 0, 0, 25, 0, 0, 0, 10, 40.0f, 20),
    METEORO("tengai_shinsei", "Tengai Shinsei", SkillType.NINJUTSU, 0, 0, 50, 0, 0, 0, 10, 50.0f, 25),
    DOTONWALL("doton_wall", "Doton Wall", SkillType.NINJUTSU, 0, 0, 25, 0, 0, 0, 10, 25.0f, 8),
    HEAL_JUTSU("mystical_palm_heal_jutsu", "Palm Heal", SkillType.NINJUTSU, 0, 0, 25, 0, 0, 0, 10, 5.0f, 8),


    // TAIJUTSUS (Requer Taijutsu/Stamina/Speed, Custa Stamina)
    LEAF_HURRICANE("leaf_hurricane", "Leaf Hurricane", SkillType.TAIJUTSU, 10, 0, 0, 0, 5, 10, 5, 15.0f, 8),
    LION_COMBO("lion_combo", "Shishi Rendan", SkillType.TAIJUTSU, 20, 0, 0, 0, 15, 50, 10, 25.0f, 10),
    PRIMARY_LOTUS("primary_lotus", "Omote Renge", SkillType.TAIJUTSU, 35, 0, 0, 0, 40, 80, 10, 45.0f, 12),

    // GENJUTSU
    KOKUANGYO("kokuangyo_genjutsu", "Kokuangyo", SkillType.GENJUTSU, 0, 20, 20, 0, 0, 0, 10, 45.0f, 12),
    NEHAN_SHOJO("nehan_shojo_genjutsu", "Nehan Shojo", SkillType.GENJUTSU, 0, 20, 20, 0, 0, 0, 10, 45.0f, 12),
    KASUMI_JUSHI("kasumi_jushi_genjutsu", "Kasumi Jushi", SkillType.GENJUTSU, 0, 20, 20, 0, 0, 0, 10, 45.0f, 12);

    private final String id;
    private final String name;
    private final SkillType type;

    private final int reqTaijutsu;
    private final int reqGenjutsu;
    private final int reqNinjutsu;
    private final int reqHealth;
    private final int reqSpeed;
    private final int reqStamina;

    private final int requiredPoints;
    private final float resourceCost;
    private final float cooldown;

    JutsuType(
            String id,
            String name,
            SkillType type,
            int reqTaijutsu,
            int reqGenjutsu,
            int reqNinjutsu,
            int reqHealth,
            int reqSpeed,
            int reqStamina,
            int requiredPoints,
            float resourceCost,
            float cooldown
    ) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.reqTaijutsu = reqTaijutsu;
        this.reqGenjutsu = reqGenjutsu;
        this.reqNinjutsu = reqNinjutsu;
        this.reqHealth = reqHealth;
        this.reqSpeed = reqSpeed;
        this.reqStamina = reqStamina;
        this.requiredPoints = requiredPoints;
        this.resourceCost = resourceCost;
        this.cooldown = cooldown;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public SkillType getType() { return type; }

    // Getters dos requisitos
    public int getReqTaijutsu() { return reqTaijutsu; }
    public int getReqGenjutsu() { return reqGenjutsu; }
    public int getReqNinjutsu() { return reqNinjutsu; }
    public int getReqHealth() { return reqHealth; }
    public int getReqSpeed() { return reqSpeed; }
    public int getReqStamina() { return reqStamina; }

    public int getRequiredPoints() { return requiredPoints; }
    public float getResourceCost() { return resourceCost; }
    public float getCooldown() { return cooldown; }

    public static JutsuType fromId(String id) {
        if (id == null) return null;
        for (JutsuType jutsu : values()) {
            if (jutsu.getId().equalsIgnoreCase(id)) return jutsu;
        }
        return null;
    }
}