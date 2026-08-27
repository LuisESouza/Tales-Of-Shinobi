package com.cachorrovascaino.plugin.Data;

import com.cachorrovascaino.plugin.Utils.ChakraUtils;
import com.cachorrovascaino.plugin.Utils.PlayerStatUtils;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.util.function.Consumer;

public enum AttributeType {

    LIFE("life", (context) ->
            PlayerStatUtils.increaseMaxHealth(context.store, context.ref, 10)),

    STAMINA("stamina", (context) ->
            PlayerStatUtils.increaseMaxStamina(context.store, context.ref, 10)),

    SPEED("speed", (context) -> {
        int newSpeedLevel = context.data.getSpeed() + 1;
        context.data.setSpeed(newSpeedLevel);

        PlayerStatUtils.applyPlayerSpeed(context.store, context.ref, context.playerRef, newSpeedLevel);
    }),

    CHAKRA("chakra", (context) -> {
        float newMax = context.data.getMaxChakra() + 20.0f;
        ChakraUtils.setMaxChakra(context.playerRef, newMax);
        ChakraUtils.addChakra(context.playerRef, 20.0f);
    }),

    NINJUTSU("ninjutsu", (context) ->
            context.data.setNinjutsu(context.data.getNinjutsu() + 1)),

    TAIJUTSU("taijutsu", (context) ->
            context.data.setTaijutsu(context.data.getTaijutsu() + 1)),

    GENJUTSU("genjutsu", (context) ->
            context.data.setGenjutsu(context.data.getGenjutsu() + 1)),

    CHAKRA_CONTROL("chakra_control", (context) ->
            context.data.setChakraControl(context.data.getChakraControl() + 1));

    private final String actionKey;
    private final Consumer<UpgradeContext> upgradeLogic;

    AttributeType(String actionKey, Consumer<UpgradeContext> upgradeLogic) {
        this.actionKey = actionKey;
        this.upgradeLogic = upgradeLogic;
    }

    public static AttributeType fromAction(String action) {
        for (AttributeType type : values()) {
            if (type.actionKey.equalsIgnoreCase(action)) {
                return type;
            }
        }
        return null;
    }

    public void apply(UpgradeContext context) {
        this.upgradeLogic.accept(context);
    }

    public record UpgradeContext(
            PlayerData data,
            PlayerRef playerRef,
            Store<EntityStore> store,
            Ref<EntityStore> ref
    ) {}
}