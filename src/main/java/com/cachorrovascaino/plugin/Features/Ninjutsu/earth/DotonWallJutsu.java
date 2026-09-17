package com.cachorrovascaino.plugin.Features.Ninjutsu.earth;

import com.cachorrovascaino.plugin.Abstractions.Jutsu;
import com.cachorrovascaino.plugin.Abstractions.SkillType;
import com.cachorrovascaino.plugin.Utils.BlockJutsuUtils;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.protocol.SoundCategory;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.SoundUtil;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.section.BlockSection;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.joml.Vector3d;
import org.joml.Vector3i;

import java.util.*;

public class DotonWallJutsu implements Jutsu {

    public static final DotonWallJutsu INSTANCE = new DotonWallJutsu();

    private static final int WALL_WIDTH = 6;
    private static final int WALL_HEIGHT = 6;
    private static final double DISTANCE = 3.0;

    @Override public String getId() { return "doton_wall"; }
    @Override public String getDisplayName() { return "Doton: Doryūheki"; }
    @Override public float getChakraCost() { return 20.0f; }
    @Override public float getCooldown() { return 10.0f; }
    @Override public SkillType getType() { return SkillType.NINJUTSU; }

    @Override
    public void execute(PlayerRef playerRef, Ref<EntityStore> playerEntityRef, Store<EntityStore> store, World world) {
        if (playerRef == null || !playerEntityRef.isValid()) return;

        SoundUtil.playSoundEvent2dToPlayer(playerRef, "SFX_Jutsu_Sound", SoundCategory.SFX);

        TransformComponent transformComp = store.getComponent(playerEntityRef, TransformComponent.getComponentType());
        if (transformComp == null) return;

        Vector3d lookVector = BlockJutsuUtils.getHorizontalLookVector(playerEntityRef, store, transformComp);
        Vector3d rightVector = BlockJutsuUtils.getRightVector(lookVector);

        Vector3d pos = transformComp.getPosition();
        double centerX = pos.x + (lookVector.x * DISTANCE);
        double centerZ = pos.z + (lookVector.z * DISTANCE);
        int baseY = (int) Math.floor(pos.y);

        double halfWidth = WALL_WIDTH / 2.0;
        int startX = (int) Math.floor(centerX - (rightVector.x * halfWidth));
        int startZ = (int) Math.floor(centerZ - (rightVector.z * halfWidth));
        int endX = (int) Math.floor(centerX + (rightVector.x * halfWidth));
        int endZ = (int) Math.floor(centerZ + (rightVector.z * halfWidth));

        Set<Vector3i> baseLine = BlockJutsuUtils.getLine2D(startX, startZ, endX, endZ, baseY);

        Map<Vector3i, String> groundBlockMap = new HashMap<>();

        for (Vector3i basePos : baseLine) {
            String blockKey = getSolidGroundBlockKey(world, basePos.x, basePos.y, basePos.z);
            groundBlockMap.put(basePos, blockKey);
        }

        Map<Integer, Set<Vector3i>> layers = new HashMap<>();
        for (int h = 0; h < WALL_HEIGHT; h++) {
            Set<Vector3i> layerBlocks = new HashSet<>();
            for (Vector3i base : baseLine) {
                layerBlocks.add(new Vector3i(base.x, base.y + h, base.z));
            }
            layers.put(h, layerBlocks);
        }

        BlockJutsuUtils.spawnAnimatedDynamicStructure(
                world,
                layers,
                groundBlockMap,
                120L,
                6000L
        );
    }

    /**
     * Busca o ID/Key do bloco sólido do chão navegando verticalmente até 3 blocos para baixo.
     */
    private String getSolidGroundBlockKey(World world, int x, int startY, int z) {
        ChunkStore chunkStore = world.getChunkStore();
        if (chunkStore == null) return "Rock_Stone";

        for (int yOffset = 0; yOffset >= -3; yOffset--) {
            int currentY = startY + yOffset;

            int chunkX = ChunkUtil.chunkCoordinate(x);
            int chunkY = ChunkUtil.chunkCoordinate(currentY);
            int chunkZ = ChunkUtil.chunkCoordinate(z);

            Ref<ChunkStore> sectionRef = chunkStore.getChunkSectionReference(chunkX, chunkY, chunkZ);
            if (sectionRef == null || !sectionRef.isValid()) continue;

            BlockSection blockSection = chunkStore.getStore().getComponent(sectionRef, BlockSection.getComponentType());
            if (blockSection == null) continue;

            int localX = x & ChunkUtil.SIZE_MASK;
            int localY = currentY & ChunkUtil.SIZE_MASK;
            int localZ = z & ChunkUtil.SIZE_MASK;
            int blockIdx = ChunkUtil.indexBlock(localX, localY, localZ);

            int blockId = blockSection.get(blockIdx);

            if (blockId != 0) {
                BlockType blockType = BlockType.getAssetMap().getAsset(blockId);
                if (blockType != null && blockType.getId() != null && !blockType.getId().equalsIgnoreCase("Empty")) {
                    return blockType.getId();
                }
            }
        }

        return "Rock_Stone";
    }
}