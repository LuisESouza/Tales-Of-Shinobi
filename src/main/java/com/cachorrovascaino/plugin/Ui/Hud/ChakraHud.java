package com.cachorrovascaino.plugin.Ui.Hud;

import com.buuz135.mhud.MultipleHUD;
import com.cachorrovascaino.plugin.Utils.ChakraUtils;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

public class ChakraHud extends CustomUIHud {

    public static final String HUD_ID = "TalesOfShinobi_ChakraHud";

    public ChakraHud(PlayerRef playerRef) {
        super(playerRef, "Shinobi/Huds/Chakra.ui");
    }

    @Override
    protected void build(@NonNullDecl UICommandBuilder uiCommandBuilder) {
        uiCommandBuilder.append("Shinobi/Huds/Chakra.ui");

        PlayerRef playerRef = getPlayerRef();

        float current = ChakraUtils.getCurrentChakra(playerRef);
        float max = ChakraUtils.getMaxChakra(playerRef);

        float progress = (max > 0) ? Math.min(current / max, 1.0f) : 0.0f;

        uiCommandBuilder.set("#ChakraBar.Value", progress);
        uiCommandBuilder.set("#ChakraText.Text", (int) current + " / " + (int) max);
    }

    public static void show(Player player, PlayerRef playerRef) {
        if (player == null || playerRef == null) return;
        ChakraHud hud = new ChakraHud(playerRef);
        MultipleHUD.getInstance().setCustomHud(player, playerRef, HUD_ID, hud);
    }

    public static void hide(Player player) {
        if (player == null) return;
        MultipleHUD.getInstance().hideCustomHud(player, HUD_ID);
    }
}