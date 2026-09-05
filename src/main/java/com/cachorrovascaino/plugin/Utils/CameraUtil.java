package com.cachorrovascaino.plugin.Utils;

import com.hypixel.hytale.protocol.*;
import com.hypixel.hytale.protocol.packets.camera.SetServerCamera;
import com.hypixel.hytale.server.core.universe.PlayerRef;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class CameraUtil {

    private static final ScheduledExecutorService SCHEDULER = Executors.newScheduledThreadPool(2);

    /**
     * Define uma câmera estilo jogo de luta (visão lateral/close-up cinemático).
     */
    public static void setCinematicCamera(PlayerRef playerRef, float distance, float yawDegrees, float pitchDegrees) {
        if (playerRef == null) return;

        ServerCameraSettings settings = new ServerCameraSettings();
        settings.distance = distance;
        settings.isFirstPerson = false;
        settings.positionLerpSpeed = 0.3f;
        settings.rotationLerpSpeed = 0.3f;
        settings.eyeOffset = true;
        settings.positionDistanceOffsetType = PositionDistanceOffsetType.DistanceOffsetRaycast;

        settings.rotationType = RotationType.Custom;
        Direction rotationDir = new Direction(
                (float) Math.toRadians(yawDegrees),
                (float) Math.toRadians(pitchDegrees),
                0f
        );
        settings.rotation = rotationDir;

        settings.movementForceRotationType = MovementForceRotationType.AttachedToHead;

        playerRef.getPacketHandler().writeNoCache(
                new SetServerCamera(ClientCameraView.Custom, true, settings)
        );
    }

    /**
     * Aplica um efeito de trepidação (shake) alterando rapidamente a distância/ângulo.
     */
    public static void applyCameraShake(PlayerRef playerRef, float baseDistance, float yawDegrees, float pitchDegrees, int durationMs) {
        if (playerRef == null) return;

        float shakeDistance = baseDistance - 0.4f;
        float shakePitch = pitchDegrees + 2.0f;

        setCinematicCamera(playerRef, shakeDistance, yawDegrees, shakePitch);

        SCHEDULER.schedule(() -> {
            setCinematicCamera(playerRef, baseDistance, yawDegrees, pitchDegrees);
        }, durationMs, TimeUnit.MILLISECONDS);
    }

    /**
     * Restaura a câmera do jogador para a visão padrão (primeira/terceira pessoa normal).
     */
    public static void resetCamera(PlayerRef playerRef) {
        if (playerRef == null) return;

        playerRef.getPacketHandler().writeNoCache(
                new SetServerCamera(ClientCameraView.Custom, false, null)
        );
    }
}