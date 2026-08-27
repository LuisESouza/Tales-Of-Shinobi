package com.cachorrovascaino.plugin.Ui.Page;

import com.cachorrovascaino.plugin.Data.AttributeType;
import com.cachorrovascaino.plugin.Data.PlayerData;
import com.cachorrovascaino.plugin.Manager.PlayerDataManager;
import com.cachorrovascaino.plugin.Ui.Page.utils.NavigationButtons;
import com.cachorrovascaino.plugin.Utils.ChakraUtils;
import com.cachorrovascaino.plugin.Utils.PlayerStatUtils;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import java.awt.Color;
import java.util.Map;
import java.util.UUID;

public class StatusPage extends InteractiveCustomUIPage<StatusPage.UIEventData> {

    private final PlayerRef playerRef;
    private final UUID uuid;
    private final PlayerDataManager dataManager;

    private static final String INTERFACE_CONTENT = "Shinobi/Menus/StatusMenu.ui";

    private static final Map<String, String> ELEMENT_UI_MAP = Map.of(
            "Earth", "Shinobi/Components/elements/Earth.ui",
            "Fire",  "Shinobi/Components/elements/Fire.ui",
            "Ray",   "Shinobi/Components/elements/Ray.ui",
            "Water", "Shinobi/Components/elements/Water.ui",
            "Wind",  "Shinobi/Components/elements/Wind.ui"
    );

    public StatusPage(@Nonnull PlayerRef playerRef, @Nonnull PlayerDataManager dataManager) {
        super(playerRef, CustomPageLifetime.CanDismissOrCloseThroughInteraction, StatusPage.UIEventData.CODEC);
        this.playerRef = playerRef;
        this.uuid = playerRef.getUuid();
        this.dataManager = dataManager;
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder cmd, @Nonnull UIEventBuilder evt, @Nonnull Store<EntityStore> store) {
        cmd.append(INTERFACE_CONTENT);
        cmd.append("#NavigationContent", "Shinobi/Components/navigation/NavigationMenus.ui");

        buildComponents(cmd, evt, store, ref);
    }

    public void buildComponents(@Nonnull UICommandBuilder cmd, @Nonnull UIEventBuilder evt, @Nonnull Store<EntityStore> store, @Nonnull Ref<EntityStore> ref) {
        String username = playerRef.getUsername();

        var player = Universe.get().getPlayer(uuid);
        if (player != null) {
            username = player.getUsername();
        }

        PlayerData data = dataManager.getPlayerData(uuid);
        if (data == null) {
            data = dataManager.loadPlayer(uuid, username);
        }
        if (data == null) {
            data = new PlayerData(username, uuid.toString());
        }

        buildLabels(cmd, data, store, ref);
        buildButtons(evt);
        buildElementsComponents(cmd, data.getElement());
    }

    public void buildLabels(@Nonnull UICommandBuilder cmd, @Nonnull PlayerData data, @Nonnull Store<EntityStore> store, @Nonnull Ref<EntityStore> ref) {
        String username = playerRef.getUsername();
        float currentChakra = ChakraUtils.getCurrentChakra(playerRef);
        float maxChakra = ChakraUtils.getMaxChakra(playerRef);

        float maxStamina = PlayerStatUtils.getMaxValue(store, ref, DefaultEntityStatTypes.getStamina());
        float maxHealth = PlayerStatUtils.getMaxValue(store, ref, DefaultEntityStatTypes.getHealth());
        float stamina = PlayerStatUtils.getValue(store, ref, DefaultEntityStatTypes.getStamina());
        float health = PlayerStatUtils.getValue(store, ref, DefaultEntityStatTypes.getHealth());

        if (maxChakra <= 0) { maxChakra = 100.0f; }
        if (currentChakra <= 0 && maxChakra > 0) { currentChakra = maxChakra; }

        cmd.set("#LabelPlayerName.Text", "Name: " + username);
        cmd.set("#LabelClan.Text", "Clan: " + (data.getClan() != null ? data.getClan() : "None"));
        cmd.set("#LabelPoints.Text", "Attribute Points: " + data.getAvailablePoints());

        cmd.set("#LabelLife.Text", "Vitality: " + (int) health + " / " + (int) maxHealth);
        cmd.set("#LabelStamina.Text", "Stamina: " + (int) stamina + " / " + (int) maxStamina);
        cmd.set("#LabelChakra.Text", "Chakra: " + (int) currentChakra + " / " + (int) maxChakra);
        cmd.set("#LabelSpeed.Text", "Speed: " + data.getSpeed());

        cmd.set("#LabelNinjutsu.Text", "Ninjutsu: " + data.getNinjutsu());
        cmd.set("#LabelTaijutsu.Text", "Taijutsu: " + data.getTaijutsu());
        cmd.set("#LabelGenjutsu.Text", "Genjutsu: " + data.getGenjutsu());
        cmd.set("#LabelChakraControl.Text", "Chakra Ctrl: " + data.getChakraControl());
    }

