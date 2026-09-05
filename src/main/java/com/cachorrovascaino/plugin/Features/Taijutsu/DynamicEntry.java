package com.cachorrovascaino.plugin.Features.Taijutsu;

import com.cachorrovascaino.plugin.Abstractions.Jutsu;
import com.cachorrovascaino.plugin.Abstractions.SkillType;
import com.cachorrovascaino.plugin.Data.Jutsus.JutsuType;
import com.cachorrovascaino.plugin.Data.PlayerData;
import com.cachorrovascaino.plugin.Main;
import com.cachorrovascaino.plugin.Systems.DamageTrackingSystem;
import com.cachorrovascaino.plugin.Utils.CameraUtil;
import com.cachorrovascaino.plugin.Utils.TargetUtils;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.spatial.SpatialResource;
import com.hypixel.hytale.math.vector.Rotation3f;
import com.hypixel.hytale.protocol.AnimationSlot;
import com.hypixel.hytale.protocol.ChangeVelocityType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.AnimationUtils;
import com.hypixel.hytale.server.core.modules.entity.EntityModule;
import com.hypixel.hytale.server.core.modules.entity.component.HeadRotation;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageCause;
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
import java.util.concurrent.TimeUnit;

public class DynamicEntry implements Jutsu {

    public static final DynamicEntry INSTANCE = new DynamicEntry();

    private static final double DASH_RANGE = 12.0;
    private static final float BASE_DAMAGE = 120.0f;
    private static final float DAMAGE_PER_LEVEL = 20.0f;
    private static final String PARTICLE_ID = "Leaf_Hurricane_Circle";

    private static final ScheduledExecutorService SCHEDULER = Executors.newScheduledThreadPool(4);

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

    @Override public String getId() { return "dynamic_entry"; }
    @Override public String getDisplayName() { return JutsuType.DYNAMIC_ENTRY.getName(); }
    @Override public float getChakraCost() { return JutsuType.DYNAMIC_ENTRY.getResourceCost(); }
    @Override public float getCooldown() { return JutsuType.DYNAMIC_ENTRY.getCooldown(); }
    @Override public SkillType getType() { return SkillType.TAIJUTSU; }
    @Override public float getChakraCost(PlayerRef playerRef) { return getChakraCost(); }

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

        Ref<EntityStore> targetRef = TargetUtils.getTargetInLineOfSight(playerEntityRef, store, DASH_RANGE, 45.0);

        if (targetRef == null) {
            playerRef.sendMessage(Message.raw(" Dynamic Entry falhou: nenhum alvo ao alcance.").color(Color.GRAY));
            return;
        }

        playerRef.sendMessage(Message.raw(" DYNAMIC ENTRY!").color(Color.GREEN));

        final Ref<EntityStore> finalTargetRef = targetRef;
        final float damage = getDamageForPlayer(playerRef);
        float currentYawDegrees = (float) Math.toDegrees(originalRotation.yaw());

        // ==========================================
        // FASE 1: WIND-UP / CARGA (Delay Inicial)
        // ==========================================
        AnimationUtils.playAnimation(playerEntityRef, AnimationSlot.Action, null, "Kick", true, store);
        CameraUtil.setCinematicCamera(playerRef, 2.8f, currentYawDegrees - 20.0f, -10.0f);

        applyVelocityInstruction(store, playerEntityRef, new Vector3d(0, 3.5, 0), ChangeVelocityType.Set);

        // ==========================================
        // FASE 2: DASH PRINCIPAL & TRACKING CAM (120ms depois)
        // ==========================================
        SCHEDULER.schedule(() -> {
            world.execute(() -> {
                if (!playerEntityRef.isValid()) return;

                CameraUtil.setCinematicCamera(playerRef, 5.5f, currentYawDegrees + 50.0f, -18.0f);

                Vector3d attackerDash = new Vector3d(lookVector.x * 38.0, 0.5, lookVector.z * 38.0);
                applyVelocityInstruction(store, playerEntityRef, attackerDash, ChangeVelocityType.Set);
            });
        }, 120, TimeUnit.MILLISECONDS);

