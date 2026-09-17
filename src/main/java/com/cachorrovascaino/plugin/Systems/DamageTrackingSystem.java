package com.cachorrovascaino.plugin.Systems;

import com.cachorrovascaino.plugin.Data.Components.Byakugan;
import com.cachorrovascaino.plugin.Data.Components.HakkeKushoComponent;
import com.cachorrovascaino.plugin.Data.PlayerData;
import com.cachorrovascaino.plugin.Features.Clan.ByakuganJutsu;
import com.cachorrovascaino.plugin.Main;
import com.cachorrovascaino.plugin.Utils.ChakraUtils;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.dependency.Dependency;
import com.hypixel.hytale.component.dependency.RootDependency;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.protocol.ChangeVelocityType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageEventSystem;
import com.hypixel.hytale.server.core.modules.physics.component.Velocity;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.meta.MetaKey;
import org.joml.Vector3d;

import java.awt.Color;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nonnull;

public class DamageTrackingSystem extends DamageEventSystem {

    private final Map<UUID, UUID> lastAttackers;
    public static final MetaKey<Boolean> RPG_DAMAGE_PROCESSED = Damage.META_REGISTRY.registerMetaObject(data -> false);

    private static final Random RANDOM = new Random();
    private static final float CHAKRA_DRAIN_AMOUNT = 60.0f;
    private static final double DRAIN_CHANCE = 0.15;

    private static Method TAKE_COMMAND_BUFFER_METHOD;
    private static Method CONSUME_METHOD;

