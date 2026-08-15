package com.cachorrovascaino.plugin.Ui.Hud;

import com.buuz135.mhud.MultipleHUD;
import com.cachorrovascaino.plugin.Data.PlayerData;
import com.cachorrovascaino.plugin.Main;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

public class LevelHud extends CustomUIHud {

    public static final String HUD_ID = "TalesOfShinobi_LevelHud";

    public LevelHud(PlayerRef playerRef) {
        super(playerRef, "Shinobi/Huds/LevelHud.ui");
    }

    @Override
    protected void build(@NonNullDecl UICommandBuilder uiCommandBuilder) {
        uiCommandBuilder.append("Shinobi/Huds/LevelHud.ui");

        PlayerRef playerRef = getPlayerRef();
        PlayerData data = Main.getDataManager().getPlayerData(playerRef.getUuid());

        if (data != null) {
            float currentXp = data.getCurrentXp();
            float xpUp = data.getXpUp();
            int level = data.getCurrentLevel();

            float progress = (xpUp > 0) ? Math.min(currentXp / xpUp, 1.0f) : 0.0f;

            uiCommandBuilder.set("#XpBar.Value", progress);
            uiCommandBuilder.set("#XpText.Text", "Level: " + level + " XP: " + (int) currentXp + " / " + (int) xpUp);
        }
    }

    /**
     * Exibe ou atualiza a HUD do jogador
     */
    public static void update(Player player, PlayerRef playerRef) {
        if (player == null || playerRef == null) return;
        LevelHud hud = new LevelHud(playerRef);
        MultipleHUD.getInstance().setCustomHud(player, playerRef, HUD_ID, hud);
    }

    public static void hide(Player player, PlayerRef playerRef) {
        if (player == null) return;
        MultipleHUD.getInstance().hideCustomHud(player, HUD_ID);
    }
}