package com.cachorrovascaino.plugin.Utils;

import com.hypixel.hytale.component.*;
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

public class ProjectileJutsuUtils {

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

    /**
     * Spawna um projétil de Jutsu com offsets de posição e escala customizada.
     */
    public static void spawnProjectileJutsuEx(
            PlayerRef playerRef,
            Ref<EntityStore> playerEntityRef,
            Store<EntityStore> store,
            World world,
            String assetKey,
            float damageAmount,
            double speed,
            double rightOffset,
            double upOffset,
            double forwardOffset,
            float scale,
            String damageCauseKey,
            String castMessage
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

        executeSpawn(playerRef, playerEntityRef, store, world, assetKey, damageAmount, spawnPosition, velocityVector, scale, damageCauseKey, castMessage);
    }

    /**
     * Spawna um METEORO que cai 100% RETO na vertical (eixo Y negativo) sobre o ponto de alvo.
     *
     * @param targetDistance Distância à frente do jogador onde o meteoro vai cair no chão.
     * @param skyHeight Altura no céu onde o meteoro vai nascer (acima do ponto de impacto).
     */
    public static void spawnVerticalMeteorJutsu(
            PlayerRef playerRef,
            Ref<EntityStore> playerEntityRef,
            Store<EntityStore> store,
            World world,
            String assetKey,
            float damageAmount,
            double fallSpeed,      // Velocidade de descida
            double targetDistance, // Ex: 25.0 blocos à frente do jogador
            double skyHeight,      // Ex: 80.0 blocos no céu
            float scale,
            String damageCauseKey,
            String castMessage
    ) {
        if (playerRef == null || playerEntityRef == null || !playerEntityRef.isValid()) return;

        TransformComponent transform = store.getComponent(playerEntityRef, TransformComponent.getComponentType());
        if (transform == null) return;

        HeadRotation headRotation = store.getComponent(playerEntityRef, HeadRotation.getComponentType());
        Rotation3f rotation = (headRotation != null) ? headRotation.getRotation() : transform.getRotation();

        // 1. Calcula a direção horizontal (apenas no plano X/Z, pitch zerado)
        Vector3d forwardVector = new Vector3d();
        PhysicsMath.vectorFromAngles(rotation.yaw(), 0.0f, forwardVector);
        forwardVector.normalize();

        // 2. Calcula onde é o ALVO NO CHÃO à frente do player
        Vector3d targetFloorPoint = new Vector3d(transform.getPosition())
                .add(new Vector3d(forwardVector).mul(targetDistance));

        // 3. Posição de SPAWN: Direto no céu (Y + skyHeight) acima do alvo
        Vector3d spawnPosition = new Vector3d(targetFloorPoint).add(0.0, skyHeight, 0.0);

        // 4. VELOCIDADE 100% RETINHA PARA BAIXO: Apenas eixo Y negativo
        Vector3d velocityVector = new Vector3d(0.0, -fallSpeed, 0.0);

        executeSpawn(playerRef, playerEntityRef, store, world, assetKey, damageAmount, spawnPosition, velocityVector, scale, damageCauseKey, castMessage);
    }

    // --- MÉTODO PRIVADO QUE EXECUTA O SPAWN, MODELO E DANO (REAPROVEITAMENTO) ---
    private static void executeSpawn(
            PlayerRef playerRef,
            Ref<EntityStore> playerEntityRef,
            Store<EntityStore> store,
            World world,
            String assetKey,
            float damageAmount,
            Vector3d spawnPosition,
            Vector3d velocityVector,
            float scale,
            String damageCauseKey,
            String castMessage
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
                        null,
                        playerEntityRef,
                        commandBuffer,
                        config,
                        spawnPosition,
                        velocityVector
                );

                commandBuffer.run(runStore -> {
                    if (!projectileRef.isValid()) return;

                    // --- RECALCULA ESCALA, HITBOX E PARTÍCULAS NATIVAMENTE ---
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
                        physicsProvider.setImpactConsumer((ref, hitPos, targetRef, detailName, cmdBuf) -> {
                            if (targetRef != null && targetRef.isValid() && targetRef.equals(playerEntityRef)) {
                                return;
                            }

                            if (targetRef != null && targetRef.isValid()) {
                                DamageCause damageCause = (DamageCause) DamageCause.getAssetMap().getAsset(damageCauseKey);
                                if (damageCause == null) {
                                    damageCause = DamageCause.getAssetMap().getAsset(0);
                                }
                                Damage.Source source = new Damage.ProjectileSource(playerEntityRef, ref);
                                Damage damageEvent = new Damage(source, damageCause, damageAmount);
                                cmdBuf.invoke(targetRef, damageEvent);
                            }

                            cmdBuf.removeEntity(ref, RemoveReason.REMOVE);
                        });
                    }
                });

                CONSUME_METHOD.invoke(commandBuffer);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}