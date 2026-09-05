package com.cachorrovascaino.plugin.Ui.Page;

import com.cachorrovascaino.plugin.Abstractions.SkillType;
import com.cachorrovascaino.plugin.Data.Clan.ClanType;
import com.cachorrovascaino.plugin.Data.Jutsus.JutsuType;
import com.cachorrovascaino.plugin.Data.MangekyouType;
import com.cachorrovascaino.plugin.Data.PlayerData;
import com.cachorrovascaino.plugin.Main;
import com.cachorrovascaino.plugin.Manager.PlayerDataManager;
import com.cachorrovascaino.plugin.Ui.Hud.JutsuEquippedHud;
import com.cachorrovascaino.plugin.Ui.Page.utils.NavigationButtons;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import java.util.*;

public class SkillsPage extends InteractiveCustomUIPage<SkillsPage.UIEventData> {

    private final PlayerRef playerRef;
    private final UUID uuid;
    private final PlayerDataManager dataManager;

    private String currentTab = "Ninjutsu";

    private int currentPage = 0;
    private static final int ITEMS_PER_PAGE = 2;

    private static final String INTERFACE_MAIN = "Shinobi/Menus/SkillsMenu.ui";
    public static final String NINJUTSU_CARD_TEMPLATE = "Shinobi/Components/skills/NinjutsuSkill.ui";
    public static final String TAIJUTSU_CARD_TEMPLATE = "Shinobi/Components/skills/TaijutsuSkill.ui";
    public static final String CLAN_CARD_TEMPLATE = "Shinobi/Components/skills/ClanSkill.ui";
    public static final String GENJUTSU_CARD_TEMPLATE = "Shinobi/Components/skills/GenjutsuSkill.ui";
    public static final String ClAN_NINJUTSU_TEMPLATE = "Shinobi/Components/skills/Clan/ClanNinjutsus.ui";

    private static final Random RANDOM = new Random();

    private static final List<MangekyouType> MANGEKYOU_POOL = List.of(
            MangekyouType.OBITO,
            MangekyouType.SHISUI
    );

    private static final Map<String, String> SKILLS_UI_MAP = Map.of(
            "Ninjutsu", "Shinobi/Components/skills/NinjutsuSkill.ui",
            "Taijutsu", "Shinobi/Components/skills/TaijutsuSkill.ui",
            "Genjutsu", "Shinobi/Components/skills/GenjutsuSkill.ui",
            "Clan",     "Shinobi/Components/skills/ClanSkill.ui"
    );

    public SkillsPage(@Nonnull PlayerRef playerRef, @Nonnull PlayerDataManager dataManager) {
        super(playerRef, CustomPageLifetime.CanDismissOrCloseThroughInteraction, SkillsPage.UIEventData.CODEC);
        this.playerRef = playerRef;
        this.uuid = playerRef.getUuid();
        this.dataManager = dataManager;
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder cmd, @Nonnull UIEventBuilder evt, @Nonnull Store<EntityStore> store) {
        cmd.append(INTERFACE_MAIN);
        cmd.append("#NavigationContent", "Shinobi/Components/navigation/NavigationMenus.ui");

        String activeUiPath = SKILLS_UI_MAP.get(currentTab);
        if (activeUiPath != null) {
            cmd.append("#SkillsContent", activeUiPath);
        }

        buildComponents(cmd, evt, store, ref);
    }

    public void buildComponents(@Nonnull UICommandBuilder cmd, @Nonnull UIEventBuilder evt, @Nonnull Store<EntityStore> store, @Nonnull Ref<EntityStore> ref) {
        String username = playerRef.getUsername();

        var player = Universe.get().getPlayer(uuid);
        if (player != null) { username = player.getUsername(); }

        PlayerData data = dataManager.getPlayerData(uuid);
        if (data == null) { data = dataManager.loadPlayer(uuid, username); }
        if (data == null) { data = new PlayerData(username, uuid.toString()); }

        if (!Objects.equals(currentTab, "Clan")) { buildSlotsSkill(data, cmd); }

        buildButtons(evt);

        if ("Ninjutsu".equalsIgnoreCase(currentTab)) { buildComponentsNinjutsu(evt, cmd, data); }
        if ("Taijutsu".equalsIgnoreCase(currentTab)) { buildComponentsTaijutsu(evt, cmd, data); }
        if ("Genjutsu".equals(currentTab)) { buildComponentsGenjutsu(evt, cmd, data); }
        if ("Clan".equalsIgnoreCase(currentTab)) {
            buildComponentsClan(evt, cmd, data);
            buildSlotSkillClan(data, cmd);
        }
    }

