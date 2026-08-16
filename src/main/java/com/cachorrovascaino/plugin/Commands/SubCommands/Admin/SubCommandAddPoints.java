package com.cachorrovascaino.plugin.Commands.SubCommands.Admin;

import com.cachorrovascaino.plugin.Data.PlayerData;
import com.cachorrovascaino.plugin.Main;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import javax.annotation.Nonnull;

public class SubCommandAddPoints extends AbstractPlayerCommand {

    private final RequiredArg<Integer> amountArg;

    public SubCommandAddPoints() {
        super("addpoint", "Adds attribute points to the player.");
        this.amountArg = this.withRequiredArg("amount", "Amount of points", ArgTypes.INTEGER);
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

        data.addPoint(amount);

        Main.getDataManager().savePlayer(playerRef.getUuid());

        commandContext.sendMessage(Message.raw("Successfully added " + amount + " status point(s)!"));
    }
}