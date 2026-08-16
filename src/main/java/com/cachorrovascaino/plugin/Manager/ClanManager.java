package com.cachorrovascaino.plugin.Manager;

import com.cachorrovascaino.plugin.Abstractions.ClanJutsu;
import com.cachorrovascaino.plugin.Data.Clan.ClanType;
import com.cachorrovascaino.plugin.Data.PlayerData;
import com.cachorrovascaino.plugin.Features.Clan.MangekyouJutsu;
import com.cachorrovascaino.plugin.Main;
import com.cachorrovascaino.plugin.Utils.ChakraUtils;
import com.cachorrovascaino.plugin.Utils.EyesUtils;
import com.cachorrovascaino.plugin.Utils.WeatherUtils;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.io.PacketHandler;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ClanManager {

    private EyesUtils eyesUtils = new EyesUtils();
    private WeatherUtils weatherUtils = new WeatherUtils();
    private final Main plugin;

    private final Map<String, ClanJutsu> clanJutsuRegistry = new HashMap<>();

    private static final Map<String, String> DOJUTSU_STAGE_ASSETS = Map.of(
            "sharingan_1", "Sharingan_HD",
            "sharingan_2", "Sharingan_Two_Tomoe_HD",
            "sharingan_3", "Sharingan_Three_Tomoe_HD",
            "byakugan", "Byakugan_HD"
    );

    private static final Map<String, String> CLAN_SLOT_COMBOS = Map.of(
            "R-L-L", "slot_1",
            "R-L-R", "slot_2",
            "R-R-L", "slot_3"
    );

    public ClanManager(Main plugin) {
        this.plugin = plugin;
        registerClanJutsus();
    }

    private void registerClanJutsus() { registerClanJutsu(MangekyouJutsu.INSTANCE); }

    private void registerClanJutsu(ClanJutsu jutsu) { clanJutsuRegistry.put(jutsu.getId(), jutsu); }

    public boolean setPlayerClan(PlayerRef playerRef, ClanType newClan) {
        if (playerRef == null || newClan == null) return false;

        PlayerData data = plugin.getDataManager().getPlayerData(playerRef.getUuid());
        if (data == null) return false;

        data.setClan(newClan.getDisplayName());
        applyClanModifiers(data, newClan);

        plugin.getJutsuManager().updateChakraHud(playerRef);
        plugin.getDataManager().savePlayer(playerRef.getUuid());
        return true;
    }

    private void applyClanModifiers(PlayerData data, ClanType clan) {
        float baseHealth = 100.0f;
        float baseChakra = 100.0f;

        data.setMaxHealth(baseHealth + clan.getBonusHealth());
        data.setCurrentHealth(Math.min(data.getCurrentHealth(), data.getMaxHealth()));

        data.setMaxChakra(baseChakra + clan.getBonusChakra());
        data.setCurrentChakra(Math.min(data.getCurrentChakra(), data.getMaxChakra()));

        data.setChakraControl(clan.getChakraControlMultiplier());
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
        PlayerData playerData = plugin.getDataManager().getPlayerData(playerRef.getUuid());
        if (playerData == null) return false;

        String equippedClanJutsuId = playerData.getEquippedClanHotbar().get(slot);

        if (equippedClanJutsuId == null) {
            playerRef.sendMessage(Message.raw("No Clan Jutsu equipped on " + slot + "!").color(Color.RED));
            plugin.getJutsuManager().clearComboState(playerRef);
            return true;
        }

        if (!playerData.hasUnlockClanSkill(equippedClanJutsuId)) {
            playerRef.sendMessage(Message.raw("You haven't learned this Clan Jutsu yet!").color(Color.RED));
            plugin.getJutsuManager().clearComboState(playerRef);
            return true;
        }

        triggerEquippedClanJutsu(playerRef, equippedClanJutsuId, combo);
        return true;
    }

    private void triggerEquippedClanJutsu(PlayerRef playerRef, String clanJutsuId, String combo) {
        ClanJutsu clanJutsu = clanJutsuRegistry.get(clanJutsuId);

        if (clanJutsu == null) {
            plugin.getLogger().atWarning().log("[ClanManager] Jutsu de Clã com ID '" + clanJutsuId + "' não foi registrado.");
            plugin.getJutsuManager().clearComboState(playerRef);
            return;
        }

        UUID uuid = playerRef.getUuid();

        if (!clanJutsu.canExecute(playerRef)) {
            plugin.getJutsuManager().clearComboState(playerRef);
            return;
        }

        if (Main.getCooldownManager().isOnCooldown(uuid, clanJutsuId)) {
            float remaining = Main.getCooldownManager().getRemainingSeconds(uuid, clanJutsuId);
            playerRef.sendMessage(Message.raw("Please wait " + String.format("%.1f", remaining) + "s before using this Jutsu again.").color(Color.RED));
            plugin.getJutsuManager().clearComboState(playerRef);
            return;
        }

        float jutsuCost = clanJutsu.getChakraCost(playerRef);
        float currentChakra = ChakraUtils.getCurrentChakra(playerRef);

        if (currentChakra < jutsuCost) {
            playerRef.sendMessage(Message.raw("Insufficient chakra: " + (int) jutsuCost).color(Color.RED));
            plugin.getJutsuManager().clearComboState(playerRef);
            return;
        }

        Ref<EntityStore> ref = playerRef.getReference();
        if (ref == null || !ref.isValid()) {
            plugin.getJutsuManager().clearComboState(playerRef);
            return;
        }

        Store<EntityStore> store = ref.getStore();
        EntityStore entityStore = store.getExternalData();
        World world = entityStore.getWorld();

        if (jutsuCost > 0) {
            ChakraUtils.consumeChakra(playerRef, jutsuCost);
            plugin.getJutsuManager().updateChakraHud(playerRef);
        }

        clanJutsu.execute(playerRef, ref, store, world);

        if (clanJutsu.getCooldown() > 0) {
            Main.getCooldownManager().setCooldown(uuid, clanJutsuId, clanJutsu.getCooldown());
        }

        String formattedSequence = combo.replace("L", "M1").replace("R", "F").replace("-", " -> ");
        plugin.getJutsuManager().updateComboHud(playerRef, formattedSequence, clanJutsu.getDisplayName(), JutsuManager.JUTSU_HUD_DISPLAY_MS);
        plugin.getJutsuManager().clearComboState(playerRef);
    }

    private boolean handleDojutsuToggleCombo(PlayerRef playerRef, String combo) {
        String formattedSequence = combo.replace("L", "M1").replace("R", "F").replace("-", " -> ");

        UUID uuid = playerRef.getUuid();
        PlayerData playerData = plugin.getDataManager().getPlayerData(uuid);

        boolean toggled = toggleDojutsu(playerRef, formattedSequence);
        if (toggled && playerData != null) {
            ClanType clan = ClanType.fromName(playerData.getClan());
            String statusMsg = playerData.isEyeDojutsuActive() ? "ACTIVATED" : "DISABLED";
            plugin.getJutsuManager().updateComboHud(
                    playerRef, formattedSequence, clan.getDisplayName() + " (" + statusMsg + ")", JutsuManager.JUTSU_HUD_DISPLAY_MS
            );
        }

        plugin.getJutsuManager().clearComboState(playerRef);
        return true;
    }

    // ==========================================================================================
    // DŌJUTSU (ativação/desativação visual do estágio PASSIVO)
    // ==========================================================================================
    public boolean toggleDojutsu(PlayerRef playerRef, String formattedSequence) {
        if (playerRef == null) return false;

        UUID uuid = playerRef.getUuid();
        PlayerData playerData = plugin.getDataManager().getPlayerData(uuid);
        if (playerData == null) return false;

        ClanType clan = ClanType.fromName(playerData.getClan());

        if (clan == ClanType.NONE) {
            playerRef.sendMessage(Message.raw("You do not belong to any clan to activate a Dōjutsu.!").color(Color.RED));
            return false;
        }

        boolean newState = !playerData.isEyeDojutsuActive();

        if (newState) {
            String currentStageAsset = playerData.getEyeDojutsuType();
            if (currentStageAsset == null || currentStageAsset.isEmpty() || "NONE".equalsIgnoreCase(currentStageAsset)) {
                playerRef.sendMessage(Message.raw("You haven't unlocked any stages of the Dōjutsu yet!").color(Color.RED));
                return false;
            }
        }

        playerData.setEyeDojutsuActive(newState);

        String statusMsg = newState ? "ACTIVATED" : "DISABLED";
        String dojutsuName = clan.getDisplayName();

        PacketHandler packetHandler = playerRef.getPacketHandler();

        if (newState) {
            eyesUtils.captureOriginalEyesIfNeeded(playerRef, playerData);

            playerData.applyDojutsuEyes(playerData.getEyeDojutsuType(), "");
            eyesUtils.updateHytalePlayerEyes(playerRef, playerData.getEyesId(), playerData.getEyesColor());

            if (clan == ClanType.UCHIHA) {weatherUtils.applyPlayerWeather(playerRef, packetHandler, "Sharingan_Vision");}

            playerRef.sendMessage(Message.raw("[" + dojutsuName + "] " + statusMsg + "!").color(Color.RED));
        } else {
            playerData.restoreOriginalEyes();
            eyesUtils.updateHytalePlayerEyes(playerRef, playerData.getOriginalEyesId(), "");

            if (clan == ClanType.UCHIHA) {weatherUtils.resetPlayerWeather(playerRef, packetHandler, "Zone1_Sunny");}

            playerRef.sendMessage(Message.raw("[" + dojutsuName + "] " + statusMsg + "!").color(Color.GRAY));
        }

        plugin.getDataManager().savePlayer(uuid);
        return true;
    }

    public void onClanSkillUnlocked(PlayerRef playerRef, PlayerData playerData, String skillId) {
        String eyeAssetId = DOJUTSU_STAGE_ASSETS.get(skillId);
        if (eyeAssetId == null) return;

        playerData.setEyeDojutsuType(eyeAssetId);
        playerData.setEyeStage(playerData.getEyeStage() + 1);

        if (playerData.isEyeDojutsuActive()) {
            playerData.applyDojutsuEyes(eyeAssetId, "");
            eyesUtils.updateHytalePlayerEyes(playerRef, eyeAssetId, "");
        }
    }
}