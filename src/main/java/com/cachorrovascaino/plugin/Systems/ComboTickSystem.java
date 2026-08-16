package com.cachorrovascaino.plugin.Systems;

import com.cachorrovascaino.plugin.Data.PlayerData;
import com.cachorrovascaino.plugin.Manager.JutsuManager;
import com.cachorrovascaino.plugin.Manager.PlayerDataManager;
import com.cachorrovascaino.plugin.Ui.Hud.JutsuEquippedHud;
import com.cachorrovascaino.plugin.Utils.EyesUtils;
import com.cachorrovascaino.plugin.Utils.WeatherUtils;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.DelayedEntitySystem;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.io.PacketHandler;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;

public class ComboTickSystem extends DelayedEntitySystem<EntityStore> {

    private final JutsuManager jutsuManager;
    private final PlayerDataManager dataManager;
    private final EyesUtils eyesUtils;
    private final WeatherUtils weatherUtils;

    public ComboTickSystem(JutsuManager jutsuManager, PlayerDataManager dataManager, EyesUtils eyesUtils, WeatherUtils weatherUtils) {
        super(0.1f);
        this.jutsuManager = jutsuManager;
        this.dataManager = dataManager;
        this.eyesUtils = eyesUtils;
        this.weatherUtils = weatherUtils;
    }

    @Override
    public void tick(float dt, int index, @Nonnull ArchetypeChunk<EntityStore> archetypeChunk,
                     @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer) {

        Ref<EntityStore> ref = archetypeChunk.getReferenceTo(index);
        PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());

        if (playerRef != null) {
            jutsuManager.ensureChakraHudLoaded(playerRef);
            jutsuManager.tickPlayerCombo(playerRef);

            Player player = store.getComponent(ref, Player.getComponentType());
            if (player != null) {
                JutsuEquippedHud.show(player, playerRef);
            }
            processDojutsuDrain(playerRef);
        }
    }

    private void processDojutsuDrain(PlayerRef playerRef) {
        PlayerData data = dataManager.getPlayerData(playerRef.getUuid());
        if (data == null || !data.isEyeDojutsuActive()) return;

        float chakraDrainPerSecond = switch (data.getEyeStage()) {
            case 1 -> 2.0f;
            case 2 -> 4.0f;
            case 3 -> 6.0f;
            default -> 10.0f;
        };

        float drainPerTick = chakraDrainPerSecond / 10.0f;

        if (data.getCurrentChakra() >= drainPerTick) {
            data.setCurrentChakra(data.getCurrentChakra() - drainPerTick);
            jutsuManager.updateChakraHud(playerRef);
        } else {
            data.setCurrentChakra(0.0f);
            data.setEyeDojutsuActive(false);

            jutsuManager.updateChakraHud(playerRef);

            data.restoreOriginalEyes();
            eyesUtils.updateHytalePlayerEyes(playerRef, data.getEyesId(), data.getOriginalEyesColor());

            PacketHandler packetHandler = playerRef.getPacketHandler();
            if (packetHandler != null) {
                weatherUtils.resetPlayerWeather(playerRef, packetHandler, "Sun");
            }

            dataManager.savePlayer(playerRef.getUuid());
        }
    }

    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        return Query.and(Player.getComponentType());
    }
}