    public void buildButtons(@Nonnull UIEventBuilder evt) {
        evt.addEventBinding(CustomUIEventBindingType.Activating, "#BtnTabTaijutsu", new EventData().append("Action", "Taijutsu"), false);
        evt.addEventBinding(CustomUIEventBindingType.Activating, "#BtnTabNinjutsu", new EventData().append("Action", "Ninjutsu"), false);
        evt.addEventBinding(CustomUIEventBindingType.Activating, "#BtnTabGenjutsu", new EventData().append("Action", "Genjutsu"), false);
        evt.addEventBinding(CustomUIEventBindingType.Activating, "#BtnTabClan", new EventData().append("Action", "Clan"), false);

        evt.addEventBinding(CustomUIEventBindingType.Activating, "#BtnPrev", new EventData().append("Action", "PrevPage"), false);
        evt.addEventBinding(CustomUIEventBindingType.Activating, "#BtnNext", new EventData().append("Action", "NextPage"), false);

        NavigationButtons.bindButtons(evt);
    }

    public void buildSlotSkillClan(@Nonnull PlayerData data, @Nonnull UICommandBuilder cmd) {
        cmd.clear("#EquippedJutsusPanel");
        cmd.append("#EquippedJutsusPanel", "Shinobi/Components/slots/ClanSlot.ui");

        Map<String, String> clanHotbar = data.getEquippedClanHotbar();

        String slot1Jutsu = formatJutsuName(data, clanHotbar != null ? clanHotbar.get("slot_1") : null);
        String slot2Jutsu = formatJutsuName(data, clanHotbar != null ? clanHotbar.get("slot_2") : null);
        String slot3Jutsu = formatJutsuName(data, clanHotbar != null ? clanHotbar.get("slot_3") : null);

        cmd.set("#LabelSlot1.Text", slot1Jutsu);
        cmd.set("#LabelSlot2.Text", slot2Jutsu);
        cmd.set("#LabelSlot3.Text", slot3Jutsu);
    }

    public void buildSlotsSkill(@Nonnull PlayerData data, @Nonnull UICommandBuilder cmd) {
        cmd.clear("#EquippedJutsusPanel");
        cmd.append("#EquippedJutsusPanel", "Shinobi/Components/slots/ComboSlot.ui");
        Map<String, String> hotbar = data.getEquippedHotbar();

        String slot1Jutsu = formatJutsuName(data, hotbar != null ? hotbar.get("slot_1") : null);
        String slot2Jutsu = formatJutsuName(data, hotbar != null ? hotbar.get("slot_2") : null);
        String slot3Jutsu = formatJutsuName(data, hotbar != null ? hotbar.get("slot_3") : null);
        String slot4Jutsu = formatJutsuName(data, hotbar != null ? hotbar.get("slot_4") : null);

        cmd.set("#LabelSlot1.Text", slot1Jutsu);
        cmd.set("#LabelSlot2.Text", slot2Jutsu);
        cmd.set("#LabelSlot3.Text", slot3Jutsu);
        cmd.set("#LabelSlot4.Text", slot4Jutsu);
    }

