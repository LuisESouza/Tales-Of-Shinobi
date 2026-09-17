package com.cachorrovascaino.plugin.Systems;

import com.cachorrovascaino.plugin.Data.PlayerData;
import com.cachorrovascaino.plugin.Interactions.KunaiHitInteraction;
import com.cachorrovascaino.plugin.Main;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.world.ParticleUtil;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.joml.Vector3d;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.UUID;

public class HiraishinParticleSystem extends EntityTickingSystem<EntityStore> {

    private float timer = 0.0f;
    private static final float INTERVAL = 0.4f;

    @Override
    public void tick(float dt, int index, @Nonnull ArchetypeChunk<EntityStore> chunk, @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer) {
        this.timer += dt;
        if (this.timer < INTERVAL) return;
        this.timer = 0.0f;

        Ref<EntityStore> ref = chunk.getReferenceTo(index);
        if (!ref.isValid()) return;

        UUIDComponent uuidComponent = store.getComponent(ref, UUIDComponent.getComponentType());
        if (uuidComponent == null) return;

        UUID playerUuid = uuidComponent.getUuid();
        PlayerData data = Main.getDataManager().getPlayerData(playerUuid);
        if (data == null) return;

        List<Vector3d> marks = data.getHiraishinMarks();
        if (marks.isEmpty()) return;

        for (Vector3d markPos : marks) {
            ParticleUtil.spawnParticleEffect(
                    KunaiHitInteraction.HIRAISHIN_PARTICLE,
                    markPos,
                    store
            );
        }
    }

    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        return Query.and(
                Player.getComponentType(),
                UUIDComponent.getComponentType()
        );
    }
}