    public void buildButtons(UIEventBuilder evt) {
        evt.addEventBinding(CustomUIEventBindingType.Activating, "#BtnLife", new EventData().append("Action", "life"), false);
        evt.addEventBinding(CustomUIEventBindingType.Activating, "#BtnStamina", new EventData().append("Action", "stamina"), false);
        evt.addEventBinding(CustomUIEventBindingType.Activating, "#BtnSpeed", new EventData().append("Action", "speed"), false);

        evt.addEventBinding(CustomUIEventBindingType.Activating, "#BtnChakra", new EventData().append("Action", "chakra"), false);
        evt.addEventBinding(CustomUIEventBindingType.Activating, "#BtnNinjutsu", new EventData().append("Action", "ninjutsu"), false);
        evt.addEventBinding(CustomUIEventBindingType.Activating, "#BtnTaijutsu", new EventData().append("Action", "taijutsu"), false);
        evt.addEventBinding(CustomUIEventBindingType.Activating, "#BtnGenjutsu", new EventData().append("Action", "genjutsu"), false);
        evt.addEventBinding(CustomUIEventBindingType.Activating, "#BtnChakraControl", new EventData().append("Action", "chakra_control"), false);

        NavigationButtons.bindButtons(evt);
    }

    public void buildElementsComponents(@Nonnull UICommandBuilder cmd, String element) {
        if (element == null) return;

        String uiPath = ELEMENT_UI_MAP.get(element);
        if (uiPath != null) {
            cmd.append("#RowElement", uiPath);
        }
    }

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, @Nonnull UIEventData data) {
        if (data.getAction() == null) return;

        if (NavigationButtons.handleNavigation(data.getAction(), store, ref, playerRef)) {
            return;
        }

        PlayerData playerData = dataManager.getPlayerData(uuid);
        if (playerData == null) return;

        AttributeType attribute = AttributeType.fromAction(data.getAction());
        if (attribute == null) return;

        if (playerData.getAvailablePoints() <= 0) {
            playerRef.sendMessage(Message.raw("Sem pontos suficientes!").color(Color.RED));
            return;
        }

        AttributeType.UpgradeContext context = new AttributeType.UpgradeContext(
                playerData,
                playerRef,
                store,
                ref
        );
        attribute.apply(context);

        playerData.setAvailablePoints(playerData.getAvailablePoints() - 1);
        dataManager.savePlayer(uuid);

        rebuild();
    }

    public static class UIEventData {
        public static final BuilderCodec<StatusPage.UIEventData> CODEC = BuilderCodec.builder(StatusPage.UIEventData.class, StatusPage.UIEventData::new)
                .append(new KeyedCodec<>("Action", Codec.STRING), (e, v) -> e.action = v, e -> e.action).add()
                .append(new KeyedCodec<>("Slot", Codec.STRING), (e, v) -> e.slot = v, e -> e.slot).add()
                .build();

        private String action;
        private String slot;

        public UIEventData() {}

        public String getAction() {
            return action;
        }

        public String getSlot() {
            return slot;
        }
    }
}