package com.cachorrovascaino.plugin.Manager;

import com.cachorrovascaino.plugin.Data.Jutsus.JutsuType;
import com.cachorrovascaino.plugin.Data.PlayerData;
import com.cachorrovascaino.plugin.Features.Jutsu.fire.FireBallJutsu;
import com.cachorrovascaino.plugin.Abstractions.Jutsu;
import com.cachorrovascaino.plugin.Features.Jutsu.ShadowCloneJutsu;
import com.cachorrovascaino.plugin.Features.Jutsu.SubstitutionJutsu;
import com.cachorrovascaino.plugin.Features.Jutsu.fire.MeteoroJutsu;
import com.cachorrovascaino.plugin.Features.Jutsu.water.WaterBallJutsu;
import com.cachorrovascaino.plugin.Main;
import com.cachorrovascaino.plugin.Ui.Hud.ChakraHud;
import com.cachorrovascaino.plugin.Ui.Hud.ComboHud;
import com.cachorrovascaino.plugin.Utils.ChakraUtils;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.*;

public class JutsuManager {

    private final Main plugin;
    private final Map<String, Jutsu> jutsuRegistry = new HashMap<>();

    private final Map<UUID, String> activeCombos = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastClickTimes = new ConcurrentHashMap<>();
    private final Map<UUID, Long> hudHideTimes = new ConcurrentHashMap<>();
    private final Map<UUID, ChakraHud> activeChakraHuds = new ConcurrentHashMap<>();

    private final Set<UUID> chargingPlayers = ConcurrentHashMap.newKeySet();
    private final Map<UUID, PlayerRef> activePlayerRefs = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    private static final long COMBO_TIMEOUT_MS = 2500;
    private static final long DEBOUNCE_MS = 180;
    static final long JUTSU_HUD_DISPLAY_MS = 5000;

    public JutsuManager(Main plugin) {
        this.plugin = plugin;
        registerJutsus();
        startChakraLoop();
    }

    private void registerJutsus() {
        registerJutsu(ShadowCloneJutsu.INSTANCE);
        registerJutsu(FireBallJutsu.INSTANCE);
        registerJutsu(SubstitutionJutsu.INSTANCE);
        registerJutsu(WaterBallJutsu.INSTANCE);
        registerJutsu(MeteoroJutsu.INSTANCE);
    }

    private void registerJutsu(Jutsu jutsu) {
        jutsuRegistry.put(jutsu.getId(), jutsu);
    }

    private void startChakraLoop() {
        scheduler.scheduleAtFixedRate(() -> {
            for (UUID uuid : chargingPlayers) {
                PlayerRef pRef = activePlayerRefs.get(uuid);
                if (pRef == null || pRef.getWorldUuid() == null) continue;

                World world = Universe.get().getWorld(pRef.getWorldUuid());
                if (world == null) continue;

                world.execute(() -> tickChakraRecharge(pRef));
            }
        }, 0, 200, TimeUnit.MILLISECONDS);
    }

    public void updateChakraHud(PlayerRef playerRef) {
        if (playerRef == null) return;

        Ref<EntityStore> ref = playerRef.getReference();
        if (ref == null || !ref.isValid()) return;

        Player playerComp = ref.getStore().getComponent(ref, Player.getComponentType());
        if (playerComp != null) {
            ChakraHud.show(playerComp, playerRef);
        }
    }

    public void tickPlayerCombo(PlayerRef playerRef) {
        UUID uuid = playerRef.getUuid();
        long now = System.currentTimeMillis();

        if (lastClickTimes.containsKey(uuid)) {
            long lastClick = lastClickTimes.get(uuid);
            if (now - lastClick > COMBO_TIMEOUT_MS) {
                resetComboData(playerRef);
            }
        }

        if (hudHideTimes.containsKey(uuid)) {
            long hideAt = hudHideTimes.get(uuid);
            if (now >= hideAt) {
                hudHideTimes.remove(uuid);
                hideComboHud(playerRef);
            }
        }
    }

    public void ensureChakraHudLoaded(PlayerRef playerRef) {
        if (playerRef == null) return;
        UUID uuid = playerRef.getUuid();

        if (activeChakraHuds.containsKey(uuid)) return;

        Ref<EntityStore> ref = playerRef.getReference();
        if (ref == null || !ref.isValid()) return;

        Player playerComp = ref.getStore().getComponent(ref, Player.getComponentType());
        if (playerComp != null) {
            ChakraHud hud = new ChakraHud(playerRef);
            ChakraHud.show(playerComp, playerRef);
            updateChakraHud(playerRef);
            activeChakraHuds.put(uuid, hud);
        }
    }

