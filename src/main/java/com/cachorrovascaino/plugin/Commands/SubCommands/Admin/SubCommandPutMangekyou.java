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

public class SubCommandPutMangekyou extends AbstractPlayerCommand {
    private final RequiredArg<String> nameArg;

    public SubCommandPutMangekyou() {
        super("mangekyou", "Alter mangekyou player.");
        this.nameArg = this.withRequiredArg("Name mangekyou. Ex: OBITO", "Name mangekyou", ArgTypes.STRING);
        this.requireNoPermission();
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

        String name = this.nameArg.get(commandContext);
        data.setMangekyouType(name);
        Main.getDataManager().savePlayer(playerRef.getUuid());

        commandContext.sendMessage(Message.raw("Successfully"));
    }
}
