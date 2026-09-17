package com.cachorrovascaino.plugin.Features.Ninjutsu.heal;

import com.cachorrovascaino.plugin.Abstractions.Jutsu;
import com.cachorrovascaino.plugin.Abstractions.SkillType;
import com.cachorrovascaino.plugin.Data.Jutsus.JutsuType;
import com.cachorrovascaino.plugin.Data.PlayerData;
import com.cachorrovascaino.plugin.Main;
import com.cachorrovascaino.plugin.Utils.PlayerStatUtils;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.spatial.SpatialResource;
import com.hypixel.hytale.protocol.SoundCategory;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.modules.entity.EntityModule;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.ParticleUtil;
import com.hypixel.hytale.server.core.universe.world.SoundUtil;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.joml.Vector3d;

import java.awt.Color;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class HealJutsu implements Jutsu {

    public static final HealJutsu INSTANCE = new HealJutsu();
    private static final ScheduledExecutorService SCHEDULER = Executors.newScheduledThreadPool(2);

    private static final String HEAL_PARTICLE_ID = "Totem_Heal_TopIcon";
    private static final float CHAKRA_COST_PER_TICK = 5.0f;
    private static final float HEAL_AMOUNT_PER_TICK = 3.0f;
    private static final long TICK_INTERVAL_MS = 250;
    private static final double MOVE_THRESHOLD = 0.15;

    private static final double HEAD_Y_OFFSET = 0.01;

    @Override public String getId() { return "mystical_palm_heal_jutsu"; }
    @Override public String getDisplayName() { return JutsuType.HEAL_JUTSU.getName(); }
    @Override public float getChakraCost() { return JutsuType.HEAL_JUTSU.getResourceCost(); }
    @Override public float getCooldown() { return JutsuType.HEAL_JUTSU.getCooldown(); }
    @Override public SkillType getType() { return SkillType.NINJUTSU; }

    @Override
    public void execute(PlayerRef playerRef, Ref<EntityStore> playerEntityRef, Store<EntityStore> store, World world) {
        if (playerRef == null || playerEntityRef == null || !playerEntityRef.isValid()) return;

        SoundUtil.playSoundEvent2dToPlayer(playerRef, "SFX_Jutsu_Sound", SoundCategory.SFX);

        UUID uuid = playerRef.getUuid();

        TransformComponent transform = store.getComponent(playerEntityRef, TransformComponent.getComponentType());
        if (transform == null) return;

        Vector3d initialPos = new Vector3d(transform.getPosition());

        playerRef.sendMessage(Message.raw("You started channeling healing... Stay still!").color(Color.GREEN));

        AtomicBoolean isChanneling = new AtomicBoolean(true);

        final int[] tickCounter = {0};

        ScheduledFuture<?> healTask = SCHEDULER.scheduleAtFixedRate(() -> {
            if (!isChanneling.get()) return;

            world.execute(() -> {
                try {
                    if (!playerEntityRef.isValid()) {
                        isChanneling.set(false);
                        return;
                    }

                    TransformComponent currentTransform = store.getComponent(playerEntityRef, TransformComponent.getComponentType());
                    if (currentTransform == null) {
                        isChanneling.set(false);
                        return;
                    }

                    Vector3d currentPos = currentTransform.getPosition();
                    if (initialPos.distance(currentPos) > MOVE_THRESHOLD) {
                        isChanneling.set(false);
                        playerRef.sendMessage(Message.raw("Healing canceled: You moved!").color(Color.YELLOW));
                        return;
                    }

                    int healthIndex = DefaultEntityStatTypes.getHealth();
                    float currentHealth = PlayerStatUtils.getValue(store, playerEntityRef, healthIndex);
                    float maxHealth = PlayerStatUtils.getMaxValue(store, playerEntityRef, healthIndex);

                    if (currentHealth >= maxHealth) {
                        isChanneling.set(false);
                        playerRef.sendMessage(Message.raw("Healing completed: Full health!").color(Color.GREEN));
                        return;
                    }

                    PlayerData playerData = Main.getDataManager().getPlayerData(uuid);
                    if (playerData == null || playerData.getCurrentChakra() < CHAKRA_COST_PER_TICK) {
                        isChanneling.set(false);
                        playerRef.sendMessage(Message.raw("Healing canceled: Insufficient chakra!").color(Color.RED));
                        return;
                    }

                    playerData.setCurrentChakra(playerData.getCurrentChakra() - CHAKRA_COST_PER_TICK);
                    PlayerStatUtils.healDirectly(store, playerEntityRef, HEAL_AMOUNT_PER_TICK);
                    Main.getJutsuManager().updateChakraHud(playerRef);

                    if (tickCounter[0] % 4 == 0) {
                        spawnHeadParticlePulse(store, playerEntityRef, currentTransform, 0.9f);
                    }
                    tickCounter[0]++;

                } catch (Exception e) {
                    isChanneling.set(false);
                }
            });
        }, 0, TICK_INTERVAL_MS, TimeUnit.MILLISECONDS);

        SCHEDULER.schedule(() -> {
            if (!isChanneling.get()) {
                healTask.cancel(true);
            }
        }, 30, TimeUnit.SECONDS);
    }

    private void spawnHeadParticlePulse(Store<EntityStore> store, Ref<EntityStore> playerEntityRef, TransformComponent transform, float durationSeconds) {
        Vector3d headPos = new Vector3d(
                transform.getPosition().x,
                transform.getPosition().y + HEAD_Y_OFFSET,
                transform.getPosition().z
        );

        SpatialResource<Ref<EntityStore>, EntityStore> playerSpatial = store.getResource(EntityModule.get().getPlayerSpatialResourceType());

        @SuppressWarnings("unchecked")
        List<Ref<EntityStore>> playersToNotify = (List<Ref<EntityStore>>) (List<?>) SpatialResource.getThreadLocalReferenceList();
        playerSpatial.getSpatialStructure().collect(headPos, 85.0, playersToNotify);

        if (!playersToNotify.isEmpty()) {
            ParticleUtil.spawnParticleEffect(
                    HEAL_PARTICLE_ID,
                    headPos.x(),
                    headPos.y(),
                    headPos.z(),
                    0.0f,
                    0.0f,
                    0.0f,
                    1.0f,
                    null,
                    null,
                    playersToNotify,
                    store,
                    durationSeconds
            );
        }
    }
}