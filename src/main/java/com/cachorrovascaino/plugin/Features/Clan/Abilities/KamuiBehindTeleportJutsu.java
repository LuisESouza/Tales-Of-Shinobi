package com.cachorrovascaino.plugin.Features.Clan.Abilities;

import com.cachorrovascaino.plugin.Abstractions.ClanJutsu;
import com.cachorrovascaino.plugin.Data.Components.MangekyouSharingan;
import com.cachorrovascaino.plugin.Data.PlayerData;
import com.cachorrovascaino.plugin.Main;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.spatial.SpatialResource;
import com.hypixel.hytale.math.vector.Rotation3f;
import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.modules.entity.EntityModule;
import com.hypixel.hytale.server.core.modules.entity.component.HeadRotation;
import com.hypixel.hytale.server.core.modules.entity.component.Intangible;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.teleport.Teleport;
import com.hypixel.hytale.server.core.modules.physics.util.PhysicsMath;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.joml.Vector3d;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class KamuiBehindTeleportJutsu implements ClanJutsu {

    public static final KamuiBehindTeleportJutsu INSTANCE = new KamuiBehindTeleportJutsu();
    private static final ScheduledExecutorService SCHEDULER = Executors.newSingleThreadScheduledExecutor();
    private static final double DASH_RANGE = 25.0;

    private KamuiBehindTeleportJutsu() {}

    @Override public String getId() { return "kamui_behind_teleport"; }
    @Override public String getDisplayName() { return "Kamui: Temporal Ambush"; }
    @Override public float getChakraCost() { return 40.0f; }
    @Override public float getCooldown() { return 10.0f; }

    @Override
    public boolean canExecute(PlayerRef playerRef) {
        PlayerData playerData = Main.getDataManager().getPlayerData(playerRef.getUuid());
        if (playerData == null) return false;

        if (!"OBITO".equalsIgnoreCase(playerData.getMangekyouType())) {
            playerRef.sendMessage(Message.raw("This Mangekyō Sharingan does not possess the Kamui technique!").color(Color.RED));
            return false;
        }

        return true;
    }

    @Override
    public void execute(PlayerRef playerRef, Ref<EntityStore> playerEntityRef, Store<EntityStore> store, World world) {
        if (playerEntityRef == null || !playerEntityRef.isValid() || world == null) return;
        if (!canExecute(playerRef)) return;

        ComponentType<EntityStore, MangekyouSharingan> mangekyouType = Main.get().getMangekyouSharinganComponentType();

        world.execute(() -> {
            if (!playerEntityRef.isValid()) return;

            if (mangekyouType == null || store.getComponent(playerEntityRef, mangekyouType) == null) {
                playerRef.sendMessage(Message.raw("Activate Mangekyō Sharingan to use Kamui!").color(Color.RED));
                return;
            }

            TransformComponent playerTransform = store.getComponent(playerEntityRef, TransformComponent.getComponentType());
            if (playerTransform == null) return;

            HeadRotation headRotation = store.getComponent(playerEntityRef, HeadRotation.getComponentType());
            Rotation3f originalRotation = (headRotation != null) ? headRotation.getRotation() : playerTransform.getRotation();

            Vector3d lookVector = new Vector3d();
            PhysicsMath.vectorFromAngles(originalRotation.yaw(), originalRotation.pitch(), lookVector);
            lookVector.normalize();

            Vector3d searchCenter = new Vector3d(playerTransform.getPosition()).add(new Vector3d(lookVector).mul(3.0));

            SpatialResource<Ref<EntityStore>, EntityStore> spatial = store.getResource(EntityModule.get().getEntitySpatialResourceType());
            if (spatial == null || spatial.getSpatialStructure() == null) {
                playerRef.sendMessage(Message.raw("Error accessing the game world.").color(Color.RED));
                return;
            }

            List<Ref<EntityStore>> nearbyEntities = new ArrayList<>();
            spatial.getSpatialStructure().collect(searchCenter, DASH_RANGE, nearbyEntities);

            Ref<EntityStore> targetRef = null;
            for (Ref<EntityStore> entity : nearbyEntities) {
                if (entity != null && entity.isValid() && !entity.equals(playerEntityRef)) {
                    targetRef = entity;
                    break;
                }
            }

            if (targetRef == null) {
                playerRef.sendMessage(Message.raw("Kamui failed: no target in line of sight.").color(Color.GRAY));
                return;
            }

            TransformComponent targetTrans = store.getComponent(targetRef, TransformComponent.getComponentType());
            if (targetTrans == null) return;

            Vector3d targetPos = targetTrans.getPosition();
            HeadRotation targetHead = store.getComponent(targetRef, HeadRotation.getComponentType());
            Rotation3f targetRot = (targetHead != null) ? targetHead.getRotation() : targetTrans.getRotation();

            double yawRad = targetRot.yaw();
            double behindX = targetPos.x - (Math.sin(yawRad) * 1.5);
            double behindZ = targetPos.z - (Math.cos(yawRad) * 1.5);
            Vector3d destination = new Vector3d(behindX, targetPos.y, behindZ);

            store.ensureComponent(playerEntityRef, Intangible.getComponentType());
            teleportEntity(world, store, playerEntityRef, destination, targetRot);

            playerRef.sendMessage(Message.raw("Kamui!").color(Color.MAGENTA));

            SCHEDULER.schedule(() -> {
                world.execute(() -> {
                    if (playerEntityRef.isValid()) {
                        store.removeComponent(playerEntityRef, Intangible.getComponentType());
                    }
                });
            }, 800, TimeUnit.MILLISECONDS);
        });
    }

    private void teleportEntity(World world, Store<EntityStore> store, Ref<EntityStore> entityRef, Vector3d position, Rotation3f rotation) {
        if (entityRef == null || !entityRef.isValid()) return;

        Transform targetTransform = new Transform(
                position.x, position.y, position.z,
                0.0f,
                rotation != null ? rotation.yaw() : 0.0f,
                0.0f
        );

        Teleport teleportComponent = Teleport.createForPlayer(world, targetTransform);
        store.addComponent(entityRef, Teleport.getComponentType(), teleportComponent);

        HeadRotation head = store.getComponent(entityRef, HeadRotation.getComponentType());
        if (head != null && rotation != null) {
            head.setRotation(new Rotation3f(0.0f, rotation.yaw(), 0.0f));
        }
    }
}