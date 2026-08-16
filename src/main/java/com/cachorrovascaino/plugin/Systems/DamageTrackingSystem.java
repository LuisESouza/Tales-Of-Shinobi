package com.cachorrovascaino.plugin.Systems;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.dependency.Dependency;
import com.hypixel.hytale.component.dependency.RootDependency;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageEventSystem;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.meta.MetaKey;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nonnull;

public class DamageTrackingSystem extends DamageEventSystem {

    private final Map<UUID, UUID> lastAttackers;
    public static final MetaKey<Boolean> RPG_DAMAGE_PROCESSED = Damage.META_REGISTRY.registerMetaObject(data -> false);

    public DamageTrackingSystem(Map<UUID, UUID> lastAttackers) {
        this.lastAttackers = lastAttackers;
    }

    @Override
    public void handle(int index, @Nonnull ArchetypeChunk<EntityStore> chunk, @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer, @Nonnull Damage damage) {
        Ref<EntityStore> targetRef = chunk.getReferenceTo(index);

        if (targetRef == null || !targetRef.isValid() || damage.getMetaObject(RPG_DAMAGE_PROCESSED)) return;

        Ref<EntityStore> attackerRef = null;
        if (damage.getSource() instanceof Damage.EntitySource src) {
            attackerRef = src.getRef();
        } else if (damage.getSource() instanceof Damage.ProjectileSource src) {
            attackerRef = src.getRef();
        }

        if (attackerRef == null || !attackerRef.isValid()) return;

        UUIDComponent attackerUuidComp = (UUIDComponent) store.getComponent(attackerRef, UUIDComponent.getComponentType());
        UUIDComponent targetUuidComp = (UUIDComponent) store.getComponent(targetRef, UUIDComponent.getComponentType());

        if (attackerUuidComp == null || targetUuidComp == null) return;

        UUID attackerUUID = attackerUuidComp.getUuid();
        UUID targetUUID = targetUuidComp.getUuid();

        this.lastAttackers.put(targetUUID, attackerUUID);
    }

    @Override
    public Set<Dependency<EntityStore>> getDependencies() {
        return Collections.singleton(RootDependency.first());
    }

    @Override
    public Query<EntityStore> getQuery() {
        return Query.any();
    }
}