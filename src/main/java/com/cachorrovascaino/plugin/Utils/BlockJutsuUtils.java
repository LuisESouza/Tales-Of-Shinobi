package com.cachorrovascaino.plugin.Utils;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Rotation3f;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.modules.entity.component.HeadRotation;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.physics.util.PhysicsMath;
import com.hypixel.hytale.server.core.universe.world.World;
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
        Map<Vector3i, String> originalBlocks = new ConcurrentHashMap<>();

        layersByStep.forEach((step, blockPositions) -> {
            long delay = step * delayBetweenStepsMs;

            SCHEDULER.schedule(() -> {
                world.execute(() -> {
                    for (Vector3i pos : blockPositions) {
                        BlockType currentType = world.getBlockType(pos.x, pos.y, pos.z);
                        String oldKey = (currentType != null) ? currentType.getId() : "Empty";

                        originalBlocks.putIfAbsent(pos, oldKey);

                        world.setBlock(pos.x, pos.y, pos.z, blockKey);
                    }
                });
            }, delay, TimeUnit.MILLISECONDS);
        });

        scheduleCleanup(world, originalBlocks, layersByStep.size(), delayBetweenStepsMs, wallDurationMs);
    }

    /**
     * Constrói uma estrutura animada onde CADA COLUNA herda dinamicamente o bloco do chão original.
     *
     * @param customBlockPerColumn Mapa com a posição base do chão (Vector3i) -> ID do bloco a ser propagado nessa coluna.
     */
    public static void spawnAnimatedDynamicStructure(
            World world,
            Map<Integer, Set<Vector3i>> layersByStep,
            Map<Vector3i, String> customBlockPerColumn,
            long delayBetweenStepsMs,
            long wallDurationMs
    ) {
        Map<Vector3i, String> originalBlocks = new ConcurrentHashMap<>();

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

                        BlockType currentType = world.getBlockType(pos.x, pos.y, pos.z);
                        String oldKey = (currentType != null) ? currentType.getId() : "Empty";

                        originalBlocks.putIfAbsent(pos, oldKey);

                        world.setBlock(pos.x, pos.y, pos.z, dynamicBlockKey);
                    }
                });
            }, delay, TimeUnit.MILLISECONDS);
        });

        scheduleCleanup(world, originalBlocks, layersByStep.size(), delayBetweenStepsMs, wallDurationMs);
    }

    /**
     * Método interno auxiliar para agendar a remoção/restauração dos blocos.
     */
    private static void scheduleCleanup(
            World world,
            Map<Vector3i, String> originalBlocks,
            int totalSteps,
            long delayBetweenStepsMs,
            long wallDurationMs
    ) {
        long totalAnimTime = totalSteps * delayBetweenStepsMs;

        SCHEDULER.schedule(() -> {
            world.execute(() -> {
                originalBlocks.forEach((pos, oldKey) -> {
                    if (oldKey == null || oldKey.equalsIgnoreCase("Empty")) {
                        world.setBlock(pos.x, pos.y, pos.z, "Empty");
                    } else {
                        world.setBlock(pos.x, pos.y, pos.z, oldKey);
                    }
                });
            });
        }, totalAnimTime + wallDurationMs, TimeUnit.MILLISECONDS);
    }
}