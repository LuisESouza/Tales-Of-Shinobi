package com.cachorrovascaino.plugin.Ui.Page.renderers;

import com.cachorrovascaino.plugin.Data.Clan.ClanType;
import com.cachorrovascaino.plugin.Data.MangekyouType;
import com.cachorrovascaino.plugin.Data.PlayerData;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;

import java.util.Arrays;
import java.util.List;

public class ClanTabRenderer implements SkillTabRenderer {

    public static final String CLAN_CARD_TEMPLATE = "Shinobi/Components/skills/ClanSkill.ui";
    public static final String CLAN_NINJUTSU_TEMPLATE = "Shinobi/Components/skills/Clan/ClanNinjutsus.ui";

    @Override
    public int getTotalItems(PlayerData data) {
        boolean hasClan = data.getClan() != null && !data.getClan().equalsIgnoreCase("Nenhum") && !data.getClan().equalsIgnoreCase("None");
        if (!hasClan) {
            return (int) Arrays.stream(ClanType.values()).filter(c -> c != ClanType.NONE).count();
        }

        ClanType playerClan = ClanType.fromName(data.getClan());
        List<ClanType.ClanSkill> skills = getFilteredSkills(data, playerClan);
        return skills != null ? skills.size() : 0;
    }

    @Override
    public void render(UICommandBuilder cmd, UIEventBuilder evt, PlayerData data, int currentPage, int itemsPerPage) {
        cmd.clear("#SkillsContent");
        boolean hasClan = data.getClan() != null && !data.getClan().equalsIgnoreCase("Nenhum") && !data.getClan().equalsIgnoreCase("None");

        if (!hasClan) {
            renderClanSelection(cmd, evt, currentPage, itemsPerPage);
        } else {
            renderClanSkills(cmd, evt, data, currentPage, itemsPerPage);
        }
    }

    private void renderClanSelection(UICommandBuilder cmd, UIEventBuilder evt, int currentPage, int itemsPerPage) {
        List<ClanType> allClans = Arrays.stream(ClanType.values()).filter(c -> c != ClanType.NONE).toList();
        int start = currentPage * itemsPerPage;
        int end = Math.min(start + itemsPerPage, allClans.size());

        int renderIndex = 0;
        for (ClanType clan : allClans.subList(start, end)) {
            cmd.append("#SkillsContent", CLAN_CARD_TEMPLATE);
            String basePath = "#SkillsContent[" + renderIndex + "]";

            cmd.set(basePath + " #SelectClanView.Visible", true);
            cmd.set(basePath + " #LabelClanName.Text", clan.getDisplayName());
            cmd.set(basePath + " #LabelClanDescription.Text", clan.getDescription() != null ? clan.getDescription() : "");
            cmd.set(basePath + " #LabelClanHpBonus.Text", "Vida: +" + (int) clan.getBonusHealth());
            cmd.set(basePath + " #LabelClanChakraBonus.Text", "Chakra: +" + (int) clan.getBonusChakra());
            cmd.set(basePath + " #LabelClanCtrlBonus.Text", "Controle: " + clan.getChakraControlMultiplier() + "x");
            cmd.set(basePath + " #BtnSelectClan.Text", "ENTRAR NO CLÃ");

            evt.addEventBinding(CustomUIEventBindingType.Activating, basePath + " #BtnSelectClan",
                    new EventData().append("Action", "SelectClan").append("Slot", clan.name()), false);
            renderIndex++;
        }
    }

    private void renderClanSkills(UICommandBuilder cmd, UIEventBuilder evt, PlayerData data, int currentPage, int itemsPerPage) {
        ClanType playerClan = ClanType.fromName(data.getClan());
        List<ClanType.ClanSkill> filteredSkills = getFilteredSkills(data, playerClan);

        if (filteredSkills == null || filteredSkills.isEmpty()) return;

        int start = currentPage * itemsPerPage;
        int end = Math.min(start + itemsPerPage, filteredSkills.size());

        int renderIndex = 0;
        for (ClanType.ClanSkill skill : filteredSkills.subList(start, end)) {
            cmd.append("#SkillsContent", CLAN_NINJUTSU_TEMPLATE);
            String skillPath = "#SkillsContent[" + renderIndex + "]";

            boolean isUnlocked = data.hasUnlockClanSkill(skill.getId());
            boolean isEquipped = skill.isEquippable() && data.getEquippedClanHotbar() != null && data.getEquippedClanHotbar().containsValue(skill.getId());
            boolean canUnlock = data.canUnlockClanSkill(skill);

            cmd.set(skillPath + " #LabelJutsuName.Text", skill.getName());
            cmd.set(skillPath + " #LabelJutsuType.Text", "[" + playerClan.getDisplayName() + "]");
            cmd.set(skillPath + " #LabelJutsuCost.Text", "Chakra: " + (int) skill.getChakraCost());

            cmd.set(skillPath + " #LabelJutsuReq1.Text", "Ninj: " + skill.getRequiredNinjutsu());
            cmd.set(skillPath + " #LabelJutsuReq2.Text", "Taij: " + skill.getRequiredTaijutsu());
            cmd.set(skillPath + " #LabelJutsuReq3.Text", "Genj: " + skill.getRequiredGenjutsu());
            cmd.set(skillPath + " #LabelJutsuReq4.Text", "Skill: " + (skill.getRequiredSkillId() != null ? skill.getRequiredSkillId() : "Nenhuma"));
            cmd.set(skillPath + " #LabelJutsuReq5.Text", "Chakra Max: " + (int) skill.getRequiredMaxChakra());

            String actionText = !isUnlocked ? (!canUnlock ? "BLOCKED" : "LEARN") : (skill.isEquippable() ? (isEquipped ? "UNEQUIP" : "EQUIP") : "LEARNED");
            cmd.set(skillPath + " #BtnJutsuAction.Text", actionText);

            evt.addEventBinding(CustomUIEventBindingType.Activating, skillPath + " #BtnJutsuAction",
                    new EventData().append("Action", "ToggleClanSkill").append("Slot", skill.getId()), false);

            renderIndex++;
        }
    }

    private List<ClanType.ClanSkill> getFilteredSkills(PlayerData data, ClanType clan) {
        if (clan == null || clan.getSkills() == null) return List.of();
        MangekyouType playerMangekyou = MangekyouType.fromName(data.getMangekyouType());

        return clan.getSkills().stream().filter(skill -> {
            String id = skill.getId().toLowerCase();
            if (id.startsWith("kamui_")) return playerMangekyou == MangekyouType.OBITO;
            if (id.startsWith("kotoamatsukami_")) return playerMangekyou == MangekyouType.SHISUI;
            return true;
        }).toList();
    }
}