/*
 * Copyright (C) 2023 timesnake
 */

package de.timesnake.basic.loungebridge.core.main;

import de.timesnake.basic.bukkit.util.Server;
import de.timesnake.basic.loungebridge.util.tool.WorldBorderToolManager;
import de.timesnake.library.chat.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public class BasicLoungeBridge extends JavaPlugin {

  public static BasicLoungeBridge getPlugin() {
    return plugin;
  }

  private static BasicLoungeBridge plugin;

  @Override
  public void onEnable() {
    plugin = this;

    Server.getCommandManager().addCommand(this, "worldborder", List.of("wb"),
        new WorldBorderToolManager(), Plugin.GAME);
  }
}
