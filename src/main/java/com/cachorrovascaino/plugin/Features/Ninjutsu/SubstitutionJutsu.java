package com.cachorrovascaino.plugin.Features.Ninjutsu;

import com.cachorrovascaino.plugin.Abstractions.Jutsu;
import com.cachorrovascaino.plugin.Abstractions.SkillType;
import com.cachorrovascaino.plugin.Data.Jutsus.JutsuType;
import com.cachorrovascaino.plugin.Data.PlayerData;
import com.cachorrovascaino.plugin.Main;
import com.cachorrovascaino.plugin.Utils.CloneJutsuUtils;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.teleport.Teleport;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class SubstitutionJutsu implements Jutsu {

    public static final SubstitutionJutsu INSTANCE = new SubstitutionJutsu();
    private static final float COST_PER_LEVEL = 4.0f;
    private static final double TELEPORT_DISTANCE = 8.0;

    @Override public String getId() {return "substitution_jutsu";}
    @Override public String getDisplayName() {return "Kawarimi no Jutsu";}
    @Override public float getChakraCost() {return JutsuType.SUBSTITUTION.getResourceCost();}
    @Override public float getCooldown() { return JutsuType.SUBSTITUTION.getCooldown(); }
    @Override public SkillType getType() { return SkillType.NINJUTSU; }

    @Override
    public float getChakraCost(PlayerRef playerRef) {
        PlayerData data = Main.getDataManager().getPlayerData(playerRef.getUuid());
        int level = (data != null) ? data.getJutsuLevel(getId()) : 1;

        float baseCost = getChakraCost();
        return baseCost + ((level - 1) * COST_PER_LEVEL);
    }

    @Override
    public void execute(PlayerRef playerRef, Ref<EntityStore> playerEntityRef, Store<EntityStore> store, World world) {
        if (playerRef == null || !playerEntityRef.isValid()) return;

        CloneJutsuUtils.spawnClone(
                playerRef,
                playerEntityRef,
                store,
                world,
                0.0,
                0.0,
                5,
                "ShadowClone",
                "Kawarimi no Jutsu!"
        );

        TransformComponent currentTransform = store.getComponent(playerEntityRef, TransformComponent.getComponentType());
        if (currentTransform == null) return;

        double targetX = currentTransform.getPosition().x - TELEPORT_DISTANCE;
        double targetZ = currentTransform.getPosition().z - TELEPORT_DISTANCE;

        double finalY = currentTransform.getPosition().y;

        ChunkStore chunkStore = world.getChunkStore();
        if (chunkStore != null) {
            long chunkIndex = ChunkUtil.indexChunkFromBlock((int) targetX, (int) targetZ);
            Ref<ChunkStore> chunkRef = chunkStore.getChunkReference(chunkIndex);

            if (chunkRef != null && chunkRef.isValid()) {
                finalY = currentTransform.getPosition().y;
            }
        }

        Transform targetTransform = new Transform(
                targetX,
                finalY,
                targetZ,
                currentTransform.getRotation().pitch(),
                currentTransform.getRotation().yaw(),
                0.0f
        );

        CompletableFuture.delayedExecutor(2L, TimeUnit.MILLISECONDS, world).execute(() -> {
            if (playerEntityRef.isValid()) {
                Teleport teleport = Teleport.createForPlayer(world, targetTransform);
                playerEntityRef.getStore().addComponent(playerEntityRef, Teleport.getComponentType(), teleport);
            }
        });
    }
}