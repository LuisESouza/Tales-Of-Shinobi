package com.cachorrovascaino.plugin.Data;

import com.cachorrovascaino.plugin.Data.Clan.ClanType;
import org.joml.Vector3d;

import java.util.*;

public class PlayerData {

    // --- ESTADOS DO DŌJUTSU ---
    public enum DojutsuState {
        NONE,
        SHARINGAN,
        MANGEKYO,
        BYAKUGAN
    }

    private static final int MAX_HIRAISHIN_MARKS = 5;
    private final LinkedList<Vector3d> hiraishinMarks = new LinkedList<>();

    /**
     * Adiciona uma nova marca ao Hiraishin.
     * Mantém sempre no máximo 5 posições ativas.
     * Ao adicionar a 6ª, a 1ª (mais antiga) é removida para dar lugar à nova.
     */
    public synchronized void addHiraishinMark(Vector3d newMark) {
        if (hiraishinMarks.size() >= MAX_HIRAISHIN_MARKS) {
            hiraishinMarks.removeFirst();
        }
        hiraishinMarks.addLast(newMark);
    }

    public synchronized List<Vector3d> getHiraishinMarks() {
        return new ArrayList<>(hiraishinMarks);
    }

    public synchronized Vector3d getLatestHiraishinMark() {
        return hiraishinMarks.peekLast();
    }

    public synchronized boolean removeHiraishinMark(Vector3d mark) {
        return hiraishinMarks.remove(mark);
    }

    public synchronized void clearHiraishinMarks() {
        hiraishinMarks.clear();
    }

    private String name;
    private String uuid;
    private String village;
    private String clan;
    private String ninjaRank;
    private String element;

    // --- COSMÉTICOS & APARÊNCIA VISUAL ---
    private String hairId = "";
    private String hairColor = "";
    private String eyesId = "";
    private String eyesColor = "";
    private String originalEyesId = "";
    private String originalEyesColor = "";

    // --- DŌJUTSU / TRANSFORMAÇÕES VISUAIS DE OLHOS ---
    private DojutsuState activeDojutsu = DojutsuState.NONE;
    private int eyeStage = 1;
    private String eyeDojutsuType = "NONE";
    private String mangekyouType = null;

    // --- PROGRESSÃO GERAL DO JOGADOR ---
    private int currentLevel;
    private int maxLevel;
    private float currentXp;
    private float xpUp;
    private int availablePoints;

    // --- RECURSOS DINÂMICOS (Atual / Máximo) ---
    private float currentHealth;
    private float maxHealth;

    private float currentChakra;
    private float maxChakra;
    private float chakraControl;

    private float currentStamina;
    private float maxStamina;

    private int speed;

    // --- ATRIBUTOS SHINOBI ---
    private int taijutsu;
    private int ninjutsu;
    private int genjutsu;
    private int kekkeiGenkai;

    // --- PROGRESSÃO INDIVIDUAL DOS JUTSUS ---
    private Map<String, Integer> jutsuLevels = new HashMap<>();
    private Map<String, Float> jutsuXp = new HashMap<>();

    // --- JUTSUS DESBLOQUEADOS E HOTBAR ---
    private List<String> unlockedJutsus = new ArrayList<>();
    Map<String, String> equippedHotbar = new HashMap<>();

    // --- CLAN JUTSUS ---
    private List<String> unlockedClanJutsu = new ArrayList<>();
    private Map<String, String> equippedClanHotbar = new HashMap<>();

    public PlayerData() {}

    public PlayerData(String name, String uuid) {
        this.name = name;
        this.uuid = uuid;
        this.village = "Konoha";
        this.clan = "None";
        this.ninjaRank = "Estudante";
        this.element = "Ray";

        // Dōjutsu Base & Mangekyō
        this.eyeDojutsuType = "NONE";
        this.mangekyouType = null;

        // Progressão Geral
        this.currentLevel = 1;
        this.maxLevel = 100;
        this.currentXp = 0.0f;
        this.xpUp = 100.0f;
        this.availablePoints = 5;

        // Status Base
        this.maxHealth = 100.0f;
        this.currentHealth = 100.0f;

        this.maxChakra = 100.0f;
        this.currentChakra = 100.0f;
        this.chakraControl = 1.0f;

        this.maxStamina = 100.0f;
        this.currentStamina = 100.0f;

        this.speed = 10;

        // Atributos Shinobi
        this.taijutsu = 10;
        this.ninjutsu = 10;
        this.genjutsu = 1;
        this.kekkeiGenkai = 0;

        // Coleções
        this.jutsuLevels = new HashMap<>();
        this.jutsuXp = new HashMap<>();
        this.unlockedJutsus = new ArrayList<>();
        this.equippedHotbar = new HashMap<>();

        this.unlockedClanJutsu = new ArrayList<>();
        this.equippedClanHotbar = new HashMap<>();
    }

