package com.cachorrovascaino.plugin.Features.Clan.Abilities;

import com.cachorrovascaino.plugin.Abstractions.ClanJutsu;
import com.cachorrovascaino.plugin.Data.Components.MangekyouSharingan;
import com.cachorrovascaino.plugin.Data.PlayerData;
import com.cachorrovascaino.plugin.Main;
import com.hypixel.hytale.component.Archetype;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.component.Intangible;
import com.hypixel.hytale.server.core.modules.entity.component.Invulnerable;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.awt.Color;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class KamuiIntangibilityJutsu implements ClanJutsu {

    public static final KamuiIntangibilityJutsu INSTANCE = new KamuiIntangibilityJutsu();
    private static final ScheduledExecutorService SCHEDULER = Executors.newSingleThreadScheduledExecutor();

    private KamuiIntangibilityJutsu() {}

    @Override public String getId() { return "kamui_intangibility"; }
    @Override public String getDisplayName() { return "Kamui: Intangibility"; }
    @Override public float getChakraCost() { return 50.0f; }
    @Override public float getCooldown() { return 20.0f; }

    @Override
    public boolean canExecute(PlayerRef playerRef) {
        PlayerData playerData = Main.getDataManager().getPlayerData(playerRef.getUuid());
        if (playerData == null) return false;

        if (!"OBITO".equalsIgnoreCase(playerData.getMangekyouType())) {
            playerRef.sendMessage(Message.raw("This Mangekyō Sharingan does not possess the Kamui technique!").color(Color.RED));
            return false;
        }

        return true;
    }

    @Override
    public void execute(PlayerRef playerRef, Ref<EntityStore> playerEntityRef, Store<EntityStore> store, World world) {
        if (playerEntityRef == null || !playerEntityRef.isValid() || world == null) return;
        if (!canExecute(playerRef)) return;

        ComponentType<EntityStore, MangekyouSharingan> mangekyouType = Main.get().getMangekyouSharinganComponentType();

        world.execute(() -> {
            if (!playerEntityRef.isValid()) return;

            if (mangekyouType == null || store.getComponent(playerEntityRef, mangekyouType) == null) {
                playerRef.sendMessage(Message.raw("Activate Mangekyō Sharingan to use Kamui!").color(Color.RED));
                return;
            }

            store.ensureComponent(playerEntityRef, Intangible.getComponentType());
            store.ensureComponent(playerEntityRef, Invulnerable.getComponentType());

            Player playerComponent = store.getComponent(playerEntityRef, Player.getComponentType());
            if (playerComponent != null) {
                playerComponent.setFlying(playerEntityRef, true, store);
                playerComponent.setNoClip(playerEntityRef, true, false, store);
            }

            playerRef.sendMessage(Message.raw("Kamui: Your body passes through attacks and matter!").color(Color.MAGENTA));
        });

        SCHEDULER.schedule(() -> {
            world.execute(() -> {
                if (!playerEntityRef.isValid()) return;

                Archetype<EntityStore> archetype = store.getArchetype(playerEntityRef);

                if (archetype.contains(Intangible.getComponentType())) {
                    store.removeComponent(playerEntityRef, Intangible.getComponentType());
                }
                if (archetype.contains(Invulnerable.getComponentType())) {
                    store.removeComponent(playerEntityRef, Invulnerable.getComponentType());
                }

                Player playerComponent = store.getComponent(playerEntityRef, Player.getComponentType());
                if (playerComponent != null) {
                    if (playerComponent.isNoClip()) {
                        playerComponent.setNoClip(playerEntityRef, false, false, store);
                    }
                    playerComponent.setFlying(playerEntityRef, false, store);
                }

                playerRef.sendMessage(Message.raw("Kamui: Intangibility deactivated.").color(Color.GRAY));
            });
        }, 15, TimeUnit.SECONDS);
    }
}