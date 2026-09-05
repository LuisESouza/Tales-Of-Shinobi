package com.cachorrovascaino.plugin.Data.Components;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nullable;

public class WaterWalk implements Component<EntityStore> {
    public static final BuilderCodec<WaterWalk> CODEC = BuilderCodec.builder(WaterWalk.class, WaterWalk::new).build();
    
    public WaterWalk() {}

    @Nullable
    public Component<EntityStore> clone() {
        return new WaterWalk();
    }
}
