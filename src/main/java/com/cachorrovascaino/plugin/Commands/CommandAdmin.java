package com.cachorrovascaino.plugin.Commands;

import com.cachorrovascaino.plugin.Commands.SubCommands.Admin.SubCommandAddPoints;
import com.cachorrovascaino.plugin.Commands.SubCommands.Admin.SubCommandAddXp;
import com.cachorrovascaino.plugin.Commands.SubCommands.Admin.SubCommandPutClan;
import com.cachorrovascaino.plugin.Commands.SubCommands.Admin.SubCommandPutMangekyou;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;

public class CommandAdmin extends AbstractCommandCollection {

    public CommandAdmin() {
        super("admin", "Comandos de administração do mod");

        this.requireNoPermission();
        addSubCommand(new SubCommandPutMangekyou());
        addSubCommand(new SubCommandAddPoints());
        addSubCommand(new SubCommandAddXp());
        addSubCommand(new SubCommandPutClan());
    }
}