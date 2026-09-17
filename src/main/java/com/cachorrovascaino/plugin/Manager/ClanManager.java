package com.cachorrovascaino.plugin.Manager;

import com.cachorrovascaino.plugin.Abstractions.ClanJutsu;
import com.cachorrovascaino.plugin.Data.Clan.ClanType;
import com.cachorrovascaino.plugin.Data.MangekyouType;
import com.cachorrovascaino.plugin.Data.PlayerData;
import com.cachorrovascaino.plugin.Features.Clan.Abilities.Hyuga.HakkeKushoJutsu;
import com.cachorrovascaino.plugin.Features.Clan.Abilities.Hyuga.KaitenJutsu;
import com.cachorrovascaino.plugin.Features.Clan.Abilities.Uchiha.KamuiBehindTeleportJutsu;
import com.cachorrovascaino.plugin.Features.Clan.Abilities.Uchiha.KamuiIntangibilityJutsu;
import com.cachorrovascaino.plugin.Features.Clan.Abilities.Uchiha.KotoamatsukamiOpticalTetherJutsu;
import com.cachorrovascaino.plugin.Features.Clan.Abilities.Uchiha.KotoamatsukamiSensoryBlindspotJutsu;
import com.cachorrovascaino.plugin.Features.Clan.ByakuganJutsu;
import com.cachorrovascaino.plugin.Features.Clan.MangekyouJutsu;
import com.cachorrovascaino.plugin.Features.Clan.SharinganJutsu;
import com.cachorrovascaino.plugin.Main;
import com.cachorrovascaino.plugin.Utils.ChakraUtils;
import com.cachorrovascaino.plugin.Utils.EyesUtils;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.awt.Color;
import java.util.*;

public class ClanManager {

    private final EyesUtils eyesUtils = new EyesUtils();
    private final Main plugin;

    private final Map<String, ClanJutsu> clanJutsuRegistry = new HashMap<>();

    private static final Map<String, String> DOJUTSU_STAGE_ASSETS = Map.of(
            "sharingan_1", "Sharingan_HD",
            "sharingan_2", "Sharingan_Two_Tomoe_HD",
            "sharingan_3", "Sharingan_Three_Tomoe_HD",
            "byakugan", "Byakugan_HD"
    );

    private static final Random RANDOM = new Random();
    private static final List<MangekyouType> MANGEKYOU_POOL = List.of(
            MangekyouType.OBITO,
            MangekyouType.SHISUI
    );

    private static final Map<String, String> CLAN_SLOT_COMBOS = Map.of(
            "R-L-L", "slot_1",
            "R-R-L", "slot_2",
            "R-L-R", "slot_3"
    );

    public ClanManager(Main plugin) {
        this.plugin = plugin;
        registerClanJutsus();
    }

    private void registerClanJutsus() {
        // Uchiha
        registerClanJutsu(SharinganJutsu.INSTANCE);
        registerClanJutsu(MangekyouJutsu.INSTANCE);
        // OBITO
        registerClanJutsu(KamuiIntangibilityJutsu.INSTANCE);
        registerClanJutsu(KamuiBehindTeleportJutsu.INSTANCE);
        // SHISUI
        registerClanJutsu(KotoamatsukamiOpticalTetherJutsu.INSTANCE);
        registerClanJutsu(KotoamatsukamiSensoryBlindspotJutsu.INSTANCE);

        //Hyuga
        registerClanJutsu(ByakuganJutsu.INSTANCE);
        registerClanJutsu(KaitenJutsu.INSTANCE);
        registerClanJutsu(HakkeKushoJutsu.INSTANCE);
    }

    private void registerClanJutsu(ClanJutsu jutsu) { clanJutsuRegistry.put(jutsu.getId(), jutsu); }

    public void setPlayerClan(PlayerRef playerRef, ClanType newClan) {
        if (playerRef == null || newClan == null) return;

        PlayerData data = Main.getDataManager().getPlayerData(playerRef.getUuid());
        if (data == null) return;

        ClanType oldClan = ClanType.fromName(data.getClan());
        if (oldClan != ClanType.NONE) {
            removeClanModifiers(data, oldClan);
        }

        if (data.getUnlockedClanJutsu() != null) { data.getUnlockedClanJutsu().clear(); }
        if (data.getEquippedClanHotbar() != null) { data.getEquippedClanHotbar().clear(); }

        data.setActiveDojutsu(PlayerData.DojutsuState.NONE);
        data.setEyeDojutsuType(null);
        data.setEyeStage(0);

        if (data.getOriginalEyesColor() != null && !data.getOriginalEyesColor().isEmpty()) {
            data.setEyesColor(data.getOriginalEyesColor());
        }
        if (data.getOriginalEyesId() != null && !data.getOriginalEyesId().isEmpty()) {
            data.setEyesId(data.getOriginalEyesId());
        }

        if (newClan == ClanType.UCHIHA) {
            if (data.getMangekyouType() == null || data.getMangekyouType().isEmpty() || data.getMangekyouType().equalsIgnoreCase("NONE")) {
                MangekyouType drawn = MANGEKYOU_POOL.get(RANDOM.nextInt(MANGEKYOU_POOL.size()));
                data.setMangekyouType(drawn.name());
            }
        } else {
            data.setMangekyouType(null);
        }

        data.setClan(newClan.name());
        applyClanModifiers(data, newClan);

        // 7. Salva no disco/cache
        Main.getDataManager().savePlayer(playerRef.getUuid());
    }

