package com.cachorrovascaino.plugin.Interactions;

import com.cachorrovascaino.plugin.Main;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInteraction;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;

/**
 * FIX: substitui o antigo PacketListener/SyncInteractionChains (que parou de chegar no
 * PlayerPacketFilter genérico depois da atualização do jogo - agora GenericPacketHandler
 * despacha esse pacote direto pra uma fila interna, sem passar pelo PacketAdapters).
 *
 * Essa classe roda dentro do sistema NATIVO de Interaction/RootInteraction, ligada via
 * UnarmedInteractions (estado "sem item na mão"). O InteractionType (Primary/Use) já vem
 * resolvido no parâmetro "type" do tick0 - não precisamos mais inspecionar pacote cru.
 */
public class JutsuComboInteraction extends SimpleInteraction {

    public static final BuilderCodec<JutsuComboInteraction> CODEC =
            BuilderCodec.builder(JutsuComboInteraction.class, JutsuComboInteraction::new, SimpleInteraction.CODEC)
                    .build();

    @Override
    protected void tick0(boolean firstRun, float time, @Nonnull InteractionType type,
                         @Nonnull InteractionContext context, @Nonnull CooldownHandler cooldownHandler) {
        if (!firstRun) return;

        Ref<EntityStore> owningEntity = context.getOwningEntity();
        if(owningEntity == null) return;
        Store<EntityStore> store = owningEntity.getStore();

        PlayerRef playerRef = store.getComponent(owningEntity, PlayerRef.getComponentType());
        if (playerRef == null) return;

        boolean isRightClick = type == InteractionType.Use;
        Main.getJutsuManager().processClick(playerRef, isRightClick);
    }
}