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
import com.hypixel.hytale.math.raycast.RaycastAABB;
import com.hypixel.hytale.math.vector.Rotation3f;
import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.protocol.AnimationSlot;
import com.hypixel.hytale.protocol.ChangeVelocityType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.AnimationUtils;
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

    private static final double DASH_RANGE = 12.0;
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
            playerRef.sendMessage(Message.raw(" Omote Renge failed: no target within range.").color(Color.GRAY));
            return;
        }

        playerRef.sendMessage(Message.raw(" Omote Renge!").color(Color.RED));

        final Ref<EntityStore> finalTargetRef = targetRef;
        final float damage = getDamageForPlayer(playerRef);
        float currentYawDegrees = (float) Math.toDegrees(originalRotation.yaw());

        // ==========================================
        // FASE 1: WIND-UP / CHUTE INICIAL (Take 1)
        // ==========================================
        CameraUtil.setCinematicCamera(playerRef, 3.0f, currentYawDegrees - 15.0f, -25.0f);
        AnimationUtils.playAnimation(playerEntityRef, AnimationSlot.Action, null, "Kick", true, store);

        Vector3d dashVelocity = new Vector3d(lookVector.x * 22.0, 3.0, lookVector.z * 22.0);
        applyVelocityInstruction(store, playerEntityRef, dashVelocity, ChangeVelocityType.Set);

        SCHEDULER.schedule(() -> {
            world.execute(() -> {
                if (!playerEntityRef.isValid() || !finalTargetRef.isValid()) {
                    CameraUtil.resetCamera(playerRef);
                    AnimationUtils.playAnimation(playerEntityRef, AnimationSlot.Action, (String) null, true, store);
                    return;
                }

                // ==========================================
                // FASE 2: ASCENSÃO AÉREA (Take 2)
                // ==========================================
                CameraUtil.setCinematicCamera(playerRef, 7.0f, currentYawDegrees + 60.0f, -10.0f);

                final int totalAscendTicks = 16;
                final int[] currentTick = {0};
                final double ASCENT_SPEED = 18.0;

                final ScheduledFuture<?>[] ascendTask = new ScheduledFuture<?>[1];
                ascendTask[0] = SCHEDULER.scheduleAtFixedRate(() -> {
                    world.execute(() -> {
                        if (!playerEntityRef.isValid() || !finalTargetRef.isValid()) {
                            if (ascendTask[0] != null) ascendTask[0].cancel(false);
                            CameraUtil.resetCamera(playerRef);
                            AnimationUtils.playAnimation(playerEntityRef, AnimationSlot.Action, (String) null, true, store);
                            return;
                        }

                        currentTick[0]++;

                        Vector3d ascendVec = new Vector3d(0.0, ASCENT_SPEED, 0.0);
                        applyVelocityInstruction(store, playerEntityRef, ascendVec, ChangeVelocityType.Set);
                        applyVelocityInstruction(store, finalTargetRef, ascendVec, ChangeVelocityType.Set);

                        syncTargetPosition(store, playerEntityRef, finalTargetRef, lookVector);

                        TransformComponent targetTrans = store.getComponent(finalTargetRef, TransformComponent.getComponentType());
                        if (targetTrans != null) {
                            spawnParticleRing(targetTrans.getPosition(), 1.5, 8, store, originalRotation);
                        }

                        if (currentTick[0] >= totalAscendTicks) {
                            if (ascendTask[0] != null) ascendTask[0].cancel(false);
                            startPileDriver(world, store, playerRef, playerEntityRef, finalTargetRef, lookVector, originalRotation, damage, currentYawDegrees);
                        }
                    });
                }, 0, 50, TimeUnit.MILLISECONDS);
            });
        }, 180, TimeUnit.MILLISECONDS);
    }

    private void startPileDriver(World world, Store<EntityStore> store, PlayerRef playerRef, Ref<EntityStore> attacker, Ref<EntityStore> victim, Vector3d lookVector, Rotation3f rotation, float damage, float currentYawDegrees) {
        // ==========================================
        // FASE 3: QUEDA / MERGULHO (Take 3)
        // ==========================================
        CameraUtil.setCinematicCamera(playerRef, 6.0f, currentYawDegrees - 30.0f, 35.0f);

        final double DESCENT_SPEED = -30.0;
        final ScheduledFuture<?>[] descendTask = new ScheduledFuture<?>[1];

        descendTask[0] = SCHEDULER.scheduleAtFixedRate(() -> {
            world.execute(() -> {
                if (!attacker.isValid() || !victim.isValid()) {
                    if (descendTask[0] != null) descendTask[0].cancel(false);
                    CameraUtil.resetCamera(playerRef);
                    AnimationUtils.playAnimation(attacker, AnimationSlot.Action, (String) null, true, store);
                    return;
                }

                Vector3d descendVec = new Vector3d(0.0, DESCENT_SPEED, 0.0);
                applyVelocityInstruction(store, attacker, descendVec, ChangeVelocityType.Set);
                applyVelocityInstruction(store, victim, descendVec, ChangeVelocityType.Set);

                syncTargetPosition(store, attacker, victim, lookVector);

                TransformComponent victimTrans = store.getComponent(victim, TransformComponent.getComponentType());
                if (victimTrans == null) return;

                Vector3d currentPos = victimTrans.getPosition();
                boolean hitGround = checkGroundImpact(currentPos, DESCENT_SPEED * 0.05);

                if (hitGround) {
                    if (descendTask[0] != null) descendTask[0].cancel(false);

                    // ==========================================
                    // FASE 4: IMPACTO NO SOLO (Take 4)
                    // ==========================================
                    applyVelocityInstruction(store, attacker, new Vector3d(0, 0, 0), ChangeVelocityType.Set);
                    applyVelocityInstruction(store, victim, new Vector3d(0, 0, 0), ChangeVelocityType.Set);

                    teleportEntity(world, store, victim, currentPos, rotation);
                    teleportEntity(world, store, attacker, new Vector3d(currentPos).add(0.0, 0.2, 0.0), rotation);

                    CameraUtil.setCinematicCamera(playerRef, 3.2f, currentYawDegrees, -25.0f);
                    CameraUtil.applyCameraShake(playerRef, 3.2f, currentYawDegrees, -25.0f, 180);

                    applyDamage(playerRef, attacker, victim, damage, store);
                    spawnImpactParticles(currentPos, store, rotation);

                    // ==========================================
                    // FASE 5: RECUO DE SALTO PARA TRÁS (Take 5)
                    // ==========================================
                    SCHEDULER.schedule(() -> {
                        world.execute(() -> {
                            if (!attacker.isValid()) return;

                            Vector3d backDash = new Vector3d(-lookVector.x * 14.0, 6.5, -lookVector.z * 14.0);
                            applyVelocityInstruction(store, attacker, backDash, ChangeVelocityType.Set);

                            CameraUtil.setCinematicCamera(playerRef, 6.0f, currentYawDegrees + 20.0f, -10.0f);

                            AnimationUtils.playAnimation(attacker, AnimationSlot.Action, (String) null, true, store);
                            AnimationUtils.playAnimation(attacker, AnimationSlot.Action, null, "Ninja", true, store);

                            SCHEDULER.schedule(() -> {
                                world.execute(() -> {
                                    CameraUtil.resetCamera(playerRef);
                                    AnimationUtils.playAnimation(attacker, AnimationSlot.Action, (String) null, true, store);
                                });
                            }, 3000, TimeUnit.MILLISECONDS);
                        });
                    }, 90, TimeUnit.MILLISECONDS);
                }
            });
        }, 0, 50, TimeUnit.MILLISECONDS);
    }

    private void syncTargetPosition(Store<EntityStore> store, Ref<EntityStore> attackerRef, Ref<EntityStore> victimRef, Vector3d lookVector) {
        TransformComponent attackerTrans = store.getComponent(attackerRef, TransformComponent.getComponentType());
        TransformComponent victimTrans = store.getComponent(victimRef, TransformComponent.getComponentType());

        if (attackerTrans != null && victimTrans != null) {
            Vector3d attackerPos = attackerTrans.getPosition();
            Vector3d targetPos = new Vector3d(attackerPos).add(new Vector3d(lookVector).mul(0.5));
            victimTrans.getPosition().set(targetPos);
        }
    }

    private boolean checkGroundImpact(Vector3d pos, double distanceThisFrame) {
        double minX = pos.x - 0.6, maxX = pos.x + 0.6;
        double minZ = pos.z - 0.6, maxZ = pos.z + 0.6;
        double minY = pos.y - 3.0, maxY = pos.y;

        double distance = RaycastAABB.intersect(
                minX, minY, minZ,
                maxX, maxY, maxZ,
                pos.x, pos.y, pos.z,
                0.0, -1.0, 0.0
        );

        return distance != Double.POSITIVE_INFINITY && distance <= Math.abs(distanceThisFrame);
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

    private void teleportEntity(World world, Store<EntityStore> store, Ref<EntityStore> entityRef, Vector3d position, Rotation3f rotation) {
        if (entityRef == null || !entityRef.isValid()) return;

        Transform targetTransform = new Transform(
                position.x, position.y, position.z,
                rotation != null ? rotation.pitch() : 0.0f,
                rotation != null ? rotation.yaw() : 0.0f,
                rotation != null ? rotation.roll() : 0.0f
        );

        Teleport teleportComponent = Teleport.createForPlayer(world, targetTransform);
        store.addComponent(entityRef, Teleport.getComponentType(), teleportComponent);

        HeadRotation head = store.getComponent(entityRef, HeadRotation.getComponentType());
        if (head != null && rotation != null) {
            head.setRotation(rotation);
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

    private void spawnParticleRing(Vector3d center, double radius, int points, Store<EntityStore> store, Rotation3f rotation) {
        SpatialResource<Ref<EntityStore>, EntityStore> playerSpatial = store.getResource(EntityModule.get().getPlayerSpatialResourceType());

        if (playerSpatial != null) {
            @SuppressWarnings("unchecked")
            List<Ref<EntityStore>> playersToNotify = (List<Ref<EntityStore>>) (List<?>) SpatialResource.getThreadLocalReferenceList();
            playerSpatial.getSpatialStructure().collect(center, 75.0, playersToNotify);

            if (!playersToNotify.isEmpty()) {
                double increment = (2 * Math.PI) / points;
                for (int i = 0; i < points; i++) {
                    double angle = i * increment;
                    double x = center.x + (radius * Math.cos(angle));
                    double y = center.y;
                    double z = center.z + (radius * Math.sin(angle));

                    ParticleUtil.spawnParticleEffect(PARTICLE_ID, new Vector3d(x, y, z), rotation, playersToNotify, store);
                }
            }
        }
    }

    private void spawnImpactParticles(Vector3d pos, Store<EntityStore> store, Rotation3f rotation) {
        spawnParticleRing(pos, 3.5, 24, store, rotation);
        spawnParticleRing(pos, 6.0, 36, store, rotation);
    }
}