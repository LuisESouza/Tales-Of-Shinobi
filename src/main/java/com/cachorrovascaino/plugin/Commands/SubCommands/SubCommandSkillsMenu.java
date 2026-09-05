package com.cachorrovascaino.plugin.Commands.SubCommands;

import com.cachorrovascaino.plugin.Main;
import com.cachorrovascaino.plugin.Ui.Page.SkillsPage;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

public class SubCommandSkillsMenu  extends AbstractPlayerCommand {

    public SubCommandSkillsMenu(){
        super("skill", "Open panel of status", false);
        this.requireNoPermission();
    }

    @Override
    protected void execute(
            @NonNullDecl CommandContext commandContext,
            @NonNullDecl Store<EntityStore> store,
            @NonNullDecl Ref<EntityStore> ref,
            @NonNullDecl PlayerRef playerRef,
            @NonNullDecl World world
    ) {
        try {
            Player player = store.getComponent(ref, Player.getComponentType());
            if (player == null) { return; }
            player.getPageManager().openCustomPage(ref, store, new SkillsPage(playerRef, Main.getDataManager()));
        } catch (Exception e) {
            commandContext.sendMessage(Message.raw("Error opening dashboard: " + e.getMessage()));
        }
    }
}
