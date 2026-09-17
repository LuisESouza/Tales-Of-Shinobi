package com.cachorrovascaino.plugin.Ui.Page;

import com.cachorrovascaino.plugin.Abstractions.SkillType;
import com.cachorrovascaino.plugin.Data.Clan.ClanType;
import com.cachorrovascaino.plugin.Data.Jutsus.ElementType;
import com.cachorrovascaino.plugin.Data.Jutsus.JutsuType;
import com.cachorrovascaino.plugin.Data.PlayerData;
import com.cachorrovascaino.plugin.Main;
import com.cachorrovascaino.plugin.Manager.PlayerDataManager;
import com.cachorrovascaino.plugin.Ui.Hud.JutsuEquippedHud;
import com.cachorrovascaino.plugin.Ui.Page.renderers.ClanTabRenderer;
import com.cachorrovascaino.plugin.Ui.Page.renderers.JutsuTabRenderer;
import com.cachorrovascaino.plugin.Ui.Page.renderers.SkillTabRenderer;
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
    private ElementType selectedElement = ElementType.NONE; // NONE = Mostrar todos
    private int currentPage = 0;
    private static final int ITEMS_PER_PAGE = 2;

    private static final String INTERFACE_MAIN = "Shinobi/Menus/SkillsMenu.ui";

    private final Map<String, SkillTabRenderer> tabRenderers = Map.of(
            "Ninjutsu", new JutsuTabRenderer(SkillType.NINJUTSU, "Shinobi/Components/skills/NinjutsuSkill.ui"),
            "Taijutsu", new JutsuTabRenderer(SkillType.TAIJUTSU, "Shinobi/Components/skills/TaijutsuSkill.ui"),
            "Genjutsu", new JutsuTabRenderer(SkillType.GENJUTSU, "Shinobi/Components/skills/GenjutsuSkill.ui"),
            "Clan", new ClanTabRenderer()
    );

    public SkillsPage(@Nonnull PlayerRef playerRef, @Nonnull PlayerDataManager dataManager) {
        super(playerRef, CustomPageLifetime.CanDismissOrCloseThroughInteraction, UIEventData.CODEC);
        this.playerRef = playerRef;
        this.uuid = playerRef.getUuid();
        this.dataManager = dataManager;
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder cmd, @Nonnull UIEventBuilder evt, @Nonnull Store<EntityStore> store) {
        cmd.append(INTERFACE_MAIN);
        cmd.append("#NavigationContent", "Shinobi/Components/navigation/NavigationMenus.ui");

        PlayerData data = getPlayerData();

        boolean isNinjutsuTab = "Ninjutsu".equalsIgnoreCase(currentTab);
        cmd.set("#NavBarElements.Visible", isNinjutsuTab);

        if ("Clan".equalsIgnoreCase(currentTab)) {
            buildSlotSkillClan(data, cmd);
        } else {
            buildSlotsSkill(data, cmd);
        }

        buildButtons(evt);

        SkillTabRenderer renderer = tabRenderers.get(currentTab);
        if (renderer != null) {
            int totalItems = (renderer instanceof JutsuTabRenderer jutsuRenderer && isNinjutsuTab)
                    ? jutsuRenderer.getTotalItems(data, selectedElement)
                    : renderer.getTotalItems(data);

            int totalPages = Math.max(1, (int) Math.ceil((double) totalItems / ITEMS_PER_PAGE));
            validateCurrentPage(totalPages);

            cmd.set("#LabelPagination.Text", (currentPage + 1) + "/" + totalPages);

            if (renderer instanceof JutsuTabRenderer jutsuRenderer && isNinjutsuTab) {
                jutsuRenderer.render(cmd, evt, data, currentPage, ITEMS_PER_PAGE, selectedElement);
            } else {
                renderer.render(cmd, evt, data, currentPage, ITEMS_PER_PAGE);
            }
        }
    }

    private PlayerData getPlayerData() {
        String username = playerRef.getUsername();
        var player = Universe.get().getPlayer(uuid);
        if (player != null) username = player.getUsername();

        PlayerData data = dataManager.getPlayerData(uuid);
        if (data == null) data = dataManager.loadPlayer(uuid, username);
        return data != null ? data : new PlayerData(username, uuid.toString());
    }

    private void buildButtons(@Nonnull UIEventBuilder evt) {
        // Abas Principais
        evt.addEventBinding(CustomUIEventBindingType.Activating, "#BtnTabTaijutsu", new EventData().append("Action", "Taijutsu"), false);
        evt.addEventBinding(CustomUIEventBindingType.Activating, "#BtnTabNinjutsu", new EventData().append("Action", "Ninjutsu"), false);
        evt.addEventBinding(CustomUIEventBindingType.Activating, "#BtnTabGenjutsu", new EventData().append("Action", "Genjutsu"), false);
        evt.addEventBinding(CustomUIEventBindingType.Activating, "#BtnTabClan", new EventData().append("Action", "Clan"), false);

        // Botões dos Elementos (Others agora envia NONE diretamente)
        evt.addEventBinding(CustomUIEventBindingType.Activating, "#BtnTabFire", new EventData().append("Action", "FilterElement").append("Slot", "FIRE"), false);
        evt.addEventBinding(CustomUIEventBindingType.Activating, "#BtnTabWater", new EventData().append("Action", "FilterElement").append("Slot", "WATER"), false);
        evt.addEventBinding(CustomUIEventBindingType.Activating, "#BtnTabWind", new EventData().append("Action", "FilterElement").append("Slot", "WIND"), false);
        evt.addEventBinding(CustomUIEventBindingType.Activating, "#BtnTabEarth", new EventData().append("Action", "FilterElement").append("Slot", "EARTH"), false);
        evt.addEventBinding(CustomUIEventBindingType.Activating, "#BtnTabRain", new EventData().append("Action", "FilterElement").append("Slot", "RAIN"), false);
        evt.addEventBinding(CustomUIEventBindingType.Activating, "#BtnTabOthers", new EventData().append("Action", "FilterElement").append("Slot", "NONE"), false);

        // Paginação
        evt.addEventBinding(CustomUIEventBindingType.Activating, "#BtnPrev", new EventData().append("Action", "PrevPage"), false);
        evt.addEventBinding(CustomUIEventBindingType.Activating, "#BtnNext", new EventData().append("Action", "NextPage"), false);

        NavigationButtons.bindButtons(evt);
    }

    private void buildSlotSkillClan(@Nonnull PlayerData data, @Nonnull UICommandBuilder cmd) {
        cmd.clear("#EquippedJutsusPanel");
        cmd.append("#EquippedJutsusPanel", "Shinobi/Components/slots/ClanSlot.ui");
        Map<String, String> clanHotbar = data.getEquippedClanHotbar();

        cmd.set("#LabelSlot1.Text", formatJutsuName(data, clanHotbar != null ? clanHotbar.get("slot_1") : null));
        cmd.set("#LabelSlot2.Text", formatJutsuName(data, clanHotbar != null ? clanHotbar.get("slot_2") : null));
        cmd.set("#LabelSlot3.Text", formatJutsuName(data, clanHotbar != null ? clanHotbar.get("slot_3") : null));
    }

    private void buildSlotsSkill(@Nonnull PlayerData data, @Nonnull UICommandBuilder cmd) {
        cmd.clear("#EquippedJutsusPanel");
        cmd.append("#EquippedJutsusPanel", "Shinobi/Components/slots/ComboSlot.ui");
        Map<String, String> hotbar = data.getEquippedHotbar();

        cmd.set("#LabelSlot1.Text", formatJutsuName(data, hotbar != null ? hotbar.get("slot_1") : null));
        cmd.set("#LabelSlot2.Text", formatJutsuName(data, hotbar != null ? hotbar.get("slot_2") : null));
        cmd.set("#LabelSlot3.Text", formatJutsuName(data, hotbar != null ? hotbar.get("slot_3") : null));
        cmd.set("#LabelSlot4.Text", formatJutsuName(data, hotbar != null ? hotbar.get("slot_4") : null));
    }

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, @Nonnull UIEventData data) {
        String action = data.getAction();
        if (action == null) return;

        if (NavigationButtons.handleNavigation(action, store, ref, playerRef)) return;

        PlayerData playerData = dataManager.getPlayerData(uuid);
        if (playerData == null) return;

        if (tabRenderers.containsKey(action)) {
            this.currentTab = action;
            this.selectedElement = null;
            this.currentPage = 0;
            rebuild();
            return;
        }

        if ("FilterElement".equals(action)) {
            String elementSlot = data.getSlot();
            if (elementSlot != null) {
                try {
                    this.selectedElement = ElementType.valueOf(elementSlot);
                } catch (IllegalArgumentException e) {
                    this.selectedElement = ElementType.NONE;
                }
            }
            this.currentPage = 0;
            rebuild();
            return;
        }

        if ("PrevPage".equals(action)) {
            if (currentPage > 0) { currentPage--; rebuild(); }
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
                    Main.getClanManager().setPlayerClan(playerRef, chosenClan);
                    this.currentPage = 0;
                }
            }
            rebuild();
            return;
        }

        if ("ToggleJutsu".equals(action)) {
            if (data.getSlot() != null) {
                handleJutsuAction(playerData, data.getSlot());
                updateHud(ref);
            }
            rebuild();
            return;
        }

        if ("ToggleClanSkill".equals(action)) {
            if (data.getSlot() != null) {
                handleClanSkillAction(playerData, data.getSlot());
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
                Main.getClanManager().onClanSkillUnlocked(playerRef, data, skillId);
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

    private void validateCurrentPage(int totalPages) {
        if (currentPage >= totalPages) currentPage = totalPages - 1;
        if (currentPage < 0) currentPage = 0;
    }

    private String formatJutsuName(PlayerData data, String jutsuId) {
        if (jutsuId == null || jutsuId.isBlank()) return "Empty Slot";

        JutsuType jutsu = JutsuType.fromId(jutsuId);
        if (jutsu != null) return jutsu.getName();

        ClanType playerClan = ClanType.fromName(data.getClan());
        if (playerClan != ClanType.NONE) {
            for (ClanType.ClanSkill skill : playerClan.getSkills()) {
                if (skill.getId().equalsIgnoreCase(jutsuId)) return skill.getName();
            }
        }

        String[] words = jutsuId.split("_");
        StringBuilder formatted = new StringBuilder();
        for (String word : words) {
            if (!word.isEmpty()) {
                formatted.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1)).append(" ");
            }
        }
        return formatted.toString().trim();
    }

    public static class UIEventData {
        public static final BuilderCodec<UIEventData> CODEC = BuilderCodec.builder(UIEventData.class, UIEventData::new)
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