package com.cachorrovascaino.plugin.Features.Ninjutsu.wind;

import com.cachorrovascaino.plugin.Abstractions.Jutsu;
import com.cachorrovascaino.plugin.Abstractions.SkillType;
import com.cachorrovascaino.plugin.Utils.ProjectileJutsuUtils;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.SoundCategory;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.SoundUtil;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public class WindTornadoJutsu implements Jutsu {

    public static final WindTornadoJutsu INSTANCE = new WindTornadoJutsu();

    private static final String PROJECTILE_PARTICLE_ID = "Jutsu_Futon_Charge";

    @Override public String getId() { return "futon_tornado_jutsu"; }
    @Override public String getDisplayName() { return "Fūton: Tatsumaki no Jutsu"; }
    @Override public float getChakraCost() { return 95.0f; }
    @Override public float getCooldown() { return 30.0f; }
    @Override public SkillType getType() { return SkillType.NINJUTSU; }

    @Override
    public void execute(PlayerRef playerRef, Ref<EntityStore> playerEntityRef, Store<EntityStore> store, World world) {
        if (playerRef == null || playerEntityRef == null || !playerEntityRef.isValid()) return;

        SoundUtil.playSoundEvent2dToPlayer(playerRef, "SFX_Jutsu_Sound", SoundCategory.SFX);

        ProjectileJutsuUtils.spawnWalkingTornadoJutsu(
                playerRef,
                playerEntityRef,
                store,
                world,
                PROJECTILE_PARTICLE_ID,
                120.0f,
                12.0,
                2.0,
                2.0f,
                100,
                2.5,
                "Wind",
                "Fūton: Tatsumaki no Jutsu!",
                null
        );
    }
}