package com.cachorrovascaino.plugin.Systems;

import com.cachorrovascaino.plugin.Data.Components.DeathProcessed;
import com.cachorrovascaino.plugin.Main;
import com.cachorrovascaino.plugin.Data.PlayerData;
import com.cachorrovascaino.plugin.Ui.Hud.LevelHud;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathComponent;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatsModule;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.awt.Color;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nonnull;

public class DeathDetectionSystem extends EntityTickingSystem<EntityStore> {

    private final Map<UUID, UUID> lastAttackers;
    private final ComponentType<EntityStore, DeathProcessed> markerType;

    public DeathDetectionSystem(Map<UUID, UUID> lastAttackers, ComponentType<EntityStore, DeathProcessed> markerType) {
        this.lastAttackers = lastAttackers;
        this.markerType = markerType;
    }

    @Override
    public void tick(float dt, int index, @Nonnull ArchetypeChunk<EntityStore> chunk, @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer) {
        Ref<EntityStore> ref = chunk.getReferenceTo(index);
        if (!ref.isValid()) return;

        if (store.getComponent(ref, this.markerType) == null) {
            UUIDComponent uuidComp = (UUIDComponent) store.getComponent(ref, UUIDComponent.getComponentType());
            if (uuidComp != null) {
                UUID entityUuid = uuidComp.getUuid();
                UUID attackerUuid = this.lastAttackers.remove(entityUuid);

                if (attackerUuid != null) {
                    PlayerRef killer = Universe.get().getPlayer(attackerUuid);
                    if (killer != null && killer.isValid()) {

                        EntityStatMap statMap = (EntityStatMap) store.getComponent(ref, EntityStatsModule.get().getEntityStatMapComponentType());
                        double maxHp = 10.0;
                        if (statMap != null) {
                            var health = statMap.get(DefaultEntityStatTypes.getHealth());
                            if (health != null) maxHp = health.getMax();
                        }

                        float xpFinal = Math.max(10.0f, (float) (10.0 + (maxHp * 0.15f)));

                        PlayerData playerData = Main.getDataManager().getPlayerData(killer.getUuid());
                        if (playerData != null) {
                            boolean leveledUp = playerData.addXp(xpFinal);

                            Ref<EntityStore> killerEntityRef = killer.getReference();
                            if (killerEntityRef != null && killerEntityRef.isValid()) {
                                Player killerPlayer = store.getComponent(killerEntityRef, Player.getComponentType());

                                if (killerPlayer != null) {
                                    LevelHud.update(killerPlayer, killer);
                                }
                            }

                            // Feedback visual no chat
                            if (leveledUp) {
                                killer.sendMessage(Message.raw("========================================").color(Color.YELLOW));
                                killer.sendMessage(Message.raw(" LEVEL UP! Você alcançou o Nível " + playerData.getCurrentLevel() + "!").color(Color.GREEN));
                                killer.sendMessage(Message.raw(" +3 Pontos de Atributo disponíveis!").color(Color.YELLOW));
                                killer.sendMessage(Message.raw("========================================").color(Color.YELLOW));
                            } else {
                                killer.sendMessage(Message.raw("+ " + (int) xpFinal + " XP (" + (int) playerData.getCurrentXp() + "/" + (int) playerData.getXpUp() + ")").color(Color.CYAN));
                            }
                        }

                        commandBuffer.addComponent(ref, this.markerType, new DeathProcessed());
                    }
                }
            }
        }
    }

    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        return Query.and(DeathComponent.getComponentType(), Query.not(this.markerType));
    }
}