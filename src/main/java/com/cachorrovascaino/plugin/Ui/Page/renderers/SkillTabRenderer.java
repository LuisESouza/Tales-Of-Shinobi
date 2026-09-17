package com.cachorrovascaino.plugin.Ui.Page.renderers;

import com.cachorrovascaino.plugin.Data.PlayerData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;

public interface SkillTabRenderer {
    void render(UICommandBuilder cmd, UIEventBuilder evt, PlayerData data, int currentPage, int itemsPerPage);
    int getTotalItems(PlayerData data);
}