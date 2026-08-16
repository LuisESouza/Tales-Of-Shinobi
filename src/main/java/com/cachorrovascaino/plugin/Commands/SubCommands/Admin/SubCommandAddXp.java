package com.cachorrovascaino.plugin.Commands.SubCommands.Admin;

import com.cachorrovascaino.plugin.Data.PlayerData;
import com.cachorrovascaino.plugin.Main;
import com.cachorrovascaino.plugin.Ui.Hud.LevelHud;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;

public class SubCommandAddXp extends AbstractPlayerCommand {

    private final RequiredArg<Integer> amountArg;

    public SubCommandAddXp() {
        super("addxp", "Adds XP to the player.");
        this.amountArg = this.withRequiredArg("amount", "XP amount to add", ArgTypes.INTEGER);
    }

    @Override
    protected void execute(
            @Nonnull CommandContext commandContext,
            @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> ref,
            @Nonnull PlayerRef playerRef,
            @Nonnull World world
    ) {
        PlayerData data = Main.getDataManager().getPlayerData(playerRef.getUuid());

        if (data == null) {
            commandContext.sendMessage(Message.raw("Error: Player data was not found in cache/disk."));
            return;
        }

        int amount = this.amountArg.get(commandContext);
        boolean leveledUp = data.addXp(amount);
        Main.getDataManager().savePlayer(playerRef.getUuid());
        Player player = store.getComponent(ref, Player.getComponentType());
        if (player != null) {LevelHud.update(player, playerRef);}

        commandContext.sendMessage(Message.raw("Successfully added " + amount + " XP!"));
    }
}