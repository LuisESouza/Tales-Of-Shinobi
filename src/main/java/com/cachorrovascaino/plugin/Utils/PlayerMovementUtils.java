package com.cachorrovascaino.plugin.Utils;

import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.protocol.MovementSettings;
import com.hypixel.hytale.server.core.entity.entities.player.movement.MovementManager;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import javax.annotation.Nonnull;

public final class PlayerMovementUtils {

    private PlayerMovementUtils() {}

    /**
     * Congela completamente a movimentação do jogador (andado, corrida, pulo e agachamento).
     * Sincroniza a alteração instantaneamente com o cliente via pacote de rede.
     */
    public static void freezePlayer(@Nonnull Ref<EntityStore> targetRef, @Nonnull ComponentAccessor<EntityStore> accessor) {
        MovementManager movementManager = accessor.getComponent(targetRef, MovementManager.getComponentType());
        PlayerRef playerRef = accessor.getComponent(targetRef, PlayerRef.getComponentType());

        if (movementManager == null || playerRef == null) {
            return;
        }

        MovementSettings settings = movementManager.getSettings();
        if (settings == null) {
            return;
        }

        settings.baseSpeed = 0.0F;
        settings.jumpForce = 0.0F;
        settings.swimJumpForce = 0.0F;

        settings.forwardWalkSpeedMultiplier = 0.0F;
        settings.backwardWalkSpeedMultiplier = 0.0F;
        settings.strafeWalkSpeedMultiplier = 0.0F;

        settings.forwardRunSpeedMultiplier = 0.0F;
        settings.backwardRunSpeedMultiplier = 0.0F;
        settings.strafeRunSpeedMultiplier = 0.0F;

        settings.forwardSprintSpeedMultiplier = 0.0F;

        settings.forwardCrouchSpeedMultiplier = 0.0F;
        settings.backwardCrouchSpeedMultiplier = 0.0F;
        settings.strafeCrouchSpeedMultiplier = 0.0F;

        movementManager.update(playerRef.getPacketHandler());
    }

    /**
     * Restaura os valores padrão de movimentação do jogador e envia a atualização ao cliente.
     */
    public static void unfreezePlayer(@Nonnull Ref<EntityStore> targetRef, @Nonnull ComponentAccessor<EntityStore> accessor) {
        MovementManager movementManager = accessor.getComponent(targetRef, MovementManager.getComponentType());

        if (movementManager == null) {
            return;
        }

        movementManager.resetDefaultsAndUpdate(targetRef, accessor);
    }
}
