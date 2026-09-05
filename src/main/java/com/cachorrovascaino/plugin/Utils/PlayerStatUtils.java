package com.cachorrovascaino.plugin.Utils;

import com.cachorrovascaino.plugin.Data.PlayerData;
import com.cachorrovascaino.plugin.Manager.PlayerDataManager;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.MovementSettings;
import com.hypixel.hytale.server.core.entity.entities.player.movement.MovementManager;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatValue;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.modules.entitystats.modifier.Modifier;
import com.hypixel.hytale.server.core.modules.entitystats.modifier.StaticModifier;
import com.hypixel.hytale.server.core.modules.entitystats.modifier.Modifier.ModifierTarget;
import com.hypixel.hytale.server.core.modules.entitystats.modifier.StaticModifier.CalculationType;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import java.util.UUID;

public final class PlayerStatUtils {
    private final PlayerDataManager dataManager;
    private final PlayerRef playerRef;
    private final UUID uuid;

    private PlayerStatUtils(@Nonnull PlayerRef playerRef, PlayerDataManager dataManager) {
        this.dataManager = dataManager;
        this.playerRef = playerRef;
        this.uuid = playerRef.getUuid();
    }

    public static void healDirectly(Store<EntityStore> store, Ref<EntityStore> ref, float amount) {
        int healthIndex = DefaultEntityStatTypes.getHealth();
        EntityStatMap statMap = store.getComponent(ref, EntityStatMap.getComponentType());
        if (statMap != null) {
            EntityStatValue health = statMap.get(healthIndex);
            if (health != null) {
                float newHealth = Math.min(health.getMax(), health.get() + amount);
                statMap.setStatValue(healthIndex, newHealth);
            }
        }
    }

    // ============================================================
    // CONSULTA DE DADOS NATIVOS (GETTERS)
    // ============================================================

    public static float getValue(Store<EntityStore> store, Ref<EntityStore> ref, int statIndex) {
        EntityStatMap statMap = store.getComponent(ref, EntityStatMap.getComponentType());
        if (statMap == null) return 0.0F;

        EntityStatValue stat = statMap.get(statIndex);
        return stat != null ? stat.get() : 0.0F;
    }

    public static float getMaxValue(Store<EntityStore> store, Ref<EntityStore> ref, int statIndex) {
        EntityStatMap statMap = store.getComponent(ref, EntityStatMap.getComponentType());
        if (statMap == null) return 0.0F;

        EntityStatValue stat = statMap.get(statIndex);
        return stat != null ? stat.getMax() : 0.0F;
    }

    // ============================================================
    // MODIFICADORES NATIVOS DA ENGINE (VIDA E STAMINA)
    // ============================================================

    public static void increaseMaxHealth(Store<EntityStore> store, Ref<EntityStore> ref, float amount) {
        applyStaticModifier(store, ref, DefaultEntityStatTypes.getHealth(), "increaseMaxHealth", amount);
    }

    public static void increaseMaxStamina(Store<EntityStore> store, Ref<EntityStore> ref, float amount) {
        applyStaticModifier(store, ref, DefaultEntityStatTypes.getStamina(), "increaseMaxStamina", amount);
    }

    // ============================================================
    // CONSUMO DE RECURSOS NATIVOS
    // ============================================================

    public static void consumeStamina(Store<EntityStore> store, Ref<EntityStore> ref, float percentual) {
        consumeResource(store, ref, DefaultEntityStatTypes.getStamina(), percentual);
    }

    public static void consumeHealthDirectly(Store<EntityStore> store, Ref<EntityStore> ref, float amount) {
        int healthIndex = DefaultEntityStatTypes.getHealth();
        EntityStatMap statMap = store.getComponent(ref, EntityStatMap.getComponentType());
        if (statMap != null) {
            EntityStatValue health = statMap.get(healthIndex);
            if (health != null) {
                float newHealth = Math.max(0.0F, health.get() - amount);
                statMap.setStatValue(healthIndex, newHealth);
            }
        }
    }

    // ============================================================
    // MÉTODOS INTERNOS
    // ============================================================

    private static void applyStaticModifier(Store<EntityStore> store, Ref<EntityStore> ref, int index, String key, float amount) {
        EntityStatMap statMap = store.getComponent(ref, EntityStatMap.getComponentType());
        if (statMap == null) return;

        Modifier existing = statMap.getModifier(index, key);
        float newAmount = (existing instanceof StaticModifier sm) ? sm.getAmount() + amount : amount;

        Modifier modifier = new StaticModifier(ModifierTarget.MAX, CalculationType.ADDITIVE, newAmount);
        statMap.putModifier(index, key, modifier);
    }

    private static void consumeResource(Store<EntityStore> store, Ref<EntityStore> ref, int index, float percentual) {
        EntityStatMap statMap = store.getComponent(ref, EntityStatMap.getComponentType());
        if (statMap == null) return;

        EntityStatValue stat = statMap.get(index);
        if (stat != null) {
            float gasto = stat.getMax() * percentual;
            float novaValue = Math.max(0.0F, stat.get() - gasto);
            statMap.setStatValue(index, novaValue);
        }
    }

    public static final float BASE_HYTALE_SPEED = 5.5f;
    public static final float SPEED_PERCENT_PER_LEVEL = 0.002f;

    public static void applyPlayerSpeed(Store<EntityStore> store, Ref<EntityStore> ref, PlayerRef playerRef, int speedLevel) {
        if (playerRef == null || ref == null || !ref.isValid()) return;

        float newBaseSpeed = BASE_HYTALE_SPEED * (1.0f + ((speedLevel - 1) * SPEED_PERCENT_PER_LEVEL));

        MovementManager movementManager = store.getComponent(ref, MovementManager.getComponentType());
        if (movementManager != null) {
            MovementSettings currentSettings = movementManager.getSettings();
            if (currentSettings != null) {
                currentSettings.baseSpeed = newBaseSpeed;
            }

            MovementSettings defaultSettings = movementManager.getDefaultSettings();
            if (defaultSettings != null) {
                defaultSettings.baseSpeed = newBaseSpeed;
            }

            if (playerRef.getPacketHandler() != null) {
                movementManager.update(playerRef.getPacketHandler());
            }
        }
    }
}