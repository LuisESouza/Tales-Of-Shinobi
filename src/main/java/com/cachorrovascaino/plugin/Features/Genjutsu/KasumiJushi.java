package com.cachorrovascaino.plugin.Features.Genjutsu;

import com.cachorrovascaino.plugin.Abstractions.Jutsu;
import com.cachorrovascaino.plugin.Abstractions.SkillType;
import com.cachorrovascaino.plugin.Data.Jutsus.JutsuType;
import com.cachorrovascaino.plugin.Utils.CloneJutsuUtils;
import com.cachorrovascaino.plugin.Utils.PlayerMovementUtils;
import com.cachorrovascaino.plugin.Utils.WeatherUtils;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.spatial.SpatialResource;
import com.hypixel.hytale.math.vector.Rotation3f;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.Frozen;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.io.PacketHandler;
import com.hypixel.hytale.server.core.modules.entity.EntityModule;
import com.hypixel.hytale.server.core.modules.entity.component.HeadRotation;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.player.PlayerInput;
import com.hypixel.hytale.server.core.modules.physics.component.Velocity;
import com.hypixel.hytale.server.core.modules.physics.util.PhysicsMath;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.hypixel.hytale.server.npc.role.Role;
import org.joml.Vector3d;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class KasumiJushi implements Jutsu {

    public static final KasumiJushi INSTANCE = new KasumiJushi();

    private static final double RANGE = 15.0;
    private static final String GENJUTSU_WEATHER = "Kasumi_Genjutsu";
    private static final String DEFAULT_WEATHER = "Sun";
    private static final long DURATION_SECONDS = 6;
    private static final String CLONE_ROLE = "ShadowClone";

    private static final ScheduledExecutorService SCHEDULER = Executors.newScheduledThreadPool(2);

    @Override public String getId() { return "kasumi_jushi_genjutsu"; }
    @Override public String getDisplayName() { return JutsuType.KASUMI_JUSHI.getName(); }
    @Override public float getChakraCost() { return JutsuType.KASUMI_JUSHI.getResourceCost(); }
    @Override public float getCooldown() { return JutsuType.KASUMI_JUSHI.getCooldown(); }
    @Override public SkillType getType() { return SkillType.GENJUTSU; }

    @Override
    public void execute(PlayerRef playerRef, Ref<EntityStore> playerEntityRef, Store<EntityStore> store, World world) {
        if (playerRef == null || playerEntityRef == null || !playerEntityRef.isValid()) return;

        TransformComponent playerTransform = store.getComponent(playerEntityRef, TransformComponent.getComponentType());
        if (playerTransform == null) return;

        HeadRotation headRotation = store.getComponent(playerEntityRef, HeadRotation.getComponentType());
        Rotation3f rotation = (headRotation != null) ? headRotation.getRotation() : playerTransform.getRotation();

        Vector3d lookVector = new Vector3d();
        PhysicsMath.vectorFromAngles(rotation.yaw(), rotation.pitch(), lookVector);
        lookVector.normalize();

        Vector3d searchCenter = new Vector3d(playerTransform.getPosition()).add(new Vector3d(lookVector).mul(3.0));
        SpatialResource<Ref<EntityStore>, EntityStore> spatial = store.getResource(EntityModule.get().getEntitySpatialResourceType());

        if (spatial == null) {
            spatial = store.getResource(EntityModule.get().getPlayerSpatialResourceType());
        }

        Ref<EntityStore> targetEntityRef = null;
        PlayerRef targetPlayerRef = null;

        @SuppressWarnings("unchecked")
        List<Ref<EntityStore>> nearbyEntities = (List<Ref<EntityStore>>) (List<?>) SpatialResource.getThreadLocalReferenceList();
        spatial.getSpatialStructure().collect(searchCenter, RANGE, nearbyEntities);

        for (Ref<EntityStore> entity : nearbyEntities) {
            if (entity != null && entity.isValid() && !entity.equals(playerEntityRef)) {
                targetEntityRef = entity;
                targetPlayerRef = store.getComponent(entity, PlayerRef.getComponentType());
                break;
            }
        }

        // Se nenhum alvo for encontrado, cancela a execução sem afetar o conjurador
        if (targetEntityRef == null) {
            playerRef.sendMessage(Message.raw("No valid target found for Kasumi Jūshi!").color(Color.RED));
            return;
        }

        TransformComponent targetTransform = store.getComponent(targetEntityRef, TransformComponent.getComponentType());
        Vector3d targetPos = (targetTransform != null) ? targetTransform.getPosition() : playerTransform.getPosition();

        double[][] offsets = {{2.0, 2.0}, {-2.0, 2.0}, {2.0, -2.0}, {-2.0, -2.0}};
        List<Ref<EntityStore>> spawnedClones = new ArrayList<>();

        for (double[] offset : offsets) {
            Ref<EntityStore> cloneRef = CloneJutsuUtils.spawnClone(
                    playerRef, playerEntityRef, store, world,
                    (targetPos.x - playerTransform.getPosition().x) + offset[0],
                    (targetPos.z - playerTransform.getPosition().z) + offset[1],
                    DURATION_SECONDS, CLONE_ROLE, null
            );

            if (cloneRef != null && cloneRef.isValid()) {
                spawnedClones.add(cloneRef);
                CloneJutsuUtils.forceCloneAttack(cloneRef, targetEntityRef, store);
            }
        }

        final Ref<EntityStore> finalTargetEntityRef = targetEntityRef;

        if (targetPlayerRef != null) {
            PacketHandler targetPacketHandler = targetPlayerRef.getPacketHandler();
            WeatherUtils.applyPlayerWeather(targetPlayerRef, targetPacketHandler, GENJUTSU_WEATHER);

            Player targetPlayerComponent = store.getComponent(finalTargetEntityRef, Player.getComponentType());
            if (targetPlayerComponent != null) {
                targetPlayerComponent.executeTriggers = false;
            }

            PlayerMovementUtils.freezePlayer(finalTargetEntityRef, store);

            playerRef.sendMessage(Message.raw("You trapped " + targetPlayerRef.getUsername() + " in Kasumi Jūshi!").color(Color.GREEN));
            targetPlayerRef.sendMessage(Message.raw("You fell into Kasumi Jūshi! Your body and movement are completely paralyzed.").color(Color.DARK_GRAY));

            AtomicBoolean isActive = new AtomicBoolean(true);

            ScheduledFuture<?> freezeTask = SCHEDULER.scheduleAtFixedRate(() -> {
                if (!isActive.get()) return;

                world.execute(() -> {
                    try {
                        if (!finalTargetEntityRef.isValid()) return;

                        PlayerInput playerInput = store.getComponent(finalTargetEntityRef, PlayerInput.getComponentType());
                        if (playerInput != null) {
                            playerInput.getMovementUpdateQueue().clear();
                        }

                        Velocity velocity = store.getComponent(finalTargetEntityRef, Velocity.getComponentType());
                        if (velocity != null) {
                            velocity.getVelocity().set(0.0, 0.0, 0.0);
                        }
                    } catch (Exception ignored) {
                    }
                });
            }, 0, 50, TimeUnit.MILLISECONDS);

            final PlayerRef finalTargetPlayerRef = targetPlayerRef;

            SCHEDULER.schedule(() -> {
                isActive.set(false);
                freezeTask.cancel(true);

                world.execute(() -> {
                    try {
                        if (finalTargetEntityRef.isValid()) {
                            if (targetPlayerComponent != null) {
                                targetPlayerComponent.executeTriggers = true;
                            }
                            PlayerMovementUtils.unfreezePlayer(finalTargetEntityRef, store);
                        }
                        WeatherUtils.resetPlayerWeather(finalTargetPlayerRef, targetPacketHandler, DEFAULT_WEATHER);
                        finalTargetPlayerRef.sendMessage(Message.raw("The effect of Kasumi Jūshi has dissipated.").color(Color.GRAY));
                    } catch (Exception e) {
                        PlayerMovementUtils.unfreezePlayer(finalTargetEntityRef, store);
                        WeatherUtils.resetPlayerWeather(finalTargetPlayerRef, targetPacketHandler, DEFAULT_WEATHER);
                    }
                });
            }, DURATION_SECONDS, TimeUnit.SECONDS);

        } else {
            ComponentType<EntityStore, Frozen> frozenType = Frozen.getComponentType();
            if (frozenType != null) {
                store.putComponent(targetEntityRef, frozenType, Frozen.get());
            }

            playerRef.sendMessage(Message.raw("The creature was paralyzed by Kasumi Jūshi!").color(Color.GREEN));

            ScheduledFuture<?> mobTask = SCHEDULER.scheduleAtFixedRate(() -> {
                world.execute(() -> {
                    if (!finalTargetEntityRef.isValid()) return;
                    suppressMobAI(finalTargetEntityRef, store);
                });
            }, 0, 200, TimeUnit.MILLISECONDS);

            SCHEDULER.schedule(() -> {
                mobTask.cancel(true);
                world.execute(() -> {
                    if (finalTargetEntityRef.isValid() && frozenType != null) {
                        store.removeComponent(finalTargetEntityRef, frozenType);
                    }
                });
            }, DURATION_SECONDS, TimeUnit.SECONDS);
        }
    }

    private void suppressMobAI(Ref<EntityStore> mobRef, Store<EntityStore> store) {
        if (NPCEntity.getComponentType() == null) return;
        NPCEntity npcEntity = store.getComponent(mobRef, NPCEntity.getComponentType());
        if (npcEntity != null && npcEntity.getRole() != null) {
            Role role = npcEntity.getRole();
            role.setMarkedTarget("target", null);
            role.setMarkedTarget("combatTarget", null);
            role.setMarkedTarget("player", null);
            role.getStateSupport().setState(mobRef, "Idle", null, store);
        }
    }
}