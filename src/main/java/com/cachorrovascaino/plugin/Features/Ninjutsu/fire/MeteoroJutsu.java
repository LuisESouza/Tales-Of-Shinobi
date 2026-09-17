package com.cachorrovascaino.plugin.Features.Ninjutsu.fire;

import com.cachorrovascaino.plugin.Abstractions.Jutsu;
import com.cachorrovascaino.plugin.Abstractions.SkillType;
import com.cachorrovascaino.plugin.Data.Jutsus.JutsuType;
import com.cachorrovascaino.plugin.Data.PlayerData;
import com.cachorrovascaino.plugin.Main;
import com.cachorrovascaino.plugin.Systems.DamageTrackingSystem;
import com.cachorrovascaino.plugin.Utils.ProjectileJutsuUtils;
import com.cachorrovascaino.plugin.Utils.TargetUtils;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Rotation3f;
import com.hypixel.hytale.protocol.SoundCategory;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.ParticleUtil;
import com.hypixel.hytale.server.core.universe.world.SoundUtil;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.modules.entity.component.HeadRotation;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageCause;
import com.hypixel.hytale.server.core.modules.physics.util.PhysicsMath;
import org.joml.Vector3d;

import java.awt.Color;
import java.lang.reflect.Method;
import java.util.List;

public class MeteoroJutsu implements Jutsu {

    public static final MeteoroJutsu INSTANCE = new MeteoroJutsu();
    private static final float BASE_DAMAGE = 150.0f;
    private static final float DAMAGE_PER_LEVEL = 25.0f;

    private static final float DIRECT_DAMAGE_PERCENTAGE = 1.0f;
    private static final float AOE_DAMAGE_PERCENTAGE = 0.40f;

    private static final double AOE_RADIUS = 8.0;
    private static final String INDICATOR_PARTICLE_ID = "Fire_AoE_Spawn";

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

    @Override public String getId() { return "tengai_shinsei"; }
    @Override public String getDisplayName() { return "Tengai Shinsei"; }
    @Override public float getChakraCost() { return JutsuType.METEORO.getResourceCost();}
    @Override public float getCooldown() { return JutsuType.METEORO.getCooldown(); }
    @Override public SkillType getType() { return SkillType.NINJUTSU; }

    public float getDamageForPlayer(PlayerRef playerRef) {
        PlayerData data = Main.getDataManager().getPlayerData(playerRef.getUuid());
        int level = (data != null) ? data.getJutsuLevel(getId()) : 1;
        return BASE_DAMAGE + ((level - 1) * DAMAGE_PER_LEVEL);
    }

    @Override
    public void execute(PlayerRef playerRef, Ref<EntityStore> playerEntityRef, Store<EntityStore> store, World world) {
        if (playerRef == null || playerEntityRef == null || !playerEntityRef.isValid()) return;

        SoundUtil.playSoundEvent2dToPlayer(playerRef, "SFX_Jutsu_Sound", SoundCategory.SFX);

        float fullDamage = getDamageForPlayer(playerRef);

        double fallSpeed      = 25.0;
        double targetDistance = 25.0;
        double skyHeight      = 90.0;
        float scale           = 10.0f;

        TransformComponent transform = store.getComponent(playerEntityRef, TransformComponent.getComponentType());
        if (transform == null) return;

        HeadRotation headRotation = store.getComponent(playerEntityRef, HeadRotation.getComponentType());
        Rotation3f rotation = (headRotation != null) ? headRotation.getRotation() : transform.getRotation();

        Vector3d forwardVector = new Vector3d();
        PhysicsMath.vectorFromAngles(rotation.yaw(), 0.0f, forwardVector);
        forwardVector.normalize();

        Vector3d targetImpactPos = new Vector3d(transform.getPosition())
                .add(new Vector3d(forwardVector).mul(targetDistance));

        spawnTargetIndicator(targetImpactPos, store, rotation);

        ProjectileJutsuUtils.spawnVerticalMeteorJutsu(
                playerRef,
                playerEntityRef,
                store,
                world,
                "Jutsu_Fireball_Charge",
                fullDamage * DIRECT_DAMAGE_PERCENTAGE,
                fallSpeed,
                targetDistance,
                skyHeight,
                scale,
                "Fire",
                getDisplayName(),
                (actualHitPos) -> {
                    world.execute(() -> {
                        applyAoeDamage(playerRef, playerEntityRef, actualHitPos, fullDamage * AOE_DAMAGE_PERCENTAGE, store);
                    });
                }
        );
    }

    private void spawnTargetIndicator(Vector3d impactPos, Store<EntityStore> store, Rotation3f rotation) {
        List<Ref<EntityStore>> nearbyEntities = TargetUtils.getEntitiesInRadius(impactPos, 85.0, store);

        List<Ref<EntityStore>> playersToNotify = nearbyEntities.stream()
                .filter(ref -> ref != null && ref.isValid() && store.getComponent(ref, PlayerRef.getComponentType()) != null)
                .toList();

        if (!playersToNotify.isEmpty()) {
            Vector3d particlePos = new Vector3d(impactPos.x, impactPos.y + 0.2, impactPos.z);
            ParticleUtil.spawnParticleEffect(INDICATOR_PARTICLE_ID, particlePos, rotation, playersToNotify, store);
        }
    }

    private void applyAoeDamage(
            PlayerRef playerRef,
            Ref<EntityStore> playerEntityRef,
            Vector3d impactPos,
            float aoeDamage,
            Store<EntityStore> store
    ) {
        if (!playerEntityRef.isValid()) return;

        List<Ref<EntityStore>> nearbyEntities = TargetUtils.getEntitiesInRadius(impactPos, AOE_RADIUS, store);

        DamageCause damageCause = DamageCause.getAssetMap().getAsset("Fire");
        if (damageCause == null) {
            damageCause = DamageCause.getAssetMap().getAsset(0);
        }

        Damage.Source source = new Damage.EntitySource(playerEntityRef);
        int targetsHit = 0;
        int playersHit = 0;

        try {
            @SuppressWarnings("unchecked")
            CommandBuffer<EntityStore> commandBuffer = (CommandBuffer<EntityStore>) TAKE_COMMAND_BUFFER_METHOD.invoke(store);

            for (Ref<EntityStore> targetRef : nearbyEntities) {
                if (targetRef == null || !targetRef.isValid() || targetRef.equals(playerEntityRef)) {continue;}

                if (damageCause == null) return;

                if (store.getComponent(targetRef, PlayerRef.getComponentType()) != null) {playersHit++;}

                Damage damageEvent = new Damage(source, damageCause, aoeDamage);
                damageEvent.putMetaObject(DamageTrackingSystem.RPG_DAMAGE_PROCESSED, false);

                commandBuffer.invoke(targetRef, damageEvent);
                targetsHit++;
            }

            CONSUME_METHOD.invoke(commandBuffer);
        } catch (Exception e) {
            e.printStackTrace();
        }

        playerRef.sendMessage(Message.raw(" Tengai Shinsei! Impact hit " + targetsHit + " target(s) (" + playersHit + " player(s)) in the area.").color(Color.RED));
    }
}