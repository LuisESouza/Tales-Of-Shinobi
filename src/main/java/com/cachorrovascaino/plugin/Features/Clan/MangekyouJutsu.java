package com.cachorrovascaino.plugin.Features.Clan;

import com.cachorrovascaino.plugin.Abstractions.ClanJutsu;
import com.cachorrovascaino.plugin.Data.PlayerData;
import com.cachorrovascaino.plugin.Main;
import com.cachorrovascaino.plugin.Utils.EyesUtils;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.awt.Color;

/**
 * Mangekyō Sharingan - habilidade de clã EQUIPÁVEL (não faz parte do toggle
 * passivo F-F-F). Precisa do Dōjutsu base já ativo pra poder ser usada
 * (ver canExecute). Cada execução liga/desliga o Mangekyō por cima do
 * estágio base atual (sharingan_3, etc) - funciona como uma transformação
 * dentro da transformação.
 */
public class MangekyouJutsu implements ClanJutsu {

    public static final MangekyouJutsu INSTANCE = new MangekyouJutsu();

    private static final String MANGEKYOU_EYE_ASSET = "Mangekyo_Sharingan_Obito_HD";

    private final EyesUtils eyesUtils = new EyesUtils();

    private MangekyouJutsu() {
    }

    @Override
    public String getId() {
        return "mangekyou";
    }

    @Override
    public String getDisplayName() {
        return "Mangekyō Sharingan";
    }

    @Override
    public float getChakraCost() {
        return 80.0f;
    }

    @Override
    public float getCooldown() {
        return 60.0f;
    }

    @Override
    public boolean canExecute(PlayerRef playerRef) {
        PlayerData playerData = Main.get().getDataManager().getPlayerData(playerRef.getUuid());
        if (playerData == null) return false;

        if (!playerData.isEyeDojutsuActive()) {
            playerRef.sendMessage(Message.raw("Ative o Dōjutsu (F-F-F) antes de usar o Mangekyō!").color(Color.RED));
            return false;
        }

        return true;
    }

    @Override
    public void execute(PlayerRef playerRef, Ref<EntityStore> playerEntityRef, Store<EntityStore> store, World world) {
        PlayerData playerData = Main.get().getDataManager().getPlayerData(playerRef.getUuid());
        if (playerData == null) return;

        boolean mangekyouActive = MANGEKYOU_EYE_ASSET.equals(playerData.getEyesId());

        if (mangekyouActive) {
            String baseStage = playerData.getEyeDojutsuType();
            playerData.applyDojutsuEyes(baseStage, "");
            eyesUtils.updateHytalePlayerEyes(playerRef, baseStage, "");
            playerRef.sendMessage(Message.raw("Mangekyō Sharingan desativado.").color(Color.GRAY));
        } else {
            playerData.applyDojutsuEyes(MANGEKYOU_EYE_ASSET, "");
            eyesUtils.updateHytalePlayerEyes(playerRef, MANGEKYOU_EYE_ASSET, "");
            playerRef.sendMessage(Message.raw("Mangekyō Sharingan ativado!").color(Color.RED));
        }

        Main.get().getDataManager().savePlayer(playerRef.getUuid());
    }
}