    private void removeClanModifiers(PlayerData data, ClanType oldClan) {
        float currentMaxHp = Math.max(100.0f, data.getMaxHealth() - oldClan.getBonusHealth());
        data.setMaxHealth(currentMaxHp);
        data.setCurrentHealth(Math.min(data.getCurrentHealth(), data.getMaxHealth()));

        float currentMaxChakra = Math.max(100.0f, data.getMaxChakra() - oldClan.getBonusChakra());
        data.setMaxChakra(currentMaxChakra);
        data.setCurrentChakra(Math.min(data.getCurrentChakra(), data.getMaxChakra()));
    }

    private void applyClanModifiers(PlayerData data, ClanType clan) {
        if (clan == ClanType.NONE) return;

        data.setMaxHealth(data.getMaxHealth() + clan.getBonusHealth());
        data.setCurrentHealth(Math.min(data.getCurrentHealth(), data.getMaxHealth()));

        data.setMaxChakra(data.getMaxChakra() + clan.getBonusChakra());
        data.setCurrentChakra(Math.min(data.getCurrentChakra(), data.getMaxChakra()));

        data.setChakraControl(data.getChakraControl() + clan.getChakraControlMultiplier());
    }

    // ==========================================================================================
    // COMBOS DE TRANSFORMAÇÃO / JUTSUS DE CLÃ
    // ==========================================================================================
    public boolean handleTransformationCombo(PlayerRef playerRef, String combo) {
        if (combo.equals("R-R-R")) { return handleDojutsuToggleCombo(playerRef, combo); }
        String slot = CLAN_SLOT_COMBOS.get(combo);
        if (slot != null) { return checkClanComboExecution(playerRef, combo, slot); }
        return false;
    }

    private boolean checkClanComboExecution(PlayerRef playerRef, String combo, String slot) {
        PlayerData playerData = Main.getDataManager().getPlayerData(playerRef.getUuid());
        if (playerData == null) return false;

        String equippedClanJutsuId = playerData.getEquippedClanHotbar().get(slot);

        if (equippedClanJutsuId == null) {
            playerRef.sendMessage(Message.raw("No Clan Jutsu equipped on " + slot + "!").color(Color.RED));
            Main.getJutsuManager().clearComboState(playerRef);
            return true;
        }

        if (!playerData.hasUnlockClanSkill(equippedClanJutsuId)) {
            playerRef.sendMessage(Message.raw("You haven't learned this Clan Jutsu yet!").color(Color.RED));
            Main.getJutsuManager().clearComboState(playerRef);
            return true;
        }

        triggerEquippedClanJutsu(playerRef, equippedClanJutsuId, combo);
        return true;
    }

    private void triggerEquippedClanJutsu(PlayerRef playerRef, String clanJutsuId, String combo) {
        ClanJutsu clanJutsu = clanJutsuRegistry.get(clanJutsuId);

        if (clanJutsu == null) {
            plugin.getLogger().atWarning().log("[ClanManager] Jutsu de Clã com ID '" + clanJutsuId + "' não foi registrado.");
            Main.getJutsuManager().clearComboState(playerRef);
            return;
        }

        UUID uuid = playerRef.getUuid();

        if (!clanJutsu.canExecute(playerRef)) {
            Main.getJutsuManager().clearComboState(playerRef);
            return;
        }

        if (Main.getCooldownManager().isOnCooldown(uuid, clanJutsuId)) {
            float remaining = Main.getCooldownManager().getRemainingSeconds(uuid, clanJutsuId);
            playerRef.sendMessage(Message.raw("Please wait " + String.format("%.1f", remaining) + "s before using this Jutsu again.").color(Color.RED));
            Main.getJutsuManager().clearComboState(playerRef);
            return;
        }

        float jutsuCost = clanJutsu.getChakraCost(playerRef);
        float currentChakra = ChakraUtils.getCurrentChakra(playerRef);

        if (currentChakra < jutsuCost) {
            playerRef.sendMessage(Message.raw("Insufficient chakra: " + (int) jutsuCost).color(Color.RED));
            Main.getJutsuManager().clearComboState(playerRef);
            return;
        }

        Ref<EntityStore> ref = playerRef.getReference();
        if (ref == null || !ref.isValid()) {
            Main.getJutsuManager().clearComboState(playerRef);
            return;
        }

        Store<EntityStore> store = ref.getStore();
        EntityStore entityStore = store.getExternalData();
        World world = entityStore.getWorld();

        if (jutsuCost > 0) {
            ChakraUtils.consumeChakra(playerRef, jutsuCost);
            Main.getJutsuManager().updateChakraHud(playerRef);
        }

        clanJutsu.execute(playerRef, ref, store, world);

        if (clanJutsu.getCooldown() > 0) {
            Main.getCooldownManager().setCooldown(uuid, clanJutsuId, clanJutsu.getCooldown());
        }

        String formattedSequence = combo.replace("L", "M1").replace("R", "F").replace("-", " -> ");
        Main.getJutsuManager().updateComboHud(playerRef, formattedSequence, clanJutsu.getDisplayName(), JutsuManager.JUTSU_HUD_DISPLAY_MS);
        Main.getJutsuManager().clearComboState(playerRef);
    }

