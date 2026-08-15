package com.cachorrovascaino.plugin.Manager;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CooldownManager {

    private final Map<UUID, Map<String, Long>> cooldowns = new HashMap<>();

    /**
     * Aplica um cooldown em segundos para um jutsu específico do jogador.
     */
    public void setCooldown(UUID playerUuid, String jutsuId, double seconds) {
        long expireTime = System.currentTimeMillis() + (long) (seconds * 1000);
        cooldowns.computeIfAbsent(playerUuid, k -> new HashMap<>()).put(jutsuId, expireTime);
    }

    /**
     * Verifica se o jutsu está em cooldown.
     */
    public boolean isOnCooldown(UUID playerUuid, String jutsuId) {
        return getRemainingSeconds(playerUuid, jutsuId) > 0;
    }

    /**
     * Retorna o tempo restante em SEGUNDOS. Retorna 0.0 se não estiver em cooldown.
     */
    public float getRemainingSeconds(UUID playerUuid, String jutsuId) {
        Map<String, Long> playerCooldowns = cooldowns.get(playerUuid);
        if (playerCooldowns == null || !playerCooldowns.containsKey(jutsuId)) {
            return 0.0f;
        }

        long expireTime = playerCooldowns.get(jutsuId);
        long now = System.currentTimeMillis();

        if (now >= expireTime) {
            playerCooldowns.remove(jutsuId);
            return 0.0f;
        }

        return (expireTime - now) / 1000.0f;
    }
}