package com.cachorrovascaino.plugin.Systems;

import com.cachorrovascaino.plugin.Manager.JutsuManager;
import com.cachorrovascaino.plugin.Ui.Hud.JutsuEquippedHud;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.DelayedEntitySystem;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;

public class ComboTickSystem extends DelayedEntitySystem<EntityStore> {

    private final JutsuManager jutsuManager;

    public ComboTickSystem(JutsuManager jutsuManager) {
        super(0.1f);
        this.jutsuManager = jutsuManager;
    }

    @Override
    public void tick(float dt, int index, @Nonnull ArchetypeChunk<EntityStore> archetypeChunk,
                     @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer) {

        Ref<EntityStore> ref = archetypeChunk.getReferenceTo(index);
        PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
        if (playerRef != null) {
            jutsuManager.ensureChakraHudLoaded(playerRef);
            jutsuManager.tickPlayerCombo(playerRef);

            Player player = store.getComponent(ref, Player.getComponentType());
            if (player != null) {
                JutsuEquippedHud.show(player, playerRef);
            }
        }
    }

    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        return Query.and(Player.getComponentType());
    }
}