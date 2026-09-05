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
    @SuppressWarnings("unchecked")
    public static List<Ref<EntityStore>> getEntitiesInRadius(Vector3d center, double radius, Store<EntityStore> store) {
        Set<Ref<EntityStore>> uniqueEntities = new HashSet<>();

        SpatialResource<Ref<EntityStore>, EntityStore> entitySpatial = store.getResource(EntityModule.get().getEntitySpatialResourceType());
        if (entitySpatial != null) {
            List<Ref<EntityStore>> mobList = (List<Ref<EntityStore>>) (List<?>) SpatialResource.getThreadLocalReferenceList();
            entitySpatial.getSpatialStructure().collect(center, radius, mobList);
            uniqueEntities.addAll(mobList);
        }

        SpatialResource<Ref<EntityStore>, EntityStore> playerSpatial = store.getResource(EntityModule.get().getPlayerSpatialResourceType());
        if (playerSpatial != null) {
            List<Ref<EntityStore>> playerList = (List<Ref<EntityStore>>) (List<?>) SpatialResource.getThreadLocalReferenceList();
            playerSpatial.getSpatialStructure().collect(center, radius, playerList);
            uniqueEntities.addAll(playerList);
        }

        return new ArrayList<>(uniqueEntities);
    }

    /**
     * Encontra a entidade (Player ou Mob) exatamente na mira/mira direcional (Raycast com cone de precisão).
     *
     * @param casterRef EntityRef de quem está lançando o jutsu
     * @param store Store de componentes
     * @param maxDistance Distância máxima do golpe/jutsu (ex: 4.0 para Taijutsu, 20.0 para Kamui)
     * @param toleranceDegrees Ângulo de tolerância para acertar a mira (ex: 20.0 graus)
     * @return Ref<EntityStore> da vítima ou null se não encontrar nada
     */
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