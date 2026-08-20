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

public class LeafHurricane implements Jutsu {

    public static final LeafHurricane INSTANCE = new LeafHurricane();

    private static final double RADIUS = 6.0;
    private static final float BASE_DAMAGE = 35.0f;

    private static final float DAMAGE_PER_LEVEL = 7.0f;
    private static final double RADIUS_PER_LEVEL = 1.0;
    private static final String PARTICLE_ID = "Leaf_Hurricane_Circle";

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

    @Override public String getId() { return "leaf_hurricane"; }
    @Override public String getDisplayName() { return "Konoha Senpū"; }
    @Override public float getChakraCost() { return JutsuType.LEAF_HURRICANE.getResourceCost(); }
    @Override public float getCooldown() { return JutsuType.LEAF_HURRICANE.getCooldown(); }
    @Override public SkillType getType() { return SkillType.TAIJUTSU; }

    public double getRadiusForPlayer(PlayerRef playerRef) {
        PlayerData data = Main.getDataManager().getPlayerData(playerRef.getUuid());
        int level = (data != null) ? data.getJutsuLevel(getId()) : 1;
        int effectiveLevel = Math.min(level, 4);

        return RADIUS + ((effectiveLevel - 1) * RADIUS_PER_LEVEL);
    }

    public float getDamageForPlayer(PlayerRef playerRef) {
        PlayerData data = Main.getDataManager().getPlayerData(playerRef.getUuid());
        int level = (data != null) ? data.getJutsuLevel(getId()) : 1;

        return BASE_DAMAGE + ((level - 1) * DAMAGE_PER_LEVEL);
    }

    @Override
    public void execute(PlayerRef playerRef, Ref<EntityStore> playerEntityRef, Store<EntityStore> store, World world) {
        if (playerRef == null || playerEntityRef == null || !playerEntityRef.isValid()) return;

        world.execute(() -> {
            if (!playerEntityRef.isValid()) return;

            TransformComponent transform = store.getComponent(playerEntityRef, TransformComponent.getComponentType());
            if (transform == null) return;

            Vector3d playerPos = transform.getPosition();
            double radius = getRadiusForPlayer(playerRef);

            spawnParticleRing(playerPos, radius, 36, store, transform);

            SpatialResource<Ref<EntityStore>, EntityStore> spatial = store.getResource(EntityModule.get().getEntitySpatialResourceType());

            @SuppressWarnings("unchecked")
            List<Ref<EntityStore>> nearbyEntities = (List<Ref<EntityStore>>) (List<?>) SpatialResource.getThreadLocalReferenceList();
            spatial.getSpatialStructure().collect(playerPos, radius, nearbyEntities);

            int targetsHit = 0;

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
                    targetsHit++;
                }

                CONSUME_METHOD.invoke(commandBuffer);
            } catch (Exception e) {
                e.printStackTrace();
            }

            playerRef.sendMessage(Message.raw(" Konoha Senpū! Entidades atingidas: " + targetsHit).color(Color.GREEN));
        });
    }

    private void spawnParticleRing(Vector3d center, double radius, int points, Store<EntityStore> store, TransformComponent transform) {
        SpatialResource<Ref<EntityStore>, EntityStore> playerSpatial = store.getResource(EntityModule.get().getPlayerSpatialResourceType());

        @SuppressWarnings("unchecked")
        List<Ref<EntityStore>> playersToNotify = (List<Ref<EntityStore>>) (List<?>) SpatialResource.getThreadLocalReferenceList();
        playerSpatial.getSpatialStructure().collect(center, 75.0, playersToNotify);

        if (playersToNotify.isEmpty()) return;

        double increment = (2 * Math.PI) / points;

        for (int i = 0; i < points; i++) {
            double angle = i * increment;
            double x = center.x + (radius * Math.cos(angle));
            double y = center.y + 0.2;
            double z = center.z + (radius * Math.sin(angle));

            Vector3d pointPos = new Vector3d(x, y, z);

            ParticleUtil.spawnParticleEffect(PARTICLE_ID, pointPos, transform.getRotation(), playersToNotify, store);
        }
    }
}