package com.cachorrovascaino.plugin.Features.Ninjutsu.water;

import com.cachorrovascaino.plugin.Abstractions.Jutsu;
import com.cachorrovascaino.plugin.Abstractions.SkillType;
import com.cachorrovascaino.plugin.Data.PlayerData;
import com.cachorrovascaino.plugin.Main;
import com.cachorrovascaino.plugin.Systems.DamageTrackingSystem;
import com.cachorrovascaino.plugin.Utils.TargetUtils;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Rotation3f;
import com.hypixel.hytale.protocol.SoundCategory;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.modules.entity.component.HeadRotation;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageCause;
import com.hypixel.hytale.server.core.modules.physics.util.PhysicsMath;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.ParticleUtil;
import com.hypixel.hytale.server.core.universe.world.SoundUtil;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.joml.Vector3d;

import java.awt.Color;
import java.lang.reflect.Method;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class SuitonWaterJetJutsu implements Jutsu {

    public static final SuitonWaterJetJutsu INSTANCE = new SuitonWaterJetJutsu();
    private static final ScheduledExecutorService SCHEDULER = Executors.newScheduledThreadPool(2);

    private static final String WATER_JET_PARTICLE_ID = "Jato_Agua";
    private static final double RANGE = 8.0; // 14.0
    private static final double HITBOX_RADIUS = 2.2;
    private static final double FORWARD_OFFSET = 0.5;
    private static final long TICK_INTERVAL_MS = 150;
    private static final double MOVE_THRESHOLD = 0.2;
    private static final float CHAKRA_COST_PER_TICK = 3.5f;
    private static final float TICK_DAMAGE = 4.5f;

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

    private SuitonWaterJetJutsu() {}

    @Override public String getId() { return "suiton_water_jet"; }
    @Override public String getDisplayName() { return "Suiton: Suiryū Shōten no Jutsu"; }
    @Override public float getChakraCost() { return CHAKRA_COST_PER_TICK; }
    @Override public float getCooldown() { return 10.0f; }
    @Override public SkillType getType() { return SkillType.NINJUTSU; }

    @Override
    public void execute(PlayerRef playerRef, Ref<EntityStore> playerEntityRef, Store<EntityStore> store, World world) {
        if (playerRef == null || playerEntityRef == null || !playerEntityRef.isValid() || world == null) return;

        UUID uuid = playerRef.getUuid();

        world.execute(() -> {
            if (!playerEntityRef.isValid()) return;

            TransformComponent initialTransform = store.getComponent(playerEntityRef, TransformComponent.getComponentType());
            if (initialTransform == null) return;

            final Vector3d initialPos = new Vector3d(initialTransform.getPosition());

            playerRef.sendMessage(Message.raw(getDisplayName() + "!").color(Color.CYAN));
            SoundUtil.playSoundEvent2dToPlayer(playerRef, "SFX_Jutsu_Sound", SoundCategory.SFX);

            AtomicBoolean isChanneling = new AtomicBoolean(true);
            ScheduledFuture<?>[] taskHolder = new ScheduledFuture<?>[1];

            taskHolder[0] = SCHEDULER.scheduleAtFixedRate(() -> {
                if (!isChanneling.get()) return;

                world.execute(() -> {
                    try {
                        if (!playerEntityRef.isValid()) {
                            stopWaterJet(isChanneling, taskHolder[0]);
                            return;
                        }

                        TransformComponent currentTransform = store.getComponent(playerEntityRef, TransformComponent.getComponentType());
                        if (currentTransform == null) {
                            stopWaterJet(isChanneling, taskHolder[0]);
                            return;
                        }

                        Vector3d currentPos = currentTransform.getPosition();

                        if (initialPos.distance(currentPos) > MOVE_THRESHOLD) {
                            stopWaterJet(isChanneling, taskHolder[0]);
                            playerRef.sendMessage(Message.raw("Suiton canceled: You moved!").color(Color.YELLOW));
                            return;
                        }

                        PlayerData playerData = Main.getDataManager().getPlayerData(uuid);
                        if (playerData == null || playerData.getCurrentChakra() < CHAKRA_COST_PER_TICK) {
                            stopWaterJet(isChanneling, taskHolder[0]);
                            playerRef.sendMessage(Message.raw("Suiton canceled: Insufficient chakra!").color(Color.RED));
                            return;
                        }

                        playerData.setCurrentChakra(playerData.getCurrentChakra() - CHAKRA_COST_PER_TICK);
                        Main.getJutsuManager().updateChakraHud(playerRef);

                        HeadRotation headRotation = store.getComponent(playerEntityRef, HeadRotation.getComponentType());
                        Rotation3f rotation = (headRotation != null) ? headRotation.getRotation() : currentTransform.getRotation();

                        Vector3d eyePos = new Vector3d(currentPos).add(0, 1.2, 0);

                        Vector3d forwardDir = new Vector3d();
                        PhysicsMath.vectorFromAngles(rotation.yaw(), rotation.pitch(), forwardDir);
                        forwardDir.normalize();

                        Vector3d spawnPos = new Vector3d(eyePos).add(new Vector3d(forwardDir).mul(FORWARD_OFFSET));

                        List<Ref<EntityStore>> nearbyPlayers = TargetUtils.getPlayersInRadius(spawnPos, 50.0, store);
                        if (!nearbyPlayers.isEmpty()) {
                            ParticleUtil.spawnParticleEffect(
                                    WATER_JET_PARTICLE_ID,
                                    spawnPos,
                                    rotation,
                                    nearbyPlayers,
                                    store
                            );
                        }

                        processJetDamage(playerEntityRef, eyePos, forwardDir, TICK_DAMAGE, store);

                    } catch (Exception e) {
                        stopWaterJet(isChanneling, taskHolder[0]);
                    }
                });
            }, 0, TICK_INTERVAL_MS, TimeUnit.MILLISECONDS);
        });
    }

    private void stopWaterJet(AtomicBoolean isChanneling, ScheduledFuture<?> task) {
        isChanneling.set(false);
        if (task != null) {
            task.cancel(false);
        }
    }

    private void processJetDamage(Ref<EntityStore> casterRef, Vector3d origin, Vector3d forwardDir, float damage, Store<EntityStore> store) {
        List<Ref<EntityStore>> targets = TargetUtils.getEntitiesInRadius(origin, RANGE, store);

        DamageCause damageCause = DamageCause.getAssetMap().getAsset("Water");
        if (damageCause == null) damageCause = DamageCause.getAssetMap().getAsset(0);
        Damage.Source source = new Damage.EntitySource(casterRef);

        try {
            @SuppressWarnings("unchecked")
            CommandBuffer<EntityStore> commandBuffer = (CommandBuffer<EntityStore>) TAKE_COMMAND_BUFFER_METHOD.invoke(store);

            for (Ref<EntityStore> targetRef : targets) {
                if (targetRef == null || !targetRef.isValid() || targetRef.equals(casterRef)) continue;

                TransformComponent targetTransform = store.getComponent(targetRef, TransformComponent.getComponentType());
                if (targetTransform == null) continue;

                Vector3d targetCenterPos = new Vector3d(targetTransform.getPosition()).add(0, 1.0, 0);

                if (isInJetPath(origin, forwardDir, targetCenterPos)) {
                    Damage damageEvent = new Damage(source, damageCause, damage);
                    damageEvent.putMetaObject(DamageTrackingSystem.RPG_DAMAGE_PROCESSED, false);
                    commandBuffer.invoke(targetRef, damageEvent);
                }
            }

            CONSUME_METHOD.invoke(commandBuffer);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean isInJetPath(Vector3d origin, Vector3d dir, Vector3d targetPos) {
        Vector3d toTarget = new Vector3d(targetPos).sub(origin);
        double projection = toTarget.dot(dir);

        if (projection > 0 && projection <= RANGE) {
            Vector3d closestPoint = new Vector3d(origin).add(new Vector3d(dir).mul(projection));
            return closestPoint.distance(targetPos) <= HITBOX_RADIUS;
        }
        return false;
    }
}