    public void buildComponentsClan(@Nonnull UIEventBuilder evt, @Nonnull UICommandBuilder cmd, @Nonnull PlayerData data) {
        cmd.clear("#SkillsContent");

        boolean hasClan = data.getClan() != null && !data.getClan().equalsIgnoreCase("Nenhum") && !data.getClan().equalsIgnoreCase("None");

        if (!hasClan) {
            List<ClanType> allClans = Arrays.stream(ClanType.values())
                    .filter(c -> c != ClanType.NONE)
                    .toList();

            int totalPages = Math.max(1, (int) Math.ceil((double) allClans.size() / ITEMS_PER_PAGE));
            validateCurrentPage(totalPages);

            cmd.set("#LabelPagination.Text", (currentPage + 1) + "/" + totalPages);

            int start = currentPage * ITEMS_PER_PAGE;
            int end = Math.min(start + ITEMS_PER_PAGE, allClans.size());
            List<ClanType> pageClans = allClans.subList(start, end);

            int renderIndex = 0;
            for (ClanType clan : pageClans) {
                cmd.append("#SkillsContent", CLAN_CARD_TEMPLATE);
                String basePath = "#SkillsContent[" + renderIndex + "]";

                cmd.set(basePath + " #SelectClanView.Visible", true);

                cmd.set(basePath + " #LabelClanName.Text", clan.getDisplayName());
                cmd.set(basePath + " #LabelClanDescription.Text", clan.getDescription() != null ? clan.getDescription() : "");
                cmd.set(basePath + " #LabelClanHpBonus.Text", "Vida: +" + (int) clan.getBonusHealth());
                cmd.set(basePath + " #LabelClanChakraBonus.Text", "Chakra: +" + (int) clan.getBonusChakra());
                cmd.set(basePath + " #LabelClanCtrlBonus.Text", "Controle: " + clan.getChakraControlMultiplier() + "x");

                cmd.set(basePath + " #BtnSelectClan.Text", "ENTRAR NO CLÃ");

                evt.addEventBinding(
                        CustomUIEventBindingType.Activating,
                        basePath + " #BtnSelectClan",
                        new EventData().append("Action", "SelectClan").append("Slot", clan.name()),
                        false
                );

                renderIndex++;
            }
        } else {
            ClanType playerClan = ClanType.fromName(data.getClan());
            List<ClanType.ClanSkill> allClanSkills = playerClan.getSkills();

            if (allClanSkills != null && !allClanSkills.isEmpty()) {

                MangekyouType playerMangekyou = MangekyouType.fromName(data.getMangekyouType());

                List<ClanType.ClanSkill> filteredSkills = allClanSkills.stream().filter(skill -> {
                    String id = skill.getId().toLowerCase();

                    if (id.startsWith("kamui_")) { return playerMangekyou == MangekyouType.OBITO; }
                    if (id.startsWith("kotoamatsukami_")) { return playerMangekyou == MangekyouType.SHISUI; }

                    return true;
                }).toList();

                int totalPages = Math.max(1, (int) Math.ceil((double) filteredSkills.size() / ITEMS_PER_PAGE));
                validateCurrentPage(totalPages);

                cmd.set("#LabelPagination.Text", (currentPage + 1) + "/" + totalPages);

                int start = currentPage * ITEMS_PER_PAGE;
                int end = Math.min(start + ITEMS_PER_PAGE, filteredSkills.size());
                List<ClanType.ClanSkill> pageSkills = filteredSkills.subList(start, end);

                int renderIndex = 0;
                for (ClanType.ClanSkill skill : pageSkills) {
                    cmd.append("#SkillsContent", ClAN_NINJUTSU_TEMPLATE);
                    String skillPath = "#SkillsContent[" + renderIndex + "]";

                    boolean isUnlocked = data.hasUnlockClanSkill(skill.getId());
                    boolean isEquipped = skill.isEquippable()
                            && data.getEquippedClanHotbar() != null
                            && data.getEquippedClanHotbar().containsValue(skill.getId());
                    boolean canUnlock = data.canUnlockClanSkill(skill);

                    cmd.set(skillPath + " #LabelJutsuName.Text", skill.getName());
                    cmd.set(skillPath + " #LabelJutsuType.Text", "[" + playerClan.getDisplayName() + "]");
                    cmd.set(skillPath + " #LabelJutsuCost.Text", "Chakra: " + (int) skill.getChakraCost());

                    cmd.set(skillPath + " #LabelJutsuReq1.Text", "Ninj: " + skill.getRequiredNinjutsu());
                    cmd.set(skillPath + " #LabelJutsuReq2.Text", "Taij: " + skill.getRequiredTaijutsu());
                    cmd.set(skillPath + " #LabelJutsuReq3.Text", "Genj: " + skill.getRequiredGenjutsu());
                    cmd.set(skillPath + " #LabelJutsuReq4.Text", "Skill: " + (skill.getRequiredSkillId() != null ? skill.getRequiredSkillId() : "Nenhuma"));
                    cmd.set(skillPath + " #LabelJutsuReq5.Text", "Chakra Max: " + (int) skill.getRequiredMaxChakra());

                    String actionText = "LEARN";
                    if (!isUnlocked) {
                        if (!canUnlock) {
                            actionText = "BLOCKED";
                        }
                    } else if (skill.isEquippable()) {
                        actionText = isEquipped ? "UNEQUIP" : "EQUIP";
                    } else {
                        actionText = "LEARNED";
                    }

                    cmd.set(skillPath + " #BtnJutsuAction.Text", actionText);

                    evt.addEventBinding(
                            CustomUIEventBindingType.Activating,
                            skillPath + " #BtnJutsuAction",
                            new EventData().append("Action", "ToggleClanSkill").append("Slot", skill.getId()),
                            false
                    );

                    renderIndex++;
                }
            } else {
                cmd.set("#LabelPagination.Text", "1/1");
            }
        }
    }

