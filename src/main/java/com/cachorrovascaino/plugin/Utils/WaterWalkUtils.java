package com.cachorrovascaino.plugin.Utils;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.util.MathUtil;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.modules.collision.WorldUtil;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import org.joml.Vector3i;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class WaterWalkUtils {

    private static class PlatformData {
        final String originalBlockKey;
        ScheduledFuture<?> removalTask;

        PlatformData(String originalBlockKey, ScheduledFuture<?> removalTask) {
            this.originalBlockKey = originalBlockKey;
            this.removalTask = removalTask;
        }
    }

    private static final Map<Vector3i, PlatformData> ACTIVE_WATER_PLATFORMS = new ConcurrentHashMap<>();

    public static boolean isPlatformActive(Vector3i pos) {
        return ACTIVE_WATER_PLATFORMS.containsKey(pos);
    }

    /**
     * Gera uma plataforma 3x3 de blocos temporários apenas onde houver líquido (fluidId != 0).
     */
    public static void createTemporaryPlatform(World world, int centerX, int y, int centerZ, String solidBlockKey, long durationMs) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                int targetX = centerX + dx;
                int targetZ = centerZ + dz;

                Vector3i pos = new Vector3i(targetX, y, targetZ);

                if (ACTIVE_WATER_PLATFORMS.containsKey(pos)) {
                    PlatformData data = ACTIVE_WATER_PLATFORMS.get(pos);
                    if (data != null && data.removalTask != null) {
                        data.removalTask.cancel(false);
                        data.removalTask = scheduleRemoval(world, pos, durationMs);
                    }
                    continue;
                }

                if (!isFluidAt(world, targetX, y, targetZ)) {
                    continue;
                }

                BlockType currentType = world.getBlockType(pos.x, pos.y, pos.z);
                String oldKey = (currentType != null && !currentType.isUnknown()) ? currentType.getId() : "Empty";

                world.setBlock(pos.x, pos.y, pos.z, solidBlockKey);

                ScheduledFuture<?> task = scheduleRemoval(world, pos, durationMs);
                ACTIVE_WATER_PLATFORMS.put(pos, new PlatformData(oldKey, task));
            }
        }
    }

    /**
     * Replica a verificação nativa de fluido usada no InFluidCondition da engine do Hytale.
     */
    private static boolean isFluidAt(World world, int x, int y, int z) {
        if (world == null) return false;

        try {
            ChunkStore chunkStore = world.getChunkStore();
            if (chunkStore == null) return false;

            Store<ChunkStore> chunkComponentStore = chunkStore.getStore();
            Ref<ChunkStore> chunkRef = chunkStore.getChunkReference(ChunkUtil.indexChunkFromBlock(x, z));

            if (chunkRef != null && chunkRef.isValid()) {
                long packed = WorldUtil.getPackedMaterialAndFluidAtPosition(chunkRef, chunkComponentStore, (double) x, (double) y, (double) z);
                int fluidId = MathUtil.unpackRight(packed);
                return fluidId != 0;
            }
        } catch (Exception ignored) {}

        return false;
    }

    private static ScheduledFuture<?> scheduleRemoval(World world, Vector3i pos, long durationMs) {
        return BlockJutsuUtils.SCHEDULER.schedule(() -> {
            world.execute(() -> {
                PlatformData data = ACTIVE_WATER_PLATFORMS.remove(pos);
                if (data != null) {
                    String originalBlock = data.originalBlockKey;
                    world.setBlock(pos.x, pos.y, pos.z, "Empty".equalsIgnoreCase(originalBlock) ? "Empty" : originalBlock);
                }
            });
        }, durationMs, TimeUnit.MILLISECONDS);
    }
}