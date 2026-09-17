package com.cachorrovascaino.plugin.Data.Components;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nullable;

public class HakkeKushoComponent implements Component<EntityStore> {

    public static final BuilderCodec<HakkeKushoComponent> CODEC = BuilderCodec.builder(HakkeKushoComponent.class, HakkeKushoComponent::new).build();

    private long expireTimeMs;
    private int charges;

    public HakkeKushoComponent() {
        this.expireTimeMs = 0;
        this.charges = 0;
    }

    public HakkeKushoComponent(long durationMs, int charges) {
        this.expireTimeMs = System.currentTimeMillis() + durationMs;
        this.charges = charges;
    }

    public boolean isExpired() {
        return System.currentTimeMillis() > expireTimeMs || charges <= 0;
    }

    public void consumeCharge() {
        this.charges--;
    }

    public int getCharges() {
        return charges;
    }

    @Nullable
    @Override
    public Component<EntityStore> clone() {
        HakkeKushoComponent cloned = new HakkeKushoComponent();
        cloned.expireTimeMs = this.expireTimeMs;
        cloned.charges = this.charges;
        return cloned;
    }
}