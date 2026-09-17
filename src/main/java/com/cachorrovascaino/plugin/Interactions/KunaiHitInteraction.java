package com.cachorrovascaino.plugin.Interactions;

import com.cachorrovascaino.plugin.Data.PlayerData;
import com.cachorrovascaino.plugin.Main;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.BlockPosition;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInteraction;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.ParticleUtil;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.joml.Vector3d;
import org.joml.Vector4d;

import javax.annotation.Nonnull;

public class KunaiHitInteraction extends SimpleInteraction {
    public static final String HIRAISHIN_PARTICLE = "Hiraishin_Particle";

    public static final BuilderCodec<KunaiHitInteraction> CODEC =
            BuilderCodec.builder(KunaiHitInteraction.class, KunaiHitInteraction::new, SimpleInteraction.CODEC)
                    .build();

    @Override
    protected void tick0(boolean firstRun, float time, @Nonnull InteractionType type,
                         @Nonnull InteractionContext context, @Nonnull CooldownHandler cooldownHandler) {
        if (!firstRun) return;

        Ref<EntityStore> sourceEntity = context.getOwningEntity();
        if (sourceEntity == null || !sourceEntity.isValid()) return;

        Store<EntityStore> store = sourceEntity.getStore();
        PlayerRef playerRef = store.getComponent(sourceEntity, PlayerRef.getComponentType());
        if (playerRef == null) return;

        Vector3d impactPos = null;

        if (context.getMetaStore() != null) {
            Object hitObj = context.getMetaStore().getIfPresentMetaObject(Interaction.HIT_LOCATION);
            if (hitObj instanceof Vector4d hitLoc) {
                impactPos = new Vector3d(hitLoc.x, hitLoc.y, hitLoc.z);
            } else if (hitObj instanceof Vector3d hitLoc3) {
                impactPos = new Vector3d(hitLoc3);
            }
        }

        if (impactPos == null) {
            Ref<EntityStore> targetEntity = context.getTargetEntity();
            if (targetEntity != null && targetEntity.isValid()) {
                TransformComponent transform = store.getComponent(targetEntity, TransformComponent.getComponentType());
                if (transform != null) {
                    impactPos = new Vector3d(transform.getPosition());
                }
            }
        }

        if (impactPos == null) {
            BlockPosition targetBlock = context.getTargetBlock();
            if (targetBlock != null) {
                impactPos = new Vector3d(
                        targetBlock.x + 0.5,
                        targetBlock.y + 1.0,
                        targetBlock.z + 0.5
                );
            }
        }

        if (impactPos != null) {
            PlayerData data = Main.getDataManager().getPlayerData(playerRef.getUuid());
            if (data != null) {
                data.addHiraishinMark(impactPos);
                ParticleUtil.spawnParticleEffect(HIRAISHIN_PARTICLE, impactPos, store);
            }
        }
    }
}