package com.cachorrovascaino.plugin.Abstractions;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public interface Jutsu {
    String getId();
    String getDisplayName();

    float getChakraCost();

    float getCooldown();

    default float getChakraCost(PlayerRef playerRef) {
        return getChakraCost();
    }

    void execute(PlayerRef playerRef, Ref<EntityStore> playerEntityRef, Store<EntityStore> store, World world);
}