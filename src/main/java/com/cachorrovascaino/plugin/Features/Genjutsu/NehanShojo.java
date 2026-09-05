package com.cachorrovascaino.plugin.Features.Genjutsu;

import com.cachorrovascaino.plugin.Abstractions.Jutsu;
import com.cachorrovascaino.plugin.Abstractions.SkillType;
import com.cachorrovascaino.plugin.Data.Jutsus.JutsuType;
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
import com.hypixel.hytale.server.core.universe.world.ParticleUtil;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.hypixel.hytale.server.npc.role.Role;
import org.joml.Vector3d;

import java.awt.Color;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class NehanShojo implements Jutsu {

    public static final NehanShojo INSTANCE = new NehanShojo();

    private static final double RANGE = 15.0;
    private static final String GENJUTSU_WEATHER = "Kokuangyo_Genjutsu";
    private static final String DEFAULT_WEATHER = "Sun";
    private static final long DURATION_SECONDS = 6;

    private static final String INDICATOR_PARTICLE_ID = "Genjutsu_AoE";

    private static final ScheduledExecutorService SCHEDULER = Executors.newScheduledThreadPool(2);

    @Override public String getId() { return "nehan_shojo_genjutsu"; }
    @Override public String getDisplayName() { return JutsuType.NEHAN_SHOJO.getName(); }
    @Override public float getChakraCost() { return JutsuType.NEHAN_SHOJO.getResourceCost(); }
    @Override public float getCooldown() { return JutsuType.NEHAN_SHOJO.getCooldown(); }
    @Override public SkillType getType() { return SkillType.GENJUTSU; }

    @Override
    public void execute(PlayerRef playerRef, Ref<EntityStore> playerEntityRef, Store<EntityStore> store, World world) {
        if (playerRef == null || playerEntityRef == null || !playerEntityRef.isValid()) return;

        TransformComponent playerTransform = store.getComponent(playerEntityRef, TransformComponent.getComponentType());
        if (playerTransform == null) return;

        HeadRotation headRotation = store.getComponent(playerEntityRef, HeadRotation.getComponentType());
        Rotation3f rotation = (headRotation != null) ? headRotation.getRotation() : playerTransform.getRotation();

        Vector3d lookVector = new Vector3d();
        PhysicsMath.vectorFromAngles(rotation.yaw(), 0.0f, lookVector);
        lookVector.normalize();

        final Vector3d zoneCenter = new Vector3d(playerTransform.getPosition()).add(new Vector3d(lookVector).mul(4.0));

        Set<Ref<EntityStore>> trappedEntities = new HashSet<>();
        AtomicBoolean isActive = new AtomicBoolean(true);

        playerRef.sendMessage(Message.raw(" Nehan Shōjō no Jutsu! Área de ilusão criada.").color(Color.GREEN));

        ScheduledFuture<?> zoneTask = SCHEDULER.scheduleAtFixedRate(() -> {
            if (!isActive.get()) return;

            world.execute(() -> {
                try {
                    spawnSingleAoeParticle(zoneCenter, store, rotation);

                    SpatialResource<Ref<EntityStore>, EntityStore> spatial = store.getResource(EntityModule.get().getEntitySpatialResourceType());
                    if (spatial == null) {
                        spatial = store.getResource(EntityModule.get().getPlayerSpatialResourceType());
                    }

                    if (spatial == null) return;

                    @SuppressWarnings("unchecked")
                    List<Ref<EntityStore>> nearbyEntities = (List<Ref<EntityStore>>) (List<?>) SpatialResource.getThreadLocalReferenceList();
                    spatial.getSpatialStructure().collect(zoneCenter, RANGE, nearbyEntities);

                    ComponentType<EntityStore, Frozen> frozenType = Frozen.getComponentType();

                    for (Ref<EntityStore> entityRef : nearbyEntities) {
                        if (entityRef == null || !entityRef.isValid() || entityRef.equals(playerEntityRef)) {
                            continue;
                        }

                        PlayerRef targetPlayer = store.getComponent(entityRef, PlayerRef.getComponentType());

                        if (!trappedEntities.contains(entityRef)) {
                            trappedEntities.add(entityRef);

                            if (targetPlayer != null) {
                                PacketHandler packetHandler = targetPlayer.getPacketHandler();
                                WeatherUtils.applyPlayerWeather(targetPlayer, packetHandler, GENJUTSU_WEATHER);

                                Player targetPlayerComponent = store.getComponent(entityRef, Player.getComponentType());
                                if (targetPlayerComponent != null) {
                                    targetPlayerComponent.executeTriggers = false;
                                }

                                PlayerMovementUtils.freezePlayer(entityRef, store);
                                targetPlayer.sendMessage(Message.raw(" Você entrou na área do Nehan Shōjō e foi paralisado!").color(Color.DARK_GRAY));
                            } else {
                                if (frozenType != null) {
                                    store.putComponent(entityRef, frozenType, Frozen.get());
                                }
                                suppressMobAI(entityRef, store);
                            }
                        }

                        if (targetPlayer != null) {
                            PlayerInput playerInput = store.getComponent(entityRef, PlayerInput.getComponentType());
                            if (playerInput != null) {
                                playerInput.getMovementUpdateQueue().clear();
                            }

                            Velocity velocity = store.getComponent(entityRef, Velocity.getComponentType());
                            if (velocity != null) {
                                velocity.getVelocity().set(0.0, 0.0, 0.0);
                            }
                        }
                    }
                } catch (Exception ignored) {
                }
            });
        }, 0, 100, TimeUnit.MILLISECONDS);

        SCHEDULER.schedule(() -> {
            isActive.set(false);
            zoneTask.cancel(true);

            world.execute(() -> {
                ComponentType<EntityStore, Frozen> frozenType = Frozen.getComponentType();

                for (Ref<EntityStore> entityRef : trappedEntities) {
                    if (entityRef != null && entityRef.isValid()) {

                        PlayerRef targetPlayer = store.getComponent(entityRef, PlayerRef.getComponentType());
                        if (targetPlayer != null) {
                            PacketHandler packetHandler = targetPlayer.getPacketHandler();

                            Player targetPlayerComponent = store.getComponent(entityRef, Player.getComponentType());
                            if (targetPlayerComponent != null) {
                                targetPlayerComponent.executeTriggers = true;
                            }

                            PlayerMovementUtils.unfreezePlayer(entityRef, store);
                            WeatherUtils.resetPlayerWeather(targetPlayer, packetHandler, DEFAULT_WEATHER);
                            targetPlayer.sendMessage(Message.raw(" O efeito do Nehan Shōjō se dissipou.").color(Color.GRAY));
                        } else {
                            if (frozenType != null) {
                                store.removeComponent(entityRef, frozenType);
                            }
                        }
                    }
                }
                trappedEntities.clear();
            });
        }, DURATION_SECONDS, TimeUnit.SECONDS);
    }

    private void spawnSingleAoeParticle(Vector3d center, Store<EntityStore> store, Rotation3f rotation) {
        SpatialResource<Ref<EntityStore>, EntityStore> playerSpatial = store.getResource(EntityModule.get().getPlayerSpatialResourceType());

        @SuppressWarnings("unchecked")
        List<Ref<EntityStore>> playersToNotify = (List<Ref<EntityStore>>) (List<?>) SpatialResource.getThreadLocalReferenceList();
        playerSpatial.getSpatialStructure().collect(center, 85.0, playersToNotify);

        if (playersToNotify.isEmpty()) return;

        double x = center.x();
        double y = center.y() + 0.2;
        double z = center.z();

        ParticleUtil.spawnParticleEffect(
                INDICATOR_PARTICLE_ID,
                x, y, z,
                rotation.yaw(), rotation.pitch(), rotation.roll(),
                2,
                null,
                null,
                playersToNotify,
                store
        );
    }

    private void suppressMobAI(Ref<EntityStore> mobRef, Store<EntityStore> store) {
        if (NPCEntity.getComponentType() == null) return;
    }
}