package com.cachorrovascaino.plugin.Features.Ninjutsu;

import com.cachorrovascaino.plugin.Abstractions.Jutsu;
import com.cachorrovascaino.plugin.Abstractions.SkillType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.joml.Vector3d;

public class WaterWalkJutsu implements Jutsu {

    public static final WaterWalkJutsu INSTANCE = new WaterWalkJutsu();

    @Override public String getId() { return "suiton_water_walk"; }
    @Override public String getDisplayName() { return "Suiton: Mizu Kinobori"; }
    @Override public float getChakraCost() { return 15.0f; }
    @Override public float getCooldown() { return 1.0f; }
    @Override public SkillType getType() {return SkillType.NINJUTSU;}


    @Override
    public void execute(PlayerRef playerRef, Ref<EntityStore> playerEntityRef, Store<EntityStore> store, World world) {
        if (playerRef == null || !playerEntityRef.isValid()) return;

        TransformComponent transformComp = store.getComponent(playerEntityRef, TransformComponent.getComponentType());
        if (transformComp == null) return;

        Vector3d pos = transformComp.getPosition();

        int blockX = (int) Math.floor(pos.x);
        int blockY = (int) Math.floor(pos.y);
        int blockZ = (int) Math.floor(pos.z);

        BlockType currentBlock = world.getBlockType(blockX, blockY, blockZ);
        BlockType blockBelow = world.getBlockType(blockX, blockY - 1, blockZ);

        boolean isStandingOnWater = isWater(currentBlock) || isWater(blockBelow);

        if (!isStandingOnWater) return;

        double surfaceY = Math.floor(pos.y) + 0.95;

        if (pos.y < surfaceY) {
            Vector3d newPos = new Vector3d(pos.x, surfaceY, pos.z);
            transformComp.setPosition(newPos);
        }
    }

    private boolean isWater(BlockType blockType) {
        if (blockType == null) return false;
        String id = blockType.getId().toLowerCase();
        return id.contains("water") || id.contains("fluid") || id.contains("ocean") || id.contains("river");
    }
}