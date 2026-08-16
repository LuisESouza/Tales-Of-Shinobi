package com.cachorrovascaino.plugin.Data.Clan;

import java.util.List;

public enum ClanType {
    NONE("Nenhum", "Sem clã definido.", 0.0f, 0.0f, 1.0f, List.of()),

    UCHIHA("Uchiha", "Mestres do Katon com excelente controle de Chakra e Dōjutsu evolutivo.", 0.0f, 25.0f, 1.2f, List.of(
            new ClanSkill("sharingan_1", "Sharingan (1 Tomoe)", "Melhora percepção e esquiva básica.", 20.0f, 140.0f, 15, 25, 10,  null, false),

            new ClanSkill("sharingan_2", "Sharingan (2 Tomoes)", "Permite antecipar movimentos e copiar técnicas simples.", 20.0f, 200.0f, 30, 45, 20,  "sharingan_1", false),

            new ClanSkill("sharingan_3", "Sharingan (3 Tomoes)", "Percepção máxima e domínio completo do dōjutsu base.", 20.0f, 320.0f, 40, 55, 30, "sharingan_2", false),

            new ClanSkill("mangekyou", "Mangekyō Sharingan", "Desperta os poderes ocluares supremos do clã.", 20.0f, 420.0f, 60, 75, 50,  "sharingan_3", true),

            new ClanSkill("susanoo", "Susanoo", "Invocação da armadura humanoide de chakra supremo.", 10.0f, 0.0f, 0, 0, 0,  "mangekyou", true)
    )),

    HYUGA("Hyūga", "Especialistas em Taijutsu e controle de pontos de pressão.", 20.0f, 10.0f, 1.1f, List.of(

            new ClanSkill("byakugan", "Byakugan", "Visão de 360 graus e leitura do sistema de chakra.", 15.0f, 50.0f, 0, 0, 0, null, false),

            new ClanSkill("juken", "Jūken", "Golpes diretos aos tenketsus para bloquear chakra.", 20.0f, 60.0f, 45, 15, 15,"byakugan", false),

            new ClanSkill("kaiten", "Hakkeshō Kaiten", "Defesa absoluta giratória expelindo chakra.", 40.0f, 100.0f, 60, 30, 20, "byakugan", false)
    )),

    UZUMAKI("Uzumaki", "Possuem vitalidade formidável e reservas imensas de Chakra.", 50.0f, 50.0f, 1.0f, List.of(
            new ClanSkill("kongo_fusa", "Correntes de Selamento", "Correntes de chakra que restringem e drenam o alvo.", 35.0f, 150.0f, 20, 40, 30,  null, true)
    )),

    SENJU("Senju", "Corpo abençoado com alta resistência e vigor físico.", 40.0f, 20.0f, 1.1f, List.of(
            new ClanSkill("wood_release", "Mokuton", "Manipulação de árvores e elementos de madeira.", 50.0f, 180.0f, 40, 50, 20, null, true)
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