    public void startCharging(PlayerRef playerRef) {
        if (playerRef == null) return;
        UUID uuid = playerRef.getUuid();

        activePlayerRefs.put(uuid, playerRef);
        chargingPlayers.add(uuid);
    }

    public void stopCharging(PlayerRef playerRef) {
        if (playerRef == null) return;
        UUID uuid = playerRef.getUuid();

        if (chargingPlayers.remove(uuid)) {
            activePlayerRefs.remove(uuid);
        }
    }

    public void tickChakraRecharge(PlayerRef playerRef) {
        PlayerData data = plugin.getDataManager().getPlayerData(playerRef.getUuid());
        if (data == null) return;

        float current = data.getCurrentChakra();
        float max = data.getMaxChakra();

        if (current >= max) return;

        float baseRecharge = 2.0f;
        float controlBonus = (data.getChakraControl() - 1.0f) * 1.0f;
        float totalRecharge = baseRecharge + Math.max(0.0f, controlBonus);

        float nextChakra = Math.min(max, current + totalRecharge);
        data.setCurrentChakra(nextChakra);

        updateChakraHud(playerRef);
    }

    // --- CONTROLE DE COMBO E CLIQUES ---
    public void processClick(PlayerRef playerRef, boolean isRightClick) {
        UUID uuid = playerRef.getUuid();
        long now = System.currentTimeMillis();

        if (lastClickTimes.containsKey(uuid)) {
            long timeSinceLastClick = now - lastClickTimes.get(uuid);

            if (timeSinceLastClick < DEBOUNCE_MS) return;

            if (timeSinceLastClick > COMBO_TIMEOUT_MS) {
                resetCombo(playerRef);
            }
        }

        String currentCombo = activeCombos.getOrDefault(uuid, "");
        String inputTag = isRightClick ? "R" : "L";

        currentCombo = currentCombo.isEmpty() ? inputTag : currentCombo + "-" + inputTag;

        activeCombos.put(uuid, currentCombo);
        lastClickTimes.put(uuid, now);

        String formattedSequence = currentCombo.replace("L", "M1").replace("R", "F").replace("-", " -> ");
        updateComboHud(playerRef, formattedSequence, "", 0);

        if (currentCombo.startsWith("L")) {
            checkComboExecution(playerRef, currentCombo);
            return;
        }

        if (currentCombo.startsWith("R")) {
            checkTransformationExecution(playerRef, currentCombo);
        }
    }

    // --- PROCESSA COMBOS DE JUTSU (Iniciados com L) ---
    private void checkComboExecution(PlayerRef playerRef, String combo) {
        PlayerData playerData = plugin.getDataManager().getPlayerData(playerRef.getUuid());
        if (playerData == null) return;

        String slot = null;
        if (combo.equals("L-L-L")) slot = "slot_1";
        if (combo.equals("L-L-R")) slot = "slot_2";
        if (combo.equals("L-R-L")) slot = "slot_3";
        if (combo.equals("L-R-R")) slot = "slot_4";

        if (slot == null) {
            if (combo.split("-").length >= 3) {
                resetComboData(playerRef);
            }
            return;
        }

        String equippedJutsuId = playerData.getEquippedHotbar().get(slot);

        if (equippedJutsuId == null) {
            playerRef.sendMessage(Message.raw("No Jutsu equipped on " + slot + "!").color(Color.RED));
            resetComboData(playerRef);
            return;
        }

        if (!playerData.hasJutsuUnlocked(equippedJutsuId)) {
            playerRef.sendMessage(Message.raw("You haven't learned this slot's Jutsu yet!").color(Color.RED));
            resetComboData(playerRef);
            return;
        }

        triggerEquippedJutsu(playerRef, equippedJutsuId, combo);
    }

    private void checkTransformationExecution(PlayerRef playerRef, String combo) {
        if (combo.split("-").length < 3) return;

        boolean handled = plugin.getClanManager().handleTransformationCombo(playerRef, combo);
        if (!handled) {
            resetComboData(playerRef);
        }
    }

