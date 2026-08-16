package com.cachorrovascaino.plugin.Abstractions;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

/**
 * Espelha a interface Jutsu, mas pra habilidades de clã (Susanoo, Jūken, Kaiten,
 * Mangekyō, etc). Separado de Jutsu porque jutsus de clã são equipados/executados
 * num hotbar próprio (equippedClanHotbar), com seu próprio esquema de combo,
 * controlado pelo ClanManager em vez do JutsuManager.
 */
public interface ClanJutsu {
    String getId();
    String getDisplayName();

    float getChakraCost();

    float getCooldown();

    default float getChakraCost(PlayerRef playerRef) {
        return getChakraCost();
    }

    /**
     * NOVO: pré-condição opcional, checada ANTES de cobrar chakra/setar cooldown.
     * Ex: Mangekyō só pode ser usado com o Dōjutsu base já ativo - se retornar false,
     * a execução é cancelada sem custo nenhum pro jogador. A própria implementação
     * é responsável por mandar a mensagem de erro pro jogador, se quiser.
     */
    default boolean canExecute(PlayerRef playerRef) {
        return true;
    }

    void execute(PlayerRef playerRef, Ref<EntityStore> playerEntityRef, Store<EntityStore> store, World world);
}