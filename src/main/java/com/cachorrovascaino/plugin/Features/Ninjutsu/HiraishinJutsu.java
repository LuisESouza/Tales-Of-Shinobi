package com.cachorrovascaino.plugin.Features.Ninjutsu;

import com.cachorrovascaino.plugin.Abstractions.Jutsu;
import com.cachorrovascaino.plugin.Abstractions.SkillType;
import com.cachorrovascaino.plugin.Data.PlayerData;
import com.cachorrovascaino.plugin.Main;
import com.cachorrovascaino.plugin.Utils.BlockJutsuUtils;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.protocol.SoundCategory;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.teleport.Teleport;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.ParticleUtil;
import com.hypixel.hytale.server.core.universe.world.SoundUtil;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.joml.Vector3d;

import java.util.List;

public class HiraishinJutsu implements Jutsu {

    public static final HiraishinJutsu INSTANCE = new HiraishinJutsu();
    public static final String HIRAISHIN_PARTICLE = "Hiraishin_Particle";

    @Override public String getId() { return "hiraishin_jutsu"; }
    @Override public String getDisplayName() { return "Hiraishin no Jutsu"; }
    @Override public float getChakraCost() { return 30.0f; }
    @Override public float getCooldown() { return 5.0f; }
    @Override public SkillType getType() { return SkillType.NINJUTSU; }

    @Override
    public void execute(PlayerRef playerRef, Ref<EntityStore> playerEntityRef, Store<EntityStore> store, World world) {
        if (playerRef == null || !playerEntityRef.isValid()) return;

        SoundUtil.playSoundEvent2dToPlayer(playerRef, "SFX_Hiraishin_Minato", SoundCategory.SFX);

        world.execute(() -> {
            if (!playerEntityRef.isValid()) return;

            PlayerData data = Main.getDataManager().getPlayerData(playerRef.getUuid());
            if (data == null) return;

            List<Vector3d> marks = data.getHiraishinMarks();
            if (marks == null || marks.isEmpty()) return;

            TransformComponent currentTransform = store.getComponent(playerEntityRef, TransformComponent.getComponentType());
            if (currentTransform == null) return;

            Vector3d playerPos = currentTransform.getPosition();
            Vector3d lookDir = BlockJutsuUtils.getHorizontalLookVector(playerEntityRef, store, currentTransform);

            Vector3d targetMark = selectBestMark(marks, playerPos, lookDir);
            if (targetMark == null) return;

            Transform targetTransform = new Transform(
                    targetMark.x,
                    targetMark.y + 0.2,
                    targetMark.z,
                    currentTransform.getRotation().pitch(),
                    currentTransform.getRotation().yaw(),
                    0.0f
            );

            Teleport teleport = Teleport.createForPlayer(world, targetTransform);
            store.addComponent(playerEntityRef, Teleport.getComponentType(), teleport);
        });
    }

    private Vector3d selectBestMark(List<Vector3d> marks, Vector3d playerPos, Vector3d lookDir) {
        Vector3d bestMark = null;
        double bestDot = 0.707;

        Vector3d toMark = new Vector3d();

        for (Vector3d mark : marks) {
            toMark.set(mark).sub(playerPos);
            toMark.y = 0;

            if (toMark.lengthSquared() < 0.0001) continue;

            toMark.normalize();
            double dot = lookDir.dot(toMark);

            if (dot > bestDot) {
                bestDot = dot;
                bestMark = mark;
            }
        }

        return (bestMark != null) ? bestMark : marks.get(marks.size() - 1);
    }
}