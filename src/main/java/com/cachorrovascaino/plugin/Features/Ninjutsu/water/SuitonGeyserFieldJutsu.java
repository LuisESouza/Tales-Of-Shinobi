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
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Rotation3f;
import com.hypixel.hytale.protocol.SoundCategory;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.modules.entity.component.HeadRotation;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageCause;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.ParticleUtil;
import com.hypixel.hytale.server.core.universe.world.SoundUtil;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.section.BlockSection;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.joml.Vector3d;

import java.awt.Color;
import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class SuitonGeyserFieldJutsu implements Jutsu {

    public static final SuitonGeyserFieldJutsu INSTANCE = new SuitonGeyserFieldJutsu();

    private static final float BASE_DAMAGE = 18.0f;
    private static final float DAMAGE_PER_LEVEL = 3.5f;

    private static final int TOTAL_GEYSERS = 3;
    private static final int DELAY_BETWEEN_GEYSERS_MS = 3500;

    private static final double MAX_TARGET_RANGE = 18.0;
    private static final double GEYSER_HITBOX_RADIUS = 2.2;
    private static final double GEYSER_HEIGHT_HITBOX = 3.5;

    private static final double GROUND_Y_OFFSET = -0.6;

    private static final String GEYSER_PARTICLE_ID = "Geyzer";

    private static final ScheduledExecutorService SCHEDULER = Executors.newSingleThreadScheduledExecutor();

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

    @Override public String getId() { return "suiton_geyser_field"; }
    @Override public String getDisplayName() { return "Suiton: Suijin Chisen no Jutsu"; }
    @Override public float getChakraCost() { return 90.0f; }
    @Override public float getCooldown() { return 30.0f; }
    @Override public SkillType getType() { return SkillType.NINJUTSU; }

    public float getDamagePerGeyser(PlayerRef playerRef) {
        PlayerData data = Main.getDataManager().getPlayerData(playerRef.getUuid());
        int level = (data != null) ? data.getJutsuLevel(getId()) : 1;
        return BASE_DAMAGE + ((level - 1) * DAMAGE_PER_LEVEL);
    }

    @Override
    public void execute(PlayerRef playerRef, Ref<EntityStore> playerEntityRef, Store<EntityStore> store, World world) {
        if (playerRef == null || playerEntityRef == null || !playerEntityRef.isValid()) return;

        SoundUtil.playSoundEvent2dToPlayer(playerRef, "SFX_Jutsu_Sound", SoundCategory.SFX);

        Ref<EntityStore> targetRef = getNearbyTarget(playerEntityRef, MAX_TARGET_RANGE, store);

        if (targetRef == null || !targetRef.isValid()) {
            playerRef.sendMessage(Message.raw("No targets around you for Suiton.!").color(Color.RED));
            return;
        }

        final Ref<EntityStore> finalTargetRef = targetRef;
        float damagePerGeyser = getDamagePerGeyser(playerRef);
        playerRef.sendMessage(Message.raw(getDisplayName() + " Activated! Geysers tracking target!").color(Color.CYAN));

        HeadRotation headRotation = store.getComponent(playerEntityRef, HeadRotation.getComponentType());
        TransformComponent castTransform = store.getComponent(playerEntityRef, TransformComponent.getComponentType());
        Rotation3f rotation = (headRotation != null) ? headRotation.getRotation() : castTransform.getRotation();

        ScheduledFuture<?>[] taskFuture = new ScheduledFuture<?>[1];
        int[] geyserCount = {0};

        taskFuture[0] = SCHEDULER.scheduleAtFixedRate(() -> {
            if (geyserCount[0] >= TOTAL_GEYSERS) {
                if (taskFuture[0] != null) taskFuture[0].cancel(false);
                return;
            }
            geyserCount[0]++;

            world.execute(() -> {
                if (!playerEntityRef.isValid() || !finalTargetRef.isValid()) {
                    if (taskFuture[0] != null) taskFuture[0].cancel(false);
                    return;
                }

                TransformComponent targetTransform = store.getComponent(finalTargetRef, TransformComponent.getComponentType());
                if (targetTransform == null) return;

                Vector3d currentTargetPos = new Vector3d(targetTransform.getPosition());
                double exactGroundY = getExactGroundY(world, currentTargetPos.x, currentTargetPos.y + 1.0, currentTargetPos.z);
                Vector3d spawnPos = new Vector3d(currentTargetPos.x, exactGroundY + GROUND_Y_OFFSET, currentTargetPos.z);

                spawnParticle(GEYSER_PARTICLE_ID, spawnPos, 1.0f, rotation, store);

                processGeyserEruptionDamage(playerEntityRef, spawnPos, damagePerGeyser, store);
            });
        }, 0, DELAY_BETWEEN_GEYSERS_MS, TimeUnit.MILLISECONDS);
    }

    private Ref<EntityStore> getNearbyTarget(Ref<EntityStore> casterRef, double radius, Store<EntityStore> store) {
        TransformComponent casterTransform = store.getComponent(casterRef, TransformComponent.getComponentType());
        if (casterTransform == null) return null;

        Vector3d center = casterTransform.getPosition();
        List<Ref<EntityStore>> nearbyEntities = TargetUtils.getEntitiesInRadius(center, radius, store);

        Ref<EntityStore> closestTarget = null;
        double closestDistanceSq = Double.MAX_VALUE;

        for (Ref<EntityStore> entity : nearbyEntities) {
            if (entity != null && entity.isValid() && !entity.equals(casterRef)) {
                TransformComponent targetTransform = store.getComponent(entity, TransformComponent.getComponentType());
                if (targetTransform != null) {
                    double distSq = center.distanceSquared(targetTransform.getPosition());
                    if (distSq < closestDistanceSq) {
                        closestDistanceSq = distSq;
                        closestTarget = entity;
                    }
                }
            }
        }

        return closestTarget;
    }

    private void processGeyserEruptionDamage(Ref<EntityStore> casterEntityRef, Vector3d geyserPos, float damage, Store<EntityStore> store) {
        List<Ref<EntityStore>> nearbyEntities = TargetUtils.getEntitiesInRadius(geyserPos, GEYSER_HITBOX_RADIUS, store);
        DamageCause damageCause = DamageCause.getAssetMap().getAsset("Water");
        if (damageCause == null) {
            damageCause = DamageCause.getAssetMap().getAsset(0);
        }

        Damage.Source source = new Damage.EntitySource(casterEntityRef);

        try {
            @SuppressWarnings("unchecked")
            CommandBuffer<EntityStore> commandBuffer = (CommandBuffer<EntityStore>) TAKE_COMMAND_BUFFER_METHOD.invoke(store);

            for (Ref<EntityStore> targetRef : nearbyEntities) {
                if (targetRef == null || !targetRef.isValid() || targetRef.equals(casterEntityRef)) continue;

                TransformComponent targetTransform = store.getComponent(targetRef, TransformComponent.getComponentType());
                if (targetTransform == null) continue;

                Vector3d targetPos = targetTransform.getPosition();

                double yDiff = targetPos.y - geyserPos.y;
                if (yDiff >= -1.0 && yDiff <= GEYSER_HEIGHT_HITBOX) {
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

    private double getExactGroundY(World world, double x, double startY, double z) {
        ChunkStore chunkStore = world.getChunkStore();

        int blockX = (int) Math.floor(x);
        int startBlockY = (int) Math.floor(startY);
        int blockZ = (int) Math.floor(z);

        int chunkX = ChunkUtil.chunkCoordinate(blockX);
        int chunkZ = ChunkUtil.chunkCoordinate(blockZ);

        for (int y = startBlockY; y >= startBlockY - 8; y--) {
            int chunkY = ChunkUtil.chunkCoordinate(y);

            Ref<ChunkStore> sectionRef = chunkStore.getChunkSectionReference(chunkX, chunkY, chunkZ);
            if (sectionRef != null && sectionRef.isValid()) {
                BlockSection blockSection = chunkStore.getStore().getComponent(sectionRef, BlockSection.getComponentType());
                if (blockSection != null) {
                    int localX = blockX & ChunkUtil.SIZE_MASK;
                    int localY = y & ChunkUtil.SIZE_MASK;
                    int localZ = blockZ & ChunkUtil.SIZE_MASK;
                    int blockIdx = ChunkUtil.indexBlock(localX, localY, localZ);

                    if (blockSection.get(blockIdx) != 0) {
                        return y + 1.0;
                    }
                }
            }
        }
        return startY - 1.0;
    }

    private void spawnParticle(String particleId, Vector3d position, float scale, Rotation3f rotation, Store<EntityStore> store) {
        List<Ref<EntityStore>> nearbyEntities = TargetUtils.getEntitiesInRadius(position, 50.0, store);

        List<Ref<EntityStore>> playersToNotify = nearbyEntities.stream()
                .filter(ref -> ref != null && ref.isValid() && store.getComponent(ref, PlayerRef.getComponentType()) != null)
                .toList();

        if (!playersToNotify.isEmpty()) {
            ParticleUtil.spawnParticleEffect(
                    particleId,
                    position.x(), position.y(), position.z(),
                    rotation.yaw(), rotation.pitch(), rotation.roll(),
                    scale,
                    null,
                    null,
                    playersToNotify,
                    store
            );
        }
    }
}