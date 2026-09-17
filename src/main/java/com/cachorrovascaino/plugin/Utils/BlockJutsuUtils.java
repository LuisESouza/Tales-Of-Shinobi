package com.cachorrovascaino.plugin.Utils;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Rotation3f;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.modules.entity.component.HeadRotation;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.physics.util.PhysicsMath;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.section.BlockSection;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.joml.Vector3d;
import org.joml.Vector3i;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class BlockJutsuUtils {

    public static final ScheduledExecutorService SCHEDULER = Executors.newSingleThreadScheduledExecutor();

    private static class OriginalBlockData {
        final int blockId;
        final int rotation;
        final int filler;

        OriginalBlockData(int blockId, int rotation, int filler) {
            this.blockId = blockId;
            this.rotation = rotation;
            this.filler = filler;
        }
    }

    /**
     * Pega o vetor para onde o jogador está olhando no plano XZ (horizontal puro).
     */
    public static Vector3d getHorizontalLookVector(
            Ref<EntityStore> playerEntityRef,
            Store<EntityStore> store,
            TransformComponent transformComp
    ) {
        HeadRotation headRotation = store.getComponent(playerEntityRef, HeadRotation.getComponentType());
        Rotation3f rotation = (headRotation != null) ? headRotation.getRotation() : transformComp.getRotation();

        Vector3d lookVector = new Vector3d();
        PhysicsMath.vectorFromAngles(rotation.yaw(), rotation.pitch(), lookVector);
        lookVector.y = 0;

        if (lookVector.lengthSquared() < 0.0001) {
            lookVector.set(0, 0, 1);
        } else {
            lookVector.normalize();
        }

        return lookVector;
    }

    /**
     * Calcula o vetor perpendicular à visão (vetor lateral/direita).
     */
    public static Vector3d getRightVector(Vector3d lookVector) {
        return new Vector3d(lookVector).cross(0.0, 1.0, 0.0).normalize();
    }

    /**
     * Algoritmo de Bresenham 2D: Gera uma linha contínua de blocos entre dois pontos (sem buracos).
     */
    public static Set<Vector3i> getLine2D(int x0, int z0, int x1, int z1, int y) {
        Set<Vector3i> line = new HashSet<>();

        int dx = Math.abs(x1 - x0);
        int dz = Math.abs(z1 - z0);

        int sx = x0 < x1 ? 1 : -1;
        int sz = z0 < z1 ? 1 : -1;

        int err = dx - dz;

        int currX = x0;
        int currZ = z0;

        while (true) {
            line.add(new Vector3i(currX, y, currZ));

            if (currX == x1 && currZ == z1) break;

            int e2 = 2 * err;
            if (e2 > -dz) {
                err -= dz;
                currX += sx;
            }
            if (e2 < dx) {
                err += dx;
                currZ += sz;
            }
        }

        return line;
    }

    /**
     * Gera um disco/círculo preenchido no chão (útil para Pilares, Domos ou Áreas de Efeito).
     */
    public static Set<Vector3i> getCircle2D(int centerX, int centerZ, int radius, int y) {
        Set<Vector3i> circle = new HashSet<>();
        int radiusSq = radius * radius;

        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                if ((x * x) + (z * z) <= radiusSq) {
                    circle.add(new Vector3i(centerX + x, y, centerZ + z));
                }
            }
        }

        return circle;
    }

    /**
     * Constrói uma estrutura animada por camadas usando um ÚNICO bloco fixo (ex: "Rock_Stone", "Wood_Log_Oak").
     */
    public static void spawnAnimatedStructure(
            World world,
            Map<Integer, Set<Vector3i>> layersByStep,
            String blockKey,
            long delayBetweenStepsMs,
            long wallDurationMs
    ) {
        Map<Vector3i, OriginalBlockData> originalBlocks = new ConcurrentHashMap<>();
        int newBlockId = BlockType.getAssetMap().getIndex(blockKey);

        layersByStep.forEach((step, blockPositions) -> {
            long delay = step * delayBetweenStepsMs;

            SCHEDULER.schedule(() -> {
                world.execute(() -> {
                    for (Vector3i pos : blockPositions) {
                        setBlockInstant(world, pos, newBlockId, 0, 0, originalBlocks);
                    }
                });
            }, delay, TimeUnit.MILLISECONDS);
        });

        scheduleCleanup(world, originalBlocks, layersByStep.size(), delayBetweenStepsMs, wallDurationMs);
    }

    /**
     * Constrói uma estrutura animada onde CADA COLUNA herda dinamicamente o bloco do chão original.
     */
    public static void spawnAnimatedDynamicStructure(
            World world,
            Map<Integer, Set<Vector3i>> layersByStep,
            Map<Vector3i, String> customBlockPerColumn,
            long delayBetweenStepsMs,
            long wallDurationMs
    ) {
        Map<Vector3i, OriginalBlockData> originalBlocks = new ConcurrentHashMap<>();

        int groundY = layersByStep.containsKey(0) && !layersByStep.get(0).isEmpty()
                ? layersByStep.get(0).iterator().next().y
                : 0;

        layersByStep.forEach((step, blockPositions) -> {
            long delay = step * delayBetweenStepsMs;

            SCHEDULER.schedule(() -> {
                world.execute(() -> {
                    for (Vector3i pos : blockPositions) {
                        Vector3i baseGroundPos = new Vector3i(pos.x, groundY, pos.z);
                        String dynamicBlockKey = customBlockPerColumn.getOrDefault(baseGroundPos, "Rock_Stone");
                        int dynamicBlockId = BlockType.getAssetMap().getIndex(dynamicBlockKey);

                        setBlockInstant(world, pos, dynamicBlockId, 0, 0, originalBlocks);
                    }
                });
            }, delay, TimeUnit.MILLISECONDS);
        });

        scheduleCleanup(world, originalBlocks, layersByStep.size(), delayBetweenStepsMs, wallDurationMs);
    }

    /**
     * Aplica a alteração do bloco de forma síncrona diretamente na BlockSection da ChunkStore.
     */
    private static void setBlockInstant(
            World world,
            Vector3i pos,
            int newBlockId,
            int rotation,
            int filler,
            Map<Vector3i, OriginalBlockData> originalBlocks
    ) {
        ChunkStore chunkStore = world.getChunkStore();
        if (chunkStore == null) return;

        int chunkX = ChunkUtil.chunkCoordinate(pos.x);
        int chunkY = ChunkUtil.chunkCoordinate(pos.y);
        int chunkZ = ChunkUtil.chunkCoordinate(pos.z);

        Ref<ChunkStore> sectionRef = chunkStore.getChunkSectionReference(chunkX, chunkY, chunkZ);

        if (sectionRef != null && sectionRef.isValid()) {
            applyBlockToSection(chunkStore, sectionRef, pos, newBlockId, rotation, filler, originalBlocks);
        } else {
            chunkStore.getChunkSectionReferenceAsync(chunkX, chunkY, chunkZ).thenAcceptAsync(ref -> {
                if (ref != null && ref.isValid()) {
                    applyBlockToSection(chunkStore, ref, pos, newBlockId, rotation, filler, originalBlocks);
                }
            }, world);
        }
    }

    private static void applyBlockToSection(
            ChunkStore chunkStore,
            Ref<ChunkStore> ref,
            Vector3i pos,
            int newBlockId,
            int rotation,
            int filler,
            Map<Vector3i, OriginalBlockData> originalBlocks
    ) {
        BlockSection blockSection = chunkStore.getStore().getComponent(ref, BlockSection.getComponentType());
        if (blockSection == null) return;

        int localX = pos.x & ChunkUtil.SIZE_MASK;
        int localY = pos.y & ChunkUtil.SIZE_MASK;
        int localZ = pos.z & ChunkUtil.SIZE_MASK;
        int blockIdx = ChunkUtil.indexBlock(localX, localY, localZ);

        if (originalBlocks != null && !originalBlocks.containsKey(pos)) {
            int oldBlockId = blockSection.get(blockIdx);
            int oldRotation = blockSection.getRotationIndex(blockIdx);
            int oldFiller = blockSection.getFiller(blockIdx);

            originalBlocks.put(pos, new OriginalBlockData(oldBlockId, oldRotation, oldFiller));
        }

        blockSection.set(blockIdx, newBlockId, rotation, filler);
    }

    /**
     * Agenda a restauração e remoção dos blocos modificados.
     */
    private static void scheduleCleanup(
            World world,
            Map<Vector3i, OriginalBlockData> originalBlocks,
            int totalSteps,
            long delayBetweenStepsMs,
            long wallDurationMs
    ) {
        long totalAnimTime = totalSteps * delayBetweenStepsMs;

        SCHEDULER.schedule(() -> {
            world.execute(() -> {
                originalBlocks.forEach((pos, data) -> {
                    setBlockInstant(world, pos, data.blockId, data.rotation, data.filler, null);
                });
            });
        }, totalAnimTime + wallDurationMs, TimeUnit.MILLISECONDS);
    }

    /**
     * Procura o bloco sólido mais alto em uma coluna (x, z) a partir de startY até (startY - maxSearchDepth).
     * Utiliza o padrão de consulta de BlockSection direto do ChunkStore.
     */
    public static int getHighestBlockYAt(World world, int x, int startY, int z, int maxSearchDepth) {
        ChunkStore chunkStore = world.getChunkStore();
        if (chunkStore == null) return startY;

        Store<ChunkStore> store = chunkStore.getStore();
        if (store == null) return startY;

        for (int y = startY; y >= startY - maxSearchDepth; y--) {
            int chunkX = ChunkUtil.chunkCoordinate(x);
            int chunkY = ChunkUtil.chunkCoordinate(y);
            int chunkZ = ChunkUtil.chunkCoordinate(z);

            Ref<ChunkStore> sectionRef = chunkStore.getChunkSectionReference(chunkX, chunkY, chunkZ);

            if (sectionRef != null && sectionRef.isValid()) {
                BlockSection blockSection = store.getComponent(sectionRef, BlockSection.getComponentType());
                if (blockSection != null) {
                    int localX = x & ChunkUtil.SIZE_MASK;
                    int localY = y & ChunkUtil.SIZE_MASK;
                    int localZ = z & ChunkUtil.SIZE_MASK;
                    int blockIdx = ChunkUtil.indexBlock(localX, localY, localZ);

                    int blockId = blockSection.get(blockIdx);

                    if (blockId != 0) {
                        return y;
                    }
                }
            }
        }

        return startY;
    }
}