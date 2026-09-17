package com.cachorrovascaino.plugin.Listener;

import com.cachorrovascaino.plugin.Main;
import com.cachorrovascaino.plugin.Data.PlayerData;
import com.cachorrovascaino.plugin.Ui.Hud.JutsuEquippedHud;
import com.cachorrovascaino.plugin.Ui.Hud.LevelHud;
import com.cachorrovascaino.plugin.Utils.ChakraUtils;
import com.cachorrovascaino.plugin.Utils.EyesUtils;
import com.cachorrovascaino.plugin.Utils.PlayerStatUtils;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.event.EventRegistry;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.asset.type.model.config.ModelAsset;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.player.PlayerConnectEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class PlayerListener {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static EyesUtils eyesUtils = new EyesUtils();

    public void register(EventRegistry registry) {
        try {
            registry.register(PlayerConnectEvent.class, this::onPlayerConnect);
            registry.register(PlayerDisconnectEvent.class, this::onPlayerDisconnect);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void onPlayerConnect(PlayerConnectEvent event) {}

    private void onPlayerDisconnect(PlayerDisconnectEvent event) {
        PlayerRef playerRef = event.getPlayerRef();
        UUID uuid = playerRef.getUuid();
        Main.getDataManager().unloadPlayer(uuid);
        Main.getJutsuManager().unregisterPlayer(uuid);
    }

    public static void onPlayerReady(PlayerReadyEvent event) {
        Player player = event.getPlayer();
        Ref<EntityStore> ref = event.getPlayerRef();
        World world = player.getWorld();

        Store<EntityStore> entityStore = ref.getStore();
        PlayerRef pRef = entityStore.getComponent(ref, PlayerRef.getComponentType());

        if (pRef != null) {
            UUID uuid = pRef.getUuid();
            String username = pRef.getUsername();

            PlayerData playerData = Main.getDataManager().loadPlayer(uuid, username);

            if (playerData != null) {
                ChakraUtils.initChakraIfFirstTime(pRef, playerData.getCurrentChakra(), playerData.getMaxChakra());
                ChakraUtils.setChakra(pRef, playerData.getCurrentChakra());
            }

            if(playerData == null) return;

            PlayerStatUtils.applyPlayerSpeed(
                    entityStore,
                    ref,
                    pRef,
                    playerData.getSpeed()
            );

            if (world != null) {
                try {
                    if (pRef.isValid() && ref.isValid()) {
                        Main.getJutsuManager().ensureChakraHudLoaded(pRef);
                        Main.getJutsuManager().updateChakraHud(pRef);

                        if ("Hyuga".equalsIgnoreCase(playerData.getClan())) {
                            eyesUtils.captureOriginalEyesIfNeeded(pRef, playerData);

                            if (playerData.getEyeDojutsuType() == null || playerData.getEyeDojutsuType().isEmpty() || "NONE".equalsIgnoreCase(playerData.getEyeDojutsuType())) {
                                playerData.setEyeDojutsuType("Byakugan_HD");
                            }
                            if (playerData.getEyeStage() <= 0) {
                                playerData.setEyeStage(1);
                            }

                            eyesUtils.updateHytalePlayerEyes(world, pRef, playerData.getEyeDojutsuType(), playerData.getEyesColor());

                            Main.getDataManager().savePlayer(uuid);
                        }

                        if (player != null) {
                            LevelHud.update(player, pRef);
                            JutsuEquippedHud.show(player, pRef);
                        }
                    }
                } catch (Exception e) {
                    LOGGER.atWarning().log("Erro ao inicializar HUDs e Stats no PlayerReady: " + e.getMessage());
                }
            }
        } else {
            LOGGER.atWarning().log("PlayerRef veio NULO do EntityStore no PlayerReadyEvent!");
        }
    }
}