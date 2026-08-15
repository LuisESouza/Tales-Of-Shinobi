package com.cachorrovascaino.plugin.Ui.Hud;

import com.buuz135.mhud.MultipleHUD;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

public class ComboHud extends CustomUIHud {

    public static final String HUD_ID = "TalesOfShinobi_ComboHud";
    private final String currentSequence;
    private final String currentJutsu;
    private final boolean visible;

    public ComboHud(PlayerRef playerRef, String sequence, String jutsu, boolean visible) {
        super(playerRef, "Shinobi/Huds/ComboJutsu.ui");
        this.currentSequence = sequence != null ? sequence : "";
        this.currentJutsu = jutsu != null ? jutsu : "";
        this.visible = visible;
    }

    @Override
    protected void build(@NonNullDecl UICommandBuilder uiCommandBuilder) {
        uiCommandBuilder.append("Shinobi/Huds/ComboJutsu.ui");

        boolean showContent = visible && (!currentSequence.isEmpty() || !currentJutsu.isEmpty());

        uiCommandBuilder.set("#ComboJutsuRoot.Visible", showContent);
        uiCommandBuilder.set("#KeyText.Text", showContent ? currentSequence : "");
        uiCommandBuilder.set("#JutsuName.Text", showContent ? currentJutsu : "");
    }

    public static void show(Player player, PlayerRef playerRef, String sequence, String jutsu) {
        ComboHud hud = new ComboHud(playerRef, sequence, jutsu, true);
        MultipleHUD.getInstance().setCustomHud(player, playerRef, HUD_ID, hud);
    }

    public static void hide(Player player, PlayerRef playerRef) {
        ComboHud hiddenHud = new ComboHud(playerRef, "", "", false);
        MultipleHUD.getInstance().setCustomHud(player, playerRef, HUD_ID, hiddenHud);
        MultipleHUD.getInstance().hideCustomHud(player, HUD_ID);
    }
}