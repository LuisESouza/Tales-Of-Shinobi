package com.cachorrovascaino.plugin.Utils;

import com.cachorrovascaino.plugin.Data.PlayerData;
import com.cachorrovascaino.plugin.Main;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatValue;
import com.hypixel.hytale.server.core.modules.entitystats.modifier.Modifier;
import com.hypixel.hytale.server.core.modules.entitystats.modifier.StaticModifier;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;

public class ChakraUtils {

    private static final String MAX_CHAKRA_MODIFIER_KEY = "naruto_mod_max_chakra";

    /**
     * Valida se o índice retornado pelo Main é um id registrado válido.
     */
    private static boolean isValidStatIndex(int index) {
        return index >= 0;
    }

    /**
     * Retorna a quantidade atual de Chakra do jogador.
     */
    public static float getCurrentChakra(@Nonnull PlayerRef playerRef) {
        PlayerData data = Main.getDataManager().getPlayerData(playerRef.getUuid());
        if (data != null) {
            return data.getCurrentChakra();
        }

        int chakraIndex = Main.getChakraStatIndex();
        if (!isValidStatIndex(chakraIndex)) return 0.0f;

        try {
            EntityStatMap statMap = playerRef.getComponent(EntityStatMap.getComponentType());
            if (statMap != null) {
                EntityStatValue statValue = statMap.get(chakraIndex);
                if (statValue != null) {
                    return statValue.get();
                }
            }
        } catch (Exception ignored) {}
        return 0.0f;
    }

    /**
     * Retorna o limite máximo de Chakra do jogador.
     */
    public static float getMaxChakra(@Nonnull PlayerRef playerRef) {
        PlayerData data = Main.getDataManager().getPlayerData(playerRef.getUuid());
        if (data != null) {
            return data.getMaxChakra();
        }

        int chakraIndex = Main.getChakraStatIndex();
        if (!isValidStatIndex(chakraIndex)) return 0.0f;

        try {
            EntityStatMap statMap = playerRef.getComponent(EntityStatMap.getComponentType());
            if (statMap != null) {
                EntityStatValue statValue = statMap.get(chakraIndex);
                if (statValue != null) {
                    return statValue.getMax();
                }
            }
        } catch (Exception ignored) {}
        return 0.0f;
    }

    /**
     * Verifica se o jogador possui Chakra suficiente.
     */
    public static boolean hasEnoughChakra(@Nonnull PlayerRef playerRef, float amount) {
        return getCurrentChakra(playerRef) >= amount;
    }

