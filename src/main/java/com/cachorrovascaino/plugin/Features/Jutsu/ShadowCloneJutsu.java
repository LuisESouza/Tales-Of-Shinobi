package com.cachorrovascaino.plugin.Features.Jutsu;

import com.cachorrovascaino.plugin.Abstractions.Jutsu;
import com.cachorrovascaino.plugin.Data.Jutsus.JutsuType;
import com.cachorrovascaino.plugin.Data.PlayerData;
import com.cachorrovascaino.plugin.Main;
import com.cachorrovascaino.plugin.Utils.CloneJutsuUtils;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public class ShadowCloneJutsu implements Jutsu {

    public static final ShadowCloneJutsu INSTANCE = new ShadowCloneJutsu();

    @Override
    public String getId() {
        return "shadow_clone";
    }

    @Override
    public String getDisplayName() {
        return "Kage Bunshin no Jutsu";
    }

    @Override
    public float getChakraCost() {
        return JutsuType.SHADOW_CLONE.getChakraCost();
    }

    @Override
    public float getCooldown() {return JutsuType.SHADOW_CLONE.getCooldown();}

    @Override
    public float getChakraCost(PlayerRef playerRef) {
        PlayerData data = Main.getDataManager().getPlayerData(playerRef.getUuid());
        int level = (data != null) ? data.getJutsuLevel(getId()) : 1;

        return getChakraCost() + ((level - 1) * 5.0f);
    }

    @Override
    public void execute(PlayerRef playerRef, Ref<EntityStore> playerEntityRef, Store<EntityStore> store, World world) {
        CloneJutsuUtils.spawnClone(
                playerRef,
                playerEntityRef,
                store,
                world,
                1.5,
                1.5,
                20,
                "ShadowClone",
                "Kage Bunshin no Jutsu!"
        );
    }
}