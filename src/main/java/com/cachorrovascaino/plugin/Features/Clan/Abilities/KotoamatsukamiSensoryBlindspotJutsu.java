package com.cachorrovascaino.plugin.Features.Clan.Abilities;

import com.cachorrovascaino.plugin.Abstractions.ClanJutsu;
import com.cachorrovascaino.plugin.Data.Components.MangekyouSharingan;
import com.cachorrovascaino.plugin.Data.PlayerData;
import com.cachorrovascaino.plugin.Main;
import com.cachorrovascaino.plugin.Utils.TargetUtils;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.modules.entity.component.HiddenFromAdventurePlayers;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.joml.Vector3d;

import java.awt.Color;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class KotoamatsukamiSensoryBlindspotJutsu implements ClanJutsu {

    public static final KotoamatsukamiSensoryBlindspotJutsu INSTANCE = new KotoamatsukamiSensoryBlindspotJutsu();
    private static final ScheduledExecutorService SCHEDULER = Executors.newSingleThreadScheduledExecutor();
    private static final long DURATION_SECONDS = 4;
    private static final double RADIUS = 15.0;

    private KotoamatsukamiSensoryBlindspotJutsu() {}

    @Override public String getId() { return "kotoamatsukami_blindspot"; }
    @Override public String getDisplayName() { return "Kotoamatsukami: Sensory Blindspot"; }
    @Override public float getChakraCost() { return 130.0f; }
    @Override public float getCooldown() { return 30.0f; }

    @Override
    public boolean canExecute(PlayerRef playerRef) {
        PlayerData playerData = Main.getDataManager().getPlayerData(playerRef.getUuid());
        if (playerData == null) return false;

        if (!"SHISUI".equalsIgnoreCase(playerData.getMangekyouType())) {
            playerRef.sendMessage(Message.raw("This Mangekyō Sharingan does not possess Sensory Blindspot!").color(Color.RED));
            return false;
        }

        return true;
    }

    @Override
    public void execute(PlayerRef playerRef, Ref<EntityStore> playerEntityRef, Store<EntityStore> store, World world) {
        if (playerEntityRef == null || !playerEntityRef.isValid() || world == null) return;
        if (!canExecute(playerRef)) return;

        ComponentType<EntityStore, MangekyouSharingan> mangekyouType = Main.get().getMangekyouSharinganComponentType();

        world.execute(() -> {
            if (!playerEntityRef.isValid()) return;

            if (mangekyouType == null || store.getComponent(playerEntityRef, mangekyouType) == null) {
                playerRef.sendMessage(Message.raw("Activate Mangekyō Sharingan to use Kotoamatsukami!").color(Color.RED));
                return;
            }

            TransformComponent playerTransform = store.getComponent(playerEntityRef, TransformComponent.getComponentType());
            if (playerTransform == null) return;

            Vector3d playerPos = playerTransform.getPosition();

            List<Ref<EntityStore>> targets = TargetUtils.getEntitiesInRadius(playerPos, RADIUS, store);

            targets.remove(playerEntityRef);

            if (targets.isEmpty()) {
                playerRef.sendMessage(Message.raw("Sensory Blindspot: No targets nearby.").color(Color.GRAY));
                return;
            }

            for (Ref<EntityStore> targetRef : targets) {
                if (targetRef != null && targetRef.isValid()) {
                    store.addComponent(targetRef, HiddenFromAdventurePlayers.getComponentType(), HiddenFromAdventurePlayers.INSTANCE);
                }
            }

            playerRef.sendMessage(Message.raw("Kotoamatsukami: Sensory Blindspot activated on " + targets.size() + " target(s)!").color(Color.GREEN));

            SCHEDULER.schedule(() -> {
                world.execute(() -> {
                    for (Ref<EntityStore> targetRef : targets) {
                        if (targetRef != null && targetRef.isValid()) {
                            store.removeComponent(targetRef, HiddenFromAdventurePlayers.getComponentType());
                        }
                    }
                    if (playerEntityRef.isValid()) {
                        playerRef.sendMessage(Message.raw("Sensory Blindspot effect has ended.").color(Color.YELLOW));
                    }
                });
            }, DURATION_SECONDS, TimeUnit.SECONDS);
        });
    }
}