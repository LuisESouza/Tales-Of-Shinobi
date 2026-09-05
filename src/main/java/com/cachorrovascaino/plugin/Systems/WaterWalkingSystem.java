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
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.entity.entities.Player;
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
import java.util.UUID;

public class WaterWalkingSystem extends EntityTickingSystem<EntityStore> {

    private static final String DEFAULT_WALK_BLOCK = "Barrier";
    private static final float CHAKRA_DRAIN_PER_SECOND = 2.0f;

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

        if (player == null || transform == null || uuidComponent == null) return;

        Vector3d pos = transform.getPosition();

        double checkY = pos.y - 0.2;
        int blockX = MathUtil.floor(pos.x);
        int blockY = MathUtil.floor(checkY);
        int blockZ = MathUtil.floor(pos.z);

        World world = store.getExternalData().getWorld();
        ChunkStore chunkStore = world.getChunkStore();

        long chunkIndex = ChunkUtil.indexChunkFromBlock(blockX, blockZ);
        Ref<ChunkStore> chunkRef = chunkStore.getChunkReference(chunkIndex);

        if (chunkRef == null || !chunkRef.isValid()) return;

        long packed = WorldUtil.getPackedMaterialAndFluidAtPosition(chunkRef, chunkStore.getStore(), pos.x, checkY, pos.z);
        int fluidId = MathUtil.unpackRight(packed);

        if (fluidId != 0) {
            UUID playerUuid = uuidComponent.getUuid();
            PlayerRef playerRef = Universe.get().getPlayer(playerUuid);

            if (playerRef != null && playerRef.isValid()) {
                float cost = CHAKRA_DRAIN_PER_SECOND * dt;

                if (!ChakraUtils.consumeChakra(playerRef, cost)) {
                    commandBuffer.removeComponent(ref, this.waterWalkType);
                    return;
                }

                if (Main.getJutsuManager() != null) {
                    Main.getJutsuManager().updateChakraHud(playerRef);
                }
            }

            Vector3i platformPos = new Vector3i(blockX, blockY, blockZ);

            if (!WaterWalkUtils.isPlatformActive(platformPos)) {
                WaterWalkUtils.createTemporaryPlatform(world, blockX, blockY, blockZ, DEFAULT_WALK_BLOCK, 1200);
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
                this.waterWalkType
        );
    }
}