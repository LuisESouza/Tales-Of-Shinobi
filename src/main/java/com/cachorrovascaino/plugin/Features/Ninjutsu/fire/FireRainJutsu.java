package com.cachorrovascaino.plugin.Features.Ninjutsu.fire;

import com.cachorrovascaino.plugin.Abstractions.Jutsu;
import com.cachorrovascaino.plugin.Abstractions.SkillType;
import com.cachorrovascaino.plugin.Data.PlayerData;
import com.cachorrovascaino.plugin.Main;
import com.cachorrovascaino.plugin.Systems.DamageTrackingSystem;
import com.cachorrovascaino.plugin.Utils.ProjectileJutsuUtils;
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
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class FireRainJutsu implements Jutsu {

    public static final FireRainJutsu INSTANCE = new FireRainJutsu();

    private static final float BASE_DAMAGE = 25.0f;
    private static final float DAMAGE_PER_LEVEL = 4.0f;
    private static final float AOE_DAMAGE_PERCENTAGE = 0.50f;

    private static final int DURATION_SECONDS = 40;
    private static final int INTERVAL_MS = 250;

    private static final double RAIN_RADIUS = 10.0;

    private static final double AOE_RADIUS = 3.5;

    private static final double SKY_HEIGHT = 35.0;
    private static final double FALL_SPEED = 40.0;

    private static final String INDICATOR_PARTICLE_ID = "Fire_AoE_Spawn";
    private static final Random RANDOM = new Random();
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

    @Override public String getId() { return "katon_rain_jutsu"; }
    @Override public String getDisplayName() { return "Katon: Tenkyū no Rain"; }
    @Override public float getChakraCost() { return 120.0f; }
    @Override public float getCooldown() { return 60.0f; }
    @Override public SkillType getType() { return SkillType.NINJUTSU; }

    public float getDamagePerMeteor(PlayerRef playerRef) {
        PlayerData data = Main.getDataManager().getPlayerData(playerRef.getUuid());
        int level = (data != null) ? data.getJutsuLevel(getId()) : 1;
        return BASE_DAMAGE + ((level - 1) * DAMAGE_PER_LEVEL);
    }

    @Override
    public void execute(PlayerRef playerRef, Ref<EntityStore> playerEntityRef, Store<EntityStore> store, World world) {
        if (playerRef == null || playerEntityRef == null || !playerEntityRef.isValid()) return;

        //Sound
        SoundUtil.playSoundEvent2dToPlayer(playerRef, "SFX_Jutsu_Sound", SoundCategory.SFX);
        TransformComponent castTransform = store.getComponent(playerEntityRef, TransformComponent.getComponentType());
        if (castTransform == null) return;

        Vector3d castPos = new Vector3d(castTransform.getPosition());

        double groundY = findGroundY(world, castPos.x, castPos.y, castPos.z);
        final Vector3d fixedGroundCenter = new Vector3d(castPos.x, groundY, castPos.z);

        float damagePerMeteor = getDamagePerMeteor(playerRef);
        playerRef.sendMessage(Message.raw(getDisplayName() + " ativado! Tempestade de Fogo caindo na área!").color(Color.ORANGE));

        int totalTicks = (DURATION_SECONDS * 500) / INTERVAL_MS;

        ScheduledFuture<?>[] futureHolder = new ScheduledFuture<?>[1];
        int[] currentTick = {0};

        futureHolder[0] = SCHEDULER.scheduleAtFixedRate(() -> {
            if (currentTick[0] >= totalTicks) {
                if (futureHolder[0] != null) futureHolder[0].cancel(false);
                return;
            }
            currentTick[0]++;

            world.execute(() -> {
                if (!playerEntityRef.isValid()) {
                    if (futureHolder[0] != null) futureHolder[0].cancel(false);
                    return;
                }

                HeadRotation headRotation = store.getComponent(playerEntityRef, HeadRotation.getComponentType());
                Rotation3f rotation = (headRotation != null) ? headRotation.getRotation() : castTransform.getRotation();

                double angle = RANDOM.nextDouble() * 2.0 * Math.PI;
                double distance = Math.sqrt(RANDOM.nextDouble()) * RAIN_RADIUS;

                double offsetX = distance * Math.cos(angle);
                double offsetZ = distance * Math.sin(angle);

                double targetX = fixedGroundCenter.x + offsetX;
                double targetZ = fixedGroundCenter.z + offsetZ;

                double localGroundY = findGroundY(world, targetX, fixedGroundCenter.y + 10.0, targetZ);

                Vector3d impactPos = new Vector3d(targetX, localGroundY, targetZ);
                Vector3d spawnPos = new Vector3d(targetX, localGroundY + SKY_HEIGHT, targetZ);
                Vector3d velocityVector = new Vector3d(0.0, -FALL_SPEED, 0.0);

                spawnTargetIndicator(impactPos, store, rotation);

                ProjectileJutsuUtils.spawnDirectMeteorJutsu(
                        playerRef,
                        playerEntityRef,
                        store,
                        world,
                        "Jutsu_Fireball_Charge",
                        damagePerMeteor,
                        spawnPos,
                        velocityVector,
                        1.2f,
                        "Fire",
                        (actualHitPos) -> {
                            world.execute(() -> {
                                applyAoeDamage(playerEntityRef, actualHitPos, damagePerMeteor * AOE_DAMAGE_PERCENTAGE, store);
                            });
                        }
                );
            });
        }, 0, INTERVAL_MS, TimeUnit.MILLISECONDS);
    }

    private double findGroundY(World world, double x, double startY, double z) {
        ChunkStore chunkStore = world.getChunkStore();
        if (chunkStore == null) return startY;

        int blockX = (int) Math.floor(x);
        int startBlockY = (int) Math.floor(startY);
        int blockZ = (int) Math.floor(z);

        int chunkX = ChunkUtil.chunkCoordinate(blockX);
        int chunkZ = ChunkUtil.chunkCoordinate(blockZ);

        for (int y = startBlockY; y > Math.max(0, startBlockY - 128); y--) {
            int chunkY = ChunkUtil.chunkCoordinate(y);

            Ref<ChunkStore> sectionRef = chunkStore.getChunkSectionReference(chunkX, chunkY, chunkZ);
            if (sectionRef != null && sectionRef.isValid()) {
                BlockSection blockSection = chunkStore.getStore().getComponent(sectionRef, BlockSection.getComponentType());
                if (blockSection != null) {
                    int localX = blockX & ChunkUtil.SIZE_MASK;
                    int localY = y & ChunkUtil.SIZE_MASK;
                    int localZ = blockZ & ChunkUtil.SIZE_MASK;
                    int blockIdx = ChunkUtil.indexBlock(localX, localY, localZ);

                    int blockId = blockSection.get(blockIdx);
                    if (blockId != 0) {
                        return y + 1.0;
                    }
                }
            }
        }
        return startY;
    }

    private void spawnTargetIndicator(Vector3d impactPos, Store<EntityStore> store, Rotation3f rotation) {
        List<Ref<EntityStore>> nearbyEntities = TargetUtils.getEntitiesInRadius(impactPos, 60.0, store);

        List<Ref<EntityStore>> playersToNotify = nearbyEntities.stream()
                .filter(ref -> ref != null && ref.isValid() && store.getComponent(ref, PlayerRef.getComponentType()) != null)
                .toList();

        if (!playersToNotify.isEmpty()) {
            Vector3d particlePos = new Vector3d(impactPos.x, impactPos.y + 0.15, impactPos.z);
            float particleScale = 0.2f;

            ParticleUtil.spawnParticleEffect(
                    INDICATOR_PARTICLE_ID,
                    particlePos.x(), particlePos.y(), particlePos.z(),
                    rotation.yaw(), rotation.pitch(), rotation.roll(),
                    particleScale,
                    null,
                    null,
                    playersToNotify,
                    store
            );
        }
    }

    private void applyAoeDamage(Ref<EntityStore> playerEntityRef, Vector3d impactPos, float aoeDamage, Store<EntityStore> store) {
        if (!playerEntityRef.isValid()) return;

        List<Ref<EntityStore>> nearbyEntities = TargetUtils.getEntitiesInRadius(impactPos, AOE_RADIUS, store);
        DamageCause damageCause = DamageCause.getAssetMap().getAsset("Fire");
        if (damageCause == null) {
            damageCause = DamageCause.getAssetMap().getAsset(0);
        }

        Damage.Source source = new Damage.EntitySource(playerEntityRef);

        try {
            @SuppressWarnings("unchecked")
            CommandBuffer<EntityStore> commandBuffer = (CommandBuffer<EntityStore>) TAKE_COMMAND_BUFFER_METHOD.invoke(store);

            for (Ref<EntityStore> targetRef : nearbyEntities) {
                if (targetRef == null || !targetRef.isValid() || targetRef.equals(playerEntityRef)) continue;

                Damage damageEvent = new Damage(source, damageCause, aoeDamage);
                damageEvent.putMetaObject(DamageTrackingSystem.RPG_DAMAGE_PROCESSED, false);
                commandBuffer.invoke(targetRef, damageEvent);
            }

            CONSUME_METHOD.invoke(commandBuffer);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}