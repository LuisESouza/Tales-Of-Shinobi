package com.cachorrovascaino.plugin.Features.Ninjutsu;

import com.cachorrovascaino.plugin.Abstractions.Jutsu;
import com.cachorrovascaino.plugin.Abstractions.SkillType;
import com.cachorrovascaino.plugin.Data.Components.WaterWalk;
import com.cachorrovascaino.plugin.Data.Jutsus.JutsuType;
import com.cachorrovascaino.plugin.Main;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public class WaterWalkJutsu implements Jutsu {

    public static final WaterWalkJutsu INSTANCE = new WaterWalkJutsu();

    @Override public String getId() { return "water_walk"; }
    @Override public String getDisplayName() { return "Water Walk"; }
    @Override public float getChakraCost() { return JutsuType.SUBSTITUTION.getResourceCost(); }
    @Override public float getCooldown() { return JutsuType.SUBSTITUTION.getCooldown(); }
    @Override public SkillType getType() { return SkillType.NINJUTSU; }

    @Override
    public void execute(PlayerRef playerRef, Ref<EntityStore> playerEntityRef, Store<EntityStore> store, World world) {
        if (playerRef == null || !playerEntityRef.isValid() || world == null) return;

        ComponentType<EntityStore, WaterWalk> waterWalkType = Main.get().getWaterWalkComponentType();
        if (waterWalkType == null) return;

        world.execute(() -> {
            if (!playerEntityRef.isValid()) return;

            if (store.getComponent(playerEntityRef, waterWalkType) != null) {
                store.removeComponent(playerEntityRef, waterWalkType);
            } else {
                store.addComponent(playerEntityRef, waterWalkType, new WaterWalk());
            }
        });
    }
}