        // ==========================================
        // FASE 3: IMPACTO & FINISHER (300ms depois do arranque)
        // ==========================================
        SCHEDULER.schedule(() -> {
            world.execute(() -> {
                if (!playerEntityRef.isValid() || !finalTargetRef.isValid()) {
                    CameraUtil.resetCamera(playerRef);
                    AnimationUtils.playAnimation(playerEntityRef, AnimationSlot.Action, (String) null, true, store);
                    return;
                }

                TransformComponent targetTrans = store.getComponent(finalTargetRef, TransformComponent.getComponentType());
                if (targetTrans == null) {
                    CameraUtil.resetCamera(playerRef);
                    AnimationUtils.playAnimation(playerEntityRef, AnimationSlot.Action, (String) null, true, store);
                    return;
                }

                applyVelocityInstruction(store, playerEntityRef, new Vector3d(0, 0, 0), ChangeVelocityType.Set);

                CameraUtil.setCinematicCamera(playerRef, 3.0f, currentYawDegrees - 10.0f, -25.0f);
                CameraUtil.applyCameraShake(playerRef, 3.0f, currentYawDegrees - 10.0f, -25.0f, 160);

                Vector3d pushVector = new Vector3d(targetTrans.getPosition()).sub(playerTransform.getPosition());
                pushVector.y = 0;

                if (pushVector.lengthSquared() > 0.0001) {
                    pushVector.normalize();
                } else {
                    float yaw = originalRotation.yaw();
                    pushVector.set(-Math.sin(yaw), 0, Math.cos(yaw));
                }

                applyVelocityInstruction(store, finalTargetRef, new Vector3d(0, 14.0, 0), ChangeVelocityType.Set);
                executeForcedKnockback(world, store, finalTargetRef, pushVector.x, pushVector.z, 12.0, 8);

                applyDamage(playerRef, playerEntityRef, finalTargetRef, damage, store);
                spawnImpactParticles(targetTrans.getPosition(), store, originalRotation);

                SCHEDULER.schedule(() -> {
                    world.execute(() -> {
                        CameraUtil.resetCamera(playerRef);
                        AnimationUtils.playAnimation(playerEntityRef, AnimationSlot.Action, (String) null, true, store);
                    });
                }, 220, TimeUnit.MILLISECONDS);
            });
        }, 300, TimeUnit.MILLISECONDS);
    }

    private void executeForcedKnockback(World world, Store<EntityStore> store, Ref<EntityStore> targetRef, double dirX, double dirZ, double totalDistance, int steps) {
        double stepDistance = totalDistance / steps;

        for (int i = 1; i <= steps; i++) {
            final int currentStep = i;
            SCHEDULER.schedule(() -> {
                world.execute(() -> {
                    if (targetRef == null || !targetRef.isValid()) return;

                    TransformComponent targetTrans = store.getComponent(targetRef, TransformComponent.getComponentType());
                    if (targetTrans != null) {
                        Vector3d currentPos = targetTrans.getPosition();
                        Vector3d newPos = new Vector3d(
                                currentPos.x + (dirX * stepDistance),
                                currentPos.y,
                                currentPos.z + (dirZ * stepDistance)
                        );

                        targetTrans.teleportPosition(newPos);
                    }
                });
            }, currentStep * 20L, TimeUnit.MILLISECONDS);
        }
    }

    private void applyVelocityInstruction(Store<EntityStore> store, Ref<EntityStore> entityRef, Vector3d velocity, ChangeVelocityType type) {
        if (entityRef == null || !entityRef.isValid()) return;

        try {
            @SuppressWarnings("unchecked")
            CommandBuffer<EntityStore> commandBuffer = (CommandBuffer<EntityStore>) TAKE_COMMAND_BUFFER_METHOD.invoke(store);

            commandBuffer.run(runStore -> {
                if (!entityRef.isValid()) return;

                Velocity velComponent = runStore.getComponent(entityRef, Velocity.getComponentType());
                if (velComponent != null) {
                    velComponent.addInstruction(velocity, null, type);
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

        if (playerSpatial != null) {
            @SuppressWarnings("unchecked")
            List<Ref<EntityStore>> playersToNotify = (List<Ref<EntityStore>>) (List<?>) SpatialResource.getThreadLocalReferenceList();
            playerSpatial.getSpatialStructure().collect(pos, 75.0, playersToNotify);

            if (!playersToNotify.isEmpty()) {
                Vector3d particlePos = new Vector3d(pos.x, pos.y + 0.5, pos.z);
                ParticleUtil.spawnParticleEffect(PARTICLE_ID, particlePos, rotation, playersToNotify, store);
            }
        }
    }
}