    private boolean handleDojutsuToggleCombo(PlayerRef playerRef, String combo) {
        String formattedSequence = combo.replace("L", "M1").replace("R", "F").replace("-", " -> ");

        UUID uuid = playerRef.getUuid();
        PlayerData playerData = Main.getDataManager().getPlayerData(uuid);
        if (playerData == null) return false;

        Ref<EntityStore> ref = playerRef.getReference();
        if (ref == null || !ref.isValid()) return false;

        Store<EntityStore> store = ref.getStore();
        ClanType clan = ClanType.fromName(playerData.getClan());

        boolean willBeActive = false;
        if (clan == ClanType.HYUGA) {
            willBeActive = store.getComponent(ref, plugin.getByakuganComponentType()) == null;
        }
        if (clan == ClanType.UCHIHA) {
            willBeActive = store.getComponent(ref, plugin.getSharinganComponentType()) == null;
        }

        boolean toggled = toggleDojutsu(playerRef);

        if (toggled) {
            String statusMsg = willBeActive ? "ACTIVATED" : "DISABLED";
            Main.getJutsuManager().updateComboHud(
                    playerRef, formattedSequence, clan.getDisplayName() + " (" + statusMsg + ")", JutsuManager.JUTSU_HUD_DISPLAY_MS
            );
        }

        Main.getJutsuManager().clearComboState(playerRef);
        return true;
    }

    // ==========================================================================================
    // DŌJUTSU (ativação/desativação visual do estágio PASSIVO)
    // ==========================================================================================
    public boolean toggleDojutsu(PlayerRef playerRef) {
        if (playerRef == null) return false;

        UUID uuid = playerRef.getUuid();
        PlayerData playerData = Main.getDataManager().getPlayerData(uuid);
        if (playerData == null) return false;

        ClanType clan = ClanType.fromName(playerData.getClan());

        if (clan == ClanType.NONE) {
            playerRef.sendMessage(Message.raw("Você não pertence a nenhum clã para ativar um Dōjutsu!").color(Color.RED));
            return false;
        }

        Ref<EntityStore> ref = playerRef.getReference();
        if (ref == null || !ref.isValid()) return false;

        Store<EntityStore> store = ref.getStore();
        World world = store.getExternalData().getWorld();

        if (clan == ClanType.HYUGA) {
            if (!ByakuganJutsu.INSTANCE.canExecute(playerRef)) {
                return false;
            }
            ByakuganJutsu.INSTANCE.execute(playerRef, ref, store, world);
            return true;
        }

        if (clan == ClanType.UCHIHA) {
            if (!SharinganJutsu.INSTANCE.canExecute(playerRef)) {
                return false;
            }
            SharinganJutsu.INSTANCE.execute(playerRef, ref, store, world);
            return true;
        }

        return false;
    }

    public void onClanSkillUnlocked(PlayerRef playerRef, PlayerData playerData, String skillId) {
        String eyeAssetId = DOJUTSU_STAGE_ASSETS.get(skillId);
        if (eyeAssetId == null) return;

        playerData.setEyeDojutsuType(eyeAssetId);
        playerData.setEyeStage(playerData.getEyeStage() + 1);

        Ref<EntityStore> ref = playerRef.getReference();
        if (ref != null && ref.isValid()) {
            Store<EntityStore> store = ref.getStore();
            World world = store.getExternalData().getWorld();

            if (store.getComponent(ref, plugin.getSharinganComponentType()) != null) {
                eyesUtils.updateHytalePlayerEyes(world, playerRef, eyeAssetId, "");
            }
        }
    }
}