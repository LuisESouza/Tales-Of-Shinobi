package com.cachorrovascaino.plugin.Systems;

import com.cachorrovascaino.plugin.Data.Components.Byakugan;
import com.cachorrovascaino.plugin.Data.Components.MangekyouSharingan;
import com.cachorrovascaino.plugin.Data.Components.Sharingan;
import com.cachorrovascaino.plugin.Data.PlayerData;
import com.cachorrovascaino.plugin.Features.Clan.ByakuganJutsu;
import com.cachorrovascaino.plugin.Main;
import com.cachorrovascaino.plugin.Manager.JutsuManager;
import com.cachorrovascaino.plugin.Manager.PlayerDataManager;
import com.cachorrovascaino.plugin.Ui.Hud.JutsuEquippedHud;
import com.cachorrovascaino.plugin.Utils.EyesUtils;
import com.cachorrovascaino.plugin.Utils.WeatherUtils;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.DelayedEntitySystem;
import com.hypixel.hytale.protocol.MovementStates;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.movement.MovementStatesComponent;
import com.hypixel.hytale.server.core.io.PacketHandler;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;

public class ComboTickSystem extends DelayedEntitySystem<EntityStore> {

    private final JutsuManager jutsuManager;
    private final PlayerDataManager dataManager;
    private final EyesUtils eyesUtils;

    public ComboTickSystem(JutsuManager jutsuManager, PlayerDataManager dataManager, EyesUtils eyesUtils, WeatherUtils weatherUtils) {
        super(0.1f);
        this.jutsuManager = jutsuManager;
        this.dataManager = dataManager;
        this.eyesUtils = eyesUtils;
    }

    @Override
    public void tick(float dt, int index, @Nonnull ArchetypeChunk<EntityStore> archetypeChunk,
                     @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer) {

        Ref<EntityStore> ref = archetypeChunk.getReferenceTo(index);
        PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());

        if (playerRef != null) {
            jutsuManager.ensureChakraHudLoaded(playerRef);
            jutsuManager.tickPlayerCombo(playerRef);

            World world = store.getExternalData().getWorld();

            if (playerRef.getWorldUuid() != null && world != null) {
                world.execute(() -> {
                    MovementStatesComponent moveComp = store.getComponent(ref, MovementStatesComponent.getComponentType());
                    if (moveComp != null) {
                        MovementStates states = moveComp.getMovementStates();

                        if (states != null && states.crouching) {
                            Main.getJutsuManager().startCharging(playerRef);
                        } else {
                            Main.getJutsuManager().stopCharging(playerRef);
                        }
                    }
                });
            }

            Player player = store.getComponent(ref, Player.getComponentType());
            if (player != null) {
                JutsuEquippedHud.show(player, playerRef);
            }

            processDojutsuDrain(ref, playerRef, store, commandBuffer, world);
        }
    }

    private void processDojutsuDrain(Ref<EntityStore> playerEntityRef, PlayerRef playerRef, Store<EntityStore> store, CommandBuffer<EntityStore> commandBuffer, World world) {
        PlayerData data = dataManager.getPlayerData(playerRef.getUuid());
        if (data == null) return;

        ComponentType<EntityStore, Sharingan> sharinganType = Main.get().getSharinganComponentType();
        ComponentType<EntityStore, MangekyouSharingan> mangekyouType = Main.get().getMangekyouSharinganComponentType();
        ComponentType<EntityStore, Byakugan> byakuganType = Main.get().getByakuganComponentType();

        boolean hasSharingan = sharinganType != null && store.getComponent(playerEntityRef, sharinganType) != null;
        boolean hasMangekyou = mangekyouType != null && store.getComponent(playerEntityRef, mangekyouType) != null;
        boolean hasByakugan = byakuganType != null && store.getComponent(playerEntityRef, byakuganType) != null;

        if (!hasSharingan && !hasMangekyou && !hasByakugan) return;

        float chakraDrainPerSecond;

        if (hasMangekyou) {
            chakraDrainPerSecond = 12.0f;
        } else if (hasByakugan) {
            chakraDrainPerSecond = 5.0f;
        } else {
            chakraDrainPerSecond = switch (data.getEyeStage()) {
                case 1 -> 2.0f;
                case 2 -> 4.0f;
                case 3 -> 6.0f;
                default -> 10.0f;
            };
        }

        float drainPerTick = chakraDrainPerSecond / 10.0f;

        if (data.getCurrentChakra() >= drainPerTick) {
            data.setCurrentChakra(data.getCurrentChakra() - drainPerTick);
            jutsuManager.updateChakraHud(playerRef);
        } else {
            data.setCurrentChakra(0.0f);
            jutsuManager.updateChakraHud(playerRef);

            if (hasSharingan) commandBuffer.removeComponent(playerEntityRef, sharinganType);
            if (hasMangekyou) commandBuffer.removeComponent(playerEntityRef, mangekyouType);
            if (hasByakugan) {
                commandBuffer.removeComponent(playerEntityRef, byakuganType);
                ByakuganJutsu.INSTANCE.clearByakuganChakraScan(playerEntityRef, store);
            }

            String originalEyes = data.getOriginalEyesId();
            String originalColor = data.getEyesColor();
            if (originalEyes != null && !originalEyes.isEmpty()) {
                eyesUtils.updateHytalePlayerEyes(world, playerRef, originalEyes, originalColor);
            }

            PacketHandler packetHandler = playerRef.getPacketHandler();
            if (packetHandler != null) {
                WeatherUtils.resetPlayerWeather(playerRef, packetHandler, "Zone1_Sunny");
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