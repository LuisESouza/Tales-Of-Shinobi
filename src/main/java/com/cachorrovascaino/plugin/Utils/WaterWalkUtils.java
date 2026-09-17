package com.cachorrovascaino.plugin.Utils;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.util.MathUtil;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.modules.collision.WorldUtil;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.section.BlockSection;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import org.joml.Vector3i;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class WaterWalkUtils {

    private static class PlatformData {
        final int originalBlockId;
        final int originalRotation;
        final int originalFiller;
        ScheduledFuture<?> removalTask;

        PlatformData(int originalBlockId, int originalRotation, int originalFiller, ScheduledFuture<?> removalTask) {
            this.originalBlockId = originalBlockId;
            this.originalRotation = originalRotation;
            this.originalFiller = originalFiller;
            this.removalTask = removalTask;
        }
    }

    private static final Map<Vector3i, PlatformData> ACTIVE_WATER_PLATFORMS = new ConcurrentHashMap<>();

    public static boolean isPlatformActive(Vector3i pos) {
        return ACTIVE_WATER_PLATFORMS.containsKey(pos);
    }

    public static void createTemporaryPlatform(World world, int centerX, int y, int centerZ, String solidBlockKey, long durationMs) {
        int newBlockId = BlockType.getAssetMap().getIndex(solidBlockKey);

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

                setBlockAtInstant(world, pos.x, pos.y, pos.z, newBlockId, 0, 0, (oldId, oldRotation, oldFiller) -> {
                    ScheduledFuture<?> task = scheduleRemoval(world, pos, durationMs);
                    ACTIVE_WATER_PLATFORMS.put(pos, new PlatformData(oldId, oldRotation, oldFiller, task));
                });
            }
        }
    }

    @FunctionalInterface
    private interface BlockChangeCallback {
        void onComplete(int oldBlockId, int oldRotation, int oldFiller);
    }

    /**
     * Tenta pegar a referência diretamente na memória de forma síncrona/instantânea.
     * Caso o chunk por algum motivo raro não esteja pronto, faz o fallback pro Async.
     */
    private static void setBlockAtInstant(World world, int x, int y, int z, int newBlockId, int rotation, int filler, BlockChangeCallback callback) {
        ChunkStore chunkStore = world.getChunkStore();
        if (chunkStore == null) return;

        int chunkX = ChunkUtil.chunkCoordinate(x);
        int chunkY = ChunkUtil.chunkCoordinate(y);
        int chunkZ = ChunkUtil.chunkCoordinate(z);

        Ref<ChunkStore> sectionRef = chunkStore.getChunkSectionReference(chunkX, chunkY, chunkZ);

        if (sectionRef != null && sectionRef.isValid()) {
            applyBlockDirect(chunkStore, sectionRef, x, y, z, newBlockId, rotation, filler, callback);
        } else {
            chunkStore.getChunkSectionReferenceAsync(chunkX, chunkY, chunkZ).thenAcceptAsync(ref -> {
                if (ref != null && ref.isValid()) {
                    applyBlockDirect(chunkStore, ref, x, y, z, newBlockId, rotation, filler, callback);
                }
            }, world);
        }
    }

    private static void applyBlockDirect(ChunkStore chunkStore, Ref<ChunkStore> ref, int x, int y, int z, int newBlockId, int rotation, int filler, BlockChangeCallback callback) {
        BlockSection blockSection = chunkStore.getStore().getComponent(ref, BlockSection.getComponentType());
        if (blockSection != null) {
            int localX = x & ChunkUtil.SIZE_MASK;
            int localY = y & ChunkUtil.SIZE_MASK;
            int localZ = z & ChunkUtil.SIZE_MASK;

            int blockIdx = ChunkUtil.indexBlock(localX, localY, localZ);

            int oldBlockId = blockSection.get(blockIdx);
            int oldRotation = blockSection.getRotationIndex(blockIdx);
            int oldFiller = blockSection.getFiller(blockIdx);

            blockSection.set(blockIdx, newBlockId, rotation, filler);

            if (callback != null) {
                callback.onComplete(oldBlockId, oldRotation, oldFiller);
            }
        }
    }

    private static boolean isFluidAt(World world, int x, int y, int z) {
        if (world == null) return false;

        try {
            ChunkStore chunkStore = world.getChunkStore();
            if (chunkStore == null) return false;

            Ref<ChunkStore> chunkRef = chunkStore.getChunkReference(ChunkUtil.indexChunkFromBlock(x, z));

            if (chunkRef != null && chunkRef.isValid()) {
                long packed = WorldUtil.getPackedMaterialAndFluidAtPosition(chunkStore, (double) x, (double) y, (double) z);
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
                    setBlockAtInstant(world, pos.x, pos.y, pos.z, data.originalBlockId, data.originalRotation, data.originalFiller, null);
                }
            });
        }, durationMs, TimeUnit.MILLISECONDS);
    }
}