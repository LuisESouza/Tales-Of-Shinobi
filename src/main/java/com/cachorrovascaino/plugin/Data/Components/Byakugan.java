package com.cachorrovascaino.plugin.Data.Components;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nullable;

public class Byakugan implements Component<EntityStore> {
    public static final BuilderCodec<Byakugan> CODEC = BuilderCodec.builder(Byakugan.class, Byakugan::new).build();

    public Byakugan() {}

    @Nullable
    @Override
    public Component<EntityStore> clone() {
        return new Byakugan();
    }
}