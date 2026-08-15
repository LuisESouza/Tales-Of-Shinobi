package com.cachorrovascaino.plugin.Systems;

import com.cachorrovascaino.plugin.Data.PlayerData;
import com.cachorrovascaino.plugin.Main;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.math.raycast.RaycastAABB;
import com.hypixel.hytale.math.vector.Rotation3f;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.component.HeadRotation;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.physics.component.PhysicsValues;
import com.hypixel.hytale.server.core.modules.physics.component.Velocity;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.joml.Vector3d;

import javax.annotation.Nonnull;

public class WallRunSystem extends EntityTickingSystem<EntityStore> {

    // Configurações do Sistema
    private static final float CHAKRA_BASE_COST_PER_SEC = 6.0f; // Custo base de Chakra por segundo
    private static final double CLIMB_SPEED = 0.45;              // Velocidade de subida na parede
    private static final float MAX_WALL_DISTANCE = 1.1f;         // Distância máxima para detectar a parede
    private static final float WALL_PITCH_ANGLE = (float) Math.toRadians(75.0); // Ângulo para deitar o corpo (Naruto style)

    @Override
    public void tick(float dt, int index, @Nonnull ArchetypeChunk<EntityStore> chunk, @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer) {
        var ref = chunk.getReferenceTo(index);
        if (!ref.isValid()) return;

        Player player = store.getComponent(ref, Player.getComponentType());
        PlayerRef playerRef = (PlayerRef) store.getComponent(ref, PlayerRef.getComponentType());
        PhysicsValues physics = store.getComponent(ref, PhysicsValues.getComponentType());
        TransformComponent transform = store.getComponent(ref, TransformComponent.getComponentType());
        HeadRotation headRotation = store.getComponent(ref, HeadRotation.getComponentType());
        Velocity velocity = store.getComponent(ref, Velocity.getComponentType());

        if (player == null || playerRef == null || physics == null || transform == null || headRotation == null || velocity == null) return;

        PlayerData data = Main.getDataManager().getPlayerData(playerRef.getUuid());

        // 1. Validação de Chakra suficiente
        if (data == null || data.getCurrentChakra() < 1.0f) {
            resetWallState(physics, transform);
            return;
        }

        // 2. Raycast Nativo (RaycastAABB) para detectar se há parede vertical à frente
        boolean isTouchingWall = checkWallCollision(transform, headRotation);

        if (isTouchingWall) {
            // Calcula eficiência baseada no chakraControl do jogador (quanto maior o controle, menor o gasto)
            float efficiency = Math.max(0.1f, 1.0f - (data.getChakraControl() * 0.05f));
            float chakraCost = (CHAKRA_BASE_COST_PER_SEC * efficiency) * dt;

            if (data.getCurrentChakra() >= chakraCost) {
                // Consome o Chakra com base na sua estrutura de PlayerData
                float newChakra = Math.max(0.0f, data.getCurrentChakra() - chakraCost);
                data.setCurrentChakra(newChakra);

                // --- A. FÍSICA E VELOCIDADE ---
                velocity.setY(CLIMB_SPEED);

                // --- B. VISUAL (Corpo paralelo à parede) ---
                Rotation3f currentRot = transform.getRotation();
                // Ajusta o Pitch do corpo para inclinar (~75°), mantendo o Yaw (direção)
                Rotation3f wallRot = new Rotation3f(currentRot.yaw(), WALL_PITCH_ANGLE, currentRot.roll());
                transform.teleportRotation(wallRot);

                return;
            }
        }

        // 3. Se não estiver na parede ou o Chakra acabar, restaura o estado padrão
        resetWallState(physics, transform);
    }

    /**
     * Usa a RaycastAABB nativa da engine para verificar colisão com superfície vertical
     */
    private boolean checkWallCollision(TransformComponent transform, HeadRotation headRotation) {
        Vector3d pos = transform.getPosition();
        Vector3d dir = headRotation.getDirection();

        // Origem do raio na altura do peito do jogador
        double ox = pos.x();
        double oy = pos.y() + 1.2;
        double oz = pos.z();

        // Vetor de direção horizontal baseado no olhar da cabeça
        double dx = dir.x();
        double dy = 0.0;
        double dz = dir.z();

        // Bloco de destino aproximado para criar a caixa AABB do teste
        int targetX = (int) Math.floor(ox + dx * MAX_WALL_DISTANCE);
        int targetY = (int) Math.floor(oy);
        int targetZ = (int) Math.floor(oz + dz * MAX_WALL_DISTANCE);

        final boolean[] hitVerticalWall = {false};

        // Utiliza o método nativo de interseção por caixa delimitadora
        RaycastAABB.intersect(
                targetX, targetY, targetZ,
                targetX + 1.0, targetY + 1.0, targetZ + 1.0,
                ox, oy, oz,
                dx, dy, dz,
                (hit, rx, ry, rz, rdx, rdy, rdz, dist, nx, ny, nz) -> {
                    if (hit && dist <= MAX_WALL_DISTANCE && Math.abs(ny) < 0.1) {
                        hitVerticalWall[0] = true;
                    }
                }
        );

        return hitVerticalWall[0];
    }

    /**
     * Reseta a inclinação do corpo e as configurações de física de volta ao padrão
     */
    private void resetWallState(PhysicsValues physics, TransformComponent transform) {
        // Reseta inclinação do corpo para 0 (em pé)
        Rotation3f bodyRot = transform.getRotation();
        if (bodyRot.pitch() != 0.0f) {
            transform.teleportRotation(new Rotation3f(bodyRot.yaw(), 0.0f, bodyRot.roll()));
        }

        // Reseta valores de massa e atrito da física
        if (physics.getDragCoefficient() != 0.5) {
            physics.resetToDefault();
        }
    }

    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        return Query.and(
                Player.getComponentType(),
                PhysicsValues.getComponentType(),
                TransformComponent.getComponentType(),
                HeadRotation.getComponentType(),
                Velocity.getComponentType()
        );
    }
}