    // --- PROGRESSÃO DE XP E PONTOS ---
    public boolean addXp(float amount) {
        if (currentLevel >= maxLevel) return false;

        this.currentXp += amount;
        boolean leveledUp = false;

        while (this.currentXp >= this.xpUp && this.currentLevel < this.maxLevel) {
            this.currentXp -= this.xpUp;
            this.currentLevel++;
            addPoint(6);
            this.xpUp = (float) Math.floor(this.xpUp * 1.5f);
            leveledUp = true;
        }

        return leveledUp;
    }

    public void addPoint(int point) {
        this.availablePoints += point;
    }

    public boolean addJutsuXp(String jutsuId, float amount) {
        int currentLvl = getJutsuLevel(jutsuId);
        float currentXpVal = getJutsuXp(jutsuId);

        currentXpVal += amount;
        boolean leveledUp = false;

        while (true) {
            float requiredXp = currentLvl * 50.0f;
            if (currentXpVal >= requiredXp) {
                currentXpVal -= requiredXp;
                currentLvl++;
                leveledUp = true;
            } else {
                break;
            }
        }

        if (leveledUp) {
            if (jutsuLevels == null) jutsuLevels = new HashMap<>();
            jutsuLevels.put(jutsuId, currentLvl);
        }
        if (jutsuXp == null) jutsuXp = new HashMap<>();
        jutsuXp.put(jutsuId, currentXpVal);

        return leveledUp;
    }

    public int getJutsuLevel(String jutsuId) {
        if (jutsuLevels == null) return 1;
        return jutsuLevels.getOrDefault(jutsuId, 1);
    }

    public float getJutsuXp(String jutsuId) {
        if (jutsuXp == null) return 0.0f;
        return jutsuXp.getOrDefault(jutsuId, 0.0f);
    }

    // UNLOCKED
    public boolean hasUnlockClanSkill(String jutsuClanId) {
        if (unlockedClanJutsu == null) return false;
        return unlockedClanJutsu.contains(jutsuClanId);
    }

    public boolean hasJutsuUnlocked(String jutsuId) {
        if (unlockedJutsus == null) return false;
        return unlockedJutsus.contains(jutsuId);
    }

    // --- GETTERS & SETTERS DE APARÊNCIA ---
    public String getHairId() { return hairId; }
    public void setHairId(String hairId) { this.hairId = hairId; }

    public String getHairColor() { return hairColor; }
    public void setHairColor(String hairColor) { this.hairColor = hairColor; }

    public String getEyesId() { return eyesId; }
    public void setEyesId(String eyesId) { this.eyesId = eyesId; }

    public String getEyesColor() { return eyesColor; }
    public void setEyesColor(String eyesColor) { this.eyesColor = eyesColor; }

    public String getOriginalEyesId() { return originalEyesId; }
    public void setOriginalEyesId(String originalEyesId) { this.originalEyesId = originalEyesId; }

    public String getOriginalEyesColor() { return originalEyesColor; }
    public void setOriginalEyesColor(String originalEyesColor) { this.originalEyesColor = originalEyesColor; }

    // --- DŌJUTSU GETTERS E SETTERS ---
    public DojutsuState getActiveDojutsu() { return activeDojutsu; }
    public void setActiveDojutsu(DojutsuState activeDojutsu) { this.activeDojutsu = activeDojutsu; }

    public int getEyeStage() { return eyeStage; }
    public void setEyeStage(int eyeStage) { this.eyeStage = eyeStage; }

    public String getEyeDojutsuType() { return eyeDojutsuType != null ? eyeDojutsuType : "NONE"; }
    public void setEyeDojutsuType(String eyeDojutsuType) { this.eyeDojutsuType = eyeDojutsuType; }

    public String getMangekyouType() { return mangekyouType; }
    public void setMangekyouType(String mangekyouType) { this.mangekyouType = mangekyouType; }

    // --- GETTERS E SETTERS GERAIS ---
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getUuid() { return uuid; }
    public void setUuid(String uuid) { this.uuid = uuid; }

    public String getVillage() { return village; }
    public void setVillage(String village) { this.village = village; }

    public String getClan() { return clan; }
    public void setClan(String clan) { this.clan = clan; }

    public String getNinjaRank() { return ninjaRank; }
    public void setNinjaRank(String ninjaRank) { this.ninjaRank = ninjaRank; }

    public String getElement() { return element; }
    public void setElement(String element) { this.element = element; }

    public int getCurrentLevel() { return currentLevel; }
    public void setCurrentLevel(int currentLevel) { this.currentLevel = currentLevel; }

