package com.cachorrovascaino.plugin.Features.Clan.Abilities;

import com.cachorrovascaino.plugin.Abstractions.ClanJutsu;
import com.cachorrovascaino.plugin.Data.Components.MangekyouSharingan;
import com.cachorrovascaino.plugin.Data.PlayerData;
import com.cachorrovascaino.plugin.Main;
import com.cachorrovascaino.plugin.Utils.TargetUtils;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.joml.Vector3d;

import java.awt.Color;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class KotoamatsukamiOpticalTetherJutsu implements ClanJutsu {

    public static final KotoamatsukamiOpticalTetherJutsu INSTANCE = new KotoamatsukamiOpticalTetherJutsu();
    private static final ScheduledExecutorService SCHEDULER = Executors.newScheduledThreadPool(2);
    private static final double MAX_TARGET_RANGE = 25.0;
    private static final double TETHER_MAX_DISTANCE = 8.0;

    private KotoamatsukamiOpticalTetherJutsu() {}

    @Override public String getId() { return "kotoamatsukami_tether"; }
    @Override public String getDisplayName() { return "Kotoamatsukami: Optical Tether"; }
    @Override public float getChakraCost() { return 120.0f; }
    @Override public float getCooldown() { return 25.0f; }

    @Override
    public boolean canExecute(PlayerRef playerRef) {
        PlayerData playerData = Main.getDataManager().getPlayerData(playerRef.getUuid());
        if (playerData == null) return false;

        if (!"SHISUI".equalsIgnoreCase(playerData.getMangekyouType())) {
            playerRef.sendMessage(Message.raw("This Mangekyō Sharingan does not possess Optical Tether!").color(Color.RED));
            return false;
        }

        return true;
    }

    @Override
    public void execute(PlayerRef playerRef, Ref<EntityStore> playerEntityRef, Store<EntityStore> store, World world) {
        if (playerEntityRef == null || !playerEntityRef.isValid() || world == null) return;
        if (!canExecute(playerRef)) return;

        ComponentType<EntityStore, MangekyouSharingan> mangekyouType = Main.get().getMangekyouSharinganComponentType();

        world.execute(() -> {
            if (!playerEntityRef.isValid()) return;

            if (mangekyouType == null || store.getComponent(playerEntityRef, mangekyouType) == null) {
                playerRef.sendMessage(Message.raw("Activate Mangekyō Sharingan to use Kotoamatsukami!").color(Color.RED));
                return;
            }

            Ref<EntityStore> targetRef = TargetUtils.getTargetInLineOfSight(playerEntityRef, store, MAX_TARGET_RANGE, 45.0);

            if (targetRef == null) {
                playerRef.sendMessage(Message.raw("Optical Tether failed: no target in line of sight.").color(Color.GRAY));
                return;
            }

            final Ref<EntityStore> finalTarget = targetRef;
            playerRef.sendMessage(Message.raw("Kotoamatsukami: Optical Tether linked to target!").color(Color.GREEN));

            final int[] ticks = {0};
            ScheduledFuture<?>[] taskHolder = new ScheduledFuture<?>[1];

            taskHolder[0] = SCHEDULER.scheduleAtFixedRate(() -> {
                world.execute(() -> {
                    if (!playerEntityRef.isValid() || !finalTarget.isValid() || ticks[0] >= 30) { // 6 segundos de duração
                        if (taskHolder[0] != null) taskHolder[0].cancel(false);
                        return;
                    }

                    TransformComponent userTrans = store.getComponent(playerEntityRef, TransformComponent.getComponentType());
                    TransformComponent targetTrans = store.getComponent(finalTarget, TransformComponent.getComponentType());

                    if (userTrans != null && targetTrans != null) {
                        Vector3d userPos = userTrans.getPosition();
                        Vector3d targetPos = targetTrans.getPosition();

                        double distance = userPos.distance(targetPos);

                        if (distance > TETHER_MAX_DISTANCE) {
                            Vector3d pullDirection = new Vector3d(userPos).sub(targetPos).normalize();
                            Vector3d pulledPos = new Vector3d(userPos).sub(pullDirection.mul(TETHER_MAX_DISTANCE - 0.5));
                            targetTrans.teleportPosition(pulledPos);
                        }
                    }

                    ticks[0]++;
                });
            }, 0, 200, TimeUnit.MILLISECONDS);
        });
    }
}