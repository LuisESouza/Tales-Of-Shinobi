package com.cachorrovascaino.plugin.Features.Genjutsu;

import com.cachorrovascaino.plugin.Abstractions.Jutsu;
import com.cachorrovascaino.plugin.Abstractions.SkillType;
import com.cachorrovascaino.plugin.Data.Jutsus.JutsuType;
import com.cachorrovascaino.plugin.Utils.WeatherUtils;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.spatial.SpatialResource;
import com.hypixel.hytale.math.vector.Rotation3f;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.io.PacketHandler;
import com.hypixel.hytale.server.core.modules.entity.EntityModule;
import com.hypixel.hytale.server.core.modules.entity.component.HeadRotation;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.physics.util.PhysicsMath;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.hypixel.hytale.server.npc.role.Role;
import org.joml.Vector3d;

import java.awt.Color;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class Kokuangyo implements Jutsu {

    public static final Kokuangyo INSTANCE = new Kokuangyo();

    private static final double RANGE = 12.0;
    private static final String GENJUTSU_WEATHER = "Kokuangyo_Genjutsu";
    private static final String DEFAULT_WEATHER = "Sun";

    private static final long DURATION_SECONDS = 5;

    private static final ScheduledExecutorService SCHEDULER = Executors.newScheduledThreadPool(2);

    @Override public String getId() { return "kokuangyo_genjutsu"; }
    @Override public String getDisplayName() { return JutsuType.KOKUANGYO.getName(); }
    @Override public float getChakraCost() { return JutsuType.KOKUANGYO.getResourceCost(); }
    @Override public float getCooldown() { return JutsuType.KOKUANGYO.getCooldown(); }
    @Override public SkillType getType() { return SkillType.GENJUTSU; }
    @Override public float getChakraCost(PlayerRef playerRef) { return getChakraCost(); }

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
        spatial.getSpatialStructure().collect(searchCenter, RANGE, nearbyEntities);

        Ref<EntityStore> targetEntityRef = null;
        PlayerRef targetPlayerRef = null;

        for (Ref<EntityStore> entity : nearbyEntities) {
            if (entity != null && entity.isValid() && !entity.equals(playerEntityRef)) {
                targetEntityRef = entity;
                targetPlayerRef = store.getComponent(entity, PlayerRef.getComponentType());
                break;
            }
        }

        boolean isSelfTest = false;

        if (targetPlayerRef != null) {
            PacketHandler targetPacketHandler = targetPlayerRef.getPacketHandler();
            WeatherUtils.applyPlayerWeather(targetPlayerRef, targetPacketHandler, GENJUTSU_WEATHER);

            if (!isSelfTest) {
                playerRef.sendMessage(Message.raw(" You trapped " + targetPlayerRef.getUsername() + " in Absolute Darkness!").color(Color.DARK_GRAY));
            }
            targetPlayerRef.sendMessage(Message.raw(" You fell into Kokuangyo no Jutsu! Your vision has been blinded by darkness.").color(Color.BLACK));

            final PlayerRef finalTargetPlayerRef = targetPlayerRef;
            SCHEDULER.schedule(() -> {
                world.execute(() -> {
                    WeatherUtils.resetPlayerWeather(finalTargetPlayerRef, targetPacketHandler, DEFAULT_WEATHER);
                    finalTargetPlayerRef.sendMessage(Message.raw(" The Kokuangyo no Jutsu has dissipated.").color(Color.GRAY));
                });
            }, DURATION_SECONDS, TimeUnit.SECONDS);
        }
        else {
            applyGenjutsuToMob(targetEntityRef, store, world);
            playerRef.sendMessage(Message.raw(" The creature was blinded by Kokuangyo no Jutsu and lost sight of you!").color(Color.DARK_GRAY));
        }
    }

    /**
     * Mantém o Mob sem foco/alvo durante toda a duração do Genjutsu.
     */
    private void applyGenjutsuToMob(Ref<EntityStore> mobRef, Store<EntityStore> store, World world) {
        if (mobRef == null || !mobRef.isValid()) return;
        if (NPCEntity.getComponentType() == null) return;

        ScheduledFuture<?> blindTask = SCHEDULER.scheduleAtFixedRate(() -> {
            world.execute(() -> {
                if (!mobRef.isValid()) return;

                NPCEntity npcEntity = store.getComponent(mobRef, NPCEntity.getComponentType());
                if (npcEntity == null) return;
            });
        }, 0, 200, TimeUnit.MILLISECONDS);

        SCHEDULER.schedule(() -> {
            blindTask.cancel(true);
        }, DURATION_SECONDS, TimeUnit.SECONDS);
    }
}