package com.cachorrovascaino.plugin.Utils;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.hypixel.hytale.server.core.inventory.InventoryComponent;
import com.hypixel.hytale.server.core.inventory.InventoryComponent.Utility;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nullable;

public class ItemUtils {

    /**
     * Retorna o Item atual na mão principal do jogador (Hotbar ativa).
     */
    @Nullable
    public static Item getMainHandItem(Ref<EntityStore> playerRef) {
        if (playerRef == null || !playerRef.isValid()) return null;

        var store = playerRef.getStore();
        ItemStack stack = InventoryComponent.getItemInHand(store, playerRef);

        return (stack != null && !stack.isEmpty()) ? stack.getItem() : null;
    }

    /**
     * Retorna o Item atual na mão secundária (Off-hand / Utility slot).
     */
    @Nullable
    public static Item getOffHandItem(Ref<EntityStore> playerRef) {
        if (playerRef == null || !playerRef.isValid()) return null;

        var store = playerRef.getStore();
        Utility utilityComponent = store.getComponent(playerRef, Utility.getComponentType());
        if (utilityComponent == null) return null;

        ItemStack stack = utilityComponent.getActiveItem();
        return (stack != null && !stack.isEmpty()) ? stack.getItem() : null;
    }

    /**
     * Verifica se o jogador está de mão vazia na mão principal.
     */
    public static boolean isMainHandEmpty(Ref<EntityStore> playerRef) {
        return getMainHandItem(playerRef) == null;
    }
}