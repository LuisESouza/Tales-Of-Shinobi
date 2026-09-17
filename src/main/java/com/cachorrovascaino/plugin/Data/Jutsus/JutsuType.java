package com.cachorrovascaino.plugin.Data.Jutsus;

import com.cachorrovascaino.plugin.Abstractions.SkillType;

public enum JutsuType {

    // NINJUTSU
    WATER_WALK("water_walk", "Suimen Hokō no Gyō", SkillType.NINJUTSU, ElementType.NONE, 0, 0, 10, 0, 0, 0, 1, 10.0f, 4),
    SUBSTITUTION("substitution_jutsu", "Kawarimi no Jutsu", SkillType.NINJUTSU, ElementType.NONE, 0, 0, 10, 0, 0, 0, 2, 10.0f, 10),
    SHADOW_CLONE("shadow_clone", "Kage Bunshin no Jutsu", SkillType.NINJUTSU, ElementType.NONE, 0, 0, 15, 0, 0, 5, 5, 25.0f, 15),
    DOTONWALL("doton_wall", "Doton: Doryūheki", SkillType.NINJUTSU, ElementType.EARTH, 0, 0, 25, 0, 0, 0, 10, 25.0f, 8),
    HEAL_JUTSU("mystical_palm_heal_jutsu", "Shōsen Jutsu", SkillType.NINJUTSU, ElementType.NONE, 0, 0, 25, 0, 0, 0, 10, 5.0f, 8),
    HIRAISHIN("hiraishin_jutsu", "Hiraishin no Jutsu", SkillType.NINJUTSU, ElementType.NONE, 0, 0, 25, 0, 0, 0, 10, 5.0f, 2),

    //WATER- NINJUTSU
    WATERBALL("waterball_jutsu", "Suiton: Suiryūdan no Jutsu", SkillType.NINJUTSU, ElementType.WATER, 0, 0, 25, 0, 0, 0, 10, 40.0f, 20),
    GEYZER("suiton_geyser_field", "Suiton: Campo de Gêiseres", SkillType.NINJUTSU, ElementType.WATER, 0, 0, 25, 0, 0, 0, 10, 40.0f, 20),
    WATERJET("suiton_water_jet", "Suiton: Suiryū Shōten no Jutsu", SkillType.NINJUTSU, ElementType.WATER, 0, 0, 25, 0, 0, 0, 10, 40.0f, 1),

    //Wind - NINJUTSU
    TORNADO("futon_tornado_jutsu", "Fūton: Tatsumaki no Jutsu", SkillType.NINJUTSU, ElementType.WIND, 0, 0, 30, 0, 0, 0, 6, 20.0f, 1),


    // FIRE - NINJUTSUS
    FIREJET("katin_fire_jet", "Katon: Suiryū Shōten no Jutsu", SkillType.NINJUTSU, ElementType.FIRE, 0, 0, 30, 0, 0, 0, 6, 20.0f, 15),
    FIREBALL("fireball_jutsu", "Katon: Gōkakyū no Jutsu", SkillType.NINJUTSU, ElementType.FIRE, 0, 0, 25, 0, 0, 0, 10, 40.0f, 20),
    METEORO("tengai_shinsei", "Katon: Tengai Ensei", SkillType.NINJUTSU, ElementType.FIRE, 0, 0, 50, 0, 0, 0, 10, 50.0f, 25),
    FIRERAIN("katon_rain_jutsu", "Katon: Tenkyū no Rain", SkillType.NINJUTSU, ElementType.FIRE, 0, 0, 50, 0, 0, 0, 10, 50.0f, 25),

    // TAIJUTSUS (Stamina: 0.15f = 15%)
    LEAF_HURRICANE("leaf_hurricane", "Konoha Senpū", SkillType.TAIJUTSU, ElementType.NONE, 10, 0, 0, 0, 5, 10, 5, 0.15f, 8),
    DYNAMIC_ENTRY("dynamic_entry", "Dainamikku Entorī", SkillType.TAIJUTSU, ElementType.NONE, 15, 0, 0, 0, 10, 20, 10, 0.20f, 8),
    LION_COMBO("lion_combo", "Shishi Rendan", SkillType.TAIJUTSU, ElementType.NONE, 25, 0, 0, 0, 15, 50, 10, 0.25f, 10),
    PRIMARY_LOTUS("primary_lotus", "Omote Renge", SkillType.TAIJUTSU, ElementType.NONE, 40, 0, 0, 0, 40, 80, 10, 0.40f, 12),

    // GENJUTSU
    KOKUANGYO("kokuangyo_genjutsu", "Kokuangyo no Jutsu", SkillType.GENJUTSU, ElementType.NONE, 0, 20, 20, 0, 0, 0, 10, 45.0f, 12),
    NEHAN_SHOJO("nehan_shojo_genjutsu", "Nehan Shōjō no Jutsu", SkillType.GENJUTSU, ElementType.NONE, 0, 20, 20, 0, 0, 0, 10, 45.0f, 12),
    KASUMI_JUSHI("kasumi_jushi_genjutsu", "Kasumi Jūshi no Jutsu", SkillType.GENJUTSU, ElementType.NONE, 0, 20, 20, 0, 0, 0, 10, 45.0f, 12);

    private final String id;
    private final String name;
    private final SkillType type;
    private final ElementType element;

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
            ElementType element,
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
        this.element = element;
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
    public ElementType getElement() { return element; }

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