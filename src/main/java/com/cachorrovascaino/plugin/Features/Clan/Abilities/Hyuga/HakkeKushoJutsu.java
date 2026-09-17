package com.cachorrovascaino.plugin.Features.Clan.Abilities.Hyuga;

import com.cachorrovascaino.plugin.Abstractions.ClanJutsu;
import com.cachorrovascaino.plugin.Data.Components.HakkeKushoComponent;
import com.cachorrovascaino.plugin.Data.PlayerData;
import com.cachorrovascaino.plugin.Main;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.AnimationSlot;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.AnimationUtils;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.awt.Color;

public class HakkeKushoJutsu implements ClanJutsu {

    public static final HakkeKushoJutsu INSTANCE = new HakkeKushoJutsu();

    private static final float CHAKRA_COST = 30.0f;
    private static final float COOLDOWN = 20.0f;
    private static final long DURATION_MS = 60_000L;
    private static final int MAX_CHARGES = 3;

    private HakkeKushoJutsu() {}

    @Override public String getId() { return "hakke_kusho"; }
    @Override public String getDisplayName() { return "Eight Trigrams Vacuum Palm"; }
    @Override public float getChakraCost() { return CHAKRA_COST; }
    @Override public float getCooldown() { return COOLDOWN; }

    @Override
    public boolean canExecute(PlayerRef playerRef) {
        PlayerData playerData = Main.getDataManager().getPlayerData(playerRef.getUuid());
        if (playerData == null) return false;

        if (!"HYUGA".equalsIgnoreCase(playerData.getClan())) {
            playerRef.sendMessage(Message.raw("Only members of the Hyūga Clan can perform Vacuum Palm.").color(Color.RED));
            return false;
        }

        if (playerData.getCurrentChakra() < CHAKRA_COST) {
            playerRef.sendMessage(Message.raw("Insufficient chakra to activate Vacuum Palm!").color(Color.RED));
            return false;
        }

        return true;
    }

    @Override
    public void execute(PlayerRef playerRef, Ref<EntityStore> playerEntityRef, Store<EntityStore> store, World world) {
        if (playerEntityRef == null || !playerEntityRef.isValid() || world == null) return;
        if (!canExecute(playerRef)) return;

        PlayerData playerData = Main.getDataManager().getPlayerData(playerRef.getUuid());
        if (playerData == null) return;

        world.execute(() -> {
            if (!playerEntityRef.isValid()) return;

            playerData.setCurrentChakra(playerData.getCurrentChakra() - CHAKRA_COST);
            Main.getJutsuManager().updateChakraHud(playerRef);

            AnimationUtils.playAnimation(playerEntityRef, AnimationSlot.Action, null, "Punch", false, store);

            ComponentType<EntityStore, HakkeKushoComponent> kushoType = Main.get().getHakkeKushoComponentType();
            if (kushoType != null) {
                store.addComponent(playerEntityRef, kushoType, new HakkeKushoComponent(DURATION_MS, MAX_CHARGES));
            }

            playerRef.sendMessage(Message.raw("[Vacuum Palm] Stance activated! Your attacks will emit air palm blasts.").color(Color.CYAN));
        });
    }
}