    public void buildComponentsNinjutsu(@Nonnull UIEventBuilder evt, @Nonnull UICommandBuilder cmd, @Nonnull PlayerData data) {
        cmd.clear("#SkillsContent");

        List<JutsuType> ninjutsus = Arrays.stream(JutsuType.values())
                .filter(j -> j.getType() == SkillType.NINJUTSU)
                .toList();

        int totalPages = Math.max(1, (int) Math.ceil((double) ninjutsus.size() / ITEMS_PER_PAGE));
        validateCurrentPage(totalPages);

        cmd.set("#LabelPagination.Text", (currentPage + 1) + "/" + totalPages);

        int start = currentPage * ITEMS_PER_PAGE;
        int end = Math.min(start + ITEMS_PER_PAGE, ninjutsus.size());
        List<JutsuType> pageJutsus = ninjutsus.subList(start, end);

        int renderIndex = 0;
        for (JutsuType jutsu : pageJutsus) {
            cmd.append("#SkillsContent", NINJUTSU_CARD_TEMPLATE);

            String basePath = "#SkillsContent[" + renderIndex + "]";

            boolean isUnlocked = data.hasJutsuUnlocked(jutsu.getId());
            boolean isEquipped = data.getEquippedHotbar() != null && data.getEquippedHotbar().containsValue(jutsu.getId());

            int jutsuLevel = data.getJutsuLevel(jutsu.getId());
            float currentXp = data.getJutsuXp(jutsu.getId());
            float requiredXp = jutsuLevel * 50.0f;

            cmd.set(basePath + " #LabelJutsuName.Text", jutsu.getName());
            cmd.set(basePath + " #LabelJutsuLevel.Text", "[Lv. " + jutsuLevel + "]");
            cmd.set(basePath + " #LabelJutsuType.Text", "[Ninjutsu]");
            cmd.set(basePath + " #LabelJutsuCost.Text", "Chakra: " + (int) jutsu.getResourceCost());
            cmd.set(basePath + " #LabelJutsuXp.Text", "XP: " + (int) currentXp + "/" + (int) requiredXp);
            cmd.set(basePath + " #LabelJutsuReq.Text", "Ninj: " + jutsu.getReqNinjutsu());

            boolean canUnlock = canUnlockJutsu(data, jutsu);

            String actionText = "LEARN";
            if (!isUnlocked) {
                if (!canUnlock) {
                    actionText = "BLOCKED";
                }
            } else {
                actionText = isEquipped ? "UNEQUIP" : "EQUIP";
            }

            cmd.set(basePath + " #BtnJutsuAction.Text", actionText);

            evt.addEventBinding(
                    CustomUIEventBindingType.Activating,
                    basePath + " #BtnJutsuAction",
                    new EventData().append("Action", "ToggleJutsu").append("Slot", jutsu.getId()),
                    false
            );

            renderIndex++;
        }
    }

