package com.cachorrovascaino.plugin.Features.Clan.Abilities.Hyuga;

import com.cachorrovascaino.plugin.Abstractions.ClanJutsu;
import com.cachorrovascaino.plugin.Data.Components.Byakugan;
import com.cachorrovascaino.plugin.Data.PlayerData;
import com.cachorrovascaino.plugin.Main;
import com.cachorrovascaino.plugin.Utils.TargetUtils;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.spatial.SpatialResource;
import com.hypixel.hytale.protocol.AnimationSlot;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.AnimationUtils;
import com.hypixel.hytale.server.core.modules.entity.EntityModule;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.ParticleUtil;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.joml.Vector3d;

import java.awt.Color;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class KaitenJutsu implements ClanJutsu {

    public static final KaitenJutsu INSTANCE = new KaitenJutsu();
    private static final ScheduledExecutorService SCHEDULER = Executors.newScheduledThreadPool(2);

    private static final String PARTICLE_ID = "kaiten_circle";
    private static final double KAITEN_RADIUS = 6.0;
    private static final long TICK_INTERVAL_MS = 150;
    private static final double MOVE_THRESHOLD = 0.15;
    private static final float CHAKRA_COST_PER_TICK = 4.0f;
    private static final float PARTICLE_DURATION_SEC = 0.5f;

    private KaitenJutsu() {}

    @Override public String getId() { return "kaiten"; }
    @Override public String getDisplayName() { return "Eight Trigrams"; }
    @Override public float getChakraCost() { return CHAKRA_COST_PER_TICK; }
    @Override public float getCooldown() { return 15.0f; }

    @Override
    public boolean canExecute(PlayerRef playerRef) {
        PlayerData playerData = Main.getDataManager().getPlayerData(playerRef.getUuid());
        if (playerData == null) return false;

        if (!"HYUGA".equalsIgnoreCase(playerData.getClan())) {
            playerRef.sendMessage(Message.raw("Only members of the Hyūga Clan can perform Kaiten.").color(Color.RED));
            return false;
        }
        return true;
    }

    @Override
    public void execute(PlayerRef playerRef, Ref<EntityStore> playerEntityRef, Store<EntityStore> store, World world) {
        if (playerEntityRef == null || !playerEntityRef.isValid() || world == null) return;
        if (!canExecute(playerRef)) return;

        UUID uuid = playerRef.getUuid();
        ComponentType<EntityStore, Byakugan> byakuganType = Main.get().getByakuganComponentType();

        world.execute(() -> {
            if (!playerEntityRef.isValid()) return;

            if (byakuganType != null && store.getComponent(playerEntityRef, byakuganType) == null) {
                playerRef.sendMessage(Message.raw("Activate Byakugan to execute Eight Trigrams Palms Revolving Heaven!").color(Color.RED));
                return;
            }

            TransformComponent initialTransform = store.getComponent(playerEntityRef, TransformComponent.getComponentType());
            if (initialTransform == null) return;

            final Vector3d initialPos = new Vector3d(initialTransform.getPosition());

            playerRef.sendMessage(Message.raw("Eight Trigrams Palms Revolving Heaven!").color(Color.CYAN));
            AnimationUtils.playAnimation(playerEntityRef, AnimationSlot.Action, null, "Spin", true, store);

            AtomicBoolean isChanneling = new AtomicBoolean(true);
            ScheduledFuture<?>[] taskHolder = new ScheduledFuture<?>[1];

            taskHolder[0] = SCHEDULER.scheduleAtFixedRate(() -> {
                if (!isChanneling.get()) return;

                world.execute(() -> {
                    try {
                        if (!playerEntityRef.isValid()) {
                            stopKaiten(playerEntityRef, isChanneling, taskHolder[0], store);
                            return;
                        }

                        TransformComponent currentTransform = store.getComponent(playerEntityRef, TransformComponent.getComponentType());
                        if (currentTransform == null) {
                            stopKaiten(playerEntityRef, isChanneling, taskHolder[0], store);
                            return;
                        }

                        Vector3d currentPos = currentTransform.getPosition();

                        if (initialPos.distance(currentPos) > MOVE_THRESHOLD) {
                            stopKaiten(playerEntityRef, isChanneling, taskHolder[0], store);
                            playerRef.sendMessage(Message.raw("Kaiten canceled: You moved!").color(Color.YELLOW));
                            return;
                        }

                        PlayerData playerData = Main.getDataManager().getPlayerData(uuid);
                        if (playerData == null || playerData.getCurrentChakra() < CHAKRA_COST_PER_TICK) {
                            stopKaiten(playerEntityRef, isChanneling, taskHolder[0], store);
                            playerRef.sendMessage(Message.raw("Kaiten canceled: Insufficient chakra!").color(Color.RED));
                            return;
                        }

                        playerData.setCurrentChakra(playerData.getCurrentChakra() - CHAKRA_COST_PER_TICK);
                        Main.getJutsuManager().updateChakraHud(playerRef);

                        spawnKaitenParticles(store, currentPos);

                        List<Ref<EntityStore>> nearbyEntities = TargetUtils.getEntitiesInRadius(currentPos, KAITEN_RADIUS + 2.0, store);

                        for (Ref<EntityStore> targetRef : nearbyEntities) {
                            if (targetRef.equals(playerEntityRef) || !targetRef.isValid()) continue;

                            TransformComponent targetTrans = store.getComponent(targetRef, TransformComponent.getComponentType());
                            if (targetTrans == null) continue;

                            Vector3d targetPos = targetTrans.getPosition();
                            double distance = currentPos.distance(targetPos);

                            if (distance < KAITEN_RADIUS) {
                                Vector3d pushDirection = new Vector3d(targetPos).sub(currentPos);
                                pushDirection.y = 0;

                                if (pushDirection.lengthSquared() > 0.0001) {
                                    pushDirection.normalize();
                                } else {
                                    pushDirection.set(1, 0, 0);
                                }

                                Vector3d repelledPos = new Vector3d(currentPos).add(pushDirection.mul(KAITEN_RADIUS + 0.5));
                                repelledPos.y = targetPos.y;

                                targetTrans.teleportPosition(repelledPos);
                            }
                        }

                    } catch (Exception e) {
                        stopKaiten(playerEntityRef, isChanneling, taskHolder[0], store);
                    }
                });
            }, 0, TICK_INTERVAL_MS, TimeUnit.MILLISECONDS);
        });
    }

    private void stopKaiten(Ref<EntityStore> playerEntityRef, AtomicBoolean isChanneling, ScheduledFuture<?> task, Store<EntityStore> store) {
        isChanneling.set(false);
        if (task != null) {
            task.cancel(false);
        }
        if (playerEntityRef.isValid()) {
            AnimationUtils.playAnimation(playerEntityRef, AnimationSlot.Action, (String) null, true, store);
        }
    }

    private void spawnKaitenParticles(Store<EntityStore> store, Vector3d pos) {
        SpatialResource<Ref<EntityStore>, EntityStore> playerSpatial = store.getResource(EntityModule.get().getPlayerSpatialResourceType());

        if (playerSpatial != null) {
            @SuppressWarnings("unchecked")
            List<Ref<EntityStore>> playersToNotify = (List<Ref<EntityStore>>) (List<?>) SpatialResource.getThreadLocalReferenceList();
            playerSpatial.getSpatialStructure().collect(pos, 85.0, playersToNotify);

            if (!playersToNotify.isEmpty()) {
                Vector3d particlePos = new Vector3d(pos.x, pos.y + 0.2, pos.z);
                ParticleUtil.spawnParticleEffect(
                        PARTICLE_ID,
                        particlePos,
                        0.0f, 0.0f, 0.0f,
                        4.0f,
                        PARTICLE_DURATION_SEC,
                        store
                );
            }
        }
    }
}