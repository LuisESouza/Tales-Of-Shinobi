package com.cachorrovascaino.plugin.Data.Components;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nullable;

public class MangekyouSharingan implements Component<EntityStore> {
    public static final BuilderCodec<MangekyouSharingan> CODEC = BuilderCodec.builder(MangekyouSharingan.class, MangekyouSharingan::new).build();

    public MangekyouSharingan() {}

    @Nullable
    @Override
    public Component<EntityStore> clone() {return new MangekyouSharingan();}
}