    public int getMaxLevel() { return maxLevel; }
    public void setMaxLevel(int maxLevel) { this.maxLevel = maxLevel; }

    public float getCurrentXp() { return currentXp; }
    public void setCurrentXp(float currentXp) { this.currentXp = currentXp; }

    public float getXpUp() { return xpUp; }
    public void setXpUp(float xpUp) { this.xpUp = xpUp; }

    public int getAvailablePoints() { return availablePoints; }
    public void setAvailablePoints(int availablePoints) { this.availablePoints = availablePoints; }

    public float getCurrentHealth() { return currentHealth; }
    public void setCurrentHealth(float currentHealth) { this.currentHealth = currentHealth; }
    public float getMaxHealth() { return maxHealth; }
    public void setMaxHealth(float maxHealth) { this.maxHealth = maxHealth; }

    public float getCurrentChakra() { return currentChakra; }
    public void setCurrentChakra(float currentChakra) { this.currentChakra = currentChakra; }
    public float getMaxChakra() { return maxChakra; }
    public void setMaxChakra(float maxChakra) { this.maxChakra = maxChakra; }
    public float getChakraControl() { return chakraControl; }
    public void setChakraControl(float chakraControl) { this.chakraControl = chakraControl; }

    public float getCurrentStamina() { return currentStamina; }
    public void setCurrentStamina(float currentStamina) { this.currentStamina = currentStamina; }
    public float getMaxStamina() { return maxStamina; }
    public void setMaxStamina(float maxStamina) { this.maxStamina = maxStamina; }

    public int getSpeed() { return speed; }
    public void setSpeed(int speed) { this.speed = speed; }

    public int getTaijutsu() { return taijutsu; }
    public void setTaijutsu(int taijutsu) { this.taijutsu = taijutsu; }

    public int getNinjutsu() { return ninjutsu; }
    public void setNinjutsu(int ninjutsu) { this.ninjutsu = ninjutsu; }

    public int getGenjutsu() { return genjutsu; }
    public void setGenjutsu(int genjutsu) { this.genjutsu = genjutsu; }

    public int getKekkeiGenkai() { return kekkeiGenkai; }
    public void setKekkeiGenkai(int kekkeiGenkai) { this.kekkeiGenkai = kekkeiGenkai; }

    // --- JUTSUS & HOTBAR ---
    public Map<String, Integer> getJutsuLevels() {
        if (jutsuLevels == null) jutsuLevels = new HashMap<>();
        return jutsuLevels;
    }
    public void setJutsuLevels(Map<String, Integer> jutsuLevels) { this.jutsuLevels = jutsuLevels; }

    public Map<String, Float> getJutsuXp() {
        if (jutsuXp == null) jutsuXp = new HashMap<>();
        return jutsuXp;
    }
    public void setJutsuXp(Map<String, Float> jutsuXp) { this.jutsuXp = jutsuXp; }

    public List<String> getUnlockedJutsus() {
        if (unlockedJutsus == null) unlockedJutsus = new ArrayList<>();
        return unlockedJutsus;
    }
    public void setUnlockedJutsus(List<String> unlockedJutsus) { this.unlockedJutsus = unlockedJutsus; }

    public Map<String, String> getEquippedHotbar() {
        if (equippedHotbar == null) equippedHotbar = new HashMap<>();
        return equippedHotbar;
    }
    public void setEquippedHotbar(Map<String, String> equippedHotbar) { this.equippedHotbar = equippedHotbar; }

    // --- CLAN ---
    public List<String> getUnlockedClanJutsu() {
        if (unlockedClanJutsu == null) unlockedClanJutsu = new ArrayList<>();
        return unlockedClanJutsu;
    }

    public void setUnlockedClanJutsu(List<String> unlockedClanJutsu) {
        this.unlockedClanJutsu = unlockedClanJutsu;
    }

    public Map<String, String> getEquippedClanHotbar() {
        if (equippedClanHotbar == null) equippedClanHotbar = new HashMap<>();
        return equippedClanHotbar;
    }

    public void setEquippedClanHotbar(Map<String, String> equippedClanHotbar) {
        this.equippedClanHotbar = equippedClanHotbar;
    }

    public boolean canUnlockClanSkill(ClanType.ClanSkill skill) {
        if (skill == null) return false;

        if (this.maxChakra < skill.getRequiredMaxChakra()) return false;
        if (this.taijutsu < skill.getRequiredTaijutsu()) return false;
        if (this.ninjutsu < skill.getRequiredNinjutsu()) return false;
        if (this.genjutsu < skill.getRequiredGenjutsu()) return false;
        if (skill.hasRequirement()) {
            return hasUnlockClanSkill(skill.getRequiredSkillId());
        }

        return true;
    }
}