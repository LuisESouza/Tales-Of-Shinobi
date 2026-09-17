package com.cachorrovascaino.plugin.Utils;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.spatial.SpatialResource;
import com.hypixel.hytale.math.vector.Rotation3f;
import com.hypixel.hytale.server.core.modules.entity.EntityModule;
import com.hypixel.hytale.server.core.modules.entity.component.HeadRotation;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.physics.util.PhysicsMath;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.joml.Vector3d;

import java.util.*;

public class TargetUtils {

    /**
     * Coleta TODAS as entidades (Players + Mobs) dentro de um raio a partir de um centro.
     */
    public static List<Ref<EntityStore>> getEntitiesInRadius(Vector3d center, double radius, Store<EntityStore> store) {
        Set<Ref<EntityStore>> uniqueEntities = new HashSet<>();

        SpatialResource<Ref<EntityStore>, EntityStore> entitySpatial = store.getResource(EntityModule.get().getEntitySpatialResourceType());
        if (entitySpatial != null) {
            List<Ref<EntityStore>> temp = new ArrayList<>();
            entitySpatial.getSpatialStructure().collect(center, radius, temp);
            uniqueEntities.addAll(temp);
        }

        SpatialResource<Ref<EntityStore>, EntityStore> playerSpatial = store.getResource(EntityModule.get().getPlayerSpatialResourceType());
        if (playerSpatial != null) {
            List<Ref<EntityStore>> temp = new ArrayList<>();
            playerSpatial.getSpatialStructure().collect(center, radius, temp);
            uniqueEntities.addAll(temp);
        }

        return new ArrayList<>(uniqueEntities);
    }

    /**
     * Coleta APENAS Players dentro de um raio a partir de um centro.
     */
    public static List<Ref<EntityStore>> getPlayersInRadius(Vector3d center, double radius, Store<EntityStore> store) {
        SpatialResource<Ref<EntityStore>, EntityStore> playerSpatial = store.getResource(EntityModule.get().getPlayerSpatialResourceType());
        if (playerSpatial != null) {
            List<Ref<EntityStore>> playerList = new ArrayList<>();
            playerSpatial.getSpatialStructure().collect(center, radius, playerList);
            return playerList;
        }
        return Collections.emptyList();
    }

    public static Ref<EntityStore> getTargetInLineOfSight(
            Ref<EntityStore> casterRef,
            Store<EntityStore> store,
            double maxDistance,
            double toleranceDegrees
    ) {
        TransformComponent casterTransform = store.getComponent(casterRef, TransformComponent.getComponentType());
        if (casterTransform == null) return null;

        HeadRotation headRot = store.getComponent(casterRef, HeadRotation.getComponentType());
        Rotation3f rot = (headRot != null) ? headRot.getRotation() : casterTransform.getRotation();

        Vector3d lookDir = new Vector3d();
        PhysicsMath.vectorFromAngles(rot.yaw(), rot.pitch(), lookDir);
        lookDir.normalize();

        Vector3d eyePos = new Vector3d(casterTransform.getPosition()).add(0, 1.5, 0);

        List<Ref<EntityStore>> nearby = getEntitiesInRadius(eyePos, maxDistance, store);

        Ref<EntityStore> bestTarget = null;
        double minAngleDiff = Math.toRadians(toleranceDegrees);

        for (Ref<EntityStore> entityRef : nearby) {
            if (entityRef == null || !entityRef.isValid() || entityRef.equals(casterRef)) continue;

            TransformComponent targetTransform = store.getComponent(entityRef, TransformComponent.getComponentType());
            if (targetTransform == null) continue;

            Vector3d targetPos = new Vector3d(targetTransform.getPosition()).add(0, 1.0, 0);
            Vector3d toTarget = new Vector3d(targetPos).sub(eyePos);

            double distance = toTarget.length();
            if (distance > maxDistance || distance == 0) continue;

            toTarget.normalize();

            double dot = Math.max(-1.0, Math.min(1.0, lookDir.dot(toTarget)));
            double angle = Math.acos(dot);

            if (angle < minAngleDiff) {
                minAngleDiff = angle;
                bestTarget = entityRef;
            }
        }

        return bestTarget;
    }
}