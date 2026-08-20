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
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.modules.entity.EntityModule;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageCause;
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

public class LionCombo implements Jutsu {

    public static final LionCombo INSTANCE = new LionCombo();

    private static final double RADIUS = 4.5;
    private static final float BASE_DAMAGE_PER_TICK = 12.0f;
    private static final float DAMAGE_PER_LEVEL = 3.0f;
    private static final String PARTICLE_ID = "Leaf_Hurricane_Circle";

    // Configurações da duração do combo
    private static final int DURATION_SECONDS = 5;
    private static final int TICK_INTERVAL_MS = 500;

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

    @Override public String getId() { return "lion_combo"; }
    @Override public String getDisplayName() { return JutsuType.LION_COMBO.getName(); }
    @Override public float getChakraCost() { return JutsuType.LION_COMBO.getResourceCost(); }
    @Override public float getCooldown() { return JutsuType.LION_COMBO.getCooldown(); }
    @Override public SkillType getType() { return SkillType.TAIJUTSU; }

    public float getDamageForPlayer(PlayerRef playerRef) {
        PlayerData data = Main.getDataManager().getPlayerData(playerRef.getUuid());
        int level = (data != null) ? data.getJutsuLevel(getId()) : 1;
        return BASE_DAMAGE_PER_TICK + ((level - 1) * DAMAGE_PER_LEVEL);
    }

    @Override
    public void execute(PlayerRef playerRef, Ref<EntityStore> playerEntityRef, Store<EntityStore> store, World world) {
        if (playerRef == null || playerEntityRef == null || !playerEntityRef.isValid()) return;

        playerRef.sendMessage(Message.raw(" Shishirendan Ativado! (" + DURATION_SECONDS + "s)").color(Color.ORANGE));

        long startTime = System.currentTimeMillis();
        long durationMs = DURATION_SECONDS * 1000L;

        SCHEDULER.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                long elapsed = System.currentTimeMillis() - startTime;

                if (elapsed >= durationMs || !playerEntityRef.isValid()) {
                    throw new RuntimeException("LionCombo Finished");
                }

                world.execute(() -> {
                    if (!playerEntityRef.isValid()) return;

                    TransformComponent transform = store.getComponent(playerEntityRef, TransformComponent.getComponentType());
                    if (transform == null) return;

                    Vector3d currentPos = transform.getPosition();

                    spawnSpiralParticles(currentPos, store, transform);
                    applyContinuousDamage(playerRef, playerEntityRef, currentPos, store);
                });
            }
        }, 0, TICK_INTERVAL_MS, TimeUnit.MILLISECONDS);
    }

    private void applyContinuousDamage(PlayerRef playerRef, Ref<EntityStore> playerEntityRef, Vector3d center, Store<EntityStore> store) {
        SpatialResource<Ref<EntityStore>, EntityStore> spatial = store.getResource(EntityModule.get().getEntitySpatialResourceType());

        @SuppressWarnings("unchecked")
        List<Ref<EntityStore>> nearbyEntities = (List<Ref<EntityStore>>) (List<?>) SpatialResource.getThreadLocalReferenceList();
        spatial.getSpatialStructure().collect(center, RADIUS, nearbyEntities);

        DamageCause damageCause = DamageCause.getAssetMap().getAsset("Physical");
        if (damageCause == null) {
            damageCause = DamageCause.getAssetMap().getAsset(0);
        }

        Damage.Source source = new Damage.EntitySource(playerEntityRef);

        try {
            @SuppressWarnings("unchecked")
            CommandBuffer<EntityStore> commandBuffer = (CommandBuffer<EntityStore>) TAKE_COMMAND_BUFFER_METHOD.invoke(store);

            for (Ref<EntityStore> targetRef : nearbyEntities) {
                if (targetRef == null || !targetRef.isValid() || targetRef.equals(playerEntityRef)) {
                    continue;
                }

                Damage damageEvent = new Damage(source, damageCause, getDamageForPlayer(playerRef));
                damageEvent.putMetaObject(DamageTrackingSystem.RPG_DAMAGE_PROCESSED, false);

                commandBuffer.invoke(targetRef, damageEvent);
            }

            CONSUME_METHOD.invoke(commandBuffer);
        } catch (Exception e) {}
    }

    private void spawnSpiralParticles(Vector3d center, Store<EntityStore> store, TransformComponent transform) {
        SpatialResource<Ref<EntityStore>, EntityStore> playerSpatial = store.getResource(EntityModule.get().getPlayerSpatialResourceType());

        @SuppressWarnings("unchecked")
        List<Ref<EntityStore>> playersToNotify = (List<Ref<EntityStore>>) (List<?>) SpatialResource.getThreadLocalReferenceList();
        playerSpatial.getSpatialStructure().collect(center, 75.0, playersToNotify);

        if (playersToNotify.isEmpty()) return;

        Vector3d groundPos = new Vector3d(center.x, center.y + 0.2, center.z);
        ParticleUtil.spawnParticleEffect(PARTICLE_ID, groundPos, transform.getRotation(), playersToNotify, store);

        Vector3d topPos = new Vector3d(center.x, center.y + 5.2, center.z);
        ParticleUtil.spawnParticleEffect(PARTICLE_ID, topPos, transform.getRotation(), playersToNotify, store);
    }
}