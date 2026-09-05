package com.cachorrovascaino.plugin.Data.Clan;

import java.util.List;

public enum ClanType {
    NONE("None", "No clan defined.", 0.0f, 0.0f, 1.0f, List.of()),

    UCHIHA("Uchiha", "Masters of Fire Release with excellent Chakra control and evolving Dōjutsu.", 0.0f, 25.0f, 1.2f, List.of(
            new ClanSkill("sharingan_1", "Sharingan (1 Tomoe)", "Enhances perception and basic evasion.", 20.0f, 140.0f, 15, 25, 10, null, false),

            new ClanSkill("sharingan_2", "Sharingan (2 Tomoes)", "Allows predicting movements and copying simple techniques.", 20.0f, 200.0f, 30, 45, 20, "sharingan_1", false),

            new ClanSkill("sharingan_3", "Sharingan (3 Tomoes)", "Maximum perception and complete mastery of the base dōjutsu.", 20.0f, 320.0f, 40, 55, 30, "sharingan_2", false),

            new ClanSkill("mangekyou", "Mangekyō Sharingan", "Awakens the supreme ocular powers of the clan.", 20.0f, 420.0f, 60, 75, 50, "sharingan_3", true),

            new ClanSkill("susanoo", "Susanoo", "Summons a humanoid armor of supreme chakra.", 10.0f, 0.0f, 0, 0, 0, "mangekyou", true),

            new ClanSkill("kamui_behind_teleport", "Kamui: Temporal Ambush", "Teleports behind the targeted enemy through space-time.", 100.0f, 450.0f, 0, 80, 0, "mangekyou", true),

            new ClanSkill("kamui_intangibility", "Kamui: Intangibility", "Renders the body intangible, allowing attacks and physical matter to pass through.", 200.0f, 1000.0f, 0, 90, 0, "mangekyou", true),

            new ClanSkill("kotoamatsukami_tether", "Kotoamatsukami: Optical Tether", "Binds target with invisible chakra tether that forces proximity.", 120.0f, 500.0f, 0, 70, 90, "mangekyou", true),

            new ClanSkill("kotoamatsukami_blindspot", "Kotoamatsukami: Sensory Blindspot", "Alters target perception, making you completely invisible to them.", 130.0f, 550.0f, 0, 70, 95, "mangekyou", true)
    )),

    HYUGA("Hyuga", "Specialists in Taijutsu and pressure point control.", 20.0f, 10.0f, 1.1f, List.of(

            new ClanSkill("byakugan", "Byakugan", "Provides 360-degree vision and perception of the chakra pathway system.", 15.0f, 50.0f, 0, 0, 0, null, false),

            new ClanSkill("juken", "Jūken", "Direct strikes to tenketsu points to block the target's chakra.", 20.0f, 60.0f, 45, 15, 15, "byakugan", false),

            new ClanSkill("kaiten", "Eight Trigrams Palms Revolving Heaven", "Absolute rotating defense that expels chakra from all pores.", 40.0f, 100.0f, 60, 30, 20, "byakugan", false)
    )),

    UZUMAKI("Uzumaki", "Possess formidable vitality and immense Chakra reserves.", 50.0f, 50.0f, 1.0f, List.of(
            new ClanSkill("kongo_fusa", "Adamantine Sealing Chains", "Chakra chains that bind, suppress, and drain the target.", 35.0f, 150.0f, 20, 40, 30, null, true)
    )),

    SENJU("Senju", "Blessed body with high endurance and physical vigor.", 40.0f, 20.0f, 1.1f, List.of(
            new ClanSkill("wood_release", "Wood Release", "Manipulation of trees and wood elements.", 50.0f, 180.0f, 40, 50, 20, null, true)
    ));

    private final String displayName;
    private final String description;
    private final float bonusHealth;
    private final float bonusChakra;
    private final float chakraControlMultiplier;
    private final List<ClanSkill> skills;

    ClanType(String displayName, String description, float bonusHealth, float bonusChakra, float chakraControlMultiplier, List<ClanSkill> skills) {
        this.displayName = displayName;
        this.description = description;
        this.bonusHealth = bonusHealth;
        this.bonusChakra = bonusChakra;
        this.chakraControlMultiplier = chakraControlMultiplier;
        this.skills = skills;
    }

    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }
    public float getBonusHealth() { return bonusHealth; }
    public float getBonusChakra() { return bonusChakra; }
    public float getChakraControlMultiplier() { return chakraControlMultiplier; }
    public List<ClanSkill> getSkills() { return skills; }

    public static ClanType fromName(String name) {
        if (name == null) return NONE;
        for (ClanType c : ClanType.values()) {
            if (c.name().equalsIgnoreCase(name) || c.getDisplayName().equalsIgnoreCase(name)) {
                return c;
            }
        }
        return NONE;
    }

    public static class ClanSkill {
        private final String id;
        private final String name;
        private final String description;
        private final float chakraCost;

        private final float requiredMaxChakra;
        private final int requiredTaijutsu;
        private final int requiredNinjutsu;
        private final int requiredGenjutsu;
        private final String requiredSkillId;
        private final boolean equippable;

        public ClanSkill(String id, String name, String description, float chakraCost,
                         float requiredMaxChakra, int requiredTaijutsu, int requiredNinjutsu,
                         int requiredGenjutsu, String requiredSkillId, boolean equippable) {
            this.id = id;
            this.name = name;
            this.description = description;
            this.chakraCost = chakraCost;
            this.requiredMaxChakra = requiredMaxChakra;
            this.requiredTaijutsu = requiredTaijutsu;
            this.requiredNinjutsu = requiredNinjutsu;
            this.requiredGenjutsu = requiredGenjutsu;
            this.requiredSkillId = requiredSkillId;
            this.equippable = equippable;
        }

        public String getId() { return id; }
        public String getName() { return name; }
        public String getDescription() { return description; }
        public float getChakraCost() { return chakraCost; }
        public float getRequiredMaxChakra() { return requiredMaxChakra; }
        public int getRequiredTaijutsu() { return requiredTaijutsu; }
        public int getRequiredNinjutsu() { return requiredNinjutsu; }
        public int getRequiredGenjutsu() { return requiredGenjutsu; }
        public String getRequiredSkillId() { return requiredSkillId; }

        public boolean isEquippable() { return equippable; }

        public boolean hasRequirement() {
            return requiredSkillId != null && !requiredSkillId.isEmpty();
        }
    }
}