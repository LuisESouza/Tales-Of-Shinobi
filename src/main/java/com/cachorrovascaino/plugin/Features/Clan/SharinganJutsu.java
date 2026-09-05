package com.cachorrovascaino.plugin.Features.Clan;

import com.cachorrovascaino.plugin.Abstractions.ClanJutsu;
import com.cachorrovascaino.plugin.Data.Components.MangekyouSharingan;
import com.cachorrovascaino.plugin.Data.Components.Sharingan;
import com.cachorrovascaino.plugin.Data.PlayerData;
import com.cachorrovascaino.plugin.Main;
import com.cachorrovascaino.plugin.Utils.EyesUtils;
import com.cachorrovascaino.plugin.Utils.WeatherUtils;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.io.PacketHandler;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.awt.Color;

public class SharinganJutsu implements ClanJutsu {

    public static final SharinganJutsu INSTANCE = new SharinganJutsu();
    private final EyesUtils eyesUtils = new EyesUtils();

    private SharinganJutsu() {}

    @Override public String getId() { return "sharingan"; }
    @Override public String getDisplayName() { return "Sharingan"; }
    @Override public float getChakraCost() { return 30.0f; }
    @Override public float getCooldown() { return 3.0f; }

    @Override
    public boolean canExecute(PlayerRef playerRef) {
        PlayerData playerData = Main.get().getDataManager().getPlayerData(playerRef.getUuid());
        if (playerData == null) return false;

        if (!"Uchiha".equalsIgnoreCase(playerData.getClan())) {
            playerRef.sendMessage(Message.raw("Only members of the Uchiha clan possess the Sharingan.").color(Color.RED));
            return false;
        }

        String currentStageAsset = playerData.getEyeDojutsuType();
        if (currentStageAsset == null || currentStageAsset.isEmpty() || "NONE".equalsIgnoreCase(currentStageAsset)) {
            playerRef.sendMessage(Message.raw("You haven't awakened any stage of the Sharingan yet!").color(Color.RED));
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
        ComponentType<EntityStore, MangekyouSharingan> mangekyouType = Main.get().getMangekyouSharinganComponentType();
        if (sharinganType == null) return;

        PacketHandler packetHandler = playerRef.getPacketHandler();

        world.execute(() -> {
            if (!playerEntityRef.isValid()) return;

            if (store.getComponent(playerEntityRef, sharinganType) != null) {
                store.removeComponent(playerEntityRef, sharinganType);
                if (mangekyouType != null && store.getComponent(playerEntityRef, mangekyouType) != null) {
                    store.removeComponent(playerEntityRef, mangekyouType);
                }
                String originalEyes = playerData.getOriginalEyesId();
                if (originalEyes == null || originalEyes.isEmpty()) {originalEyes = "Medium_Eyes";}
                eyesUtils.updateHytalePlayerEyes(world, playerRef, originalEyes, playerData.getOriginalEyesColor());
                WeatherUtils.resetPlayerWeather(playerRef, packetHandler, "Zone1_Sunny");

                playerRef.sendMessage(Message.raw("Sharingan desativado.").color(Color.GRAY));
            } else {
                store.addComponent(playerEntityRef, sharinganType, new Sharingan());

                eyesUtils.captureOriginalEyesIfNeeded(playerRef, playerData);
                eyesUtils.updateHytalePlayerEyes(world, playerRef, playerData.getEyeDojutsuType(), "");
                WeatherUtils.applyPlayerWeather(playerRef, packetHandler, "Sharingan_Vision");

                playerRef.sendMessage(Message.raw("Sharingan ativado!").color(Color.RED));
            }

            Main.getDataManager().savePlayer(playerRef.getUuid());
        });
    }
}