    public void buildComponentsTaijutsu(@Nonnull UIEventBuilder evt, @Nonnull UICommandBuilder cmd, @Nonnull PlayerData data) {
        cmd.clear("#SkillsContent");

        List<JutsuType> taijutsus = Arrays.stream(JutsuType.values())
                .filter(j -> j.getType() == SkillType.TAIJUTSU)
                .toList();

        int totalPages = Math.max(1, (int) Math.ceil((double) taijutsus.size() / ITEMS_PER_PAGE));
        validateCurrentPage(totalPages);

        cmd.set("#LabelPagination.Text", (currentPage + 1) + "/" + totalPages);

        int start = currentPage * ITEMS_PER_PAGE;
        int end = Math.min(start + ITEMS_PER_PAGE, taijutsus.size());
        List<JutsuType> pageTaijutsus = taijutsus.subList(start, end);

        int renderIndex = 0;
        for (JutsuType jutsu : pageTaijutsus) {
            cmd.append("#SkillsContent", TAIJUTSU_CARD_TEMPLATE);

            String basePath = "#SkillsContent[" + renderIndex + "]";

            boolean isUnlocked = data.hasJutsuUnlocked(jutsu.getId());
            boolean isEquipped = data.getEquippedHotbar() != null && data.getEquippedHotbar().containsValue(jutsu.getId());

            int jutsuLevel = data.getJutsuLevel(jutsu.getId());
            float currentXp = data.getJutsuXp(jutsu.getId());
            float requiredXp = jutsuLevel * 50.0f;

            cmd.set(basePath + " #LabelTaijutsuName.Text", jutsu.getName());
            cmd.set(basePath + " #LabelTaijutsuLevel.Text", "[Lv. " + jutsuLevel + "]");
            cmd.set(basePath + " #LabelTaijutsuType.Text", "[Taijutsu]");
            cmd.set(basePath + " #LabelTaijutsuCost.Text", "Stamina: " + (int) jutsu.getResourceCost());
            cmd.set(basePath + " #LabelTaijutsuXp.Text", "XP: " + (int) currentXp + "/" + (int) requiredXp);
            cmd.set(basePath + " #LabelTaijutsuReq.Text", "Taij: " + jutsu.getReqTaijutsu());
            cmd.set(basePath + " #LabelStaminaReq.Text", "Stam: " + jutsu.getReqStamina());
            cmd.set(basePath + " #LabelSpeedReq.Text", "Speed: " + jutsu.getReqSpeed());

            boolean canUnlock = canUnlockJutsu(data, jutsu);

            String actionText = "LEARN";
            if (!isUnlocked) {
                if (!canUnlock) {
                    actionText = "BLOCKED";
                }
            } else {
                actionText = isEquipped ? "UNEQUIP" : "EQUIP";
            }

            cmd.set(basePath + " #BtnTaijutsuAction.Text", actionText);

            evt.addEventBinding(
                    CustomUIEventBindingType.Activating,
                    basePath + " #BtnTaijutsuAction",
                    new EventData().append("Action", "ToggleJutsu").append("Slot", jutsu.getId()),
                    false
            );

            renderIndex++;
        }
    }

    public void buildComponentsGenjutsu(@Nonnull UIEventBuilder evt, @Nonnull UICommandBuilder cmd, @Nonnull PlayerData data) {
        cmd.clear("#SkillsContent");

        List<JutsuType> genjutsus = Arrays.stream(JutsuType.values())
                .filter(j -> j.getType() == SkillType.GENJUTSU)
                .toList();

        int totalPages = Math.max(1, (int) Math.ceil((double) genjutsus.size() / ITEMS_PER_PAGE));
        validateCurrentPage(totalPages);

        cmd.set("#LabelPagination.Text", (currentPage + 1) + "/" + totalPages);

        int start = currentPage * ITEMS_PER_PAGE;
        int end = Math.min(start + ITEMS_PER_PAGE, genjutsus.size());
        List<JutsuType> pageTaijutsus = genjutsus.subList(start, end);

        int renderIndex = 0;
        for (JutsuType jutsu : pageTaijutsus) {
            cmd.append("#SkillsContent", GENJUTSU_CARD_TEMPLATE);

            String basePath = "#SkillsContent[" + renderIndex + "]";

            boolean isUnlocked = data.hasJutsuUnlocked(jutsu.getId());
            boolean isEquipped = data.getEquippedHotbar() != null && data.getEquippedHotbar().containsValue(jutsu.getId());

            int jutsuLevel = data.getJutsuLevel(jutsu.getId());
            float currentXp = data.getJutsuXp(jutsu.getId());
            float requiredXp = jutsuLevel * 50.0f;

            cmd.set(basePath + " #LabelGenjutsuName.Text", jutsu.getName());
            cmd.set(basePath + " #LabelGenjutsuLevel.Text", "[Lv. " + jutsuLevel + "]");
            cmd.set(basePath + " #LabelGenjutsuType.Text", "[Genjutsu]");
            cmd.set(basePath + " #LabelGenjutsuCost.Text", "Chakra: " + (int) jutsu.getResourceCost());
            cmd.set(basePath + " #LabelGenjutsuXp.Text", "XP: " + (int) currentXp + "/" + (int) requiredXp);
            cmd.set(basePath + " #LabelGenjutsuReq.Text", "Genj: " + jutsu.getReqGenjutsu());
            cmd.set(basePath + " #LabelNinjutsuReq.Text", "Ninj: " + jutsu.getReqNinjutsu());

            boolean canUnlock = canUnlockJutsu(data, jutsu);

            String actionText = "LEARN";
            if (!isUnlocked) {
                if (!canUnlock) {
                    actionText = "BLOCKED";
                }
            } else {
                actionText = isEquipped ? "UNEQUIP" : "EQUIP";
            }

            cmd.set(basePath + " #BtnGenjutsuAction.Text", actionText);

            evt.addEventBinding(
                    CustomUIEventBindingType.Activating,
                    basePath + " #BtnGenjutsuAction",
                    new EventData().append("Action", "ToggleJutsu").append("Slot", jutsu.getId()),
                    false
            );

            renderIndex++;
        }
    }

