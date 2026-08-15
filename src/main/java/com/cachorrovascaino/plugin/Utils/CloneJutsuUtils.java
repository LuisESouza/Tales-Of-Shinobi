package com.cachorrovascaino.plugin.Utils;

import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.modules.entity.EntityModule;
import com.hypixel.hytale.server.core.modules.entity.component.BoundingBox;
import com.hypixel.hytale.server.core.modules.entity.component.HeadRotation;
import com.hypixel.hytale.server.core.modules.entity.component.Interactable;
import com.hypixel.hytale.server.core.modules.entity.component.ModelComponent;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.player.PlayerSkinComponent;
import com.hypixel.hytale.server.core.modules.entity.tracker.NetworkId;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.NPCPlugin;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import org.joml.Vector3d;

import java.awt.Color;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class CloneJutsuUtils {

    private static final ScheduledExecutorService SCHEDULER = Executors.newSingleThreadScheduledExecutor();

    /**
     * Spawna um clone do jogador clonando suas aparências e propriedades.
     *
     * @param playerRef     Ref do jogador invocador
     * @param playerEntityRef Ref da entidade do jogador
     * @param store         Store da entidade
     * @param world         Mundo do Hytale
     * @param offsetX       Offset de spawn no eixo X (ex: 1.5)
     * @param offsetZ       Offset de spawn no eixo Z (ex: 1.5)
     * @param durationSec   Duração do clone em segundos antes de despawnar
     * @param roleName      Role de NPC configurada no servidor (ex: "ShadowClone")
     * @param castMessage   Mensagem opcional exibida ao invocar
     */
    public static Ref<EntityStore> spawnClone(
            PlayerRef playerRef,
            Ref<EntityStore> playerEntityRef,
            Store<EntityStore> store,
            World world,
            double offsetX,
            double offsetZ,
            long durationSec,
            String roleName,
            String castMessage
    ) {
        if (playerRef == null || playerEntityRef == null || !playerEntityRef.isValid()) return null;

        if (castMessage != null && !castMessage.isBlank()) {
            playerRef.sendMessage(Message.raw(castMessage).color(Color.ORANGE));
        }

        TransformComponent transform = store.getComponent(playerEntityRef, TransformComponent.getComponentType());
        if (transform == null) return null;

        Holder<EntityStore> cloneHolder = EntityStore.REGISTRY.newHolder();

        cloneHolder.addComponent(UUIDComponent.getComponentType(), new UUIDComponent(UUID.randomUUID()));

        int nextNetId = world.getEntityStore().takeNextNetworkId();
        cloneHolder.addComponent(NetworkId.getComponentType(), new NetworkId(nextNetId));

        Vector3d spawnPos = new Vector3d(transform.getPosition()).add(offsetX, 0.0, offsetZ);
        TransformComponent cloneTransform = new TransformComponent();
        cloneTransform.getPosition().set(spawnPos);
        cloneTransform.getRotation().set(transform.getRotation());
        cloneHolder.addComponent(TransformComponent.getComponentType(), cloneTransform);

        injectHolderComponent(cloneHolder, EntityModule.get().getHeadRotationComponentType(), new HeadRotation(transform.getRotation()));

        BoundingBox playerBox = store.getComponent(playerEntityRef, EntityModule.get().getBoundingBoxComponentType());
        if (playerBox != null) {
            injectHolderComponent(cloneHolder, EntityModule.get().getBoundingBoxComponentType(), (BoundingBox) playerBox.clone());
        }

        var rawSkinType = EntityModule.get().getPlayerSkinComponentType();
        var rawModelType = EntityModule.get().getModelComponentType();

        PlayerSkinComponent playerSkin = store.getComponent(playerEntityRef, rawSkinType);
        if (playerSkin != null) {
            injectHolderComponent(cloneHolder, rawSkinType, playerSkin.clone());
        }

        ModelComponent playerModel = store.getComponent(playerEntityRef, rawModelType);
        if (playerModel != null) {
            injectHolderComponent(cloneHolder, rawModelType, playerModel.clone());
        }

        Interactable interactable = store.getComponent(playerEntityRef, EntityModule.get().getInteractableComponentType());
        if (interactable != null) {
            injectHolderComponent(cloneHolder, EntityModule.get().getInteractableComponentType(), interactable);
        }

        if (roleName != null && !roleName.isBlank()) {
            int roleIndex = NPCPlugin.get().getIndex(roleName);
            if (roleIndex != Integer.MIN_VALUE) {
                NPCEntity npcEntity = new NPCEntity();
                npcEntity.setRoleName(roleName);
                npcEntity.setRoleIndex(roleIndex);

                injectHolderComponent(cloneHolder, NPCEntity.getComponentType(), npcEntity);
            }
        }

        Ref<EntityStore> cloneRef = world.getEntityStore().getStore().addEntity(cloneHolder, AddReason.SPAWN);

        if (cloneRef != null && cloneRef.isValid() && durationSec > 0) {
            SCHEDULER.schedule(() -> {
                world.execute(() -> {
                    if (cloneRef.isValid()) {
                        world.getEntityStore().getStore().removeEntity(cloneRef, RemoveReason.REMOVE);
                    }
                });
            }, durationSec, TimeUnit.SECONDS);
        }

        return cloneRef;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static <T extends com.hypixel.hytale.component.Component<EntityStore>> void injectHolderComponent(
            Holder<EntityStore> holder,
            Object rawType,
            Object component
    ) {
        com.hypixel.hytale.component.ComponentType type = (com.hypixel.hytale.component.ComponentType) rawType;
        holder.addComponent(type, (T) component);
    }
}