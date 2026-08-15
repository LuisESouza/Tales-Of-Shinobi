package com.cachorrovascaino.plugin.Data.Components;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import javax.annotation.Nullable;

public class DeathProcessed implements Component<EntityStore> {
    public static final BuilderCodec<DeathProcessed> CODEC = BuilderCodec.builder(DeathProcessed.class, DeathProcessed::new).build();

    public DeathProcessed() {
    }

    @Nullable
    public Component<EntityStore> clone() {
        return new DeathProcessed();
    }
}