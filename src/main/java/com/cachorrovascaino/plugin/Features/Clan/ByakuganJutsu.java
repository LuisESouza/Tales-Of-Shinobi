package com.cachorrovascaino.plugin.Features.Clan;

import com.cachorrovascaino.plugin.Abstractions.ClanJutsu;
import com.cachorrovascaino.plugin.Data.Components.Byakugan;
import com.cachorrovascaino.plugin.Data.PlayerData;
import com.cachorrovascaino.plugin.Main;
import com.cachorrovascaino.plugin.Utils.ChakraUtils;
import com.cachorrovascaino.plugin.Utils.EyesUtils;
import com.cachorrovascaino.plugin.Utils.TargetUtils;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.NameplateUpdate;
import com.hypixel.hytale.protocol.SoundCategory;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.tracker.EntityTrackerSystems;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.SoundUtil;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.joml.Vector3d;

import java.awt.Color;
import java.util.List;

public class ByakuganJutsu implements ClanJutsu {

    public static final ByakuganJutsu INSTANCE = new ByakuganJutsu();
    private final EyesUtils eyesUtils = new EyesUtils();

    private ByakuganJutsu() {}

    @Override public String getId() { return "byakugan"; }
    @Override public String getDisplayName() { return "Byakugan"; }
    @Override public float getChakraCost() { return 50.0f; }
    @Override public float getCooldown() { return 5.0f; }

    @Override
    public boolean canExecute(PlayerRef playerRef) {
        PlayerData playerData = Main.getDataManager().getPlayerData(playerRef.getUuid());
        if (playerData == null) return false;

        if (!"Hyuga".equalsIgnoreCase(playerData.getClan())) {
            playerRef.sendMessage(Message.raw("Apenas membros do clã Hyūga possuem o Byakugan.").color(Color.RED));
            return false;
        }
        return true;
    }

    @Override
    public void execute(PlayerRef playerRef, Ref<EntityStore> playerEntityRef, Store<EntityStore> store, World world) {
        if (playerRef == null || !playerEntityRef.isValid() || world == null) return;

        SoundUtil.playSoundEvent2dToPlayer(playerRef, "SFX_Byakugan", SoundCategory.SFX);

        PlayerData playerData = Main.getDataManager().getPlayerData(playerRef.getUuid());
        if (playerData == null) return;

        ComponentType<EntityStore, Byakugan> byakuganType = Main.get().getByakuganComponentType();
        if (byakuganType == null) return;

        world.execute(() -> {
            if (!playerEntityRef.isValid()) return;

            if (store.getComponent(playerEntityRef, byakuganType) != null) {
                store.removeComponent(playerEntityRef, byakuganType);
                clearByakuganChakraScan(playerEntityRef, store);
                String originalEyes = playerData.getOriginalEyesId();
                String originalColor = playerData.getEyesColor();

                if (originalEyes != null && !originalEyes.isEmpty()) {
                    eyesUtils.updateHytalePlayerEyes(world, playerRef, originalEyes, originalColor);
                }
                playerRef.sendMessage(Message.raw("Byakugan desativado.").color(Color.GRAY));
            } else {
                store.addComponent(playerEntityRef, byakuganType, new Byakugan());
                eyesUtils.captureOriginalEyesIfNeeded(playerRef, playerData);
                eyesUtils.updateHytalePlayerEyes(world, playerRef, "Byakugan_HD", playerData.getEyesColor());
                triggerByakuganChakraScan(playerEntityRef, store);
                playerRef.sendMessage(Message.raw("Byakugan ativado! Visão de chakra e tenketsus revelada.").color(Color.WHITE));
            }

            Main.getDataManager().savePlayer(playerRef.getUuid());
        });
    }

    public void triggerByakuganChakraScan(Ref<EntityStore> hyugaEntityRef, Store<EntityStore> store) {
        TransformComponent transform = store.getComponent(hyugaEntityRef, TransformComponent.getComponentType());
        if (transform == null) return;

        List<Ref<EntityStore>> nearbyEntities = TargetUtils.getEntitiesInRadius(new Vector3d(transform.getPosition()), 35.0, store);

        for (Ref<EntityStore> targetRef : nearbyEntities) {
            if (targetRef.equals(hyugaEntityRef)) continue;

            EntityTrackerSystems.Visible visibleComponent = store.getComponent(
                    targetRef,
                    EntityTrackerSystems.Visible.getComponentType()
            );
            if (visibleComponent == null) continue;

            EntityTrackerSystems.EntityViewer hyugaViewer = visibleComponent.visibleTo.get(hyugaEntityRef);

            if (hyugaViewer != null) {
                PlayerRef targetPlayerRef = store.getComponent(targetRef, PlayerRef.getComponentType());
                if (targetPlayerRef != null) {
                    float currentChakra = ChakraUtils.getCurrentChakra(targetPlayerRef);
                    float maxChakra = ChakraUtils.getMaxChakra(targetPlayerRef);

                    String formattedText = String.format("%s [%.0f/%.0f CHAKRA]", targetPlayerRef.getUsername(), currentChakra, maxChakra);

                    NameplateUpdate nameplateUpdate = new NameplateUpdate(formattedText);
                    hyugaViewer.queueUpdate(targetRef, nameplateUpdate);
                }
            }
        }
    }

    public void clearByakuganChakraScan(Ref<EntityStore> hyugaEntityRef, Store<EntityStore> store) {
        TransformComponent transform = store.getComponent(hyugaEntityRef, TransformComponent.getComponentType());
        if (transform == null) return;

        List<Ref<EntityStore>> nearbyEntities = TargetUtils.getEntitiesInRadius(new Vector3d(transform.getPosition()), 50.0, store);

        for (Ref<EntityStore> targetRef : nearbyEntities) {
            if (targetRef.equals(hyugaEntityRef)) continue;

            EntityTrackerSystems.Visible visibleComponent = store.getComponent(
                    targetRef,
                    EntityTrackerSystems.Visible.getComponentType()
            );
            if (visibleComponent == null) continue;

            EntityTrackerSystems.EntityViewer hyugaViewer = visibleComponent.visibleTo.get(hyugaEntityRef);

            if (hyugaViewer != null) {
                PlayerRef targetPlayerRef = store.getComponent(targetRef, PlayerRef.getComponentType());
                if (targetPlayerRef != null) {
                    NameplateUpdate resetUpdate = new NameplateUpdate(targetPlayerRef.getUsername());
                    hyugaViewer.queueUpdate(targetRef, resetUpdate);
                }
            }
        }
    }
}