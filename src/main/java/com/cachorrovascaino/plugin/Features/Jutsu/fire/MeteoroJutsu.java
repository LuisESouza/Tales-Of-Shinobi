package com.cachorrovascaino.plugin.Features.Jutsu.fire;

import com.cachorrovascaino.plugin.Abstractions.Jutsu;
import com.cachorrovascaino.plugin.Data.PlayerData;
import com.cachorrovascaino.plugin.Main;
import com.cachorrovascaino.plugin.Utils.ProjectileJutsuUtils;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public class MeteoroJutsu implements Jutsu {

    public static final MeteoroJutsu INSTANCE = new MeteoroJutsu();
    private static final float BASE_DAMAGE = 150.0f;
    private static final float DAMAGE_PER_LEVEL = 25.0f;

    @Override
    public String getId() { return "tengai_shinsei"; }

    @Override
    public String getDisplayName() { return "Tengai Shinsei"; }

    @Override
    public float getChakraCost() { return 100.0f; }

    @Override
    public float getCooldown() { return 30.0f; }

    @Override
    public float getChakraCost(PlayerRef playerRef) { return getChakraCost(); }

    public float getDamageForPlayer(PlayerRef playerRef) {
        PlayerData data = Main.getDataManager().getPlayerData(playerRef.getUuid());
        int level = (data != null) ? data.getJutsuLevel(getId()) : 1;
        return BASE_DAMAGE + ((level - 1) * DAMAGE_PER_LEVEL);
    }

    @Override
    public void execute(PlayerRef playerRef, Ref<EntityStore> playerEntityRef, Store<EntityStore> store, World world) {
        float finalDamage = getDamageForPlayer(playerRef);

        double fallSpeed      = 25.0;
        double targetDistance = 25.0;
        double skyHeight      = 90.0;
        float scale           = 10.0f;

        ProjectileJutsuUtils.spawnVerticalMeteorJutsu(
                playerRef,
                playerEntityRef,
                store,
                world,
                "Jutsu_Fireball_Charge",
                finalDamage,
                fallSpeed,
                targetDistance,
                skyHeight,
                scale,
                "Fire",
                getDisplayName()
        );
    }
}