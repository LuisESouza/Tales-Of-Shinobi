package com.cachorrovascaino.plugin.Ui.Hud;

import com.buuz135.mhud.MultipleHUD;
import com.cachorrovascaino.plugin.Data.Jutsus.JutsuType;
import com.cachorrovascaino.plugin.Data.PlayerData;
import com.cachorrovascaino.plugin.Main;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.util.Map;

public class JutsuEquippedHud extends CustomUIHud {
    public static final String HUD_ID = "TalesOfShinobi_JutsuEquippedHud";
    private static final String SLOT_TEMPLATE = "Shinobi/Components/huds/SlotComponent.ui";

    public JutsuEquippedHud(PlayerRef playerRef) {
        super(playerRef, "Shinobi/Huds/JutsuEquipped.ui");
    }

    @Override
    protected void build(@NonNullDecl UICommandBuilder uiCommandBuilder) {
        uiCommandBuilder.append("Shinobi/Huds/JutsuEquipped.ui");
        PlayerRef playerRef = getPlayerRef();
        renderSlots(uiCommandBuilder, playerRef);
    }

    private void renderSlots(UICommandBuilder uiCommandBuilder, PlayerRef playerRef) {
        if (playerRef == null) return;

        PlayerData data = Main.getDataManager().getPlayerData(playerRef.getUuid());
        Map<String, String> hotbar = (data != null) ? data.getEquippedHotbar() : null;

        uiCommandBuilder.clear("#SlotsContainer");

        for (int i = 1; i <= 4; i++) {
            uiCommandBuilder.append("#SlotsContainer", SLOT_TEMPLATE);

            String basePath = "#SlotsContainer[" + (i - 1) + "]";
            String slotKey = "slot_" + i;
            String jutsuId = (hotbar != null) ? hotbar.get(slotKey) : null;

            JutsuType jutsu = JutsuType.fromId(jutsuId);

            if (jutsu == null) {
                uiCommandBuilder.set(basePath + " #SlotName.Text", "Empty Slot");
                uiCommandBuilder.set(basePath + " #CooldownBar.Value", 0.0f);
                uiCommandBuilder.set(basePath + " #SlotCooldown.Text", "-");
                continue;
            }

            uiCommandBuilder.set(basePath + " #SlotName.Text", jutsu.getName());

            float remainingSec = Main.getCooldownManager().getRemainingSeconds(playerRef.getUuid(), jutsu.getId());
            double totalCooldown = jutsu.getCooldown();

            boolean onCooldown = remainingSec > 0.0f;

            if (onCooldown) {
                float progress = (totalCooldown > 0) ? (remainingSec / (float) totalCooldown) : 0.0f;

                uiCommandBuilder.set(basePath + " #CooldownBar.Value", progress);
                uiCommandBuilder.set(basePath + " #SlotCooldown.Text", String.format("%.1fs", remainingSec));
                continue;
            }

            uiCommandBuilder.set(basePath + " #CooldownBar.Value", 1.0f);
            uiCommandBuilder.set(basePath + " #SlotCooldown.Text", "READY");
        }
    }

    // --- MÉTODOS ESTÁTICOS ---
    public static void show(Player player, PlayerRef playerRef) {
        if (player == null || playerRef == null) return;
        JutsuEquippedHud hud = new JutsuEquippedHud(playerRef);
        MultipleHUD.getInstance().setCustomHud(player, playerRef, HUD_ID, hud);
    }

    public static void update(Player player, PlayerRef playerRef) {
        if (player == null || playerRef == null) return;
        show(player, playerRef);
    }
}