    /**
     * Define o limite máximo de Chakra.
     */
    public static void setMaxChakra(@Nonnull PlayerRef playerRef, float maxAmount) {
        PlayerData data = Main.getDataManager().getPlayerData(playerRef.getUuid());
        if (data != null) {
            data.setMaxChakra(maxAmount);
            Main.getDataManager().savePlayer(playerRef.getUuid());
        }

        int chakraIndex = Main.getChakraStatIndex();
        if (!isValidStatIndex(chakraIndex)) return;

        Ref<EntityStore> ref = playerRef.getReference();
        if (ref == null) return;

        Store<EntityStore> store = ref.getStore();

        World world = ((EntityStore) store.getExternalData()).getWorld();

        world.execute(() -> {
            try {
                EntityStatMap statMap = (EntityStatMap) store.getComponent(ref, EntityStatMap.getComponentType());
                if (statMap != null) {
                    StaticModifier modifier = new StaticModifier(
                            Modifier.ModifierTarget.MAX,
                            StaticModifier.CalculationType.ADDITIVE,
                            maxAmount
                    );
                    statMap.putModifier(chakraIndex, MAX_CHAKRA_MODIFIER_KEY, modifier);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    /**
     * Inicializa o Chakra do jogador se for a primeira vez.
     */
    public static void initChakraIfFirstTime(@Nonnull PlayerRef playerRef, float defaultAmount, float defaultMax) {
        PlayerData data = Main.getDataManager().getPlayerData(playerRef.getUuid());
        if (data != null && data.getMaxChakra() <= 0) {
            data.setMaxChakra(defaultMax);
            data.setCurrentChakra(defaultAmount);
            Main.getDataManager().savePlayer(playerRef.getUuid());
        }

        int chakraIndex = Main.getChakraStatIndex();
        if (!isValidStatIndex(chakraIndex)) return;

        Ref<EntityStore> ref = playerRef.getReference();
        if (ref == null) return;

        Store<EntityStore> store = ref.getStore();

        World world = ((EntityStore) store.getExternalData()).getWorld();

        world.execute(() -> {
            try {
                EntityStatMap statMap = (EntityStatMap) store.getComponent(ref, EntityStatMap.getComponentType());
                if (statMap != null) {
                    boolean alreadyInitialized = statMap.getModifier(chakraIndex, MAX_CHAKRA_MODIFIER_KEY) != null;

                    if (!alreadyInitialized) {
                        StaticModifier modifier = new StaticModifier(
                                Modifier.ModifierTarget.MAX,
                                StaticModifier.CalculationType.ADDITIVE,
                                defaultMax
                        );
                        statMap.putModifier(chakraIndex, MAX_CHAKRA_MODIFIER_KEY, modifier);
                        statMap.setStatValue(chakraIndex, defaultAmount);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    /**
     * Consome Chakra com validação de memória e sincronização.
     */
    public static boolean consumeChakra(@Nonnull PlayerRef playerRef, float amount) {
        if (!hasEnoughChakra(playerRef, amount)) {
            return false;
        }

        PlayerData data = Main.getDataManager().getPlayerData(playerRef.getUuid());
        if (data != null) {
            float current = data.getCurrentChakra();
            data.setCurrentChakra(Math.max(0.0f, current - amount));
            Main.getDataManager().savePlayer(playerRef.getUuid());
        }

        int chakraIndex = Main.getChakraStatIndex();
        if (isValidStatIndex(chakraIndex)) {
            Ref<EntityStore> ref = playerRef.getReference();
            if (ref != null) {
                Store<EntityStore> store = ref.getStore();
                World world = ((EntityStore) store.getExternalData()).getWorld();
                world.execute(() -> {
                    try {
                        EntityStatMap statMap = (EntityStatMap) store.getComponent(ref, EntityStatMap.getComponentType());
                        if (statMap != null) {
                            statMap.subtractStatValue(chakraIndex, amount);
                        }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                });
            }
        }

        return true;
    }

    /**
     * Adiciona/Regenera Chakra respeitando o limite máximo do jogador.
     */
    public static void addChakra(@Nonnull PlayerRef playerRef, float amount) {
        if (amount <= 0) return;

        PlayerData data = Main.getDataManager().getPlayerData(playerRef.getUuid());
        if (data != null) {
            float max = data.getMaxChakra();
            float current = data.getCurrentChakra();
            float newAmount = Math.min(current + amount, max);

            data.setCurrentChakra(newAmount);
            Main.getDataManager().savePlayer(playerRef.getUuid());
        }

        int chakraIndex = Main.getChakraStatIndex();
        if (!isValidStatIndex(chakraIndex)) return;

        Ref<EntityStore> ref = playerRef.getReference();
        if (ref == null) return;

        Store<EntityStore> store = ref.getStore();

        World world = ((EntityStore) store.getExternalData()).getWorld();

        world.execute(() -> {
            try {
                EntityStatMap statMap = (EntityStatMap) store.getComponent(ref, EntityStatMap.getComponentType());
                if (statMap != null) {
                    statMap.addStatValue(chakraIndex, amount);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    /**
     * Define o valor atual de Chakra diretamente.
     */
    public static void setChakra(@Nonnull PlayerRef playerRef, float amount) {
        PlayerData data = Main.getDataManager().getPlayerData(playerRef.getUuid());
        if (data != null) {
            data.setCurrentChakra(amount);
            Main.getDataManager().savePlayer(playerRef.getUuid());
        }

        int chakraIndex = Main.getChakraStatIndex();
        if (!isValidStatIndex(chakraIndex)) return;

        Ref<EntityStore> ref = playerRef.getReference();
        if (ref == null) return;

        Store<EntityStore> store = ref.getStore();

        World world = ((EntityStore) store.getExternalData()).getWorld();

        world.execute(() -> {
            try {
                EntityStatMap statMap = (EntityStatMap) store.getComponent(ref, EntityStatMap.getComponentType());
                if (statMap != null) {
                    statMap.setStatValue(chakraIndex, amount);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}