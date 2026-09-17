package com.cachorrovascaino.plugin.Utils;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Rotation3f;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.asset.type.model.config.Model;
import com.hypixel.hytale.server.core.asset.type.model.config.ModelAsset;
import com.hypixel.hytale.server.core.modules.entity.component.BoundingBox;
import com.hypixel.hytale.server.core.modules.entity.component.HeadRotation;
import com.hypixel.hytale.server.core.modules.entity.component.ModelComponent;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageCause;
import com.hypixel.hytale.server.core.modules.physics.component.PhysicsValues;
import com.hypixel.hytale.server.core.modules.physics.component.Velocity;
import com.hypixel.hytale.server.core.modules.physics.util.PhysicsMath;
import com.hypixel.hytale.server.core.modules.projectile.ProjectileModule;
import com.hypixel.hytale.server.core.modules.projectile.config.ProjectileConfig;
import com.hypixel.hytale.server.core.modules.projectile.config.StandardPhysicsProvider;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.joml.Vector3d;

import java.awt.Color;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class ProjectileJutsuUtils {

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

    public enum CurvedTrajectoryType {
        ORBIT,
        SPIRAL_OUT,
        SPIRAL_IN,
        SINUSOIDAL_FORWARD,
        HELIX_FORWARD,
        BOOMERANG,
        FIGURE_EIGHT
    }

    public static void spawnProjectileJutsuEx(
            PlayerRef playerRef, Ref<EntityStore> playerEntityRef, Store<EntityStore> store, World world,
            String assetKey, float damageAmount, double speed, double rightOffset, double upOffset, double forwardOffset,
            float scale, String damageCauseKey, String castMessage
    ) {
        spawnProjectileJutsuEx(playerRef, playerEntityRef, store, world, assetKey, damageAmount, speed, rightOffset, upOffset, forwardOffset, scale, damageCauseKey, castMessage, null, true);
    }

    public static void spawnProjectileJutsuEx(
            PlayerRef playerRef, Ref<EntityStore> playerEntityRef, Store<EntityStore> store, World world,
            String assetKey, float damageAmount, double speed, double rightOffset, double upOffset, double forwardOffset,
            float scale, String damageCauseKey, String castMessage, Consumer<Vector3d> onImpactCallback, boolean destroyOnImpact
    ) {
        if (playerRef == null || playerEntityRef == null || !playerEntityRef.isValid()) return;

        TransformComponent transform = store.getComponent(playerEntityRef, TransformComponent.getComponentType());
        if (transform == null) return;

        HeadRotation headRotation = store.getComponent(playerEntityRef, HeadRotation.getComponentType());
        Rotation3f rotation = (headRotation != null) ? headRotation.getRotation() : transform.getRotation();

        Vector3d lookVector = new Vector3d();
        PhysicsMath.vectorFromAngles(rotation.yaw(), rotation.pitch(), lookVector);
        lookVector.normalize();

        Vector3d rightVector = new Vector3d(lookVector).cross(0.0, 1.0, 0.0);
        if (rightVector.lengthSquared() < 0.0001) {
            rightVector.set(1.0, 0.0, 0.0);
        } else {
            rightVector.normalize();
        }
        Vector3d upVector = new Vector3d(rightVector).cross(lookVector).normalize();

        Vector3d spawnPosition = new Vector3d(transform.getPosition())
                .add(0.0, 1.5, 0.0)
                .add(new Vector3d(rightVector).mul(rightOffset))
                .add(new Vector3d(upVector).mul(upOffset))
                .add(new Vector3d(lookVector).mul(forwardOffset));

        Vector3d velocityVector = new Vector3d(lookVector).mul(speed);

        executeSpawn(playerRef, playerEntityRef, store, world, assetKey, damageAmount, spawnPosition, velocityVector, scale, damageCauseKey, castMessage, onImpactCallback, destroyOnImpact);
    }

    public static void spawnVerticalMeteorJutsu(
            PlayerRef playerRef, Ref<EntityStore> playerEntityRef, Store<EntityStore> store, World world,
            String assetKey, float damageAmount, double fallSpeed, double targetDistance, double skyHeight,
            float scale, String damageCauseKey, String castMessage, Consumer<Vector3d> onImpactCallback
    ) {
        if (playerRef == null || playerEntityRef == null || !playerEntityRef.isValid()) return;

        TransformComponent transform = store.getComponent(playerEntityRef, TransformComponent.getComponentType());
        if (transform == null) return;

        HeadRotation headRotation = store.getComponent(playerEntityRef, HeadRotation.getComponentType());
        Rotation3f rotation = (headRotation != null) ? headRotation.getRotation() : transform.getRotation();

        Vector3d forwardVector = new Vector3d();
        PhysicsMath.vectorFromAngles(rotation.yaw(), 0.0f, forwardVector);
        forwardVector.normalize();

        Vector3d targetFloorPoint = new Vector3d(transform.getPosition())
                .add(new Vector3d(forwardVector).mul(targetDistance));

        Vector3d spawnPosition = new Vector3d(targetFloorPoint).add(0.0, skyHeight, 0.0);
        Vector3d velocityVector = new Vector3d(0.0, -fallSpeed, 0.0);

        executeSpawn(playerRef, playerEntityRef, store, world, assetKey, damageAmount, spawnPosition, velocityVector, scale, damageCauseKey, castMessage, onImpactCallback, true);
    }

    public static void spawnDirectMeteorJutsu(
            PlayerRef playerRef, Ref<EntityStore> playerEntityRef, Store<EntityStore> store, World world,
            String assetKey, float damageAmount, Vector3d spawnPosition, Vector3d velocityVector,
            float scale, String damageCauseKey, Consumer<Vector3d> onImpactCallback
    ) {
        if (playerRef == null || playerEntityRef == null || !playerEntityRef.isValid()) return;
        executeSpawn(playerRef, playerEntityRef, store, world, assetKey, damageAmount, spawnPosition, velocityVector, scale, damageCauseKey, null, onImpactCallback, true);
    }

    public static void executeSpawn(
            PlayerRef playerRef, Ref<EntityStore> playerEntityRef, Store<EntityStore> store, World world,
            String assetKey, float damageAmount, Vector3d spawnPosition, Vector3d velocityVector,
            float scale, String damageCauseKey, String castMessage, Consumer<Vector3d> onImpactCallback,
            boolean destroyOnImpact
    ) {
        ProjectileConfig config = ProjectileConfig.getAssetMap().getAsset(assetKey);
        if (config == null) {
            playerRef.sendMessage(Message.raw("Erro: Asset de projétil [" + assetKey + "] não encontrado.").color(Color.RED));
            return;
        }

        if (castMessage != null && !castMessage.isBlank()) {
            playerRef.sendMessage(Message.raw(castMessage).color(Color.ORANGE));
        }

        world.execute(() -> {
            if (!playerEntityRef.isValid()) return;

            try {
                Store<EntityStore> entityStore = world.getEntityStore().getStore();

                @SuppressWarnings("unchecked")
                CommandBuffer<EntityStore> commandBuffer = (CommandBuffer<EntityStore>) TAKE_COMMAND_BUFFER_METHOD.invoke(entityStore);

                Ref<EntityStore> projectileRef = ProjectileModule.get().spawnProjectile(
                        playerEntityRef,
                        commandBuffer,
                        config,
                        spawnPosition,
                        velocityVector
                );

                Set<Ref<EntityStore>> hitEntities = ConcurrentHashMap.newKeySet();

                commandBuffer.run(runStore -> {
                    if (!projectileRef.isValid()) return;

                    if (scale != 1.0f) {
                        ModelComponent modelComp = runStore.getComponent(projectileRef, ModelComponent.getComponentType());
                        if (modelComp != null && modelComp.getModel() != null) {
                            String modelAssetId = modelComp.getModel().getModelAssetId();
                            ModelAsset modelAsset = (ModelAsset) ModelAsset.getAssetMap().getAsset(modelAssetId);

                            if (modelAsset != null) {
                                Model scaledModel = Model.createScaledModel(modelAsset, scale);
                                runStore.putComponent(projectileRef, ModelComponent.getComponentType(), new ModelComponent(scaledModel));
                                if (scaledModel.getBoundingBox() != null) {
                                    runStore.putComponent(projectileRef, BoundingBox.getComponentType(), new BoundingBox(scaledModel.getBoundingBox()));
                                }
                            }
                        }
                    }

                    PhysicsValues physicsValues = runStore.getComponent(projectileRef, PhysicsValues.getComponentType());
                    if (physicsValues != null) {
                        try {
                            Field dragField = PhysicsValues.class.getDeclaredField("dragCoefficient");
                            dragField.setAccessible(true);
                            dragField.setDouble(physicsValues, 0.0);
                        } catch (Exception ignored) {}
                    }

                    StandardPhysicsProvider physicsProvider = runStore.getComponent(
                            projectileRef,
                            StandardPhysicsProvider.getComponentType()
                    );

                    if (physicsProvider != null) {
                        physicsProvider.setImpactConsumer((projRef, hitPos, bounceBlockPos, targetRef, collisionDetailName, cmdBuf) -> {

                            if (targetRef != null && targetRef.isValid() && targetRef.equals(playerEntityRef)) {
                                return;
                            }

                            if (targetRef != null && targetRef.isValid()) {
                                if (!destroyOnImpact && hitEntities.contains(targetRef)) {
                                    Velocity projVel = cmdBuf.getComponent(projRef, Velocity.getComponentType());
                                    if (projVel != null) {
                                        projVel.addInstruction(velocityVector, null, com.hypixel.hytale.protocol.ChangeVelocityType.Set);
                                    }
                                    return;
                                }

                                hitEntities.add(targetRef);

                                DamageCause damageCause = (DamageCause) DamageCause.getAssetMap().getAsset(damageCauseKey);
                                if (damageCause == null) {
                                    damageCause = (DamageCause) DamageCause.getAssetMap().getAsset(0);
                                }

                                Damage.Source source = new Damage.ProjectileSource(playerEntityRef, projRef);
                                Damage damageEvent = new Damage(source, damageCause, damageAmount);
                                cmdBuf.invoke(targetRef, damageEvent);

                                Velocity targetVel = cmdBuf.getComponent(targetRef, Velocity.getComponentType());
                                if (targetVel != null) {
                                    Vector3d pushVector = new Vector3d(velocityVector).normalize().mul(6.0).add(0.0, 14.0, 0.0);
                                    targetVel.addInstruction(pushVector, null, com.hypixel.hytale.protocol.ChangeVelocityType.Set);
                                }
                            }

                            if (onImpactCallback != null) {
                                onImpactCallback.accept(hitPos);
                            }

                            if (destroyOnImpact) {
                                cmdBuf.removeEntity(projRef, RemoveReason.REMOVE);
                            } else {
                                Velocity projVel = cmdBuf.getComponent(projRef, Velocity.getComponentType());
                                if (projVel != null) {
                                    projVel.addInstruction(velocityVector, null, com.hypixel.hytale.protocol.ChangeVelocityType.Set);
                                }
                            }
                        });
                    }
                });

                CONSUME_METHOD.invoke(commandBuffer);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public static void spawnCurvedProjectileJutsu(
            PlayerRef playerRef, Ref<EntityStore> playerEntityRef, Store<EntityStore> store, World world,
            String assetKey, float damageAmount, CurvedTrajectoryType trajectoryType, double radiusOrAmplitude,
            double angularSpeed, float scale, double collisionRadius, int maxTicks, String damageCauseKey,
            String castMessage, Consumer<Vector3d> onImpactCallback
    ) {
        spawnCurvedProjectileJutsu(playerRef, playerEntityRef, store, world, assetKey, damageAmount, trajectoryType,
                radiusOrAmplitude, angularSpeed, scale, collisionRadius, maxTicks, damageCauseKey, castMessage,
                onImpactCallback, true);
    }

    public static void spawnCurvedProjectileJutsu(
            PlayerRef playerRef, Ref<EntityStore> playerEntityRef, Store<EntityStore> store, World world,
            String assetKey, float damageAmount, CurvedTrajectoryType trajectoryType, double radiusOrAmplitude,
            double angularSpeed, float scale, double collisionRadius, int maxTicks, String damageCauseKey,
            String castMessage, Consumer<Vector3d> onImpactCallback, boolean destroyOnImpact
    ) {
        if (playerRef == null || playerEntityRef == null || !playerEntityRef.isValid()) return;

        ProjectileConfig config = ProjectileConfig.getAssetMap().getAsset(assetKey);
        if (config == null) {
            playerRef.sendMessage(Message.raw("Erro: Asset de projétil [" + assetKey + "] não encontrado.").color(Color.RED));
            return;
        }

        if (castMessage != null && !castMessage.isBlank()) {
            playerRef.sendMessage(Message.raw(castMessage).color(Color.ORANGE));
        }

        world.execute(() -> {
            if (!playerEntityRef.isValid()) return;

            try {
                Store<EntityStore> entityStore = world.getEntityStore().getStore();

                @SuppressWarnings("unchecked")
                CommandBuffer<EntityStore> commandBuffer = (CommandBuffer<EntityStore>) TAKE_COMMAND_BUFFER_METHOD.invoke(entityStore);

                TransformComponent pTrans = store.getComponent(playerEntityRef, TransformComponent.getComponentType());
                if (pTrans == null) return;

                Ref<EntityStore> projectileRef = ProjectileModule.get().spawnProjectile(
                        playerEntityRef,
                        commandBuffer,
                        config,
                        pTrans.getPosition(),
                        new Vector3d(0, 0, 0)
                );

                commandBuffer.run(runStore -> {
                    if (!projectileRef.isValid()) return;

                    if (scale != 1.0f) {
                        ModelComponent modelComp = runStore.getComponent(projectileRef, ModelComponent.getComponentType());
                        if (modelComp != null && modelComp.getModel() != null) {
                            String modelAssetId = modelComp.getModel().getModelAssetId();
                            ModelAsset modelAsset = (ModelAsset) ModelAsset.getAssetMap().getAsset(modelAssetId);

                            if (modelAsset != null) {
                                Model scaledModel = Model.createScaledModel(modelAsset, scale);
                                runStore.putComponent(projectileRef, ModelComponent.getComponentType(), new ModelComponent(scaledModel));
                                if (scaledModel.getBoundingBox() != null) {
                                    runStore.putComponent(projectileRef, BoundingBox.getComponentType(), new BoundingBox(scaledModel.getBoundingBox()));
                                }
                            }
                        }
                    }
                });

                CONSUME_METHOD.invoke(commandBuffer);

                startCurvedLoop(
                        world, store, playerEntityRef, projectileRef, damageAmount,
                        trajectoryType, radiusOrAmplitude, angularSpeed, collisionRadius,
                        maxTicks, damageCauseKey, onImpactCallback, destroyOnImpact
                );

            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private static void startCurvedLoop(
            World world, Store<EntityStore> store, Ref<EntityStore> playerEntityRef, Ref<EntityStore> projRef,
            float damageAmount, CurvedTrajectoryType trajectoryType, double radiusOrAmplitude,
            double angularSpeed, double collisionRadius, int maxTicks, String damageCauseKey,
            Consumer<Vector3d> onImpactCallback, boolean destroyOnImpact
    ) {
        final double[] angle = { 0.0 };
        final double[] forwardProgress = { 0.0 };
        final int[] currentTick = { 0 };

        final Map<Ref<EntityStore>, Integer> hitCooldowns = new ConcurrentHashMap<>();
        final ScheduledFuture<?>[] taskHolder = new ScheduledFuture<?>[1];

        taskHolder[0] = SCHEDULER.scheduleAtFixedRate(() -> {
            world.execute(() -> {
                currentTick[0]++;

                hitCooldowns.entrySet().removeIf(entry -> {
                    entry.setValue(entry.getValue() - 1);
                    return entry.getValue() <= 0;
                });

                if (currentTick[0] > maxTicks || !playerEntityRef.isValid() || !projRef.isValid()) {
                    destroyAndTriggerCallback(world, store, projRef, null, null);
                    if (taskHolder[0] != null) taskHolder[0].cancel(false);
                    return;
                }

                TransformComponent playerTrans = store.getComponent(playerEntityRef, TransformComponent.getComponentType());
                TransformComponent projTrans = store.getComponent(projRef, TransformComponent.getComponentType());

                if (playerTrans == null || projTrans == null) {
                    destroyAndTriggerCallback(world, store, projRef, null, null);
                    if (taskHolder[0] != null) taskHolder[0].cancel(false);
                    return;
                }

                Vector3d currentProjPos = projTrans.getPosition();
                Vector3d nextPos = new Vector3d(currentProjPos);

                angle[0] += angularSpeed;

                switch (trajectoryType) {
                    case ORBIT: {
                        Vector3d pPos = playerTrans.getPosition();
                        double offsetX = radiusOrAmplitude * Math.cos(angle[0]);
                        double offsetZ = radiusOrAmplitude * Math.sin(angle[0]);

                        nextPos.set(pPos.x + offsetX, pPos.y + 1.0, pPos.z + offsetZ);

                        float yaw = (float) Math.atan2(-offsetX, offsetZ);
                        projTrans.setRotation(new Rotation3f(yaw, 0.0f, 0.0f));
                        break;
                    }
                    case SPIRAL_OUT: {
                        Vector3d pPos = playerTrans.getPosition();
                        double dynamicRadius = (currentTick[0] * 0.05) * radiusOrAmplitude;
                        double offsetX = dynamicRadius * Math.cos(angle[0]);
                        double offsetZ = dynamicRadius * Math.sin(angle[0]);

                        nextPos.set(pPos.x + offsetX, pPos.y + (currentTick[0] * 0.02), pPos.z + offsetZ);

                        float yaw = (float) Math.atan2(-offsetX, offsetZ);
                        projTrans.setRotation(new Rotation3f(yaw, 0.0f, 0.0f));
                        break;
                    }
                    case SPIRAL_IN: {
                        Vector3d pPos = playerTrans.getPosition();
                        double dynamicRadius = Math.max(0.2, radiusOrAmplitude - (currentTick[0] * 0.05));
                        double offsetX = dynamicRadius * Math.cos(angle[0]);
                        double offsetZ = dynamicRadius * Math.sin(angle[0]);

                        nextPos.set(pPos.x + offsetX, pPos.y + 1.0, pPos.z + offsetZ);

                        float yaw = (float) Math.atan2(-offsetX, offsetZ);
                        projTrans.setRotation(new Rotation3f(yaw, 0.0f, 0.0f));
                        break;
                    }
                    case SINUSOIDAL_FORWARD: {
                        HeadRotation headRot = store.getComponent(playerEntityRef, HeadRotation.getComponentType());
                        Rotation3f rotation = (headRot != null) ? headRot.getRotation() : playerTrans.getRotation();

                        Vector3d forward = new Vector3d();
                        PhysicsMath.vectorFromAngles(rotation.yaw(), 0.0f, forward);
                        forward.normalize();

                        Vector3d right = new Vector3d(forward).cross(0.0, 1.0, 0.0);
                        if (right.lengthSquared() < 0.0001) {
                            right.set(1.0, 0.0, 0.0);
                        } else {
                            right.normalize();
                        }

                        forwardProgress[0] += 0.5;
                        double sideOffset = Math.sin(angle[0]) * radiusOrAmplitude;

                        Vector3d startPos = playerTrans.getPosition();
                        nextPos.set(startPos)
                                .add(0.0, 1.0, 0.0)
                                .add(new Vector3d(forward).mul(forwardProgress[0]))
                                .add(new Vector3d(right).mul(sideOffset));
                        break;
                    }
                    case HELIX_FORWARD: {
                        HeadRotation headRot = store.getComponent(playerEntityRef, HeadRotation.getComponentType());
                        Rotation3f rotation = (headRot != null) ? headRot.getRotation() : playerTrans.getRotation();

                        Vector3d forward = new Vector3d();
                        PhysicsMath.vectorFromAngles(rotation.yaw(), 0.0f, forward);
                        forward.normalize();

                        Vector3d right = new Vector3d(forward).cross(0.0, 1.0, 0.0);
                        if (right.lengthSquared() < 0.0001) {
                            right.set(1.0, 0.0, 0.0);
                        } else {
                            right.normalize();
                        }

                        forwardProgress[0] += 0.4;
                        double sideOffset = Math.sin(angle[0]) * radiusOrAmplitude;
                        double upOffset = Math.cos(angle[0]) * radiusOrAmplitude;

                        Vector3d startPos = playerTrans.getPosition();
                        nextPos.set(startPos)
                                .add(0.0, 1.0, 0.0)
                                .add(new Vector3d(forward).mul(forwardProgress[0]))
                                .add(new Vector3d(right).mul(sideOffset))
                                .add(0.0, upOffset, 0.0);
                        break;
                    }
                    case BOOMERANG: {
                        HeadRotation headRot = store.getComponent(playerEntityRef, HeadRotation.getComponentType());
                        Rotation3f rotation = (headRot != null) ? headRot.getRotation() : playerTrans.getRotation();

                        Vector3d forward = new Vector3d();
                        PhysicsMath.vectorFromAngles(rotation.yaw(), 0.0f, forward);
                        forward.normalize();

                        double distance = Math.sin((double) currentTick[0] / maxTicks * Math.PI) * radiusOrAmplitude;

                        Vector3d startPos = playerTrans.getPosition();
                        nextPos.set(startPos).add(0.0, 1.0, 0.0).add(new Vector3d(forward).mul(distance));
                        break;
                    }
                    case FIGURE_EIGHT: {
                        Vector3d pPos = playerTrans.getPosition();
                        double scale = radiusOrAmplitude;
                        double offsetX = scale * Math.sin(angle[0]);
                        double offsetZ = scale * Math.sin(angle[0]) * Math.cos(angle[0]);

                        nextPos.set(pPos.x + offsetX, pPos.y + 1.0, pPos.z + offsetZ);

                        float yaw = (float) Math.atan2(-offsetX, offsetZ);
                        projTrans.setRotation(new Rotation3f(yaw, 0.0f, 0.0f));
                        break;
                    }
                }

                projTrans.teleportPosition(nextPos);

                List<Ref<EntityStore>> nearbyEntities = TargetUtils.getEntitiesInRadius(nextPos, collisionRadius, store);

                for (Ref<EntityStore> target : nearbyEntities) {
                    if (target != null && target.isValid() && !target.equals(playerEntityRef) && !target.equals(projRef)) {
                        if (!hitCooldowns.containsKey(target)) {
                            applyDirectDamage(playerEntityRef, target, damageAmount, damageCauseKey, store);
                            hitCooldowns.put(target, 10);

                            if (onImpactCallback != null) {
                                onImpactCallback.accept(nextPos);
                            }

                            if (destroyOnImpact) {
                                destroyAndTriggerCallback(world, store, projRef, nextPos, null);
                                if (taskHolder[0] != null) taskHolder[0].cancel(false);
                                return;
                            }
                        }
                    }
                }

            });
        }, 0L, 30L, TimeUnit.MILLISECONDS);
    }

    public static void spawnWalkingTornadoJutsu(
            PlayerRef playerRef, Ref<EntityStore> playerEntityRef, Store<EntityStore> store, World world,
            String assetKey, float damageAmount, double speed, double forwardOffset,
            float scale, int maxTicks, double collisionRadius, String damageCauseKey,
            String castMessage, Consumer<Vector3d> onImpactCallback
    ) {
        spawnWalkingTornadoJutsu(playerRef, playerEntityRef, store, world, assetKey, damageAmount, speed, forwardOffset, scale, maxTicks, collisionRadius, damageCauseKey, castMessage, onImpactCallback, true);
    }

    public static void spawnWalkingTornadoJutsu(
            PlayerRef playerRef, Ref<EntityStore> playerEntityRef, Store<EntityStore> store, World world,
            String assetKey, float damageAmount, double speed, double forwardOffset,
            float scale, int maxTicks, double collisionRadius, String damageCauseKey,
            String castMessage, Consumer<Vector3d> onImpactCallback, boolean destroyOnImpact
    ) {
        if (playerRef == null || playerEntityRef == null || !playerEntityRef.isValid()) return;

        ProjectileConfig config = ProjectileConfig.getAssetMap().getAsset(assetKey);
        if (config == null) {
            playerRef.sendMessage(Message.raw("Erro: Asset de projétil [" + assetKey + "] não encontrado.").color(Color.RED));
            return;
        }

        if (castMessage != null && !castMessage.isBlank()) {
            playerRef.sendMessage(Message.raw(castMessage).color(Color.ORANGE));
        }

        world.execute(() -> {
            if (!playerEntityRef.isValid()) return;

            try {
                Store<EntityStore> entityStore = world.getEntityStore().getStore();

                @SuppressWarnings("unchecked")
                CommandBuffer<EntityStore> commandBuffer = (CommandBuffer<EntityStore>) TAKE_COMMAND_BUFFER_METHOD.invoke(entityStore);

                TransformComponent pTrans = store.getComponent(playerEntityRef, TransformComponent.getComponentType());
                HeadRotation headRot = store.getComponent(playerEntityRef, HeadRotation.getComponentType());
                if (pTrans == null) return;

                Rotation3f rotation = (headRot != null) ? headRot.getRotation() : pTrans.getRotation();

                Vector3d forwardVector = new Vector3d();
                PhysicsMath.vectorFromAngles(rotation.yaw(), 0.0f, forwardVector);
                forwardVector.normalize();

                Vector3d startPos = new Vector3d(pTrans.getPosition()).add(new Vector3d(forwardVector).mul(forwardOffset));

                Ref<EntityStore> projectileRef = ProjectileModule.get().spawnProjectile(
                        playerEntityRef,
                        commandBuffer,
                        config,
                        startPos,
                        new Vector3d(0, 0, 0)
                );

                commandBuffer.run(runStore -> {
                    if (!projectileRef.isValid()) return;

                    if (scale != 1.0f) {
                        ModelComponent modelComp = runStore.getComponent(projectileRef, ModelComponent.getComponentType());
                        if (modelComp != null && modelComp.getModel() != null) {
                            String modelAssetId = modelComp.getModel().getModelAssetId();
                            ModelAsset modelAsset = (ModelAsset) ModelAsset.getAssetMap().getAsset(modelAssetId);

                            if (modelAsset != null) {
                                Model scaledModel = Model.createScaledModel(modelAsset, scale);
                                runStore.putComponent(projectileRef, ModelComponent.getComponentType(), new ModelComponent(scaledModel));
                                if (scaledModel.getBoundingBox() != null) {
                                    runStore.putComponent(projectileRef, BoundingBox.getComponentType(), new BoundingBox(scaledModel.getBoundingBox()));
                                }
                            }
                        }
                    }
                });

                CONSUME_METHOD.invoke(commandBuffer);

                startWalkingGroundLoop(
                        world, store, playerEntityRef, projectileRef, damageAmount,
                        startPos, forwardVector, speed, collisionRadius, maxTicks,
                        damageCauseKey, onImpactCallback, destroyOnImpact
                );

            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private static void startWalkingGroundLoop(
            World world, Store<EntityStore> store, Ref<EntityStore> playerEntityRef, Ref<EntityStore> projRef,
            float damageAmount, Vector3d startPos, Vector3d direction, double speed,
            double collisionRadius, int maxTicks, String damageCauseKey,
            Consumer<Vector3d> onImpactCallback, boolean destroyOnImpact
    ) {
        final int[] currentTick = { 0 };
        final Vector3d currentPos = new Vector3d(startPos);
        final Map<Ref<EntityStore>, Integer> hitCooldowns = new ConcurrentHashMap<>();
        final ScheduledFuture<?>[] taskHolder = new ScheduledFuture<?>[1];

        double stepSize = speed * 0.03;

        taskHolder[0] = SCHEDULER.scheduleAtFixedRate(() -> {
            world.execute(() -> {
                currentTick[0]++;

                hitCooldowns.entrySet().removeIf(entry -> {
                    entry.setValue(entry.getValue() - 1);
                    return entry.getValue() <= 0;
                });

                if (currentTick[0] > maxTicks || !playerEntityRef.isValid() || !projRef.isValid()) {
                    destroyAndTriggerCallback(world, store, projRef, currentPos, onImpactCallback);
                    if (taskHolder[0] != null) taskHolder[0].cancel(false);
                    return;
                }

                TransformComponent projTrans = store.getComponent(projRef, TransformComponent.getComponentType());
                if (projTrans == null) {
                    if (taskHolder[0] != null) taskHolder[0].cancel(false);
                    return;
                }

                currentPos.add(new Vector3d(direction).mul(stepSize));

                int blockX = (int) Math.floor(currentPos.x);
                int blockZ = (int) Math.floor(currentPos.z);
                int startSearchY = (int) Math.floor(currentPos.y) + 2;

                int groundY = BlockJutsuUtils.getHighestBlockYAt(world, blockX, startSearchY, blockZ, 6);
                currentPos.y = groundY + 1.0;

                projTrans.teleportPosition(currentPos);

                List<Ref<EntityStore>> nearbyEntities = TargetUtils.getEntitiesInRadius(currentPos, collisionRadius, store);

                for (Ref<EntityStore> target : nearbyEntities) {
                    if (target != null && target.isValid() && !target.equals(playerEntityRef) && !target.equals(projRef)) {
                        if (!hitCooldowns.containsKey(target)) {
                            applyDirectDamage(playerEntityRef, target, damageAmount, damageCauseKey, store);
                            hitCooldowns.put(target, 10);

                            try {
                                CommandBuffer<EntityStore> cmd = (CommandBuffer<EntityStore>) TAKE_COMMAND_BUFFER_METHOD.invoke(store);
                                Velocity targetVel = cmd.getComponent(target, Velocity.getComponentType());
                                if (targetVel != null) {
                                    Vector3d pushVector = new Vector3d(direction).normalize().mul(4.0).add(0.0, 10.0, 0.0);
                                    targetVel.addInstruction(pushVector, null, com.hypixel.hytale.protocol.ChangeVelocityType.Set);
                                }
                                CONSUME_METHOD.invoke(cmd);
                            } catch (Exception ignored) {}

                            if (destroyOnImpact) {
                                destroyAndTriggerCallback(world, store, projRef, currentPos, onImpactCallback);
                                if (taskHolder[0] != null) taskHolder[0].cancel(false);
                                return;
                            }
                        }
                    }
                }

            });
        }, 0L, 30L, TimeUnit.MILLISECONDS);
    }

    private static void applyDirectDamage(
            Ref<EntityStore> attackerRef, Ref<EntityStore> victimRef, float damage,
            String damageCauseKey, Store<EntityStore> store
    ) {
        try {
            @SuppressWarnings("unchecked")
            CommandBuffer<EntityStore> cmd = (CommandBuffer<EntityStore>) TAKE_COMMAND_BUFFER_METHOD.invoke(store);

            DamageCause cause = (DamageCause) DamageCause.getAssetMap().getAsset(damageCauseKey);
            if (cause == null) cause = (DamageCause) DamageCause.getAssetMap().getAsset(0);

            Damage.Source source = new Damage.EntitySource(attackerRef);
            Damage damageEvent = new Damage(source, cause, damage);

            cmd.invoke(victimRef, damageEvent);
            CONSUME_METHOD.invoke(cmd);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void destroyAndTriggerCallback(
            World world, Store<EntityStore> store, Ref<EntityStore> projRef,
            Vector3d impactPos, Consumer<Vector3d> onImpactCallback
    ) {
        if (projRef != null && projRef.isValid()) {
            try {
                @SuppressWarnings("unchecked")
                CommandBuffer<EntityStore> cmd = (CommandBuffer<EntityStore>) TAKE_COMMAND_BUFFER_METHOD.invoke(store);
                cmd.removeEntity(projRef, RemoveReason.REMOVE);
                CONSUME_METHOD.invoke(cmd);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (onImpactCallback != null && impactPos != null) {
            onImpactCallback.accept(impactPos);
        }
    }
}