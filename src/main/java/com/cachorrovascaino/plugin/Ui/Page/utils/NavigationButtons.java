package com.cachorrovascaino.plugin.Ui.Page.utils;

import com.cachorrovascaino.plugin.Main;
import com.cachorrovascaino.plugin.Ui.Page.SkillsPage;
import com.cachorrovascaino.plugin.Ui.Page.StatusPage;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;

public class NavigationButtons {

    /**
     * Registra os eventos de clique dos botões de navegação da UI.
     * Deve ser chamado dentro do build() da página.
     */
    public static void bindButtons(@Nonnull UIEventBuilder evt) {
        evt.addEventBinding(CustomUIEventBindingType.Activating, "#BtnStatusMenu", new EventData().append("Action", "OpenStatusMenu"), false);
        evt.addEventBinding(CustomUIEventBindingType.Activating, "#BtnSkillsMenu", new EventData().append("Action", "OpenSkillsMenu"), false);
    }

    /**
     * Processa a troca de página com base na ação enviada.
     * Deve ser chamado dentro do handleDataEvent() da página.
     *
     * @return true se a ação for de navegação (para interromper a lógica interna da página)
     */
    public static boolean handleNavigation(
            @Nonnull String action,
            @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> ref,
            @Nonnull PlayerRef playerRef
    ) {
        if (action == null) return false;

        Player player = store.getComponent(ref, Player.getComponentType());
        if (player == null) return false;

        if ("OpenStatusMenu".equals(action)) {
            player.getPageManager().openCustomPage(ref, store, new StatusPage(playerRef, Main.getDataManager()));
            return true;
        }

        if ("OpenSkillsMenu".equals(action)) {
            player.getPageManager().openCustomPage(ref, store, new SkillsPage(playerRef, Main.getDataManager()));
            return true;
        }

        return false;
    }
}