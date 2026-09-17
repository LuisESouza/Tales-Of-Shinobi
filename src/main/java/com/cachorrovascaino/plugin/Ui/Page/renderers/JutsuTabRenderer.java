package com.cachorrovascaino.plugin.Ui.Page.renderers;

import com.cachorrovascaino.plugin.Abstractions.SkillType;
import com.cachorrovascaino.plugin.Data.Jutsus.ElementType;
import com.cachorrovascaino.plugin.Data.Jutsus.JutsuType;
import com.cachorrovascaino.plugin.Data.PlayerData;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;

import java.util.Arrays;
import java.util.List;

public class JutsuTabRenderer implements SkillTabRenderer {

    private final SkillType skillType;
    private final String cardTemplate;

    public JutsuTabRenderer(SkillType skillType, String cardTemplate) {
        this.skillType = skillType;
        this.cardTemplate = cardTemplate;
    }

    private List<JutsuType> getFilteredJutsus(ElementType element) {
        return Arrays.stream(JutsuType.values())
                .filter(j -> j.getType() == skillType)
                .filter(j -> element == null || j.getElement() == element) // Se element for null, mostra todos. Se for NONE, mostra só os sem elemento.
                .toList();
    }

    public int getTotalItems(PlayerData data, ElementType element) {
        return getFilteredJutsus(element).size();
    }

    @Override
    public int getTotalItems(PlayerData data) {
        return getTotalItems(data, null);
    }

    public void render(UICommandBuilder cmd, UIEventBuilder evt, PlayerData data, int currentPage, int itemsPerPage, ElementType selectedElement) {
        cmd.clear("#SkillsContent");

        List<JutsuType> jutsus = getFilteredJutsus(selectedElement);

        int start = currentPage * itemsPerPage;
        int end = Math.min(start + itemsPerPage, jutsus.size());

        SkillSelectors selectors = getSelectorsForSkill(skillType);

        int renderIndex = 0;
        for (JutsuType jutsu : jutsus.subList(start, end)) {
            cmd.append("#SkillsContent", cardTemplate);
            String basePath = "#SkillsContent[" + renderIndex + "]";

            boolean isUnlocked = data.hasJutsuUnlocked(jutsu.getId());
            boolean isEquipped = data.getEquippedHotbar() != null && data.getEquippedHotbar().containsValue(jutsu.getId());

            int jutsuLevel = data.getJutsuLevel(jutsu.getId());
            float currentXp = data.getJutsuXp(jutsu.getId());
            float requiredXp = jutsuLevel * 50.0f;

            cmd.set(basePath + " " + selectors.nameLabel + ".Text", jutsu.getName());
            cmd.set(basePath + " " + selectors.levelLabel + ".Text", "[Lv. " + jutsuLevel + "]");

            String resourcePrefix = skillType == SkillType.TAIJUTSU ? "Stamina: " : "Chakra: ";
            cmd.set(basePath + " " + selectors.costLabel + ".Text", resourcePrefix + (int) jutsu.getResourceCost());

            cmd.set(basePath + " " + selectors.xpLabel + ".Text", "XP: " + (int) currentXp + "/" + (int) requiredXp);

            int reqValue = switch (skillType) {
                case TAIJUTSU -> jutsu.getReqTaijutsu();
                case GENJUTSU -> jutsu.getReqGenjutsu();
                default -> jutsu.getReqNinjutsu();
            };
            cmd.set(basePath + " " + selectors.reqLabel + ".Text", selectors.reqPrefix + reqValue);

            boolean canUnlock = canUnlockJutsu(data, jutsu);
            String actionText = !isUnlocked ? (!canUnlock ? "BLOCKED" : "LEARN") : (isEquipped ? "UNEQUIP" : "EQUIP");

            cmd.set(basePath + " " + selectors.actionBtn + ".Text", actionText);

            evt.addEventBinding(
                    CustomUIEventBindingType.Activating,
                    basePath + " " + selectors.actionBtn,
                    new EventData().append("Action", "ToggleJutsu").append("Slot", jutsu.getId()),
                    false
            );

            renderIndex++;
        }
    }

    @Override
    public void render(UICommandBuilder cmd, UIEventBuilder evt, PlayerData data, int currentPage, int itemsPerPage) {
        render(cmd, evt, data, currentPage, itemsPerPage, null);
    }

    private boolean canUnlockJutsu(PlayerData data, JutsuType jutsu) {
        return data.getTaijutsu() >= jutsu.getReqTaijutsu()
                && data.getGenjutsu() >= jutsu.getReqGenjutsu()
                && data.getNinjutsu() >= jutsu.getReqNinjutsu()
                && data.getSpeed() >= jutsu.getReqSpeed()
                && data.getAvailablePoints() >= jutsu.getRequiredPoints();
    }

    private SkillSelectors getSelectorsForSkill(SkillType type) {
        return switch (type) {
            case TAIJUTSU -> new SkillSelectors(
                    "#LabelTaijutsuName", "#LabelTaijutsuLevel", "#LabelTaijutsuCost",
                    "#LabelTaijutsuXp", "#LabelTaijutsuReq", "#BtnTaijutsuAction", "Taij: "
            );
            case GENJUTSU -> new SkillSelectors(
                    "#LabelGenjutsuName", "#LabelGenjutsuLevel", "#LabelGenjutsuCost",
                    "#LabelGenjutsuXp", "#LabelGenjutsuReq", "#BtnGenjutsuAction", "Genj: "
            );
            default -> new SkillSelectors(
                    "#LabelJutsuName", "#LabelJutsuLevel", "#LabelJutsuCost",
                    "#LabelJutsuXp", "#LabelJutsuReq", "#BtnJutsuAction", "Ninj: "
            );
        };
    }

    private record SkillSelectors(
            String nameLabel, String levelLabel, String costLabel,
            String xpLabel, String reqLabel, String actionBtn, String reqPrefix
    ) {}
}