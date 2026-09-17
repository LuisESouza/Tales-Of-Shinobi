package com.cachorrovascaino.plugin.Systems;

import com.cachorrovascaino.plugin.Data.Components.WaterWalk;
import com.cachorrovascaino.plugin.Main;
import com.cachorrovascaino.plugin.Utils.ChakraUtils;
import com.cachorrovascaino.plugin.Utils.WaterWalkUtils;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.util.MathUtil;
import com.hypixel.hytale.protocol.MovementStates;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.movement.MovementStatesComponent;
import com.hypixel.hytale.server.core.modules.collision.WorldUtil;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.joml.Vector3d;
import org.joml.Vector3i;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class WaterWalkingSystem extends EntityTickingSystem<EntityStore> {

    private static final String DEFAULT_WALK_BLOCK = "Barrier";
    private static final float CHAKRA_DRAIN_PER_SECOND = 2.0f;
    private static final int PLATFORM_RADIUS = 1;

    private final Map<UUID, Vector3d> lastPositions = new ConcurrentHashMap<>();

    private final ComponentType<EntityStore, WaterWalk> waterWalkType;

    public WaterWalkingSystem(ComponentType<EntityStore, WaterWalk> waterWalkType) {
        this.waterWalkType = waterWalkType;
    }

    @Override
    public void tick(float dt, int index, @Nonnull ArchetypeChunk<EntityStore> chunk, @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer) {
        Ref<EntityStore> ref = chunk.getReferenceTo(index);
        if (!ref.isValid()) return;

        Player player = store.getComponent(ref, Player.getComponentType());
        TransformComponent transform = store.getComponent(ref, TransformComponent.getComponentType());
        UUIDComponent uuidComponent = store.getComponent(ref, UUIDComponent.getComponentType());
        MovementStatesComponent movementComponent = store.getComponent(ref, MovementStatesComponent.getComponentType());

        if (player == null || transform == null || uuidComponent == null || dt <= 0.0f) return;

        UUID playerUuid = uuidComponent.getUuid();
        Vector3d currentPos = new Vector3d(transform.getPosition());

        Vector3d lastPos = lastPositions.getOrDefault(playerUuid, currentPos);
        Vector3d moveVector = new Vector3d(currentPos).sub(lastPos);
        lastPositions.put(playerUuid, currentPos);

        double predictionMultiplier = 1.5;

        if (movementComponent != null) {
            MovementStates states = movementComponent.getMovementStates();
            if (states.idle || states.horizontalIdle) {predictionMultiplier = 0.0;}
            if (states.sprinting) {predictionMultiplier = 3.5;}
            if (states.running || states.walking) {predictionMultiplier = 2.0;}
        }

        Vector3d predictedPos = new Vector3d(currentPos).add(
                moveVector.x * predictionMultiplier,
                moveVector.y * predictionMultiplier,
                moveVector.z * predictionMultiplier
        );

        double checkY = predictedPos.y - 0.2;
        int centerBlockX = MathUtil.floor(predictedPos.x);
        int centerBlockY = MathUtil.floor(checkY);
        int centerBlockZ = MathUtil.floor(predictedPos.z);

        World world = store.getExternalData().getWorld();
        ChunkStore chunkStore = world.getChunkStore();

        long chunkIndex = ChunkUtil.indexChunkFromBlock(centerBlockX, centerBlockZ);
        Ref<ChunkStore> chunkRef = chunkStore.getChunkReference(chunkIndex);

        if (chunkRef == null || !chunkRef.isValid()) return;

        long packed = WorldUtil.getPackedMaterialAndFluidAtPosition(chunkStore, predictedPos.x, checkY, predictedPos.z);
        int fluidId = MathUtil.unpackRight(packed);

        if (fluidId == 0) {
            packed = WorldUtil.getPackedMaterialAndFluidAtPosition(chunkStore, currentPos.x, currentPos.y - 0.2, currentPos.z);
            fluidId = MathUtil.unpackRight(packed);

            centerBlockX = MathUtil.floor(currentPos.x);
            centerBlockY = MathUtil.floor(currentPos.y - 0.2);
            centerBlockZ = MathUtil.floor(currentPos.z);
        }

        if (fluidId != 0) {
            PlayerRef playerRef = Universe.get().getPlayer(playerUuid);

            if (playerRef != null && playerRef.isValid()) {
                float cost = CHAKRA_DRAIN_PER_SECOND * dt;

                if (!ChakraUtils.consumeChakra(playerRef, cost)) {
                    commandBuffer.removeComponent(ref, this.waterWalkType);
                    lastPositions.remove(playerUuid);
                    return;
                }

                if (Main.getJutsuManager() != null) {
                    Main.getJutsuManager().updateChakraHud(playerRef);
                }
            }

            for (int dx = -PLATFORM_RADIUS; dx <= PLATFORM_RADIUS; dx++) {
                for (int dz = -PLATFORM_RADIUS; dz <= PLATFORM_RADIUS; dz++) {
                    int targetX = centerBlockX + dx;
                    int targetZ = centerBlockZ + dz;

                    Vector3i platformPos = new Vector3i(targetX, centerBlockY, targetZ);

                    if (!WaterWalkUtils.isPlatformActive(platformPos)) {
                        WaterWalkUtils.createTemporaryPlatform(world, targetX, centerBlockY, targetZ, DEFAULT_WALK_BLOCK, 2500);
                    }
                }
            }
        }
    }

    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        return Query.and(
                Player.getComponentType(),
                TransformComponent.getComponentType(),
                UUIDComponent.getComponentType(),
                MovementStatesComponent.getComponentType(),
                this.waterWalkType
        );
    }
}