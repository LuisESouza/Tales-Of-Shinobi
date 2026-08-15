package com.cachorrovascaino.plugin.Data.Jutsus;

public class JutsuSkillData {
    private String jutsuId;
    private int level;
    private float currentXp;

    public JutsuSkillData() {}

    public JutsuSkillData(String jutsuId) {
        this.jutsuId = jutsuId;
        this.level = 1;
        this.currentXp = 0.0f;
    }

    public float getXpToNextLevel() {
        return this.level * 100.0f;
    }

    /**
     * Adiciona XP ao Jutsu.
     * @return true se o Jutsu subiu de nível.
     */
    public boolean addXp(float amount) {
        this.currentXp += amount;
        boolean leveledUp = false;

        while (this.currentXp >= getXpToNextLevel()) {
            this.currentXp -= getXpToNextLevel();
            this.level++;
            leveledUp = true;
        }

        return leveledUp;
    }

    public String getJutsuId() { return jutsuId; }
    public int getLevel() { return level; }
    public void setLevel(int level) { this.level = level; }
    public float getCurrentXp() { return currentXp; }
    public void setCurrentXp(float currentXp) { this.currentXp = currentXp; }
}