    private void triggerEquippedJutsu(PlayerRef playerRef, String jutsuId, String currentCombo) {
        Jutsu jutsu = jutsuRegistry.get(jutsuId);

        if (jutsu == null) {
            plugin.getLogger().atWarning().log("[JutsuManager] Jutsu com ID '" + jutsuId + "' não foi registrado.");
            resetComboData(playerRef);
            return;
        }

        UUID uuid = playerRef.getUuid();

        if (Main.getCooldownManager().isOnCooldown(uuid, jutsuId)) {
            float remaining = Main.getCooldownManager().getRemainingSeconds(uuid, jutsuId);
            playerRef.sendMessage(Message.raw("Please wait " + String.format("%.1f", remaining) + "s before using this Jutsu again.").color(Color.RED));
            resetComboData(playerRef);
            return;
        }

        float jutsuCost = jutsu.getChakraCost(playerRef);
        float currentChakra = ChakraUtils.getCurrentChakra(playerRef);

        if (currentChakra < jutsuCost) {
            playerRef.sendMessage(Message.raw("Insufficient chakra: " + (int) jutsuCost).color(Color.RED));
            resetComboData(playerRef);
            return;
        }

        Ref<EntityStore> ref = playerRef.getReference();
        if (ref == null || !ref.isValid()) return;

        Store<EntityStore> store = ref.getStore();
        EntityStore entityStore = store.getExternalData();
        World world = entityStore.getWorld();

        if (jutsuCost > 0) {
            ChakraUtils.consumeChakra(playerRef, jutsuCost);
            updateChakraHud(playerRef);
        }

        jutsu.execute(playerRef, ref, store, world);

        JutsuType jutsuType = JutsuType.fromId(jutsuId);
        if (jutsuType != null && jutsuType.getCooldown() > 0) {
            Main.getCooldownManager().setCooldown(uuid, jutsuId, jutsuType.getCooldown());
        }

        PlayerData playerData = plugin.getDataManager().getPlayerData(uuid);
        if (playerData != null) {
            boolean leveledUp = playerData.addJutsuXp(jutsuId, 15.0f);

            if (leveledUp) {
                int newLvl = playerData.getJutsuLevel(jutsuId);
                playerRef.sendMessage(Message.raw("Your jutsu [" + jutsu.getDisplayName() + "] up to the level of " + newLvl + "!").color(Color.YELLOW));
            }
        }

        String formattedSequence = currentCombo.replace("L", "M1").replace("R", "F").replace("-", " -> ");
        updateComboHud(playerRef, formattedSequence, jutsu.getDisplayName(), JUTSU_HUD_DISPLAY_MS);

        activeCombos.remove(uuid);
        lastClickTimes.remove(uuid);
    }

    void updateComboHud(PlayerRef playerRef, String sequence, String jutsuName, long displayDurationMs) {
        if (playerRef == null || playerRef.getReference() == null || !playerRef.getReference().isValid()) return;

        Player playerComp = playerRef.getReference().getStore().getComponent(playerRef.getReference(), Player.getComponentType());
        if (playerComp == null) return;

        ComboHud.show(playerComp, playerRef, sequence, jutsuName);

        UUID uuid = playerRef.getUuid();
        if (displayDurationMs > 0) {
            hudHideTimes.put(uuid, System.currentTimeMillis() + displayDurationMs);
            return;
        }

        hudHideTimes.remove(uuid);
    }

    private void hideComboHud(PlayerRef playerRef) {
        if (playerRef == null) return;

        Ref<EntityStore> ref = playerRef.getReference();
        if (ref == null || !ref.isValid()) return;

        Player playerComp = playerRef.getReference().getStore().getComponent(playerRef.getReference(), Player.getComponentType());
        if (playerComp != null) {
            ComboHud.hide(playerComp, playerRef);
        }
    }

    public void resetCombo(PlayerRef playerRef) {
        if (playerRef == null) return;
        resetComboData(playerRef);
    }

    public void resetComboData(PlayerRef playerRef) {
        if (playerRef == null) return;

        UUID uuid = playerRef.getUuid();

        if (activeCombos.containsKey(uuid) || lastClickTimes.containsKey(uuid)) {
            activeCombos.remove(uuid);
            lastClickTimes.remove(uuid);
            hudHideTimes.remove(uuid);
            playerRef.sendMessage(Message.raw("The combo reset").color(Color.green));
            hideComboHud(playerRef);
        }
    }

    /**
     * FIX: novo método, package-private. Limpa o estado do combo (sem mandar mensagem de
     * "combo reset" nem esconder o HUD) - usado depois que um combo foi executado com
     * SUCESSO (jutsu normal ou, agora, jutsu/transformação de clã via ClanManager).
     */
    void clearComboState(PlayerRef playerRef) {
        if (playerRef == null) return;
        UUID uuid = playerRef.getUuid();
        activeCombos.remove(uuid);
        lastClickTimes.remove(uuid);
    }

    public void unregisterPlayer(UUID uuid) {
        PlayerRef pRef = activePlayerRefs.remove(uuid);
        if (pRef != null) {
            resetComboData(pRef);
        }
        chargingPlayers.remove(uuid);
        hudHideTimes.remove(uuid);
        activeChakraHuds.remove(uuid);
    }
}