    private void validateCurrentPage(int totalPages) {
        if (currentPage >= totalPages) {
            currentPage = totalPages - 1;
        }
        if (currentPage < 0) {
            currentPage = 0;
        }
    }

    private String formatJutsuName(PlayerData data, String jutsuId) {
        if (jutsuId == null || jutsuId.isBlank()) {
            return "Empty Slot";
        }

        JutsuType jutsu = JutsuType.fromId(jutsuId);
        if (jutsu != null) {
            return jutsu.getName();
        }

        ClanType playerClan = ClanType.fromName(data.getClan());
        if (playerClan != ClanType.NONE) {
            for (ClanType.ClanSkill skill : playerClan.getSkills()) {
                if (skill.getId().equalsIgnoreCase(jutsuId)) {
                    return skill.getName();
                }
            }
        }

        String[] words = jutsuId.split("_");
        StringBuilder formatted = new StringBuilder();
        for (String word : words) {
            if (!word.isEmpty()) {
                formatted.append(Character.toUpperCase(word.charAt(0)))
                        .append(word.substring(1))
                        .append(" ");
            }
        }
        return formatted.toString().trim();
    }

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, @Nonnull SkillsPage.UIEventData data) {
        String action = data.getAction();
        if (action == null) return;

        if (NavigationButtons.handleNavigation(action, store, ref, playerRef)) {
            return;
        }

        PlayerData playerData = dataManager.getPlayerData(uuid);
        if (playerData == null) return;

        if (SKILLS_UI_MAP.containsKey(action)) {
            this.currentTab = action;
            this.currentPage = 0;
            rebuild();
            return;
        }

        if ("PrevPage".equals(action)) {
            if (currentPage > 0) {
                currentPage--;
                rebuild();
            }
            return;
        }

        if ("NextPage".equals(action)) {
            currentPage++;
            rebuild();
            return;
        }

        if ("SelectClan".equals(action)) {
            String selectedClanName = data.getSlot();
            if (selectedClanName != null) {
                ClanType chosenClan = ClanType.fromName(selectedClanName);
                if (chosenClan != ClanType.NONE) {
                    playerData.setClan(chosenClan.getDisplayName());

                    playerData.setMaxHealth(100.0f + chosenClan.getBonusHealth());
                    playerData.setMaxChakra(100.0f + chosenClan.getBonusChakra());
                    playerData.setChakraControl(chosenClan.getChakraControlMultiplier());

                    if (chosenClan == ClanType.UCHIHA) {
                        MangekyouType currentMangekyou = MangekyouType.fromName(playerData.getMangekyouType());
                        if (currentMangekyou == null) {
                            MangekyouType drawnMangekyou = MANGEKYOU_POOL.get(RANDOM.nextInt(MANGEKYOU_POOL.size()));
                            playerData.setMangekyouType(drawnMangekyou.name());
                        }
                    }

                    dataManager.savePlayer(uuid);
                    this.currentPage = 0;
                }
            }
            rebuild();
            return;
        }

        if ("ToggleJutsu".equals(action)) {
            String jutsuId = data.getSlot();
            if (jutsuId != null) {
                handleJutsuAction(playerData, jutsuId);
                updateHud(ref);
            }
            rebuild();
            return;
        }

        if ("ToggleClanSkill".equals(action)) {
            String skillId = data.getSlot();
            if (skillId != null) {
                handleClanSkillAction(playerData, skillId);
                updateHud(ref);
            }
            rebuild();
            return;
        }

        rebuild();
    }

    private void updateHud(@Nonnull Ref<EntityStore> ref) {
        Player playerComp = ref.getStore().getComponent(ref, Player.getComponentType());
        if (playerComp != null) {
            JutsuEquippedHud.update(playerComp, playerRef);
        }
    }

    private void handleJutsuAction(PlayerData data, String jutsuId) {
        Map<String, String> hotbar = data.getEquippedHotbar();
        if (hotbar == null) return;

        JutsuType targetJutsu = JutsuType.fromId(jutsuId);
        if (targetJutsu == null) return;

        if (!data.hasJutsuUnlocked(jutsuId)) {
            if (canUnlockJutsu(data, targetJutsu)) {
                if (targetJutsu.getRequiredPoints() > 0) {
                    data.setAvailablePoints(data.getAvailablePoints() - targetJutsu.getRequiredPoints());
                }

                data.getUnlockedJutsus().add(jutsuId);
                dataManager.savePlayer(uuid);
            }
            return;
        }

        toggleHotbarEquip(data, hotbar, jutsuId, 4);
    }

    private boolean canUnlockJutsu(PlayerData data, JutsuType jutsu) {
        return data.getTaijutsu() >= jutsu.getReqTaijutsu()
                && data.getGenjutsu() >= jutsu.getReqGenjutsu()
                && data.getNinjutsu() >= jutsu.getReqNinjutsu()
                && data.getSpeed() >= jutsu.getReqSpeed()
                && data.getAvailablePoints() >= jutsu.getRequiredPoints();
    }

    private void handleClanSkillAction(PlayerData data, String skillId) {
        Map<String, String> clanHotbar = data.getEquippedClanHotbar();
        if (clanHotbar == null) return;

        ClanType playerClan = ClanType.fromName(data.getClan());
        if (playerClan == ClanType.NONE) return;

        ClanType.ClanSkill targetSkill = null;
        for (ClanType.ClanSkill skill : playerClan.getSkills()) {
            if (skill.getId().equalsIgnoreCase(skillId)) {
                targetSkill = skill;
                break;
            }
        }

        if (targetSkill == null) return;

        if (!data.hasUnlockClanSkill(skillId)) {
            if (data.canUnlockClanSkill(targetSkill)) {
                data.getUnlockedClanJutsu().add(skillId);
                dataManager.savePlayer(uuid);

                Main.get().getClanManager().onClanSkillUnlocked(playerRef, data, skillId);
            }
            return;
        }

        if (!targetSkill.isEquippable()) return;

        toggleHotbarEquip(data, clanHotbar, skillId, 3);
    }

    private void toggleHotbarEquip(PlayerData data, Map<String, String> hotbar, String id, int maxSlots) {
        for (String key : hotbar.keySet()) {
            if (id.equals(hotbar.get(key))) {
                hotbar.remove(key);
                dataManager.savePlayer(uuid);
                return;
            }
        }

        for (int i = 1; i <= maxSlots; i++) {
            String slotKey = "slot_" + i;
            if (!hotbar.containsKey(slotKey) || hotbar.get(slotKey) == null) {
                hotbar.put(slotKey, id);
                dataManager.savePlayer(uuid);
                break;
            }
        }
    }

    public static class UIEventData {
        public static final BuilderCodec<SkillsPage.UIEventData> CODEC = BuilderCodec.builder(SkillsPage.UIEventData.class, SkillsPage.UIEventData::new)
                .append(new KeyedCodec<>("Action", Codec.STRING), (e, v) -> e.action = v, e -> e.action).add()
                .append(new KeyedCodec<>("Slot", Codec.STRING), (e, v) -> e.slot = v, e -> e.slot).add()
                .build();

        private String action;
        private String slot;

        public UIEventData() {}

        public String getAction() { return action; }
        public String getSlot() { return slot; }
    }
}