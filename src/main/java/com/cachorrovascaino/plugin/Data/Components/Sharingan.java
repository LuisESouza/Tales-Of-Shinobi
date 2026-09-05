package com.cachorrovascaino.plugin.Data.Components;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nullable;

public class Sharingan implements Component<EntityStore> {
    public static final BuilderCodec<Sharingan> CODEC = BuilderCodec.builder(Sharingan.class, Sharingan::new).build();

    public Sharingan() {}

    @Nullable
    @Override
    public Component<EntityStore> clone() {return new Sharingan();}
}