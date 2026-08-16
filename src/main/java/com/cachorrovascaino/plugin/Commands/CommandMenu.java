package com.cachorrovascaino.plugin.Commands;

import com.cachorrovascaino.plugin.Commands.SubCommands.SubCommandSkillsMenu;
import com.cachorrovascaino.plugin.Commands.SubCommands.SubCommandStatusMenu;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;

public class CommandMenu extends AbstractCommandCollection {

    public CommandMenu(){
        super("menu", "Open main menu");

        addSubCommand(new SubCommandStatusMenu());
        addSubCommand(new SubCommandSkillsMenu());
    }

    @Override
    protected boolean canGeneratePermission() { return false; }
}
