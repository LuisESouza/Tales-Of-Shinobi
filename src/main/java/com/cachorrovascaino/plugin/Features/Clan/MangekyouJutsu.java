package com.cachorrovascaino.plugin.Features.Clan;

import com.cachorrovascaino.plugin.Abstractions.ClanJutsu;
import com.cachorrovascaino.plugin.Data.Components.MangekyouSharingan;
import com.cachorrovascaino.plugin.Data.Components.Sharingan;
import com.cachorrovascaino.plugin.Data.MangekyouType;
import com.cachorrovascaino.plugin.Data.PlayerData;
import com.cachorrovascaino.plugin.Main;
import com.cachorrovascaino.plugin.Utils.EyesUtils;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.awt.Color;

public class MangekyouJutsu implements ClanJutsu {

    public static final MangekyouJutsu INSTANCE = new MangekyouJutsu();
    private final EyesUtils eyesUtils = new EyesUtils();

    private MangekyouJutsu() {}

    @Override public String getId() { return "mangekyou"; }
    @Override public String getDisplayName() { return "Mangekyō Sharingan"; }
    @Override public float getChakraCost() { return 80.0f; }
    @Override public float getCooldown() { return 10.0f; }

    @Override
    public boolean canExecute(PlayerRef playerRef) {
        PlayerData playerData = Main.getDataManager().getPlayerData(playerRef.getUuid());
        if (playerData == null) return false;

        if (!"Uchiha".equalsIgnoreCase(playerData.getClan())) {
            playerRef.sendMessage(Message.raw("Apenas membros do clã Uchiha podem despertar o Mangekyō!").color(Color.RED));
            return false;
        }

        return true;
    }

    @Override
    public void execute(PlayerRef playerRef, Ref<EntityStore> playerEntityRef, Store<EntityStore> store, World world) {
        if (playerRef == null || !playerEntityRef.isValid() || world == null) return;

        PlayerData playerData = Main.getDataManager().getPlayerData(playerRef.getUuid());
        if (playerData == null) return;

        ComponentType<EntityStore, Sharingan> sharinganType = Main.get().getSharinganComponentType();
        ComponentType<EntityStore, MangekyouSharingan> mangekyouTypeComp = Main.get().getMangekyouSharinganComponentType();

        if (sharinganType == null || mangekyouTypeComp == null) return;

        world.execute(() -> {
            if (!playerEntityRef.isValid()) return;

            if (store.getComponent(playerEntityRef, sharinganType) == null && store.getComponent(playerEntityRef, mangekyouTypeComp) == null) {
                playerRef.sendMessage(Message.raw("Você precisa ativar o Sharingan antes de evoluir para o Mangekyō!").color(Color.RED));
                return;
            }

            MangekyouType mType;
            String rawMangekyouType = playerData.getMangekyouType();

            try {
                if (rawMangekyouType == null || rawMangekyouType.isEmpty()) {
                    throw new IllegalArgumentException("Mangekyou não definido");
                }
                mType = MangekyouType.valueOf(rawMangekyouType.toUpperCase());
            } catch (Exception e) {
                mType = MangekyouType.OBITO;
                playerData.setMangekyouType(mType.name());
            }

            if (store.getComponent(playerEntityRef, mangekyouTypeComp) != null) {
                store.removeComponent(playerEntityRef, mangekyouTypeComp);

                String baseStage = playerData.getEyeDojutsuType();
                eyesUtils.updateHytalePlayerEyes(world, playerRef, baseStage, "");

                playerRef.sendMessage(Message.raw("Mangekyō Sharingan desativado.").color(Color.GRAY));
            } else {
                store.addComponent(playerEntityRef, mangekyouTypeComp, new MangekyouSharingan());

                String targetAsset = mType.getEyeAsset();
                eyesUtils.updateHytalePlayerEyes(world, playerRef, targetAsset, "");

                playerRef.sendMessage(Message.raw("Mangekyō Sharingan (" + mType.name() + ") ATIVADO!").color(Color.RED));
            }

            Main.getDataManager().savePlayer(playerRef.getUuid());
        });
    }
}