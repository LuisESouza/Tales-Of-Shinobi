package com.cachorrovascaino.plugin.Features.Taijutsu;

import com.cachorrovascaino.plugin.Abstractions.Jutsu;
import com.cachorrovascaino.plugin.Abstractions.SkillType;
import com.cachorrovascaino.plugin.Data.Jutsus.JutsuType;
import com.cachorrovascaino.plugin.Data.PlayerData;
import com.cachorrovascaino.plugin.Main;
import com.cachorrovascaino.plugin.Systems.DamageTrackingSystem;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.spatial.SpatialResource;
import com.hypixel.hytale.math.vector.Rotation3f;
import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.modules.entity.EntityModule;
import com.hypixel.hytale.server.core.modules.entity.component.HeadRotation;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageCause;
import com.hypixel.hytale.server.core.modules.entity.teleport.Teleport;
import com.hypixel.hytale.server.core.modules.physics.component.Velocity;
import com.hypixel.hytale.server.core.modules.physics.util.PhysicsMath;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.ParticleUtil;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.joml.Vector3d;

import java.awt.Color;
import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class PrimaryLotus implements Jutsu {

    public static final PrimaryLotus INSTANCE = new PrimaryLotus();

    private static final double DASH_RANGE = 10.0;
    private static final float BASE_DAMAGE = 180.0f;
    private static final float DAMAGE_PER_LEVEL = 30.0f;
    private static final String PARTICLE_ID = "Leaf_Hurricane_Circle";

    private static final ScheduledExecutorService SCHEDULER = Executors.newScheduledThreadPool(2);

    private static Method TAKE_COMMAND_BUFFER_METHOD;
    private static Method CONSUME_METHOD;

    static {
        try {
            TAKE_COMMAND_BUFFER_METHOD = Store.class.getDeclaredMethod("takeCommandBuffer");
            TAKE_COMMAND_BUFFER_METHOD.setAccessible(true);

            CONSUME_METHOD = CommandBuffer.class.getDeclaredMethod("consume");
            CONSUME_METHOD.setAccessible(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override public String getId() { return "primary_lotus"; }
    @Override public String getDisplayName() { return JutsuType.PRIMARY_LOTUS.getName(); }
    @Override public float getChakraCost() { return JutsuType.PRIMARY_LOTUS.getResourceCost(); }
    @Override public float getCooldown() { return JutsuType.PRIMARY_LOTUS.getCooldown(); }
    @Override public SkillType getType() { return SkillType.TAIJUTSU; }
    @Override public float getChakraCost(PlayerRef playerRef) {return getChakraCost();}

    public float getDamageForPlayer(PlayerRef playerRef) {
        PlayerData data = Main.getDataManager().getPlayerData(playerRef.getUuid());
        int level = (data != null) ? data.getJutsuLevel(getId()) : 1;
        return BASE_DAMAGE + ((level - 1) * DAMAGE_PER_LEVEL);
    }

    @Override
    public void execute(PlayerRef playerRef, Ref<EntityStore> playerEntityRef, Store<EntityStore> store, World world) {
        if (playerRef == null || playerEntityRef == null || !playerEntityRef.isValid()) return;

        TransformComponent playerTransform = store.getComponent(playerEntityRef, TransformComponent.getComponentType());
        if (playerTransform == null) return;

        HeadRotation headRotation = store.getComponent(playerEntityRef, HeadRotation.getComponentType());
        Rotation3f originalRotation = (headRotation != null) ? headRotation.getRotation() : playerTransform.getRotation();

        Vector3d lookVector = new Vector3d();
        PhysicsMath.vectorFromAngles(originalRotation.yaw(), originalRotation.pitch(), lookVector);
        lookVector.normalize();

        Vector3d searchCenter = new Vector3d(playerTransform.getPosition()).add(new Vector3d(lookVector).mul(3.0));
        SpatialResource<Ref<EntityStore>, EntityStore> spatial = store.getResource(EntityModule.get().getEntitySpatialResourceType());

        @SuppressWarnings("unchecked")
        List<Ref<EntityStore>> nearbyEntities = (List<Ref<EntityStore>>) (List<?>) SpatialResource.getThreadLocalReferenceList();
        spatial.getSpatialStructure().collect(searchCenter, DASH_RANGE, nearbyEntities);

        Ref<EntityStore> targetRef = null;
        for (Ref<EntityStore> entity : nearbyEntities) {
            if (entity != null && entity.isValid() && !entity.equals(playerEntityRef)) {
                targetRef = entity;
                break;
            }
        }

        if (targetRef == null) {
            playerRef.sendMessage(Message.raw(" Omote Renge falhou: nenhum alvo ao alcance.").color(Color.GRAY));
            return;
        }

        playerRef.sendMessage(Message.raw(" Omote Renge!").color(Color.RED));

        final Ref<EntityStore> finalTargetRef = targetRef;
        float damage = getDamageForPlayer(playerRef);

        Vector3d dashDirection = new Vector3d(lookVector.x, 0.1, lookVector.z).normalize().mul(20.0);
        applyVelocity(store, playerEntityRef, dashDirection);

        SCHEDULER.schedule(() -> {
            world.execute(() -> {
                if (!playerEntityRef.isValid() || !finalTargetRef.isValid()) {
                    return;
                }

                TransformComponent targetTrans = store.getComponent(finalTargetRef, TransformComponent.getComponentType());
                if (targetTrans == null) return;

                final double initialGroundY = playerTransform.getPosition().y;

                final long startTime = System.currentTimeMillis();
                final double targetAirHeight = 8.0;

                ScheduledFuture<?>[] ascendTask = new ScheduledFuture<?>[1];
                ascendTask[0] = SCHEDULER.scheduleAtFixedRate(() -> {
                    world.execute(() -> {
                        if (!playerEntityRef.isValid() || !finalTargetRef.isValid()) {
                            if (ascendTask[0] != null) ascendTask[0].cancel(false);
                            return;
                        }

                        long elapsed = System.currentTimeMillis() - startTime;

                        if (elapsed >= 400) {
                            if (ascendTask[0] != null) ascendTask[0].cancel(false);

                            applyVelocity(store, playerEntityRef, new Vector3d(0.0, -28.0, 0.0));
                            applyVelocity(store, finalTargetRef, new Vector3d(0.0, -28.0, 0.0));

                            SCHEDULER.schedule(() -> {
                                world.execute(() -> {
                                    if (!finalTargetRef.isValid()) return;

                                    TransformComponent curTargetTrans = store.getComponent(finalTargetRef, TransformComponent.getComponentType());
                                    Vector3d impactPos = (curTargetTrans != null) ? new Vector3d(curTargetTrans.getPosition()) : new Vector3d(playerTransform.getPosition());

                                    if (impactPos.y < initialGroundY) {
                                        impactPos.y = initialGroundY;
                                    }

                                    Vector3d finalPos = new Vector3d(impactPos.x, impactPos.y + 0.1, impactPos.z);

                                    teleportEntity(world, store, finalTargetRef, finalPos, originalRotation);
                                    teleportEntity(world, store, playerEntityRef, finalPos, originalRotation);

                                    applyVelocity(store, playerEntityRef, new Vector3d(0, 0, 0));
                                    applyVelocity(store, finalTargetRef, new Vector3d(0, 0, 0));

                                    applyDamage(playerRef, playerEntityRef, finalTargetRef, damage, store);
                                    spawnImpactParticles(impactPos, store, originalRotation);
                                });
                            }, 280, TimeUnit.MILLISECONDS);

                            return;
                        }

                        double progress = elapsed / 400.0;
                        double currentHeight = initialGroundY + (targetAirHeight * progress);

                        TransformComponent tTrans = store.getComponent(finalTargetRef, TransformComponent.getComponentType());
                        if (tTrans != null) {
                            Vector3d targetPos = new Vector3d(tTrans.getPosition().x, currentHeight, tTrans.getPosition().z);
                            Vector3d playerPos = new Vector3d(targetPos).sub(new Vector3d(lookVector).mul(0.4));

                            teleportEntity(world, store, finalTargetRef, targetPos, originalRotation);
                            teleportEntity(world, store, playerEntityRef, playerPos, originalRotation);
                        }

                        applyVelocity(store, playerEntityRef, new Vector3d(0.0, 10.0, 0.0));
                        applyVelocity(store, finalTargetRef, new Vector3d(0.0, 10.0, 0.0));
                    });
                }, 0, 30, TimeUnit.MILLISECONDS);
            });
        }, 250, TimeUnit.MILLISECONDS);
    }

    private void teleportEntity(World world, Store<EntityStore> store, Ref<EntityStore> entityRef, Vector3d position, Rotation3f rotation) {
        if (entityRef == null || !entityRef.isValid()) return;

        Transform targetTransform = new Transform(
                position.x, position.y, position.z,
                0.0f,
                rotation != null ? rotation.yaw() : 0.0f,
                0.0f
        );

        Teleport teleportComponent = Teleport.createForPlayer(world, targetTransform);
        store.addComponent(entityRef, Teleport.getComponentType(), teleportComponent);

        HeadRotation head = store.getComponent(entityRef, HeadRotation.getComponentType());
        if (head != null && rotation != null) {
            head.setRotation(new Rotation3f(0.0f, rotation.yaw(), 0.0f));
        }
    }

    private void applyVelocity(Store<EntityStore> store, Ref<EntityStore> entityRef, Vector3d velocity) {
        if (entityRef == null || !entityRef.isValid()) return;

        try {
            @SuppressWarnings("unchecked")
            CommandBuffer<EntityStore> commandBuffer = (CommandBuffer<EntityStore>) TAKE_COMMAND_BUFFER_METHOD.invoke(store);

            commandBuffer.run(runStore -> {
                if (!entityRef.isValid()) return;

                Velocity velComponent = runStore.getComponent(entityRef, Velocity.getComponentType());
                if (velComponent != null) {
                    velComponent.set(velocity.x, velocity.y, velocity.z);
                } else {
                    Velocity newVel = new Velocity();
                    newVel.set(velocity.x, velocity.y, velocity.z);
                    runStore.putComponent(entityRef, Velocity.getComponentType(), newVel);
                }
            });

            CONSUME_METHOD.invoke(commandBuffer);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void applyDamage(PlayerRef playerRef, Ref<EntityStore> attacker, Ref<EntityStore> victim, float damageAmount, Store<EntityStore> store) {
        DamageCause damageCause = DamageCause.getAssetMap().getAsset("Physical");
        if (damageCause == null) {
            damageCause = DamageCause.getAssetMap().getAsset(0);
        }

        Damage.Source source = new Damage.EntitySource(attacker);
        Damage damageEvent = new Damage(source, damageCause, damageAmount);
        damageEvent.putMetaObject(DamageTrackingSystem.RPG_DAMAGE_PROCESSED, false);

        try {
            @SuppressWarnings("unchecked")
            CommandBuffer<EntityStore> commandBuffer = (CommandBuffer<EntityStore>) TAKE_COMMAND_BUFFER_METHOD.invoke(store);
            commandBuffer.invoke(victim, damageEvent);
            CONSUME_METHOD.invoke(commandBuffer);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void spawnImpactParticles(Vector3d pos, Store<EntityStore> store, Rotation3f rotation) {
        SpatialResource<Ref<EntityStore>, EntityStore> playerSpatial = store.getResource(EntityModule.get().getPlayerSpatialResourceType());

        @SuppressWarnings("unchecked")
        List<Ref<EntityStore>> playersToNotify = (List<Ref<EntityStore>>) (List<?>) SpatialResource.getThreadLocalReferenceList();
        playerSpatial.getSpatialStructure().collect(pos, 75.0, playersToNotify);

        if (!playersToNotify.isEmpty()) {
            Vector3d particlePos = new Vector3d(pos.x, pos.y + 0.2, pos.z);
            ParticleUtil.spawnParticleEffect(PARTICLE_ID, particlePos, rotation, playersToNotify, store);
        }
    }
}