    static {
        try {
            TAKE_COMMAND_BUFFER_METHOD = Store.class.getDeclaredMethod("takeCommandBuffer");
            TAKE_COMMAND_BUFFER_METHOD.setAccessible(true);

            CONSUME_METHOD = CommandBuffer.class.getDeclaredMethod("consume");
            CONSUME_METHOD.setAccessible(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public DamageTrackingSystem(Map<UUID, UUID> lastAttackers) {
        this.lastAttackers = lastAttackers;
    }

    @Override
    public void handle(int index, @Nonnull ArchetypeChunk<EntityStore> chunk, @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer, @Nonnull Damage damage) {
        Ref<EntityStore> targetRef = chunk.getReferenceTo(index);

        if (!targetRef.isValid() || damage.getMetaObject(RPG_DAMAGE_PROCESSED)) return;

        Ref<EntityStore> attackerRef = null;
        if (damage.getSource() instanceof Damage.EntitySource src) {
            attackerRef = src.getRef();
        } else if (damage.getSource() instanceof Damage.ProjectileSource src) {
            attackerRef = src.getRef();
        }

        if (attackerRef == null || !attackerRef.isValid()) return;

        UUIDComponent attackerUuidComp = (UUIDComponent) store.getComponent(attackerRef, UUIDComponent.getComponentType());
        UUIDComponent targetUuidComp = (UUIDComponent) store.getComponent(targetRef, UUIDComponent.getComponentType());

        if (attackerUuidComp == null || targetUuidComp == null) return;

        UUID attackerUUID = attackerUuidComp.getUuid();
        UUID targetUUID = targetUuidComp.getUuid();

        PlayerData attackerData = Main.getDataManager().getPlayerData(attackerUUID);
        int taijutsu = (attackerData != null) ? attackerData.getTaijutsu() : 0;

        if (taijutsu > 0 && damage.getAmount() > 0.0f) {
            float taijutsuBonus = taijutsu * 2.5f;
            float taijutsuNow = damage.getAmount();
            damage.setAmount(taijutsuNow + taijutsuBonus);
        }

        // --- HAKKE KŪSHŌ (PALMA DE VÁCUO) ---
        ComponentType<EntityStore, HakkeKushoComponent> kushoType = Main.get().getHakkeKushoComponentType();
        if (kushoType != null) {
            HakkeKushoComponent kushoBuff = store.getComponent(attackerRef, kushoType);

            if (kushoBuff != null) {


                if (kushoBuff.isExpired()) {
                    commandBuffer.removeComponent(attackerRef, kushoType);
                } else if (damage.getAmount() > 0.0f) {
                    damage.setAmount(damage.getAmount() + 45.0f);

                    TransformComponent attackerTrans = store.getComponent(attackerRef, TransformComponent.getComponentType());
                    TransformComponent targetTrans = store.getComponent(targetRef, TransformComponent.getComponentType());

                    if (attackerTrans != null && targetTrans != null) {
                        Vector3d pushVector = new Vector3d(targetTrans.getPosition()).sub(attackerTrans.getPosition());
                        pushVector.y = 0;

                        if (pushVector.lengthSquared() > 0.0001) {
                            pushVector.normalize();
                        } else {
                            pushVector.set(1, 0, 0);
                        }

                        Vector3d launchVelocity = new Vector3d(
                                pushVector.x * 305.0,
                                12.0,
                                pushVector.z * 305.0
                        );

                        applyVelocityInstruction(store, targetRef, launchVelocity, ChangeVelocityType.Set);
                    }

                    kushoBuff.consumeCharge();
                    PlayerRef attackerPlayerRef = store.getComponent(attackerRef, PlayerRef.getComponentType());

                    if (attackerPlayerRef != null) {
                        attackerPlayerRef.sendMessage(
                                Message.raw("[Vacuum Palm] Released air wave! Charges left: " + kushoBuff.getCharges())
                                        .color(Color.CYAN)
                        );
                    }

                    if (kushoBuff.isExpired()) {
                        commandBuffer.removeComponent(attackerRef, kushoType);
                    }
                }
            }
        }

        // --- BYAKUGAN (TENKETSU DRAIN) ---
        ComponentType<EntityStore, Byakugan> byakuganType = Main.get().getByakuganComponentType();
        if (byakuganType != null && store.getComponent(attackerRef, byakuganType) != null) {
            if (RANDOM.nextDouble() <= DRAIN_CHANCE) {
                PlayerRef targetPlayerRef = store.getComponent(targetRef, PlayerRef.getComponentType());

                if (targetPlayerRef != null) {
                    float currentTargetChakra = ChakraUtils.getCurrentChakra(targetPlayerRef);

                    if (currentTargetChakra > 0) {
                        float drainAmount = Math.min(currentTargetChakra, CHAKRA_DRAIN_AMOUNT);
                        ChakraUtils.consumeChakra(targetPlayerRef, drainAmount);
                        Main.getJutsuManager().updateChakraHud(targetPlayerRef);

                        PlayerRef attackerPlayerRef = store.getComponent(attackerRef, PlayerRef.getComponentType());

                        if (attackerPlayerRef != null) {
                            ByakuganJutsu.INSTANCE.triggerByakuganChakraScan(attackerRef, store);
                            attackerPlayerRef.sendMessage(
                                    Message.raw("[Gentle Fist] You struck a tenketsu point! Drained " + (int) drainAmount + " Chakra from " + targetPlayerRef.getUsername() + ".")
                                            .color(Color.GREEN)
                            );
                        }
                        targetPlayerRef.sendMessage(
                                Message.raw("[Gentle Fist] Your tenketsu was struck! You lost " + (int) drainAmount + " Chakra.")
                                        .color(Color.RED)
                        );
                    }
                }
            }
        }

        this.lastAttackers.put(targetUUID, attackerUUID);
    }

    private void applyVelocityInstruction(Store<EntityStore> store, Ref<EntityStore> entityRef, Vector3d velocity, ChangeVelocityType type) {
        if (entityRef == null || !entityRef.isValid()) return;

        try {
            @SuppressWarnings("unchecked")
            CommandBuffer<EntityStore> commandBuffer = (CommandBuffer<EntityStore>) TAKE_COMMAND_BUFFER_METHOD.invoke(store);

            commandBuffer.run(runStore -> {
                if (!entityRef.isValid()) return;

                Velocity velComponent = runStore.getComponent(entityRef, Velocity.getComponentType());
                if (velComponent != null) {
                    velComponent.addInstruction(velocity, null, type);
                }
            });

            CONSUME_METHOD.invoke(commandBuffer);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public Set<Dependency<EntityStore>> getDependencies() {
        return Collections.singleton(RootDependency.first());
    }

    @Override
    public Query<EntityStore> getQuery() {
        return Query.any();
    }
}