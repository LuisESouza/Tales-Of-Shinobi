package com.cachorrovascaino.plugin.Features.Jutsu.water;

import com.cachorrovascaino.plugin.Abstractions.Jutsu;
import com.cachorrovascaino.plugin.Data.Jutsus.JutsuType;
import com.cachorrovascaino.plugin.Data.PlayerData;
import com.cachorrovascaino.plugin.Main;
import com.cachorrovascaino.plugin.Utils.ProjectileJutsuUtils;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public class WaterBallJutsu implements Jutsu {

    public static final WaterBallJutsu INSTANCE = new WaterBallJutsu();
    private static final float BASE_DAMAGE = 35.0f;
    private static final float DAMAGE_PER_LEVEL = 7.0f;
    private static final float COST_PER_LEVEL = 4.0f;

    @Override
    public String getId() {
        return "waterball_jutsu";
    }

    @Override
    public String getDisplayName() {
        return "Suiton: Suiryūdan no Jutsu";
    }

    @Override
    public float getChakraCost() {
        return JutsuType.WATERBALL.getChakraCost();
    }

    @Override
    public float getCooldown() {return 0;}

    @Override
    public float getChakraCost(PlayerRef playerRef) {
        PlayerData data = Main.getDataManager().getPlayerData(playerRef.getUuid());
        int level = (data != null) ? data.getJutsuLevel(getId()) : 1;

        float baseCost = getChakraCost();
        return baseCost + ((level - 1) * COST_PER_LEVEL);
    }

    public float getDamageForPlayer(PlayerRef playerRef) {
        PlayerData data = Main.getDataManager().getPlayerData(playerRef.getUuid());
        int level = (data != null) ? data.getJutsuLevel(getId()) : 1;

        return BASE_DAMAGE + ((level - 1) * DAMAGE_PER_LEVEL);
    }

    @Override
    public void execute(PlayerRef playerRef, Ref<EntityStore> playerEntityRef, Store<EntityStore> store, World world) {
        float finalDamage = getDamageForPlayer(playerRef);

        ProjectileJutsuUtils.spawnProjectileJutsuEx(
                playerRef,
                playerEntityRef,
                store,
                world,
                "Water_Ball_Jutsu",
                finalDamage,
                28.0,
                0.0,
                0.0,
                1.5,
                7.0f,
                "Ice",
                getDisplayName()
        );
    }
}