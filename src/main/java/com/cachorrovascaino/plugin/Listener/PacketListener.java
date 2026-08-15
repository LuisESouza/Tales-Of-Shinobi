package com.cachorrovascaino.plugin.Listener;

import com.cachorrovascaino.plugin.Main;
import com.cachorrovascaino.plugin.Utils.ItemUtils;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.protocol.MovementStates;
import com.hypixel.hytale.protocol.Packet;
import com.hypixel.hytale.protocol.packets.interaction.SyncInteractionChain;
import com.hypixel.hytale.protocol.packets.interaction.SyncInteractionChains;
import com.hypixel.hytale.server.core.entity.movement.MovementStatesComponent;
import com.hypixel.hytale.server.core.io.adapter.PlayerPacketFilter;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;

import javax.annotation.Nonnull;

public class PacketListener implements PlayerPacketFilter {

    private final Main plugin;

    public PacketListener(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean test(@Nonnull PlayerRef playerRef, @Nonnull Packet packet) {

        var ref = playerRef.getReference();
        if (ref == null || !ref.isValid()) { return false; }

        // --- CHAKRA  RELOAD ---
        if (playerRef.getWorldUuid() != null) {
            World world = Universe.get().getWorld(playerRef.getWorldUuid());
            if (world != null) {
                world.execute(() -> {
                    MovementStatesComponent moveComp = ref.getStore().getComponent(ref, MovementStatesComponent.getComponentType());

                    if (moveComp != null) {
                        MovementStates states = moveComp.getMovementStates();

                        if (states != null && states.crouching) {
                            plugin.getJutsuManager().startCharging(playerRef);
                        } else {
                            plugin.getJutsuManager().stopCharging(playerRef);
                        }
                    }
                });
            }
        }

        // --- JUTSUS SYSTEM ---
        if (packet instanceof SyncInteractionChains syncPacket) {
            if (syncPacket.updates != null) {
                for (SyncInteractionChain chain : syncPacket.updates) {
                    if (chain == null) continue;

                    if (chain.initial) {
                        boolean isRightClick = false;
                        boolean isLeftClick = chain.interactionType == InteractionType.Primary;

                        if (chain.interactionType == InteractionType.Use) {isRightClick = true;}

                        if (isLeftClick || isRightClick) {
                            if (playerRef.getWorldUuid() == null) { return false; }

                            World world = Universe.get().getWorld(playerRef.getWorldUuid());
                            if (world == null) { return false; }

                            final boolean rightClickFinal = isRightClick;

                            world.execute(() -> {
                                var itemNaMao = ItemUtils.getMainHandItem(ref);

                                if (itemNaMao != null) { return; }

                                if (rightClickFinal) {
                                    plugin.getJutsuManager().processClick(playerRef, true);
                                } else {
                                    plugin.getJutsuManager().processClick(playerRef, false);
                                }
                            });
                            break;
                        }
                    }
                }
            }
        }

        return false;
    }
}